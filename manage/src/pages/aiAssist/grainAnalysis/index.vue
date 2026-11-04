<template>
  <div class="ai-page">
    <!-- 页头 -->
    <div class="page-header">
      <div class="page-title">
        <span class="title-icon">🌾</span>
        <div>
          <div class="title-text">粮情智能分析</div>
          <div class="title-desc">粮情风险研判与处置方案推荐</div>
        </div>
      </div>
    </div>

    <t-row :gutter="16">
      <!-- 左侧：输入参数 -->
      <t-col :lg="5" :md="12">
        <div class="panel">
          <div class="panel-head orange">
            <div>
              <div class="panel-title">✏️ 输入参数</div>
              <div class="panel-sub">详情、修改或调整参数</div>
            </div>
            <t-button v-if="streaming" theme="danger" variant="outline" @click="finishAll">
              ■ 停止生成
            </t-button>
            <t-button v-else theme="warning" variant="outline" :loading="loading" @click="handleAnalyze">
              🔍 开始分析
            </t-button>
          </div>

          <t-form :data="form" label-align="top" class="param-form">
            <!-- 仓房信息 -->
            <div class="section-title">🏠 仓房信息</div>
            <t-row :gutter="12">
              <t-col :span="6">
                <t-form-item label="选择仓库" name="warehouseInfo.warehouseId">
                  <t-select
                    v-model="form.warehouseInfo.warehouseId"
                    :options="warehouseOptions"
                    :loading="whLoading"
                    filterable
                    clearable
                    placeholder="请选择仓库"
                    @change="onWarehouseChange"
                  />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="仓型">
                  <t-select v-model="form.warehouseInfo.warehouseType" :options="warehouseTypeOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="仓容（t）">
                  <t-input-number v-model="form.warehouseInfo.capacity" :min="0" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="现存粮量（t）">
                  <t-input-number v-model="form.warehouseInfo.storedWeight" :min="0" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="入仓日期">
                  <t-date-picker v-model="form.warehouseInfo.storedDate" mode="date" clearable placeholder="yyyy-MM-dd" style="width: 100%" />
                </t-form-item>
              </t-col>
            </t-row>

            <!-- 粮情参数 -->
            <div class="section-title">🌾 粮情参数</div>
            <t-row :gutter="12">
              <t-col :span="6">
                <t-form-item label="粮种" name="grainInfo.grainType">
                  <t-select v-model="form.grainInfo.grainType" :options="grainTypeOptions" placeholder="请选择" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="粮食水分（%）" name="grainInfo.grainMoisture">
                  <t-input-number v-model="form.grainInfo.grainMoisture" :min="0" :max="30" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="安全水分（%）">
                  <t-input-number v-model="form.grainInfo.safeMoisture" :min="0" :max="30" :step="0.1" theme="column" placeholder="当地储粮安全水分" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="平均粮温（℃）" name="grainInfo.grainAvgTemp">
                  <t-input-number v-model="form.grainInfo.grainAvgTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="最高粮温（℃）">
                  <t-input-number v-model="form.grainInfo.grainMaxTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="最低粮温（℃）">
                  <t-input-number v-model="form.grainInfo.grainMinTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="4">
                <t-form-item label="上层粮温（℃）">
                  <t-input-number v-model="form.grainInfo.topLayerTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="4">
                <t-form-item label="中层粮温（℃）">
                  <t-input-number v-model="form.grainInfo.middleLayerTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="4">
                <t-form-item label="下层粮温（℃）">
                  <t-input-number v-model="form.grainInfo.bottomLayerTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="杂质含量（%）">
                  <t-input-number v-model="form.grainInfo.impurity" :min="0" :max="100" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="粮堆高度（m）">
                  <t-input-number v-model="form.grainInfo.pileHeight" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
            </t-row>

            <!-- 监测数据 -->
            <div class="section-title">📡 监测数据（可选）</div>
            <t-row :gutter="12">
              <t-col :span="6">
                <t-form-item label="异常高温点数量">
                  <t-input-number v-model="form.monitorData.hotSpotCount" :min="0" :step="1" theme="column" placeholder="高于周边3℃以上" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="异常点最高温（℃）">
                  <t-input-number v-model="form.monitorData.hotSpotMaxTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="升温速率（℃/天）">
                  <t-input-number v-model="form.monitorData.tempRiseRate" :step="0.1" theme="column" placeholder="近7天异常点" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="上/下层温差（℃）">
                  <t-input-number v-model="form.monitorData.topBottomTempDiff" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="上/下层水分差（%）">
                  <t-input-number v-model="form.monitorData.topBottomMoistureDiff" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="害虫总密度（头/kg）">
                  <t-input-number v-model="form.monitorData.pestDensity" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="主要害虫密度（头/kg）">
                  <t-input-number v-model="form.monitorData.mainPestDensity" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="虫种（多选）">
                  <t-select v-model="form.monitorData.pestSpecies" :options="pestSpeciesOptions" multiple clearable placeholder="请选择" />
                </t-form-item>
              </t-col>
            </t-row>

            <!-- 环境数据 -->
            <div class="section-title">🌤️ 环境数据</div>
            <t-row :gutter="12">
              <t-col :span="4">
                <t-form-item label="仓温（℃）" name="envData.warehouseTemp">
                  <t-input-number v-model="form.envData.warehouseTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="4">
                <t-form-item label="仓湿（%）" name="envData.warehouseRh">
                  <t-input-number v-model="form.envData.warehouseRh" :min="0" :max="100" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="4">
                <t-form-item label="季节">
                  <t-select v-model="form.envData.season" :options="seasonOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="4">
                <t-form-item label="室外温度（℃）">
                  <t-input-number v-model="form.envData.airTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="4">
                <t-form-item label="室外湿度（%）">
                  <t-input-number v-model="form.envData.airRh" :min="0" :max="100" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
            </t-row>

            <!-- 历史与预测 -->
            <div class="section-title">📜 历史与预测</div>
            <t-row :gutter="12">
              <t-col :span="6">
                <t-form-item label="上次熏蒸日期">
                  <t-date-picker v-model="form.historyInfo.lastFumigationDate" mode="date" clearable placeholder="yyyy-MM-dd" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="上次虫害日期">
                  <t-date-picker v-model="form.historyInfo.lastPestDate" mode="date" clearable placeholder="yyyy-MM-dd" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="历史问题（多选）">
                  <t-select v-model="form.historyInfo.pastIssues" :options="pastIssueOptions" multiple clearable placeholder="请选择" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="预测周期（天）">
                  <t-radio-group v-model="form.forecastDays" variant="default-filled">
                    <t-radio-button :value="7">7天</t-radio-button>
                    <t-radio-button :value="15">15天</t-radio-button>
                    <t-radio-button :value="30">30天</t-radio-button>
                  </t-radio-group>
                </t-form-item>
              </t-col>
            </t-row>
          </t-form>
        </div>
      </t-col>

      <!-- 右侧：分析结果（流式回显） -->
      <t-col :lg="7" :md="12">
        <div class="panel">
          <div class="panel-head green">
            <div>
              <div class="panel-title">🌾 粮情分析结果</div>
            </div>
            <span v-if="streaming" class="stream-status">AI 正在输出…</span>
          </div>

          <div ref="streamBoxRef" class="stream-box">
            <t-empty v-if="!result && !generating" description="填写左侧参数后点击「开始分析」" />
            <div v-else class="chat-msg">
              <div class="chat-avatar">🌾</div>
              <div class="chat-bubble">
                <div v-if="typing" class="typing-dots"><i></i><i></i><i></i></div>
                <template v-if="result">
                  <!-- AI 追问缺失参数 -->
                  <template v-if="rk('ask')">
                    <t-alert theme="warning">
                      <template #message>
                        <div class="ask-text">{{ askText }}<span v-if="streaming" class="ask-caret"></span></div>
                        <div v-if="!streaming && result.missingParams?.length" class="ask-list">
                          <t-tag v-for="(m, i) in result.missingParams" :key="i" size="small" variant="outline" class="ask-tag">
                            {{ m }}
                          </t-tag>
                        </div>
                      </template>
                    </t-alert>
                  </template>

                  <!-- 风险结论 -->
                  <template v-if="rk('overview')">
                    <div class="risk-head">
                      <div class="risk-score" :class="riskClass">
                        <span class="rs-num">{{ result.riskScore ?? '-' }}</span>
                        <span class="rs-unit">分</span>
                      </div>
                      <div class="risk-head-info">
                        <div class="risk-head-row">
                          <t-tag size="medium" :theme="riskTheme">{{ result.riskLevelText || riskText }}</t-tag>
                          <t-tag size="medium" :theme="adviceTheme">{{ adviceText }}</t-tag>
                          <t-tag v-if="result.forecastDays" size="medium" theme="default">未来 {{ result.forecastDays }} 天</t-tag>
                        </div>
                        <div v-if="result.summary" class="scheme-summary">{{ result.summary }}</div>
                      </div>
                    </div>
                  </template>

                  <!-- 预警项详情 -->
                  <template v-if="rk('warnings') && result.warnings && result.warnings.length">
                    <div class="section-title">🚨 预警项详情</div>
                    <div class="risk-item-list">
                      <div v-for="(w, i) in result.warnings" :key="i" class="risk-item">
                        <div class="ri-head">
                          <b>{{ w.name }}</b>
                          <t-tag size="small" :theme="levelTheme(w.level)">{{ w.levelText || w.level }}</t-tag>
                        </div>
                        <div class="ri-desc">{{ w.description }}</div>
                        <div v-if="w.impact" class="ri-desc">⚠️ {{ w.impact }}</div>
                        <div v-if="w.suggestions && w.suggestions.length" class="ri-tip">
                          <div v-for="(s, j) in w.suggestions" :key="j">💡 {{ s }}</div>
                        </div>
                      </div>
                    </div>
                  </template>

                  <!-- 推荐处置方案 -->
                  <template v-if="rk('plans') && result.treatPlans && result.treatPlans.length">
                    <div class="section-title">💊 推荐处置方案</div>
                    <div v-for="(p, i) in result.treatPlans" :key="i" class="treat-card">
                      <div class="tc-head">
                        <t-tag shape="circle" theme="primary" size="small">{{ p.priority ?? i + 1 }}</t-tag>
                        <span class="tc-title">{{ p.title }}</span>
                        <t-tag size="small" theme="primary">{{ primaryText(p.primary) }}</t-tag>
                      </div>
                      <div v-if="p.reason" class="tc-reason">原因：{{ p.reason }}</div>
                      <div v-if="p.detail" class="tc-detail">{{ p.detail }}</div>
                      <t-tag v-if="p.linkLabel" size="small" theme="success" variant="light-outline" class="tc-link">🔗 {{ p.linkLabel }}</t-tag>
                    </div>
                  </template>
                </template>
              </div>
            </div>
          </div>
        </div>
      </t-col>
    </t-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';
import { getList as getWarehouseList } from '@/api/warehouse';
import { analyzeGrain } from '@/api/grainAnalysis';
import type { GrainAnalysisReq, GrainAnalysisResult } from '@/api/grainAnalysis';
import { useStreamReveal } from '../useStreamReveal';

const SECTION_KEYS = ['ask', 'overview', 'warnings', 'plans'];
const loading = ref(false);
const whLoading = ref(false);
const result = ref<GrainAnalysisResult | null>(null);
const streamBoxRef = ref<HTMLElement>();
const { typing, streaming, generating, askText, rk, reveal, typeAsk, finishAll, abort } = useStreamReveal(
  SECTION_KEYS,
  () => streamBoxRef.value,
  () => !!result.value?.requireMoreInput,
);

const form = reactive<GrainAnalysisReq>({
  warehouseInfo: {},
  grainInfo: {},
  monitorData: {},
  envData: {},
  historyInfo: {},
  forecastDays: 15,
});

const warehouseOptions = ref<{ label: string; value: string }[]>([]);

const warehouseTypeOptions = [
  { label: '平房仓', value: 'FLAT' },
  { label: '浅圆仓', value: 'SHALLOW' },
  { label: '立筒仓', value: 'SILO' },
];
const grainTypeOptions = [
  { label: '小麦', value: 'WHEAT' },
  { label: '玉米', value: 'CORN' },
  { label: '稻谷', value: 'PADDY' },
  { label: '大米', value: 'RICE' },
  { label: '大豆', value: 'SOYBEAN' },
];
const pestSpeciesOptions = [
  { label: '玉米象', value: 'CORN_WEEVIL' },
  { label: '米象', value: 'RICE_WEEVIL' },
  { label: '谷蠹', value: 'GRAIN_BORER' },
  { label: '赤拟谷盗', value: 'RED_FLOUR_BEETLE' },
  { label: '扁谷盗', value: 'FLAT_GRAIN_BEETLE' },
  { label: '书虱', value: 'BOOK_LOUSE' },
  { label: '蛾类', value: 'MOTH' },
];
const seasonOptions = [
  { label: '春', value: 'SPRING' },
  { label: '夏', value: 'SUMMER' },
  { label: '秋', value: 'AUTUMN' },
  { label: '冬', value: 'WINTER' },
];
const pastIssueOptions = [
  { label: '虫害', value: 'PEST' },
  { label: '霉变', value: 'MOLD' },
  { label: '结露', value: 'CONDENSATION' },
];

const RISK_LEVEL_MAP: Record<string, { theme: 'success' | 'warning' | 'error'; text: string }> = {
  LOW: { theme: 'success', text: '低风险' },
  MEDIUM: { theme: 'warning', text: '中风险' },
  HIGH: { theme: 'error', text: '高风险' },
};

const ADVICE_MAP: Record<string, { theme: 'success' | 'warning' | 'error'; text: string }> = {
  monitor: { theme: 'success', text: '加强监测' },
  plan: { theme: 'warning', text: '计划处置' },
  immediate: { theme: 'error', text: '立即处置' },
};

const PRIMARY_MAP: Record<string, string> = {
  VENTILATION: '通风',
  FUMIGATION: '熏蒸',
  MIXED: '综合处置',
  MONITOR: '监测',
};

const riskMeta = computed(() => RISK_LEVEL_MAP[result.value?.riskLevel || ''] || { theme: 'warning' as const, text: '—' });
const riskTheme = computed(() => riskMeta.value.theme);
const riskClass = computed(() => `risk-${(result.value?.riskLevel || '').toLowerCase()}`);
const riskText = computed(() => riskMeta.value.text);
const adviceMeta = computed(() => ADVICE_MAP[result.value?.adviceLevel || ''] || { theme: 'warning' as const, text: '—' });
const adviceTheme = computed(() => adviceMeta.value.theme);
const adviceText = computed(() => adviceMeta.value.text);

function levelTheme(level?: string): 'success' | 'warning' | 'error' | 'default' {
  if (level === 'NORMAL') return 'success';
  if (level === 'MILD') return 'warning';
  if (level === 'MODERATE' || level === 'SEVERE') return 'error';
  return 'default';
}

function primaryText(v?: string) {
  return PRIMARY_MAP[v || ''] || v || '—';
}

onMounted(loadWarehouses);

async function loadWarehouses() {
  whLoading.value = true;
  try {
    const res: any = await getWarehouseList({ pageNo: 1, pageSize: 200 });
    const rows = res?.result?.data || res?.result?.list || res?.result || [];
    warehouseOptions.value = rows
      .filter((w: any) => w && w.id)
      .map((w: any) => ({ label: w.warehouseName || w.id, value: w.id }));
  } catch {
    // 加载失败不阻塞表单
  } finally {
    whLoading.value = false;
  }
}

function onWarehouseChange(v: string) {
  const hit = warehouseOptions.value.find((o) => o.value === v);
  form.warehouseInfo.warehouseName = hit?.label || '';
}

async function handleAnalyze() {
  loading.value = true;
  generating.value = true;
  typing.value = true;
  try {
    const res: any = await analyzeGrain({ ...form });
    if (res?.status?.code === '200' || res?.result) {
      result.value = res.result;
      if (res.result?.requireMoreInput) {
        await typeAsk(res.result.aiMessage ?? '');
      } else {
        MessagePlugin.success(res?.status?.message || '分析完成');
        await reveal();
      }
    } else {
      MessagePlugin.error(res?.status?.message || '分析失败');
      abort();
    }
  } catch (e: any) {
    MessagePlugin.error(e?.message || '请求异常，请检查后端服务');
    abort();
  } finally {
    loading.value = false;
  }
}
</script>

<style lang="less" scoped>
@import '../aiPage.less';

.ai-page {
  .risk-head {
    display: flex;
    align-items: flex-start;
    gap: 14px;

    .risk-score {
      flex-shrink: 0;
      width: 92px;
      height: 92px;
      border-radius: 50%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: #fff;
      background: var(--td-warning-color);

      &.risk-low {
        background: var(--td-success-color);
      }

      &.risk-medium {
        background: var(--td-warning-color);
      }

      &.risk-high {
        background: var(--td-error-color);
      }

      .rs-num {
        font-size: 30px;
        font-weight: 700;
        line-height: 32px;
      }

      .rs-unit {
        font-size: 12px;
      }
    }

    .risk-head-info {
      flex: 1;
      min-width: 0;

      .risk-head-row {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;
      }
    }
  }

  .risk-item-list {
    .risk-item {
      border: 1px solid var(--td-component-stroke);
      border-radius: 8px;
      padding: 10px 12px;
      margin-bottom: 8px;
      background: var(--td-bg-color-secondarycontainer);

      .ri-head {
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .ri-desc {
        font-size: 12px;
        line-height: 20px;
        color: var(--td-text-color-secondary);
        margin-top: 4px;
      }

      .ri-tip {
        font-size: 12px;
        line-height: 20px;
        margin-top: 4px;
        color: var(--td-brand-color);
      }
    }
  }

  .treat-card {
    border: 1px solid var(--td-component-stroke);
    border-radius: 10px;
    padding: 12px;
    margin-bottom: 12px;

    .tc-head {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;

      .tc-title {
        font-size: 15px;
        font-weight: 600;
      }
    }

    .tc-reason {
      font-size: 12px;
      color: var(--td-text-color-secondary);
      margin-top: 6px;
    }

    .tc-detail {
      font-size: 13px;
      line-height: 20px;
      margin-top: 4px;
    }

    .tc-link {
      margin-top: 8px;
      cursor: pointer;
    }
  }
}
</style>
