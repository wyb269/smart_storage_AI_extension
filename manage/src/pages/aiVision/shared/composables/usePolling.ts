/**
 * 仓门异动功能 - 轮询器
 *
 * 依据 AI_HANDOFF.md 第 21.4 节「实时更新策略」与第 30 节「性能与可靠性要求」：
 *   - 仓门状态 10 秒；首页最近事件 15 秒；事件详情默认不轮询
 *   - 上一请求未完成时不得再次发起同类请求
 *   - 页面不可见时暂停或降低频率
 *   - 页面重新可见时立即刷新一次
 *   - 连续失败时退避，避免固定高频重试
 *   - 页面卸载时清除定时器
 *
 * <b>生命周期归属</b>（修复 P1-6 重复调度）：
 * 本模块<b>不再自行注册</b> onMounted / onBeforeUnmount，
 * 改由 {@link useDoorAnomalyPollingHost} 统一驱动 start / pause / dispose，
 * 避免「轮询自身 tick」与「宿主 resume」形成两条并行调度链。
 * 本模块仍保留 onBeforeUnmount 作为兜底清理，防止宿主遗漏。
 */

import { onBeforeUnmount, ref } from 'vue';

import { POLLING } from '../constants';

export interface PollingOptions {
  /** 轮询间隔（毫秒） */
  interval: number;
  /** 执行的任务，需自行处理异常 */
  task: () => Promise<void>;
}

export interface PollingController {
  running: ReturnType<typeof ref<boolean>>;
  failureCount: ReturnType<typeof ref<number>>;
  backingOff: ReturnType<typeof ref<boolean>>;
  /** 立即执行一次（忽略间隔） */
  refreshNow: () => void;
  /** 暂停并清理定时器 */
  pause: () => void;
  /** 恢复调度 */
  resume: () => void;
  /** 标记销毁，此后不再调度 */
  dispose: () => void;
}

/**
 * 创建受管控的轮询器。
 *
 * 特性：
 * 1. 同类请求未完成时跳过本次触发
 * 2. 连续失败按指数退避，最大上限 RETRY_MAX_DELAY
 * 3. 重调度前校验 paused/disposed，防止隐藏期间或卸载后产生孤儿定时器
 * 4. dispose 后不再写任何响应式状态
 */
export function createPolling(options: PollingOptions): PollingController {
  const { interval, task } = options;

  /** 任务是否正在执行 */
  const running = ref(false);
  /** 连续失败次数 */
  const failureCount = ref(0);
  /** 是否已销毁 */
  const destroyed = ref(false);
  /** 当前是否在退避中 */
  const backingOff = ref(false);
  /** 是否被暂停（页面隐藏或宿主暂停） */
  const paused = ref(false);

  let timer: ReturnType<typeof setTimeout> | null = null;

  /** 计算下一次间隔：失败次数越多，间隔越长 */
  function nextDelay(): number {
    if (failureCount.value === 0) return interval;
    const delay = POLLING.RETRY_BASE_DELAY * 2 ** (failureCount.value - 1);
    return Math.min(delay, POLLING.RETRY_MAX_DELAY);
  }

  function clearTimer() {
    if (timer) {
      clearTimeout(timer);
      timer = null;
    }
  }

  /** 能否继续调度：销毁或暂停时一律不再排程 */
  function canSchedule(): boolean {
    return !destroyed.value && !paused.value;
  }

  function schedule() {
    clearTimer();
    // 第21.4节：重调度前必须校验，避免隐藏/卸载后残留定时器
    if (!canSchedule()) return;
    timer = setTimeout(tick, nextDelay());
  }

  /** 执行一轮并按结果安排下一次 */
  async function tick() {
    if (!canSchedule()) return;

    // 第21.4节：上一请求未完成时不重复发起
    if (running.value) {
      schedule();
      return;
    }

    running.value = true;
    try {
      await task();
      failureCount.value = 0;
    } catch {
      failureCount.value += 1;
      // 仅记录模块内计数，不打印响应体，避免敏感信息进入控制台
      console.warn('[aiVision] 轮询失败，连续第', failureCount.value, '次');
    } finally {
      running.value = false;
    }

    // 请求可能耗时较久，期间页面可能已隐藏或组件已卸载，
    // 因此这里必须再次校验（修复 P1-6）。
    if (!canSchedule()) return;
    backingOff.value = failureCount.value > 0;
    schedule();
  }

  /** 立即执行一次（忽略间隔） */
  function refreshNow() {
    clearTimer();
    if (destroyed.value || paused.value) return;
    void tick();
  }

  /** 暂停轮询并清理定时器 */
  function pause() {
    paused.value = true;
    clearTimer();
  }

  /** 恢复轮询 */
  function resume() {
    if (destroyed.value) return;
    paused.value = false;
    schedule();
  }

  /** 标记销毁；此后不再调度也不再写状态 */
  function dispose() {
    destroyed.value = true;
    paused.value = true;
    clearTimer();
  }

  onBeforeUnmount(() => {
    // 兜底清理：即使宿主未正确调用 dispose，也不会残留定时器
    dispose();
  });

  return {
    running,
    failureCount,
    backingOff,
    refreshNow,
    pause,
    resume,
    dispose,
  };
}
