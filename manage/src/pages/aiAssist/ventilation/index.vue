<template>
  <div class="ventilation-page">
    <!-- 页头 -->
    <div class="page-header">
      <div class="page-title">
        <span class="title-icon">🍃</span>
        <div>
          <div class="title-text">通风作业智慧辅助</div>
          <div class="title-desc">智能生成最优通风方案</div>
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
            <t-button v-if="streaming" theme="danger" variant="outline" @click="stopStream">
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
                <t-form-item label="仓型" name="grainInfo.warehouseType">
                  <t-select v-model="form.grainInfo.warehouseType" :options="warehouseTypeOptions" placeholder="请选择" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="粮种" name="grainInfo.grainType">
                  <t-select v-model="form.grainInfo.grainType" :options="grainTypeOptions" placeholder="请选择" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="仓内温度（℃）">
                  <t-input-number v-model="form.warehouseTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="仓内湿度（%）">
                  <t-input-number v-model="form.warehouseRh" :min="0" :max="100" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
            </t-row>

            <!-- 通风目标 -->
            <div class="section-title">🎯 通风目标</div>
            <t-form-item name="ventilationGoals">
              <t-checkbox-group v-model="form.ventilationGoals">
                <t-checkbox value="COOLING" label="降温通风" />
                <t-checkbox value="MOISTURE_REDUCTION" label="降水通风" />
                <t-checkbox value="CONDITIONING" label="调质通风" />
                <t-checkbox value="BALANCE" label="平衡防结露" />
              </t-checkbox-group>
            </t-form-item>
            <t-row :gutter="12">
              <t-col :span="6">
                <t-form-item label="目标粮温（℃）">
                  <t-input-number v-model="form.targetGrainTemp" :step="0.1" theme="column" placeholder="降温/平衡通风达标目标" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="目标水分（%）">
                  <t-input-number v-model="form.targetMoisture" :step="0.1" theme="column" placeholder="降水/调质通风达标目标" style="width: 100%" />
                </t-form-item>
              </t-col>
            </t-row>

            <!-- 粮情参数 -->
            <div class="section-title">🌾 粮情参数</div>
            <t-row :gutter="12">
              <t-col :span="6">
                <t-form-item label="粮食质量 G（t）" name="grainInfo.grainWeight">
                  <t-input-number v-model="form.grainInfo.grainWeight" :min="0" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="粮堆体积 V（m³）" name="grainInfo.grainVolume">
                  <t-input-number v-model="form.grainInfo.grainVolume" :min="0" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="容重 r（t/m³）" name="grainInfo.bulkDensity">
                  <t-input-number v-model="form.grainInfo.bulkDensity" :min="0" :step="0.01" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="粮堆高度 h（m）" name="grainInfo.pileHeight">
                  <t-input-number v-model="form.grainInfo.pileHeight" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="粮食水分 w（%）" name="grainInfo.grainMoisture">
                  <t-input-number v-model="form.grainInfo.grainMoisture" :min="0" :max="30" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="安全水分（%）">
                  <t-input-number v-model="form.grainInfo.safeMoisture" :min="0" :max="30" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="平均粮温 t2（℃）" name="grainInfo.grainAvgTemp">
                  <t-input-number v-model="form.grainInfo.grainAvgTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="上/下层温差（℃）">
                  <t-input-number v-model="form.grainInfo.topBottomTempDiff" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="温度梯度（℃/m）">
                  <t-input-number v-model="form.grainInfo.tempGradient" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="水分梯度（%/m）">
                  <t-input-number v-model="form.grainInfo.moistureGradient" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="上/下层水分差（%）">
                  <t-input-number v-model="form.grainInfo.topBottomMoistureDiff" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
            </t-row>

            <!-- 风网设备参数 -->
            <div class="section-title">⚙️ 风网设备参数</div>
            <t-row :gutter="12">
              <t-col :span="6">
                <t-form-item label="风机类型" name="deviceInfo.fanType">
                  <t-select v-model="form.deviceInfo.fanType" :options="fanTypeOptions" placeholder="请选择" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="送风方式" name="deviceInfo.airSupplyMode">
                  <t-radio-group v-model="form.deviceInfo.airSupplyMode" variant="default-filled">
                    <t-radio-button value="PRESSURE">压入式</t-radio-button>
                    <t-radio-button value="SUCTION">吸出式</t-radio-button>
                    <t-radio-button value="MIXED">混合式</t-radio-button>
                  </t-radio-group>
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="风机数量（台）" name="deviceInfo.fanCount">
                  <t-input-number v-model="form.deviceInfo.fanCount" :min="0" :step="1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="单台风量（m³/h）" name="deviceInfo.fanAirflowPerUnit">
                  <t-input-number v-model="form.deviceInfo.fanAirflowPerUnit" :min="0" theme="column" placeholder="与台数合成总风量" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="单台功率（kW）" name="deviceInfo.fanPower">
                  <t-input-number v-model="form.deviceInfo.fanPower" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="压紧系数 φ" name="deviceInfo.compactionFactor">
                  <t-input-number v-model="form.deviceInfo.compactionFactor" :min="1" :max="1.55" :step="0.05" theme="column" placeholder="1.30~1.55" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="主风道面积（m²）" name="deviceInfo.mainDuctArea">
                  <t-input-number v-model="form.deviceInfo.mainDuctArea" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="支风道面积（m²）" name="deviceInfo.branchDuctArea">
                  <t-input-number v-model="form.deviceInfo.branchDuctArea" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="现有风道间距 L（m）" name="deviceInfo.ductSpacing">
                  <t-input-number v-model="form.deviceInfo.ductSpacing" :min="0" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="亚热带地区">
                  <t-switch v-model="form.subtropical" :label="['是', '否']" />
                </t-form-item>
              </t-col>
            </t-row>

            <!-- 使用约束 -->
            <div class="section-title">🔧 使用约束（多选）</div>
            <t-row :gutter="12">
              <t-col :span="6">
                <t-form-item label="允许通风时段">
                  <t-tag-input v-model="form.constraintInfo.allowTimeRanges" size="small" clearable placeholder="如 20:00-08:00，回车添加" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="最长连续通风时长（h）">
                  <t-input-number v-model="form.constraintInfo.maxContinuousHours" :min="0" :step="0.5" theme="column" placeholder="超出自动切分" style="width: 100%" />
                </t-form-item>
              </t-col>
            </t-row>
            <t-form-item>
              <t-checkbox v-model="form.constraintInfo.naturalOnly">只利用自然通风（零能耗）</t-checkbox>
              <t-checkbox v-model="form.constraintInfo.avoidRainHumidity">避开雨天及高湿天气</t-checkbox>
              <t-checkbox v-model="form.constraintInfo.peakValleyPricing">考虑峰谷电价（谷段优先）</t-checkbox>
            </t-form-item>

            <!-- 天气预报 -->
            <div class="section-title">
              🌤️ 未来天气预报
              <t-button size="small" variant="text" theme="primary" @click="fillDemoWeather">填充未来24h示例</t-button>
            </div>
            <div class="weather-list">
              <div v-for="(w, i) in form.weatherForecast" :key="i" class="weather-row">
                <t-input v-model="w.time" placeholder="yyyy-MM-ddTHH:mm:ss" size="small" class="wt-time" />
                <t-input-number v-model="w.temp" :step="0.1" size="small" theme="column" placeholder="气温℃" class="wt-num" />
                <t-input-number v-model="w.rh" :min="0" :max="100" size="small" theme="column" placeholder="湿度%" class="wt-num" />
                <t-select v-model="w.weather" :options="weatherKindOptions" size="small" class="wt-weather" clearable placeholder="天气" />
                <t-button size="small" variant="text" theme="danger" @click="form.weatherForecast.splice(i, 1)">删除</t-button>
              </div>
              <t-button size="small" variant="dashed" block @click="addWeatherRow">+ 添加时段</t-button>
            </div>

            <!-- 历史粮情与通风效果 -->
            <div class="section-title">📜 历史粮情与通风效果</div>
            <t-row :gutter="12">
              <t-col :span="6">
                <t-form-item label="去年同期粮温（℃）">
                  <t-input-number v-model="form.historyInfo.lastYearGrainTemp" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="去年同期水分（%）">
                  <t-input-number v-model="form.historyInfo.lastYearMoisture" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="近一年通风次数">
                  <t-input-number v-model="form.historyInfo.ventilationTimesLastYear" :min="0" :step="1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="上次通风类型">
                  <t-select v-model="form.historyInfo.lastVentilationType" :options="historyTypeOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="上次通风时长（h）">
                  <t-input-number v-model="form.historyInfo.lastVentilationHours" :min="0" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="历史通风效果评价">
                  <t-select v-model="form.historyInfo.lastEffectRating" :options="effectRatingOptions" placeholder="请选择" clearable />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="通风前粮温（℃）">
                  <t-input-number v-model="form.historyInfo.lastTempBefore" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="通风后粮温（℃）">
                  <t-input-number v-model="form.historyInfo.lastTempAfter" :step="0.1" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
              <t-col :span="6">
                <t-form-item label="上次通风能耗（kWh）">
                  <t-input-number v-model="form.historyInfo.lastEnergyKwh" :min="0" theme="column" placeholder="请输入" style="width: 100%" />
                </t-form-item>
              </t-col>
            </t-row>
          </t-form>
        </div>
      </t-col>

      <!-- 右侧：推荐方案 -->
      <t-col :lg="7" :md="12">
        <div class="panel">
          <div class="panel-head green">
            <div>
              <div class="panel-title">🍃 推荐通风方案</div>
            </div>
            <span v-if="streaming" class="stream-status">AI 正在输出…</span>
          </div>

          <div ref="streamBoxRef" class="stream-box">
            <t-empty v-if="!result && !generating" description="填写左侧参数后点击「生成方案」" />
            <div v-else class="chat-msg">
              <div class="chat-avatar">🍃</div>
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

            <!-- 方案结论卡 -->
            <template v-if="rk('conclusion')">
            <t-alert v-if="result.feasible" theme="success">
              <template #message>
                <span class="scheme-name">✅ 推荐方案：{{ result.schemeName }}</span>
                <div v-if="result.summary" class="scheme-summary">{{ result.summary }}</div>
              </template>
            </t-alert>
            <t-alert v-else theme="error">
              <template #message>
                <div class="scheme-name">❌ 当前条件不满足通风要求</div>
                <div v-for="(r, i) in result.rejectedReasons" :key="i">· {{ r }}</div>
                <div v-if="result.summary" class="scheme-summary">{{ result.summary }}</div>
              </template>
            </t-alert>
            </template>

            <!-- 预计效果 -->
            <template v-if="rk('estimate')">
            <div class="check-grid estimate-grid">
              <div v-if="result.estimatedTargetTemp != null" class="check-item">
                <div class="ci-label">预计可达粮温</div>
                <div class="ci-value">{{ result.estimatedTargetTemp }} <small>℃</small></div>
                <t-tag v-if="result.targetReached != null" size="small" :theme="result.targetReached ? 'success' : 'warning'">
                  {{ result.targetReached ? '可达标' : '未达标' }}
                </t-tag>
              </div>
              <div v-if="result.coolingRatePerHour != null" class="check-item">
                <div class="ci-label">降温速率</div>
                <div class="ci-value">{{ result.coolingRatePerHour }} <small>℃/h</small></div>
              </div>
              <div v-if="result.moistureDropRatePerHour != null" class="check-item">
                <div class="ci-label">降水速率</div>
                <div class="ci-value">{{ result.moistureDropRatePerHour }} <small>%/h</small></div>
              </div>
              <div v-if="result.estimatedHours != null" class="check-item">
                <div class="ci-label">预计通风时长</div>
                <div class="ci-value">{{ result.estimatedHours }} <small>h</small></div>
              </div>
              <div v-if="result.installedPower != null" class="check-item">
                <div class="ci-label">装机功率</div>
                <div class="ci-value">{{ result.installedPower }} <small>kW</small></div>
              </div>
              <div v-if="result.estimatedEnergy != null" class="check-item">
                <div class="ci-label">预计耗电量</div>
                <div class="ci-value">{{ fmt(result.estimatedEnergy) }} <small>kWh</small></div>
              </div>
              <div v-if="result.unitEnergy != null" class="check-item">
                <div class="ci-label">单位能耗</div>
                <div class="ci-value">{{ result.unitEnergy }}</div>
              </div>
            </div>
            </template>

            <!-- 约束条件校验 -->
            <template v-if="rk('constraints') && result.constraintChecks && result.constraintChecks.length">
              <div class="section-title">🔧 约束条件校验</div>
              <div class="check-row-list">
                <div v-for="(c, i) in result.constraintChecks" :key="i" class="check-row">
                  <t-tag size="small" :theme="c.passed ? 'success' : 'warning'">{{ c.passed ? '满足' : '不满足' }}</t-tag>
                  <b>{{ c.name }}</b>：{{ c.condition }}<span v-if="c.actual">，{{ c.actual }}</span>
                </div>
              </div>
            </template>

            <!-- 历史对比与专项建议 -->
            <template v-if="rk('advices') && result.advices && result.advices.length">
              <div class="section-title">💡 专项建议</div>
              <div class="advice-list">
                <div v-for="(a, i) in result.advices" :key="i" class="advice-item">· {{ a }}</div>
              </div>
            </template>

            <!-- 关键参数核算 -->
            <template v-if="rk('params')">
            <div class="section-title">📐 关键参数核算</div>
            <div class="check-grid">
              <div class="check-item" :class="{ fail: !result.unitAirflow.qualified }">
                <div class="ci-label">单位通风量</div>
                <div class="ci-value">{{ result.unitAirflow.value }} <small>m³/(h·t)</small></div>
                <t-tag size="small" :theme="result.unitAirflow.qualified ? 'success' : 'danger'">
                  限值 {{ result.unitAirflow.limit }}
                </t-tag>
              </div>
              <div class="check-item">
                <div class="ci-label">总通风量 Q总</div>
                <div class="ci-value">{{ fmt(result.totalAirflow) }} <small>m³/h</small></div>
              </div>
              <div class="check-item">
                <div class="ci-label">风机风量</div>
                <div class="ci-value">{{ fmt(result.fanAirflow) }} <small>m³/h</small></div>
              </div>
              <div class="check-item">
                <div class="ci-label">风机风压</div>
                <div class="ci-value">{{ fmt(result.fanPressure) }} <small>Pa</small></div>
              </div>
              <div class="check-item" :class="{ fail: !result.totalResistance.qualified }">
                <div class="ci-label">系统总阻力</div>
                <div class="ci-value">{{ fmt(result.totalResistance.grainLayerResistance) }} <small>Pa</small></div>
                <t-tag size="small" :theme="result.totalResistance.qualified ? 'success' : 'danger'">
                  限值 {{ result.totalResistance.limit }} Pa
                </t-tag>
              </div>
              <div class="check-item" :class="{ fail: !result.airPathRatio.qualified }">
                <div class="ci-label">空气途径比</div>
                <div class="ci-value">{{ result.airPathRatio.value }}</div>
                <t-tag size="small" :theme="result.airPathRatio.qualified ? 'success' : 'danger'">
                  {{ result.airPathRatio.limit }}
                </t-tag>
              </div>
              <div class="check-item" :class="{ fail: !result.ductSpacing.qualified }">
                <div class="ci-label">风道间距推荐</div>
                <div class="ci-value">{{ result.ductSpacing.recommended }} <small>m</small></div>
                <t-tag size="small" :theme="result.ductSpacing.qualified ? 'success' : 'warning'">
                  {{ result.ductSpacing.qualified ? '满足' : '需调整' }}
                </t-tag>
              </div>
              <div v-if="result.ductVelocity.main" class="check-item" :class="{ fail: !result.ductVelocity.main.qualified }">
                <div class="ci-label">主风道风速</div>
                <div class="ci-value">{{ result.ductVelocity.main.value }} <small>m/s</small></div>
                <t-tag size="small" :theme="result.ductVelocity.main.qualified ? 'success' : 'danger'">
                  ≤{{ result.ductVelocity.main.limit }}
                </t-tag>
              </div>
              <div v-if="result.ductVelocity.branch" class="check-item" :class="{ fail: !result.ductVelocity.branch.qualified }">
                <div class="ci-label">支风道风速</div>
                <div class="ci-value">{{ result.ductVelocity.branch.value }} <small>m/s</small></div>
                <t-tag size="small" :theme="result.ductVelocity.branch.qualified ? 'success' : 'danger'">
                  ≤{{ result.ductVelocity.branch.limit }}
                </t-tag>
              </div>
            </div>
            </template>

            <!-- 推荐通风时段 -->
            <template v-if="rk('windows')">
            <div class="section-title">🕒 推荐通风时段</div>
            <t-empty v-if="!result.ventilationWindows?.length" description="预报时段内均不满足通风条件" />
            <div v-else class="window-list">
              <div v-for="(w, i) in result.ventilationWindows" :key="i" class="window-item">
                <div class="wi-head">
                  <span class="wi-title">第{{ i + 1 }}次通风</span>
                  <t-tag size="small" theme="success">{{ w.judgement }}</t-tag>
                </div>
                <div class="wi-time">{{ fmtTime(w.startTime) }} ~ {{ fmtTime(w.endTime) }}（{{ w.hours }}小时）</div>
                <div class="wi-meta">
                  平均气温 {{ w.avgTemp }}℃ · 平均湿度 {{ w.avgRh }}% · 温差 {{ w.tempDiff }}℃
                  <template v-if="w.tempDrop != null"> · 预计降温 {{ w.tempDrop }}℃</template>
                  <template v-if="w.energyKwh != null"> · 预计耗电 {{ w.energyKwh }} kWh</template>
                  <template v-if="w.peakHours"> · 峰段 {{ w.peakHours }}h</template>
                </div>
                <t-collapse v-if="w.checkItems && w.checkItems.length" class="wi-collapse">
                  <t-collapse-panel value="detail" header="判定依据">
                    <div v-for="(c, j) in w.checkItems" :key="j" class="check-row">
                      <t-tag size="small" :theme="c.passed ? 'success' : 'danger'">{{ c.passed ? '通过' : '未过' }}</t-tag>
                      <b>{{ c.name }}</b>：{{ c.condition }}，实际 {{ c.actual }}
                    </div>
                  </t-collapse-panel>
                </t-collapse>
              </div>
            </div>
            </template>

            <!-- 结束通风条件 -->
            <template v-if="rk('stop') && result.stopConditions && result.stopConditions.length">
              <div class="section-title">🛑 结束通风条件（实时监测）</div>
              <div class="check-row-list">
                <div v-for="(c, i) in result.stopConditions" :key="i" class="check-row">
                  <t-tag size="small" :theme="c.passed ? 'success' : 'warning'">{{ c.passed ? '已满足' : '未满足' }}</t-tag>
                  <b>{{ c.name }}</b>：{{ c.condition }}，当前 {{ c.actual }}
                </div>
              </div>
            </template>

            <!-- 判定明细 -->
            <template v-if="rk('judge') && result.judgeDetail && result.judgeDetail.length">
              <div class="section-title">📋 判定明细</div>
              <div class="check-row-list">
                <div v-for="(c, i) in result.judgeDetail" :key="i" class="check-row">
                  <t-tag size="small" :theme="c.passed ? 'success' : 'danger'">{{ c.passed ? '通过' : '未过' }}</t-tag>
                  <b>{{ c.name }}</b>：{{ c.condition }}，{{ c.actual }}
                </div>
              </div>
            </template>

            <!-- 操作步骤指引 -->
            <template v-if="rk('steps')">
            <div class="section-title">📖 操作步骤指引</div>
            <div class="step-list">
              <div v-for="s in result.operationSteps" :key="s.order" class="step-item">
                <t-tag shape="round" theme="primary" size="small">{{ s.order }}</t-tag>
                <span>{{ s.text }}</span>
              </div>
            </div>
            </template>

            <!-- 安全注意事项 -->
            <div v-if="rk('safety')" class="safety-box">
              <div class="section-title">⚠️ 安全注意事项</div>
              <ul class="safety-list">
                <li v-for="(t, i) in result.safetyTips" :key="i">{{ t }}</li>
              </ul>
            </div>
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
import { ref, reactive, watch, nextTick } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';
import { generateVentilationPlan } from '@/api/ventilation';
import type { VentilationPlanReq, VentilationPlanResult, WeatherPoint } from '@/api/ventilation';

const loading = ref(false);
const result = ref<VentilationPlanResult | null>(null);

// ===== 模拟流式回显（类大模型对话输出）=====
const SECTION_KEYS = ['ask', 'conclusion', 'estimate', 'constraints', 'advices', 'params', 'windows', 'stop', 'judge', 'steps', 'safety'];
const streamBoxRef = ref<HTMLElement>();
const revealedCount = ref(0);
const typing = ref(false);
const streaming = ref(false);
const generating = ref(false);
/** AI 追问文案的逐字流式输出内容 */
const askText = ref('');
let askFull = '';
let streamToken = 0;

function rk(key: string) {
  if (!result.value) {
    return false;
  }
  if (key === 'ask') {
    // 追问模式不跑分块 reveal，跟随已输出的文字显隐
    return !!result.value.requireMoreInput && askText.value.length > 0;
  }
  return !result.value.requireMoreInput && revealedCount.value > SECTION_KEYS.indexOf(key);
}

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** AI 追问文案逐字打字机输出（模拟大模型流式回复）：先思考 1.2~2.8s，再 28~68ms/字 */
async function typeAsk(message: string) {
  const token = streamToken;
  askFull = message;
  askText.value = '';
  streaming.value = true;
  generating.value = true;
  // 模拟 AI 思考：打字点先动一会儿再开始输出
  await sleep(1200 + Math.random() * 1600);
  if (token !== streamToken) return;
  typing.value = false;
  for (let i = 1; i <= message.length; i++) {
    if (token !== streamToken) return;
    askText.value = message.slice(0, i);
    await sleep(28 + Math.random() * 40);
  }
  if (token !== streamToken) return;
  streaming.value = false;
  generating.value = false;
}

async function streamReveal() {
  const token = streamToken;
  streaming.value = true;
  revealedCount.value = 0;
  for (let i = 0; i < SECTION_KEYS.length; i++) {
    typing.value = true;
    await sleep(250 + Math.random() * 350);
    if (token !== streamToken) return;
    typing.value = false;
    revealedCount.value = i + 1;
    if (i < SECTION_KEYS.length - 1) await sleep(100);
    if (token !== streamToken) return;
  }
  streaming.value = false;
  generating.value = false;
}

function stopStream() {
  streamToken += 1;
  typing.value = false;
  streaming.value = false;
  generating.value = false;
  revealedCount.value = SECTION_KEYS.length;
  askText.value = askFull;
}

watch([revealedCount, typing, askText], async () => {
  await nextTick();
  streamBoxRef.value?.scrollTo({ top: streamBoxRef.value.scrollHeight, behavior: 'smooth' });
});

const form = reactive<VentilationPlanReq>({
  ventilationGoals: [],
  subtropical: false,
  targetGrainTemp: undefined,
  targetMoisture: undefined,
  warehouseTemp: undefined,
  warehouseRh: undefined,
  grainInfo: {
    warehouseType: undefined,
    grainType: undefined,
    grainWeight: undefined,
    grainVolume: undefined,
    bulkDensity: undefined,
    grainMoisture: undefined,
    safeMoisture: undefined,
    pileHeight: undefined,
    grainAvgTemp: undefined,
    topBottomTempDiff: undefined,
    tempGradient: undefined,
    moistureGradient: undefined,
    topBottomMoistureDiff: undefined,
  },
  deviceInfo: {
    fanType: undefined,
    airSupplyMode: undefined,
    fanAirflowPerUnit: undefined,
    fanCount: undefined,
    fanPower: undefined,
    mainDuctArea: undefined,
    branchDuctArea: undefined,
    ductSpacing: undefined,
    compactionFactor: undefined,
  },
  constraintInfo: {
    allowTimeRanges: [],
    naturalOnly: false,
    avoidRainHumidity: false,
    peakValleyPricing: false,
    maxContinuousHours: undefined,
  },
  historyInfo: {
    lastYearGrainTemp: undefined,
    lastYearMoisture: undefined,
    ventilationTimesLastYear: undefined,
    lastVentilationType: undefined,
    lastVentilationHours: undefined,
    lastTempBefore: undefined,
    lastTempAfter: undefined,
    lastEffectRating: undefined,
    lastEnergyKwh: undefined,
  },
  weatherForecast: [],
});

const warehouseTypeOptions = [
  { label: '房式仓', value: 'FLAT' },
  { label: '浅圆仓', value: 'SHALLOW' },
  { label: '立筒仓', value: 'SILO' },
];
const grainTypeOptions = [
  { label: '小麦', value: 'WHEAT' },
  { label: '玉米', value: 'CORN' },
  { label: '稻谷', value: 'PADDY' },
  { label: '大米', value: 'RICE' },
  { label: '大豆', value: 'SOYBEAN' },
  { label: '大麦', value: 'BARLEY' },
  { label: '花生', value: 'PEANUT' },
];
const fanTypeOptions = [
  { label: '离心风机', value: 'CENTRIFUGAL' },
  { label: '轴流风机', value: 'AXIAL' },
  { label: '混流风机', value: 'MIXED' },
  { label: '排风扇', value: 'EXHAUST' },
];
const weatherKindOptions = [
  { label: '晴', value: '晴' },
  { label: '阴', value: '阴' },
  { label: '雨', value: '雨' },
];
const historyTypeOptions = [
  { label: '降温', value: '降温' },
  { label: '降水', value: '降水' },
  { label: '调质', value: '调质' },
  { label: '环流', value: '环流' },
];
const effectRatingOptions = [
  { label: '良好', value: 'GOOD' },
  { label: '一般', value: 'MEDIUM' },
  { label: '较差', value: 'BAD' },
];

function pad(n: number) {
  return n < 10 ? `0${n}` : `${n}`;
}

function fillDemoWeather() {
  const rows: WeatherPoint[] = [];
  const kinds = ['晴', '晴', '阴', '阴', '雨'];
  const base = new Date();
  base.setMinutes(0, 0, 0);
  for (let i = 1; i <= 24; i++) {
    const d = new Date(base.getTime() + i * 3600 * 1000);
    const hour = d.getHours();
    // 简单示例曲线：夜间低温高湿、白天高温低湿
    const temp = Number((15 + 8 * Math.sin(((hour - 6) / 24) * 2 * Math.PI)).toFixed(1));
    const rh = Math.round(75 - 25 * Math.sin(((hour - 6) / 24) * 2 * Math.PI));
    rows.push({
      time: `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(hour)}:00:00`,
      temp,
      rh,
      weather: kinds[hour % kinds.length],
    });
  }
  form.weatherForecast = rows;
}

function addWeatherRow() {
  form.weatherForecast.push({
    time: '',
    temp: undefined as unknown as number,
    rh: undefined as unknown as number,
    weather: '晴',
  });
}

function fmt(v?: number) {
  return v == null ? '-' : Number(v).toLocaleString('zh-CN');
}

function fmtTime(t: string) {
  return t ? t.replace('T', ' ') : '-';
}

async function handleGenerate() {
  loading.value = true;
  generating.value = true;
  typing.value = true;
  streamToken += 1; // 取消上一次未完成的流式输出
  try {
    const res: any = await generateVentilationPlan({
      ...form,
      weatherForecast: form.weatherForecast?.filter((w) => w.time && w.temp != null && w.rh != null),
    });
    if (res?.status?.code === '200' || res?.result) {
      result.value = res.result;
      if (res.result?.requireMoreInput) {
        await typeAsk(res.result.aiMessage ?? '');
      } else {
        MessagePlugin.success(res?.status?.message || '方案生成成功');
        await streamReveal();
      }
    } else {
      MessagePlugin.error(res?.status?.message || '方案生成失败');
      generating.value = false;
      typing.value = false;
    }
  } catch (e: any) {
    MessagePlugin.error(e?.message || '请求异常，请检查后端服务');
    generating.value = false;
    typing.value = false;
  } finally {
    loading.value = false;
  }
}
</script>

<style lang="less" scoped>
.ventilation-page {
  padding: 16px;

  .page-header {
    margin-bottom: 16px;

    .page-title {
      display: flex;
      align-items: center;
      gap: 10px;

      .title-icon {
        font-size: 28px;
      }

      .title-text {
        font-size: 20px;
        font-weight: 600;
        color: var(--td-text-color-primary);
      }

      .title-desc {
        font-size: 12px;
        color: var(--td-text-color-placeholder);
      }
    }
  }

  .panel {
    background: var(--td-bg-color-container);
    border-radius: 10px;
    padding: 16px;
    margin-bottom: 16px;

    .panel-head {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding: 12px;
      border-radius: 8px;
      margin-bottom: 16px;

      &.orange {
        background: var(--td-brand-color-light, #fff1e0);
      }

      &.green {
        background: var(--td-success-color-1, #e3f9e9);
      }

      .panel-title {
        font-size: 16px;
        font-weight: 600;
      }

      .panel-sub {
        font-size: 12px;
        color: var(--td-text-color-placeholder);
        margin-top: 2px;
      }
    }
  }

  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--td-text-color-primary);
    margin: 16px 0 8px;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  // 校验错误文案改为占位显示，避免绝对定位悬挂与下方表单项重叠
  .param-form {
    :deep(.t-input__extra) {
      position: static;
      padding: 2px 0;
      white-space: normal;
    }

    :deep(.t-form__item-with-extra) {
      margin-bottom: 8px;
    }
  }

  .weather-list {
    .weather-row {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      .wt-time {
        flex: 1;
      }

      .wt-num {
        width: 110px;
      }

      .wt-weather {
        width: 84px;
      }
    }
  }

  .ask-text {
    font-size: 13px;
    line-height: 22px;

    .ask-caret {
      display: inline-block;
      width: 2px;
      height: 14px;
      margin-left: 2px;
      vertical-align: -2px;
      background: var(--td-text-color-primary);
      animation: ask-caret-blink 0.8s step-end infinite;
    }
  }

  .ask-list {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 8px;

    .ask-tag {
      font-weight: 500;
    }
  }

  .scheme-name {
    font-weight: 600;
  }

  .scheme-summary {
    font-size: 12px;
    line-height: 20px;
    margin-top: 4px;
  }

  .estimate-grid {
    margin-top: 12px;
  }

  .advice-list {
    .advice-item {
      font-size: 13px;
      line-height: 22px;
      color: var(--td-text-color-primary);
    }
  }

  .check-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;

    .check-item {
      border: 1px solid var(--td-component-stroke);
      border-radius: 8px;
      padding: 10px;
      background: var(--td-bg-color-secondarycontainer);

      &.fail {
        border-color: var(--td-error-color);
        background: var(--td-error-color-1, #fff0ed);
      }

      .ci-label {
        font-size: 12px;
        color: var(--td-text-color-secondary);
      }

      .ci-value {
        font-size: 18px;
        font-weight: 600;
        margin: 4px 0;

        small {
          font-size: 12px;
          font-weight: 400;
          color: var(--td-text-color-placeholder);
        }
      }
    }
  }

  .window-list {
    .window-item {
      border: 1px solid var(--td-component-stroke);
      border-radius: 8px;
      padding: 12px;
      margin-bottom: 10px;

      .wi-head {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 4px;

        .wi-title {
          font-weight: 600;
        }
      }

      .wi-time {
        color: var(--td-brand-color);
        font-weight: 500;
      }

      .wi-meta {
        font-size: 12px;
        color: var(--td-text-color-secondary);
        margin-top: 2px;
      }

      .wi-collapse {
        margin-top: 8px;
        border: none;

        :deep(.t-collapse-panel__header) {
          font-size: 12px;
          color: var(--td-text-color-secondary);
        }
      }
    }
  }

  .check-row-list {
    .check-row {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      padding: 4px 0;
    }
  }

  .check-row {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    padding: 4px 0;
  }

  .step-list {
    .step-item {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      font-size: 13px;
      padding: 6px 0;
      border-bottom: 1px dashed var(--td-component-stroke);

      &:last-child {
        border-bottom: none;
      }
    }
  }

  .safety-box {
    background: var(--td-warning-color-1, #fdf3e1);
    border-radius: 8px;
    padding: 12px;
    margin-top: 16px;

    .section-title {
      margin-top: 0;
    }

    .safety-list {
      margin: 0;
      padding-left: 18px;
      font-size: 13px;

      li {
        line-height: 22px;
      }
    }
  }

  .stream-status {
    font-size: 12px;
    font-weight: 400;
    color: var(--td-success-color);
    animation: stream-blink 1.2s ease-in-out infinite;
  }

  .stream-box {
    max-height: calc(100vh - 230px);
    overflow-y: auto;
    padding: 4px 8px 4px 4px;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      border-radius: 3px;
      background: var(--td-scrollbar-color, #c9c9c9);
    }
  }

  .chat-msg {
    display: flex;
    align-items: flex-start;
    gap: 10px;

    .chat-avatar {
      flex-shrink: 0;
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: var(--td-success-color-1, #e3f9e9);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
    }

    .chat-bubble {
      flex: 1;
      min-width: 0;
      background: var(--td-bg-color-secondarycontainer, #f6f7f8);
      border: 1px solid var(--td-component-stroke);
      border-radius: 4px 12px 12px 12px;
      padding: 12px 14px;
    }
  }

  .typing-dots {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 0;

    i {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: var(--td-text-color-placeholder);
      animation: typing-bounce 1.2s infinite ease-in-out;

      &:nth-child(2) {
        animation-delay: 0.2s;
      }

      &:nth-child(3) {
        animation-delay: 0.4s;
      }
    }
  }

  @keyframes typing-bounce {
    0%,
    60%,
    100% {
      transform: translateY(0);
      opacity: 0.4;
    }

    30% {
      transform: translateY(-4px);
      opacity: 1;
    }
  }

  @keyframes stream-blink {
    0%,
    100% {
      opacity: 1;
    }

    50% {
      opacity: 0.35;
    }
  }
}

@keyframes ask-caret-blink {
  50% {
    opacity: 0;
  }
}
</style>
