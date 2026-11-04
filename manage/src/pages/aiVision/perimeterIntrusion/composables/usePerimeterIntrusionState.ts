/**
 * 周界入侵识别功能 - 页面状态层
 *
 * 统一编排「防区段状态」与「最近事件」的加载、轮询与竞态控制，
 * 供监测首页与历史记录页共同复用。
 *
 * 要点：
 * - 首次进入并行请求防区段列表和最近事件
 * - 默认选择第一个可出画面的防区段
 * - 快速切换时旧请求不得覆盖新选择（竞态守卫）
 * - 轮询生命周期由共享的 usePollingHost 统一驱动
 * - 防区筛选下拉是**纯前端过滤**，只影响左侧网格：
 *   后端一次返回全量，切筛选不重新请求，因此当前选中的段与右侧画面不受干扰。
 */

import { computed, onBeforeUnmount, ref } from 'vue';

import { getStreamInfo, listMonitorPoint, listRecentEvent } from '@/api/perimeterIntrusion';

import { createPolling } from '../../shared/composables/usePolling';
import { useRequestRaceGuard } from '../../shared/composables/useRequestRaceGuard';
import { POLLING, RECENT_EVENT_LIMIT } from '../constants';
import type { MonitorPointStatus, PerimeterEvent, StreamInfo, Zone } from '../types';
import { pickDefaultPoint } from './usePerimeterDisplayStatus';

export function usePerimeterIntrusionState(options: { recentEnabled?: boolean } = {}) {
  const { recentEnabled = true } = options;

  /* ---------- 状态 ---------- */
  const points = ref<MonitorPointStatus[]>([]);
  const recentEvents = ref<PerimeterEvent[]>([]);
  const selectedPointId = ref<string>('');
  const streamInfo = ref<StreamInfo | null>(null);
  /** 防区筛选；空串 = 全部防区 */
  const zoneFilter = ref<Zone | ''>('');

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

  /** 网格实际渲染的列表：只按防区过滤，不改动选中态 */
  const visiblePoints = computed<MonitorPointStatus[]>(() => {
    if (!zoneFilter.value) return points.value;
    return points.value.filter((p) => p.zone === zoneFilter.value);
  });

  /** 无防区段时的空态 */
  const isEmpty = computed(() => !pointsLoading.value && points.value.length === 0);

  /* ---------- 加载防区段列表 ---------- */

  async function loadPoints(): Promise<void> {
    const token = pointsGuard.begin();
    pointsLoading.value = true;
    pointsError.value = '';
    try {
      const list = await listMonitorPoint();
      // 过期结果直接丢弃
      if (!pointsGuard.isCurrent(token)) return;

      points.value = list;

      // 默认选择第一个可出画面的防区段；全不可播时选第一个但不自动播放。
      // 必须走 selectItem 而不是直接改 selectedPointId：只有它会触发取流。
      // 默认选中看**全量**而不是当前筛选结果：切筛选只是换一批卡片，
      // 不该把右侧正在播的画面一起换掉。
      const defaultId = pickDefaultItem(list);
      if (!defaultId) {
        selectedPointId.value = '';
      } else if (defaultId !== selectedPointId.value) {
        selectItem(defaultId);
      }
    } catch (error: any) {
      if (pointsGuard.isCurrent(token)) {
        pointsError.value = error?.message ?? '防区段状态加载失败';
      }
      // 必须 rethrow，轮询靠它计数并做指数退避
      throw error;
    } finally {
      if (pointsGuard.isCurrent(token)) {
        pointsLoading.value = false;
      }
    }
  }

  function pickDefaultItem(list: MonitorPointStatus[]): string {
    return pickDefaultPoint(list).selected?.pointId ?? '';
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
        // 取播放地址失败时保留防区段信息并提供重试
        streamInfo.value = {
          pointId,
          cameraConfigId: null,
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

  /* ---------- 切换防区段 ---------- */

  /**
   * 切换选中的防区段。这是唯一入口：由它统一完成
   * 失效旧播放请求、清空旧流信息与拉取新流信息。
   * 直接改写 selectedPointId 不会触发取流。
   */
  function selectItem(pointId: string): void {
    if (selectedPointId.value === pointId) return;
    selectedPointId.value = pointId;
    streamGuard.invalidate();
    streamInfo.value = null;
    void loadStreamInfo(pointId);
  }

  /* ---------- 轮询 ---------- */

  const pointsPolling = createPolling({
    interval: POLLING.ITEM_INTERVAL,
    task: loadPoints,
  });

  const eventPolling = recentEnabled
    ? createPolling({
        interval: POLLING.EVENT_INTERVAL,
        task: loadRecentEvents,
      })
    : null;

  const activePollings = eventPolling ? [pointsPolling, eventPolling] : [pointsPolling];

  onBeforeUnmount(() => {
    pointsGuard.invalidate();
    eventsGuard.invalidate();
    streamGuard.invalidate();
    activePollings.forEach((p) => p.dispose());
  });

  return {
    // 数据
    points,
    visiblePoints,
    recentEvents,
    selectedPoint,
    selectedPointId,
    streamInfo,
    zoneFilter,
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
    selectItem,
    // 轮询控制（生命周期由 usePollingHost 统一驱动）
    pollings: activePollings,
    refreshPoints: pointsPolling.refreshNow,
    refreshEvents: eventPolling?.refreshNow,
    pointsPollFailureCount: pointsPolling.failureCount,
  };
}
