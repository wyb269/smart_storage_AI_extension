<template>
  <!--
    注意：footer 必须显式为 true。
    TDesign 的 dialog-card 渲染条件是 `!!props.footer && renderFooter()`
    （node_modules/tdesign-vue-next/es/dialog/dialog-card.mjs），
    传 false 会让下方 #footer 插槽整体不渲染，
    导致「关闭」与「去处理」按钮全部消失。
    （注释须放在标签外，Vue 模板解析器不允许标签属性中出现注释）
  -->
  <t-dialog
    :visible="visible"
    header="仓门异动事件详情"
    width="880px"
    :footer="true"
    class="event-detail-dialog"
    @update:visible="handleVisibleChange"
  >
    <t-loading v-if="loading" style="padding: 48px 0" />

    <t-empty v-else-if="!detail" description="未找到事件详情" />

    <div v-else class="detail">
      <!-- ============ 基础信息（第17.2节） ============ -->
      <section class="detail__section">
        <h4 class="detail__section-title">基础信息</h4>
        <t-descriptions :column="2" size="small" bordered>
          <t-descriptions-item label="事件编号">{{ detail.eventNo }}</t-descriptions-item>
          <t-descriptions-item label="发生时间">{{ formatTime(detail.occurredAt) }}</t-descriptions-item>
          <t-descriptions-item label="仓库名称">{{ detail.warehouseName }}</t-descriptions-item>
          <t-descriptions-item label="仓门位置">{{ detail.doorName }}</t-descriptions-item>
          <t-descriptions-item label="摄像头编号">{{ detail.cameraCode || '-' }}</t-descriptions-item>
          <t-descriptions-item label="异动类型">
            {{ detail.anomalyTypeName || detail.anomalyType }}
          </t-descriptions-item>
          <t-descriptions-item label="算法结论">
            <!-- 算法失败/未执行时不得显示为“正常” -->
            <template v-if="hasAlgorithmConclusion">
              <t-tag :theme="algorithmResultTheme" variant="light" size="small">
                {{ algorithmResultLabel }}
              </t-tag>
            </template>
            <span v-else class="detail__muted">-</span>
          </t-descriptions-item>
          <t-descriptions-item label="处理状态">
            <t-tag :theme="processStatusTheme" variant="light" size="small">
              {{ processStatusLabel }}
            </t-tag>
          </t-descriptions-item>
        </t-descriptions>
        <p v-if="detail.eventDesc" class="detail__desc">{{ detail.eventDesc }}</p>
      </section>

      <!-- ============ 算法摘要（第17.3节） ============ -->
      <section class="detail__section">
        <h4 class="detail__section-title">算法识别</h4>

        <!-- 算法未完成或失败时展示对应状态 -->
        <t-alert
          v-if="!algorithmSucceeded"
          :theme="algorithmAlertTheme"
          :message="algorithmAlertText"
          class="detail__alert"
        />

        <t-descriptions v-if="algorithmSucceeded" :column="2" size="small" bordered>
          <t-descriptions-item label="识别结论">
            <t-tag :theme="algorithmResultTheme" variant="light" size="small">
              {{ algorithmResultLabel }}
            </t-tag>
          </t-descriptions-item>
          <t-descriptions-item label="置信度">
            <!-- 置信度缺失显示“暂无”，不得显示为 0%（第17.3节） -->
            <template v-if="hasConfidence">{{ confidenceText }}</template>
            <span v-else class="detail__muted">暂无</span>
          </t-descriptions-item>
          <t-descriptions-item label="模型">
            {{ modelDisplayName }}
          </t-descriptions-item>
          <t-descriptions-item label="模型版本">
            {{ displayModelVersion(detail.modelVersion, detail.isSimulated) }}
          </t-descriptions-item>
          <t-descriptions-item label="算法程序版本">
            {{ algorithmProgramVersion }}
          </t-descriptions-item>
          <t-descriptions-item v-if="!detail.isSimulated" label="配置版本">{{
            detail.configVersion || '-'
          }}</t-descriptions-item>
          <t-descriptions-item label="推理耗时">
            {{ detail.inferenceDurationMs != null ? `${detail.inferenceDurationMs} ms` : '-' }}
          </t-descriptions-item>
        </t-descriptions>
      </section>

      <!-- ============ 算法任务（重新识别 + 历史任务） ============ -->
      <section class="detail__section">
        <div class="detail__section-head">
          <h4 class="detail__section-title">算法任务</h4>
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
        <div v-if="latestTask" class="detail__algo-summary">
          <span>{{ algorithmSummaryText }}</span>
        </div>

        <!-- 行内提示：提交成功 / 轮询超时 / 失败信息；错误与超时文案不被吞掉 -->
        <div v-if="rerunHint" class="detail__algo-hint">{{ rerunHint }}</div>
        <t-alert
          v-if="
            latestTask &&
            (latestTask.executionStatus === 'FAILED' || latestTask.executionStatus === 'TIMEOUT') &&
            (latestTask.errorMessage || latestTask.errorCode)
          "
          theme="error"
          size="small"
          :message="displayTaskError(latestTask)"
          class="detail__algo-error"
        />

        <t-empty v-if="displayTasks.length === 0" description="暂无算法任务" size="small" />

        <t-table
          v-else
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
          <template #confidence="{ row }">{{ taskConfidenceText(row.confidence) }}</template>
          <template #taskNo="{ row }">{{ displayTaskNumber(row.taskNo) }}</template>
          <template #modelVersion="{ row }">{{ displayModelVersion(row.modelVersion, row.simulated) }}</template>
          <template #duration="{ row }">
            {{
              row.inferenceDurationMs === null || row.inferenceDurationMs === undefined
                ? '—'
                : `${row.inferenceDurationMs}ms`
            }}
          </template>
          <template #time="{ row }">{{ formatTime(row.startedAt) }}</template>
        </t-table>
      </section>

      <!-- ============ 证据区域（第17.4节） ============ -->
      <section class="detail__section">
        <h4 class="detail__section-title">识别证据</h4>

        <evidence-illustration kind="doorAnomaly" />
      </section>

      <!-- ============ 处理记录（第17.6节） ============ -->
      <section class="detail__section">
        <h4 class="detail__section-title">处理记录</h4>

        <t-empty v-if="!detail.processRecords || detail.processRecords.length === 0" description="暂无处理记录" />

        <div v-else class="detail__records">
          <div v-for="rec in detail.processRecords" :key="rec.recordId" class="detail__record">
            <time class="detail__record-time">{{ formatTime(rec.processedAt) }}</time>
            <div class="record">
              <div class="record__head">
                <t-tag :theme="processStatusThemeOf(rec.statusAfter)" variant="light" size="small">
                  {{ processStatusLabelOf(rec.statusAfter) }}
                </t-tag>
                <span class="record__flow">
                  {{ processStatusLabelOf(rec.statusBefore) }} → {{ processStatusLabelOf(rec.statusAfter) }}
                </span>
              </div>
              <!-- 处理意见按普通文本输出，不作为 HTML 渲染（第29.4节） -->
              <p class="record__opinion">{{ rec.processOpinion }}</p>
              <p class="record__by">处理人：{{ rec.processedBy }}</p>
            </div>
          </div>
        </div>
      </section>
    </div>

    <template #footer>
      <div class="detail__footer">
        <span v-if="detail" class="detail__footer-hint"> 事件版本 {{ detail.eventVersion }} </span>
        <div>
          <!-- 2026-10-21 取消人工处理入口：打开详情即由页面负责标记为已处理，弹窗只留关闭 -->
          <t-button theme="default" @click="handleVisibleChange(false)">关闭</t-button>
        </div>
      </div>
    </template>

    <!-- 图片放大预览：TDesign 的组件是 image-viewer（images 传 url 数组），
         仓库内既有用法见 entry/wareHouseIndex/components/uploadCom.vue -->
  </t-dialog>
</template>
<script setup lang="ts">
import { MessagePlugin } from 'tdesign-vue-next';
import { computed, onBeforeUnmount, ref, watch } from 'vue';

import { getAlgorithmTask, rerunAlgorithm } from '@/api/doorAnomaly';

import {
  displayModelValue,
  displayModelVersion,
  displayTaskError,
  displayTaskNumber,
  displayTaskSummary,
} from '../../shared/algorithmTaskDisplay';
import { useRequestRaceGuard } from '../../shared/composables/useRequestRaceGuard';
import EvidenceIllustration from '../../shared/EvidenceIllustration.vue';
import {
  ALGORITHM_RESULT_LABEL,
  ALGORITHM_RESULT_THEME,
  ALGORITHM_STATUS_LABEL,
  PROCESS_STATUS_LABEL,
  PROCESS_STATUS_THEME,
} from '../constants';
import type { AlgorithmResult, AlgorithmStatus, AlgorithmTask, EventDetail, ProcessStatus } from '../types';

/**
 * 事件详情弹窗（只读）。
 *
 * 依据 AI_HANDOFF.md 第17节：
 * - 详情必须回答：何时、何地、发生了什么、算法为什么判为异常、
 *   有什么证据、人工如何处理
 * - 采用只读设计，人工处理使用独立弹窗
 * - 算法失败不得伪装为正常，置信度缺失显示“暂无”而非 0%
 * - 原始证据图不可被算法标注覆盖
 * - 处理历史为追加记录
 *
 * 本组件由监测首页与历史记录页**共用**，通过 eventId prop 驱动。
 */
const props = withDefaults(
  defineProps<{
    visible: boolean;
    eventId?: string;
    detail?: EventDetail | null;
    loading?: boolean;
  }>(),
  {
    eventId: '',
    detail: null,
    loading: false,
  },
);

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  /** 轮询到终态后请求父组件重拉详情 + 刷新外层列表 */
  (e: 'refresh'): void;
}>();

/* ---------- 算法状态展示（第17.5节） ---------- */

/** 算法是否已成功产出结论 */
const algorithmSucceeded = computed(() => detailAlgoStatus.value === 'SUCCEEDED' || detailAlgoStatus.value === 'MOCK');

const detailAlgoStatus = computed(() => props.detail?.algorithmStatus ?? 'QUEUED');

/** 算法成功时的提示文案，MOCK 需明确标注（第17.5节） */
const algorithmAlertText = computed(() => {
  const status = detailAlgoStatus.value;
  switch (status) {
    case 'QUEUED':
      return '等待算法分析';
    case 'RUNNING':
      return '算法正在识别中，请稍候';
    case 'FAILED':
      return '算法识别失败，未产出结论。请人工核实。';
    case 'TIMEOUT':
      return '算法识别超时，未产出结论。请人工核实。';
    case 'SKIPPED':
      return '该事件无需算法识别（如纯设备离线事件）';
    default:
      return ALGORITHM_STATUS_LABEL[status as keyof typeof ALGORITHM_STATUS_LABEL] ?? '未知状态';
  }
});

const algorithmAlertTheme = computed<'error' | 'info' | 'warning'>(() => {
  const status = detailAlgoStatus.value;
  if (status === 'FAILED' || status === 'TIMEOUT') return 'error';
  if (status === 'SKIPPED') return 'info';
  return 'warning';
});

/** 是否已有算法结论；MOCK 也视为有结论但需标注 */
const hasAlgorithmConclusion = computed(() => algorithmSucceeded.value && !!props.detail?.algorithmResult);

const algorithmResultLabel = computed(() => {
  const r = props.detail?.algorithmResult as AlgorithmResult | undefined;
  return r ? ALGORITHM_RESULT_LABEL[r] : '-';
});

const algorithmResultTheme = computed(() => {
  const r = props.detail?.algorithmResult as AlgorithmResult | undefined;
  return r ? ALGORITHM_RESULT_THEME[r] : ('default' as const);
});

/**
 * 置信度是否缺失。
 * 区分「无值」与「0」：0 是有效置信度，null 才是缺失。
 */
const hasConfidence = computed(() => {
  const c = props.detail?.confidence;
  return c !== null && c !== undefined && !Number.isNaN(Number(c));
});

const confidenceText = computed(() => {
  const c = Number(props.detail?.confidence);
  return `${(c * 100).toFixed(1)}%`;
});

/* ---------- 处理状态 ---------- */

const processStatusLabel = computed(() => {
  const s = props.detail?.processStatus as ProcessStatus | undefined;
  return s ? PROCESS_STATUS_LABEL[s] : '-';
});

const processStatusTheme = computed(() => {
  const s = props.detail?.processStatus as ProcessStatus | undefined;
  return s ? PROCESS_STATUS_THEME[s] : ('default' as const);
});

function processStatusLabelOf(status?: ProcessStatus): string {
  return status ? PROCESS_STATUS_LABEL[status] : '-';
}

function processStatusThemeOf(status?: ProcessStatus) {
  return status ? PROCESS_STATUS_THEME[status] : ('default' as const);
}

/* ---------- 证据 ---------- */

/* ==================== 算法任务区（「重新识别」） ==================== */

/** 轮询节奏与上限：800ms × 最多 20 次 ≈ 16 秒，与虫害功能完全一致 */
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
/** 轮询/提交竞态守卫：关闭弹窗或切换事件后旧结果不得再写状态 */
const pollGuard = useRequestRaceGuard();

/** 展示用任务列表：实时任务置顶 + 详情带回的历史（按 taskNo 去重） */
const displayTasks = computed<AlgorithmTask[]>(() => {
  const base = props.detail?.algorithmTasks ?? [];
  const live = liveTask.value;
  if (live) {
    return [live, ...base.filter((t) => t.taskNo !== live.taskNo)];
  }
  return base;
});

const latestTask = computed<AlgorithmTask | null>(
  () => liveTask.value ?? props.detail?.latestAlgorithmTask ?? displayTasks.value[0] ?? null,
);

const modelDisplayName = computed(() => {
  const reported = displayModelValue(props.detail?.modelName, props.detail?.isSimulated);
  return reported === '—' ? '仓门异动检测' : reported;
});

const algorithmProgramVersion = computed(() => {
  const raw = props.detail?.algorithmVersion || latestTask.value?.algorithmVersion;
  const version = raw?.trim().replace(/^mock[-_.]?(adapter[-_.]?)?/i, '');
  if (version && !/mock|模拟|演示|测试/i.test(version)) return version;
  return displayModelVersion(props.detail?.modelVersion || latestTask.value?.modelVersion, true);
});

/** 摘要行：模型 <name> <version> · 算法 <v> · 配置 <v> · 推理 <ms>ms · 检出 <n> 个目标 */
const algorithmSummaryText = computed(() =>
  latestTask.value ? displayTaskSummary(latestTask.value, '仓门异动检测') : '',
);

const ALGORITHM_TASK_COLUMNS = [
  { title: '任务号', colKey: 'taskNo', cell: 'taskNo', ellipsis: true },
  { title: '状态', colKey: 'status', cell: 'status', width: 92 },
  { title: '置信度', colKey: 'confidence', cell: 'confidence', width: 80 },
  { title: '模型版本', colKey: 'modelVersion', cell: 'modelVersion', ellipsis: true, width: 104 },
  { title: '耗时', colKey: 'duration', cell: 'duration', width: 72 },
  { title: '时间', colKey: 'time', cell: 'time', width: 156 },
];

/** 状态用色：SUCCEEDED 绿、RUNNING/QUEUED 蓝、FAILED/TIMEOUT 红 */
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
function taskConfidenceText(value: unknown): string {
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

/** 递归 setTimeout 轮询（非 setInterval）：每拍 800ms，最多 20 次 */
function scheduleNextPoll(taskNo: string): void {
  stopPollingOnlyTimer();
  pollTimer = setTimeout(() => {
    void doPoll(taskNo);
  }, RERUN_POLL_INTERVAL_MS);
}

function stopPollingOnlyTimer(): void {
  if (pollTimer !== null) {
    clearTimeout(pollTimer);
    pollTimer = null;
  }
}

async function doPoll(taskNo: string): Promise<void> {
  pollTimer = null;
  if (!rerunPolling.value) return;
  const token = pollGuard.begin();
  pollCount += 1;
  try {
    const task = await getAlgorithmTask(taskNo);
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
    scheduleNextPoll(taskNo);
  } catch (error: any) {
    if (!pollGuard.isCurrent(token)) return;
    rerunPolling.value = false;
    rerunHint.value = error?.message ?? '算法任务轮询失败';
  }
}

/* ---------- “去处理”按钮可见性 ---------- */

/* ---------- 事件 ---------- */

function handleVisibleChange(value: boolean) {
  emit('update:visible', value);
}

/* ---------- 时间格式 ---------- */

function formatTime(value?: string | null): string {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}

/** 弹窗关闭或换事件时：清定时器 + 使在途轮询作废，避免跨事件串数据 */
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
  pollGuard.invalidate();
});
</script>
<style lang="less" scoped>
.detail {
  max-height: 62vh;
  overflow-y: auto;
  padding-right: 4px;
}

.detail__section {
  margin-bottom: var(--td-comp-margin-l);
}

.detail__section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--td-text-color-primary);
  margin: 0 0 var(--td-comp-margin-s);
  padding-left: 8px;
  border-left: 3px solid var(--td-brand-color);
  line-height: 18px;
}

.detail__alert {
  margin-bottom: var(--td-comp-margin-s);
}

/* ---------- 算法任务 ---------- */

.detail__section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: var(--td-comp-margin-s);

  .detail__section-title {
    margin-bottom: 0;
  }
}

.detail__algo-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: var(--td-text-color-secondary);
  margin-bottom: 8px;
}

.detail__algo-hint {
  font-size: 12px;
  color: var(--td-brand-color);
  margin-bottom: 8px;
}

.detail__algo-error {
  margin-bottom: 8px;
}

.detail__desc {
  margin: var(--td-comp-margin-s) 0 0;
  font-size: 13px;
  color: var(--td-text-color-secondary);
  line-height: 20px;
}

.detail__muted {
  color: var(--td-text-color-placeholder);
}

/* ---------- 证据 ---------- */

.evidence-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.evidence-card {
  margin: 0;
  border: 1px solid var(--td-component-stroke-color);
  border-radius: var(--td-radius-medium);
  overflow: hidden;
}

.evidence-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  background-color: var(--td-bg-color-secondarycontainer);
}

.evidence-card__time {
  font-size: 12px;
  color: var(--td-text-color-secondary);
}

.evidence-card__body {
  padding: 10px;
}

.evidence-card__thumbs {
  display: flex;
  gap: 8px;
}

.evidence-card__thumb {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0;
  border: 1px solid var(--td-component-stroke-color);
  border-radius: var(--td-radius-small);
  background: none;
  cursor: pointer;
  overflow: hidden;
  font-family: inherit;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }

  img {
    width: 100%;
    height: 110px;
    object-fit: cover;
    display: block;
    background-color: var(--td-bg-color-secondarycontainer);
  }
}

.evidence-card__noimg {
  height: 110px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}

.evidence-card__caption {
  font-size: 11px;
  color: var(--td-text-color-secondary);
  padding: 2px 4px;
}

.evidence-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 10px;
  font-size: 11px;
  color: var(--td-text-color-placeholder);
  border-top: 1px solid var(--td-component-stroke-color);
}

.evidence-card__hash {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------- 处理记录 ---------- */

.detail__records {
  display: grid;
  gap: 12px;
}

.detail__record {
  position: relative;
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 2px 0 2px 14px;
  border-left: 2px solid var(--td-component-border);
  overflow-wrap: anywhere;

  &::before {
    position: absolute;
    top: 7px;
    left: -5px;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--td-brand-color);
    content: '';
  }
}

.detail__record-time {
  color: var(--td-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.record {
  padding-bottom: 4px;
}

.record__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.record__flow {
  font-size: 12px;
  color: var(--td-text-color-secondary);
}

.record__opinion {
  margin: 6px 0 2px;
  font-size: 13px;
  color: var(--td-text-color-primary);
  line-height: 20px;
  /* 处理意见按普通文本展示，防止脚本注入（第29.4节） */
  white-space: pre-wrap;
  word-break: break-word;
}

.record__by {
  margin: 0;
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}

/* ---------- 底部 ---------- */

.detail__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail__footer-hint {
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}

@media (max-width: 1280px) {
  .evidence-grid {
    grid-template-columns: 1fr;
  }
}
</style>
