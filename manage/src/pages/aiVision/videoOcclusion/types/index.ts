/**
 * 视频遮挡检测功能 - 类型定义
 *
 * 字段名严格对齐后端 DTO。页面组件只依赖本文件类型，
 * 不直接依赖后端原始字段（若后端命名不同，由 api 层转换）。
 *
 * 设备连接状态、告警状态、页面综合展示状态取自 aiVision 共享层，
 * 因为这三者在所有「摄像头 + 状态卡片 + 历史记录」形态的功能里语义相同。
 */

import type { AlarmStatus, DeviceStatus } from '../../shared/types';

export type { AlarmStatus, DeviceStatus, DisplayStatus } from '../../shared/types';

/** 遮挡类型 */
export type OcclusionType = 'FULL' | 'PARTIAL' | 'PAINT' | 'LENS_COVER' | 'OTHER';

/** 算法任务状态 */
export type AlgorithmStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'TIMEOUT' | 'SKIPPED' | 'MOCK';

/** 算法结论 */
export type AlgorithmResult = 'NORMAL' | 'ANOMALY' | 'UNCERTAIN';

/**
 * 人工处理状态。
 *
 * 库中保留五种取值，页面只显示两态：PENDING = 未处理，其余 = 已处理。
 */
export type ProcessStatus = 'PENDING' | 'PROCESSING' | 'VERIFIED_TRUE' | 'VERIFIED_FALSE' | 'CLOSED';

/** 视频协议 */
export type StreamProtocol = 'HLS' | 'FLV' | 'WEBRTC' | 'MP4';

/** 证据类型 */
export type EvidenceType = 'OCCLUDED_FRAME' | 'NORMAL_REFERENCE' | 'RECOVERY_IMAGE' | 'EVENT_VIDEO';

/** 摄像头状态卡片对象 */
export interface CameraStatus {
  cameraId: string;
  cameraCode: string;
  cameraName: string;
  warehouseId: string;
  warehouseName: string;
  platformCameraId?: string | null;
  deviceStatus: DeviceStatus;
  /** 遮挡告警状态，复用 AlarmStatus 语义 */
  occlusionStatus: AlarmStatus;
  lastHeartbeatAt?: string | null;
  latestEventAt?: string | null;
  sortNo: number;
}

/** 播放信息；禁止承载摄像头凭据或永久地址 */
export interface StreamInfo {
  cameraId: string;
  platformCameraId?: string | null;
  playableUrl?: string | null;
  protocol: StreamProtocol;
  expiresAt?: string | null;
  canPlay: boolean;
  unavailableReason?: string | null;
}

/** 遮挡事件列表项 */
export interface OcclusionEvent {
  eventId: string;
  eventNo: string;
  occurredAt: string;
  warehouseId: string;
  warehouseName: string;
  cameraConfigId: string;
  cameraCode: string;
  cameraPosition: string;
  occlusionType: OcclusionType;
  occlusionTypeName?: string | null;
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
export interface OcclusionEvidence {
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
 * 算法任务行（详情弹窗「算法任务」表 + 轮询接口共用）。
 *
 * 字段与后端 OcclusionAlgorithmTaskRespDTO 逐一对应：
 * 结论列为本功能特有的 occlusionType；后端不回传 detections 原文，
 * 前端因此只展示 detectedCount，绝不解析原始算法输出。
 */
export interface AlgorithmTask {
  /** 对外任务标识（= taskNo，轮询参数） */
  taskId: string;
  taskNo: string;
  eventId?: string | null;
  executionStatus: AlgorithmStatus;
  conclusion?: AlgorithmResult | null;
  occlusionType?: OcclusionType | null;
  /** 置信度 0–1；null 表示「暂无」，不得渲染为 0% */
  confidence?: number | string | null;
  modelName?: string | null;
  modelVersion?: string | null;
  algorithmVersion?: string | null;
  configVersion?: string | null;
  inferenceDurationMs?: number | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  /** 检出目标框数量（后端由 detections 派生） */
  detectedCount?: number | null;
  /** 模拟结果标识：true 时页面必须显示「模拟」标签，不可省略 */
  simulated?: boolean | null;
  errorCode?: string | null;
  errorMessage?: string | null;
  retryCount?: number | null;
}

/** 「重新识别」提交受理结果 */
export interface RerunAccepted {
  taskId: string;
  executionStatus: AlgorithmStatus;
  simulated: boolean;
}

/** 事件详情 */
export interface OcclusionEventDetail extends OcclusionEvent {
  eventDesc?: string | null;
  recoveredAt?: string | null;
  durationSeconds?: number | null;
  /** 是否为模拟数据 */
  isSimulated?: boolean;
  evidences: OcclusionEvidence[];
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
  cameraConfigId?: string;
  cameraCode?: string;
  occlusionType?: OcclusionType | '';
  processStatus?: ProcessStatus | '';
  startTime?: string;
  endTime?: string;
}

/** 播放器可见状态 */
export type PlayerVisibleState =
  | 'NO_CAMERA_SELECTED'
  | 'LOADING'
  | 'PLAYING'
  | 'ERROR'
  | 'EXPIRED'
  | 'OFFLINE'
  | 'NO_ADDRESS'
  | 'UNSUPPORTED_PROTOCOL';

export type ExportSource = 'RECENT' | 'HISTORY';
