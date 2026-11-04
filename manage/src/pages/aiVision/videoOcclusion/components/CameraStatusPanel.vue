<template>
  <div class="camera-panel">
    <div class="camera-panel__head">
      <span class="camera-panel__title">摄像头状态</span>
      <span class="camera-panel__legend">
        <span class="camera-panel__legend-item"> <i class="camera-panel__dot camera-panel__dot--NORMAL" />正常 </span>
        <span class="camera-panel__legend-item"> <i class="camera-panel__dot camera-panel__dot--ABNORMAL" />遮挡 </span>
        <span class="camera-panel__legend-item"> <i class="camera-panel__dot camera-panel__dot--OFFLINE" />离线 </span>
      </span>
    </div>

    <t-empty v-if="isEmpty" description="暂无可监测摄像头" />

    <div v-else class="camera-panel__grid" :class="{ 'camera-panel__grid--loading': loading }">
      <button
        v-for="camera in cameras"
        :key="camera.cameraId"
        type="button"
        class="camera-card"
        :class="[
          `camera-card--${displayStatusOf(camera)}`,
          { 'camera-card--selected': camera.cameraId === modelValue },
        ]"
        :aria-pressed="camera.cameraId === modelValue"
        @click="handleSelect(camera)"
      >
        <span v-if="camera.cameraId === modelValue" class="camera-card__badge">当前</span>

        <span class="camera-card__code">{{ camera.cameraCode }}</span>
        <span class="camera-card__name" :title="camera.cameraName">{{ camera.cameraName }}</span>

        <span class="camera-card__meta">
          <t-icon name="video-camera" class="camera-card__icon" />
          <span class="camera-card__status">{{ statusTextOf(camera) }}</span>
        </span>

        <!-- 在线状态的呼吸灯，直观区分「在监测」与「已掉线」 -->
        <span v-if="displayStatusOf(camera) === 'NORMAL'" class="camera-card__pulse" aria-hidden="true" />
      </button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue';

import { computeDisplayStatus, connectionStatusLabel } from '../../shared/displayStatus';
import type { DisplayStatus } from '../../shared/types';
import { DISPLAY_STATUS_LABEL } from '../constants';
import type { CameraStatus } from '../types';

/**
 * 摄像头状态面板。
 *
 * 展示状态由共享的 computeDisplayStatus 计算，不依赖后端单一字符串。
 * 选中态用独立边框 + 角标表达，不改变语义色，避免「遮挡」被选中时看起来像恢复正常。
 */
const props = withDefaults(
  defineProps<{
    cameras: CameraStatus[];
    modelValue?: string;
    loading?: boolean;
    isEmpty?: boolean;
  }>(),
  {
    modelValue: '',
    loading: false,
    isEmpty: false,
  },
);

const emit = defineEmits<{
  (e: 'update:modelValue', cameraId: string): void;
  (e: 'change', camera: CameraStatus): void;
}>();

function displayStatusOf(camera: CameraStatus): DisplayStatus {
  return computeDisplayStatus({
    deviceStatus: camera.deviceStatus,
    alarmStatus: camera.occlusionStatus,
    lastHeartbeatAt: camera.lastHeartbeatAt,
  });
}

function statusTextOf(camera: CameraStatus): string {
  const connection = connectionStatusLabel(camera);
  return displayStatusOf(camera) === 'ABNORMAL' ? `${connection} · ${DISPLAY_STATUS_LABEL.ABNORMAL}` : connection;
}

function handleSelect(camera: CameraStatus) {
  emit('update:modelValue', camera.cameraId);
  emit('change', camera);
}

const occludedCount = computed(() => props.cameras.filter((c) => displayStatusOf(c) === 'ABNORMAL').length);

defineExpose({ occludedCount });
</script>
<style lang="less" scoped>
.camera-panel {
  display: flex;
  flex-direction: column;
  background-color: var(--td-bg-color-container);
  border-radius: var(--td-radius-medium);
  padding: var(--td-comp-paddingLR-l) var(--td-comp-paddingTB-l);
}

.camera-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: var(--td-comp-margin-m);
  flex-wrap: wrap;
}

.camera-panel__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--td-text-color-primary);
}

.camera-panel__legend {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--td-text-color-secondary);
}

.camera-panel__legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.camera-panel__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.camera-panel__dot--NORMAL {
  background-color: #00a870;
}
.camera-panel__dot--ABNORMAL {
  background-color: #d54941;
}
.camera-panel__dot--OFFLINE {
  background-color: #86909c;
}

/* 3 列网格，窄容器自动降到 2 列 */
.camera-panel__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(96px, 1fr));
  gap: 10px;
  align-content: start;
}

@media (max-width: 420px) {
  .camera-panel__grid {
    grid-template-columns: repeat(2, minmax(96px, 1fr));
  }
}

.camera-panel__grid--loading {
  opacity: 0.6;
  pointer-events: none;
}

.camera-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 10px;
  /* 预留 2px 透明边框占位，选中时只改色不改动尺寸 */
  border: 2px solid transparent;
  border-radius: var(--td-radius-medium);
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition:
    box-shadow 0.2s,
    transform 0.2s,
    border-color 0.2s;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }

  &:focus-visible {
    outline: 2px solid var(--td-brand-color);
    outline-offset: 2px;
  }
}

.camera-card--NORMAL {
  background-color: #e8f8f0;
  .camera-card__icon,
  .camera-card__status {
    color: #00a870;
  }
  .camera-card__code {
    color: #0a3d2c;
  }
  .camera-card__name {
    color: #2f6b55;
  }
}

.camera-card--ABNORMAL {
  background-color: #fdeeee;
  .camera-card__icon,
  .camera-card__status {
    color: #d54941;
  }
  .camera-card__code {
    color: #5c1f1c;
  }
  .camera-card__name {
    color: #94514c;
  }
}

.camera-card--OFFLINE {
  background-color: #f2f3f5;
  .camera-card__icon,
  .camera-card__status {
    color: #86909c;
  }
  .camera-card__code {
    color: #4e5969;
  }
  .camera-card__name {
    color: #86909c;
  }
}

/* 未知：虚线边框，与「明确离线」在视觉上区分开 */
.camera-card--UNKNOWN {
  background-color: #f8fafc;
  border: 2px dashed #c9cdd4;
  .camera-card__icon,
  .camera-card__status,
  .camera-card__code,
  .camera-card__name {
    color: #86909c;
  }
}

.camera-card--selected {
  border-color: var(--td-brand-color);
  box-shadow: 0 0 0 3px rgba(0, 82, 217, 0.12);
}

.camera-card__badge {
  position: absolute;
  top: -1px;
  right: -1px;
  padding: 2px 8px;
  font-size: 11px;
  line-height: 16px;
  color: #fff;
  background-color: var(--td-brand-color);
  border-radius: 0 var(--td-radius-medium) 0 var(--td-radius-medium);
  z-index: 1;
}

.camera-card__code {
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
}

.camera-card__name {
  font-size: 12px;
  line-height: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}

.camera-card__meta {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
}

.camera-card__icon {
  font-size: 16px;
  flex-shrink: 0;
  opacity: 0.9;
}

.camera-card__status {
  font-size: 12px;
  line-height: 16px;
}

.camera-card__pulse {
  position: absolute;
  bottom: 8px;
  right: 8px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #00a870;
  animation: camera-pulse 2s ease-in-out infinite;
}

@keyframes camera-pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(1.5);
  }
}

/* 状态图标置顶，按原型使用语义色；在线/离线文字保持可读。 */
.camera-card {
  align-items: center;
  text-align: center;
}

.camera-card__meta {
  display: contents;
}

.camera-card__icon {
  order: -1;
  align-self: center;
  margin-bottom: 4px;
  font-size: 24px;
  opacity: 1;
}

.camera-card__status {
  order: 2;
  font-weight: 600;
}

.camera-card__name,
.camera-card__code,
.camera-card__title {
  width: 100%;
  text-align: center;
}
</style>
