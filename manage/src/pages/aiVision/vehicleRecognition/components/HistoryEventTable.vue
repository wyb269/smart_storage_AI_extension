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
          <t-link theme="primary" size="small" @click="emit('view', rowEventId(row))"> 查看 </t-link>
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

import { DIRECTION_LABEL, DIRECTION_THEME, VERIFY_STATUS_LABEL, VERIFY_STATUS_THEME } from '../constants';
import type { Direction, VehicleEvent, VerifyStatus } from '../types';

/**
 * 出入事件记录分页表格。
 *
 * 分页由服务端完成（指南禁止前端伪分页），本组件只负责把
 * 页码与页大小变化上抛，不自行切片。
 *
 * 列与原型图一致：车牌号 / 车辆类型 / 行驶方向 / 识别地点 / 状态 / 操作，
 * 另补识别时间与仓库便于跨天检索。
 */
const props = defineProps<{
  events: VehicleEvent[];
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
function directionOf(row: unknown): Direction {
  return (row as VehicleEvent).direction;
}

function verifyOf(row: unknown): VerifyStatus {
  return (row as VehicleEvent).verifyStatus;
}

function rowEventId(row: unknown): string {
  return (row as VehicleEvent).eventId;
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
  { colKey: 'plateNo', title: '车牌号', width: 130, ellipsis: true },
  { colKey: 'vehicleType', title: '车辆类型', width: 120, ellipsis: true },
  { colKey: 'direction', title: '行驶方向', width: 110, cell: 'direction' },
  { colKey: 'gateName', title: '识别地点', width: 140, ellipsis: true },
  { colKey: 'recognizedAt', title: '识别时间', width: 170 },
  { colKey: 'verifyStatus', title: '状态', width: 100, cell: 'verifyStatus' },
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
