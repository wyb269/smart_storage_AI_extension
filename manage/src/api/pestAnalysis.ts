import { request } from '@/utils/request';

// 虫害分析接口（后端纯算法计算，无 AI 模型）

export interface PestWarehouseInfo {
  warehouseId?: string; // 仓库ID（必填）
  warehouseName?: string; // 仓库名称/仓号
  warehouseType?: string; // FLAT / SHALLOW / SILO（必填）
  capacity?: number; // 仓容（吨）
  storedWeight?: number; // 现存粮食数量（t）
  spaceVolume?: number; // 仓房空间体积（m³）
  grainVolume?: number; // 粮堆体积（m³）
  sealingMethod?: string; // WHOLE 整仓 / UNDER_FILM 膜下 / CIRCULATION 环流
  sealingLevel?: string; // 仓房密封性等级：LEVEL_1 一级 / LEVEL_2 二级 / LEVEL_3 三级
  airtightDecay?: number; // 气密性：500Pa→250Pa 压力半衰期（s）
}

export interface PestGrainInfo {
  grainType?: string; // （必填）
  grainMoisture?: number; // 粮食水分（%）
  impurity?: number; // 杂质含量（%）
  grainAvgTemp?: number; // 平均粮温（℃）（必填）
  pileHeight?: number; // 粮堆高度（m）
  storedDays?: number; // 已储存天数
  storedYears?: number; // 储存年限（年）；未传 storedDays 时按 365×年限 折算
}

export interface PestSpeciesItem {
  code: string; // 虫种编码（必填）
  density?: number; // 该虫种密度（头/kg）
  isMain?: boolean; // 是否主要虫种
  distribution?: string; // TOP 上层 / MIDDLE 中层 / BOTTOM 下层 / WHOLE 全仓
}

export interface PestPestInfo {
  species: PestSpeciesItem[]; // 虫种列表（必填）
  totalDensity?: number; // 害虫总密度（头/kg）（必填）
  samplePoints?: number; // 采样点数
  maxPointDensity?: number; // 最大点密度（头/kg）
  detectionMethod?: string; // SIEVE 筛检 / TRAP 诱捕器 / VISUAL 目测 / TRAP_COUNT 诱捕计数
  resistanceLevel?: string; // HIGH 耐药 / MEDIUM 一般 / LOW 敏感
}

export interface PestEnvData {
  airTemp?: number; // 室外温度（℃）
  airRh?: number; // 室外湿度（%）
  season?: string; // SPRING / SUMMER / AUTUMN / WINTER
  warehouseTemp?: number; // 仓温（℃）
  warehouseRh?: number; // 仓内湿度（%）
}

export interface PestHistoryInfo {
  lastFumigationDate?: string; // 上次熏蒸日期
  lastFumigationEffect?: string; // 上次熏蒸效果
  lastPestDate?: string; // 上次虫害日期
  recurrenceCount?: number; // 近一年发生次数
  maxHistoricalDensity?: number; // 历史最高密度（头/kg）
  historicalMainSpecies?: string; // 历史主要虫种（虫种编码）
}

export interface PestPreventionInfo {
  protectantUsed?: boolean; // 是否使用防护剂
  cleaningLevel?: string; // CLEAN 良好 / NORMAL 一般 / POOR 较差
  pestFacility?: string; // COMPLETE 完善 / PARTIAL 部分 / NONE 无
}

export interface PestControlInfo {
  lastControlMethod?: string; // FUMIGATION 熏蒸 / VENTILATION 通风 / PHYSICAL 物理防治 / CHEMICAL 防护剂
  densityBefore?: number; // 防治前密度（头/kg）
  densityAfter?: number; // 防治后密度（头/kg）
  effectRating?: string; // EFFECTIVE 有效 / PARTIAL 部分有效 / INEFFECTIVE 无效
}

export interface PestAnalysisReq {
  warehouseInfo: PestWarehouseInfo;
  grainInfo: PestGrainInfo;
  pestInfo: PestPestInfo;
  envData: PestEnvData;
  historyInfo?: PestHistoryInfo;
  preventionInfo?: PestPreventionInfo;
  controlInfo?: PestControlInfo;
}

export interface PestRiskItem {
  type?: string; // PEST_DENSITY / ENVIRONMENT / SEALING / RESISTANCE / HISTORY / CLEANLINESS / FACILITY / IMPURITY
  name?: string;
  level?: string; // NORMAL / MILD / MODERATE / SEVERE
  levelText?: string; // 正常/一般/较重/严重
  description?: string;
  suggestion?: string;
  remark?: string;
}

export interface PestFumigationBrief {
  totalDose?: number;
  applicationPoints?: number;
  sealDays?: number;
  aerateDays?: number;
  targetConcentration?: number;
  dosePerVolume?: number; // 粮堆单位用药量（g/m³）
  agentName?: string;
  schemeName?: string;
}

export interface PestPlan {
  code?: string;
  name?: string;
  recommended?: boolean;
  feasible?: boolean;
  feasibilityNote?: string;
  summary?: string;
  starRating?: number; // 推荐指数 1~5
  effectDescription?: string;
  fumigation?: PestFumigationBrief;
  targetTemp?: number;
  totalDays?: number;
  effectRate?: number;
  costEstimate?: number;
  advantages?: string[];
  disadvantages?: string[];
}

export interface PestMonitorPlan {
  recheckDays?: number;
  items?: string[];
}

export interface PestAnalysisResult {
  riskScore?: number; // 风险分 0~100
  /** AI 追问缺失参数模式（true 时其余分析字段为空） */
  requireMoreInput?: boolean;
  missingParams?: string[];
  aiMessage?: string;
  riskLevel?: string; // LOW / MEDIUM / HIGH
  riskLevelText?: string;
  summary?: string;
  riskItems?: PestRiskItem[];
  grainPestGrade?: string; // NONE 基本无虫 / GENERAL 一般 / SEVERE 严重 / DANGEROUS 危险
  treatDeadline?: string; // 处理时限提示
  resistanceGroup?: string; // A / B / C
  plans?: PestPlan[];
  monitorPlan?: PestMonitorPlan;
}

export function analyzePest(data: PestAnalysisReq) {
  return request.post({
    url: '/ai/pestAnalysis/analyze',
    method: 'post',
    data,
  });
}
