<template>
  <div class="door-panel">
    <!-- 顶部统计 -->
    <div class="door-panel__head">
      <span class="door-panel__title">仓门状态</span>
      <span class="door-panel__count">
        共 {{ doors.length }} 个仓门
        <t-tag v-if="abnormalCount > 0" theme="danger" variant="light" size="small" class="door-panel__tag">
          {{ abnormalCount }} 个异常
        </t-tag>
        <t-tag v-if="offlineCount > 0" theme="default" variant="light" size="small" class="door-panel__tag">
          {{ offlineCount }} 个离线
        </t-tag>
      </span>
    </div>

    <!-- 空态 -->
    <t-empty v-if="isEmpty" description="暂无可监控仓门" />

    <!-- 仓门网格 -->
    <div v-else class="door-panel__grid" :class="{ 'door-panel__grid--loading': loading }">
      <button
        v-for="door in doors"
        :key="door.doorId"
        type="button"
        class="door-card"
        :class="[`door-card--${displayStatusOf(door)}`, { 'door-card--selected': door.doorId === modelValue }]"
        :aria-pressed="door.doorId === modelValue"
        @click="handleSelect(door)"
      >
        <!-- 选中标识角标 -->
        <span v-if="door.doorId === modelValue" class="door-card__badge">当前</span>

        <!-- 仓门名称 -->
        <span class="door-card__name">{{ door.doorName }}</span>

        <!-- 状态行：图标 + 文字 -->
        <span class="door-card__meta">
          <t-icon name="login" class="door-card__icon" />
          <span class="door-card__status">{{ displayStatusLabel(door) }}</span>
        </span>

        <!-- 在线状态的呼吸灯动画（仅正常状态显示） -->
        <span v-if="displayStatusOf(door) === 'NORMAL'" class="door-card__pulse" aria-hidden="true"></span>
      </button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue';

import { computeDisplayStatus, connectionStatusLabel } from '../../shared/displayStatus';
import { DISPLAY_STATUS_LABEL } from '../constants';
import type { DisplayStatus, DoorStatus } from '../types';

/**
 * 仓门状态面板。
 *
 * 依据原型图与 AI_HANDOFF.md 第16.3节：
 * - 网格布局，自动适应列数
 * - 颜色语义：正常浅绿、异常浅红、离线灰、未知中性虚线
 * - 选中状态使用蓝色边框 + "当前"角标
 * - 在线状态有呼吸灯动画增强视觉反馈
 */
const props = withDefaults(
  defineProps<{
    doors: DoorStatus[];
    /** 当前选中的仓门 ID */
    modelValue?: string;
    loading?: boolean;
    /** 无仓门时的空态展示 */
    isEmpty?: boolean;
  }>(),
  {
    modelValue: '',
    loading: false,
    isEmpty: false,
  },
);

const emit = defineEmits<{
  (e: 'update:modelValue', doorId: string): void;
  (e: 'change', door: DoorStatus): void;
}>();

/** 计算某仓门的展示状态 */
function displayStatusOf(door: DoorStatus): DisplayStatus {
  return computeDisplayStatus({
    deviceStatus: door.deviceStatus,
    alarmStatus: door.alarmStatus,
    lastHeartbeatAt: door.lastHeartbeatAt,
  });
}

/** 展示状态中文文案 */
function displayStatusLabel(door: DoorStatus): string {
  const connection = connectionStatusLabel(door);
  return displayStatusOf(door) === 'ABNORMAL' ? `${connection} · ${DISPLAY_STATUS_LABEL.ABNORMAL}` : connection;
}

function handleSelect(door: DoorStatus) {
  emit('update:modelValue', door.doorId);
  emit('change', door);
}

const abnormalCount = computed(() => props.doors.filter((d) => displayStatusOf(d) === 'ABNORMAL').length);

const offlineCount = computed(() => props.doors.filter((d) => displayStatusOf(d) === 'OFFLINE').length);
</script>
<style lang="less" scoped>
.door-panel {
  display: flex;
  flex-direction: column;
  background-color: var(--td-bg-color-container);
  border-radius: var(--td-radius-medium);
  padding: var(--td-comp-paddingLR-l) var(--td-comp-paddingTB-l);
}

.door-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--td-comp-margin-m);
}

.door-panel__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--td-text-color-primary);
}

.door-panel__count {
  font-size: 12px;
  color: var(--td-text-color-secondary);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

/* 仓门卡片网格 - 3列为主，窄屏自动2列 */
.door-panel__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(110px, 1fr));
  gap: 10px;
  align-content: start;
}

@media (max-width: 420px) {
  .door-panel__grid {
    grid-template-columns: repeat(2, minmax(110px, 1fr));
  }
}

.door-panel__grid--loading {
  opacity: 0.6;
  pointer-events: none;
}

/* ---------- 仓门卡片 ---------- */

.door-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 10px;
  /* 预留透明边框占位，避免选中时尺寸抖动 */
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

/* 正常：浅绿背景 + 呼吸灯 */
.door-card--NORMAL {
  background-color: #e8f8f0;
  .door-card__icon {
    color: #00a870;
  }
  .door-card__status {
    color: #00a870;
  }
  .door-card__name {
    color: #0a3d2c;
  }
}

/* 异常：浅红背景 */
.door-card--ABNORMAL {
  background-color: #fdeeee;
  .door-card__icon {
    color: #d54941;
  }
  .door-card__status {
    color: #d54941;
  }
  .door-card__name {
    color: #5c1f1c;
  }
}

/* 离线：灰色背景 */
.door-card--OFFLINE {
  background-color: #f2f3f5;
  .door-card__icon {
    color: #86909c;
  }
  .door-card__status {
    color: #86909c;
  }
  .door-card__name {
    color: #4e5969;
  }
}

/* 未知：中性浅色 + 虚线边框 */
.door-card--UNKNOWN {
  background-color: #f8fafc;
  border: 2px dashed #c9cdd4;
  .door-card__icon {
    color: #c9cdd4;
  }
  .door-card__status {
    color: #86909c;
  }
  .door-card__name {
    color: #86909c;
  }
}

/* 选中态：蓝色边框 + 阴影 */
.door-card--selected {
  border-color: var(--td-brand-color);
  box-shadow: 0 0 0 3px rgba(0, 82, 217, 0.12);
}

/* 当前角标 */
.door-card__badge {
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

/* 仓门名称 */
.door-card__name {
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}

/* 状态行 */
.door-card__meta {
  display: flex;
  align-items: center;
  gap: 4px;
}

.door-card__icon {
  font-size: 16px;
  flex-shrink: 0;
  opacity: 0.9;
}

.door-card__status {
  font-size: 12px;
  line-height: 16px;
}

/* 呼吸灯动画（仅在线状态显示） */
.door-card__pulse {
  position: absolute;
  bottom: 8px;
  right: 8px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #00a870;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
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

/* 响应式：窄屏调整间距 */
@media (max-width: 1440px) {
  .door-panel__grid {
    gap: 6px;
  }
  .door-card {
    padding: 10px 8px;
  }
}

/* 状态图标置顶，按原型使用语义色；在线/离线文字保持可读。 */
.door-card {
  align-items: center;
  text-align: center;
}

.door-card__meta {
  display: contents;
}

.door-card__icon {
  order: -1;
  align-self: center;
  margin-bottom: 4px;
  font-size: 24px;
  opacity: 1;
}

.door-card__status {
  order: 2;
  font-weight: 600;
}

.door-card__name,
.door-card__code,
.door-card__title {
  width: 100%;
  text-align: center;
}
</style>
