import { request } from '@/utils/request';

// 通风作业智慧辅助接口（LS/T 1202-2002 算法，后端纯计算，无 AI 模型）

export interface CheckRow {
  name: string;
  condition: string;
  actual: string;
  passed: boolean;
}

export interface CheckItem {
  value: number;
  limit: number;
  qualified: boolean;
  basis: string;
}

export interface ResistanceCheck {
  grainLayerResistance: number;
  limit: number;
  qualified: boolean;
  detail: CheckRow;
}

export interface DuctSpacingCheck {
  recommended: number;
  qualified: boolean;
}

export interface DuctVelocityCheck {
  main: CheckItem;
  branch: CheckItem;
}

export interface VentilationWindow {
  startTime: string;
  endTime: string;
  hours: number;
  avgTemp: number;
  avgRh: number;
  tempDiff: number;
  judgement: string;
  checkItems: CheckRow[];
  peakHours: number; // 峰段小时数（峰谷电价约束用）
  tempDrop: number; // 本时段预计粮温降幅（℃）
  energyKwh: number; // 本时段预计耗电（kWh）
}

export interface OperationStep {
  order: number;
  text: string;
}

export interface GrainInfo {
  warehouseType?: string; // FLAT 房式仓 / SHALLOW 浅圆仓 / SILO 立筒仓
  grainType?: string; // WHEAT/CORN/PADDY/RICE/SOYBEAN/BARLEY/PEANUT
  grainWeight?: number; // 粮食质量 G（t）
  grainVolume?: number; // 粮堆体积 V（m³）
  bulkDensity?: number; // 容重 r（t/m³）
  grainMoisture?: number; // 粮食水分 w（%）
  safeMoisture?: number; // 当地储粮安全水分（%）
  pileHeight?: number; // 粮堆高度 h（m）
  grainAvgTemp?: number; // 粮堆平均温度 t2（℃）
  topBottomTempDiff?: number; // 上/下层温差（℃）
  tempGradient?: number; // 温度梯度（℃/m）
  moistureGradient?: number; // 水分梯度（%/m）
  topBottomMoistureDiff?: number; // 上/下层水分差（%）
}

export interface DeviceInfo {
  fanType?: string; // CENTRIFUGAL 离心 / AXIAL 轴流 / MIXED 混流 / EXHAUST 排风扇
  airSupplyMode?: string; // PRESSURE 压入 / SUCTION 吸出 / MIXED 混合
  fanAirflowPerUnit?: number; // 单台风机风量（m³/h），与风机数量合成设备总风量
  fanCount?: number; // 风机数量（台）
  fanPower?: number; // 单台风机功率（kW）
  mainDuctArea?: number; // 主风道横截面积（m²）
  branchDuctArea?: number; // 支风道横截面积（m²）
  ductSpacing?: number; // 现有风道间距 L（m）
  compactionFactor?: number; // 压紧系数 φ
}

export interface WeatherPoint {
  time: string; // yyyy-MM-dd'T'HH:mm:ss
  temp: number; // ℃
  rh: number; // %
  weather?: string; // 晴 / 阴 / 雨
}

export interface ConstraintInfo {
  allowTimeRanges?: string[]; // 允许通风时间窗 HH:mm-HH:mm，支持跨零点
  naturalOnly?: boolean; // 只利用自然通风（零能耗）
  avoidRainHumidity?: boolean; // 避开雨天及高湿天气（RH>85%）
  peakValleyPricing?: boolean; // 考虑峰谷电价（谷段优先）
  maxContinuousHours?: number; // 最长连续通风时长（h），超出自动切分
}

export interface HistoryInfo {
  lastYearGrainTemp?: number; // 去年同期粮温（℃）
  lastYearMoisture?: number; // 去年同期水分（%）
  ventilationTimesLastYear?: number; // 近一年通风次数
  lastVentilationType?: string; // 上次通风类型：降温/降水/调质/环流
  lastVentilationHours?: number; // 上次通风时长（h）
  lastTempBefore?: number; // 通风前粮温（℃）
  lastTempAfter?: number; // 通风后粮温（℃）
  lastEffectRating?: string; // GOOD 良好 / MEDIUM 一般 / BAD 较差（兼容中文）
  lastEnergyKwh?: number; // 上次通风能耗（kWh）
}

export interface VentilationPlanReq {
  ventilationGoals: string[]; // COOLING/MOISTURE_REDUCTION/CONDITIONING/BALANCE
  grainInfo: GrainInfo;
  deviceInfo: DeviceInfo;
  weatherForecast?: WeatherPoint[];
  subtropical?: boolean;
  targetGrainTemp?: number; // 目标粮温（℃），降温/平衡通风达标目标
  targetMoisture?: number; // 目标水分（%），降水/调质通风达标目标
  warehouseTemp?: number; // 仓内温度（℃）
  warehouseRh?: number; // 仓内湿度（%）
  constraintInfo?: ConstraintInfo;
  historyInfo?: HistoryInfo;
}

export interface VentilationPlanResult {
  schemeName: string;
  /** AI 追问缺失参数模式（true 时其余方案字段为空） */
  requireMoreInput?: boolean;
  missingParams?: string[];
  aiMessage?: string;
  feasible: boolean;
  rejectedReasons: string[];
  summary: string; // 方案结论（预计效果描述）
  unitAirflow: CheckItem;
  totalAirflow: number;
  fanAirflow: number;
  fanPressure: number;
  totalResistance: ResistanceCheck;
  airPathRatio: CheckItem;
  ductSpacing: DuctSpacingCheck;
  ductVelocity: DuctVelocityCheck;
  ventilationWindows: VentilationWindow[];
  stopConditions: CheckRow[];
  operationSteps: OperationStep[];
  safetyTips: string[];
  judgeDetail: CheckRow[];
  targetGrainTemp?: number;
  targetMoisture?: number;
  estimatedTargetTemp?: number; // 预计可达平均粮温（℃）
  targetReached?: boolean; // 推荐时段内是否可达到目标粮温
  coolingRatePerHour?: number; // 估算降温速率（℃/h）
  moistureDropRatePerHour?: number; // 估算降水速率（%/h）
  estimatedHours?: number; // 预计累计通风时长（h）
  installedPower?: number; // 装机功率（kW）
  estimatedEnergy?: number; // 预计耗电量（kWh）
  unitEnergy?: number; // 单位能耗
  constraintChecks?: CheckRow[]; // 约束条件校验结果
  advices?: string[]; // 历史通风效果与专项建议
}

export function generateVentilationPlan(data: VentilationPlanReq) {
  return request.post({
    url: '/ai/ventilation/generatePlan',
    method: 'post',
    data,
  });
}
