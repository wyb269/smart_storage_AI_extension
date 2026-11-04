<template>
  <div class="recent-table">
    <div class="recent-table__head">
      <div class="recent-table__head-left">
        <t-icon name="time" class="recent-table__head-icon" />
        <span class="recent-table__title">遮挡历史记录</span>
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
      <template #processStatus="{ row }">
        <t-tag :theme="PROCESS_STATUS_THEME[statusOf(row)]" variant="light" size="small">
          {{ PROCESS_STATUS_LABEL[statusOf(row)] }}
        </t-tag>
      </template>

      <template #op="{ row }">
        <t-link theme="primary" size="small" @click="emit('view', row.eventId)">查看</t-link>
      </template>
    </t-table>
  </div>
</template>
<script setup lang="ts">
import type { PrimaryTableCol } from 'tdesign-vue-next';

import { PROCESS_STATUS_LABEL, PROCESS_STATUS_THEME } from '../constants';
import type { OcclusionEvent, ProcessStatus } from '../types';

/**
 * 首页最近遮挡事件表。
 *
 * 列与原型图一致：事件编号 / 摄像头 / 摄像头位置 / 处理状态 / 操作。
 * 处理状态按两态展示（未处理 / 已处理），底层五种编码不合并。
 */
defineProps<{
  events: OcclusionEvent[];
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
function statusOf(row: unknown): ProcessStatus {
  return (row as OcclusionEvent).processStatus;
}

const columns: PrimaryTableCol[] = [
  { colKey: 'eventNo', title: '事件编号', width: 180, ellipsis: true },
  { colKey: 'cameraCode', title: '摄像头', width: 120 },
  { colKey: 'cameraPosition', title: '摄像头位置', width: 180, ellipsis: true },
  { colKey: 'processStatus', title: '处理状态', width: 110, cell: 'processStatus' },
  { colKey: 'op', title: '操作', width: 90, cell: 'op', fixed: 'right' },
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
</style>
