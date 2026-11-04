/**
 * 周界入侵识别功能 - 类型定义
 *
 * 字段名严格对齐后端 DTO。页面组件只依赖本文件类型，
 * 不直接依赖后端原始字段（若后端命名不同，由 api 层转换）。
 *
 * 设备连接状态、告警状态取自 aiVision 共享层；
 * 但**页面综合展示状态在本功能被扩了一维**（多一个「未布防」），
 * 因此本文件在共享 DisplayStatus 之上派生 PerimeterDisplayStatus，
 * 不修改共享层。
 */

import type { AlarmStatus, DeviceStatus, DisplayStatus } from '../../shared/types';

export type { AlarmStatus, DeviceStatus, DisplayStatus } from '../../shared/types';

/** 所属防区 */
export type Zone = 'EAST' | 'WEST' | 'SOUTH' | 'NORTH';

/** 人工布防开关；DISARMED 时页面显示「未布防」 */
export type DefenseStatus = 'ARMED' | 'DISARMED';

/**
 * 本功能的页面展示状态。
 *
 * 共享层的 DisplayStatus 只有 NORMAL/ABNORMAL/OFFLINE/UNKNOWN 四态，
 * 表达不出「人工撤防」，因此这里加一个 DISARMED 分支。
 * 计算见 composables/usePerimeterDisplayStatus.ts。
 */
export type PerimeterDisplayStatus = DisplayStatus | 'DISARMED';

/** 入侵结论类型 */
export type IntrusionType = 'CLIMB' | 'CROSS' | 'INTRUSION' | 'LOITER' | 'OTHER';

/** 算法任务状态 */
export type AlgorithmStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'TIMEOUT' | 'SKIPPED' | 'MOCK';

/** 算法结论 */
export type AlgorithmResult = 'NORMAL' | 'ANOMALY' | 'UNCERTAIN';

/**
 * 人工处理状态（三值，与原型一致）。
 *
 * 与遮挡那一版的五值（含 VERIFIED_TRUE/CLOSED 等）不同，
 * 本功能按原型只用 PENDING/PROCESSING/DONE。
 */
export type ProcessStatus = 'PENDING' | 'PROCESSING' | 'DONE';

/** 视频协议 */
export type StreamProtocol = 'HLS' | 'FLV' | 'WEBRTC' | 'MP4';

/** 证据类型 */
export type EvidenceType = 'INTRUSION_FRAME' | 'NORMAL_REFERENCE' | 'RECOVERY_IMAGE' | 'EVENT_VIDEO';

/** 防区段状态卡片对象 */
export interface MonitorPointStatus {
  pointId: string;
  pointCode: string;
  /** 卡片标题，形如「东防区-东墙段01」 */
  pointName: string;
  zone: Zone;
  /** 防区展示名，如「东区」 */
  zoneName: string;
  segmentName: string;
  warehouseId: string;
  warehouseName: string;
  cameraConfigId: string;
  cameraCode: string;
  /** 绑定摄像头的安装位置，右侧监控标题用 */
  cameraName: string;
  platformCameraId?: string | null;
  deviceStatus: DeviceStatus;
  /** 入侵告警状态，复用 AlarmStatus 语义 */
  alarmStatus: AlarmStatus;
  /** 人工布防开关 */
  defenseStatus: DefenseStatus;
  lastHeartbeatAt?: string | null;
  latestEventAt?: string | null;
  sortNo: number;
}

/** 播放信息；禁止承载摄像头凭据或永久地址 */
export interface StreamInfo {
  pointId: string;
  cameraConfigId?: string | null;
  platformCameraId?: string | null;
  playableUrl?: string | null;
  protocol: StreamProtocol;
  expiresAt?: string | null;
  canPlay: boolean;
  unavailableReason?: string | null;
}

/** 入侵事件列表项 */
export interface PerimeterEvent {
  eventId: string;
  eventNo: string;
  occurredAt: string;
  warehouseId: string;
  warehouseName: string;
  pointId: string;
  pointName: string;
  zone: Zone;
  zoneName: string;
  cameraConfigId: string;
  cameraCode: string;
  cameraPosition: string;
  intrusionType: IntrusionType;
  intrusionTypeName?: string | null;
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
export interface PerimeterEvidence {
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
 * 周界任务的结论类型字段是 intrusionType（后端 PerimeterAlgorithmTaskRespDTO），
 * 不是虫害侧的 conclusionType，不得混用。
 */
export interface AlgorithmTask {
  /** 对外任务标识（= taskNo，轮询参数） */
  taskId: string;
  taskNo: string;
  eventId?: string | null;
  /** 固定 PERIMETER_INTRUSION_DETECT */
  taskType?: string | null;
  executionStatus: AlgorithmStatus;
  conclusion?: AlgorithmResult | null;
  /** 标准化入侵类型 CLIMB/CROSS/INTRUSION/LOITER/OTHER；未产出为 null */
  intrusionType?: IntrusionType | null;
  /** 置信度 0–1；null 表示「暂无」，不得渲染为 0% */
  confidence?: number | string | null;
  modelName?: string | null;
  modelVersion?: string | null;
  algorithmVersion?: string | null;
  configVersion?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  inferenceDurationMs?: number | null;
  /** 检出目标数（后端由 detections 派生） */
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
export interface PerimeterEventDetail extends PerimeterEvent {
  eventDesc?: string | null;
  recoveredAt?: string | null;
  durationSeconds?: number | null;
  /** 是否为模拟数据 */
  isSimulated?: boolean;
  evidences: PerimeterEvidence[];
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
  /** 防区筛选，对应网格上方同一个下拉 */
  zone?: Zone | '';
  cameraCode?: string;
  intrusionType?: IntrusionType | '';
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
  | 'DISARMED'
  | 'NO_ADDRESS'
  | 'UNSUPPORTED_PROTOCOL';

export type ExportSource = 'RECENT' | 'HISTORY';
