/**
 * 仓门异动功能 - 轮询生命周期宿主
 *
 * 依据 AI_HANDOFF.md：
 *   - 第21.4节：上一请求未完成时不得再次发起同类请求；
 *                页面不可见时暂停或降低频率；
 *                页面重新可见时立即刷新一次；
 *                连续失败时退避；页面卸载时清除定时器。
 *   - 第30节：页面卸载后停止轮询、请求和播放器。
 *
 * 设计要点（修复 P1-6「页面隐藏与轮询存在重复调度风险」）：
 *
 * 1. **单一入口**：轮询只在 onMounted 由本宿主启动一次。
 *    页面自身的初始化请求（loadDoors / loadRecentEvents）不再另行启动轮询，
 *    避免「首次 tick 与 onMounted 重复加载同一批数据」。
 *
 * 2. **paused / disposed 双状态**：
 *    - `paused`：页面隐藏或主动暂停时为 true，此时不再调度定时器；
 *    - `disposed`：组件卸载后为 true，此后不再写任何响应式状态。
 *
 * 3. **重调度前校验**：任务在 `finally` 中重新调度前必须再次校验
 *    `paused`/`disposed`，否则「请求执行期间页面被隐藏」仍会留下孤儿定时器。
 *
 * 4. **恢复只启一个轮询**：visibilitychange 与 resume() 共用同一入口，
 *    且先清理已有定时器再启动，避免出现两个并行轮询。
 */

import { onBeforeUnmount, onMounted, ref } from 'vue';

export interface PollingHostOptions {
  /** 需要纳入统一生命周期的轮询器集合 */
  pollings: Array<{
    pause: () => void;
    resume: () => void;
    refreshNow: () => void;
  }>;
}

export function usePollingHost(options: PollingHostOptions) {
  const { pollings } = options;

  /** 页面是否被主动暂停（隐藏或手动暂停） */
  const paused = ref(false);
  /** 组件是否已卸载；为 true 后禁止任何状态写入与定时器调度 */
  const disposed = ref(false);

  let visibilityHandler: (() => void) | null = null;

  /** 统一启动入口：先清理再启动，保证同一时刻只有一个定时器链 */
  function startAll({ immediate }: { immediate: boolean }) {
    if (disposed.value || paused.value) return;
    pollings.forEach((p) => {
      if (immediate) {
        p.refreshNow();
      } else {
        p.resume();
      }
    });
  }

  /** 统一暂停入口：清理全部定时器 */
  function pauseAll() {
    pollings.forEach((p) => p.pause());
  }

  function handleVisibilityChange() {
    if (disposed.value) return;
    if (document.visibilityState === 'visible') {
      paused.value = false;
      // 第21.4节：重新可见时立即刷新一次
      startAll({ immediate: true });
    } else {
      // 页面不可见 → 标记 paused 并立即清定时器
      paused.value = true;
      pauseAll();
    }
  }

  onMounted(() => {
    // 首屏数据由页面自身初始化请求负责，此处只接管后续轮询，
    // 避免与页面初始化重复请求同一批数据（单一入口）。
    startAll({ immediate: false });

    visibilityHandler = handleVisibilityChange;
    document.addEventListener('visibilitychange', visibilityHandler);
  });

  onBeforeUnmount(() => {
    disposed.value = true;
    pauseAll();
    if (visibilityHandler) {
      document.removeEventListener('visibilitychange', visibilityHandler);
      visibilityHandler = null;
    }
  });

  return {
    paused,
    disposed,
    /** 页面主动暂停（供路由离开等场景调用） */
    pause: () => {
      paused.value = true;
      pauseAll();
    },
    /** 恢复轮询；若页面仍处于隐藏状态则不启动 */
    resume: () => {
      if (disposed.value) return;
      if (document.visibilityState !== 'visible') return;
      paused.value = false;
      startAll({ immediate: false });
    },
  };
}
