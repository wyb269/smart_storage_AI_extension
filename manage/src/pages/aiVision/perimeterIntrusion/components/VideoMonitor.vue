<template>
  <div class="video-monitor">
    <div class="video-monitor__head">
      <div class="video-monitor__head-left">
        <t-icon name="play-circle" class="video-monitor__head-icon" />
        <span class="video-monitor__title">{{ headTitle }}</span>
      </div>
      <t-link theme="primary" size="small" @click="toggleFullscreen">全屏</t-link>
    </div>

    <div ref="stageRef" class="video-monitor__stage">
      <!-- 可播放时才挂 video，避免无地址时元素报错刷屏 -->
      <video
        v-if="showVideo"
        ref="videoRef"
        class="video-monitor__el"
        :src="stream?.playableUrl || undefined"
        controls
        autoplay
        muted
        playsinline
        @error="onVideoError"
        @playing="state = 'PLAYING'"
      />

      <div v-else class="video-monitor__placeholder">
        <t-icon :name="placeholderIcon" size="40" class="video-monitor__placeholder-icon" />
        <p class="video-monitor__placeholder-text">{{ stateText }}</p>
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

    <div class="video-monitor__foot">
      <span class="video-monitor__foot-text">{{ stateText }}</span>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import { computePerimeterDisplayStatus } from '../composables/usePerimeterDisplayStatus';
import { PLAYER_STATE_TEXT } from '../constants';
import type { MonitorPointStatus, PlayerVisibleState, StreamInfo } from '../types';

/**
 * 实时监控区。
 *
 * 一期只使用原生 <video>，不引入任何播放器库；
 * HLS / FLV / WebRTC 明确提示不支持，而不是静默黑屏。
 *
 * <p><b>在线判定只有一条链路</b>：遮挡那一版一处用后端 deviceStatus、
 * 一处用前端心跳新鲜度，二者会互相矛盾（交接文档 §8 列为待决策项）。
 * 本功能统一用 computePerimeterDisplayStatus 的结果判「能不能播」，
 * 因此撤防与心跳过期都会立刻反映到右侧，不会出现左右不一致。</p>
 */
const props = defineProps<{
  point: MonitorPointStatus | null;
  stream: StreamInfo | null;
  loading?: boolean;
}>();

const emit = defineEmits<{ (e: 'retry'): void }>();

const stageRef = ref<HTMLElement | null>(null);
const videoRef = ref<HTMLVideoElement | null>(null);
const state = ref<PlayerVisibleState>('NO_POINT_SELECTED');

const headTitle = computed(() => {
  if (!props.point) return '实时监控';
  return `实时监控 - ${props.point.cameraCode} ${props.point.cameraName}`;
});

/** 综合展示状态：离线 / 未布防 都不出画面 */
const displayStatus = computed(() => (props.point ? computePerimeterDisplayStatus(props.point) : null));

const pointPlayable = computed(() => displayStatus.value === 'NORMAL' || displayStatus.value === 'ABNORMAL');

const showVideo = computed(() => props.stream?.canPlay === true && !!props.stream?.playableUrl);

const stateText = computed(() => {
  if (props.loading) return PLAYER_STATE_TEXT.LOADING;
  return PLAYER_STATE_TEXT[state.value];
});

const placeholderIcon = computed(() => {
  if (state.value === 'OFFLINE') return 'poweroff';
  if (state.value === 'DISARMED') return 'lock-on';
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

/** 由 props 驱动状态机；防区段、流信息或加载态任一变化都要重算 */
watch(
  () => [props.point, props.stream, props.loading] as const,
  () => {
    if (!props.point) {
      state.value = 'NO_POINT_SELECTED';
      return;
    }
    if (!pointPlayable.value) {
      // 未布防与离线要给出不同文案：一个是「人没设防」，一个是「设备掉了」
      state.value = displayStatus.value === 'DISARMED' ? 'DISARMED' : 'OFFLINE';
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
    if (reason === 'POINT_DISARMED') {
      state.value = 'DISARMED';
      return;
    }
    if (reason === 'UNSUPPORTED_PROTOCOL') {
      state.value = 'UNSUPPORTED_PROTOCOL';
      return;
    }
    state.value = 'NO_ADDRESS';
  },
  { immediate: true, deep: true },
);
</script>
<style lang="less" scoped>
.video-monitor {
  display: flex;
  flex-direction: column;
  background-color: var(--td-bg-color-container);
  border-radius: var(--td-radius-medium);
  padding: var(--td-comp-paddingLR-l) var(--td-comp-paddingTB-l);
  height: 100%;
}

.video-monitor__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: var(--td-comp-margin-m);
}

.video-monitor__head-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.video-monitor__head-icon {
  font-size: 18px;
  color: var(--td-brand-color);
  flex-shrink: 0;
}

.video-monitor__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--td-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.video-monitor__stage {
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

.video-monitor__el {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background-color: #000;
}

.video-monitor__placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #a6a6a6;
}

.video-monitor__placeholder-icon {
  opacity: 0.7;
}

.video-monitor__placeholder-text {
  margin: 0;
  font-size: 14px;
}

.video-monitor__foot {
  margin-top: 10px;
}

.video-monitor__foot-text {
  font-size: 12px;
  color: var(--td-text-color-secondary);
}
</style>
