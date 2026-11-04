<template>
  <div class="video-monitor">
    <div class="video-monitor__head">
      <div class="video-monitor__info">
        <span class="video-monitor__door">{{ doorTitle }}</span>
        <span v-if="cameraCode" class="video-monitor__camera">
          摄像头 {{ cameraCode }}
          <t-tag :theme="cameraStatusTheme" variant="light" size="small" class="video-monitor__tag">
            {{ cameraStatusLabel }}
          </t-tag>
        </span>
      </div>
    </div>

    <!--
      P1-2 修复：全屏容器同时包含 <video>、状态覆盖层与退出按钮。
      原实现只有画面区域进入全屏，标题栏与退出按钮留在全屏 DOM 之外，
      进入全屏后无法退出。
    -->
    <div ref="fullscreenRef" class="video-monitor__stage" :class="{ 'video-monitor__stage--fullscreen': isFullscreen }">
      <div ref="containerRef" class="video-monitor__screen">
        <!-- 真实播放器：原生 <video>，一期不引入任何播放器依赖 -->
        <video
          v-if="shouldRenderVideo"
          ref="videoRef"
          class="video-monitor__video"
          :src="playableUrl"
          :poster="undefined"
          autoplay
          muted
          playsinline
          preload="auto"
          @loadeddata="handleLoadedData"
          @canplay="handleCanPlay"
          @playing="handlePlaying"
          @error="handleError"
          @stalled="handleStalled"
        />

        <!-- 状态覆盖层：仅在非播放态展示 -->
        <div v-if="playerState !== 'PLAYING'" class="video-monitor__overlay">
          <template v-if="playerState === 'NO_DOOR_SELECTED'">
            <t-icon name="video" class="video-monitor__overlay-icon" />
            <p class="video-monitor__overlay-text">{{ stateText }}</p>
          </template>

          <template v-else-if="playerState === 'OFFLINE'">
            <t-icon name="poweroff" class="video-monitor__overlay-icon" />
            <p class="video-monitor__overlay-text">{{ stateText }}</p>
            <p v-if="lastHeartbeatAt" class="video-monitor__overlay-sub">最后心跳：{{ formatTime(lastHeartbeatAt) }}</p>
          </template>

          <template v-else-if="playerState === 'NO_ADDRESS'">
            <t-icon name="info-circle" class="video-monitor__overlay-icon" />
            <p class="video-monitor__overlay-text">{{ stateText }}</p>
          </template>

          <template v-else-if="playerState === 'UNSUPPORTED_PROTOCOL'">
            <t-icon name="help-circle" class="video-monitor__overlay-icon" />
            <p class="video-monitor__overlay-text">{{ stateText }}</p>
            <p class="video-monitor__overlay-sub">{{ unsupportedHint }}</p>
          </template>

          <template v-else-if="playerState === 'LOADING'">
            <t-loading size="medium" />
            <p class="video-monitor__overlay-text">{{ stateText }}</p>
          </template>

          <template v-else>
            <!-- ERROR / EXPIRED：提供可点击重播入口（第16.4、31节） -->
            <t-icon name="error-circle" class="video-monitor__overlay-icon" />
            <p class="video-monitor__overlay-text">{{ stateText }}</p>
            <t-button theme="primary" size="small" :disabled="loading" @click="handleRetry">
              {{ playerState === 'EXPIRED' ? '重新获取播放地址' : '重播' }}
            </t-button>
            <p v-if="playerState === 'EXPIRED'" class="video-monitor__overlay-sub">
              播放地址为短时地址，失效后需重新申请
            </p>
          </template>
        </div>
      </div>

      <!-- 全屏按钮与状态标签置于同一容器，按钮与 Esc 均可退出全屏 -->
      <div class="video-monitor__controls">
        <span class="video-monitor__state">{{ stateText }}</span>
        <t-space size="8px">
          <t-button theme="default" variant="outline" size="small" @click="toggleFullscreen">
            <template #icon><t-icon name="fullscreen" /></template>
            {{ isFullscreen ? '退出全屏' : '全屏' }}
          </t-button>
          <t-button
            theme="default"
            variant="outline"
            size="small"
            :disabled="playerState === 'NO_DOOR_SELECTED' || playerState === 'OFFLINE'"
            @click="handleRetry"
          >
            <template #icon><t-icon name="refresh" /></template>
            重试
          </t-button>
        </t-space>
      </div>
    </div>

    <p v-if="expiresAt && playerState === 'PLAYING'" class="video-monitor__footer">
      播放地址将于 {{ formatTime(expiresAt) }} 失效
    </p>
  </div>
</template>
<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

import { DISPLAY_STATUS_THEME } from '../../shared/constants';
import { computeDisplayStatus } from '../../shared/displayStatus';
import type { DisplayStatus } from '../../shared/types';
import { PLAYER_STATE_TEXT, STREAM_UNSUPPORTED_HINT } from '../constants';
import type { DoorStatus, PlayerVisibleState, StreamInfo, StreamProtocol } from '../types';

/**
 * 实时监控组件。
 *
 * 依据 AI_HANDOFF.md：
 * - 第16.4节：必须支持全部可见状态；只允许一路视频活动；
 *              切换仓门、关闭页面或路由离开时必须释放旧播放器和网络连接；
 *              全屏对象是视频容器，支持按钮退出与 Esc 退出
 * - 第22节：未确认协议前禁止安装视频播放器依赖
 * - 第26.1节：MP4 为浏览器原生可播放协议，HLS/FLV 需播放器库
 *
 * <b>本期范围</b>：使用原生 {@code <video>}，支持 MP4 / WebM。
 * HLS / FLV / WebRTC 明确提示「本期不支持」且<b>不新增任何依赖</b>。
 *
 * <b>状态机</b>：
 * NO_ADDRESS（无播放地址）/ LOADING（加载中）/ PLAYING（播放中）/
 * ERROR（播放失败）/ EXPIRED（地址过期）/ OFFLINE（摄像头离线）。
 */
const props = withDefaults(
  defineProps<{
    /** 当前选中的仓门，未选中时为 null */
    door?: DoorStatus | null;
    /** 播放信息，由页面层获取后传入 */
    streamInfo?: StreamInfo | null;
    /** 是否正在获取播放信息 */
    loading?: boolean;
  }>(),
  {
    door: null,
    streamInfo: null,
    loading: false,
  },
);

const emit = defineEmits<{
  (e: 'retry'): void;
  /** 播放地址过期或播放失败，通知上层重新获取（第26.2节） */
  (e: 'refresh'): void;
}>();

/* ---------- DOM 引用 ---------- */

const fullscreenRef = ref<HTMLElement | null>(null);
const containerRef = ref<HTMLElement | null>(null);
const videoRef = ref<HTMLVideoElement | null>(null);

/* ---------- 响应式状态 ---------- */

const isFullscreen = ref(false);

/**
 * 播放器可见状态。
 *
 * 由仓门状态、播放信息与 <video> 元素事件共同决定，
 * 不依赖任何播放器库的黑盒回调。
 */
const playerState = ref<PlayerVisibleState>('NO_DOOR_SELECTED');

/** 定时器句柄：过期检测、播放失败重试退避 */
let expireTimer: ReturnType<typeof setTimeout> | null = null;

/**
 * 是否已因自动播放失败提示过重播，避免无限重试（第31节）。
 *
 * <p>重试上限由状态机保证：自动播放失败后状态置为 ERROR 并停止自动重试，
 * 后续只能由用户点击「重播」或「重试」显式触发（第16.4节）。</p>
 */
let autoPlayFailed = false;

/* ==================== 派生数据 ==================== */

const doorTitle = computed(() => props.door?.doorName ?? '未选择仓门');

const cameraCode = computed(() => props.door?.cameraCode ?? '');

const cameraStatus = computed<DisplayStatus>(() =>
  props.door
    ? computeDisplayStatus({
        deviceStatus: props.door.deviceStatus,
        alarmStatus: props.door.alarmStatus,
        lastHeartbeatAt: props.door.lastHeartbeatAt,
      })
    : 'UNKNOWN',
);
const cameraStatusLabel = computed(() => {
  switch (cameraStatus.value) {
    case 'NORMAL':
      return '在线';
    case 'ABNORMAL':
      return '异常';
    case 'OFFLINE':
      return '离线';
    default:
      return '未知';
  }
});
const cameraStatusTheme = computed(() => DISPLAY_STATUS_THEME[cameraStatus.value]);
const cameraOnline = computed(() => cameraStatus.value === 'NORMAL' || cameraStatus.value === 'ABNORMAL');

const lastHeartbeatAt = computed(() => props.door?.lastHeartbeatAt ?? '');

const expiresAt = computed(() => props.streamInfo?.expiresAt ?? '');

const playableUrl = computed(() => props.streamInfo?.playableUrl ?? '');

const stateText = computed(() => PLAYER_STATE_TEXT[playerState.value]);

/** 不支持协议时的明确说明（第26.1节：一期只支持 MP4/WebM） */
const unsupportedHint = computed(
  () => STREAM_UNSUPPORTED_HINT[(props.streamInfo?.protocol ?? 'MP4') as StreamProtocol] ?? '',
);

/**
 * 浏览器原生可播放的协议白名单。
 * 一期只支持 MP4 / WebM，不引入 HLS/FLV 播放器（第22、26.1节）。
 */
const NATIVE_PROTOCOLS = new Set<StreamProtocol>(['MP4']);

/** 当前协议是否在原生支持范围内 */
function isProtocolSupported(protocol?: string | null): boolean {
  if (!protocol) return false;
  return NATIVE_PROTOCOLS.has(protocol.toUpperCase() as StreamProtocol);
}

/** 是否渲染真实 <video> 元素：仅在有地址、协议受支持且仓门在线时渲染 */
const shouldRenderVideo = computed(
  () =>
    !!playableUrl.value &&
    isProtocolSupported(props.streamInfo?.protocol) &&
    cameraOnline.value &&
    // ERROR 状态下保留元素以便用户点击重播
    playerState.value !== 'EXPIRED' &&
    playerState.value !== 'NO_ADDRESS',
);

/* ==================== 状态推导 ==================== */

/**
 * 由仓门与播放信息推导「元素之外」的状态。
 *
 * <p>依赖 <video> 事件的 LOADING / PLAYING / ERROR / EXPIRED
 * 不在此处理，避免与事件回调互相覆盖。</p>
 */
function deriveBaseState(): PlayerVisibleState {
  if (!props.door) return 'NO_DOOR_SELECTED';

  // 设备离线（第16.4节：仓门或摄像头离线）
  if (!cameraOnline.value) return 'OFFLINE';

  // 正在获取播放信息
  if (props.loading) return 'LOADING';

  const info = props.streamInfo;
  if (!info) return 'LOADING';

  // 后端明确告知不可播放
  if (!info.canPlay) {
    return info.unavailableReason === 'CAMERA_OFFLINE' ? 'OFFLINE' : 'NO_ADDRESS';
  }

  // 无播放地址（第31节：无播放地址应明确提示）
  if (!info.playableUrl) return 'NO_ADDRESS';

  // 协议不在一期原生支持范围
  if (!isProtocolSupported(info.protocol)) return 'UNSUPPORTED_PROTOCOL';

  return 'LOADING';
}

/** 同步基础状态（不覆盖由 video 事件驱动的状态） */
function syncBaseState() {
  const base = deriveBaseState();
  const current = playerState.value;

  // 已进入播放相关状态时，只有仓门/协议/地址等前提变化才允许覆盖
  if (current === 'PLAYING' && base === 'LOADING') return;
  if (current === 'ERROR' && base === 'LOADING') return;

  playerState.value = base;
}

/* ==================== video 事件 ==================== */

function handleLoadedData() {
  // 已具备首帧数据
  if (playerState.value === 'LOADING') {
    playerState.value = 'PLAYING';
  }
}

function handleCanPlay() {
  if (playerState.value === 'LOADING') {
    playerState.value = 'PLAYING';
  }
}

function handlePlaying() {
  autoPlayFailed = false;
  playerState.value = 'PLAYING';
}

function handleStalled() {
  // 缓冲停滞不立即判定为失败，交由 canplay/error 决定
  if (playerState.value === 'PLAYING') {
    playerState.value = 'LOADING';
  }
}

/**
 * 播放失败。
 *
 * <p>第31节：连续播放失败时停止自动重试，允许人工重试。
 * 这里置为 ERROR 并交由用户点击重播，<b>不做无限自动重试</b>。</p>
 */
function handleError() {
  playerState.value = 'ERROR';
  autoPlayFailed = true;
  releaseVideoElement();
}

/**
 * 自动播放失败兜底。
 *
 * <p>浏览器可能因用户手势策略拒绝 autoplay，此时不会触发 error，
 * 但视频始终停在 LOADING。此处用一次延时检测给出可点击重播，
 * <b>重试次数有上限</b>，不会无限循环。</p>
 */
let autoplayCheckTimer: ReturnType<typeof setTimeout> | null = null;

function scheduleAutoplayCheck() {
  clearAutoplayCheckTimer();
  autoplayCheckTimer = setTimeout(() => {
    autoplayCheckTimer = null;
    const video = videoRef.value;
    if (!video) return;
    if (playerState.value !== 'LOADING') return;
    // 已经能播放则不算失败
    if (video.readyState >= HTMLMediaElement.HAVE_CURRENT_DATA) {
      playerState.value = 'PLAYING';
      return;
    }
    if (!autoPlayFailed) {
      autoPlayFailed = true;
      playerState.value = 'ERROR';
    }
  }, 3000);
}

function clearAutoplayCheckTimer() {
  if (autoplayCheckTimer) {
    clearTimeout(autoplayCheckTimer);
    autoplayCheckTimer = null;
  }
}

/* ==================== 资源释放 ==================== */

/**
 * 释放播放器与网络连接（第16.4、30节）。
 *
 * <p>真实 <video> 必须显式 pause 并清空 src，
 * 否则切换仓门后旧流仍可能继续下载。</p>
 */
function releaseVideoElement() {
  const video = videoRef.value;
  if (video) {
    try {
      video.pause();
      // 置空 src 并调用 load() 才会真正中断网络请求
      video.removeAttribute('src');
      video.load();
    } catch {
      // 忽略释放异常，卸载流程不因播放器异常中断
    }
  }
}

/** 清理全部定时器（切仓门与卸载都必须调用） */
function clearAllTimers() {
  if (expireTimer) {
    clearTimeout(expireTimer);
    expireTimer = null;
  }
  clearAutoplayCheckTimer();
}

/* ==================== 过期处理（P1-3） ==================== */

/**
 * 按 expiresAt 安排**单次**过期定时器。
 *
 * <p>P1-3 修复：此前过期判断依赖 Date.now() 但没有定时器驱动，
 * 只监听属性变化，自然流逝不会触发。现在按到期时间精确调度一次。</p>
 *
 * <p>到期后转为 EXPIRED 并通知上层重新申请地址；
 * <b>不无限重试</b>（第31节）。</p>
 */
function scheduleExpiry(expiresAtValue?: string | null) {
  // 先清理上一次的单次定时器，避免多个过期回调叠加
  if (expireTimer) {
    clearTimeout(expireTimer);
    expireTimer = null;
  }

  if (!expiresAtValue) return;

  const ts = new Date(expiresAtValue).getTime();
  if (Number.isNaN(ts)) return;

  const remain = ts - Date.now();
  if (remain <= 0) {
    playerState.value = 'EXPIRED';
    emit('refresh');
    return;
  }

  expireTimer = setTimeout(() => {
    expireTimer = null;
    playerState.value = 'EXPIRED';
    releaseVideoElement();
    emit('refresh');
  }, remain);
}

/* ==================== 重试 ==================== */

function handleRetry() {
  const state = playerState.value;

  // 播放失败：优先在当前地址上重播一次，不重复请求
  if (state === 'ERROR' && playableUrl.value) {
    autoPlayFailed = false;
    playerState.value = 'LOADING';
    const video = videoRef.value;
    if (video) {
      void video.play().catch(() => {
        playerState.value = 'ERROR';
      });
    }
    return;
  }

  // 其余情况（过期、取址失败等）向上层请求新的播放地址
  emit('retry');
  emit('refresh');
}

/* ==================== 时间格式 ==================== */

function formatTime(value: string): string {
  if (!value) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}

/* ==================== 全屏 ==================== */

async function enterFullscreen() {
  const el = fullscreenRef.value;
  if (!el) return;
  try {
    await el.requestFullscreen();
    isFullscreen.value = true;
  } catch {
    // 浏览器不支持时保持原状，不影响页面
    isFullscreen.value = false;
  }
}

async function exitFullscreen() {
  try {
    if (document.fullscreenElement) {
      await document.exitFullscreen();
    }
  } catch {
    // 忽略退出异常
  }
  isFullscreen.value = false;
}

function toggleFullscreen() {
  if (isFullscreen.value) {
    void exitFullscreen();
  } else {
    void enterFullscreen();
  }
}

/** Esc 退出全屏（第16.4节） */
function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && isFullscreen.value) {
    void exitFullscreen();
  }
}

/** 监听浏览器全屏状态变化，保持按钮文案同步（含用户按 Esc 的情况） */
function handleFullscreenChange() {
  isFullscreen.value = document.fullscreenElement === fullscreenRef.value;
}

/* ==================== 生命周期 ==================== */

onMounted(() => {
  window.addEventListener('keydown', handleKeydown);
  document.addEventListener('fullscreenchange', handleFullscreenChange);
  syncBaseState();
  scheduleAutoplayCheck();
});

onBeforeUnmount(() => {
  // 第16.4、30节：关闭页面或路由离开时必须释放播放器和网络连接
  clearAllTimers();
  releaseVideoElement();
  void exitFullscreen();
  window.removeEventListener('keydown', handleKeydown);
  document.removeEventListener('fullscreenchange', handleFullscreenChange);
});

/**
 * 切换仓门：释放上一路播放并清理定时器。
 * 第16.4节：只允许当前选中的一路视频处于活动状态。
 */
watch(
  () => props.door?.doorId,
  () => {
    clearAllTimers();
    releaseVideoElement();
    autoPlayFailed = false;
    playerState.value = deriveBaseState();
    scheduleAutoplayCheck();
  },
);

/** 播放信息变化：重新推导状态并重新安排过期定时器 */
watch(
  () => props.streamInfo,
  () => {
    syncBaseState();
    scheduleExpiry(props.streamInfo?.expiresAt);
    if (shouldRenderVideo.value) {
      scheduleAutoplayCheck();
    }
  },
);

/** 加载态变化 */
watch(
  () => props.loading,
  () => {
    syncBaseState();
  },
);
</script>
<style lang="less" scoped>
.video-monitor {
  display: flex;
  flex-direction: column;
  background-color: var(--td-bg-color-container);
  border-radius: var(--td-radius-medium);
  padding: var(--td-comp-paddingLR-l) var(--td-comp-paddingTB-l);
  min-height: 0;
}

/*
 * 全屏容器：包含视频画面、状态覆盖层与控制按钮。
 * P1-2 修复后，退出按钮不再位于全屏 DOM 之外。
 */
.video-monitor__stage {
  display: flex;
  flex-direction: column;
  gap: var(--td-comp-margin-s);
  min-height: 0;
  background-color: var(--td-bg-color-container);
  border-radius: var(--td-radius-medium);
}

.video-monitor__stage--fullscreen {
  background-color: #1b1f27;
  padding: 12px;
}

.video-monitor__stage:fullscreen {
  width: 100vw;
  height: 100vh;
  justify-content: center;
}

/*
 * 视频区域高度使用 clamp() 随视口自适应，
 * 避免 1280×720 下固定高度 + overflow:hidden 造成内容裁切（P1-9）。
 * 不通过缩小字号来掩盖布局不足。
 */
.video-monitor__screen {
  position: relative;
  width: 100%;
  height: clamp(200px, 34vh, 420px);
  background-color: #1b1f27;
  border-radius: var(--td-radius-medium);
  overflow: hidden;
}

.video-monitor__stage:fullscreen .video-monitor__screen {
  height: auto;
  flex: 1;
  min-height: 0;
}

.video-monitor__video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background-color: #000;
}

.video-monitor__overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #fff;
  text-align: center;
  padding: 16px;
}

.video-monitor__overlay-icon {
  font-size: 36px;
  color: rgba(255, 255, 255, 0.65);
}

.video-monitor__overlay-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0;
}

.video-monitor__overlay-sub {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
  margin: 0;
}

/* 控制栏：与视频同处一个全屏容器内 */
.video-monitor__controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
}

.video-monitor__state {
  font-size: 12px;
  color: var(--td-text-color-secondary);
}

.video-monitor__stage:fullscreen .video-monitor__controls {
  color: #fff;
}

.video-monitor__stage:fullscreen .video-monitor__state {
  color: rgba(255, 255, 255, 0.85);
}

.video-monitor__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--td-comp-margin-m);
  gap: 12px;
  flex-shrink: 0;
}

.video-monitor__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.video-monitor__door {
  font-size: 15px;
  font-weight: 600;
  color: var(--td-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.video-monitor__camera {
  font-size: 12px;
  color: var(--td-text-color-secondary);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.video-monitor__tag {
  margin-left: 2px;
}

.video-monitor__footer {
  margin: var(--td-comp-margin-s) 0 0;
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}
</style>
