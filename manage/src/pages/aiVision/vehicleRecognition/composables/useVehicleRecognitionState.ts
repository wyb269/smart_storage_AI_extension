/**
 * 车辆智能识别功能 - 页面状态层
 *
 * 统一编排「今日识别车辆」与「最近识别记录」的加载、轮询与竞态控制，
 * 供监测首页与历史记录页共同复用。
 *
 * 要点：
 * - 首次进入并行请求今日识别列表和最近记录
 * - 默认选中今日最新一条识别记录；选中即取该记录所属点位的摄像头
 * - 快速切换时旧请求不得覆盖新选择（竞态守卫）
 * - 轮询生命周期由共享的 usePollingHost 统一驱动
 */

import { computed, onBeforeUnmount, ref } from 'vue';

import { getStreamInfo, listRecentEvent } from '@/api/vehicleRecognition';

import { createPolling } from '../../shared/composables/usePolling';
import { useRequestRaceGuard } from '../../shared/composables/useRequestRaceGuard';
import { POLLING, RECENT_EVENT_LIMIT } from '../constants';
import type { StreamInfo, TodayVehicle, VehicleEvent } from '../types';

export function useVehicleRecognitionState(options: { recentEnabled?: boolean } = {}) {
  const { recentEnabled = true } = options;

  /* ---------- 状态 ---------- */
  const todayVehicles = ref<TodayVehicle[]>([]);
  const recentEvents = ref<VehicleEvent[]>([]);
  const selectedEventId = ref<string>('');
  const selectedPointId = ref<string>('');
  const streamInfo = ref<StreamInfo | null>(null);

  const todayLoading = ref(false);
  const eventsLoading = ref(false);
  const streamLoading = ref(false);
  const todayError = ref('');
  const eventsError = ref('');

  /* ---------- 竞态守卫 ---------- */
  const todayGuard = useRequestRaceGuard();
  const eventsGuard = useRequestRaceGuard();
  const streamGuard = useRequestRaceGuard();

  /* ---------- 派生数据 ---------- */

  /** 当前选中的识别记录（左侧卡片高亮 + 右侧监控标题都依赖它） */
  const selectedVehicle = computed<TodayVehicle | null>(() => {
    if (!selectedEventId.value) return null;
    return todayVehicles.value.find((v) => v.eventId === selectedEventId.value) ?? null;
  });

  /** 无识别记录时的空态 */
  const isEmpty = computed(() => !todayLoading.value && todayVehicles.value.length === 0);

  /* ---------- 加载今日识别车辆 ---------- */

  async function loadTodayVehicles(): Promise<void> {
    const token = todayGuard.begin();
    todayLoading.value = true;
    todayError.value = '';
    try {
      // 真实库最近一批记录可跨天；当天无车时仍能浏览最近识别结果。
      const list = await listRecentEvent(20);
      // 过期结果直接丢弃
      if (!todayGuard.isCurrent(token)) return;

      todayVehicles.value = list;

      // 默认选中今日最新一条（后端按识别时间倒序，取第一条）。
      // 必须走 selectItem 而不是直接改 selectedEventId：只有它会触发取流。
      // 若当前选中项已不在新列表里（跨天/被处理），重新落到最新一条。
      const stillExists = list.some((v) => v.eventId === selectedEventId.value);
      const defaultId = stillExists ? selectedEventId.value : (list[0]?.eventId ?? '');
      if (!defaultId) {
        selectedEventId.value = '';
        selectedPointId.value = '';
        streamInfo.value = null;
      } else if (defaultId !== selectedEventId.value) {
        selectItem(defaultId);
      }
    } catch (error: any) {
      if (todayGuard.isCurrent(token)) {
        todayError.value = error?.message ?? '最近识别车辆加载失败';
      }
      // 必须 rethrow，轮询靠它计数并做指数退避
      throw error;
    } finally {
      if (todayGuard.isCurrent(token)) {
        todayLoading.value = false;
      }
    }
  }

  /* ---------- 加载最近记录 ---------- */

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
        eventsError.value = error?.message ?? '最近识别记录加载失败';
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

  /* ---------- 切换识别记录 ---------- */

  /**
   * 切换选中的识别记录。这是唯一入口：由它统一完成
   * 失效旧播放请求、清空旧流信息与按记录所属点位拉取新流信息。
   * 直接改写 selectedEventId 不会触发取流。
   */
  function selectItem(eventId: string): void {
    if (selectedEventId.value === eventId) return;
    const record = todayVehicles.value.find((v) => v.eventId === eventId);
    selectedEventId.value = eventId;
    selectedPointId.value = record?.pointId ?? '';
    streamGuard.invalidate();
    streamInfo.value = null;
    void loadStreamInfo(selectedPointId.value);
  }

  /* ---------- 轮询 ---------- */

  const todayPolling = createPolling({
    interval: POLLING.ITEM_INTERVAL,
    task: loadTodayVehicles,
  });

  const eventPolling = recentEnabled
    ? createPolling({
        interval: POLLING.EVENT_INTERVAL,
        task: loadRecentEvents,
      })
    : null;

  const activePollings = eventPolling ? [todayPolling, eventPolling] : [todayPolling];

  onBeforeUnmount(() => {
    todayGuard.invalidate();
    eventsGuard.invalidate();
    streamGuard.invalidate();
    activePollings.forEach((p) => p.dispose());
  });

  return {
    // 数据
    todayVehicles,
    recentEvents,
    selectedVehicle,
    selectedEventId,
    selectedPointId,
    streamInfo,
    // 加载态
    todayLoading,
    eventsLoading,
    streamLoading,
    todayError,
    eventsError,
    isEmpty,
    // 行为
    loadTodayVehicles,
    loadRecentEvents,
    loadStreamInfo,
    selectItem,
    // 轮询控制（生命周期由 usePollingHost 统一驱动）
    pollings: activePollings,
    refreshToday: todayPolling.refreshNow,
    refreshEvents: eventPolling?.refreshNow,
    todayPollFailureCount: todayPolling.failureCount,
  };
}
