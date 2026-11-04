<template>
  <div class="history-table">
    <t-alert v-if="error" theme="error" :message="error" class="history-table__error">
      <template #operation>
        <t-link theme="primary" size="small" @click="handleRetry">重试</t-link>
      </template>
    </t-alert>

    <template v-else>
      <t-table
        :data="displayEvents"
        :columns="columns"
        row-key="eventId"
        :loading="loading"
        :bordered="false"
        size="small"
        :hover="true"
        table-layout="fixed"
      >
        <template #algorithmConclusion="{ row }">
          <!-- 算法失败或未执行时不得显示为“正常” -->
          <t-tag v-if="hasConclusion(row)" :theme="algorithmResultTheme(row)" variant="light" size="small">
            {{ algorithmResultLabel(row) }}
          </t-tag>
          <span v-else class="history-table__muted">-</span>
        </template>

        <template #processStatus="{ row }">
          <t-tag :theme="processStatusTheme(row)" variant="light" size="small">
            {{ processStatusLabel(row) }}
          </t-tag>
        </template>

        <template #operation="{ row }">
          <!-- 2026-10-21 取消人工处理入口：操作列只保留「查看」，查看即标记为已处理 -->
          <t-link theme="primary" size="small" @click="handleView(row)">查看</t-link>
        </template>

        <template #empty>
          <span class="history-table__muted">暂无符合条件的记录</span>
        </template>
      </t-table>
      <div class="history-table__pager">
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

import {
  ALGORITHM_RESULT_LABEL,
  ALGORITHM_RESULT_THEME,
  PROCESS_STATUS_SHORT_LABEL,
  PROCESS_STATUS_THEME,
} from '../constants';
import type { AnomalyEvent } from '../types';

/**
 * 历史事件表格（服务端分页）。
 *
 * 依据 AI_HANDOFF.md 第18.4节：
 * - 使用服务端分页，禁止前端一次性加载全部历史数据
 * - 默认每页 10 或 20 条
 * - 翻页时保留筛选条件
 * - 页码超出有效范围时回到最后一个有效页
 */
const props = withDefaults(
  defineProps<{
    events: AnomalyEvent[];
    total: number;
    pageNo: number;
    pageSize: number;
    loading?: boolean;
    error?: string;
  }>(),
  {
    loading: false,
    error: '',
  },
);

const emit = defineEmits<{
  (e: 'update:pageNo', page: number): void;
  (e: 'update:pageSize', size: number): void;
  (e: 'view', eventId: string): void;
  (e: 'page-change', page: number, size: number): void;
  (e: 'page-size-change', size: number): void;
  (e: 'retry'): void;
}>();

const columns: PrimaryTableCol[] = [
  { title: '事件编号', colKey: 'eventNo', width: 150 },
  { title: '发生时间', colKey: 'occurredAt', width: 165 },
  { title: '仓门位置', colKey: 'doorName', width: 130 },
  { title: '摄像头', colKey: 'cameraCode', width: 110 },
  { title: '异动类型', colKey: 'anomalyTypeName', width: 120 },
  // 第18.3节要求显示算法结论
  { title: '算法结论', colKey: 'algorithmConclusion', width: 100 },
  { title: '处理状态', colKey: 'processStatus', width: 100 },
  { title: '操作', colKey: 'operation', width: 120, fixed: 'right' },
];

/**
 * 分页配置由外部完全受控。
 * 第18.4节：查询条件变化后回到第一页、翻页保留筛选，
 * 均通过父组件更新 pageNo/pageSize 实现。
 */
const currentPage = computed({
  get: () => props.pageNo,
  set: (value: number) => emit('update:pageNo', value),
});

const currentSize = computed({
  get: () => props.pageSize,
  set: (value: number) => emit('update:pageSize', value),
});

function formatTime(value: string): string {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}

const displayEvents = computed(() =>
  props.events.map((e) => ({
    ...e,
    occurredAt: formatTime(e.occurredAt),
    cameraCode: e.cameraCode || '-',
    anomalyTypeName: e.anomalyTypeName || e.anomalyType,
  })),
);

function hasConclusion(row: AnomalyEvent): boolean {
  return (row.algorithmStatus === 'SUCCEEDED' || row.algorithmStatus === 'MOCK') && !!row.algorithmResult;
}

function algorithmResultLabel(row: AnomalyEvent): string {
  return row.algorithmResult ? ALGORITHM_RESULT_LABEL[row.algorithmResult] : '-';
}

function algorithmResultTheme(row: AnomalyEvent) {
  return row.algorithmResult ? ALGORITHM_RESULT_THEME[row.algorithmResult] : 'default';
}

function processStatusLabel(row: AnomalyEvent): string {
  return PROCESS_STATUS_SHORT_LABEL[row.processStatus] ?? '-';
}

function processStatusTheme(row: AnomalyEvent) {
  return PROCESS_STATUS_THEME[row.processStatus] ?? 'default';
}

/**
 * 分页变更入口。
 *
 * <p>修复 P1-4：此前仅 emit v-model 更新，父组件收到后没有重新请求，
 * 表现为「翻页后表格内容不变」。</p>
 *
 * <p>区分两种情况（第18.4节）：</p>
 * <ul>
 *   <li>页大小发生变化 → 视为查询条件变化，交由父组件回到第一页；</li>
 *   <li>仅页码变化 → 保留当前筛选条件，直接翻页。</li>
 * </ul>
 */
function handlePageChange(context: { current: number; pageSize: number }) {
  if (context.pageSize !== props.pageSize) {
    emit('page-size-change', context.pageSize);
  } else {
    emit('page-change', context.current, context.pageSize);
  }
}

function handleView(row: AnomalyEvent) {
  emit('view', row.eventId);
}

function handleRetry() {
  emit('retry');
}
</script>
<style lang="less" scoped>
.history-table {
  background-color: var(--td-bg-color-container);
  border-radius: var(--td-radius-medium);
  padding: var(--td-comp-paddingLR-l) var(--td-comp-paddingTB-l);
}

.history-table__pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

.history-table__error {
  margin-bottom: var(--td-comp-margin-s);
}

.history-table__muted {
  color: var(--td-text-color-placeholder);
  font-size: 12px;
}
</style>
