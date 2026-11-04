import { request } from '@/utils/request';

// 熏蒸作业智能辅助接口（后端纯算法计算，无 AI 模型）

export interface FumWarehouseInfo {
  warehouseId?: string; // 仓库ID（必填）
  warehouseName?: string; // 仓库名称/仓号，用于方案说明回显
  warehouseType?: string; // FLAT 平房仓 / SHALLOW 浅圆仓 / SILO 立筒仓（必填）
  capacity?: number; // 仓库容量（t）
  grainWeight?: number; // 粮食数量（t）
  spaceVolume?: number; // 仓房空间体积（m³，粮面以上）；不传按粮堆体积15%估算
  grainVolume?: number; // 粮堆体积（m³）；不传由仓长×仓宽×粮堆高度推算
  pileHeight?: number; // 粮堆高度（m）
  warehouseLength?: number; // 仓长（m）
  warehouseWidth?: number; // 仓宽（m）
  airtightLevel?: string; // LEVEL_1 一级(300s) / LEVEL_2 二级(200s) / LEVEL_3 三级(100s) 参考半衰期
  airtightDecay?: number; // 气密性：500Pa→250Pa 压力半衰期（s），实测值优先于等级
  sealingMethod?: string; // WHOLE 整仓 / UNDER_FILM 膜下 / CIRCULATION 环流
  hasCirculation?: boolean; // 是否具备环流装置
}

export interface FumGrainInfo {
  grainType?: string; // WHEAT/CORN/PADDY/RICE/SOYBEAN 等（必填）
  grainMoisture?: number; // 粮食水分（%）
  impurity?: number; // 杂质含量（%）
  grainTemp?: number; // 害虫发生部位最低粮温（℃）；不传取 grainAvgTemp（自查参考值，平房仓校验阈值90s）
  grainAvgTemp?: number; // 平均粮温（℃）
  storedDays?: number; // 已储存天数
  lastFumigationDate?: string; // 上次熏蒸日期 yyyy-MM-dd
  seedGrain?: boolean; // 是否种子粮
}

export interface FumPestInfo {
  pestSpecies: string[]; // CORN_WEEVIL 玉米象 / RICE_WEEVIL 米象 / GRAIN_BORER 谷蠹 / RED_FLOUR_BEETLE 赤拟谷盗 / FLAT_GRAIN_BEETLE 扁谷盗 / BOOK_LOUSE 书虱 / MOTH 蛾类（必填）
  pestDensity?: number; // 害虫密度（头/kg）
  hazardLevel?: string; // GENERAL 一般 / SEVERE 严重 / EXTREME 极重
  resistanceLevel?: string; // HIGH 耐药 / MEDIUM 一般 / LOW 敏感
}

export interface FumEnvInfo {
  weather?: string; // 晴 / 阴 / 雨
  airTemp?: number; // 气温（℃）
  airRh?: number; // 相对湿度（%）
}

export interface FumOperationInfo {
  operationType?: string; // ROUTINE 常规熏蒸 / EXTERNAL 外包 / EMERGENCY 应急
  planDate?: string; // 计划作业日期 yyyy-MM-dd
  operator?: string; // 作业负责人
  workerCount?: number; // 作业人数
  operationLevel?: string; // GENERAL 一般 / MAJOR 重大 / CRITICAL 特殊
}

export interface FumGasParam {
  gasType?: string; // PH3 磷化氢 / SF 硫酰氟 / CO2 二氧化碳
  targetConcentration?: number; // 目标浓度（mL/m³，ppm）
  minEffectiveConcentration?: number; // 最低有效浓度（ppm）
  concentrationUniformity?: number; // 浓度均匀要求（%），≥90 时自动加密取样点
  ctRequirement?: number; // CT值要求（ppm·h）
  sealDays?: number; // 密闭天数（天）
  circulationLevel?: string; // CIRCULATION 有环流 / NATURAL 无环流
}

export interface FumChemicalInfo {
  agentType?: string; // ALUMINIUM_PHOSPHIDE 磷化铝 / MAGNESIUM_PHOSPHIDE 磷化镁
  applicationMethods?: string[]; // SURFACE 粮面施药 / PROBE 探管施药 / CIRCULATION 环流熏蒸 / BAG 气囊施药
  auxiliaryMeasures?: string[]; // CLEANING 清洁卫生 / VENTILATION_PRETREAT 通风降温预处理 / PEST_LINE 防虫线设置 / TURNOVER 翻仓倒仓
}

export interface FumigationPlanReq {
  warehouseInfo: FumWarehouseInfo;
  grainInfo: FumGrainInfo;
  pestInfo: FumPestInfo;
  envInfo?: FumEnvInfo;
  operationInfo?: FumOperationInfo;
  gasParam?: FumGasParam;
  chemicalInfo?: FumChemicalInfo;
}

export interface FumPointDetail {
  location: string; // 位置名称：粮面 / 探管 / 环流管道
  points: number; // 点位数
  dosePerPoint: number; // 每点药量（g）
}

export interface FumDoseInfo {
  dosePerVolumePile?: number; // 粮堆单位用药量（g/m³）
  dosePerVolumeSpace?: number; // 空间单位用药量（g/m³）
  dosePile?: number; // 粮堆用药量（kg）
  doseSpace?: number; // 空间用药量（kg）
  totalDose?: number; // 总药量（kg）
  tabletCount?: number; // 片数
  applicationPoints?: number; // 投药点数
  maxPerPoint?: number; // 每点最大药量（g）
  pointDetails?: FumPointDetail[];
}

export interface FumTimelineItem {
  phase: string;
  name: string;
  desc: string;
}

export interface FumScheduleInfo {
  totalDays?: number; // 总作业周期（天）
  sealDays?: number; // 密闭熏蒸天数（≥14）
  aerateDays?: number; // 散气排毒天数
  aerationTargetConcentration?: number; // 散气目标浓度（mL/m³，低于该值方可进仓）
  timeline?: FumTimelineItem[];
}

export interface FumConcCheck {
  time: string;
  require: string;
}

export interface FumOperationStep {
  stepNo: number;
  title: string;
  content: string;
}

export interface FumigationPlanResult {
  schemeName?: string;
  /** AI 追问缺失参数模式（true 时其余方案字段为空） */
  requireMoreInput?: boolean;
  missingParams?: string[];
  aiMessage?: string;
  summary?: string;
  warehouseName?: string;
  feasible?: boolean;
  rejectedReasons?: string[];
  manualOverrides?: string[]; // 人工干预项说明
  resistanceGroup?: string; // A 敏感 / B 抗性 / C 强抗性
  agentType?: string;
  agentName?: string;
  agentSpec?: string;
  tabletWeight?: number;
  targetConcentration?: number;
  minEffectiveConcentration?: number;
  ctValue?: number;
  ctRequired?: number;
  dose?: FumDoseInfo;
  schedule?: FumScheduleInfo;
  concentrationCheck?: FumConcCheck[];
  samplingPoints?: number;
  operationSteps?: FumOperationStep[];
  safetyTips?: string[];
}

export function generateFumigationPlan(data: FumigationPlanReq) {
  return request.post({
    url: '/ai/fumigation/generatePlan',
    method: 'post',
    data,
  });
}
