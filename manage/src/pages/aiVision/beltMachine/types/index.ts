/**
 * 皮带机下站人监测功能 - 类型定义
 *
 * 字段名严格对齐后端 DTO。页面组件只依赖本文件类型，
 * 不直接依赖后端原始字段（若后端命名不同，由 api 层转换）。
 *
 * 设备连接状态、告警状态、页面综合展示状态取自 aiVision 共享层，
 * 因为这三者在所有「监测对象 + 状态卡片 + 历史记录」形态的功能里语义相同。
 */

import type { AlarmStatus, DeviceStatus } from '../../shared/types';

export type { AlarmStatus, DeviceStatus, DisplayStatus } from '../../shared/types';

/** 结论类型（本功能的事件类型） */
export type ConclusionType = 'STANDING_UNDER' | 'CROSSING' | 'LOITERING' | 'OTHER';

/** 算法任务状态 */
export type AlgorithmStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'TIMEOUT' | 'SKIPPED' | 'MOCK';

/** 算法结论 */
export type AlgorithmResult = 'NORMAL' | 'ANOMALY' | 'UNCERTAIN';

/**
 * 人工处理状态。
 *
 * 按原型图取三值：未核实 / 处理中 / 已处理。
 * 与视频遮挡那版的五值不同，两版功能不可共用同一份标签映射。
 */
export type ProcessStatus = 'PENDING' | 'PROCESSING' | 'DONE';

/** 视频协议 */
export type StreamProtocol = 'HLS' | 'FLV' | 'WEBRTC' | 'MP4';

/** 证据类型 */
export type EvidenceType = 'ANOMALY_FRAME' | 'NORMAL_REFERENCE' | 'ANNOTATED_FRAME' | 'EVENT_VIDEO';

/**
 * 皮带机监测点位卡片对象。
 *
 * `cameraId` 为空表示该皮带机没有绑定摄像头，
 * 与 `deviceStatus='UNKNOWN'` 一起构成页面的「未部署」态。
 */
export interface MonitorPoint {
  pointId: string;
  deviceCode: string;
  /** 卡片标题：一号皮带机 / 二号皮带机 … */
  deviceName: string;
  areaName: string;
  warehouseId: string;
  warehouseName: string;
  /** 绑定摄像头平台标识，空 = 未绑定 */
  cameraId?: string | null;
  cameraCode?: string | null;
  cameraName?: string | null;
  deviceStatus: DeviceStatus;
  /** 行为告警状态，复用 AlarmStatus 语义 */
  alarmStatus: AlarmStatus;
  lastHeartbeatAt?: string | null;
  latestEventAt?: string | null;
  sortNo: number;
}

/** 播放信息；禁止承载摄像头凭据或永久地址 */
export interface StreamInfo {
  pointId: string;
  cameraId?: string | null;
  cameraCode?: string | null;
  cameraName?: string | null;
  playableUrl?: string | null;
  protocol: StreamProtocol;
  expiresAt?: string | null;
  canPlay: boolean;
  unavailableReason?: string | null;
}

/** 事件列表项 */
export interface BeltMachineEvent {
  eventId: string;
  eventNo: string;
  occurredAt: string;
  warehouseId: string;
  warehouseName: string;
  pointId: string;
  deviceCode: string;
  /** 皮带机名称快照，列表「皮带机」列 */
  deviceName: string;
  /** 摄像头编号快照，列表「摄像头」列 */
  cameraCode: string;
  conclusionType: ConclusionType;
  conclusionTypeName?: string | null;
  /** 画面内检出人数，null 表示算法未给出 */
  personCount?: number | null;
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
export interface BeltMachineEvidence {
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
 * 算法任务视图（详情弹窗「算法任务」区与「重新识别」轮询共用）。
 *
 * 字段与后端 BeltMachineAlgorithmTaskRespDTO 逐一对应。
 * confidence / detectedCount 等在完成前为 null，页面显示「暂无」。
 */
export interface AlgorithmTask {
  /** 库内任务标识（字符串化 BIGINT） */
  taskId?: string | null;
  /** 任务编号，同时是轮询接口的路径参数 */
  taskNo: string;
  executionStatus: AlgorithmStatus;
  conclusion?: AlgorithmResult | null;
  conclusionType?: ConclusionType | null;
  /** 置信度 0–1，null 表示暂无 */
  confidence?: number | null;
  modelName?: string | null;
  modelVersion?: string | null;
  algorithmVersion?: string | null;
  configVersion?: string | null;
  inferenceDurationMs?: number | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  /** 检出目标数，来自 detections 数组长度 */
  detectedCount?: number | null;
  /** 模拟结果标识，true 时页面必须显示「模拟」标签 */
  simulated?: boolean | null;
  errorCode?: string | null;
  errorMessage?: string | null;
}

/** 「重新识别」受理回执 */
export interface RerunAccepted {
  taskId: string;
  executionStatus: AlgorithmStatus;
  simulated: boolean;
}

/** 事件详情 */
export interface BeltMachineEventDetail extends BeltMachineEvent {
  eventDesc?: string | null;
  recoveredAt?: string | null;
  durationSeconds?: number | null;
  /** 是否为模拟数据 */
  isSimulated?: boolean;
  evidences: BeltMachineEvidence[];
  processRecords: ProcessRecord[];
  /** 算法任务列表（开始时间倒序，最多 20 条），弹窗「算法任务」区数据源 */
  algorithmTasks: AlgorithmTask[];
  /** 最近一次算法任务，可为空 */
  latestAlgorithmTask?: AlgorithmTask | null;
}

/** 查询参数 */
export interface EventQueryParams {
  pageNo?: number;
  pageSize?: number;
  eventNo?: string;
  warehouseId?: string;
  pointId?: string;
  deviceName?: string;
  cameraCode?: string;
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
  | 'NOT_DEPLOYED'
  | 'NO_ADDRESS'
  | 'UNSUPPORTED_PROTOCOL';

export type ExportSource = 'RECENT' | 'HISTORY';
