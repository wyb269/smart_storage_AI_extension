<template>
  <div class="ai-page">
    <!-- 页头 -->
    <div class="page-header">
      <div class="page-title">
        <span class="title-icon">🧪</span>
        <div>
          <div class="title-text">熏蒸作业智能辅助</div>
          <div class="title-desc">智能生成最优熏蒸方案</div>
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
            <t-button v-else theme="warning" variant="outline" :loading="loading" @click="handleGenerate">
              ⚡ 生成方案
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
                <t-form-item label="仓库容量（t）">
                  <t-input-number v-model="form.warehouseInfo.capacity" :min="0" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="粮食数量（t）">
                  <t-input-number v-model="form.warehouseInfo.grainWeight" :min="0" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="仓房空间体积（m³）" name="warehouseInfo.spaceVolume">
                  <t-input-number v-model="form.warehouseInfo.spaceVolume" :min="0" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="粮堆体积（m³）" name="warehouseInfo.grainVolume">
                  <t-input-number v-model="form.warehouseInfo.grainVolume" :min="0" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="粮堆高度（m）">
                  <t-input-number v-model="form.warehouseInfo.pileHeight" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="仓长（m）">
                  <t-input-number v-model="form.warehouseInfo.warehouseLength" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="仓宽（m）">
                  <t-input-number v-model="form.warehouseInfo.warehouseWidth" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="气密性等级" name="warehouseInfo.airtightLevel">
                  <t-select v-model="form.warehouseInfo.airtightLevel" :options="airtightLevelOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="压力半衰期（s）" name="warehouseInfo.airtightDecay">
                  <t-input-number v-model="form.warehouseInfo.airtightDecay" :min="0" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="密闭方式">
                  <t-select v-model="form.warehouseInfo.sealingMethod" :options="sealingMethodOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="具备环流装置">
                  <t-switch v-model="form.warehouseInfo.hasCirculation" :label="['是', '否']" />
                </t-form-item>
              </t-col>
            </t-row>

            <!-- 粮食信息 -->
            <div class="section-title">🌾 粮食信息</div>
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
                <t-form-item label="虫害部位最低粮温（℃）" name="grainInfo.grainTemp">
                  <t-input-number v-model="form.grainInfo.grainTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="平均粮温（℃）">
                  <t-input-number v-model="form.grainInfo.grainAvgTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="已储存天数">
                  <t-input-number v-model="form.grainInfo.storedDays" :min="0" :step="1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="上次熏蒸日期">
                  <t-date-picker v-model="form.grainInfo.lastFumigationDate" mode="date" clearable placeholder="yyyy-MM-dd" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="种子粮">
                  <t-switch v-model="form.grainInfo.seedGrain" :label="['是', '否']" />
                </t-form-item>
              </t-col>
            </t-row>

            <!-- 虫害信息 -->
            <div class="section-title">🐛 虫害信息</div>
            <t-row :gutter="12">
              <t-col :span="12">
                <t-form-item label="虫种（多选）" name="pestInfo.pestSpecies">
                  <t-select v-model="form.pestInfo.pestSpecies" :options="pestSpeciesOptions" multiple clearable placeholder="请选择虫种" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="害虫密度（头/kg）">
                  <t-input-number v-model="form.pestInfo.pestDensity" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="危害等级">
                  <t-select v-model="form.pestInfo.hazardLevel" :options="hazardLevelOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="抗药性">
                  <t-select v-model="form.pestInfo.resistanceLevel" :options="resistanceLevelOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
            </t-row>

            <!-- 环境信息 -->
            <div class="section-title">🌤️ 环境信息</div>
            <t-row :gutter="12">
              <t-col :span="4">
                <t-form-item label="天气">
                  <t-select v-model="form.envInfo.weather" :options="weatherKindOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="4">
                <t-form-item label="气温（℃）">
                  <t-input-number v-model="form.envInfo.airTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="4">
                <t-form-item label="相对湿度（%）">
                  <t-input-number v-model="form.envInfo.airRh" :min="0" :max="100" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
            </t-row>

            <!-- 作业信息 -->
            <div class="section-title">👷 作业信息</div>
            <t-row :gutter="12">
              <t-col :span="6">
                <t-form-item label="作业类型" name="operationInfo.operationType">
                  <t-select v-model="form.operationInfo.operationType" :options="operationTypeOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="计划作业日期" name="operationInfo.planDate">
                  <t-date-picker v-model="form.operationInfo.planDate" mode="date" clearable placeholder="yyyy-MM-dd" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="作业负责人" name="operationInfo.operator">
                  <t-input v-model="form.operationInfo.operator" placeholder="请输入" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="作业人数" name="operationInfo.workerCount">
                  <t-input-number v-model="form.operationInfo.workerCount" :min="0" :step="1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="作业等级" name="operationInfo.operationLevel">
                  <t-select v-model="form.operationInfo.operationLevel" :options="operationLevelOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
            </t-row>

            <!-- 气体参数 -->
            <div class="section-title">💨 气体参数</div>
            <t-row :gutter="12">
              <t-col :span="6">
                <t-form-item label="目标气体" name="gasParam.gasType">
                  <t-select v-model="form.gasParam.gasType" :options="gasTypeOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="目标浓度（ppm）" name="gasParam.targetConcentration">
                  <t-input-number v-model="form.gasParam.targetConcentration" :min="0" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="最低有效浓度（ppm）" name="gasParam.minEffectiveConcentration">
                  <t-input-number v-model="form.gasParam.minEffectiveConcentration" :min="0" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="浓度均匀要求（%）" name="gasParam.concentrationUniformity">
                  <t-input-number v-model="form.gasParam.concentrationUniformity" :min="0" :max="100" theme="column" placeholder="≥90 自动加密取样点" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="CT值要求（ppm·h）" name="gasParam.ctRequirement">
                  <t-input-number v-model="form.gasParam.ctRequirement" :min="0" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="密闭天数（天）" name="gasParam.sealDays">
                  <t-input-number v-model="form.gasParam.sealDays" :min="0" :step="1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="环流条件" name="gasParam.circulationLevel">
                  <t-select v-model="form.gasParam.circulationLevel" :options="circulationLevelOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
            </t-row>

            <!-- 药剂信息 -->
            <div class="section-title">💊 药剂信息</div>
            <t-row :gutter="12">
              <t-col :span="6">
                <t-form-item label="药剂类型" name="chemicalInfo.agentType">
                  <t-select v-model="form.chemicalInfo.agentType" :options="agentTypeOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="施药方式（多选）" name="chemicalInfo.applicationMethods">
                  <t-select v-model="form.chemicalInfo.applicationMethods" :options="applicationMethodOptions" multiple clearable placeholder="请选择" />
                </t-form-item>
              </t-col>
              <t-col :span="12">
                <t-form-item label="历史辅助措施（多选）" name="chemicalInfo.auxiliaryMeasures">
                  <t-select v-model="form.chemicalInfo.auxiliaryMeasures" :options="auxiliaryMeasureOptions" multiple clearable placeholder="请选择" />
                </t-form-item>
              </t-col>
            </t-row>
          </t-form>
        </div>
      </t-col>

      <!-- 右侧：推荐方案（流式回显） -->
      <t-col :lg="7" :md="12">
        <div class="panel">
          <div class="panel-head green">
            <div>
              <div class="panel-title">🧪 推荐熏蒸方案</div>
            </div>
            <span v-if="streaming" class="stream-status">AI 正在输出…</span>
          </div>

          <div ref="streamBoxRef" class="stream-box">
            <t-empty v-if="!result && !generating" description="填写左侧参数后点击「生成方案」" />
            <div v-else class="chat-msg">
              <div class="chat-avatar">🧪</div>
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

                  <!-- 方案结论 -->
                  <template v-if="rk('conclusion')">
                    <t-alert v-if="result.feasible" theme="success">
                      <template #message>
                        <span class="scheme-name">✅ {{ result.schemeName }}<span v-if="result.warehouseName">（{{ result.warehouseName }}）</span></span>
                        <div v-if="result.summary" class="scheme-summary">{{ result.summary }}</div>
                      </template>
                    </t-alert>
                    <t-alert v-else theme="error">
                      <template #message>
                        <div class="scheme-name">❌ 当前条件不满足熏蒸要求</div>
                        <div v-for="(r, i) in result.rejectedReasons" :key="i">· {{ r }}</div>
                        <div v-if="result.summary" class="scheme-summary">{{ result.summary }}</div>
                      </template>
                    </t-alert>
                    <t-alert v-if="result.manualOverrides && result.manualOverrides.length" theme="warning" class="override-alert">
                      <template #message>
                        <div class="scheme-name">⚠️ 人工干预项</div>
                        <div v-for="(r, i) in result.manualOverrides" :key="i">· {{ r }}</div>
                      </template>
                    </t-alert>
                  </template>

                  <!-- 药剂与浓度 -->
                  <template v-if="rk('agent')">
                    <div class="section-title">💊 药剂与浓度设定</div>
                    <div class="check-grid">
                      <div class="check-item">
                        <div class="ci-label">药剂</div>
                        <div class="ci-value">{{ result.agentName || '-' }} <small v-if="result.agentSpec">{{ result.agentSpec }}</small></div>
                        <t-tag v-if="result.resistanceGroup" size="small" theme="primary">抗性分组 {{ result.resistanceGroup }}</t-tag>
                      </div>
                      <div class="check-item">
                        <div class="ci-label">设定浓度</div>
                        <div class="ci-value">{{ result.targetConcentration ?? '-' }} <small>mL/m³</small></div>
                      </div>
                      <div class="check-item">
                        <div class="ci-label">最低有效浓度</div>
                        <div class="ci-value">{{ result.minEffectiveConcentration ?? '-' }} <small>mL/m³</small></div>
                      </div>
                      <div class="check-item">
                        <div class="ci-label">CT 值</div>
                        <div class="ci-value">{{ fmt(result.ctValue) }} <small>ppm·h</small></div>
                        <t-tag v-if="result.ctRequired != null" size="small" theme="success">要求 {{ result.ctRequired }}</t-tag>
                      </div>
                      <div class="check-item">
                        <div class="ci-label">密闭天数</div>
                        <div class="ci-value">{{ result.schedule?.sealDays ?? '-' }} <small>天</small></div>
                      </div>
                      <div class="check-item">
                        <div class="ci-label">气体取样点</div>
                        <div class="ci-value">{{ result.samplingPoints ?? '-' }} <small>个</small></div>
                      </div>
                    </div>
                  </template>

                  <!-- 用药量 -->
                  <template v-if="rk('dose') && result.dose">
                    <div class="section-title">⚖️ 用药量计算</div>
                    <div class="check-grid">
                      <div v-if="result.dose.dosePerVolumePile != null" class="check-item">
                        <div class="ci-label">粮堆单位用药量</div>
                        <div class="ci-value">{{ result.dose.dosePerVolumePile }} <small>g/m³</small></div>
                      </div>
                      <div v-if="result.dose.dosePerVolumeSpace != null" class="check-item">
                        <div class="ci-label">空间单位用药量</div>
                        <div class="ci-value">{{ result.dose.dosePerVolumeSpace }} <small>g/m³</small></div>
                      </div>
                      <div v-if="result.dose.dosePile != null" class="check-item">
                        <div class="ci-label">粮堆用药量</div>
                        <div class="ci-value">{{ fmt(result.dose.dosePile) }} <small>kg</small></div>
                      </div>
                      <div v-if="result.dose.doseSpace != null" class="check-item">
                        <div class="ci-label">空间用药量</div>
                        <div class="ci-value">{{ fmt(result.dose.doseSpace) }} <small>kg</small></div>
                      </div>
                      <div class="check-item">
                        <div class="ci-label">总药量</div>
                        <div class="ci-value">{{ fmt(result.dose.totalDose) }} <small>kg</small></div>
                      </div>
                      <div v-if="result.dose.tabletCount != null" class="check-item">
                        <div class="ci-label">片数</div>
                        <div class="ci-value">{{ fmt(result.dose.tabletCount) }} <small>片</small></div>
                      </div>
                      <div v-if="result.dose.applicationPoints != null" class="check-item">
                        <div class="ci-label">投药点数</div>
                        <div class="ci-value">{{ result.dose.applicationPoints }} <small>个</small></div>
                      </div>
                      <div v-if="result.dose.maxPerPoint != null" class="check-item">
                        <div class="ci-label">每点最大药量</div>
                        <div class="ci-value">{{ result.dose.maxPerPoint }} <small>g</small></div>
                      </div>
                    </div>
                    <div v-if="result.dose.pointDetails && result.dose.pointDetails.length" class="advice-list">
                      <div v-for="(p, i) in result.dose.pointDetails" :key="i" class="advice-item">
                        · {{ p.location }} {{ p.points }} 处，每点 {{ p.dosePerPoint }} g
                      </div>
                    </div>
                  </template>

                  <!-- 时间安排 -->
                  <template v-if="rk('schedule') && result.schedule">
                    <div class="section-title">🕒 时间安排</div>
                    <div class="check-grid">
                      <div class="check-item">
                        <div class="ci-label">总作业周期</div>
                        <div class="ci-value">{{ result.schedule.totalDays ?? '-' }} <small>天</small></div>
                      </div>
                      <div class="check-item">
                        <div class="ci-label">密闭熏蒸</div>
                        <div class="ci-value">{{ result.schedule.sealDays ?? '-' }} <small>天</small></div>
                      </div>
                      <div class="check-item">
                        <div class="ci-label">散气排毒</div>
                        <div class="ci-value">{{ result.schedule.aerateDays ?? '-' }} <small>天</small></div>
                      </div>
                    </div>
                    <div v-if="result.schedule.aerationTargetConcentration != null" class="advice-list">
                      <div class="advice-item">· 散气目标浓度 ≤ {{ result.schedule.aerationTargetConcentration }} mL/m³，低于该值方可进仓</div>
                    </div>
                    <div v-if="result.schedule.timeline && result.schedule.timeline.length" class="timeline-list">
                      <div v-for="(t, i) in result.schedule.timeline" :key="i" class="timeline-item">
                        <t-tag size="small" theme="primary">{{ t.phase }}</t-tag>
                        <b>{{ t.name }}</b>
                        <span class="tl-desc">{{ t.desc }}</span>
                      </div>
                    </div>
                  </template>

                  <!-- 浓度检测计划 -->
                  <template v-if="rk('check') && result.concentrationCheck && result.concentrationCheck.length">
                    <div class="section-title">📋 浓度检测计划</div>
                    <div class="check-row-list">
                      <div v-for="(c, i) in result.concentrationCheck" :key="i" class="check-row">
                        <t-tag size="small" theme="primary">{{ c.time }}</t-tag>
                        <span>{{ c.require }}</span>
                      </div>
                    </div>
                  </template>

                  <!-- 操作步骤 -->
                  <template v-if="rk('steps') && result.operationSteps && result.operationSteps.length">
                    <div class="section-title">📖 操作步骤</div>
                    <div class="step-list">
                      <div v-for="s in result.operationSteps" :key="s.stepNo" class="step-item">
                        <t-tag shape="circle" theme="primary" size="small">{{ s.stepNo }}</t-tag>
                        <div>
                          <b>{{ s.title }}</b>
                          <div class="step-content">{{ s.content }}</div>
                        </div>
                      </div>
                    </div>
                  </template>

                  <!-- 安全警示 -->
                  <template v-if="rk('safety') && result.safetyTips && result.safetyTips.length">
                    <div class="safety-box">
                      <div class="section-title">⚠️ 安全警示（务必遵守）</div>
                      <ul class="safety-list">
                        <li v-for="(t, i) in result.safetyTips" :key="i">{{ t }}</li>
                      </ul>
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
import { ref, reactive, onMounted } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';
import { getList as getWarehouseList } from '@/api/warehouse';
import { generateFumigationPlan } from '@/api/fumigation';
import type { FumigationPlanReq, FumigationPlanResult } from '@/api/fumigation';
import { useStreamReveal } from '../useStreamReveal';

const SECTION_KEYS = ['ask', 'conclusion', 'agent', 'dose', 'schedule', 'check', 'steps', 'safety'];
const loading = ref(false);
const whLoading = ref(false);
const result = ref<FumigationPlanResult | null>(null);
const streamBoxRef = ref<HTMLElement>();
const { typing, streaming, generating, askText, rk, reveal, typeAsk, finishAll, abort } = useStreamReveal(
  SECTION_KEYS,
  () => streamBoxRef.value,
  () => !!result.value?.requireMoreInput,
);

const form = reactive<FumigationPlanReq>({
  warehouseInfo: { hasCirculation: false },
  grainInfo: { seedGrain: false },
  pestInfo: { pestSpecies: [] },
  envInfo: {},
  operationInfo: {},
  gasParam: {},
  chemicalInfo: {},
});

const warehouseOptions = ref<{ label: string; value: string }[]>([]);

const warehouseTypeOptions = [
  { label: '平房仓', value: 'FLAT' },
  { label: '浅圆仓', value: 'SHALLOW' },
  { label: '立筒仓', value: 'SILO' },
];
const airtightLevelOptions = [
  { label: '一级（半衰期≥300s）', value: 'LEVEL_1' },
  { label: '二级（半衰期≥200s）', value: 'LEVEL_2' },
  { label: '三级（半衰期≥100s）', value: 'LEVEL_3' },
];
const sealingMethodOptions = [
  { label: '整仓密闭', value: 'WHOLE' },
  { label: '膜下密闭', value: 'UNDER_FILM' },
  { label: '环流密闭', value: 'CIRCULATION' },
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
const hazardLevelOptions = [
  { label: '一般', value: 'GENERAL' },
  { label: '严重', value: 'SEVERE' },
  { label: '极重', value: 'EXTREME' },
];
const resistanceLevelOptions = [
  { label: '耐药', value: 'HIGH' },
  { label: '一般', value: 'MEDIUM' },
  { label: '敏感', value: 'LOW' },
];
const weatherKindOptions = [
  { label: '晴', value: '晴' },
  { label: '阴', value: '阴' },
  { label: '雨', value: '雨' },
];
const operationTypeOptions = [
  { label: '常规熏蒸', value: 'ROUTINE' },
  { label: '外包作业', value: 'EXTERNAL' },
  { label: '应急熏蒸', value: 'EMERGENCY' },
];
const operationLevelOptions = [
  { label: '一般作业', value: 'GENERAL' },
  { label: '重大作业', value: 'MAJOR' },
  { label: '特殊作业', value: 'CRITICAL' },
];
const gasTypeOptions = [
  { label: '磷化氢', value: 'PH3' },
  { label: '硫酰氟', value: 'SF' },
  { label: '二氧化碳', value: 'CO2' },
];
const circulationLevelOptions = [
  { label: '有环流', value: 'CIRCULATION' },
  { label: '无环流', value: 'NATURAL' },
];
const agentTypeOptions = [
  { label: '磷化铝', value: 'ALUMINIUM_PHOSPHIDE' },
  { label: '磷化镁', value: 'MAGNESIUM_PHOSPHIDE' },
];
const applicationMethodOptions = [
  { label: '粮面施药', value: 'SURFACE' },
  { label: '探管施药', value: 'PROBE' },
  { label: '环流熏蒸', value: 'CIRCULATION' },
  { label: '气囊施药', value: 'BAG' },
];
const auxiliaryMeasureOptions = [
  { label: '清洁卫生', value: 'CLEANING' },
  { label: '通风降温预处理', value: 'VENTILATION_PRETREAT' },
  { label: '防虫线设置', value: 'PEST_LINE' },
  { label: '翻仓倒仓', value: 'TURNOVER' },
];

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
    // 加载失败不阻塞表单，仓库编码可手填
  } finally {
    whLoading.value = false;
  }
}

function onWarehouseChange(v: string) {
  const hit = warehouseOptions.value.find((o) => o.value === v);
  form.warehouseInfo.warehouseName = hit?.label || '';
}

function fmt(v?: number) {
  return v == null ? '-' : Number(v).toLocaleString('zh-CN');
}

async function handleGenerate() {
  loading.value = true;
  generating.value = true;
  typing.value = true;
  try {
    const res: any = await generateFumigationPlan({
      ...form,
      pestInfo: { ...form.pestInfo, pestSpecies: form.pestInfo.pestSpecies.filter(Boolean) },
    });
    if (res?.status?.code === '200' || res?.result) {
      result.value = res.result;
      if (res.result?.requireMoreInput) {
        await typeAsk(res.result.aiMessage ?? '');
      } else {
        MessagePlugin.success(res?.status?.message || '方案生成成功');
        await reveal();
      }
    } else {
      MessagePlugin.error(res?.status?.message || '方案生成失败');
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
</style>
