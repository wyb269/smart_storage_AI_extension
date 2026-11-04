<template>
  <div class="belt-monitor">
    <div class="belt-monitor__head">
      <div class="belt-monitor__head-left">
        <t-icon name="play-circle" class="belt-monitor__head-icon" />
        <span class="belt-monitor__title">{{ headTitle }}</span>
      </div>
      <t-link theme="primary" size="small" @click="toggleFullscreen">全屏</t-link>
    </div>

    <div ref="stageRef" class="belt-monitor__stage">
      <!-- 可播放时才挂 video，避免无地址时元素报错刷屏 -->
      <video
        v-if="showVideo"
        ref="videoRef"
        class="belt-monitor__el"
        :src="stream?.playableUrl || undefined"
        controls
        autoplay
        muted
        playsinline
        @error="onVideoError"
        @playing="state = 'PLAYING'"
      />

      <div v-else class="belt-monitor__placeholder">
        <t-icon :name="placeholderIcon" size="40" class="belt-monitor__placeholder-icon" />
        <p class="belt-monitor__placeholder-text">{{ stateText }}</p>
        <t-button
          v-if="state === 'ERROR' || state === 'NO_ADDRESS' || state === 'EXPIRED'"
          theme="default"
          variant="outline"
          size="small"
          @click="emit('retry')"
        >
          <template #icon><t-icon name="refresh" /></template>
          重试
        </t-button>
      </div>
    </div>

    <div class="belt-monitor__foot">
      <span class="belt-monitor__foot-text">{{ stateText }}</span>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import { PLAYER_STATE_TEXT } from '../constants';
import type { MonitorPoint, PlayerVisibleState, StreamInfo } from '../types';

/**
 * 实时监控区。
 *
 * 一期只使用原生 <video>，不引入任何播放器库；
 * HLS / FLV / WebRTC 明确提示不支持，而不是静默黑屏。
 *
 * 与视频遮挡那一版的差异：多一个 NOT_DEPLOYED 分支。
 * 未部署点位没有摄像头可播，提示语要指向「装摄像头」而不是「查网络」。
 */
const props = defineProps<{
  point: MonitorPoint | null;
  stream: StreamInfo | null;
  loading?: boolean;
}>();

const emit = defineEmits<{ (e: 'retry'): void }>();

const stageRef = ref<HTMLElement | null>(null);
const videoRef = ref<HTMLVideoElement | null>(null);
const state = ref<PlayerVisibleState>('NO_POINT_SELECTED');

/** 标题取绑定摄像头的编码与位置；未部署时退化为皮带机名，避免出现「CAM- 空」 */
const headTitle = computed(() => {
  const point = props.point;
  if (!point) return '实时监控';
  if (!point.cameraId) return `实时监控 - ${point.deviceName}（未部署）`;
  return `实时监控 - ${point.cameraCode} ${point.cameraName || point.areaName}`;
});

/**
 * 摄像头是否在线：直接看后端 deviceStatus。
 *
 * ⚠️ 这与左侧卡片的综合展示态（含 90 分钟心跳新鲜度）是两条判定链路，
 * 可能出现 deviceStatus=ONLINE 但心跳过期、卡片显示「离线」而这里显示「播放中」。
 * 以谁为准是产品口径问题（视频遮挡那版同样存在，未擅自统一），此处保持与模板一致。
 */
const cameraOnline = computed(() => props.point?.deviceStatus === 'ONLINE');

/** 未部署：点位没有绑定摄像头，与「离线」区分开 */
const cameraNotDeployed = computed(() => !!props.point && !props.point.cameraId);

const showVideo = computed(() => props.stream?.canPlay === true && !!props.stream?.playableUrl);

const stateText = computed(() => {
  if (props.loading) return PLAYER_STATE_TEXT.LOADING;
  return PLAYER_STATE_TEXT[state.value];
});

const placeholderIcon = computed(() => {
  if (state.value === 'OFFLINE') return 'poweroff';
  if (state.value === 'NOT_DEPLOYED') return 'disable';
  if (state.value === 'ERROR') return 'error-circle';
  return 'info-circle';
});

function onVideoError() {
  state.value = 'ERROR';
}

function toggleFullscreen() {
  const el = stageRef.value;
  if (!el) return;
  if (document.fullscreenElement) {
    void document.exitFullscreen();
  } else {
    void el.requestFullscreen?.();
  }
}

/** 由 props 驱动状态机；点位、流信息或加载态任一变化都要重算 */
watch(
  () => [props.point, props.stream, props.loading] as const,
  () => {
    if (!props.point) {
      state.value = 'NO_POINT_SELECTED';
      return;
    }
    // 未部署必须在离线判断之前：未部署点位的 deviceStatus 也是 UNKNOWN，
    // 先判离线会让用户以为设备掉线，实际是根本没装摄像头。
    if (cameraNotDeployed.value) {
      state.value = 'NOT_DEPLOYED';
      return;
    }
    if (!cameraOnline.value) {
      state.value = 'OFFLINE';
      return;
    }
    if (props.loading || !props.stream) {
      state.value = 'LOADING';
      return;
    }
    if (props.stream.canPlay && props.stream.playableUrl) {
      state.value = 'PLAYING';
      return;
    }
    const reason = props.stream.unavailableReason;
    if (reason === 'URL_EXPIRED') {
      state.value = 'EXPIRED';
      return;
    }
    if (reason === 'UNSUPPORTED_PROTOCOL') {
      state.value = 'UNSUPPORTED_PROTOCOL';
      return;
    }
    if (reason === 'CAMERA_NOT_BOUND') {
      state.value = 'NOT_DEPLOYED';
      return;
    }
    state.value = 'NO_ADDRESS';
  },
  { immediate: true, deep: true },
);
</script>
<style lang="less" scoped>
.belt-monitor {
  display: flex;
  flex-direction: column;
  background-color: var(--td-bg-color-container);
  border-radius: var(--td-radius-medium);
  padding: var(--td-comp-paddingLR-l) var(--td-comp-paddingTB-l);
  height: 100%;
}

.belt-monitor__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: var(--td-comp-margin-m);
}

.belt-monitor__head-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.belt-monitor__head-icon {
  font-size: 18px;
  color: var(--td-brand-color);
  flex-shrink: 0;
}

.belt-monitor__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--td-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.belt-monitor__stage {
  position: relative;
  flex: 1;
  min-height: 320px;
  background-color: #1a1a1a;
  border-radius: var(--td-radius-medium);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.belt-monitor__el {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background-color: #000;
}

.belt-monitor__placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #a6a6a6;
}

.belt-monitor__placeholder-icon {
  opacity: 0.7;
}

.belt-monitor__placeholder-text {
  margin: 0;
  font-size: 14px;
}

.belt-monitor__foot {
  margin-top: 10px;
}

.belt-monitor__foot-text {
  font-size: 12px;
  color: var(--td-text-color-secondary);
}
</style>
