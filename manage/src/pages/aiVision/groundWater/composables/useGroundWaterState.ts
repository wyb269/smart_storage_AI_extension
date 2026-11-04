/**
 * 地面积水监测功能 - 页面状态层
 *
 * 统一编排「监测点位状态」与「最近事件」的加载、轮询与竞态控制，
 * 供监测首页与历史记录页共同复用。
 *
 * 要点：
 * - 首次进入并行请求点位列表和最近事件
 * - 默认选择第一个在线点位
 * - 快速切换时旧请求不得覆盖新选择（竞态守卫）
 * - 轮询生命周期由共享的 usePollingHost 统一驱动
 */

import { computed, onBeforeUnmount, ref } from 'vue';

import { getStreamInfo, listMonitorPoint, listRecentEvent } from '@/api/groundWater';

import { createPolling } from '../../shared/composables/usePolling';
import { useRequestRaceGuard } from '../../shared/composables/useRequestRaceGuard';
import { pickDefaultItem } from '../../shared/displayStatus';
import { POLLING, RECENT_EVENT_LIMIT } from '../constants';
import type { MonitorPointStatus, StreamInfo, WaterEvent } from '../types';

export function useGroundWaterState(options: { recentEnabled?: boolean } = {}) {
  const { recentEnabled = true } = options;

  /* ---------- 状态 ---------- */
  const points = ref<MonitorPointStatus[]>([]);
  const recentEvents = ref<WaterEvent[]>([]);
  const selectedPointId = ref<string>('');
  const streamInfo = ref<StreamInfo | null>(null);

  const pointsLoading = ref(false);
  const eventsLoading = ref(false);
  const streamLoading = ref(false);
  const pointsError = ref('');
  const eventsError = ref('');

  /* ---------- 竞态守卫 ---------- */
  const pointsGuard = useRequestRaceGuard();
  const eventsGuard = useRequestRaceGuard();
  const streamGuard = useRequestRaceGuard();

  /* ---------- 派生数据 ---------- */

  const selectedPoint = computed<MonitorPointStatus | null>(() => {
    if (!selectedPointId.value) return null;
    return points.value.find((p) => p.pointId === selectedPointId.value) ?? null;
  });

  /** 无监测点位时的空态 */
  const isEmpty = computed(() => !pointsLoading.value && points.value.length === 0);

  /* ---------- 加载监测点位列表 ---------- */

  async function loadPoints(): Promise<void> {
    const token = pointsGuard.begin();
    pointsLoading.value = true;
    pointsError.value = '';
    try {
      const list = await listMonitorPoint();
      // 过期结果直接丢弃
      if (!pointsGuard.isCurrent(token)) return;

      points.value = list;

      // 默认选择第一个在线点位；全部离线时选第一个但不自动播放。
      // 必须走 selectPoint 而不是直接改 selectedPointId：只有它会触发取流。
      // 遮挡那一版就是因为首屏只赋了 id，getStreamInfo 从来没发出去。
      // 首屏那次 loadPoints 可能被轮询的竞态守卫整份丢弃（visibilitychange 会
      // 立刻刷一轮），此时选中由真正落地的那一轮完成，取流也就跟着补上了。
      const defaultId = pickDefaultItem(list).selected?.pointId ?? '';
      if (!defaultId) {
        selectedPointId.value = '';
      } else if (defaultId !== selectedPointId.value) {
        selectPoint(defaultId);
      }
    } catch (error: any) {
      if (pointsGuard.isCurrent(token)) {
        pointsError.value = error?.message ?? '监测点位状态加载失败';
      }
      // 必须 rethrow，轮询靠它计数并做指数退避
      throw error;
    } finally {
      if (pointsGuard.isCurrent(token)) {
        pointsLoading.value = false;
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

  async function loadStreamInfo(pointId: string): Promise<void> {
    const token = streamGuard.begin();
    if (!pointId) {
      streamInfo.value = null;
      return;
    }
    streamLoading.value = true;
    try {
      const info = await getStreamInfo(pointId);
      if (!streamGuard.isCurrent(token)) return;
      streamInfo.value = info;
    } catch {
      if (streamGuard.isCurrent(token)) {
        // 取播放地址失败时保留点位信息并提供重试
        streamInfo.value = {
          pointId,
          pointName: null,
          cameraCode: null,
          platformCameraId: null,
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

  /* ---------- 切换点位 ---------- */

  /**
   * 切换选中的监测点位。这是唯一入口：由它统一完成
   * 失效旧播放请求、清空旧流信息与拉取新流信息。
   * 直接改写 selectedPointId 不会触发取流。
   */
  function selectPoint(pointId: string): void {
    if (selectedPointId.value === pointId) return;
    selectedPointId.value = pointId;
    streamGuard.invalidate();
    streamInfo.value = null;
    void loadStreamInfo(pointId);
  }

  /* ---------- 轮询 ---------- */

  const pointPolling = createPolling({
    interval: POLLING.ITEM_INTERVAL,
    task: loadPoints,
  });

  const eventPolling = recentEnabled
    ? createPolling({
        interval: POLLING.EVENT_INTERVAL,
        task: loadRecentEvents,
      })
    : null;

  const activePollings = eventPolling ? [pointPolling, eventPolling] : [pointPolling];

  onBeforeUnmount(() => {
    pointsGuard.invalidate();
    eventsGuard.invalidate();
    streamGuard.invalidate();
    activePollings.forEach((p) => p.dispose());
  });

  return {
    // 数据
    points,
    recentEvents,
    selectedPoint,
    selectedPointId,
    streamInfo,
    // 加载态
    pointsLoading,
    eventsLoading,
    streamLoading,
    pointsError,
    eventsError,
    isEmpty,
    // 行为
    loadPoints,
    loadRecentEvents,
    loadStreamInfo,
    selectPoint,
    // 轮询控制（生命周期由 usePollingHost 统一驱动）
    pollings: activePollings,
    refreshPoints: pointPolling.refreshNow,
    refreshEvents: eventPolling?.refreshNow,
    pointPollFailureCount: pointPolling.failureCount,
  };
}
