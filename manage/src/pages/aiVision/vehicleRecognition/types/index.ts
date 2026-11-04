/**
 * 车辆智能识别功能 - 类型定义
 *
 * 字段名严格对齐后端 DTO。页面组件只依赖本文件类型，
 * 不直接依赖后端原始字段（若后端命名不同，由 api 层转换）。
 *
 * 设备连接状态、告警状态、页面综合展示状态取自 aiVision 共享层，
 * 因为这三者在所有「点位 + 摄像头 + 实时监控」形态的功能里语义相同。
 */

import type { AlarmStatus, DeviceStatus } from '../../shared/types';

export type { AlarmStatus, DeviceStatus, DisplayStatus } from '../../shared/types';

/** 行驶方向 */
export type Direction = 'IN' | 'OUT';

/** 核实状态：PENDING 待核实 / VERIFIED 已核实 / REJECTED 误报（预留） */
export type VerifyStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

/** 算法任务状态 */
export type AlgorithmStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'TIMEOUT' | 'SKIPPED' | 'MOCK';

/**
 * 算法任务结论。
 *
 * 车辆事件表没有 algorithm_result 列，结论由任务行给出；
 * 结论类型字段是 vehicleType（后端 VehicleAlgorithmTaskRespDTO），不是 conclusionType。
 */
export type AlgorithmConclusion = 'RECOGNIZED' | 'NO_VEHICLE' | 'UNCERTAIN';

/** 视频协议 */
export type StreamProtocol = 'HLS' | 'FLV' | 'WEBRTC' | 'MP4';

/** 证据类型 */
export type EvidenceType = 'VEHICLE_IMAGE' | 'PLATE_CROP' | 'ANNOTATED';

/**
 * 识别点位。
 *
 * 车辆功能左侧不是点位状态网格（而是今日识别车辆），但取流要按点位定位摄像头，
 * 因此点位对象仍带设备连接状态与心跳，供 VideoMonitor 的在线判定与后续点位视图使用。
 */
export interface MonitorPoint {
  pointId: string;
  pointCode: string;
  pointName: string;
  warehouseId: string;
  warehouseName: string;
  cameraCode: string;
  cameraName: string;
  platformCameraId?: string | null;
  deviceStatus: DeviceStatus;
  alarmStatus: AlarmStatus;
  lastHeartbeatAt?: string | null;
  latestEventAt?: string | null;
  sortNo: number;
}

/** 播放信息；禁止承载摄像头凭据或永久地址 */
export interface StreamInfo {
  pointId: string;
  platformCameraId?: string | null;
  playableUrl?: string | null;
  protocol: StreamProtocol;
  expiresAt?: string | null;
  canPlay: boolean;
  unavailableReason?: string | null;
}

/** 今日识别车辆卡片（左侧列表项） */
export interface TodayVehicle {
  eventId: string;
  plateNo: string;
  vehicleType: string;
  vehicleColor: string;
  direction: Direction;
  directionName: string;
  /** 取流入参：选中本条后播放它所属点位的摄像头 */
  pointId: string;
  gateName: string;
  recognizedAt: string;
  verifyStatus: VerifyStatus;
}

/** 出入事件记录列表项（底部表 / 历史页） */
export interface VehicleEvent {
  eventId: string;
  eventNo: string;
  plateNo: string;
  vehicleType: string;
  vehicleColor: string;
  direction: Direction;
  directionName: string;
  pointId: string;
  gateName: string;
  recognizedAt: string;
  warehouseId: string;
  warehouseName: string;
  cameraCode: string;
  algorithmStatus: AlgorithmStatus;
  /** 置信度 0–1，null 表示「暂无」，不得渲染为 0% */
  confidence?: number | null;
  verifyStatus: VerifyStatus;
  processedBy?: string | null;
  processedAt?: string | null;
  verifyOpinion?: string | null;
  /** 乐观锁版本号，提交处理时必须原样回传 */
  eventVersion: number;
}

/** 证据 */
export interface VehicleEvidence {
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

/** 核实审计记录 */
export interface ProcessRecord {
  recordId: string;
  statusBefore: VerifyStatus;
  statusAfter: VerifyStatus;
  processResult: string;
  processOpinion: string;
  processedBy: string;
  processedAt: string;
  source?: string | null;
}

/**
 * 算法任务行（详情弹窗「算法任务」表 + 轮询接口共用）。
 *
 * 字段严格对齐后端 VehicleAlgorithmTaskRespDTO：
 * - 无 detections / 完整车牌，车牌只有掩码 plateMasked（后端已脱敏）；
 * - 结论类型是 vehicleType。
 */
export interface AlgorithmTask {
  /** 对外任务标识（= taskNo，轮询参数） */
  taskId: string;
  taskNo: string;
  eventId?: string | null;
  pointId?: string | null;
  taskType?: string | null;
  executionStatus: AlgorithmStatus;
  conclusion?: AlgorithmConclusion | null;
  /** 算法给出的标准化车型（结论类型字段） */
  vehicleType?: string | null;
  /** 算法给出的行驶方向 */
  direction?: Direction | null;
  /** 车牌掩码（后端脱敏后），仅展示，不得与事件车牌互相覆盖 */
  plateMasked?: string | null;
  plateRef?: string | null;
  /** 置信度 0–1；null 表示「暂无」，不得渲染为 0% */
  confidence?: number | string | null;
  modelName?: string | null;
  modelVersion?: string | null;
  algorithmVersion?: string | null;
  configVersion?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  inferenceDurationMs?: number | null;
  /** 检出目标框数量（后端派生） */
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

/** 记录详情 */
export interface VehicleEventDetail extends VehicleEvent {
  eventDesc?: string | null;
  /** 是否为模拟数据 */
  isSimulated?: boolean;
  evidences: VehicleEvidence[];
  processRecords: ProcessRecord[];
  /** 历史算法任务（按开始时间倒序，最多 20 条），随详情一次带回 */
  algorithmTasks: AlgorithmTask[];
  /** 最新一条算法任务；无任务时为空 */
  latestAlgorithmTask?: AlgorithmTask | null;
}

/** 查询参数 */
export interface EventQueryParams {
  pageNo?: number;
  pageSize?: number;
  plateNo?: string;
  vehicleType?: string;
  direction?: Direction | '';
  gateName?: string;
  verifyStatus?: VerifyStatus | '';
  warehouseId?: string;
  pointId?: string;
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
