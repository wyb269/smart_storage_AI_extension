import { request } from '@/utils/request';

// 粮情智能分析接口（后端纯算法计算，无 AI 模型）

export interface GrainWarehouseInfo {
  warehouseId?: string; // 仓库ID（必填）
  warehouseName?: string; // 仓库名称/仓号，用于结论回显
  warehouseType?: string; // FLAT / SHALLOW / SILO
  capacity?: number; // 仓容（吨）
  storedWeight?: number; // 现存粮食数量（t）
  storedDate?: string; // 入仓日期 yyyy-MM-dd
}

export interface GrainGrainInfo {
  grainType?: string; // （必填）
  grainMoisture?: number; // 粮食水分（%）（必填）
  safeMoisture?: number; // 当地储粮安全水分（%）
  grainAvgTemp?: number; // 平均粮温（℃）；未传时按上/中/下层均值推算
  grainMaxTemp?: number; // 最高粮温（℃）
  grainMinTemp?: number; // 最低粮温（℃）
  topLayerTemp?: number; // 上层平均粮温（℃）
  middleLayerTemp?: number; // 中层平均粮温（℃）
  bottomLayerTemp?: number; // 下层平均粮温（℃）
  impurity?: number; // 杂质含量（%）
  pileHeight?: number; // 粮堆高度（m）
}

export interface GrainMonitorData {
  hotSpotCount?: number; // 异常高温点数量（高于周边3℃以上）
  hotSpotMaxTemp?: number; // 异常点最高温度
  tempRiseRate?: number; // 近7天异常点升温速率（℃/天）
  topBottomTempDiff?: number; // 上/下层温差
  topBottomMoistureDiff?: number; // 上/下层水分差
  pestDensity?: number; // 害虫总密度（头/kg）
  mainPestDensity?: number; // 主要害虫密度（头/kg）
  pestSpecies?: string[]; // 虫种编码列表
}

export interface GrainEnvData {
  warehouseTemp?: number; // 仓温（℃）（必填）
  warehouseRh?: number; // 仓湿（%）（必填）
  airTemp?: number; // 室外温度（℃）
  airRh?: number; // 室外湿度（%）
  season?: string; // SPRING / SUMMER / AUTUMN / WINTER
}

export interface GrainHistoryInfo {
  lastFumigationDate?: string; // 上次熏蒸日期
  lastPestDate?: string; // 上次虫害日期
  pastIssues?: string[]; // 历史问题：PEST / MOLD / CONDENSATION
}

export interface GrainAnalysisReq {
  warehouseInfo: GrainWarehouseInfo;
  grainInfo: GrainGrainInfo;
  monitorData?: GrainMonitorData;
  envData: GrainEnvData;
  historyInfo?: GrainHistoryInfo;
  forecastDays?: number; // 预测周期（天）：7 / 15 / 30
}

export interface GrainWarning {
  type?: string; // TEMP_ABNORMAL / PEST / MOISTURE / ENVIRONMENT
  name?: string;
  level?: string; // NORMAL / MILD / MODERATE / SEVERE
  levelText?: string; // 正常/一般/较重/严重
  description?: string;
  impact?: string;
  suggestions?: string[];
  remark?: string;
}

export interface GrainTreatPlan {
  priority?: number; // 优先级 1/2/3
  primary?: string; // VENTILATION 通风 / FUMIGATION 熏蒸 / MIXED 综合 / MONITOR 监测
  title?: string;
  reason?: string;
  detail?: string;
  linkTo?: string;
  linkLabel?: string;
}

export interface GrainAnalysisResult {
  riskScore?: number; // 综合风险分 0~100，越高越危险
  /** AI 追问缺失参数模式（true 时其余分析字段为空） */
  requireMoreInput?: boolean;
  missingParams?: string[];
  aiMessage?: string;
  riskLevel?: string; // LOW / MEDIUM / HIGH
  riskLevelText?: string;
  summary?: string;
  forecastDays?: number;
  adviceLevel?: string; // monitor 加强监测 / plan 计划处置 / immediate 立即处置
  warnings?: GrainWarning[];
  treatPlans?: GrainTreatPlan[];
}

export function analyzeGrain(data: GrainAnalysisReq) {
  return request.post({
    url: '/ai/grainAnalysis/analyze',
    method: 'post',
    data,
  });
}
