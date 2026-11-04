<template>
  <div class="vehicle-panel">
    <div class="vehicle-panel__head">
      <t-icon name="user-list" class="vehicle-panel__head-icon" />
      <span class="vehicle-panel__title">最近识别车辆</span>
      <span v-if="vehicles.length" class="vehicle-panel__count">共 {{ vehicles.length }} 辆</span>
    </div>

    <t-empty v-if="isEmpty" description="暂无识别记录" />

    <div v-else class="vehicle-panel__list" :class="{ 'vehicle-panel__list--loading': loading }">
      <button
        v-for="v in vehicles"
        :key="v.eventId"
        type="button"
        class="vehicle-card"
        :class="[`vehicle-card--${v.direction}`, { 'vehicle-card--selected': v.eventId === modelValue }]"
        :aria-pressed="v.eventId === modelValue"
        @click="handleSelect(v)"
      >
        <span v-if="v.eventId === modelValue" class="vehicle-card__badge">当前</span>

        <div class="vehicle-card__main">
          <div class="vehicle-card__plate">{{ v.plateNo }}</div>
          <div class="vehicle-card__sub">{{ vehicleSub(v) }}</div>
          <div class="vehicle-card__meta">
            <span class="vehicle-card__time">{{ v.recognizedAt }}</span>
            <span class="vehicle-card__gate">{{ v.gateName }}</span>
          </div>
        </div>

        <div class="vehicle-card__side">
          <t-tag :theme="DIRECTION_THEME[v.direction]" variant="light" size="small">
            {{ v.directionName || DIRECTION_LABEL[v.direction] }}
          </t-tag>
        </div>
      </button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { DIRECTION_LABEL, DIRECTION_THEME } from '../constants';
import type { TodayVehicle } from '../types';

/**
 * 最近识别车辆面板（左侧列表，可跨天）。
 *
 * 与遮挡/仓门的「设备状态网格」不同：这里每张卡是一辆今天被识别到的车，
 * 内容 = 车牌号 + 车型·颜色 + 识别时间 + 识别大门，右侧一个「入库/出库」tag。
 * 选中一条 → 右侧播放它所属识别点位的摄像头（由父级经 selectItem 触发取流）。
 */
const props = withDefaults(
  defineProps<{
    vehicles: TodayVehicle[];
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
  (e: 'update:modelValue', eventId: string): void;
  (e: 'change', vehicle: TodayVehicle): void;
}>();

/** 车型·颜色：颜色为空时只显示车型，避免出现「柴油货车·」这种尾巴 */
function vehicleSub(v: TodayVehicle): string {
  return v.vehicleColor ? `${v.vehicleType}·${v.vehicleColor}` : v.vehicleType || '未知车型';
}

function handleSelect(vehicle: TodayVehicle) {
  emit('update:modelValue', vehicle.eventId);
  emit('change', vehicle);
}

void props;
</script>
<style lang="less" scoped>
.vehicle-panel {
  display: flex;
  flex-direction: column;
  background-color: var(--td-bg-color-container);
  border-radius: var(--td-radius-medium);
  padding: var(--td-comp-paddingLR-l) var(--td-comp-paddingTB-l);
}

.vehicle-panel__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: var(--td-comp-margin-m);
}

.vehicle-panel__head-icon {
  font-size: 18px;
  color: var(--td-brand-color);
}

.vehicle-panel__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--td-text-color-primary);
}

.vehicle-panel__count {
  margin-left: auto;
  font-size: 12px;
  color: var(--td-text-color-secondary);
}

.vehicle-panel__list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 460px;
  overflow-y: auto;
}

.vehicle-panel__list--loading {
  opacity: 0.6;
  pointer-events: none;
}

.vehicle-card {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
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

/* 入库=绿、出库=橙，与原型卡片底色一致 */
.vehicle-card--IN {
  background-color: #e8f8f0;
  .vehicle-card__plate {
    color: #0a3d2c;
  }
}

.vehicle-card--OUT {
  background-color: #fff3e8;
  .vehicle-card__plate {
    color: #6b3d10;
  }
}

.vehicle-card--selected {
  border-color: var(--td-brand-color);
  box-shadow: 0 0 0 3px rgba(0, 82, 217, 0.12);
}

.vehicle-card__badge {
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

.vehicle-card__main {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.vehicle-card__plate {
  font-size: 15px;
  font-weight: 700;
  line-height: 20px;
}

.vehicle-card__sub {
  font-size: 12px;
  color: var(--td-text-color-secondary);
}

.vehicle-card__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}

.vehicle-card__gate {
  color: var(--td-text-color-secondary);
}

.vehicle-card__side {
  flex-shrink: 0;
}
</style>
