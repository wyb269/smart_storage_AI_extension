<template>
  <div class="person-history">
    <div class="person-history__header">
      <div class="person-history__header-left">
        <t-icon name="time" class="person-history__title-icon" />
        <h2 class="person-history__title">人员行为监测历史记录</h2>
      </div>
      <t-button theme="default" variant="outline" size="small" @click="handleBack">
        <template #icon><t-icon name="rollback" style="margin-right: 4px" /></template>
        返回
      </t-button>
    </div>

    <div class="person-history__filter">
      <t-form :data="queryForm" label-align="right" :label-width="80" @reset="handleReset" @submit="handleSearch">
        <t-row :gutter="[48, 12]">
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
              <t-select v-model="queryForm.warehouseId" placeholder="请选择仓库" clearable filterable>
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
              <t-date-range-picker v-model="queryForm.dateRange" allow-input clearable />
            </t-form-item>
          </t-col>

          <t-col :xs="24" :sm="24" :md="24" :lg="6" :xl="4" class="person-history__operations">
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

        <div v-if="advancedVisible" class="person-history__advanced">
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
              <t-form-item label="行为类型" name="behaviorType">
                <t-select v-model="queryForm.behaviorType" placeholder="全部类型" clearable>
                  <t-option
                    v-for="opt in BEHAVIOR_TYPE_OPTIONS"
                    :key="opt.value"
                    :value="opt.value"
                    :label="opt.label"
                  />
                </t-select>
              </t-form-item>
            </t-col>
          </t-row>
        </div>

        <div class="person-history__filter-actions">
          <t-link theme="primary" size="small" @click="advancedVisible = !advancedVisible">
            <template #icon>
              <t-icon :name="advancedVisible ? 'chevron-up' : 'chevron-down'" style="margin-right: 2px" />
            </template>
            {{ advancedVisible ? '收起筛选' : '更多筛选' }}
          </t-link>
        </div>
      </t-form>
    </div>

    <div class="person-history__toolbar">
      <t-button theme="default" variant="outline" size="small" :loading="exporting" @click="handleExport">
        <template #icon><t-icon name="download" style="margin-right: 4px" /></template>
        导出记录
      </t-button>
    </div>

    <history-event-table
      v-model:page-no="pageNo"
      v-model:page-size="pageSize"
      class="person-history__table"
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
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { exportEvent, getEventDetail, listEventByCondition, markEventViewed } from '@/api/personBehavior';

import { useRequestRaceGuard } from '../shared/composables/useRequestRaceGuard';
import { PAGINATION_DEFAULT } from '../shared/constants';
import EventDetailDialog from './components/EventDetailDialog.vue';
import HistoryEventTable from './components/HistoryEventTable.vue';
import { usePersonBehaviorState } from './composables/usePersonBehaviorState';
import { BEHAVIOR_TYPE_OPTIONS, PROCESS_STATUS_OPTIONS } from './constants';
import type { BehaviorType, PersonEvent, PersonEventDetail, ProcessStatus } from './types';

/**
 * 人员行为历史记录页。
 *
 * - 服务端分页，禁止一次性加载全部历史
 * - 默认按发生时间倒序、默认查询最近 7 天
 * - 查询条件变化后回到第一页，翻页时保留筛选条件
 * - 返回监测首页时恢复离开前选中的区域
 */
const router = useRouter();
const route = useRoute();

// 历史页不需要最近事件轮询
const state = usePersonBehaviorState({ recentEnabled: false });

const events = ref<PersonEvent[]>([]);
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
const activeDetail = ref<PersonEventDetail | null>(null);
const detailGuard = useRequestRaceGuard();

const queryForm = reactive({
  processStatus: '' as ProcessStatus | '',
  warehouseId: '',
  dateRange: defaultDateRange(),
  eventNo: '',
  cameraCode: '',
  behaviorType: '' as BehaviorType | '',
});

/** 默认查询最近 7 天 */
function defaultDateRange(): DateRangeValue {
  const pad = (n: number) => String(n).padStart(2, '0');
  const fmt = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  const end = new Date();
  const start = new Date(end.getTime() - PAGINATION_DEFAULT.DEFAULT_DAYS * 86_400_000);
  return [fmt(start) as any, fmt(end) as any];
}

/** 仓库下拉从区域状态派生，不额外请求接口 */
const warehouseOptions = computed(() => {
  const seen = new Map<string, string>();
  state.points.value.forEach((p) => {
    if (p.warehouseId && !seen.has(p.warehouseId)) {
      seen.set(p.warehouseId, p.warehouseName);
    }
  });
  return Array.from(seen, ([warehouseId, warehouseName]) => ({ warehouseId, warehouseName }));
});

function buildParams() {
  const [start, end] = queryForm.dateRange || [];
  // 处理状态三值都是库里的真实取值，直接下推，不需要像遮挡那版做两态折叠
  return {
    pageNo: pageNo.value,
    pageSize: pageSize.value,
    eventNo: queryForm.eventNo || undefined,
    warehouseId: queryForm.warehouseId || undefined,
    cameraCode: queryForm.cameraCode || undefined,
    behaviorType: queryForm.behaviorType || undefined,
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
  queryForm.processStatus = '';
  queryForm.warehouseId = '';
  queryForm.dateRange = defaultDateRange();
  queryForm.eventNo = '';
  queryForm.cameraCode = '';
  queryForm.behaviorType = '';
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

/** 标记成功后本地同步状态并刷新列表，失败只提示不关弹窗 */
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
 * 重拉详情并把回写后的算法列带进历史列表（列结构本身不动）。
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
    const { startTime, endTime, eventNo, warehouseId, cameraCode, behaviorType, processStatus } = buildParams();
    const res: any = await exportEvent({
      source: 'HISTORY',
      startTime,
      endTime,
      eventNo,
      warehouseId,
      cameraCode,
      behaviorType,
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
    link.download = `人员行为记录_${Date.now()}.xlsx`;
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
    path: '/aiVision/personBehavior/monitor',
    query: { pointId: (route.query.pointId as string) || undefined },
  });
}

onMounted(async () => {
  // 区域列表用于仓库下拉；失败不阻塞历史列表加载
  await Promise.allSettled([state.loadPoints()]);
  void loadEvents();
});
</script>
<style lang="less" scoped>
.person-history {
  display: flex;
  flex-direction: column;
  gap: var(--td-comp-margin-l);
}

.person-history__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.person-history__header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.person-history__title-icon {
  font-size: 22px;
  color: var(--td-brand-color);
}

.person-history__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--td-text-color-primary);
}

.person-history__filter,
.person-history__toolbar,
.person-history__table {
  background-color: var(--td-bg-color-container);
  border-radius: var(--td-radius-medium);
  padding: var(--td-comp-paddingLR-l) var(--td-comp-paddingTB-l);
}

.person-history__operations {
  display: flex;
  justify-content: flex-end;
}

.person-history__filter-actions {
  margin-top: 8px;
  text-align: right;
}
</style>
