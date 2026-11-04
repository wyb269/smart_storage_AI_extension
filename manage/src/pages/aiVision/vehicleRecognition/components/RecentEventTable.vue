<template>
  <div class="recent-table">
    <div class="recent-table__head">
      <div class="recent-table__head-left">
        <t-icon name="time" class="recent-table__head-icon" />
        <span class="recent-table__title">出入事件记录</span>
      </div>
      <t-link theme="primary" size="small" @click="emit('view-all')">
        <template #suffix><t-icon name="chevron-right" /></template>
        查看全部
      </t-link>
    </div>

    <t-alert v-if="error" theme="error" :message="error" class="recent-table__alert">
      <template #operation>
        <t-link theme="primary" size="small" @click="emit('retry')">重试</t-link>
      </template>
    </t-alert>

    <t-table
      v-else
      row-key="eventId"
      :data="events"
      :columns="columns"
      :loading="loading"
      size="small"
      :hover="true"
      table-layout="fixed"
    >
      <template #direction="{ row }">
        <t-tag :theme="DIRECTION_THEME[directionOf(row)]" variant="light" size="small">
          {{ row.directionName || DIRECTION_LABEL[directionOf(row)] }}
        </t-tag>
      </template>

      <template #verifyStatus="{ row }">
        <t-tag :theme="VERIFY_STATUS_THEME[verifyOf(row)]" variant="light" size="small">
          {{ VERIFY_STATUS_LABEL[verifyOf(row)] }}
        </t-tag>
      </template>

      <template #op="{ row }">
        <t-link theme="primary" size="small" @click="emit('view', rowEventId(row))">查看</t-link>
      </template>
    </t-table>
  </div>
</template>
<script setup lang="ts">
import type { PrimaryTableCol } from 'tdesign-vue-next';

import { DIRECTION_LABEL, DIRECTION_THEME, VERIFY_STATUS_LABEL, VERIFY_STATUS_THEME } from '../constants';
import type { Direction, VehicleEvent, VerifyStatus } from '../types';

/**
 * 首页最近识别记录表（出入事件记录）。
 *
 * 列与原型图一致：车牌号 / 车辆类型 / 行驶方向 / 识别地点 / 状态 / 操作。
 */
defineProps<{
  events: VehicleEvent[];
  loading?: boolean;
  error?: string;
}>();

const emit = defineEmits<{
  (e: 'view', eventId: string): void;
  (e: 'view-all'): void;
  (e: 'retry'): void;
}>();

/**
 * t-table 插槽的 row 是 any，直接索引字面量主题映射表会触发 TS7053。
 * 这里集中收窄一次，避免在每个插槽里重复断言。
 */
function directionOf(row: unknown): Direction {
  return (row as VehicleEvent).direction;
}

function verifyOf(row: unknown): VerifyStatus {
  return (row as VehicleEvent).verifyStatus;
}

function rowEventId(row: unknown): string {
  return (row as VehicleEvent).eventId;
}

const columns: PrimaryTableCol[] = [
  { colKey: 'plateNo', title: '车牌号', width: 130, ellipsis: true },
  { colKey: 'vehicleType', title: '车辆类型', width: 120, ellipsis: true },
  { colKey: 'direction', title: '行驶方向', width: 110, cell: 'direction' },
  { colKey: 'gateName', title: '识别地点', width: 140, ellipsis: true },
  { colKey: 'verifyStatus', title: '状态', width: 100, cell: 'verifyStatus' },
  { colKey: 'op', title: '操作', width: 120, cell: 'op', fixed: 'right' },
];
</script>
<style lang="less" scoped>
.recent-table {
  background-color: var(--td-bg-color-container);
  border-radius: var(--td-radius-medium);
  padding: var(--td-comp-paddingLR-l) var(--td-comp-paddingTB-l);
}

.recent-table__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: var(--td-comp-margin-m);
}

.recent-table__head-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.recent-table__head-icon {
  font-size: 18px;
  color: var(--td-brand-color);
}

.recent-table__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--td-text-color-primary);
}

.recent-table__alert {
  margin-bottom: 8px;
}

.recent-table__op-process {
  margin-left: 12px;
}
</style>
