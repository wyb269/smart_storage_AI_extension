<template>
  <div class="recent-table">
    <div class="recent-table__head">
      <span class="recent-table__title">最近异动记录</span>
      <t-button theme="primary" variant="text" size="small" @click="handleViewAll">
        查看全部
        <template #icon><t-icon name="chevron-right" /></template>
      </t-button>
    </div>

    <t-alert v-if="error" theme="error" :message="error" class="recent-table__error">
      <template #operation>
        <t-link theme="primary" size="small" @click="handleRetry">重试</t-link>
      </template>
    </t-alert>

    <t-table
      v-else
      :data="displayEvents"
      :columns="columns"
      row-key="eventId"
      :loading="loading"
      :bordered="false"
      size="small"
      :hover="true"
    >
      <template #algorithmConclusion="{ row }">
        <!-- 算法失败或未执行时不得显示为“正常” -->
        <t-tag v-if="hasConclusion(row)" :theme="algorithmResultTheme(row)" variant="light" size="small">
          {{ algorithmResultLabel(row) }}
        </t-tag>
        <span v-else class="recent-table__muted">-</span>
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
        <span class="recent-table__muted">暂无异动记录</span>
      </template>
    </t-table>
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
 * 首页最近异动记录表格。
 *
 * 依据 AI_HANDOFF.md 第16.5节：
 * - 首页只展示最近事件，建议 5 条
 * - 至少包含事件编号、发生时间、仓门位置、摄像头、处理状态、操作
 *
 * 2026-10-21 产品变更：操作列只保留「查看」，不再提供人工处理入口。
 */
const props = withDefaults(
  defineProps<{
    events: AnomalyEvent[];
    loading?: boolean;
    error?: string;
  }>(),
  {
    loading: false,
    error: '',
  },
);

const emit = defineEmits<{
  (e: 'view', eventId: string): void;
  (e: 'view-all'): void;
  (e: 'retry'): void;
}>();

const columns: PrimaryTableCol[] = [
  { title: '事件编号', colKey: 'eventNo', width: 150 },
  { title: '发生时间', colKey: 'occurredAt', width: 165 },
  { title: '仓门位置', colKey: 'doorName', width: 130 },
  { title: '摄像头', colKey: 'cameraCode', width: 110 },
  { title: '处理状态', colKey: 'processStatus', width: 100 },
  { title: '操作', colKey: 'operation', width: 120, fixed: 'right' },
];

/** 算法是否已产出结论 */
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

/** 表格列直接用格式化后的值，避免在模板里散落格式逻辑 */
const displayEvents = computed(() =>
  props.events.map((e) => ({
    ...e,
    occurredAt: formatTime(e.occurredAt),
    cameraCode: e.cameraCode || '-',
  })),
);

function handleView(row: AnomalyEvent) {
  emit('view', row.eventId);
}

function handleViewAll() {
  emit('view-all');
}

function handleRetry() {
  emit('retry');
}
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
  margin-bottom: var(--td-comp-margin-m);
}

.recent-table__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--td-text-color-primary);
}

.recent-table__error {
  margin-bottom: var(--td-comp-margin-s);
}

.recent-table__muted {
  color: var(--td-text-color-placeholder);
  font-size: 12px;
}
</style>
