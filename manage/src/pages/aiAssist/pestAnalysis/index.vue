<template>
  <div class="ai-page">
    <!-- 页头 -->
    <div class="page-header">
      <div class="page-title">
        <span class="title-icon">🐛</span>
        <div>
          <div class="title-text">虫害智能分析</div>
          <div class="title-desc">虫害风险研判与防治方案推荐</div>
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
                <t-form-item label="仓型" name="warehouseInfo.warehouseType">
                  <t-select v-model="form.warehouseInfo.warehouseType" :options="warehouseTypeOptions" placeholder="请选择" />
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
                <t-form-item label="仓房空间体积（m³）">
                  <t-input-number v-model="form.warehouseInfo.spaceVolume" :min="0" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="粮堆体积（m³）">
                  <t-input-number v-model="form.warehouseInfo.grainVolume" :min="0" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="密闭方式">
                  <t-select v-model="form.warehouseInfo.sealingMethod" :options="sealingMethodOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="密封性等级">
                  <t-select v-model="form.warehouseInfo.sealingLevel" :options="sealingLevelOptions" placeholder="一级/二级/三级" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="压力半衰期（s）">
                  <t-input-number v-model="form.warehouseInfo.airtightDecay" :min="0" theme="column" placeholder="500Pa→250Pa 实测值" style="width: 100%" />
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
                <t-form-item label="粮食水分（%）">
                  <t-input-number v-model="form.grainInfo.grainMoisture" :min="0" :max="30" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="杂质含量（%）">
                  <t-input-number v-model="form.grainInfo.impurity" :min="0" :max="100" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="平均粮温（℃）" name="grainInfo.grainAvgTemp">
                  <t-input-number v-model="form.grainInfo.grainAvgTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="粮堆高度（m）">
                  <t-input-number v-model="form.grainInfo.pileHeight" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="已储存天数" name="grainInfo.storedDays">
                  <t-input-number
                    v-model="form.grainInfo.storedDays"
                    :min="0"
                    :step="1"
                    theme="column"
                    placeholder="请输入"
                    style="width: 100%"
                  />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="储存年限（年）" name="grainInfo.storedYears">
                  <t-input-number
                    v-model="form.grainInfo.storedYears"
                    :min="0"
                    :step="1"
                    theme="column"
                    placeholder="请输入"
                    style="width: 100%"
                  />
                </t-form-item>
              </t-col>
            </t-row>

            <!-- 害虫检测 -->
            <div class="section-title">
              🐛 害虫检测
              <t-button size="small" variant="text" theme="primary" @click="addSpeciesRow">+ 添加虫种</t-button>
            </div>
            <div class="species-list">
              <div v-for="(s, i) in form.pestInfo.species" :key="i" class="species-row">
                <t-select v-model="s.code" :options="pestSpeciesOptions" size="small" class="sp-code" placeholder="虫种" />
                <t-input-number v-model="s.density" :min="0" :step="0.1" size="small" theme="column" placeholder="头/kg" class="sp-num" />
                <t-select v-model="s.distribution" :options="distributionOptions" size="small" class="sp-dist" clearable placeholder="部位" />
                <t-checkbox v-model="s.isMain" size="small">主要</t-checkbox>
                <t-button size="small" variant="text" theme="danger" @click="form.pestInfo.species.splice(i, 1)">删除</t-button>
              </div>
            </div>
            <t-row :gutter="12">
              <t-col :span="6">
                <t-form-item label="害虫总密度（头/kg）" name="pestInfo.totalDensity">
                  <t-input-number v-model="form.pestInfo.totalDensity" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="采样点数">
                  <t-input-number v-model="form.pestInfo.samplePoints" :min="0" :step="1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="最大点密度（头/kg）">
                  <t-input-number v-model="form.pestInfo.maxPointDensity" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="检测方式">
                  <t-select v-model="form.pestInfo.detectionMethod" :options="detectionMethodOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="抗药性">
                  <t-select v-model="form.pestInfo.resistanceLevel" :options="resistanceLevelOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
            </t-row>

            <!-- 环境数据 -->
            <div class="section-title">🌤️ 环境数据</div>
            <t-row :gutter="12">
              <t-col :span="4">
                <t-form-item label="仓温（℃）">
                  <t-input-number v-model="form.envData.warehouseTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="4">
                <t-form-item label="仓内湿度（%）">
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

            <!-- 历史信息 -->
            <div class="section-title">📜 历史与防治信息</div>
            <t-row :gutter="12">
              <t-col :span="6">
                <t-form-item label="上次熏蒸日期">
                  <t-date-picker v-model="form.historyInfo.lastFumigationDate" mode="date" clearable placeholder="yyyy-MM-dd" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="上次熏蒸效果">
                  <t-input v-model="form.historyInfo.lastFumigationEffect" placeholder="如：虫密度降至0.5头/kg" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="上次虫害日期">
                  <t-date-picker v-model="form.historyInfo.lastPestDate" mode="date" clearable placeholder="yyyy-MM-dd" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="近一年发生次数">
                  <t-input-number v-model="form.historyInfo.recurrenceCount" :min="0" :step="1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="历史最高密度（头/kg）">
                  <t-input-number v-model="form.historyInfo.maxHistoricalDensity" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="历史主要虫种">
                  <t-select v-model="form.historyInfo.historicalMainSpecies" :options="pestSpeciesOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
            </t-row>
            <t-row :gutter="12">
              <t-col :span="6">
                <t-form-item label="使用防护剂">
                  <t-switch v-model="form.preventionInfo.protectantUsed" :label="['是', '否']" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="清洁卫生等级">
                  <t-select v-model="form.preventionInfo.cleaningLevel" :options="cleaningLevelOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="防虫设施">
                  <t-select v-model="form.preventionInfo.pestFacility" :options="facilityOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="上次防治方式">
                  <t-select v-model="form.controlInfo.lastControlMethod" :options="controlMethodOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="防治前密度（头/kg）">
                  <t-input-number v-model="form.controlInfo.densityBefore" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="防治后密度（头/kg）">
                  <t-input-number v-model="form.controlInfo.densityAfter" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="防治效果评价">
                  <t-select v-model="form.controlInfo.effectRating" :options="effectRatingOptions" placeholder="请选择" clearable />
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
              <div class="panel-title">🐛 虫害分析结果</div>
            </div>
            <span v-if="streaming" class="stream-status">AI 正在输出…</span>
          </div>

          <div ref="streamBoxRef" class="stream-box">
            <t-empty v-if="!result && !generating" description="填写左侧参数后点击「开始分析」" />
            <div v-else class="chat-msg">
              <div class="chat-avatar">🐛</div>
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
                          <t-tag v-if="gradeText" size="medium" theme="warning">虫粮等级：{{ gradeText }}</t-tag>
                          <t-tag v-if="result.resistanceGroup" size="medium" theme="primary">抗性分组 {{ result.resistanceGroup }}</t-tag>
                        </div>
                        <div v-if="result.summary" class="scheme-summary">{{ result.summary }}</div>
                        <div v-if="result.treatDeadline" class="deadline">⏰ {{ result.treatDeadline }}</div>
                      </div>
                    </div>
                  </template>

                  <!-- 风险项明细 -->
                  <template v-if="rk('risks') && result.riskItems && result.riskItems.length">
                    <div class="section-title">🔍 风险项详情</div>
                    <div class="risk-item-list">
                      <div v-for="(r, i) in result.riskItems" :key="i" class="risk-item">
                        <div class="ri-head">
                          <b>{{ r.name }}</b>
                          <t-tag size="small" :theme="levelTheme(r.level)">{{ r.levelText || r.level }}</t-tag>
                        </div>
                        <div class="ri-desc">{{ r.description }}</div>
                        <div v-if="r.suggestion" class="ri-tip">💡 {{ r.suggestion }}</div>
                      </div>
                    </div>
                  </template>

                  <!-- 推荐防治方案 -->
                  <template v-if="rk('plans') && result.plans && result.plans.length">
                    <div class="section-title">💊 推荐防治方案</div>
                    <div v-for="(p, i) in result.plans" :key="i" class="plan-card" :class="{ recommended: p.recommended }">
                      <div class="pc-head">
                        <span class="pc-name">{{ p.name }}</span>
                        <t-tag v-if="p.recommended" size="small" theme="success">推荐</t-tag>
                        <span class="pc-stars">{{ stars(p.starRating) }}</span>
                        <t-tag size="small" :theme="p.feasible ? 'success' : 'danger'">{{ p.feasible ? '可行' : '不可行' }}</t-tag>
                      </div>
                      <div v-if="p.feasibilityNote" class="pc-note">{{ p.feasibilityNote }}</div>
                      <div v-if="p.summary" class="pc-summary">{{ p.summary }}</div>
                      <div v-if="p.effectDescription" class="pc-effect">🎯 {{ p.effectDescription }}</div>
                      <div class="check-grid pc-grid">
                        <div v-if="p.fumigation" class="check-item">
                          <div class="ci-label">药剂</div>
                          <div class="ci-value">{{ p.fumigation.agentName || '-' }}</div>
                        </div>
                        <div v-if="p.fumigation && p.fumigation.totalDose != null" class="check-item">
                          <div class="ci-label">总药量</div>
                          <div class="ci-value">{{ fmt(p.fumigation.totalDose) }} <small>kg</small></div>
                        </div>
                        <div v-if="p.fumigation && p.fumigation.sealDays != null" class="check-item">
                          <div class="ci-label">密闭/散气</div>
                          <div class="ci-value">{{ p.fumigation.sealDays }}<small>/</small>{{ p.fumigation.aerateDays ?? '-' }} <small>天</small></div>
                        </div>
                        <div v-if="p.targetTemp != null" class="check-item">
                          <div class="ci-label">目标温度</div>
                          <div class="ci-value">{{ p.targetTemp }} <small>℃</small></div>
                        </div>
                        <div v-if="p.totalDays != null" class="check-item">
                          <div class="ci-label">作业周期</div>
                          <div class="ci-value">{{ p.totalDays }} <small>天</small></div>
                        </div>
                        <div v-if="p.effectRate != null" class="check-item">
                          <div class="ci-label">防治有效率</div>
                          <div class="ci-value">{{ p.effectRate }}<small>%</small></div>
                        </div>
                        <div v-if="p.costEstimate != null" class="check-item">
                          <div class="ci-label">预估成本</div>
                          <div class="ci-value">{{ fmt(p.costEstimate) }} <small>元</small></div>
                        </div>
                      </div>
                      <div v-if="p.advantages && p.advantages.length" class="pc-list">
                        <div v-for="(a, j) in p.advantages" :key="`a${j}`" class="pc-li good">＋ {{ a }}</div>
                      </div>
                      <div v-if="p.disadvantages && p.disadvantages.length" class="pc-list">
                        <div v-for="(a, j) in p.disadvantages" :key="`d${j}`" class="pc-li bad">－ {{ a }}</div>
                      </div>
                    </div>
                  </template>

                  <!-- 防治后监测建议 -->
                  <template v-if="rk('monitor') && result.monitorPlan">
                    <div class="safety-box">
                      <div class="section-title">🔔 防治后监测建议</div>
                      <div class="advice-list">
                        <div class="advice-item">· 防治后 {{ result.monitorPlan.recheckDays ?? '-' }} 天复查</div>
                        <div v-for="(m, i) in result.monitorPlan.items" :key="i" class="advice-item">· {{ m }}</div>
                      </div>
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
import { analyzePest } from '@/api/pestAnalysis';
import type { PestAnalysisReq, PestAnalysisResult } from '@/api/pestAnalysis';
import { useStreamReveal } from '../useStreamReveal';

const SECTION_KEYS = ['ask', 'overview', 'risks', 'plans', 'monitor'];
const loading = ref(false);
const whLoading = ref(false);
const result = ref<PestAnalysisResult | null>(null);
const streamBoxRef = ref<HTMLElement>();
const { typing, streaming, generating, askText, rk, reveal, typeAsk, finishAll, abort } = useStreamReveal(
  SECTION_KEYS,
  () => streamBoxRef.value,
  () => !!result.value?.requireMoreInput,
);

const form = reactive<PestAnalysisReq>({
  warehouseInfo: {},
  grainInfo: {},
  pestInfo: { species: [{ code: '', density: undefined as unknown as number, isMain: false }] },
  envData: {},
  historyInfo: {},
  preventionInfo: { protectantUsed: false },
  controlInfo: {},
});

const warehouseOptions = ref<{ label: string; value: string }[]>([]);

const warehouseTypeOptions = [
  { label: '平房仓', value: 'FLAT' },
  { label: '浅圆仓', value: 'SHALLOW' },
  { label: '立筒仓', value: 'SILO' },
];
const sealingMethodOptions = [
  { label: '整仓密闭', value: 'WHOLE' },
  { label: '膜下密闭', value: 'UNDER_FILM' },
  { label: '环流密闭', value: 'CIRCULATION' },
];
const sealingLevelOptions = [
  { label: '一级（半衰期≥300s）', value: 'LEVEL_1' },
  { label: '二级（半衰期≥200s）', value: 'LEVEL_2' },
  { label: '三级（半衰期≥100s）', value: 'LEVEL_3' },
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
const distributionOptions = [
  { label: '上层', value: 'TOP' },
  { label: '中层', value: 'MIDDLE' },
  { label: '下层', value: 'BOTTOM' },
  { label: '全仓', value: 'WHOLE' },
];
const detectionMethodOptions = [
  { label: '筛检', value: 'SIEVE' },
  { label: '诱捕器', value: 'TRAP' },
  { label: '目测', value: 'VISUAL' },
  { label: '诱捕计数', value: 'TRAP_COUNT' },
];
const resistanceLevelOptions = [
  { label: '耐药', value: 'HIGH' },
  { label: '一般', value: 'MEDIUM' },
  { label: '敏感', value: 'LOW' },
];
const seasonOptions = [
  { label: '春', value: 'SPRING' },
  { label: '夏', value: 'SUMMER' },
  { label: '秋', value: 'AUTUMN' },
  { label: '冬', value: 'WINTER' },
];
const cleaningLevelOptions = [
  { label: '良好', value: 'CLEAN' },
  { label: '一般', value: 'NORMAL' },
  { label: '较差', value: 'POOR' },
];
const facilityOptions = [
  { label: '完善', value: 'COMPLETE' },
  { label: '部分', value: 'PARTIAL' },
  { label: '无', value: 'NONE' },
];
const controlMethodOptions = [
  { label: '熏蒸', value: 'FUMIGATION' },
  { label: '通风', value: 'VENTILATION' },
  { label: '物理防治', value: 'PHYSICAL' },
  { label: '防护剂', value: 'CHEMICAL' },
];
const effectRatingOptions = [
  { label: '有效', value: 'EFFECTIVE' },
  { label: '部分有效', value: 'PARTIAL' },
  { label: '无效', value: 'INEFFECTIVE' },
];

const RISK_LEVEL_MAP: Record<string, { theme: 'success' | 'warning' | 'error'; text: string }> = {
  LOW: { theme: 'success', text: '低风险' },
  MEDIUM: { theme: 'warning', text: '中风险' },
  HIGH: { theme: 'error', text: '高风险' },
};

const GRADE_MAP: Record<string, string> = {
  NONE: '基本无虫粮',
  GENERAL: '一般虫粮',
  SEVERE: '严重虫粮',
  DANGEROUS: '危险虫粮',
};

const riskMeta = computed(() => RISK_LEVEL_MAP[result.value?.riskLevel || ''] || { theme: 'warning' as const, text: '—' });
const riskTheme = computed(() => riskMeta.value.theme);
const riskClass = computed(() => `risk-${(result.value?.riskLevel || '').toLowerCase()}`);
const riskText = computed(() => riskMeta.value.text);
const gradeText = computed(() => GRADE_MAP[result.value?.grainPestGrade || ''] || '');

function levelTheme(level?: string): 'success' | 'warning' | 'error' | 'default' {
  if (level === 'NORMAL') return 'success';
  if (level === 'MILD') return 'warning';
  if (level === 'MODERATE' || level === 'SEVERE') return 'error';
  return 'default';
}

function stars(n?: number) {
  const count = Math.max(0, Math.min(5, n || 0));
  return '★'.repeat(count) + '☆'.repeat(5 - count);
}

function addSpeciesRow() {
  form.pestInfo.species.push({ code: '', density: undefined as unknown as number, isMain: false });
}

function fmt(v?: number) {
  return v == null ? '-' : Number(v).toLocaleString('zh-CN');
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
    const res: any = await analyzePest({
      ...form,
      pestInfo: { ...form.pestInfo, species: form.pestInfo.species.filter((s) => s.code) },
    });
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
  .species-list {
    .species-row {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      .sp-code {
        flex: 1;
      }

      .sp-num {
        width: 110px;
      }

      .sp-dist {
        width: 90px;
      }
    }
  }

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

      .deadline {
        margin-top: 6px;
        font-size: 13px;
        font-weight: 600;
        color: var(--td-error-color);
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

  .plan-card {
    border: 1px solid var(--td-component-stroke);
    border-radius: 10px;
    padding: 12px;
    margin-bottom: 12px;

    &.recommended {
      border-color: var(--td-success-color);
      background: var(--td-success-color-1, #e3f9e9);
    }

    .pc-head {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;

      .pc-name {
        font-size: 15px;
        font-weight: 600;
      }

      .pc-stars {
        color: var(--td-warning-color);
        letter-spacing: 2px;
      }
    }

    .pc-note {
      font-size: 12px;
      color: var(--td-text-color-secondary);
      margin-top: 4px;
    }

    .pc-summary {
      font-size: 13px;
      line-height: 20px;
      margin-top: 6px;
    }

    .pc-effect {
      font-size: 13px;
      line-height: 20px;
      margin-top: 4px;
      color: var(--td-brand-color);
    }

    .pc-grid {
      margin-top: 8px;
    }

    .pc-list {
      margin-top: 6px;

      .pc-li {
        font-size: 12px;
        line-height: 20px;

        &.good {
          color: var(--td-success-color);
        }

        &.bad {
          color: var(--td-error-color);
        }
      }
    }
  }
}
</style>
