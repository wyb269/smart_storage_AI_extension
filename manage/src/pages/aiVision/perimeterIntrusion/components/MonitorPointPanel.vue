<template>
  <div class="point-panel">
    <div class="point-panel__head">
      <span class="point-panel__title">四面防区状态</span>
      <t-select
        :value="zone"
        class="point-panel__filter"
        size="small"
        :options="ZONE_OPTIONS"
        @change="handleZoneChange"
      />
    </div>

    <div class="point-panel__legend">
      <span v-for="item in LEGEND" :key="item.status" class="point-panel__legend-item">
        <i class="point-panel__dot" :style="{ backgroundColor: DISPLAY_STATUS_COLOR[item.status] }" />
        {{ item.label }}
      </span>
    </div>

    <t-empty v-if="isEmpty" description="暂无可监测防区段" />

    <div v-else class="point-panel__grid" :class="{ 'point-panel__grid--loading': loading }">
      <button
        v-for="point in points"
        :key="point.pointId"
        type="button"
        class="point-card"
        :class="[`point-card--${statusOf(point)}`, { 'point-card--selected': point.pointId === modelValue }]"
        :aria-pressed="point.pointId === modelValue"
        @click="handleSelect(point)"
      >
        <span v-if="point.pointId === modelValue" class="point-card__badge">当前</span>

        <span class="point-card__name" :title="point.pointName">{{ point.pointName }}</span>
        <span class="point-card__code">{{ point.cameraCode }} {{ point.cameraName }}</span>

        <span class="point-card__meta">
          <t-icon name="secured" class="point-card__icon" />
          <span class="point-card__status">{{ statusTextOf(point) }}</span>
        </span>

        <!-- 布防开关单独一行：撤防是风险信息，不能只靠状态文案表达 -->
        <span class="point-card__defense" :class="{ 'point-card__defense--off': point.defenseStatus === 'DISARMED' }">
          <t-icon :name="point.defenseStatus === 'DISARMED' ? 'lock-on' : 'lock-off'" />
          {{ DEFENSE_STATUS_LABEL[point.defenseStatus] }}
        </span>

        <!-- 在线且已布防状态的呼吸灯，直观区分「在监测」与「已掉线/已撤防」 -->
        <span v-if="statusOf(point) === 'NORMAL'" class="point-card__pulse" aria-hidden="true" />
      </button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue';

import { connectionStatusLabel } from '../../shared/displayStatus';
import { computePerimeterDisplayStatus } from '../composables/usePerimeterDisplayStatus';
import { DEFENSE_STATUS_LABEL, DISPLAY_STATUS_COLOR, DISPLAY_STATUS_LABEL, ZONE_OPTIONS } from '../constants';
import type { MonitorPointStatus, PerimeterDisplayStatus, Zone } from '../types';

/**
 * 防区段状态面板。
 *
 * 展示状态由本功能目录内的 computePerimeterDisplayStatus 计算
 * （内部先判布防、再回落共享层），不依赖后端单一字符串。
 * 选中态用独立边框 + 角标表达，不改变语义色，避免「入侵报警」被选中时
 * 看起来像恢复正常。
 *
 * 防区下拉只过滤本组件渲染的 points（父级传入的已是过滤后列表），
 * 不触发任何请求，因此切换筛选不会打断右侧画面。
 */
const props = withDefaults(
  defineProps<{
    points: MonitorPointStatus[];
    modelValue?: string;
    loading?: boolean;
    isEmpty?: boolean;
    zone?: Zone | '';
  }>(),
  {
    modelValue: '',
    loading: false,
    isEmpty: false,
    zone: '',
  },
);

const emit = defineEmits<{
  (e: 'update:modelValue', pointId: string): void;
  (e: 'change', point: MonitorPointStatus): void;
  (e: 'update:zone', zone: Zone | ''): void;
}>();

/** 图例只列会出现的四类，未知不单列（卡片有虚线框可辨） */
const LEGEND: Array<{ status: PerimeterDisplayStatus; label: string }> = [
  { status: 'NORMAL', label: '正常布防' },
  { status: 'ABNORMAL', label: '入侵报警' },
  { status: 'DISARMED', label: '未布防' },
  { status: 'OFFLINE', label: '离线' },
];

function statusOf(point: MonitorPointStatus): PerimeterDisplayStatus {
  return computePerimeterDisplayStatus(point);
}

function statusTextOf(point: MonitorPointStatus): string {
  const connection = connectionStatusLabel(point);
  const status = statusOf(point);
  return status === 'ABNORMAL' || status === 'DISARMED'
    ? `${connection} · ${DISPLAY_STATUS_LABEL[status]}`
    : connection;
}

function handleSelect(point: MonitorPointStatus) {
  emit('update:modelValue', point.pointId);
  emit('change', point);
}

// t-select 回传的是 SelectValue（可能是对象/数组），这里只认防区编码字符串
function handleZoneChange(value: unknown) {
  emit('update:zone', typeof value === 'string' ? (value as Zone | '') : '');
}

/** 处于入侵报警的段数，供父级做提示 */
const alarmCount = computed(() => props.points.filter((p) => statusOf(p) === 'ABNORMAL').length);

defineExpose({ alarmCount });
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
  flex-wrap: wrap;
}

.point-panel__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--td-text-color-primary);
}

.point-panel__filter {
  width: 110px;
  flex-shrink: 0;
}

.point-panel__legend {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 8px 0 var(--td-comp-margin-m);
  font-size: 12px;
  color: var(--td-text-color-secondary);
  flex-wrap: wrap;
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
  color: var(--td-text-color-placeholder);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.point-card__defense {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  line-height: 16px;
  color: var(--td-text-color-placeholder);
}

.point-card__defense--off {
  color: #e37318;
}

/* 正常布防 */
.point-card--NORMAL {
  background-color: #e8f8f0;
  .point-card__icon,
  .point-card__status {
    color: #00a870;
  }
  .point-card__name {
    color: #0a3d2c;
  }
}

/* 入侵报警 */
.point-card--ABNORMAL {
  background-color: #fdeeee;
  .point-card__icon,
  .point-card__status {
    color: #d54941;
  }
  .point-card__name {
    color: #5c1f1c;
  }
}

/* 未布防：橙色底，与「离线」的灰、「报警」的红都拉开 */
.point-card--DISARMED {
  background-color: #fff3e9;
  .point-card__icon,
  .point-card__status {
    color: #e37318;
  }
  .point-card__name {
    color: #6b3a10;
  }
}

/* 离线 */
.point-card--OFFLINE {
  background-color: #f2f3f5;
  .point-card__icon,
  .point-card__status {
    color: #86909c;
  }
  .point-card__name {
    color: #4e5969;
  }
}

/* 未知：虚线边框，与「明确离线」在视觉上区分开 */
.point-card--UNKNOWN {
  background-color: #f8fafc;
  border: 2px dashed #c9cdd4;
  .point-card__icon,
  .point-card__status,
  .point-card__name {
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
