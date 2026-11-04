<template>
  <div class="door-anomaly">
    <!-- ============ 标题区（第16.2节） ============ -->
    <div class="door-anomaly__header">
      <div class="door-anomaly__header-left">
        <t-icon name="view-list" class="door-anomaly__header-icon" />
        <div>
          <h2 class="door-anomaly__title">仓门异动监测</h2>
          <p class="door-anomaly__desc">实时监测粮仓仓门状态，识别异常开启、移动或设备离线情况</p>
        </div>
      </div>
      <div class="door-anomaly__header-right">
        <span v-if="lastUpdatedAt" class="door-anomaly__updated"> 最后更新 {{ lastUpdatedAt }} </span>
        <t-button theme="default" variant="outline" size="small" @click="handleRefreshAll">
          <template #icon><t-icon name="refresh" /></template>
          刷新
        </t-button>
      </div>
    </div>

    <!-- 独立区域失败提示：第30节「单个区域失败不得阻塞其他区域」 -->
    <t-alert v-if="doorsError" theme="error" :message="doorsError" class="door-anomaly__alert">
      <template #operation>
        <t-link theme="primary" size="small" @click="refreshDoors">重试</t-link>
      </template>
    </t-alert>

    <!--
      状态面板与监控面板左右布局，约 1:3（第22.1节）。
      左侧 280–320px，右侧自适应。
    -->
    <div class="door-anomaly__main">
      <div class="door-anomaly__left">
        <door-status-panel
          :model-value="selectedDoorId"
          :doors="doors"
          :loading="doorsLoading"
          :is-empty="isEmpty"
          @change="handleDoorChange"
        />
      </div>

      <div class="door-anomaly__right">
        <video-monitor
          :door="selectedDoor"
          :stream-info="streamInfo"
          :loading="streamLoading"
          @retry="handleRetryStream"
          @refresh="handleRetryStream"
        />
      </div>
    </div>

    <!-- ============ 最近异动记录（第16.5节） ============ -->
    <recent-event-table
      class="door-anomaly__recent"
      :events="recentEvents"
      :loading="eventsLoading"
      :error="eventsError"
      @view="handleViewEvent"
      @view-all="handleViewAll"
      @retry="refreshEvents"
    />

    <!--
      事件详情弹窗：查看即标记为已处理（2026-10-21 产品变更）。
      已取消人工处理入口，状态只有「未处理 / 已处理」两种，
      因此这里不再传 can-process，也不再接处理弹窗。
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
import { MessagePlugin } from 'tdesign-vue-next';
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getEventDetail, markEventViewed } from '@/api/doorAnomaly';

import { usePollingHost } from '../shared/composables/usePollingHost';
import { useRequestRaceGuard } from '../shared/composables/useRequestRaceGuard';
import DoorStatusPanel from './components/DoorStatusPanel.vue';
import EventDetailDialog from './components/EventDetailDialog.vue';
import RecentEventTable from './components/RecentEventTable.vue';
import VideoMonitor from './components/VideoMonitor.vue';
import { useDoorAnomalyState } from './composables/useDoorAnomalyState';
import type { DoorStatus, EventDetail } from './types';

/**
 * 仓门异动监测首页。
 *
 * 依据 AI_HANDOFF.md 第16节：
 * - 只开在现有后台布局内部的内容区，不重复创建导航、顶部栏和页签
 * - 首次进入并行请求仓门列表和最近事件
 * - 默认选择第一个在线仓门；全部离线时选择第一个但不自动播放
 * - 点击仓门只切换监控画面，默认不自动过滤历史事件表格
 * - “查看全部”跳转到独立历史记录页
 */
const router = useRouter();

/* ---------- 状态层 ---------- */

const state = useDoorAnomalyState({ recentEnabled: true });

/*
 * 组合式函数返回的 ref 在模板中需通过 script 顶层变量暴露，
 * Vue 模板会自动解包 ref，故此处直接解构出各 ref 供模板使用。
 */
const {
  doors,
  recentEvents,
  selectedDoor,
  selectedDoorId,
  streamInfo,
  doorsLoading,
  eventsLoading,
  streamLoading,
  doorsError,
  eventsError,
  isEmpty,
  loadDoors,
  loadRecentEvents,
  loadStreamInfo,
  refreshDoors,
  refreshEvents,
} = state;

/* ---------- 弹窗状态 ---------- */

const detailVisible = ref(false);
const detailLoading = ref(false);
const activeEventId = ref('');
const activeDetail = ref<EventDetail | null>(null);
const lastUpdatedAt = ref('');

/** 详情请求竞态守卫：连续点击不同事件时只有最后一次生效（P1-5） */
const detailGuard = useRequestRaceGuard();

/* ---------- 轮询生命周期（P1-6） ---------- */

/**
 * 统一接管轮询的启动、暂停与销毁。
 * 页面自身的初始化请求不通过轮询发起，避免首屏重复加载。
 */
usePollingHost({ pollings: state.pollings });

/* ---------- 事件详情加载 ---------- */

/**
 * 打开详情弹窗并加载详情，随后把该事件标记为已处理。
 *
 * <p><b>2026-10-21 产品变更</b>：取消人工处理入口，状态只有
 * 「未处理 / 已处理」两种，<b>点开详情即视为已处理</b>。
 * 因此这里在详情读取成功后调用一次 {@link markEventViewed}。</p>
 *
 * <p>标记失败<b>不阻断详情展示</b>：详情已经拿到，用户要看的内容
 * 已经在屏幕上，只提示标记未成功，不把弹窗关掉。</p>
 *
 * <p>使用独立的详情请求序列号，快速连续点击不同事件时，
 * 只有最后一次请求的结果会被写入界面（P1-5）。</p>
 */
async function openDetail(eventId: string) {
  const token = detailGuard.begin();
  activeEventId.value = eventId;
  detailVisible.value = true;
  detailLoading.value = true;
  activeDetail.value = null;
  try {
    const detail = await getEventDetail(eventId);
    // 旧请求的结果不得覆盖最新点击的事件
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
 * 重拉详情（算法任务表 + 事件算法列都会变）并同步外层最近记录。
 * openDetail 里的 markViewed 幂等，重复进入无副作用。
 */
function handleAlgorithmRefresh() {
  if (activeEventId.value) {
    void openDetail(activeEventId.value);
  }
  void refreshEvents?.();
}

/* ---------- 查看即标记为已处理 ---------- */

/**
 * 把刚被查看的事件标记为已处理。
 *
 * <p>只在详情当前展示的就是这条事件时更新本地状态，避免用户
 * 已经切到别的事件却被旧结果覆盖（沿用 P1-5 的同一套序列号守卫）。</p>
 *
 * <p>列表刷新失败或不刷新都不影响本条事件的展示，因此不 await 到详情流程里。</p>
 */
async function markViewed(eventId: string, token: number) {
  const result = await markEventViewed(eventId);
  if (!result.success) {
    MessagePlugin.warning(result.message);
    return;
  }
  // 本地立即同步为已处理，避免等下一次轮询才变化
  if (detailGuard.isCurrent(token) && activeDetail.value?.eventId === eventId) {
    activeDetail.value = { ...activeDetail.value, processStatus: 'CLOSED' };
  }
  void refreshEvents?.();
}

/* ---------- 仓门切换 ---------- */

/**
 * 第16.6节：点击仓门只切换监控画面，
 * 默认不自动过滤下方历史事件表格。
 *
 * <p><b>唯一入口</b>：页面所有仓门切换都必须经过
 * {@link useDoorAnomalyState.selectDoor}，由它统一完成
 * 失效旧播放请求、清空旧流信息与拉取新流信息。
 * 若在此处直接改写 selectedDoorId，则不会触发取流，
 * 会导致「切换仓门后视频仍是上一路」（P0-4）。</p>
 */
function handleDoorChange(door: DoorStatus) {
  state.selectDoor(door.doorId);
}

/* ---------- 监控重试 ---------- */

function handleRetryStream() {
  if (selectedDoorId.value) {
    void loadStreamInfo(selectedDoorId.value);
  }
}

/* ---------- 刷新与初始化 ---------- */

async function handleRefreshAll() {
  await Promise.allSettled([loadDoors(), loadRecentEvents()]);
  handleRetryStream();
  updateTimestamp();
}

/* ---------- 跳转历史页 ---------- */

/**
 * 跳转到独立历史记录页。
 * 第16.5节：不能简单在首页弹出一个更大的表格。
 *
 * 通过 query 传递当前仓门，便于历史页返回时恢复（第14.1节）。
 */
function handleViewAll() {
  void router.push({
    path: '/aiVision/doorAnomaly/history',
    query: {
      // 第18节：历史页返回时恢复离开前选中的仓门
      doorId: selectedDoorId.value || undefined,
    },
  });
}

function updateTimestamp() {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  lastUpdatedAt.value = `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

onMounted(async () => {
  // 第16.6节：首次进入并行请求仓门列表和最近事件。
  // 轮询由 usePollingHost 在 onMounted 中统一启动，
  // 此处不再触发轮询，避免首屏重复加载同一批数据（单一入口）。
  await Promise.allSettled([loadDoors(), loadRecentEvents()]);

  updateTimestamp();

  // 默认选中仓门（pickDefaultDoor 已优先在线仓门）后拉取播放信息
  if (selectedDoorId.value) {
    await loadStreamInfo(selectedDoorId.value);
  }
});
</script>
<style lang="less" scoped>
/*
 * 页面只填充布局内容区，不触碰全局布局与主题（第34节第4、5条）。
 */
.door-anomaly {
  display: flex;
  flex-direction: column;
  gap: var(--td-comp-margin-l);
  min-height: 100%;
}

/*
 * P1-9 修复：1280×720 下主区必须可纵向访问全部内容。
 * 原实现为 height:100% + overflow:hidden，
 * 视频区最小高度 + 底部最近事件表格会被裁切且无法滚动。
 * 现在改为内容自然撑开，由布局容器提供纵向滚动。
 */

.door-anomaly__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
}

.door-anomaly__header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.door-anomaly__header-icon {
  font-size: 22px;
  color: var(--td-brand-color);
}

.door-anomaly__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 24px;
  color: var(--td-text-color-primary);
}

.door-anomaly__desc {
  margin: 2px 0 0;
  font-size: 12px;
  line-height: 18px;
  color: var(--td-text-color-secondary);
}

.door-anomaly__header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.door-anomaly__updated {
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}

.door-anomaly__alert {
  flex-shrink: 0;
}

/*
 * 左右布局约 1:3；左侧 280–320px（第22.1节）。
 * 空间不足时允许上下排列，但不得产生整页横向滚动。
 */
.door-anomaly__main {
  display: grid;
  /* 左侧栏宽用 clamp() 随视口弹性伸缩 */
  grid-template-columns: clamp(380px, 30vw, 500px) minmax(0, 1fr);
  gap: var(--td-comp-margin-l);
  align-items: start;
}

.door-anomaly__left,
.door-anomaly__right {
  min-width: 0;
}

.door-anomaly__recent {
  flex-shrink: 0;
}

@media (max-width: 1280px) {
  .door-anomaly__main {
    grid-template-columns: clamp(360px, 28vw, 460px) minmax(0, 1fr);
  }
}

/*
 * 竖屏或窄屏时上下排列，避免横向滚动（第22.1节）。
 */
@media (max-width: 900px) {
  .door-anomaly__main {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
