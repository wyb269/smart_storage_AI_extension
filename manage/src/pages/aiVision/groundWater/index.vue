<template>
  <div class="water">
    <div class="water__header">
      <div class="water__header-left">
        <t-icon name="water-wave" class="water__header-icon" />
        <div>
          <h2 class="water__title">地面积水监测</h2>
          <p class="water__desc">实时监测地面水浸和积水情况，防范泄漏事故和粮堆受潮风险</p>
        </div>
      </div>
      <div class="water__header-right">
        <span v-if="lastUpdatedAt" class="water__updated">最后更新 {{ lastUpdatedAt }}</span>
        <t-button theme="default" variant="outline" size="small" @click="handleRefreshAll">
          <template #icon><t-icon name="refresh" /></template>
          刷新
        </t-button>
      </div>
    </div>

    <!-- 单个区域失败不得阻塞其他区域 -->
    <t-alert v-if="pointsError" theme="error" :message="pointsError" class="water__alert">
      <template #operation>
        <t-link theme="primary" size="small" @click="loadPoints">重试</t-link>
      </template>
    </t-alert>

    <div class="water__main">
      <div class="water__left">
        <monitor-point-panel
          :model-value="selectedPointId"
          :points="points"
          :loading="pointsLoading"
          :is-empty="isEmpty"
          @change="handlePointChange"
        />
      </div>

      <div class="water__right">
        <video-monitor
          :point="selectedPoint"
          :stream="streamInfo"
          :loading="streamLoading"
          @retry="handleRetryStream"
        />
      </div>
    </div>

    <recent-event-table
      class="water__recent"
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

import { getEventDetail, markEventViewed } from '@/api/groundWater';

import { usePollingHost } from '../shared/composables/usePollingHost';
import { useRequestRaceGuard } from '../shared/composables/useRequestRaceGuard';
import EventDetailDialog from './components/EventDetailDialog.vue';
import MonitorPointPanel from './components/MonitorPointPanel.vue';
import RecentEventTable from './components/RecentEventTable.vue';
import VideoMonitor from './components/VideoMonitor.vue';
import { useGroundWaterState } from './composables/useGroundWaterState';
import type { MonitorPointStatus, WaterEventDetail } from './types';

/**
 * 地面积水监测首页。
 *
 * - 只填充后台布局内容区，不重复创建导航与页签
 * - 首次进入并行请求点位列表和最近事件
 * - 默认选择第一个在线点位；全部离线时选第一个但不自动播放
 * - 点击点位只切换监控画面，不过滤下方事件表格
 */
const router = useRouter();

const state = useGroundWaterState({ recentEnabled: true });

const {
  points,
  recentEvents,
  selectedPoint,
  selectedPointId,
  streamInfo,
  pointsLoading,
  eventsLoading,
  streamLoading,
  pointsError,
  eventsError,
  isEmpty,
  loadPoints,
  loadRecentEvents,
  loadStreamInfo,
} = state;

/* ---------- 弹窗 ---------- */
const detailVisible = ref(false);
const detailLoading = ref(false);
const activeEventId = ref('');
const activeDetail = ref<WaterEventDetail | null>(null);
const lastUpdatedAt = ref('');

/** 详情请求竞态守卫：连续点击不同事件时只有最后一次生效 */
const detailGuard = useRequestRaceGuard();

/* ---------- 轮询生命周期统一接管 ---------- */
usePollingHost({ pollings: state.pollings });

/**
 * 打开详情并加载，随后把该事件标记为已处理。
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
    // 三态下只有「未核实」会被自动标记；「处理中」说明已有人在跟进，不覆盖
    if (detail.processStatus !== 'DONE') {
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

/** 把刚被查看的事件本地同步为已处理，避免等下一次轮询才变化 */
async function markViewed(eventId: string, token: number) {
  const result = await markEventViewed(eventId);
  if (!result.success) {
    MessagePlugin.warning(result.message);
    return;
  }
  if (detailGuard.isCurrent(token) && activeDetail.value?.eventId === eventId) {
    activeDetail.value = { ...activeDetail.value, processStatus: 'DONE' };
  }
  void state.refreshEvents?.();
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
  void state.refreshEvents?.();
}

/** 点位切换的唯一入口，直接改 selectedPointId 不会触发取流 */
function handlePointChange(point: MonitorPointStatus) {
  state.selectPoint(point.pointId);
}

function handleRetryStream() {
  if (selectedPointId.value) {
    void loadStreamInfo(selectedPointId.value);
  }
}

async function handleRefreshAll() {
  await Promise.allSettled([loadPoints(), loadRecentEvents()]);
  handleRetryStream();
  updateTimestamp();
}

function handleViewAll() {
  void router.push({
    path: '/aiVision/groundWater/history',
    query: { pointId: selectedPointId.value || undefined },
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
  // 取流不在这里做：loadPoints 定下默认点位后会经 selectPoint 自己拉，
  // 在这里再发一次会和它抢，出现「后发的旧结果覆盖新选中项」的竞态。
  await Promise.allSettled([loadPoints(), loadRecentEvents()]);
  updateTimestamp();
});
</script>
<style lang="less" scoped>
.water {
  display: flex;
  flex-direction: column;
  gap: var(--td-comp-margin-l);
  min-height: 100%;
}

.water__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
}

.water__header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.water__header-icon {
  font-size: 22px;
  color: var(--td-brand-color);
}

.water__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 24px;
  color: var(--td-text-color-primary);
}

.water__desc {
  margin: 2px 0 0;
  font-size: 12px;
  line-height: 18px;
  color: var(--td-text-color-secondary);
}

.water__header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.water__updated {
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}

.water__alert {
  flex-shrink: 0;
}

.water__main {
  display: grid;
  grid-template-columns: clamp(380px, 30vw, 500px) minmax(0, 1fr);
  gap: var(--td-comp-margin-l);
  align-items: start;
}

.water__left,
.water__right {
  min-width: 0;
}

.water__recent {
  flex-shrink: 0;
}

@media (max-width: 1280px) {
  .water__main {
    grid-template-columns: clamp(360px, 28vw, 460px) minmax(0, 1fr);
  }
}

@media (max-width: 900px) {
  .water__main {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
