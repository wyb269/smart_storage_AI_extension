<template>
  <div class="door-history">
    <!-- ============ 标题区（按原型：左标题+图标，右返回按钮） ============ -->
    <div class="door-history__header">
      <div class="door-history__header-left">
        <t-icon name="time" class="door-history__title-icon" />
        <h2 class="door-history__title">仓门异动监测历史记录</h2>
      </div>

      <t-button theme="default" variant="outline" size="small" @click="handleBack">
        <template #icon><t-icon name="rollback" style="margin-right: 4px" /></template>
        返回
      </t-button>
    </div>

    <!-- ============ 查询条件区（按原型：单行紧凑排列，查询/重置靠右） ============ -->
    <div class="door-history__filter">
      <t-form :data="queryForm" label-align="right" :label-width="68" @reset="handleReset" @submit="handleSearch">
        <t-row :gutter="[48, 12]" class="door-history__filter-row">
          <!-- 一期必须支持：处理状态、仓库名称、发生时间范围（第18.2节） -->
          <t-col :xs="12" :sm="12" :md="6" :lg="6" :xl="4">
            <t-form-item label="处理状态" name="processStatus">
              <t-select v-model="queryForm.processStatus" placeholder="请选择处理状态" clearable>
                <t-option
                  v-for="opt in PROCESS_STATUS_OPTIONS"
                  :key="opt.value"
                  :value="opt.value"
                  :label="opt.label"
                />
              </t-select>
            </t-form-item>
          </t-col>

          <t-col :xs="12" :sm="12" :md="6" :lg="6" :xl="4">
            <t-form-item label="仓库名称" name="warehouseId">
              <t-select v-model="queryForm.warehouseId" placeholder="请输入仓库名称" clearable filterable>
                <t-option
                  v-for="w in warehouseOptions"
                  :key="w.warehouseId"
                  :value="w.warehouseId"
                  :label="w.warehouseName"
                />
              </t-select>
            </t-form-item>
          </t-col>

          <t-col :xs="24" :sm="24" :md="12" :lg="8" :xl="5">
            <t-form-item label="发生时间" name="dateRange">
              <t-date-range-picker
                v-model="queryForm.dateRange"
                placeholder="开始时间  -  结束时间"
                allow-input
                clearable
              />
            </t-form-item>
          </t-col>

          <!--
            按钮组：与筛选项同行并靠右对齐。
            P2 修复：原实现整行占满（:md="24"）导致按钮被挤到下一行独占整行，
            与原型「同一行右侧」不一致。此处改为固定窄栅格 + 自动换行：
            宽度足够时留在筛选项右侧，不足时整体换行到下一行右侧。
          -->
          <t-col :xs="24" :sm="24" :md="24" :lg="6" :xl="4" class="door-history__operations">
            <t-space size="10px">
              <t-button theme="primary" type="submit">
                <template #icon><t-icon name="search" style="margin-right: 4px" /></template>
                查询
              </t-button>
              <t-button theme="default" variant="outline" type="reset">
                <template #icon><t-icon name="refresh" style="margin-right: 4px" /></template>
                重置
              </t-button>
            </t-space>
          </t-col>
        </t-row>

        <!-- 建议同时支持的筛选项（第18.2节）：默认折叠，避免首屏过于拥挤 -->
        <div v-if="advancedVisible" class="door-history__advanced">
          <t-row :gutter="[48, 12]">
            <t-col :xs="12" :sm="12" :md="6" :lg="6" :xl="4">
              <t-form-item label="事件编号" name="eventNo">
                <t-input v-model="queryForm.eventNo" placeholder="请输入事件编号" clearable />
              </t-form-item>
            </t-col>

            <t-col :xs="12" :sm="12" :md="6" :lg="6" :xl="4">
              <t-form-item label="仓门位置" name="doorId">
                <t-select v-model="queryForm.doorId" placeholder="全部仓门" clearable filterable>
                  <t-option v-for="d in state.doors.value" :key="d.doorId" :value="d.doorId" :label="d.doorName" />
                </t-select>
              </t-form-item>
            </t-col>

            <t-col :xs="12" :sm="12" :md="6" :lg="6" :xl="4">
              <t-form-item label="摄像头" name="cameraCode">
                <t-input v-model="queryForm.cameraCode" placeholder="请输入摄像头编号" clearable />
              </t-form-item>
            </t-col>

            <t-col :xs="12" :sm="12" :md="6" :lg="6" :xl="4">
              <t-form-item label="异动类型" name="anomalyType">
                <t-select v-model="queryForm.anomalyType" placeholder="全部类型" clearable>
                  <t-option
                    v-for="opt in ANOMALY_TYPE_OPTIONS"
                    :key="opt.value"
                    :value="opt.value"
                    :label="opt.label"
                  />
                </t-select>
              </t-form-item>
            </t-col>
          </t-row>
        </div>

        <div class="door-history__filter-actions">
          <t-link theme="primary" size="small" @click="advancedVisible = !advancedVisible">
            <template #icon>
              <t-icon :name="advancedVisible ? 'chevron-up' : 'chevron-down'" style="margin-right: 2px" />
            </template>
            {{ advancedVisible ? '收起筛选' : '更多筛选' }}
          </t-link>
        </div>
      </t-form>
    </div>

    <!-- ============ 工具条（按原型：导出按钮独立一行，居左） ============ -->
    <div class="door-history__toolbar">
      <t-button theme="default" variant="outline" size="small" :loading="exporting" @click="handleExport">
        <template #icon><t-icon name="download" style="margin-right: 4px" /></template>
        导出记录
      </t-button>
    </div>

    <!-- 当前筛选条件提示：第16.6节要求明确显示并提供清除入口 -->
    <t-alert
      v-if="activeFilterText"
      theme="info"
      :message="`当前筛选：${activeFilterText}`"
      class="door-history__filter-hint"
    >
      <template #operation>
        <t-link theme="primary" size="small" @click="handleClearFilter">清除筛选</t-link>
      </template>
    </t-alert>

    <!-- ============ 服务端分页表格（第18.4节） ============ -->
    <history-event-table
      v-model:page-no="pageNo"
      v-model:page-size="pageSize"
      class="door-history__table"
      :events="events"
      :total="total"
      :loading="loading"
      :error="error"
      @view="handleViewEvent"
      @page-change="handlePageChange"
      @page-size-change="handlePageSizeChange"
      @retry="loadEvents"
    />

    <!--
      事件详情弹窗：查看即标记为已处理（2026-10-21 产品变更）。
      已取消人工处理入口，状态只有「未处理 / 已处理」两种。
    -->
    <event-detail-dialog
      v-model:visible="detailVisible"
      :event-id="activeEventId"
      :detail="activeDetail"
      :loading="detailLoading"
      @refresh="handleAlgorithmRefresh"
    />
  </div>
</template>
<script setup lang="ts">
import type { DateRangeValue, DateValue } from 'tdesign-vue-next';
import { MessagePlugin } from 'tdesign-vue-next';
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { exportEvent, getEventDetail, listEventByCondition, markEventViewed } from '@/api/doorAnomaly';

import { usePollingHost } from '../shared/composables/usePollingHost';
import { useRequestRaceGuard } from '../shared/composables/useRequestRaceGuard';
import EventDetailDialog from './components/EventDetailDialog.vue';
import HistoryEventTable from './components/HistoryEventTable.vue';
import { useDoorAnomalyState } from './composables/useDoorAnomalyState';
import { ANOMALY_TYPE_LABEL, PAGINATION_DEFAULT, PROCESS_STATUS_LABEL } from './constants';
import type { AnomalyEvent, AnomalyType, EventDetail, ProcessStatus } from './types';

/**
 * 仓门异动监测历史记录页。
 *
 * 依据 AI_HANDOFF.md 第18节：
 * - 服务端分页，禁止前端一次性加载全部历史数据
 * - 默认按发生时间倒序
 * - 查询条件变化后回到第一页，翻页时保留筛选条件
 * - 页码超出有效范围时回到最后一个有效页
 * - 返回监测首页时恢复离开前选中的仓门，且不产生重复页签
 */
const router = useRouter();
const route = useRoute();

/* ---------- 状态 ---------- */

// 历史页不需要最近事件轮询（第21.4节：事件详情与历史列表不轮询）
const state = useDoorAnomalyState({ recentEnabled: false });

const events = ref<AnomalyEvent[]>([]);
const total = ref(0);
const pageNo = ref(1);
// 显式标注为 number：常量默认值为字面量 10，
// 若不标注则 ref 会被推断为 Ref<10>，导致页大小赋值时报类型错误
const pageSize = ref<number>(PAGINATION_DEFAULT.PAGE_SIZE);

/**
 * 更多筛选项是否展开。
 * 默认收起，仅展示原型图中的三个核心条件，保持首屏简洁。
 */
const advancedVisible = ref(false);
const loading = ref(false);
const error = ref('');
const exporting = ref(false);

/* ---------- 竞态守卫（P1-5） ---------- */

/** 列表请求守卫：快速筛选/翻页时只有最后一次结果生效 */
const listGuard = useRequestRaceGuard();
/** 详情请求守卫：连续点击不同事件时只有最后一次生效 */
const detailGuard = useRequestRaceGuard();

/* ---------- 轮询生命周期（P1-6） ---------- */

/**
 * 历史页不启用最近事件轮询（第21.4节），
 * 此处仍交由统一宿主管理仓门状态轮询，避免各自注册造成重复调度。
 */
usePollingHost({ pollings: state.pollings });

/* ---------- 查询表单 ---------- */

/**
 * 首次进入默认查询最近 30 天，覆盖现有样例且保持有界查询。
 */
function defaultDateRange(): DateRangeValue {
  const end = new Date();
  const start = new Date(end.getTime() - 30 * 24 * 60 * 60 * 1000);
  return [formatDateTime(start), formatDateTime(end)];
}

const queryForm = reactive({
  eventNo: '',
  warehouseId: '',
  doorId: (route.query.doorId as string) ?? '',
  cameraCode: '',
  anomalyType: '',
  processStatus: '',
  dateRange: defaultDateRange(),
});

/* ---------- 选项 ---------- */

/** 下拉选项：显式声明为 {label,value} 结构，避免 v-for 解构类型歧义 */
const PROCESS_STATUS_OPTIONS = (Object.keys(PROCESS_STATUS_LABEL) as ProcessStatus[]).map((value) => ({
  value,
  label: PROCESS_STATUS_LABEL[value],
}));

const ANOMALY_TYPE_OPTIONS = (Object.keys(ANOMALY_TYPE_LABEL) as AnomalyType[]).map((value) => ({
  value,
  label: ANOMALY_TYPE_LABEL[value],
}));

/** 仓库选项来自仓门列表去重，避免额外接口 */
const warehouseOptions = computed(() => {
  const seen = new Map<string, string>();
  state.doors.value.forEach((d) => {
    if (d.warehouseId && !seen.has(d.warehouseId)) {
      seen.set(d.warehouseId, d.warehouseName);
    }
  });
  return Array.from(seen, ([warehouseId, warehouseName]) => ({
    warehouseId,
    warehouseName,
  }));
});

/* ---------- 当前筛选提示 ---------- */

const activeFilterText = computed(() => {
  const parts: string[] = [];
  if (queryForm.eventNo) parts.push(`事件编号 ${queryForm.eventNo}`);
  if (queryForm.warehouseId) {
    const w = warehouseOptions.value.find((x) => x.warehouseId === queryForm.warehouseId);
    parts.push(`仓库 ${w?.warehouseName ?? queryForm.warehouseId}`);
  }
  if (queryForm.doorId) {
    const d = state.doors.value.find((x) => x.doorId === queryForm.doorId);
    parts.push(`仓门 ${d?.doorName ?? queryForm.doorId}`);
  }
  if (queryForm.cameraCode) parts.push(`摄像头 ${queryForm.cameraCode}`);
  if (queryForm.anomalyType) {
    const label = ANOMALY_TYPE_LABEL[queryForm.anomalyType as AnomalyType];
    if (label) parts.push(`类型 ${label}`);
  }
  if (queryForm.processStatus) {
    const label = PROCESS_STATUS_LABEL[queryForm.processStatus as ProcessStatus];
    if (label) parts.push(`状态 ${label}`);
  }
  if (queryForm.dateRange?.length === 2) {
    parts.push(`时间 ${queryForm.dateRange[0]} ~ ${queryForm.dateRange[1]}`);
  }
  // 默认时间范围不计入「额外筛选」，避免误导
  return parts.length > 2 ? parts.join('；') : '';
});

/* ---------- 数据加载 ---------- */

/**
 * 加载历史事件（服务端分页）。
 * 默认按发生时间倒序（第18.3节）。
 *
 * <p>使用独立的请求序列号：快速连续筛选或翻页时，
 * 较早发出的慢响应不得覆盖较新的筛选结果（P1-5）。</p>
 */
async function loadEvents() {
  const token = listGuard.begin();
  loading.value = true;
  error.value = '';
  try {
    const res = await listEventByCondition({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      eventNo: queryForm.eventNo || undefined,
      warehouseId: queryForm.warehouseId || undefined,
      doorId: queryForm.doorId || undefined,
      cameraCode: queryForm.cameraCode || undefined,
      anomalyType: queryForm.anomalyType || undefined,
      processStatus: queryForm.processStatus || undefined,
      startTime: toBoundaryTime(queryForm.dateRange?.[0], 'start'),
      endTime: toBoundaryTime(queryForm.dateRange?.[1], 'end'),
    });

    if (!listGuard.isCurrent(token)) return;

    events.value = res.data;
    total.value = res.total;

    // 第18.4节：页码超出有效范围时回到最后一个有效页
    const lastPage = Math.max(1, Math.ceil(res.total / pageSize.value));
    if (pageNo.value > lastPage) {
      pageNo.value = lastPage;
      await loadEvents();
    }
  } catch (e: any) {
    if (!listGuard.isCurrent(token)) return;
    error.value = e?.message ?? '历史记录加载失败';
  } finally {
    if (listGuard.isCurrent(token)) {
      loading.value = false;
    }
  }
}

/* ---------- 分页（第18.4节） ---------- */

/**
 * 翻页：保留筛选条件，只改页码并重新请求。
 *
 * <p>修复 P1-4：分页组件发出的页码变更此前没有触发重新请求，
 * 表现为「翻页后表格内容不变」。</p>
 */
function handlePageChange(nextPageNo: number) {
  pageNo.value = nextPageNo;
  void loadEvents();
}

/**
 * 改变页大小：回到第一页并重新请求。
 *
 * <p>第18.4节：查询条件变化后回到第一页。
 * 页大小变化同样属于查询条件变化。</p>
 */
function handlePageSizeChange(nextPageSize: number) {
  pageSize.value = nextPageSize;
  pageNo.value = 1;
  void loadEvents();
}

/* ---------- 查询与重置 ---------- */

/**
 * 第18.4节：查询条件变化后回到第一页。
 */
async function handleSearch() {
  pageNo.value = 1;
  await loadEvents();
}

async function handleReset() {
  queryForm.eventNo = '';
  queryForm.warehouseId = '';
  queryForm.doorId = '';
  queryForm.cameraCode = '';
  queryForm.anomalyType = '';
  queryForm.processStatus = '';
  queryForm.dateRange = defaultDateRange();
  pageNo.value = 1;
  await loadEvents();
}

function handleClearFilter() {
  void handleReset();
}

/* ---------- 弹窗 ---------- */

const detailVisible = ref(false);
const detailLoading = ref(false);
const activeEventId = ref('');
const activeDetail = ref<EventDetail | null>(null);

/**
 * 打开事件详情，并把该事件标记为已处理。
 *
 * <p><b>2026-10-21 产品变更</b>：取消人工处理入口，状态只有
 * 「未处理 / 已处理」两种，点开详情即视为已处理。</p>
 *
 * <p>标记失败不关闭详情：用户要看的内容已经在屏幕上，
 * 只提示标记未成功。</p>
 */
async function openDetail(eventId: string) {
  const token = detailGuard.begin();
  activeEventId.value = eventId;
  detailVisible.value = true;
  detailLoading.value = true;
  activeDetail.value = null;
  try {
    const detail = await getEventDetail(eventId);
    if (!detailGuard.isCurrent(token)) return;
    activeDetail.value = detail;
    if (detail.processStatus === 'PENDING') {
      void markViewed(eventId, token);
    }
  } catch (error: any) {
    if (!detailGuard.isCurrent(token)) return;
    MessagePlugin.error(error?.message ?? '事件详情加载失败');
    detailVisible.value = false;
  } finally {
    if (detailGuard.isCurrent(token)) {
      detailLoading.value = false;
    }
  }
}

function handleViewEvent(eventId: string) {
  void openDetail(eventId);
}

/**
 * 「重新识别」轮询到终态后的刷新入口：
 * 重拉详情并把回写后的算法列带进历史列表（列结构本身不动）。
 */
function handleAlgorithmRefresh() {
  if (activeEventId.value) {
    void openDetail(activeEventId.value);
  }
  void loadEvents();
}

/* ---------- 查看即标记为已处理 ---------- */

/**
 * 标记成功后同步本地状态与当前页列表。
 *
 * <p>列表按当前筛选条件重载一次；<b>不强制改变用户当前页</b>（第31节）。
 * 详情弹窗里同步为已处理，避免要等下一次刷新才变。</p>
 */
async function markViewed(eventId: string, token: number) {
  const result = await markEventViewed(eventId);
  if (!result.success) {
    MessagePlugin.warning(result.message);
    return;
  }
  if (detailGuard.isCurrent(token) && activeDetail.value?.eventId === eventId) {
    activeDetail.value = { ...activeDetail.value, processStatus: 'CLOSED' };
  }
  await loadEvents();
}

/* ---------- 导出（第18.5节） ---------- */

async function handleExport() {
  exporting.value = true;
  try {
    const res = await exportEvent({
      source: 'HISTORY',
      eventNo: queryForm.eventNo || undefined,
      warehouseId: queryForm.warehouseId || undefined,
      doorId: queryForm.doorId || undefined,
      cameraCode: queryForm.cameraCode || undefined,
      anomalyType: queryForm.anomalyType || undefined,
      processStatus: queryForm.processStatus || undefined,
      startTime: toBoundaryTime(queryForm.dateRange?.[0], 'start'),
      endTime: toBoundaryTime(queryForm.dateRange?.[1], 'end'),
    });

    // 失败必须被识别出来，不能把错误响应存成文件。
    // 后端异常时 GlobalDefaultExceptionHandler 以 **HTTP 200** 返回 JSON，
    // 而本请求是 responseType:'blob'，JSON 会被包成 Blob；
    // 旧实现只判了 `!res?.data`，Blob 恒为真值，
    // 于是用户会下载到一个内容为 JSON 的 .xlsx 并看到「导出成功」。
    const raw = res?.data as Blob | undefined;
    if (!raw || raw.size === 0) {
      throw new Error('导出失败：响应为空');
    }

    // 按**响应头**判断类型：下面会用固定 MIME 重建 Blob，
    // 届时 raw.type 已不可信
    const contentType: string = String(res?.headers?.['content-type'] ?? raw.type ?? '');
    if (contentType.includes('json') || contentType.includes('text')) {
      let message = '导出失败，请重试';
      try {
        message = JSON.parse(await raw.text())?.status?.message || message;
      } catch {
        // 不是 JSON 就用兜底文案，不让解析异常盖住真实原因
      }
      throw new Error(message);
    }

    const blob = new Blob([raw], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `仓门异动记录_${stamp()}.xlsx`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);

    MessagePlugin.success('导出成功');
  } catch (e: any) {
    MessagePlugin.error(e?.message ?? '导出失败，请重试');
  } finally {
    exporting.value = false;
  }
}

/** `yyyy-MM-dd HH:mm:ss`，后端时间字段要求的格式 */
function formatDateTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}

/**
 * 把区间控件的值补齐成后端要求的完整时间。
 *
 * 控件没有开启时间选择时只返回 `yyyy-MM-dd`，而 SQL 里是闭区间
 * `occurred_at <= endTime`：若结束边界按当天 00:00:00 下发，
 * 用户选中的那一天会被整天排除。因此开始补 `00:00:00`、结束补 `23:59:59`。
 * 已经带时间的值原样下发。
 */
function toBoundaryTime(value: DateValue | undefined, boundary: 'start' | 'end'): string | undefined {
  if (value === undefined || value === null || value === '') return undefined;
  const text = value instanceof Date ? formatDateTime(value) : String(value).trim();
  if (/^\d{4}-\d{2}-\d{2}$/.test(text)) {
    return `${text} ${boundary === 'start' ? '00:00:00' : '23:59:59'}`;
  }
  return text;
}

function stamp(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}` +
    `${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`
  );
}

/* ---------- 返回（第14.1节） ---------- */

/**
 * 返回监测首页，并通过 query 恢复选中仓门。
 * 第18.3条验收：不产生重复页签或错误路由。
 */
function handleBack() {
  void router.push({
    path: '/aiVision/doorAnomaly',
    query: {
      doorId: queryForm.doorId || (route.query.doorId as string) || undefined,
    },
  });
}

onMounted(async () => {
  // 仓门列表用于筛选项与名称回显，不阻塞列表加载
  await Promise.allSettled([state.loadDoors(), loadEvents()]);
});
</script>
<style lang="less" scoped>
.door-history {
  display: flex;
  flex-direction: column;
  gap: var(--td-comp-margin-l);
}

.door-history__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.door-history__header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.door-history__title-icon {
  font-size: 22px;
  color: var(--td-brand-color);
}

.door-history__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--td-text-color-primary);
}

.door-history__filter,
.door-history__toolbar,
.door-history__table {
  background-color: var(--td-bg-color-container);
  border-radius: var(--td-radius-medium);
  padding: var(--td-comp-paddingLR-l) var(--td-comp-paddingTB-l);
}

.door-history__operations {
  display: flex;
  justify-content: flex-end;
}

.door-history__filter-actions {
  margin-top: 8px;
  text-align: right;
}

.door-history__filter-hint {
  flex-shrink: 0;
}
</style>
