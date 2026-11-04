/**
 * 仓门异动功能 - 页面状态层
 *
 * 统一编排「仓门状态」与「最近事件」的加载、轮询与竞态控制，
 * 供监测首页与历史记录页共同复用。
 *
 * 依据 AI_HANDOFF.md：
 *   - 第16.6节：首次进入并行请求仓门列表和最近事件；
 *                默认选择第一个在线仓门
 *   - 第30节：快速切换仓门时旧请求不得覆盖新选择
 *   - 第21.4节：轮询要求与资源释放
 */

import { computed, onBeforeUnmount, ref } from 'vue';

import { getStreamInfo, listDoorStatus, listRecentEvent } from '@/api/doorAnomaly';

import { createPolling } from '../../shared/composables/usePolling';
import { useRequestRaceGuard } from '../../shared/composables/useRequestRaceGuard';
import { pickDefaultItem } from '../../shared/displayStatus';
import { POLLING, RECENT_EVENT_LIMIT } from '../constants';
import type { AnomalyEvent, DoorStatus, StreamInfo } from '../types';

/**
 * 仓门异动页面状态层。
 *
 * 配置项中的 recentEnabled 控制是否启用最近事件轮询，历史页可关闭。
 */
export function useDoorAnomalyState(options: { recentEnabled?: boolean } = {}) {
  const { recentEnabled = true } = options;

  /* ---------- 状态 ---------- */
  const doors = ref<DoorStatus[]>([]);
  const recentEvents = ref<AnomalyEvent[]>([]);
  const selectedDoorId = ref<string>('');
  const streamInfo = ref<StreamInfo | null>(null);

  const doorsLoading = ref(false);
  const eventsLoading = ref(false);
  const streamLoading = ref(false);
  const doorsError = ref('');
  const eventsError = ref('');

  /* ---------- 竞态守卫 ---------- */
  const doorsGuard = useRequestRaceGuard();
  const eventsGuard = useRequestRaceGuard();
  const streamGuard = useRequestRaceGuard();

  /* ---------- 派生数据 ---------- */

  const selectedDoor = computed<DoorStatus | null>(() => {
    if (!selectedDoorId.value) return null;
    return doors.value.find((d) => d.doorId === selectedDoorId.value) ?? null;
  });

  /** 当前是否处于无仓门状态（第31节边界场景） */
  const isEmpty = computed(() => !doorsLoading.value && doors.value.length === 0);

  /* ---------- 加载仓门列表 ---------- */

  async function loadDoors(): Promise<void> {
    const token = doorsGuard.begin();
    doorsLoading.value = true;
    doorsError.value = '';
    try {
      const list = await listDoorStatus();
      // 第30节：过期结果直接丢弃
      if (!doorsGuard.isCurrent(token)) return;

      doors.value = list;

      // 第16.6节：默认选择第一个在线仓门；全部离线时选第一个但不自动播放
      // 使用 pickDefaultItem 统一判定，避免首页选中离线仓门而呈现「无视频」
      if (!selectedDoorId.value || !list.some((d) => d.doorId === selectedDoorId.value)) {
        selectedDoorId.value = pickDefaultItem(list).selected?.doorId ?? '';
      }
    } catch (error: any) {
      if (doorsGuard.isCurrent(token)) {
        doorsError.value = error?.message ?? '仓门状态加载失败';
      }
      throw error;
    } finally {
      if (doorsGuard.isCurrent(token)) {
        doorsLoading.value = false;
      }
    }
  }

  /* ---------- 加载最近事件 ---------- */

  async function loadRecentEvents(): Promise<void> {
    const token = eventsGuard.begin();
    eventsLoading.value = true;
    eventsError.value = '';
    try {
      const list = await listRecentEvent(RECENT_EVENT_LIMIT);
      if (!eventsGuard.isCurrent(token)) return;
      recentEvents.value = list;
    } catch (error: any) {
      if (eventsGuard.isCurrent(token)) {
        eventsError.value = error?.message ?? '最近事件加载失败';
      }
      throw error;
    } finally {
      if (eventsGuard.isCurrent(token)) {
        eventsLoading.value = false;
      }
    }
  }

  /* ---------- 获取播放信息 ---------- */

  /**
   * 拉取指定仓门的短时播放信息。
   * 第26.2节：地址过期后需重新调用本方法。
   */
  async function loadStreamInfo(doorId: string): Promise<void> {
    const token = streamGuard.begin();
    if (!doorId) {
      streamInfo.value = null;
      return;
    }
    streamLoading.value = true;
    try {
      const info = await getStreamInfo(doorId);
      if (!streamGuard.isCurrent(token)) return;
      streamInfo.value = info;
    } catch {
      if (streamGuard.isCurrent(token)) {
        // 取播放地址失败时保留仓门信息并提供重试（第31节）
        streamInfo.value = {
          doorId,
          cameraId: null,
          playableUrl: null,
          protocol: 'MP4',
          expiresAt: null,
          canPlay: false,
          unavailableReason: 'FETCH_FAILED',
        };
      }
    } finally {
      if (streamGuard.isCurrent(token)) {
        streamLoading.value = false;
      }
    }
  }

  /* ---------- 切换仓门 ---------- */

  /**
   * 切换选中的仓门。
   * 第16.6节：点击仓门只切换监控画面，默认不自动过滤历史事件表格。
   */
  function selectDoor(doorId: string): void {
    if (selectedDoorId.value === doorId) return;
    selectedDoorId.value = doorId;
    // 立即失效旧的播放请求，避免慢响应覆盖新选择
    streamGuard.invalidate();
    streamInfo.value = null;
    void loadStreamInfo(doorId);
  }

  /* ---------- 轮询 ---------- */

  // 第21.4节：仓门状态 10 秒
  const doorPolling = createPolling({
    interval: POLLING.ITEM_INTERVAL,
    task: loadDoors,
  });

  // 第21.4节：首页最近事件 15 秒；历史页不启用
  const eventPolling = recentEnabled
    ? createPolling({
        interval: POLLING.EVENT_INTERVAL,
        task: loadRecentEvents,
      })
    : null;

  /** 纳入宿主统一调度的轮询器集合 */
  const activePollings = eventPolling ? [doorPolling, eventPolling] : [doorPolling];

  onBeforeUnmount(() => {
    // 卸载时取消在途请求的结果写入，并销毁全部轮询器
    doorsGuard.invalidate();
    eventsGuard.invalidate();
    streamGuard.invalidate();
    activePollings.forEach((p) => p.dispose());
  });

  return {
    // 数据
    doors,
    recentEvents,
    selectedDoor,
    selectedDoorId,
    streamInfo,
    // 加载态
    doorsLoading,
    eventsLoading,
    streamLoading,
    doorsError,
    eventsError,
    isEmpty,
    // 行为
    loadDoors,
    loadRecentEvents,
    loadStreamInfo,
    selectDoor,
    // 轮询控制（生命周期由 usePollingHost 统一驱动）
    pollings: activePollings,
    refreshDoors: doorPolling.refreshNow,
    refreshEvents: eventPolling?.refreshNow,
    // 运行指标（用于调试与状态提示）
    doorPollFailureCount: doorPolling.failureCount,
  };
}
