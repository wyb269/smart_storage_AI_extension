/**
 * 地面积水监测功能 - 类型定义
 *
 * 字段名严格对齐后端 DTO。页面组件只依赖本文件类型，
 * 不直接依赖后端原始字段（若后端命名不同，由 api 层转换）。
 *
 * 设备连接状态、告警状态、页面综合展示状态取自 aiVision 共享层，
 * 因为这三者在所有「监测对象 + 状态卡片 + 历史记录」形态的功能里语义相同。
 */

import type { AlarmStatus, DeviceStatus } from '../../shared/types';

export type { AlarmStatus, DeviceStatus, DisplayStatus } from '../../shared/types';

/** 积水结论类型 */
export type WaterType = 'PONDING' | 'LEAK' | 'ICE' | 'OTHER';

/** 算法任务状态 */
export type AlgorithmStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'TIMEOUT' | 'SKIPPED' | 'MOCK';

/** 算法结论 */
export type AlgorithmResult = 'NORMAL' | 'ANOMALY' | 'UNCERTAIN';

/**
 * 人工处理状态。
 *
 * 与遮挡那版（PENDING/PROCESSING/VERIFIED_TRUE/VERIFIED_FALSE/CLOSED 五值折叠成两态）不同，
 * 本功能按原型建三值，页面三态一一对应展示。
 */
export type ProcessStatus = 'PENDING' | 'PROCESSING' | 'DONE';

/** 视频协议 */
export type StreamProtocol = 'HLS' | 'FLV' | 'WEBRTC' | 'MP4';

/** 证据类型 */
export type EvidenceType = 'WATER_FRAME' | 'NORMAL_REFERENCE' | 'RECOVERY_IMAGE' | 'EVENT_VIDEO';

/** 监测点位状态卡片对象 */
export interface MonitorPointStatus {
  pointId: string;
  /** 卡片标题，如「主通廊东侧」 */
  pointName: string;
  cameraCode: string;
  platformCameraId?: string | null;
  warehouseId: string;
  warehouseName: string;
  deviceStatus: DeviceStatus;
  /** 积水告警状态，复用 AlarmStatus 语义（ACTIVE = 页面「积水」） */
  alarmStatus: AlarmStatus;
  lastHeartbeatAt?: string | null;
  latestEventAt?: string | null;
  sortNo: number;
}

/** 播放信息；禁止承载摄像头凭据或永久地址 */
export interface StreamInfo {
  pointId: string;
  pointName?: string | null;
  cameraCode?: string | null;
  platformCameraId?: string | null;
  playableUrl?: string | null;
  protocol: StreamProtocol;
  expiresAt?: string | null;
  canPlay: boolean;
  unavailableReason?: string | null;
}

/** 积水事件列表项 */
export interface WaterEvent {
  eventId: string;
  eventNo: string;
  occurredAt: string;
  warehouseId: string;
  warehouseName: string;
  /** 监测位置（点位名称快照），对应原型「监测位置」列 */
  pointId: string;
  pointName: string;
  cameraCode: string;
  waterType: WaterType;
  waterTypeName?: string | null;
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
export interface WaterEvidence {
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

/** 算法任务行（详情弹窗「算法任务」表 + 轮询接口共用） */
export interface AlgorithmTask {
  /** 对外任务标识（= taskNo，轮询参数） */
  taskId: string;
  taskNo: string;
  eventId?: string | null;
  executionStatus: AlgorithmStatus;
  conclusion?: AlgorithmResult | null;
  /** 地面积水结论类型（后端 WaterAlgorithmTaskRespDTO.waterType），未出结论为 null */
  waterType?: WaterType | null;
  /** 置信度 0–1；null 表示「暂无」，不得渲染为 0% */
  confidence?: number | string | null;
  modelName?: string | null;
  modelVersion?: string | null;
  algorithmVersion?: string | null;
  configVersion?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  inferenceDurationMs?: number | string | null;
  /** 检出目标框数量 */
  detectedCount?: number | null;
  /** 模拟结果标识：true 时页面必须显示「模拟」标签，不可省略 */
  simulated?: boolean | null;
  errorCode?: string | null;
  errorMessage?: string | null;
}

/** 「重新识别」提交受理结果 */
export interface RerunAccepted {
  taskId: string;
  executionStatus: AlgorithmStatus;
  simulated: boolean;
}

/** 事件详情 */
export interface WaterEventDetail extends WaterEvent {
  eventDesc?: string | null;
  recoveredAt?: string | null;
  durationSeconds?: number | null;
  /** 是否为模拟数据 */
  isSimulated?: boolean;
  evidences: WaterEvidence[];
  processRecords: ProcessRecord[];
  /** 历史算法任务（按 started_at 倒序，最多 20 条），随详情一次带回 */
  algorithmTasks: AlgorithmTask[];
  /** 最新一条算法任务；无任务时为空 */
  latestAlgorithmTask?: AlgorithmTask | null;
}

/** 查询参数 */
export interface EventQueryParams {
  pageNo?: number;
  pageSize?: number;
  eventNo?: string;
  warehouseId?: string;
  pointId?: string;
  cameraCode?: string;
  waterType?: WaterType | '';
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
