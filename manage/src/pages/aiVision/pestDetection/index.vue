<template>
  <div class="pest">
    <div class="pest__header">
      <div class="pest__header-left">
        <t-icon name="search" class="pest__header-icon" />
        <div>
          <h2 class="pest__title">粮面虫害智能识别</h2>
          <p class="pest__desc">对各仓房粮面虫害与霉变进行采样检测，及时发现密度超标与虫情变化</p>
        </div>
      </div>
      <div class="pest__header-right">
        <span v-if="lastUpdatedAt" class="pest__updated">最后更新 {{ lastUpdatedAt }}</span>
        <t-button theme="default" variant="outline" size="small" @click="handleRefreshAll">
          <template #icon><t-icon name="refresh" /></template>
          刷新
        </t-button>
      </div>
    </div>

    <!-- 单个区域失败不得阻塞其他区域 -->
    <t-alert v-if="pointsError" theme="error" :message="pointsError" class="pest__alert">
      <template #operation>
        <t-link theme="primary" size="small" @click="loadPoints">重试</t-link>
      </template>
    </t-alert>

    <div class="pest__main">
      <div class="pest__left">
        <monitor-point-panel
          :model-value="selectedPointId"
          :points="points"
          :loading="pointsLoading"
          :is-empty="isEmpty"
          @change="handlePointChange"
        />
      </div>

      <div class="pest__right">
        <video-monitor
          :point="selectedPoint"
          :stream="streamInfo"
          :loading="streamLoading"
          @retry="handleRetryStream"
        />
      </div>
    </div>

    <recent-event-table
      class="pest__recent"
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

import { getEventDetail, markEventViewed } from '@/api/pestDetection';

import { usePollingHost } from '../shared/composables/usePollingHost';
import { useRequestRaceGuard } from '../shared/composables/useRequestRaceGuard';
import EventDetailDialog from './components/EventDetailDialog.vue';
import MonitorPointPanel from './components/MonitorPointPanel.vue';
import RecentEventTable from './components/RecentEventTable.vue';
import VideoMonitor from './components/VideoMonitor.vue';
import { usePestDetectionState } from './composables/usePestDetectionState';
import type { MonitorPoint, PestEventDetail } from './types';

/**
 * 粮面虫害智能识别首页。
 *
 * - 只填充后台布局内容区，不重复创建导航与页签
 * - 首次进入并行请求点位列表和最近记录
 * - 默认选择第一个在线点位；全部离线时选第一个但不自动播放
 * - 点击点位只切换监控画面，不过滤下方记录表格
 */
const router = useRouter();

const state = usePestDetectionState({ recentEnabled: true });

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
const activeDetail = ref<PestEventDetail | null>(null);
const lastUpdatedAt = ref('');

/** 详情请求竞态守卫：连续点击不同记录时只有最后一次生效 */
const detailGuard = useRequestRaceGuard();

/* ---------- 轮询生命周期统一接管 ---------- */
usePollingHost({ pollings: state.pollings });

/**
 * 打开详情并加载，随后把该记录标记为已处理。
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
    MessagePlugin.error(error?.message ?? '检测记录详情加载失败');
    detailVisible.value = false;
  } finally {
    if (detailGuard.isCurrent(token)) {
      detailLoading.value = false;
    }
  }
}

/** 把刚被查看的记录本地同步为已处理，避免等下一次轮询才变化 */
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
    path: '/aiVision/pestDetection/history',
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
  // 取流不在这里做：loadPoints 定下默认点位时会经 selectItem 自己拉。
  await Promise.allSettled([loadPoints(), loadRecentEvents()]);
  updateTimestamp();
});
</script>
<style lang="less" scoped>
.pest {
  display: flex;
  flex-direction: column;
  gap: var(--td-comp-margin-l);
  min-height: 100%;
}

.pest__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
}

.pest__header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pest__header-icon {
  font-size: 22px;
  color: var(--td-brand-color);
}

.pest__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 24px;
  color: var(--td-text-color-primary);
}

.pest__desc {
  margin: 2px 0 0;
  font-size: 12px;
  line-height: 18px;
  color: var(--td-text-color-secondary);
}

.pest__header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.pest__updated {
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}

.pest__alert {
  flex-shrink: 0;
}

.pest__main {
  display: grid;
  grid-template-columns: clamp(380px, 30vw, 500px) minmax(0, 1fr);
  gap: var(--td-comp-margin-l);
  align-items: start;
}

.pest__left,
.pest__right {
  min-width: 0;
}

.pest__recent {
  flex-shrink: 0;
}

@media (max-width: 1280px) {
  .pest__main {
    grid-template-columns: clamp(360px, 28vw, 460px) minmax(0, 1fr);
  }
}

@media (max-width: 900px) {
  .pest__main {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
