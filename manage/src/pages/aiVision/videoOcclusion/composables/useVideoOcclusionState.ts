/**
 * 视频遮挡检测功能 - 页面状态层
 *
 * 统一编排「摄像头状态」与「最近事件」的加载、轮询与竞态控制，
 * 供监测首页与历史记录页共同复用。
 *
 * 要点：
 * - 首次进入并行请求摄像头列表和最近事件
 * - 默认选择第一个在线摄像头
 * - 快速切换时旧请求不得覆盖新选择（竞态守卫）
 * - 轮询生命周期由共享的 usePollingHost 统一驱动
 */

import { computed, onBeforeUnmount, ref } from 'vue';

import { getStreamInfo, listCameraStatus, listRecentEvent } from '@/api/videoOcclusion';

import { createPolling } from '../../shared/composables/usePolling';
import { useRequestRaceGuard } from '../../shared/composables/useRequestRaceGuard';
import { pickDefaultItem } from '../../shared/displayStatus';
import { POLLING, RECENT_EVENT_LIMIT } from '../constants';
import type { CameraStatus, OcclusionEvent, StreamInfo } from '../types';

export function useVideoOcclusionState(options: { recentEnabled?: boolean } = {}) {
  const { recentEnabled = true } = options;

  /* ---------- 状态 ---------- */
  const cameras = ref<CameraStatus[]>([]);
  const recentEvents = ref<OcclusionEvent[]>([]);
  const selectedCameraId = ref<string>('');
  const streamInfo = ref<StreamInfo | null>(null);

  const camerasLoading = ref(false);
  const eventsLoading = ref(false);
  const streamLoading = ref(false);
  const camerasError = ref('');
  const eventsError = ref('');

  /* ---------- 竞态守卫 ---------- */
  const camerasGuard = useRequestRaceGuard();
  const eventsGuard = useRequestRaceGuard();
  const streamGuard = useRequestRaceGuard();

  /* ---------- 派生数据 ---------- */

  const selectedCamera = computed<CameraStatus | null>(() => {
    if (!selectedCameraId.value) return null;
    return cameras.value.find((c) => c.cameraId === selectedCameraId.value) ?? null;
  });

  /** 无摄像头时的空态 */
  const isEmpty = computed(() => !camerasLoading.value && cameras.value.length === 0);

  /* ---------- 加载摄像头列表 ---------- */

  async function loadCameras(): Promise<void> {
    const token = camerasGuard.begin();
    camerasLoading.value = true;
    camerasError.value = '';
    try {
      const list = await listCameraStatus();
      // 过期结果直接丢弃
      if (!camerasGuard.isCurrent(token)) return;

      cameras.value = list;

      // 默认选择第一个在线摄像头；全部离线时选第一个但不自动播放。
      // 必须走 selectCamera 而不是直接改 selectedCameraId：只有它会触发取流。
      // 首屏那次 loadCameras 可能被轮询的竞态守卫整份丢弃（visibilitychange 会
      // 立刻刷一轮），此时选中由真正落地的那一轮完成，取流也就跟着补上了。
      const defaultId = pickDefaultItem(list).selected?.cameraId ?? '';
      if (!defaultId) {
        selectedCameraId.value = '';
      } else if (defaultId !== selectedCameraId.value) {
        selectCamera(defaultId);
      }
    } catch (error: any) {
      if (camerasGuard.isCurrent(token)) {
        camerasError.value = error?.message ?? '摄像头状态加载失败';
      }
      // 必须 rethrow，轮询靠它计数并做指数退避
      throw error;
    } finally {
      if (camerasGuard.isCurrent(token)) {
        camerasLoading.value = false;
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

  async function loadStreamInfo(cameraId: string): Promise<void> {
    const token = streamGuard.begin();
    if (!cameraId) {
      streamInfo.value = null;
      return;
    }
    streamLoading.value = true;
    try {
      const info = await getStreamInfo(cameraId);
      if (!streamGuard.isCurrent(token)) return;
      streamInfo.value = info;
    } catch {
      if (streamGuard.isCurrent(token)) {
        // 取播放地址失败时保留摄像头信息并提供重试
        streamInfo.value = {
          cameraId,
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

  /* ---------- 切换摄像头 ---------- */

  /**
   * 切换选中的摄像头。这是唯一入口：由它统一完成
   * 失效旧播放请求、清空旧流信息与拉取新流信息。
   * 直接改写 selectedCameraId 不会触发取流。
   */
  function selectCamera(cameraId: string): void {
    if (selectedCameraId.value === cameraId) return;
    selectedCameraId.value = cameraId;
    streamGuard.invalidate();
    streamInfo.value = null;
    void loadStreamInfo(cameraId);
  }

  /* ---------- 轮询 ---------- */

  const cameraPolling = createPolling({
    interval: POLLING.ITEM_INTERVAL,
    task: loadCameras,
  });

  const eventPolling = recentEnabled
    ? createPolling({
        interval: POLLING.EVENT_INTERVAL,
        task: loadRecentEvents,
      })
    : null;

  const activePollings = eventPolling ? [cameraPolling, eventPolling] : [cameraPolling];

  onBeforeUnmount(() => {
    camerasGuard.invalidate();
    eventsGuard.invalidate();
    streamGuard.invalidate();
    activePollings.forEach((p) => p.dispose());
  });

  return {
    // 数据
    cameras,
    recentEvents,
    selectedCamera,
    selectedCameraId,
    streamInfo,
    // 加载态
    camerasLoading,
    eventsLoading,
    streamLoading,
    camerasError,
    eventsError,
    isEmpty,
    // 行为
    loadCameras,
    loadRecentEvents,
    loadStreamInfo,
    selectCamera,
    // 轮询控制（生命周期由 usePollingHost 统一驱动）
    pollings: activePollings,
    refreshCameras: cameraPolling.refreshNow,
    refreshEvents: eventPolling?.refreshNow,
    cameraPollFailureCount: cameraPolling.failureCount,
  };
}
