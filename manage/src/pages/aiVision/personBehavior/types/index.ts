/**
 * 人员行为监测功能 - 类型定义
 *
 * 字段名严格对齐后端 DTO。页面组件只依赖本文件类型，
 * 不直接依赖后端原始字段（若后端命名不同，由 api 层转换）。
 *
 * 设备连接状态、告警状态、页面综合展示状态取自 aiVision 共享层，
 * 因为这三者在所有「监测对象 + 状态卡片 + 历史记录」形态的功能里语义相同。
 */

import type { AlarmStatus, DeviceStatus } from '../../shared/types';

export type { AlarmStatus, DeviceStatus, DisplayStatus } from '../../shared/types';

/** 行为类型（异常结论） */
export type BehaviorType = 'CROWD' | 'NO_HELMET' | 'FALL' | 'INTRUSION' | 'OTHER';

/** 算法任务状态 */
export type AlgorithmStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'TIMEOUT' | 'SKIPPED' | 'MOCK';

/** 算法结论 */
export type AlgorithmResult = 'NORMAL' | 'ANOMALY' | 'UNCERTAIN';

/**
 * 算法任务行（详情弹窗「算法任务」表 + 轮询接口共用）。
 *
 * 字段严格对齐后端 `PersonAlgorithmTaskRespDTO`：
 * 人员行为侧结论类型列是 behaviorType，不是虫害那版的 conclusionType。
 */
export interface AlgorithmTask {
  /** 对外任务标识（= taskNo，轮询参数） */
  taskId: string;
  taskNo: string;
  eventId?: string | null;
  executionStatus: AlgorithmStatus;
  conclusion?: AlgorithmResult | null;
  behaviorType?: BehaviorType | null;
  /** 置信度 0–1；null 表示「暂无」，不得渲染为 0% */
  confidence?: number | string | null;
  modelName?: string | null;
  modelVersion?: string | null;
  algorithmVersion?: string | null;
  configVersion?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  inferenceDurationMs?: number | null;
  /** 检出目标数（后端由 detections JSON 派生） */
  detectedCount?: number | null;
  /** 模拟结果标识：非 false 时页面必须显示「模拟」标签 */
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

/**
 * 人工处理状态。
 *
 * 三值，与原型图的「未核实 / 处理中 / 已处理」一一对应。
 * 视频遮挡那一版是五值折叠成两态，本功能不折叠。
 */
export type ProcessStatus = 'PENDING' | 'PROCESSING' | 'DONE';

/** 视频协议 */
export type StreamProtocol = 'HLS' | 'FLV' | 'WEBRTC' | 'MP4';

/** 证据类型 */
export type EvidenceType = 'BEHAVIOR_FRAME' | 'NORMAL_REFERENCE' | 'RECOVERY_IMAGE' | 'EVENT_VIDEO';

/** 监测区域状态卡片对象 */
export interface MonitorPoint {
  pointId: string;
  pointCode: string;
  /** 区域名，卡片标题与右侧监控标题都用它 */
  pointName: string;
  warehouseId: string;
  warehouseName: string;
  cameraId?: string | null;
  cameraCode: string;
  platformCameraId?: string | null;
  deviceStatus: DeviceStatus;
  alarmStatus: AlarmStatus;
  /**
   * 当前告警对应的行为类型。
   * 共享基座的综合展示态只有一个 ABNORMAL，区分不了「人员聚集」与「未戴安全帽」，
   * 异常卡片的文案靠这一列还原。
   */
  alarmBehaviorType?: BehaviorType | null;
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

/** 人员行为事件列表项 */
export interface PersonEvent {
  eventId: string;
  eventNo: string;
  occurredAt: string;
  warehouseId: string;
  warehouseName: string;
  pointId: string;
  cameraCode: string;
  areaName: string;
  behaviorType: BehaviorType;
  behaviorTypeName?: string | null;
  algorithmStatus: AlgorithmStatus;
  algorithmResult?: AlgorithmResult | null;
  /** 置信度 0–1，null 表示「暂无」，不得渲染为 0% */
  confidence?: number | null;
  /** 聚集人数，仅 CROWD 类事件有值；null 表示该指标不适用 */
  personCount?: number | null;
  processStatus: ProcessStatus;
  processStatusName?: string | null;
  processedBy?: string | null;
  processedAt?: string | null;
  processOpinion?: string | null;
  /** 乐观锁版本号，提交处理时必须原样回传 */
  eventVersion: number;
}

/** 证据 */
export interface PersonEvidence {
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

/** 事件详情 */
export interface PersonEventDetail extends PersonEvent {
  eventDesc?: string | null;
  recoveredAt?: string | null;
  durationSeconds?: number | null;
  /** 是否为模拟数据 */
  isSimulated?: boolean;
  evidences: PersonEvidence[];
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
  behaviorType?: BehaviorType | '';
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
