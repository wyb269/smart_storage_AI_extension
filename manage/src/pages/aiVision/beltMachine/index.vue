<template>
  <div class="belt">
    <div class="belt__header">
      <div class="belt__header-left">
        <t-icon name="user" class="belt__header-icon" />
        <div>
          <h2 class="belt__title">皮带机下站人监测</h2>
          <p class="belt__desc">实时识别皮带机下方及运行区域的人员行为，防范机械伤害事故</p>
        </div>
      </div>
      <div class="belt__header-right">
        <span v-if="lastUpdatedAt" class="belt__updated">最后更新 {{ lastUpdatedAt }}</span>
        <t-button theme="default" variant="outline" size="small" @click="handleRefreshAll">
          <template #icon><t-icon name="refresh" /></template>
          刷新
        </t-button>
      </div>
    </div>

    <!-- 单个区域失败不得阻塞其他区域 -->
    <t-alert v-if="pointsError" theme="error" :message="pointsError" class="belt__alert">
      <template #operation>
        <t-link theme="primary" size="small" @click="loadPoints">重试</t-link>
      </template>
    </t-alert>

    <div class="belt__main">
      <div class="belt__left">
        <monitor-point-panel
          :model-value="selectedPointId"
          :points="points"
          :loading="pointsLoading"
          :is-empty="isEmpty"
          @change="handlePointChange"
        />
      </div>

      <div class="belt__right">
        <belt-machine-monitor
          :point="selectedPoint"
          :stream="streamInfo"
          :loading="streamLoading"
          @retry="handleRetryStream"
        />
      </div>
    </div>

    <recent-event-table
      class="belt__recent"
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
      @algorithm-finished="handleAlgorithmFinished"
    />
  </div>
</template>
<script setup lang="ts">
import { MessagePlugin } from 'tdesign-vue-next';
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { getEventDetail, markEventViewed } from '@/api/beltMachine';

import { usePollingHost } from '../shared/composables/usePollingHost';
import { useRequestRaceGuard } from '../shared/composables/useRequestRaceGuard';
import BeltMachineMonitor from './components/BeltMachineMonitor.vue';
import EventDetailDialog from './components/EventDetailDialog.vue';
import MonitorPointPanel from './components/MonitorPointPanel.vue';
import RecentEventTable from './components/RecentEventTable.vue';
import { useBeltMachineState } from './composables/useBeltMachineState';
import type { BeltMachineEventDetail, MonitorPoint } from './types';

/**
 * 皮带机下站人监测首页。
 *
 * - 只填充后台布局内容区，不重复创建导航与页签
 * - 首次进入并行请求点位列表和最近事件
 * - 默认选中第一个在线点位；全部离线或未部署时选第一个但不自动播放
 * - 点击点位只切换监控画面，不过滤下方事件表格
 */
const router = useRouter();

const state = useBeltMachineState({ recentEnabled: true });

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
const activeDetail = ref<BeltMachineEventDetail | null>(null);
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

/** 重新识别到达终态：重拉详情（算法任务表与事件算法列都会变）并刷新最近记录 */
function handleAlgorithmFinished(eventId: string) {
  if (eventId) {
    void openDetail(eventId);
  }
  void state.refreshEvents?.();
}

/** 皮带机切换的唯一入口，直接改 selectedPointId 不会触发取流 */
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
    path: '/aiVision/beltMachine/history',
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
.belt {
  display: flex;
  flex-direction: column;
  gap: var(--td-comp-margin-l);
  min-height: 100%;
}

.belt__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
}

.belt__header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.belt__header-icon {
  font-size: 22px;
  color: var(--td-brand-color);
}

.belt__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 24px;
  color: var(--td-text-color-primary);
}

.belt__desc {
  margin: 2px 0 0;
  font-size: 12px;
  line-height: 18px;
  color: var(--td-text-color-secondary);
}

.belt__header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.belt__updated {
  font-size: 12px;
  color: var(--td-text-color-placeholder);
}

.belt__alert {
  flex-shrink: 0;
}

.belt__main {
  display: grid;
  grid-template-columns: clamp(380px, 30vw, 500px) minmax(0, 1fr);
  gap: var(--td-comp-margin-l);
  align-items: start;
}

.belt__left,
.belt__right {
  min-width: 0;
}

.belt__recent {
  flex-shrink: 0;
}

@media (max-width: 1280px) {
  .belt__main {
    grid-template-columns: clamp(360px, 28vw, 460px) minmax(0, 1fr);
  }
}

@media (max-width: 900px) {
  .belt__main {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
