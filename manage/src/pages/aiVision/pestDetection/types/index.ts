/**
 * 粮面虫害智能识别功能 - 类型定义
 *
 * 字段名严格对齐后端 DTO。页面组件只依赖本文件类型，
 * 不直接依赖后端原始字段（若后端命名不同，由 api 层转换）。
 *
 * 设备连接状态、告警状态、页面综合展示状态取自 aiVision 共享层，
 * 因为这三者在所有「监测对象 + 状态卡片 + 历史记录」形态的功能里语义相同。
 */

import type { AlarmStatus, DeviceStatus } from '../../shared/types';

export type { AlarmStatus, DeviceStatus, DisplayStatus } from '../../shared/types';

/** 采样检测结论类型 */
export type ConclusionType = 'GRAIN_INSECT' | 'DENSITY_HIGH' | 'MOLD' | 'OTHER';

/** 算法任务状态 */
export type AlgorithmStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'TIMEOUT' | 'SKIPPED' | 'MOCK';

/** 算法结论 */
export type AlgorithmResult = 'NORMAL' | 'ANOMALY' | 'UNCERTAIN';

/**
 * 人工处理状态（三值，按原型建）。
 *
 * 与遮挡那版的两态不同：本功能保留 未核实 / 处理中 / 已处理。
 */
export type ProcessStatus = 'PENDING' | 'PROCESSING' | 'DONE';

/** 视频协议 */
export type StreamProtocol = 'HLS' | 'FLV' | 'WEBRTC' | 'MP4';

/** 证据类型 */
export type EvidenceType = 'PEST_FRAME' | 'NORMAL_REFERENCE' | 'RECOVERY_IMAGE' | 'EVENT_VIDEO';

/**
 * 粮面点位状态卡片对象。
 *
 * 监测对象不是摄像头，而是「仓房 + 粮种 + 吨位」的粮面点位；
 * 卡片标题形如「三号仓 · 小麦5000吨」，由 warehouseName/grainType/grainTonnage 拼出。
 */
export interface MonitorPoint {
  pointId: string;
  pointName: string;
  warehouseId: string;
  warehouseName: string;
  grainType: string;
  grainTonnage: number | string;
  cameraCode: string;
  platformCameraId?: string | null;
  deviceStatus: DeviceStatus;
  /** 告警状态，复用 AlarmStatus 语义（ACTIVE = 页面「告警」） */
  alarmStatus: AlarmStatus;
  lastHeartbeatAt?: string | null;
  latestEventAt?: string | null;
  sortNo: number;
}

/** 播放信息；禁止承载摄像头凭据或永久地址 */
export interface StreamInfo {
  pointId: string;
  cameraCode?: string | null;
  platformCameraId?: string | null;
  playableUrl?: string | null;
  protocol: StreamProtocol;
  expiresAt?: string | null;
  canPlay: boolean;
  unavailableReason?: string | null;
}

/** 虫害检测记录列表项 */
export interface PestEvent {
  eventId: string;
  eventNo: string;
  occurredAt: string;
  warehouseId: string;
  warehouseName: string;
  pointId: string;
  cameraCode: string;
  samplePosition: string;
  grainType?: string | null;
  conclusionType: ConclusionType;
  conclusionTypeName?: string | null;
  insectSpecies?: string | null;
  /** 密度（头/kg） */
  density?: number | string | null;
  /** 判定阈值（头/kg） */
  densityThreshold?: number | string | null;
  algorithmStatus: AlgorithmStatus;
  algorithmResult?: AlgorithmResult | null;
  /** 置信度 0–1，null 表示「暂无」，不得渲染为 0% */
  confidence?: number | null;
  processStatus: ProcessStatus;
  processedBy?: string | null;
  processedAt?: string | null;
  processOpinion?: string | null;
  /** 乐观锁版本号，提交处理时必须原样回传 */
  eventVersion: number;
}

/** 证据 */
export interface PestEvidence {
  evidenceId: string;
  evidenceType: EvidenceType;
  originalUrl?: string | null;
  /** 标注图与原图分离，不得互相覆盖 */
  annotatedUrl?: string | null;
  thumbnailUrl?: string | null;
  capturedAt: string;
  cameraCode?: string | null;
  contentHash?: string | null;
  sortNo: number;
}

/** 处理审计记录 */
export interface ProcessRecord {
  recordId: string;
  statusBefore: ProcessStatus;
  statusAfter: ProcessStatus;
  processResult: string;
  processOpinion: string;
  processedBy: string;
  processedAt: string;
  source?: string | null;
}

/**
 * 算法检测框（detections JSON 元素）。
 *
 * 虫害侧特有字段：species（虫种）与 count（该框内虫数，1kg 采样即密度头数）。
 * 历史种子数据与 Mock 都带；真实网关结果可能没有，展示层必须容错。
 */
export interface AlgorithmDetection {
  category?: string | null;
  species?: string | null;
  count?: number | null;
  confidence?: number | string | null;
  bbox?: string | null;
  trackId?: string | null;
}

/** 算法任务行（详情弹窗「算法任务」表 + 轮询接口共用） */
export interface PestAlgorithmTask {
  /** 对外任务标识（= taskNo，轮询参数） */
  taskId: string;
  /** 任务行主键（字符串） */
  id?: string | null;
  taskNo: string;
  eventId?: string | null;
  pointId?: string | null;
  cameraCode?: string | null;
  taskType?: string | null;
  executionStatus: AlgorithmStatus;
  conclusion?: AlgorithmResult | null;
  conclusionType?: ConclusionType | null;
  /** 置信度 0–1；null 表示「暂无」，不得渲染为 0% */
  confidence?: number | string | null;
  confidenceThreshold?: number | string | null;
  modelName?: string | null;
  modelVersion?: string | null;
  algorithmVersion?: string | null;
  configVersion?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  inferenceDurationMs?: number | null;
  /** 检出目标框数量（后端由 detections 派生） */
  detectedCount?: number | null;
  /** 检测结果原始 JSON 字符串，仅展示解析，解析失败必须容错 */
  detections?: string | null;
  mediaRefs?: string | null;
  /** 模拟结果标识：true 时页面必须显示「模拟」标签，不可省略 */
  simulated?: boolean | null;
  errorCode?: string | null;
  errorMessage?: string | null;
  traceId?: string | null;
}

/** 「重新识别」提交受理结果 */
export interface AlgorithmRerunResult {
  taskId: string;
  executionStatus: AlgorithmStatus;
  simulated: boolean;
}

/** 检测记录详情 */
export interface PestEventDetail extends PestEvent {
  eventDesc?: string | null;
  recoveredAt?: string | null;
  durationSeconds?: number | null;
  /** 是否为模拟数据 */
  isSimulated?: boolean;
  /** 密度是否超过判定阈值（后端派生） */
  densityExceeded?: boolean | null;
  evidences: PestEvidence[];
  processRecords: ProcessRecord[];
  /** 历史算法任务（按 started_at 倒序，最多 20 条），随详情一次带回 */
  algorithmTasks: PestAlgorithmTask[];
  /** 最新一条算法任务；无任务时为空 */
  latestAlgorithmTask?: PestAlgorithmTask | null;
}

/** 查询参数 */
export interface EventQueryParams {
  pageNo?: number;
  pageSize?: number;
  eventNo?: string;
  warehouseId?: string;
  pointId?: string;
  cameraCode?: string;
  samplePosition?: string;
  conclusionType?: ConclusionType | '';
  processStatus?: ProcessStatus | '';
  startTime?: string;
  endTime?: string;
}

/** 播放器可见状态 */
export type PlayerVisibleState =
  | 'NO_POINT_SELECTED'
  | 'LOADING'
  | 'PLAYING'
  | 'ERROR'
  | 'EXPIRED'
  | 'OFFLINE'
  | 'NO_ADDRESS'
  | 'UNSUPPORTED_PROTOCOL';

export type ExportSource = 'RECENT' | 'HISTORY';
