<template>
  <t-dialog
    :visible="visible"
    :header="`皮带机下站人事件详情 - ${detail?.eventNo || eventId}`"
    width="760px"
    :footer="false"
    @update:visible="emit('update:visible', $event)"
  >
    <t-loading :loading="loading">
      <div v-if="detail" class="belt-detail">
        <t-descriptions :column="2" item-layout="horizontal" size="small" bordered>
          <t-descriptions-item label="事件编号">{{ detail.eventNo }}</t-descriptions-item>
          <t-descriptions-item label="发生时间">{{ detail.occurredAt }}</t-descriptions-item>
          <t-descriptions-item label="仓库">{{ detail.warehouseName }}</t-descriptions-item>
          <t-descriptions-item label="皮带机">{{ detail.deviceName }}</t-descriptions-item>
          <t-descriptions-item label="摄像头">{{ detail.cameraCode || '—' }}</t-descriptions-item>
          <t-descriptions-item label="结论类型">
            {{ detail.conclusionTypeName || detail.conclusionType }}
          </t-descriptions-item>
          <t-descriptions-item label="检出人数">
            {{ detail.personCount === null || detail.personCount === undefined ? '暂无' : detail.personCount }}
          </t-descriptions-item>
          <t-descriptions-item label="处理状态">
            <t-tag :theme="PROCESS_STATUS_THEME[detail.processStatus]" variant="light" size="small">
              {{ PROCESS_STATUS_LABEL[detail.processStatus] }}
            </t-tag>
          </t-descriptions-item>
          <t-descriptions-item label="算法结论">
            {{ detail.algorithmResult ? ALGORITHM_RESULT_LABEL[detail.algorithmResult] : '暂无' }}
          </t-descriptions-item>
          <t-descriptions-item label="事件描述" :span="2">
            {{ detail.eventDesc || '—' }}
          </t-descriptions-item>
        </t-descriptions>

        <div class="belt-detail__section">
          <div class="belt-detail__section-title">证据</div>
          <evidence-illustration kind="beltMachine" />
        </div>

        <!-- ==================== 算法任务 ==================== -->
        <div class="belt-detail__section">
          <div class="belt-detail__task-head">
            <div class="belt-detail__section-title belt-detail__section-title--inline">算法任务</div>
            <div class="belt-detail__task-actions">
              <t-tooltip v-if="rerunDisabledReason" :content="rerunDisabledReason" placement="top">
                <t-button theme="primary" variant="outline" size="small" :disabled="true">
                  <template #icon><t-icon name="refresh" /></template>
                  重新识别
                </t-button>
              </t-tooltip>
              <t-button
                v-else
                theme="primary"
                variant="outline"
                size="small"
                :disabled="rerunDisabled"
                :loading="rerunning"
                @click="handleRerun"
              >
                <template #icon><t-icon name="refresh" /></template>
                重新识别
              </t-button>
            </div>
          </div>

          <!-- 行内提示：受理 / 完成 / 失败原因都在这里，不吞掉 errorMessage -->
          <div v-if="rerunHint" class="belt-detail__rerun-hint">
            {{ rerunHint }}
            <span v-if="latestTask && latestTask.errorMessage" class="belt-detail__rerun-error">
              （{{ displayTaskError(latestTask) }}）
            </span>
          </div>

          <!-- 最近一次任务摘要：未核实的模型元数据不展示 -->
          <div v-if="latestTask" class="belt-detail__task-summary">
            <span>{{ displayTaskSummary(latestTask, '皮带机下站人检测') }}</span>
          </div>
          <t-empty v-else description="暂无算法任务" size="small" />

          <t-table
            v-if="taskRows.length"
            class="belt-detail__task-table"
            row-key="taskNo"
            :data="taskRows"
            :columns="taskColumns"
            size="small"
            :hover="true"
            table-layout="fixed"
          >
            <template #executionStatus="{ row }">
              <t-tag :theme="taskStatusTheme(row.executionStatus)" variant="light" size="small">
                {{ taskStatusLabelOf(row.executionStatus) }}
              </t-tag>
            </template>
            <template #confidence="{ row }">
              {{ row.confidence == null ? '暂无' : `${(row.confidence * 100).toFixed(2)}%` }}
            </template>
            <template #taskNo="{ row }">{{ displayTaskNumber(row.taskNo) }}</template>
            <template #modelVersion="{ row }">{{ displayModelVersion(row.modelVersion, row.simulated) }}</template>
            <template #duration="{ row }">
              {{ row.inferenceDurationMs == null ? '—' : `${row.inferenceDurationMs}ms` }}
            </template>
            <template #time="{ row }">
              {{ row.startedAt || row.finishedAt || '—' }}
            </template>
          </t-table>
        </div>

        <div v-if="detail.processRecords?.length" class="belt-detail__section">
          <div class="belt-detail__section-title">处理记录</div>
          <process-record-list :records="detail.processRecords" />
        </div>
      </div>
    </t-loading>
  </t-dialog>
</template>
<script setup lang="ts">
import type { PrimaryTableCol } from 'tdesign-vue-next';
import { MessagePlugin } from 'tdesign-vue-next';
import { computed, onUnmounted, ref, watch } from 'vue';

import { getAlgorithmTask, rerunAlgorithm } from '@/api/beltMachine';

import {
  displayModelVersion,
  displayTaskError,
  displayTaskNumber,
  displayTaskSummary,
} from '../../shared/algorithmTaskDisplay';
import { useRequestRaceGuard } from '../../shared/composables/useRequestRaceGuard';
import EvidenceIllustration from '../../shared/EvidenceIllustration.vue';
import ProcessRecordList from '../../shared/ProcessRecordList.vue';
import {
  ALGORITHM_RESULT_LABEL,
  ALGORITHM_STATUS_LABEL,
  PROCESS_STATUS_LABEL,
  PROCESS_STATUS_THEME,
} from '../constants';
import type { AlgorithmStatus, AlgorithmTask, BeltMachineEventDetail } from '../types';

/**
 * 皮带机下站人事件详情弹窗。
 *
 * 打开详情即由父组件调用 markEventViewed 标记已处理，
 * 本组件不承载处理写操作；「重新识别」是唯一的写链路，
 * 它只触发算法重新取证（只写事件算法列），不产生人工结论，
 * 因此不与「查看即标记」的产品口径冲突。
 *
 * 重新识别交互（契约 §3）：
 *   点击 → 禁用按钮 + 行内提示「已提交算法任务」→ rerunAlgorithm 拿 taskId
 *   → 每 800ms 轮询 algorithmTask，最多 20 次（约 16 秒）
 *   → 到终态后 emit('algorithm-finished')，由父层重拉详情并刷新外层列表；
 *     超限未到终态则提示「算法仍在执行，可稍后刷新」。
 *   定时器与竞态令牌在卸载/关闭弹窗时清理，不留裸 setInterval。
 *
 * 预览组件用 t-image-viewer + :images；
 * t-image-preview 与 :preview-list 在 TDesign 中不存在，会报组件解析失败。
 */
const props = defineProps<{
  visible: boolean;
  eventId: string;
  detail: BeltMachineEventDetail | null;
  loading?: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  /** 重新识别到达终态（或明确失败）后，父层重新拉取详情并刷新外层列表 */
  (e: 'algorithm-finished', eventId: string): void;
}>();

/* ==================== 重新识别状态机 ==================== */

const RERUN_POLL_INTERVAL_MS = 800;
const RERUN_POLL_MAX_TIMES = 20;

const TERMINAL_STATUS: ReadonlySet<string> = new Set(['SUCCEEDED', 'FAILED', 'TIMEOUT', 'SKIPPED', 'MOCK']);
const IN_PROGRESS_STATUS: ReadonlySet<string> = new Set(['QUEUED', 'RUNNING']);

/** 轮询/提交请求的竞态守卫：关闭弹窗或切换事件后旧轮询不得再写状态 */
const rerunGuard = useRequestRaceGuard();

const rerunning = ref(false);
const rerunHint = ref('');
/** 轮询到的最新任务快照：立即并入表格展示，不等父层重拉详情 */
const liveTask = ref<AlgorithmTask | null>(null);
let rerunTimer: ReturnType<typeof setTimeout> | null = null;

/**
 * 未部署点位（无摄像头编码快照）禁用「重新识别」并给原因，
 * 沿用取流接口 CAMERA_NOT_BOUND 的语义：没有媒体输入，
 * 跑算法只会产出伪造结论，后端同样会拒绝。
 */
const rerunDisabledReason = computed(() =>
  props.detail && !props.detail.cameraCode ? '该皮带机未绑定监测摄像头（CAMERA_NOT_BOUND），无媒体输入可重新识别' : '',
);

const rerunDisabled = computed(() => Boolean(rerunDisabledReason.value) || rerunning.value);

/** 详情随包任务 + 轮询快照（同 taskNo 覆盖，新任务置顶） */
const taskRows = computed<AlgorithmTask[]>(() => {
  const stored = props.detail?.algorithmTasks ?? [];
  const live = liveTask.value;
  if (!live) return stored;
  const idx = stored.findIndex((t) => t.taskNo === live.taskNo);
  if (idx === -1) return [live, ...stored];
  return stored.map((t) => (t.taskNo === live.taskNo ? live : t));
});

/** 摘要行看最新一条：表格行序与后端 listAlgorithmTasks 一致（新在前） */
const latestTask = computed<AlgorithmTask | null>(() => taskRows.value[0] ?? null);

function taskStatusLabelOf(status: AlgorithmStatus): string {
  const label = ALGORITHM_STATUS_LABEL[status] || status;
  return IN_PROGRESS_STATUS.has(status) ? `${label} · 进行中` : label;
}

/** 契约用色：SUCCEEDED 绿、RUNNING/QUEUED 蓝、FAILED/TIMEOUT 红 */
function taskStatusTheme(status: AlgorithmStatus): 'success' | 'primary' | 'danger' | 'default' {
  if (status === 'SUCCEEDED') return 'success';
  if (IN_PROGRESS_STATUS.has(status)) return 'primary';
  if (status === 'FAILED' || status === 'TIMEOUT') return 'danger';
  return 'default';
}

const taskColumns: PrimaryTableCol[] = [
  { colKey: 'taskNo', title: '任务号', cell: 'taskNo', width: 200, ellipsis: true },
  { colKey: 'executionStatus', title: '状态', width: 130, cell: 'executionStatus' },
  { colKey: 'confidence', title: '置信度', width: 90, cell: 'confidence' },
  { colKey: 'modelVersion', title: '模型版本', cell: 'modelVersion', width: 110, ellipsis: true },
  { colKey: 'duration', title: '耗时', width: 80, cell: 'duration' },
  { colKey: 'time', title: '时间', width: 160, cell: 'time', ellipsis: true },
];

function clearRerunTimer() {
  if (rerunTimer !== null) {
    clearTimeout(rerunTimer);
    rerunTimer = null;
  }
}

/** 停止轮询并释放按钮：弹窗关闭、事件切换、组件卸载共用 */
function stopRerun() {
  clearRerunTimer();
  rerunGuard.invalidate();
  rerunning.value = false;
}

async function handleRerun() {
  if (rerunDisabled.value || !props.eventId) return;
  const token = rerunGuard.begin();
  const eventId = props.eventId;
  rerunning.value = true;
  rerunHint.value = '已提交算法任务';
  liveTask.value = null;
  try {
    const accepted = await rerunAlgorithm(eventId);
    if (!rerunGuard.isCurrent(token)) return;
    if (!accepted.taskId) {
      rerunning.value = false;
      rerunHint.value = '';
      MessagePlugin.error('重新识别受理失败：未返回任务标识');
      return;
    }
    pollAlgorithmTask(accepted.taskId, token, eventId, 1);
  } catch (error: any) {
    if (!rerunGuard.isCurrent(token)) return;
    rerunning.value = false;
    rerunHint.value = '';
    MessagePlugin.error(error?.message ?? '重新识别提交失败');
  }
}

/**
 * 递归 setTimeout 轮询（非 setInterval）：
 * 每拍 800ms，最多 20 次；令牌失效或组件卸载即静默终止。
 */
function pollAlgorithmTask(taskNo: string, token: number, eventId: string, times: number) {
  clearRerunTimer();
  rerunTimer = setTimeout(async () => {
    rerunTimer = null;
    if (!rerunGuard.isCurrent(token)) return;
    if (times > RERUN_POLL_MAX_TIMES) {
      rerunning.value = false;
      rerunHint.value = '算法仍在执行，可稍后刷新';
      return;
    }
    let task: AlgorithmTask | null = null;
    try {
      task = await getAlgorithmTask(taskNo);
    } catch (error: any) {
      if (!rerunGuard.isCurrent(token)) return;
      rerunning.value = false;
      rerunHint.value = '';
      MessagePlugin.error(error?.message ?? '算法任务查询失败');
      return;
    }
    if (!rerunGuard.isCurrent(token)) return;
    liveTask.value = task;
    if (TERMINAL_STATUS.has(task.executionStatus)) {
      rerunning.value = false;
      if (task.executionStatus === 'SUCCEEDED') {
        rerunHint.value = '重新识别完成';
      } else {
        // 失败/超时态也要正常展示，不吞掉原因（原因拼在提示行里）
        rerunHint.value = `重新识别${ALGORITHM_STATUS_LABEL[task.executionStatus] || task.executionStatus}`;
      }
      emit('algorithm-finished', eventId);
      return;
    }
    pollAlgorithmTask(taskNo, token, eventId, times + 1);
  }, RERUN_POLL_INTERVAL_MS);
}

/* 弹窗关闭 / 事件切换：终止上一轮轮询，防止旧任务状态串到新事件 */
watch(
  () => [props.visible, props.eventId] as const,
  ([nextVisible, nextEventId], [_prevVisible, prevEventId]) => {
    if (!nextVisible || nextEventId !== prevEventId) {
      stopRerun();
      if (nextEventId !== prevEventId) {
        rerunHint.value = '';
        liveTask.value = null;
      }
    }
  },
);

onUnmounted(() => {
  clearRerunTimer();
});
</script>
<style lang="less" scoped>
.belt-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: calc(100vh - 220px);
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.belt-detail__section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--td-text-color-primary);
  margin-bottom: 8px;
}

.belt-detail__section-title--inline {
  margin-bottom: 0;
}

.belt-detail__task-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.belt-detail__task-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.belt-detail__rerun-hint {
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--td-text-color-secondary);
}

.belt-detail__rerun-error {
  color: var(--td-error-color);
}

.belt-detail__task-summary {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--td-text-color-secondary);
}

.belt-detail__task-table {
  margin-bottom: 4px;
}

.belt-detail__evidence {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 10px;
}

.belt-detail__card {
  border: 1px solid var(--td-component-border);
  border-radius: var(--td-radius-medium);
  overflow: hidden;
}

.belt-detail__img {
  width: 100%;
  height: 100px;
  cursor: pointer;
  display: block;
}

.belt-detail__img--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--td-bg-color-secondarycontainer);
  color: var(--td-text-color-placeholder);
  font-size: 12px;
  cursor: default;
}

.belt-detail__card-foot {
  padding: 6px 8px;
  font-size: 12px;
  color: var(--td-text-color-secondary);
  display: flex;
  justify-content: space-between;
  gap: 6px;
}
</style>
