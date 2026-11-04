<template>
  <div class="vehicle">
    <div class="vehicle__header">
      <div class="vehicle__header-left">
        <t-icon name="user-list" class="vehicle__header-icon" />
        <div>
          <h2 class="vehicle__title">车辆智能识别</h2>
          <p class="vehicle__desc">车牌识别 · 车型识别 · 出入库车辆识别 · 一体化看板管理</p>
        </div>
      </div>
      <div class="vehicle__header-right">
        <span v-if="lastUpdatedAt" class="vehicle__updated">最后更新 {{ lastUpdatedAt }}</span>
        <t-button theme="default" variant="outline" size="small" @click="handleRefreshAll">
          <template #icon><t-icon name="refresh" /></template>
          刷新
        </t-button>
      </div>
    </div>

    <!-- 单个区域失败不得阻塞其他区域 -->
    <t-alert v-if="todayError" theme="error" :message="todayError" class="vehicle__alert">
      <template #operation>
        <t-link theme="primary" size="small" @click="loadTodayVehicles">重试</t-link>
      </template>
    </t-alert>

    <div class="vehicle__main">
      <div class="vehicle__left">
        <today-vehicle-panel
          :model-value="selectedEventId"
          :vehicles="todayVehicles"
          :loading="todayLoading"
          :is-empty="isEmpty"
          @change="handleVehicleChange"
        />
      </div>

      <div class="vehicle__right">
        <video-monitor
          :vehicle="selectedVehicle"
          :stream="streamInfo"
          :loading="streamLoading"
          @retry="handleRetryStream"
        />
      </div>
    </div>

    <recent-event-table
      class="vehicle__recent"
      :events="recentEvents"
      :loading="eventsLoading"
      :error="eventsError"
      @view="handleViewEvent"
      @view-all="handleViewAll"
      @retry="loadRecentEvents"
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
import { MessagePlugin } from 'tdesign-vue-next';
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getEventDetail, markEventViewed } from '@/api/vehicleRecognition';

import { usePollingHost } from '../shared/composables/usePollingHost';
import { useRequestRaceGuard } from '../shared/composables/useRequestRaceGuard';
import EventDetailDialog from './components/EventDetailDialog.vue';
import RecentEventTable from './components/RecentEventTable.vue';
import TodayVehiclePanel from './components/TodayVehiclePanel.vue';
import VideoMonitor from './components/VideoMonitor.vue';
import { useVehicleRecognitionState } from './composables/useVehicleRecognitionState';
import type { TodayVehicle, VehicleEventDetail } from './types';

/**
 * 车辆智能识别首页。
 *
 * - 只填充后台布局内容区，不重复创建导航与页签
 * - 首次进入并行请求今日识别列表和最近记录
 * - 默认选中今日最新一条识别记录；选中即取该记录所属点位的摄像头
 * - 点击识别记录只切换监控画面，不过滤下方记录表格
 */
const router = useRouter();

const state = useVehicleRecognitionState({ recentEnabled: true });

const {
  todayVehicles,
  recentEvents,
  selectedVehicle,
  selectedEventId,
  streamInfo,
  todayLoading,
  eventsLoading,
  streamLoading,
  todayError,
  eventsError,
  isEmpty,
  loadTodayVehicles,
  loadRecentEvents,
  loadStreamInfo,
} = state;

/* ---------- 弹窗 ---------- */
const detailVisible = ref(false);
const detailLoading = ref(false);
const activeEventId = ref('');
const activeDetail = ref<VehicleEventDetail | null>(null);
const lastUpdatedAt = ref('');

/** 详情请求竞态守卫：连续点击不同记录时只有最后一次生效 */
const detailGuard = useRequestRaceGuard();

/* ---------- 轮询生命周期统一接管 ---------- */
usePollingHost({ pollings: state.pollings });

/**
 * 打开详情并加载，随后把该记录标记为已核实。
 *
 * 标记失败不阻断详情展示：用户要看的内容已经在屏幕上，
 * 只提示标记未成功，不把弹窗关掉。
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
    if (detail.verifyStatus === 'PENDING') {
      void markViewed(eventId, token);
    }
  } catch (error: any) {
    if (!detailGuard.isCurrent(token)) return;
    MessagePlugin.error(error?.message ?? '识别记录详情加载失败');
    detailVisible.value = false;
  } finally {
    if (detailGuard.isCurrent(token)) {
      detailLoading.value = false;
    }
  }
}

/** 把刚被查看的记录本地同步为已核实，避免等下一次轮询才变化 */
async function markViewed(eventId: string, token: number) {
  const result = await markEventViewed(eventId);
  if (!result.success) {
    MessagePlugin.warning(result.message);
    return;
  }
  if (detailGuard.isCurrent(token) && activeDetail.value?.eventId === eventId) {
    activeDetail.value = { ...activeDetail.value, verifyStatus: 'VERIFIED' };
  }
  void state.refreshEvents?.();
}

function handleViewEvent(eventId: string) {
  void openDetail(eventId);
}

/**
 * 「重新识别」轮询到终态后的刷新入口：
 * 重拉详情拿到新的算法任务表，并刷新左侧今日识别车辆（结论/置信度会被回写）。
 * openDetail 里的 markViewed 幂等，重复进入无副作用。
 */
function handleAlgorithmRefresh() {
  if (activeEventId.value) {
    void openDetail(activeEventId.value);
  }
  void loadTodayVehicles();
}

/** 识别记录切换的唯一入口，直接改 selectedEventId 不会触发取流 */
function handleVehicleChange(vehicle: TodayVehicle) {
  state.selectItem(vehicle.eventId);
}

function handleRetryStream() {
  if (selectedEventId.value) {
    void loadStreamInfo(state.selectedPointId.value);
  }
}

async function handleRefreshAll() {
  await Promise.allSettled([loadTodayVehicles(), loadRecentEvents()]);
  handleRetryStream();
  updateTimestamp();
}

function handleViewAll() {
  void router.push({
    path: '/aiVision/vehicleRecognition/history',
    query: { eventId: selectedEventId.value || undefined },
  });
}

function updateTimestamp() {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  lastUpdatedAt.value = `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

onMounted(async () => {
  // 轮询由 usePollingHost 在 onMounted 中统一启动，
  // 这里只负责首屏数据，避免同一批数据被请求两次。
  // 取流不在这里做：loadTodayVehicles 定下默认选中记录时会经 selectItem 自己拉。
  await Promise.allSettled([loadTodayVehicles(), loadRecentEvents()]);
  updateTimestamp();
});
</script>
<style lang="less" scoped>
.vehicle {
  display: flex;
  flex-direction: column;
  gap: var(--td-comp-margin-l);
  min-height: 100%;
}

.vehicle__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
}

.vehicle__header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.vehicle__header-icon {
  font-size: 22px;
  color: var(--td-brand-color);
}

.vehicle__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 24px;
  color: var(--td-text-color-primary);
}

.vehicle__desc {
  margin: 2px 0 0;
  font-size: 12px;
  line-height: 18px;
  color: var(--td-text-color-secondary);
}

.vehicle__header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.vehicle__updated {
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}

.vehicle__alert {
  flex-shrink: 0;
}

.vehicle__main {
  display: grid;
  grid-template-columns: clamp(380px, 30vw, 500px) minmax(0, 1fr);
  gap: var(--td-comp-margin-l);
  align-items: start;
}

.vehicle__left,
.vehicle__right {
  min-width: 0;
}

.vehicle__recent {
  flex-shrink: 0;
}

@media (max-width: 1280px) {
  .vehicle__main {
    grid-template-columns: clamp(360px, 28vw, 460px) minmax(0, 1fr);
  }
}

@media (max-width: 900px) {
  .vehicle__main {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
