<template>
  <div class="perimeter-history">
    <div class="perimeter-history__header">
      <div class="perimeter-history__header-left">
        <t-icon name="time" class="perimeter-history__title-icon" />
        <h2 class="perimeter-history__title">周界入侵识别历史记录</h2>
      </div>
      <t-button theme="default" variant="outline" size="small" @click="handleBack">
        <template #icon><t-icon name="rollback" style="margin-right: 4px" /></template>
        返回
      </t-button>
    </div>

    <div class="perimeter-history__filter">
      <t-form :data="queryForm" label-align="right" :label-width="80" @reset="handleReset" @submit="handleSearch">
        <t-row :gutter="[48, 12]">
          <t-col :xs="12" :sm="12" :md="6" :lg="6" :xl="4">
            <t-form-item label="防区" name="zone">
              <t-select v-model="queryForm.zone" placeholder="全部防区" :options="ZONE_OPTIONS" />
            </t-form-item>
          </t-col>

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

          <t-col :xs="24" :sm="24" :md="12" :lg="8" :xl="5">
            <t-form-item label="发生时间" name="dateRange">
              <t-date-range-picker v-model="queryForm.dateRange" allow-input clearable />
            </t-form-item>
          </t-col>

          <t-col :xs="24" :sm="24" :md="24" :lg="6" :xl="4" class="perimeter-history__operations">
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

        <div v-if="advancedVisible" class="perimeter-history__advanced">
          <t-row :gutter="[48, 12]">
            <t-col :xs="12" :sm="12" :md="6" :lg="6" :xl="4">
              <t-form-item label="事件编号" name="eventNo">
                <t-input v-model="queryForm.eventNo" placeholder="请输入事件编号" clearable />
              </t-form-item>
            </t-col>

            <t-col :xs="12" :sm="12" :md="6" :lg="6" :xl="4">
              <t-form-item label="摄像头" name="cameraCode">
                <t-input v-model="queryForm.cameraCode" placeholder="请输入摄像头编号" clearable />
              </t-form-item>
            </t-col>

            <t-col :xs="12" :sm="12" :md="6" :lg="6" :xl="4">
              <t-form-item label="入侵类型" name="intrusionType">
                <t-select v-model="queryForm.intrusionType" placeholder="全部类型" clearable>
                  <t-option
                    v-for="opt in INTRUSION_TYPE_OPTIONS"
                    :key="opt.value"
                    :value="opt.value"
                    :label="opt.label"
                  />
                </t-select>
              </t-form-item>
            </t-col>
          </t-row>
        </div>

        <div class="perimeter-history__filter-actions">
          <t-link theme="primary" size="small" @click="advancedVisible = !advancedVisible">
            <template #icon>
              <t-icon :name="advancedVisible ? 'chevron-up' : 'chevron-down'" style="margin-right: 2px" />
            </template>
            {{ advancedVisible ? '收起筛选' : '更多筛选' }}
          </t-link>
        </div>
      </t-form>
    </div>

    <div class="perimeter-history__toolbar">
      <t-button theme="default" variant="outline" size="small" :loading="exporting" @click="handleExport">
        <template #icon><t-icon name="download" style="margin-right: 4px" /></template>
        导出记录
      </t-button>
    </div>

    <history-event-table
      v-model:page-no="pageNo"
      v-model:page-size="pageSize"
      class="perimeter-history__table"
      :events="events"
      :total="total"
      :loading="loading"
      :error="error"
      @view="handleViewEvent"
      @page-change="handlePageChange"
      @page-size-change="handlePageSizeChange"
      @retry="loadEvents"
    />

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
import type { DateRangeValue } from 'tdesign-vue-next';
import { MessagePlugin } from 'tdesign-vue-next';
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { exportEvent, getEventDetail, listEventByCondition, markEventViewed } from '@/api/perimeterIntrusion';

import { useRequestRaceGuard } from '../shared/composables/useRequestRaceGuard';
import { PAGINATION_DEFAULT } from '../shared/constants';
import EventDetailDialog from './components/EventDetailDialog.vue';
import HistoryEventTable from './components/HistoryEventTable.vue';
import { INTRUSION_TYPE_OPTIONS, PROCESS_STATUS_OPTIONS, ZONE_OPTIONS } from './constants';
import type { IntrusionType, PerimeterEvent, PerimeterEventDetail, ProcessStatus, Zone } from './types';

/**
 * 周界入侵历史记录页。
 *
 * - 服务端分页，禁止一次性加载全部历史
 * - 默认按发生时间倒序、默认查询最近 7 天
 * - 查询条件变化后回到第一页，翻页时保留筛选条件
 * - 防区下拉与监测首页同一个取值域（EAST/WEST/SOUTH/NORTH，空为全部），
 *   但这里是**下推到 SQL** 的：分页 total 必须跟着筛选变，前端过滤会算错页数
 * - 返回监测首页时恢复离开前选中的防区段
 */
const router = useRouter();
const route = useRoute();

// 历史页不需要最近事件轮询
const events = ref<PerimeterEvent[]>([]);
const total = ref(0);
const pageNo = ref(1);
// 显式标注 number：常量默认值是字面量，否则 ref 会被推断为 Ref<10> 导致赋值报类型错
const pageSize = ref<number>(PAGINATION_DEFAULT.PAGE_SIZE);
const loading = ref(false);
const error = ref('');
const exporting = ref(false);
const advancedVisible = ref(false);

const listGuard = useRequestRaceGuard();

const detailVisible = ref(false);
const detailLoading = ref(false);
const activeEventId = ref('');
const activeDetail = ref<PerimeterEventDetail | null>(null);
const detailGuard = useRequestRaceGuard();

const queryForm = reactive({
  zone: '' as Zone | '',
  processStatus: '' as ProcessStatus | '',
  dateRange: defaultDateRange(),
  eventNo: '',
  cameraCode: '',
  intrusionType: '' as IntrusionType | '',
});

/** 默认查询最近 7 天 */
function defaultDateRange(): DateRangeValue {
  const pad = (n: number) => String(n).padStart(2, '0');
  const fmt = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  const end = new Date();
  const start = new Date(end.getTime() - PAGINATION_DEFAULT.DEFAULT_DAYS * 86_400_000);
  return [fmt(start) as any, fmt(end) as any];
}

function buildParams() {
  const [start, end] = queryForm.dateRange || [];
  return {
    pageNo: pageNo.value,
    pageSize: pageSize.value,
    eventNo: queryForm.eventNo || undefined,
    zone: queryForm.zone || undefined,
    cameraCode: queryForm.cameraCode || undefined,
    intrusionType: queryForm.intrusionType || undefined,
    // 三值处理状态原样下推，不做「已处理」之类的合并
    processStatus: queryForm.processStatus || undefined,
    startTime: start ? `${String(start)} 00:00:00` : undefined,
    endTime: end ? `${String(end)} 23:59:59` : undefined,
  };
}

async function loadEvents() {
  const token = listGuard.begin();
  loading.value = true;
  error.value = '';
  try {
    const page = await listEventByCondition(buildParams());
    if (!listGuard.isCurrent(token)) return;
    events.value = page.data;
    total.value = page.total;

    // 页码超出有效范围时回到最后一个有效页
    const lastPage = Math.max(1, Math.ceil(page.total / pageSize.value));
    if (pageNo.value > lastPage) {
      pageNo.value = lastPage;
      void loadEvents();
    }
  } catch (e: any) {
    if (listGuard.isCurrent(token)) {
      error.value = e?.message ?? '历史记录加载失败';
    }
  } finally {
    if (listGuard.isCurrent(token)) {
      loading.value = false;
    }
  }
}

function handleSearch() {
  pageNo.value = 1;
  void loadEvents();
}

function handleReset() {
  queryForm.zone = '';
  queryForm.processStatus = '';
  queryForm.dateRange = defaultDateRange();
  queryForm.eventNo = '';
  queryForm.cameraCode = '';
  queryForm.intrusionType = '';
  pageNo.value = 1;
  void loadEvents();
}

function handlePageChange(next: number) {
  pageNo.value = next;
  void loadEvents();
}

function handlePageSizeChange(next: number) {
  pageSize.value = next;
  pageNo.value = 1;
  void loadEvents();
}

/** 标记成功后本地同步为已处理并刷新列表，失败只提示不关弹窗 */
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
    if (detail.processStatus !== 'DONE') {
      void markViewed(eventId, token);
    }
  } catch (e: any) {
    if (!detailGuard.isCurrent(token)) return;
    MessagePlugin.error(e?.message ?? '事件详情加载失败');
    detailVisible.value = false;
  } finally {
    if (detailGuard.isCurrent(token)) {
      detailLoading.value = false;
    }
  }
}

async function markViewed(eventId: string, token: number) {
  const result = await markEventViewed(eventId);
  if (!result.success) {
    MessagePlugin.warning(result.message);
    return;
  }
  if (detailGuard.isCurrent(token) && activeDetail.value?.eventId === eventId) {
    activeDetail.value = { ...activeDetail.value, processStatus: 'DONE' };
  }
  void loadEvents();
}

function handleViewEvent(eventId: string) {
  void openDetail(eventId);
}

/**
 * 「重新识别」轮询到终态后的刷新入口：
 * 重拉详情并把回写后的算法列带进历史列表（列结构与防区筛选都不动）。
 */
function handleAlgorithmRefresh() {
  if (activeEventId.value) {
    void openDetail(activeEventId.value);
  }
  void loadEvents();
}

/**
 * 导出当前筛选条件下的全部记录。
 *
 * 服务端直出 xlsx 二进制，因此需要手动构造下载链接；
 * 失败时给出可理解的提示，而不是静默无反应。
 */
async function handleExport() {
  exporting.value = true;
  try {
    const { startTime, endTime, eventNo, cameraCode, intrusionType, processStatus, zone } = buildParams();
    const res: any = await exportEvent({
      source: 'HISTORY',
      startTime,
      endTime,
      eventNo,
      zone,
      cameraCode,
      intrusionType,
      processStatus,
    });
    const blob: Blob = res?.data;
    if (!blob || blob.size === 0) {
      MessagePlugin.error('导出失败：服务端未返回文件内容');
      return;
    }
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `周界入侵记录_${Date.now()}.xlsx`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    // 立即 revoke 会让部分浏览器来不及落盘，延后一拍
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    MessagePlugin.success('导出成功');
  } catch (e: any) {
    MessagePlugin.error(e?.message ?? '导出失败');
  } finally {
    exporting.value = false;
  }
}

function handleBack() {
  router.push({
    path: '/aiVision/perimeterIntrusion/monitor',
    query: { pointId: (route.query.pointId as string) || undefined },
  });
}

onMounted(async () => {
  // 历史页不依赖防区段列表做下拉（防区是固定四值枚举，来自 constants），
  // 因此这里只加载事件，不再多发一次状态请求。
  void loadEvents();
});
</script>
<style lang="less" scoped>
.perimeter-history {
  display: flex;
  flex-direction: column;
  gap: var(--td-comp-margin-l);
}

.perimeter-history__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.perimeter-history__header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.perimeter-history__title-icon {
  font-size: 22px;
  color: var(--td-brand-color);
}

.perimeter-history__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--td-text-color-primary);
}

.perimeter-history__filter,
.perimeter-history__toolbar,
.perimeter-history__table {
  background-color: var(--td-bg-color-container);
  border-radius: var(--td-radius-medium);
  padding: var(--td-comp-paddingLR-l) var(--td-comp-paddingTB-l);
}

.perimeter-history__operations {
  display: flex;
  justify-content: flex-end;
}

.perimeter-history__filter-actions {
  margin-top: 8px;
  text-align: right;
}
</style>
