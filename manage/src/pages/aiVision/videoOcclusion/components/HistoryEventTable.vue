<template>
  <div class="history-table">
    <t-alert v-if="error" theme="error" :message="error" class="history-table__alert">
      <template #operation>
        <t-link theme="primary" size="small" @click="emit('retry')">重试</t-link>
      </template>
    </t-alert>

    <template v-else>
      <t-table
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
          <t-link theme="primary" size="small" @click="emit('view', statusRow(row).eventId)"> 查看 </t-link>
        </template>
      </t-table>

      <div class="history-table__pager">
        <span class="history-table__total">共 {{ total }} 条</span>
        <t-pagination
          v-model="currentPage"
          v-model:page-size="currentSize"
          :total="total"
          :page-size-options="[10, 20, 50]"
          show-jumper
          size="small"
          @change="handlePageChange"
        />
      </div>
    </template>
  </div>
</template>
<script setup lang="ts">
import type { PrimaryTableCol } from 'tdesign-vue-next';
import { computed } from 'vue';

import { PROCESS_STATUS_LABEL, PROCESS_STATUS_THEME } from '../constants';
import type { OcclusionEvent, ProcessStatus } from '../types';

/**
 * 遮挡历史记录分页表格。
 *
 * 分页由服务端完成（指南禁止前端伪分页），本组件只负责把
 * 页码与页大小变化上抛，不自行切片。
 *
 * 列与原型图一致：事件编号 / 摄像头 / 摄像头位置 / 处理状态 / 操作。
 * 处理状态按两态展示，底层五种编码不合并。
 */
const props = defineProps<{
  events: OcclusionEvent[];
  total: number;
  pageNo: number;
  pageSize: number;
  loading?: boolean;
  error?: string;
}>();

const emit = defineEmits<{
  (e: 'update:pageNo', value: number): void;
  (e: 'update:pageSize', value: number): void;
  (e: 'view', eventId: string): void;
  (e: 'page-change', pageNo: number): void;
  (e: 'page-size-change', pageSize: number): void;
  (e: 'retry'): void;
}>();

/** t-table 插槽的 row 是 any，集中收窄一次，避免每个插槽重复断言 */
function statusOf(row: unknown): ProcessStatus {
  return (row as OcclusionEvent).processStatus;
}

function statusRow(row: unknown): OcclusionEvent {
  return row as OcclusionEvent;
}

const currentPage = computed({
  get: () => props.pageNo,
  set: (value: number) => emit('update:pageNo', value),
});

const currentSize = computed({
  get: () => props.pageSize,
  set: (value: number) => emit('update:pageSize', value),
});

function handlePageChange(context: { current: number; pageSize: number }) {
  // 页大小变化时同时上抛，由页面决定是否需要回到第一页
  if (context.pageSize !== props.pageSize) {
    emit('page-size-change', context.pageSize);
  }
  emit('page-change', context.current);
}

const columns: PrimaryTableCol[] = [
  { colKey: 'eventNo', title: '事件编号', width: 180, ellipsis: true },
  { colKey: 'occurredAt', title: '发生时间', width: 170 },
  { colKey: 'warehouseName', title: '仓库', width: 120, ellipsis: true },
  { colKey: 'cameraCode', title: '摄像头', width: 110 },
  { colKey: 'cameraPosition', title: '摄像头位置', width: 170, ellipsis: true },
  { colKey: 'occlusionTypeName', title: '遮挡类型', width: 120, ellipsis: true },
  { colKey: 'processStatus', title: '处理状态', width: 110, cell: 'processStatus' },
  { colKey: 'op', title: '操作', width: 90, cell: 'op', fixed: 'right' },
];
</script>
<style lang="less" scoped>
.history-table {
  background-color: var(--td-bg-color-container);
  border-radius: var(--td-radius-medium);
  padding: var(--td-comp-paddingLR-l) var(--td-comp-paddingTB-l);
}

.history-table__alert {
  margin-bottom: 8px;
}

.history-table__pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

.history-table__total {
  font-size: 12px;
  color: var(--td-text-color-secondary);
}
</style>
