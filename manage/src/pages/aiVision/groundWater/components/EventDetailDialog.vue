<template>
  <t-dialog
    :visible="visible"
    :header="`积水事件详情 - ${detail?.eventNo || eventId}`"
    width="760px"
    :footer="false"
    @update:visible="emit('update:visible', $event)"
  >
    <t-loading :loading="loading">
      <div v-if="detail" class="water-detail">
        <t-descriptions :column="2" item-layout="horizontal" size="small" bordered>
          <t-descriptions-item label="事件编号">{{ detail.eventNo }}</t-descriptions-item>
          <t-descriptions-item label="发生时间">{{ detail.occurredAt }}</t-descriptions-item>
          <t-descriptions-item label="仓库">{{ detail.warehouseName }}</t-descriptions-item>
          <t-descriptions-item label="监测位置">{{ detail.pointName }}</t-descriptions-item>
          <t-descriptions-item label="摄像头">{{ detail.cameraCode }}</t-descriptions-item>
          <t-descriptions-item label="积水类型">
            {{ detail.waterTypeName || detail.waterType }}
          </t-descriptions-item>
          <t-descriptions-item label="处理状态">
            <t-tag :theme="PROCESS_STATUS_THEME[detail.processStatus]" variant="light" size="small">
              {{ PROCESS_STATUS_LABEL[detail.processStatus] }}
            </t-tag>
          </t-descriptions-item>
          <t-descriptions-item label="算法结论">
            {{ detail.algorithmResult || '—' }}
          </t-descriptions-item>
          <t-descriptions-item label="事件描述" :span="2">
            {{ detail.eventDesc || '—' }}
          </t-descriptions-item>
        </t-descriptions>

        <div class="water-detail__section">
          <div class="water-detail__section-title">证据</div>
          <evidence-illustration kind="groundWater" />
        </div>

        <!-- ==================== 算法任务 ==================== -->
        <div class="water-detail__section">
          <div class="water-detail__section-head">
            <div class="water-detail__section-title">算法任务</div>
            <t-button
              theme="primary"
              variant="outline"
              size="small"
              :loading="rerunSubmitting"
              :disabled="rerunSubmitting || rerunPolling"
              @click="handleRerun"
            >
              <template #icon><t-icon name="refresh" /></template>
              {{ rerunPolling ? '识别中…' : '重新识别' }}
            </t-button>
          </div>

          <!-- 摘要：模型/算法/配置/推理耗时/检出数；地面积水额外给出积水类型 -->
          <div v-if="latestTask" class="water-detail__algo-summary">
            <span>{{ algorithmSummaryText }}</span>
            <span class="water-detail__algo-extra">积水类型 {{ waterTypeDisplay }}</span>
          </div>
          <div v-else class="water-detail__algo-summary water-detail__algo-summary--empty">
            暂无算法任务，可点击「重新识别」补一次取证
          </div>

          <!-- 行内提示：提交成功 / 轮询超时 / 失败信息；错误与超时文案不被吞掉 -->
          <div v-if="rerunHint" class="water-detail__algo-hint">{{ rerunHint }}</div>
          <t-alert
            v-if="
              latestTask &&
              (latestTask.executionStatus === 'FAILED' || latestTask.executionStatus === 'TIMEOUT') &&
              (latestTask.errorMessage || latestTask.errorCode)
            "
            theme="error"
            size="small"
            :message="displayTaskError(latestTask)"
            class="water-detail__algo-error"
          />

          <t-table
            v-if="displayTasks.length"
            :data="displayTasks as any"
            :columns="ALGORITHM_TASK_COLUMNS"
            :hover="true"
            size="small"
            row-key="taskNo"
          >
            <template #status="{ row }">
              <t-tag :theme="taskStatusTheme(row.executionStatus)" variant="light" size="small">
                {{ taskStatusLabel(row.executionStatus) }}
              </t-tag>
            </template>
            <template #confidence="{ row }">{{ confidenceText(row.confidence) }}</template>
            <template #taskNo="{ row }">{{ displayTaskNumber(row.taskNo) }}</template>
            <template #modelVersion="{ row }">{{ displayModelVersion(row.modelVersion, row.simulated) }}</template>
            <template #duration="{ row }">
              {{
                row.inferenceDurationMs === null || row.inferenceDurationMs === undefined
                  ? '—'
                  : `${row.inferenceDurationMs}ms`
              }}
            </template>
            <template #time="{ row }">{{ row.startedAt || '—' }}</template>
          </t-table>
        </div>

        <div v-if="detail.processRecords?.length" class="water-detail__section">
          <div class="water-detail__section-title">处理记录</div>
          <process-record-list :records="detail.processRecords" />
        </div>
      </div>
    </t-loading>
  </t-dialog>
</template>
<script setup lang="ts">
import { MessagePlugin } from 'tdesign-vue-next';
import { computed, onBeforeUnmount, ref, watch } from 'vue';

import { getAlgorithmTask, rerunAlgorithm } from '@/api/groundWater';

import {
  displayModelVersion,
  displayTaskError,
  displayTaskNumber,
  displayTaskSummary,
} from '../../shared/algorithmTaskDisplay';
import { useRequestRaceGuard } from '../../shared/composables/useRequestRaceGuard';
import EvidenceIllustration from '../../shared/EvidenceIllustration.vue';
import ProcessRecordList from '../../shared/ProcessRecordList.vue';
import { ALGORITHM_STATUS_LABEL, PROCESS_STATUS_LABEL, PROCESS_STATUS_THEME, WATER_TYPE_LABEL } from '../constants';
import type { AlgorithmStatus, AlgorithmTask, WaterEventDetail } from '../types';

/**
 * 积水事件详情弹窗。
 *
 * 打开详情即由父组件调用 markEventViewed 标记已处理，
 * 本组件不承载人工结论类写操作，也不内嵌处理表单 ——
 * 本期后端只开 7 个接口，没有人工处理接口。
 *
 * 「重新识别」是取证动作（权限 VIEW）：提交后立即返回任务号，
 * 本组件按 800ms 间隔轮询任务状态，最多 20 次（约 16 秒），
 * 拿到终态后请求父组件重拉详情并刷新外层列表；
 * 定时器在组件卸载与弹窗关闭时都必须清掉（竞态守卫 + clearTimeout，不裸 setInterval）。
 *
 * 预览组件用 t-image-viewer + :images；
 * t-image-preview 与 :preview-list 在 TDesign 中不存在，会报组件解析失败。
 */
const props = defineProps<{
  visible: boolean;
  eventId: string;
  detail: WaterEventDetail | null;
  loading?: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  /** 轮询到终态后请求父组件重拉详情 + 刷新外层列表 */
  (e: 'refresh'): void;
}>();

/* ==================== 算法任务区 ==================== */

/** 轮询节奏与上限：800ms × 最多 20 次 ≈ 16 秒 */
const RERUN_POLL_INTERVAL_MS = 800;
const RERUN_POLL_MAX_TIMES = 20;
const TERMINAL_STATUSES: AlgorithmStatus[] = ['SUCCEEDED', 'FAILED', 'TIMEOUT', 'SKIPPED', 'MOCK'];

const rerunSubmitting = ref(false);
const rerunPolling = ref(false);
const rerunHint = ref('');
/** 轮询中的实时任务行：父组件重拉详情前先在本地顶替最新任务 */
const liveTask = ref<AlgorithmTask | null>(null);

let pollTimer: ReturnType<typeof setTimeout> | null = null;
let pollCount = 0;
const pollGuard = useRequestRaceGuard();

/** 展示用任务列表：实时任务置顶 + 详情带回的历史（按 taskId 去重） */
const displayTasks = computed<AlgorithmTask[]>(() => {
  const base = props.detail?.algorithmTasks ?? [];
  const live = liveTask.value;
  if (live) {
    return [live, ...base.filter((t) => t.taskId !== live.taskId)];
  }
  return base;
});

const latestTask = computed<AlgorithmTask | null>(() => {
  if (liveTask.value) return liveTask.value;
  return props.detail?.latestAlgorithmTask ?? displayTasks.value[0] ?? null;
});

/** 摘要行：模型 <name> <version> · 算法 <v> · 配置 <v> · 推理 <ms>ms · 检出 <n> 个目标 */
const algorithmSummaryText = computed(() =>
  latestTask.value ? displayTaskSummary(latestTask.value, '地面积水检测') : '',
);

/** 积水类型：优先取任务结论列，缺失时回退事件列 */
const waterTypeDisplay = computed(() => {
  const code = latestTask.value?.waterType ?? props.detail?.waterType;
  if (!code) return '—';
  return WATER_TYPE_LABEL[code] ?? code;
});

const ALGORITHM_TASK_COLUMNS = [
  { title: '任务号', colKey: 'taskNo', cell: 'taskNo', ellipsis: true },
  { title: '状态', colKey: 'status', cell: 'status', width: 96 },
  { title: '置信度', colKey: 'confidence', cell: 'confidence', width: 84 },
  { title: '模型版本', colKey: 'modelVersion', cell: 'modelVersion', ellipsis: true, width: 110 },
  { title: '耗时', colKey: 'duration', cell: 'duration', width: 78 },
  { title: '时间', colKey: 'time', cell: 'time', width: 160 },
];

/** 状态用色：SUCCEEDED 绿、RUNNING/QUEUED 蓝且带「进行中」、FAILED/TIMEOUT 红 */
function taskStatusTheme(status: AlgorithmStatus): 'success' | 'primary' | 'danger' | 'default' {
  if (status === 'SUCCEEDED') return 'success';
  if (status === 'RUNNING' || status === 'QUEUED') return 'primary';
  if (status === 'FAILED' || status === 'TIMEOUT') return 'danger';
  return 'default';
}

function taskStatusLabel(status: AlgorithmStatus): string {
  if (status === 'RUNNING' || status === 'QUEUED') return '进行中';
  return ALGORITHM_STATUS_LABEL[status] ?? status;
}

/** 置信度：null 显示「暂无」，不得渲染为 0% */
function confidenceText(value: unknown): string {
  if (value === null || value === undefined || value === '') return '暂无';
  const n = Number(value);
  return Number.isNaN(n) ? '—' : `${(n * 100).toFixed(2)}%`;
}

function stopPolling(): void {
  if (pollTimer !== null) {
    clearTimeout(pollTimer);
    pollTimer = null;
  }
  pollCount = 0;
}

function resetRerunState(): void {
  stopPolling();
  rerunPolling.value = false;
  rerunSubmitting.value = false;
  rerunHint.value = '';
  liveTask.value = null;
}

async function handleRerun(): Promise<void> {
  if (!props.detail || rerunSubmitting.value || rerunPolling.value) return;
  rerunSubmitting.value = true;
  rerunHint.value = '';
  try {
    const submitted = await rerunAlgorithm(props.detail.eventId || props.eventId);
    if (!submitted.taskId) {
      rerunHint.value = '重新识别提交异常：未拿到任务号';
      return;
    }
    rerunHint.value = '已提交算法任务';
    liveTask.value = {
      taskId: submitted.taskId,
      taskNo: submitted.taskId,
      executionStatus: submitted.executionStatus,
      simulated: submitted.simulated,
    } as AlgorithmTask;
    pollCount = 0;
    rerunPolling.value = true;
    scheduleNextPoll(submitted.taskId);
  } catch (error: any) {
    rerunHint.value = error?.message ?? '重新识别提交失败';
    MessagePlugin.error(error?.message ?? '重新识别提交失败');
  } finally {
    rerunSubmitting.value = false;
  }
}

function stopPollingOnlyTimer(): void {
  if (pollTimer !== null) {
    clearTimeout(pollTimer);
    pollTimer = null;
  }
}

function scheduleNextPoll(taskId: string): void {
  stopPollingOnlyTimer();
  pollTimer = setTimeout(() => {
    void doPoll(taskId);
  }, RERUN_POLL_INTERVAL_MS);
}

async function doPoll(taskId: string): Promise<void> {
  pollTimer = null;
  if (!rerunPolling.value) return;
  const token = pollGuard.begin();
  pollCount += 1;
  try {
    const task = await getAlgorithmTask(taskId);
    if (!pollGuard.isCurrent(token)) return;
    liveTask.value = task;

    if (TERMINAL_STATUSES.includes(task.executionStatus)) {
      rerunPolling.value = false;
      // 终态提示：失败/超时要把 errorMessage 亮出来，不吞
      rerunHint.value =
        task.executionStatus === 'SUCCEEDED'
          ? '识别完成'
          : `识别${task.executionStatus === 'TIMEOUT' ? '超时' : '失败'}：${displayTaskError(task)}`;
      emit('refresh');
      return;
    }

    if (pollCount >= RERUN_POLL_MAX_TIMES) {
      rerunPolling.value = false;
      rerunHint.value = '算法仍在执行，可稍后刷新';
      return;
    }
    scheduleNextPoll(taskId);
  } catch (error: any) {
    if (!pollGuard.isCurrent(token)) return;
    rerunPolling.value = false;
    rerunHint.value = error?.message ?? '算法任务轮询失败';
  }
}

/** 弹窗关闭或换记录时：清定时器 + 使在途轮询作废，避免跨记录串数据 */
watch(
  () => [props.visible, props.eventId] as const,
  ([visible], [_prevVisible, prevEventId]) => {
    if (!visible || props.eventId !== prevEventId) {
      pollGuard.invalidate();
      resetRerunState();
    }
  },
);

/** 详情重拉完成后，实时行交还给 detail.algorithmTasks，本地状态清零 */
watch(
  () => props.detail?.algorithmTasks?.length,
  (len) => {
    if (len && liveTask.value && !rerunPolling.value) {
      liveTask.value = null;
    }
  },
);

onBeforeUnmount(() => {
  stopPolling();
});
</script>
<style lang="less" scoped>
.water-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: calc(100vh - 220px);
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.water-detail__section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--td-text-color-primary);
  margin-bottom: 8px;
}

.water-detail__section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;

  .water-detail__section-title {
    margin-bottom: 8px;
  }
}

.water-detail__algo-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: var(--td-text-color-secondary);
  margin-bottom: 8px;
}

.water-detail__algo-summary--empty {
  color: var(--td-text-color-placeholder);
}

.water-detail__algo-extra {
  color: var(--td-text-color-primary);
  font-weight: 600;
}

.water-detail__algo-hint {
  font-size: 12px;
  color: var(--td-brand-color);
  margin-bottom: 8px;
}

.water-detail__algo-error {
  margin-bottom: 8px;
}

.water-detail__evidence {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 10px;
}

.water-detail__card {
  border: 1px solid var(--td-component-border);
  border-radius: var(--td-radius-medium);
  overflow: hidden;
}

.water-detail__img {
  width: 100%;
  height: 100px;
  cursor: pointer;
  display: block;
}

.water-detail__img--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--td-bg-color-secondarycontainer);
  color: var(--td-text-color-placeholder);
  font-size: 12px;
  cursor: default;
}

.water-detail__card-foot {
  padding: 6px 8px;
  font-size: 12px;
  color: var(--td-text-color-secondary);
  display: flex;
  justify-content: space-between;
}
</style>
