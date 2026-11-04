<template>
  <div class="point-panel">
    <div class="point-panel__head">
      <span class="point-panel__title">皮带机情况概况</span>
      <span class="point-panel__legend">
        <span class="point-panel__legend-item"> <i class="point-panel__dot point-panel__dot--NORMAL" />正常 </span>
        <span class="point-panel__legend-item"> <i class="point-panel__dot point-panel__dot--ABNORMAL" />报警 </span>
        <span class="point-panel__legend-item"> <i class="point-panel__dot point-panel__dot--OFFLINE" />离线 </span>
        <span class="point-panel__legend-item">
          <i class="point-panel__dot point-panel__dot--NOT_DEPLOYED" />未部署
        </span>
      </span>
    </div>

    <t-empty v-if="isEmpty" description="暂无可监测皮带机" />

    <div v-else class="point-panel__grid" :class="{ 'point-panel__grid--loading': loading }">
      <button
        v-for="point in points"
        :key="point.pointId"
        type="button"
        class="point-card"
        :class="[`point-card--${cardStatusOf(point)}`, { 'point-card--selected': point.pointId === modelValue }]"
        :aria-pressed="point.pointId === modelValue"
        @click="handleSelect(point)"
      >
        <span v-if="point.pointId === modelValue" class="point-card__badge">当前</span>

        <span class="point-card__name" :title="point.deviceName">{{ point.deviceName }}</span>
        <span class="point-card__code">{{ point.deviceCode }}</span>

        <span class="point-card__meta">
          <t-icon :name="iconOf(point)" class="point-card__icon" />
          <span class="point-card__status">{{ labelOf(point) }}</span>
        </span>

        <!-- 在线状态的呼吸灯，直观区分「在监测」与「已掉线」 -->
        <span v-if="cardStatusOf(point) === 'NORMAL'" class="point-card__pulse" aria-hidden="true" />
      </button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue';

import { connectionStatusLabel } from '../../shared/displayStatus';
import type { BeltCardStatus } from '../constants';
import { pointDisplayLabel, pointDisplayStatus } from '../constants';
import type { MonitorPoint } from '../types';

/**
 * 皮带机情况概况面板。
 *
 * 展示状态由共享的 computeDisplayStatus 计算，不依赖后端单一字符串；
 * 「未部署」是共享层没有的一档（UNKNOWN 且未绑定摄像头），
 * 由本功能的 pointDisplayStatus 拆出来，见 constants 里的注释。
 *
 * 选中态用独立边框 + 角标表达，不改变语义色，避免「报警」被选中时看起来像恢复正常。
 */
const props = withDefaults(
  defineProps<{
    points: MonitorPoint[];
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
  (e: 'update:modelValue', pointId: string): void;
  (e: 'change', point: MonitorPoint): void;
}>();

function cardStatusOf(point: MonitorPoint): BeltCardStatus {
  return pointDisplayStatus(point);
}

function labelOf(point: MonitorPoint): string {
  const status = cardStatusOf(point);
  if (status === 'NOT_DEPLOYED') return pointDisplayLabel(point);
  const connection = connectionStatusLabel(point);
  return status === 'ABNORMAL' ? `${connection} · ${pointDisplayLabel(point)}` : connection;
}

/** 共享层没有 NOT_DEPLOYED 这一档的图标，这里补一个语义更准的「禁止接入」图标 */
function iconOf(point: MonitorPoint): string {
  return cardStatusOf(point) === 'NOT_DEPLOYED' ? 'disable' : 'tools';
}

function handleSelect(point: MonitorPoint) {
  emit('update:modelValue', point.pointId);
  emit('change', point);
}

const alarmingCount = computed(() => props.points.filter((p) => cardStatusOf(p) === 'ABNORMAL').length);

defineExpose({ alarmingCount });
</script>
<style lang="less" scoped>
.point-panel {
  display: flex;
  flex-direction: column;
  background-color: var(--td-bg-color-container);
  border-radius: var(--td-radius-medium);
  padding: var(--td-comp-paddingLR-l) var(--td-comp-paddingTB-l);
}

.point-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: var(--td-comp-margin-m);
  flex-wrap: wrap;
}

.point-panel__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--td-text-color-primary);
}

.point-panel__legend {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--td-text-color-secondary);
}

.point-panel__legend-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.point-panel__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.point-panel__dot--NORMAL {
  background-color: #00a870;
}
.point-panel__dot--ABNORMAL {
  background-color: #d54941;
}
.point-panel__dot--OFFLINE {
  background-color: #86909c;
}
.point-panel__dot--NOT_DEPLOYED {
  background-color: #c9cdd4;
}

/* 3 列网格，窄容器自动降到 2 列 */
.point-panel__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(96px, 1fr));
  gap: 10px;
  align-content: start;
}

@media (max-width: 420px) {
  .point-panel__grid {
    grid-template-columns: repeat(2, minmax(96px, 1fr));
  }
}

.point-panel__grid--loading {
  opacity: 0.6;
  pointer-events: none;
}

.point-card {
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

.point-card--NORMAL {
  background-color: #e8f8f0;
  .point-card__icon,
  .point-card__status {
    color: #00a870;
  }
  .point-card__name {
    color: #0a3d2c;
  }
  .point-card__code {
    color: #2f6b55;
  }
}

.point-card--ABNORMAL {
  background-color: #fdeeee;
  .point-card__icon,
  .point-card__status {
    color: #d54941;
  }
  .point-card__name {
    color: #5c1f1c;
  }
  .point-card__code {
    color: #94514c;
  }
}

.point-card--OFFLINE {
  background-color: #f2f3f5;
  .point-card__icon,
  .point-card__status {
    color: #86909c;
  }
  .point-card__name {
    color: #4e5969;
  }
  .point-card__code {
    color: #86909c;
  }
}

/* 未部署：虚线边框，与「明确离线」在视觉上区分开 */
.point-card--NOT_DEPLOYED {
  background-color: #f8fafc;
  border: 2px dashed #c9cdd4;
  .point-card__icon,
  .point-card__status,
  .point-card__name,
  .point-card__code {
    color: #86909c;
  }
}

/* 绑了摄像头但设备态判不出来：与未部署同为虚线，但底色更灰 */
.point-card--UNKNOWN {
  background-color: #f5f6f7;
  border: 2px dashed #c9cdd4;
  .point-card__icon,
  .point-card__status,
  .point-card__name,
  .point-card__code {
    color: #86909c;
  }
}

.point-card--selected {
  border-color: var(--td-brand-color);
  box-shadow: 0 0 0 3px rgba(0, 82, 217, 0.12);
}

.point-card__badge {
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

.point-card__name {
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}

.point-card__code {
  font-size: 12px;
  line-height: 16px;
}

.point-card__meta {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
}

.point-card__icon {
  font-size: 16px;
  flex-shrink: 0;
  opacity: 0.9;
}

.point-card__status {
  font-size: 12px;
  line-height: 16px;
}

.point-card__pulse {
  position: absolute;
  bottom: 8px;
  right: 8px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #00a870;
  animation: point-pulse 2s ease-in-out infinite;
}

@keyframes point-pulse {
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
.point-card {
  align-items: center;
  text-align: center;
}

.point-card__meta {
  display: contents;
}

.point-card__icon {
  order: -1;
  align-self: center;
  margin-bottom: 4px;
  font-size: 24px;
  opacity: 1;
}

.point-card__status {
  order: 2;
  font-weight: 600;
}

.point-card__name,
.point-card__code,
.point-card__title {
  width: 100%;
  text-align: center;
}
</style>
