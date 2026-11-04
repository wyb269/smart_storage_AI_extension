<template>
  <t-dialog
    :visible="visible"
    :header="`识别记录详情 - ${detail?.plateNo || eventId}`"
    width="760px"
    :footer="false"
    @update:visible="emit('update:visible', $event)"
  >
    <t-loading :loading="loading">
      <div v-if="detail" class="vehicle-detail">
        <t-descriptions :column="2" item-layout="horizontal" size="small" bordered>
          <t-descriptions-item label="事件编号">{{ detail.eventNo }}</t-descriptions-item>
          <t-descriptions-item label="车牌号">{{ detail.plateNo }}</t-descriptions-item>
          <t-descriptions-item label="车辆类型">{{ detail.vehicleType || '—' }}</t-descriptions-item>
          <t-descriptions-item label="车身颜色">{{ detail.vehicleColor || '—' }}</t-descriptions-item>
          <t-descriptions-item label="行驶方向">
            {{ detail.directionName || DIRECTION_LABEL[detail.direction] }}
          </t-descriptions-item>
          <t-descriptions-item label="识别地点">{{ detail.gateName }}</t-descriptions-item>
          <t-descriptions-item label="识别时间">{{ detail.recognizedAt }}</t-descriptions-item>
          <t-descriptions-item label="仓库">{{ detail.warehouseName }}</t-descriptions-item>
          <t-descriptions-item label="核实状态">
            <t-tag :theme="VERIFY_STATUS_THEME[detail.verifyStatus]" variant="light" size="small">
              {{ VERIFY_STATUS_LABEL[detail.verifyStatus] }}
            </t-tag>
          </t-descriptions-item>
          <t-descriptions-item label="算法状态">
            {{ ALGORITHM_STATUS_LABEL[detail.algorithmStatus] || detail.algorithmStatus || '—' }}
          </t-descriptions-item>
          <t-descriptions-item label="识别置信度">
            {{ confidenceText }}
          </t-descriptions-item>
          <t-descriptions-item label="摄像头">{{ detail.cameraCode || '—' }}</t-descriptions-item>
          <t-descriptions-item label="事件描述" :span="2">
            {{ detail.eventDesc || '—' }}
          </t-descriptions-item>
        </t-descriptions>

        <div class="vehicle-detail__section">
          <div class="vehicle-detail__section-title">证据</div>
          <evidence-illustration kind="vehicleRecognition" />
        </div>

        <!-- ==================== 算法任务 ==================== -->
        <div class="vehicle-detail__section">
          <div class="vehicle-detail__section-head">
            <div class="vehicle-detail__section-title">算法任务</div>
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

          <!-- 任务摘要只展示经过核实的模型信息 -->
          <div v-if="latestTask" class="vehicle-detail__algo-summary">
            <span>{{ algorithmSummaryText }}</span>
          </div>
          <t-empty v-else description="暂无算法任务" size="small" />

          <!-- 行内提示：受理 / 完成 / 失败原因都在这里，不吞掉 errorMessage -->
          <div v-if="rerunHint" class="vehicle-detail__algo-hint">
            {{ rerunHint }}
            <span v-if="latestTask && latestTask.errorMessage" class="vehicle-detail__algo-error-text">
              （{{ displayTaskError(latestTask) }}）
            </span>
          </div>
          <t-alert
            v-if="
              latestTask &&
              (latestTask.executionStatus === 'FAILED' || latestTask.executionStatus === 'TIMEOUT') &&
              (latestTask.errorMessage || latestTask.errorCode)
            "
            theme="error"
            size="small"
            :message="displayTaskError(latestTask)"
            class="vehicle-detail__algo-error"
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
            <template #confidence="{ row }">{{ confidenceCellText(row.confidence) }}</template>
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

        <div v-if="detail.processRecords?.length" class="vehicle-detail__section">
          <div class="vehicle-detail__section-title">核实记录</div>
          <process-record-list :records="detail.processRecords" />
        </div>
      </div>
    </t-loading>
  </t-dialog>
</template>
<script setup lang="ts">
import { MessagePlugin } from 'tdesign-vue-next';
import { computed, onBeforeUnmount, ref, watch } from 'vue';

import { getAlgorithmTask, rerunAlgorithm } from '@/api/vehicleRecognition';

import {
  displayModelVersion,
  displayTaskError,
  displayTaskNumber,
  displayTaskSummary,
} from '../../shared/algorithmTaskDisplay';
import { useRequestRaceGuard } from '../../shared/composables/useRequestRaceGuard';
import EvidenceIllustration from '../../shared/EvidenceIllustration.vue';
import ProcessRecordList from '../../shared/ProcessRecordList.vue';
import { ALGORITHM_STATUS_LABEL, DIRECTION_LABEL, VERIFY_STATUS_LABEL, VERIFY_STATUS_THEME } from '../constants';
import type { AlgorithmStatus, AlgorithmTask, VehicleEventDetail } from '../types';

/**
 * 识别记录详情弹窗。
 *
 * 打开详情即由父组件调用 markEventViewed 标记为已核实，
 * 本组件不承载任何写操作，也不出现独立的「处理」写接口 ——
 * 产品口径与遮挡一致：查看即标记。
 *
 * 「重新识别」是取证动作（权限 VIEW）：提交后立即返回任务号，
 * 本组件按 800ms 间隔轮询任务状态，最多 20 次（约 16 秒），
 * 拿到终态后请求父组件重拉详情并刷新外层列表；
 * 定时器在组件卸载与弹窗关闭时都必须清掉（竞态守卫 + clearTimeout，不裸 setInterval）。
 *
 * 车辆特例：事件表没有 algorithm_result 列，结论由任务行给出，
 * 结论类型字段是 vehicleType；任务只带车牌掩码，本组件不渲染任何车牌明文。
 *
 * 预览组件用 t-image-viewer + :images；
 * t-image-preview 与 :preview-list 在 TDesign 中不存在，会报组件解析失败。
 */
const props = defineProps<{
  visible: boolean;
  eventId: string;
  detail: VehicleEventDetail | null;
  loading?: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  /** 轮询到终态后请求父组件重拉详情 + 刷新外层列表 */
  (e: 'refresh'): void;
}>();

/** 置信度 null 显示「暂无」，不得渲染成 0% */
const confidenceText = computed(() => {
  const c = props.detail?.confidence;
  if (c === null || c === undefined) return '暂无';
  return `${(Number(c) * 100).toFixed(2)}%`;
});

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
  latestTask.value ? displayTaskSummary(latestTask.value, '车辆智能识别') : '',
);

const ALGORITHM_TASK_COLUMNS = [
  { title: '任务号', colKey: 'taskNo', cell: 'taskNo', ellipsis: true },
  { title: '状态', colKey: 'status', cell: 'status', width: 96 },
  { title: '置信度', colKey: 'confidence', cell: 'confidence', width: 84 },
  { title: '模型版本', colKey: 'modelVersion', cell: 'modelVersion', ellipsis: true, width: 110 },
  { title: '耗时', colKey: 'duration', cell: 'duration', width: 78 },
  { title: '时间', colKey: 'time', cell: 'time', width: 160 },
];

/** 状态用色：SUCCEEDED 绿、RUNNING/QUEUED 蓝且带「进行中」、FAILED/TIMEOUT 红 */
function taskStatusTheme(status: AlgorithmStatus): 'default' | 'success' | 'primary' | 'danger' {
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
function confidenceCellText(value: unknown): string {
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

function scheduleNextPoll(taskId: string): void {
  stopPollingOnlyTimer();
  pollTimer = setTimeout(() => {
    void doPoll(taskId);
  }, RERUN_POLL_INTERVAL_MS);
}

function stopPollingOnlyTimer(): void {
  if (pollTimer !== null) {
    clearTimeout(pollTimer);
    pollTimer = null;
  }
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
  ([visible], [, prevEventId]) => {
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
.vehicle-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: calc(100vh - 220px);
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.vehicle-detail__section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--td-text-color-primary);
  margin-bottom: 8px;
}

.vehicle-detail__section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;

  .vehicle-detail__section-title {
    margin-bottom: 8px;
  }
}

.vehicle-detail__algo-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: var(--td-text-color-secondary);
  margin-bottom: 8px;
}

.vehicle-detail__algo-hint {
  font-size: 12px;
  color: var(--td-brand-color);
  margin-bottom: 8px;
}

.vehicle-detail__algo-error-text {
  color: var(--td-error-color);
}

.vehicle-detail__algo-error {
  margin-bottom: 8px;
}

.vehicle-detail__evidence {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 10px;
}

.vehicle-detail__card {
  border: 1px solid var(--td-component-border);
  border-radius: var(--td-radius-medium);
  overflow: hidden;
}

.vehicle-detail__img {
  width: 100%;
  height: 100px;
  cursor: pointer;
  display: block;
}

.vehicle-detail__img--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--td-bg-color-secondarycontainer);
  color: var(--td-text-color-placeholder);
  font-size: 12px;
  cursor: default;
}

.vehicle-detail__card-foot {
  padding: 6px 8px;
  font-size: 12px;
  color: var(--td-text-color-secondary);
  display: flex;
  justify-content: space-between;
}
</style>
