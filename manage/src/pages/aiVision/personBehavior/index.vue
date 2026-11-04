<template>
  <div class="person">
    <div class="person__header">
      <div class="person__header-left">
        <t-icon name="user-1" class="person__header-icon" />
        <div>
          <h2 class="person__title">人员行为监测</h2>
          <p class="person__desc">移动检测、人员聚集、行为异常三位一体监测，保障库区人员安全</p>
        </div>
      </div>
      <div class="person__header-right">
        <span v-if="lastUpdatedAt" class="person__updated">最后更新 {{ lastUpdatedAt }}</span>
        <t-button theme="default" variant="outline" size="small" @click="handleRefreshAll">
          <template #icon><t-icon name="refresh" /></template>
          刷新
        </t-button>
      </div>
    </div>

    <!-- 单个区域失败不得阻塞其他区域 -->
    <t-alert v-if="pointsError" theme="error" :message="pointsError" class="person__alert">
      <template #operation>
        <t-link theme="primary" size="small" @click="loadPoints">重试</t-link>
      </template>
    </t-alert>

    <div class="person__main">
      <div class="person__left">
        <monitor-point-panel
          :model-value="selectedPointId"
          :points="points"
          :loading="pointsLoading"
          :is-empty="isEmpty"
          @change="handlePointChange"
        />
      </div>

      <div class="person__right">
        <video-monitor
          :point="selectedPoint"
          :stream="streamInfo"
          :loading="streamLoading"
          @retry="handleRetryStream"
        />
      </div>
    </div>

    <recent-event-table
      class="person__recent"
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

import { getEventDetail, markEventViewed } from '@/api/personBehavior';

import { usePollingHost } from '../shared/composables/usePollingHost';
import { useRequestRaceGuard } from '../shared/composables/useRequestRaceGuard';
import EventDetailDialog from './components/EventDetailDialog.vue';
import MonitorPointPanel from './components/MonitorPointPanel.vue';
import RecentEventTable from './components/RecentEventTable.vue';
import VideoMonitor from './components/VideoMonitor.vue';
import { usePersonBehaviorState } from './composables/usePersonBehaviorState';
import type { MonitorPoint, PersonEventDetail } from './types';

/**
 * 人员行为监测首页。
 *
 * - 只填充后台布局内容区，不重复创建导航与页签
 * - 首次进入并行请求区域列表和最近事件
 * - 默认选择第一个在线区域；全部离线时选第一个但不自动播放
 * - 点击区域卡片只切换监控画面，不过滤下方事件表格
 */
const router = useRouter();

const state = usePersonBehaviorState({ recentEnabled: true });

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
const activeDetail = ref<PersonEventDetail | null>(null);
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
 * 重拉详情（算法任务表 + 事件算法列都会变）并同步外层最近事件。
 * openDetail 里的 markViewed 幂等，重复进入无副作用。
 */
function handleAlgorithmRefresh() {
  if (activeEventId.value) {
    void openDetail(activeEventId.value);
  }
  void state.refreshEvents?.();
}

/** 区域切换的唯一入口，直接改 selectedPointId 不会触发取流 */
function handlePointChange(point: MonitorPoint) {
  state.selectItem(point.pointId);
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
    path: '/aiVision/personBehavior/history',
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
  // 取流不在这里做：loadPoints 定下默认区域时会经 selectItem 自己拉。
  await Promise.allSettled([loadPoints(), loadRecentEvents()]);
  updateTimestamp();
});
</script>
<style lang="less" scoped>
.person {
  display: flex;
  flex-direction: column;
  gap: var(--td-comp-margin-l);
  min-height: 100%;
}

.person__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
}

.person__header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.person__header-icon {
  font-size: 22px;
  color: var(--td-brand-color);
}

.person__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 24px;
  color: var(--td-text-color-primary);
}

.person__desc {
  margin: 2px 0 0;
  font-size: 12px;
  line-height: 18px;
  color: var(--td-text-color-secondary);
}

.person__header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.person__updated {
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}

.person__alert {
  flex-shrink: 0;
}

.person__main {
  display: grid;
  grid-template-columns: clamp(380px, 30vw, 500px) minmax(0, 1fr);
  gap: var(--td-comp-margin-l);
  align-items: start;
}

.person__left,
.person__right {
  min-width: 0;
}

.person__recent {
  flex-shrink: 0;
}

@media (max-width: 1280px) {
  .person__main {
    grid-template-columns: clamp(360px, 28vw, 460px) minmax(0, 1fr);
  }
}

@media (max-width: 900px) {
  .person__main {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
