<template>
  <div class="point-panel">
    <div class="point-panel__head">
      <span class="point-panel__title">人员监测区域分布</span>
      <span class="point-panel__legend">
        <span class="point-panel__legend-item"> <i class="point-panel__dot point-panel__dot--NORMAL" />正常 </span>
        <span class="point-panel__legend-item"> <i class="point-panel__dot point-panel__dot--ABNORMAL" />异常 </span>
        <span class="point-panel__legend-item"> <i class="point-panel__dot point-panel__dot--OFFLINE" />离线 </span>
      </span>
    </div>

    <t-empty v-if="isEmpty" description="暂无可监测区域" />

    <div v-else class="point-panel__grid" :class="{ 'point-panel__grid--loading': loading }">
      <button
        v-for="point in points"
        :key="point.pointId"
        type="button"
        class="point-card"
        :class="[`point-card--${cardVariantOf(point)}`, { 'point-card--selected': point.pointId === modelValue }]"
        :aria-pressed="point.pointId === modelValue"
        @click="handleSelect(point)"
      >
        <span v-if="point.pointId === modelValue" class="point-card__badge">当前</span>

        <span class="point-card__name" :title="point.pointName">{{ point.pointName }}</span>
        <span class="point-card__code">{{ point.cameraCode || point.pointCode }}</span>

        <span class="point-card__meta">
          <t-icon name="usergroup" class="point-card__icon" />
          <span class="point-card__status">{{ statusLabelOf(point) }}</span>
        </span>

        <!-- 在线状态的呼吸灯，直观区分「在监测」与「已掉线」 -->
        <span v-if="displayStatusOf(point) === 'NORMAL'" class="point-card__pulse" aria-hidden="true" />
      </button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue';

import { computeDisplayStatus, connectionStatusLabel } from '../../shared/displayStatus';
import type { DisplayStatus } from '../../shared/types';
import { BEHAVIOR_TYPE_CARD_CLASS, BEHAVIOR_TYPE_LABEL, DISPLAY_STATUS_LABEL } from '../constants';
import type { MonitorPoint } from '../types';

/**
 * 监测区域状态面板。
 *
 * 展示状态由共享的 computeDisplayStatus 计算，不依赖后端单一字符串。
 * 选中态用独立边框 + 角标表达，不改变语义色，避免「人员聚集」被选中时看起来像恢复正常。
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

function displayStatusOf(point: MonitorPoint): DisplayStatus {
  return computeDisplayStatus({
    deviceStatus: point.deviceStatus,
    alarmStatus: point.alarmStatus,
    lastHeartbeatAt: point.lastHeartbeatAt,
  });
}

/**
 * 异常卡片的文案。
 *
 * 共享基座只给一个 ABNORMAL，原型却要按行为类型显示「人员聚集」「未戴安全帽」，
 * 因此优先取后端回传的 alarmBehaviorType 还原，取不到才退回兜底文案。
 */
function statusLabelOf(point: MonitorPoint): string {
  const status = displayStatusOf(point);
  const connection = connectionStatusLabel(point);
  if (status === 'ABNORMAL') {
    const behavior = point.alarmBehaviorType;
    return `${connection} · ${(behavior && BEHAVIOR_TYPE_LABEL[behavior]) || DISPLAY_STATUS_LABEL.ABNORMAL}`;
  }
  return connection;
}

/**
 * 卡片配色档位。
 *
 * 原型的「人员聚集」是橙色、「未戴安全帽」是红色，两者同为 ABNORMAL，
 * 基座给不了这个区分，只能在功能内按行为类型再分一档。
 */
function cardVariantOf(point: MonitorPoint): string {
  const status = displayStatusOf(point);
  if (status !== 'ABNORMAL') return status;
  return point.alarmBehaviorType ? BEHAVIOR_TYPE_CARD_CLASS[point.alarmBehaviorType] : 'ABNORMAL';
}

function handleSelect(point: MonitorPoint) {
  emit('update:modelValue', point.pointId);
  emit('change', point);
}

const alarmingCount = computed(() => props.points.filter((p) => displayStatusOf(p) === 'ABNORMAL').length);

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

/* 窄容器里按可用宽度自动排布，原型是 4 列，容器变窄时自动降到 3/2 列 */
.point-panel__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(104px, 1fr));
  gap: 10px;
  align-content: start;
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

/* 人员聚集：橙色，与「未戴安全帽」的红色在原型上是两档 */
.point-card--CROWD {
  background-color: #fff3e0;
  .point-card__icon,
  .point-card__status {
    color: #e37318;
  }
  .point-card__name {
    color: #5c3a12;
  }
  .point-card__code {
    color: #946234;
  }
}

/* 未戴安全帽 / 倒地 / 区域闯入：红色 */
.point-card--NO_HELMET {
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

/* 兜底：告警中但拿不到行为类型，仍按异常红显示 */
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

/* 未知：虚线边框，与「明确离线」在视觉上区分开 */
.point-card--UNKNOWN {
  background-color: #f8fafc;
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
  font-size: 13px;
  font-weight: 600;
  line-height: 18px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}

.point-card__code {
  font-size: 11px;
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
