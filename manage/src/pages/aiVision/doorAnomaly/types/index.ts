/**
 * 仓门异动功能 - 类型定义
 *
 * 基线依据：AI_HANDOFF.md 第 20 节「统一业务数据契约」
 * 字段名严格对齐文档，不得擅自改名。页面组件只依赖本文件类型，
 * 不直接依赖后端原始字段（若后端命名不同，由 api 层转换）。
 */

/* ==================== 跨功能共享的状态类型 ==================== */
/**
 * 设备连接状态、告警状态、页面综合展示状态这三者
 * 在所有「摄像头 + 状态卡片 + 历史记录」形态的功能里语义完全相同，
 * 因此定义在 aiVision/shared/types.ts，这里只做再导出，
 * 让本模块内部与既有引用保持原有导入路径可用。
 */
import type { AlarmStatus, DeviceStatus } from '../../shared/types';

export type { AlarmStatus, DeviceStatus, DisplayStatus } from '../../shared/types';

/* ==================== 第19.2节 仓门物理状态 ==================== */
export type DoorPhysicalStatus = 'OPEN' | 'CLOSED' | 'UNKNOWN';

/* ==================== 第19.5节 算法任务状态 ==================== */
export type AlgorithmStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'TIMEOUT' | 'SKIPPED' | 'MOCK';

/* ==================== 算法任务视图 ==================== */
/**
 * 算法任务行（详情弹窗「算法任务」表与「重新识别」轮询共用）。
 *
 * 字段与后端 `AlgorithmTaskRespDTO` 逐一对应；仓门异动的
 * 「标准化异动类型」在任务视图里叫 `anomalyType`（不是 conclusionType）。
 * confidence / detectedCount 等在完成前为 null，页面显示「暂无」。
 */
export interface AlgorithmTask {
  /** 库内任务标识（字符串化主键） */
  taskId?: string | null;
  /** 任务编号，同时是轮询接口的路径参数 */
  taskNo: string;
  executionStatus: AlgorithmStatus;
  /** 算法结论 NORMAL/ANOMALY/UNCERTAIN，未产出时为空 */
  conclusion?: AlgorithmResult | null;
  /** 标准化异动类型（后端 DTO 字段名即 anomalyType） */
  anomalyType?: AnomalyType | null;
  /** 置信度 0–1；null 表示暂无，不得渲染为 0% */
  confidence?: number | string | null;
  modelName?: string | null;
  modelVersion?: string | null;
  algorithmVersion?: string | null;
  configVersion?: string | null;
  inferenceDurationMs?: number | null;
  startedAt?: string | null;
  finishedAt?: string | null;
  /** 检出目标框数量 */
  detectedCount?: number | null;
  /** 模拟结果标识：为 false 以外时页面必须显示「模拟」标签 */
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

/* ==================== 第19.6节 算法结论 ==================== */
export type AlgorithmResult = 'NORMAL' | 'ANOMALY' | 'UNCERTAIN';

/* ==================== 第19.7节 人工处理状态 ==================== */
export type ProcessStatus = 'PENDING' | 'PROCESSING' | 'VERIFIED_TRUE' | 'VERIFIED_FALSE' | 'CLOSED';

/* ==================== 第19.8节 异动类型 ==================== */
/**
 * 异动类型编码。最终编码由算法能力与业务规则确认（第19.8节），
 * 此处为一期占位编码；页面不得硬编码中文字符串作为唯一判断条件。
 */
export type AnomalyType = 'ILLEGAL_OPEN' | 'LONG_TIME_OPEN' | 'ABNORMAL_MOVE' | 'DEVICE_OFFLINE' | 'OTHER';

/* ==================== 第20.2节 视频协议 ==================== */
/**
 * 视频协议。第26.1节说明：
 * Mock 阶段使用 MP4 或静态占位；正式一期若允许数秒延迟优先 HLS。
 * 当前协议尚未最终确认（第37节），前端不得据此安装播放器依赖。
 */
export type StreamProtocol = 'HLS' | 'FLV' | 'WEBRTC' | 'MP4';

/* ==================== 证据类型（第20.5节） ==================== */
export type EvidenceType =
  | 'NORMAL_REFERENCE'
  | 'ABNORMAL_KEY_FRAME'
  | 'BEFORE_FRAME'
  | 'RECOVERY_IMAGE'
  | 'EVENT_VIDEO';

/* ==================== 第20.1节 仓门状态对象 ==================== */
export interface DoorStatus {
  doorId: string;
  doorName: string;
  warehouseId: string;
  warehouseName: string;
  cameraId?: string | null;
  cameraCode?: string | null;
  deviceStatus: DeviceStatus;
  doorPhysicalStatus: DoorPhysicalStatus;
  alarmStatus: AlarmStatus;
  lastHeartbeatAt?: string | null;
  latestEventAt?: string | null;
  sortNo: number;
}

/* ==================== 第20.2节 视频播放信息 ==================== */
/**
 * 禁止在此对象中承载摄像头账号、密码、永久 Token
 * 或含明文凭据的 RTSP 地址（第20.2、29.1节）。
 */
export interface StreamInfo {
  doorId: string;
  cameraId?: string | null;
  /** 浏览器可播放的短时地址 */
  playableUrl?: string | null;
  protocol: StreamProtocol;
  /** 地址失效时间（ISO 字符串） */
  expiresAt?: string | null;
  canPlay: boolean;
  /** 不可播放原因；canPlay 为 false 时必填 */
  unavailableReason?: string | null;
}

/* ==================== 第20.3节 事件列表对象 ==================== */
export interface AnomalyEvent {
  eventId: string;
  eventNo: string;
  occurredAt: string;
  warehouseId: string;
  warehouseName: string;
  doorId: string;
  doorName: string;
  cameraId?: string | null;
  cameraCode?: string | null;
  anomalyType: AnomalyType;
  anomalyTypeName?: string | null;
  algorithmStatus: AlgorithmStatus;
  algorithmResult?: AlgorithmResult | null;
  /** 置信度 0–1，可为空。空表示「暂无」，不得渲染为 0% */
  confidence?: number | null;
  processStatus: ProcessStatus;
  /** 最近处理人（后端按登录态记录） */
  processedBy?: string | null;
  /** 最近处理时间 */
  processedAt?: string | null;
  /** 最近处理意见摘要；完整历史见 processRecords（第17.6节） */
  processOpinion?: string | null;
  /** 并发控制版本号，提交处理时必须原样回传 */
  eventVersion: number;
}

/* ==================== 第20.5节 事件证据对象 ==================== */
export interface EventEvidence {
  evidenceId: string;
  evidenceType: EvidenceType;
  /** 短时原始资源地址；由后端签发，可能过期需重新获取 */
  originalUrl?: string | null;
  /** 可选的算法标注资源地址；与 originalUrl 分离，不得互相覆盖（第17.4节） */
  annotatedUrl?: string | null;
  thumbnailUrl?: string | null;
  capturedAt: string;
  cameraCode?: string | null;
  /** 资源完整性摘要 */
  contentHash?: string | null;
  sortNo: number;
}

/* ==================== 第17.6节 处理记录 ==================== */
export interface ProcessRecord {
  recordId: string;
  statusBefore: ProcessStatus;
  statusAfter: ProcessStatus;
  /** 真实异常 / 误报 */
  processResult: 'VERIFIED_TRUE' | 'VERIFIED_FALSE';
  processOpinion: string;
  processedBy: string;
  processedAt: string;
  /** 操作来源，如 WEB */
  source?: string | null;
}

/* ==================== 第20.4节 事件详情对象 ==================== */
export interface EventDetail extends AnomalyEvent {
  eventDesc?: string | null;
  algorithmTaskNo?: string | null;
  modelName?: string | null;
  modelVersion?: string | null;
  algorithmVersion?: string | null;
  configVersion?: string | null;
  inferenceStartedAt?: string | null;
  inferenceFinishedAt?: string | null;
  inferenceDurationMs?: number | null;
  /** 是否为模拟数据（第39.6节 isSimulated） */
  isSimulated?: boolean;
  evidences: EventEvidence[];
  processRecords: ProcessRecord[];
  /** 历史算法任务（按 started_at 倒序，最多 20 条），随详情一次带回 */
  algorithmTasks: AlgorithmTask[];
  /** 最新一条算法任务；无任务时为空 */
  latestAlgorithmTask?: AlgorithmTask | null;
}

/* ==================== 查询参数（第21.2节） ==================== */
export interface EventQueryParams {
  pageNo?: number;
  pageSize?: number;
  eventNo?: string;
  warehouseId?: string;
  doorId?: string;
  cameraCode?: string;
  anomalyType?: AnomalyType | '';
  processStatus?: ProcessStatus | '';
  startTime?: string;
  endTime?: string;
}

/* ==================== 播放器可见状态（第16.4节） ==================== */
/**
 * 播放器必须支持的可见状态（第16.4节，共8种）。
 * 本期仅作为状态模型与 UI 占位，不引入任何播放器库。
 */
/**
 * 播放器可见状态（第16.4节）。
 *
 * <p>一期采用原生 {@code <video>}，状态由元素事件驱动，
 * 因此状态集合收敛为下列 6 项 + 未选仓门：</p>
 *
 * - NO_DOOR_SELECTED：尚未选择仓门
 * - LOADING：正在获取播放地址或正在连接
 * - PLAYING：正在播放
 * - ERROR：播放失败（提供可点击重播，不无限自动重试）
 * - EXPIRED：播放地址已过期
 * - OFFLINE：仓门或摄像头离线
 * - NO_ADDRESS：无播放地址或后端明确不可播放
 * - UNSUPPORTED_PROTOCOL：协议不在本期原生支持范围（HLS/FLV/WebRTC）
 */
export type PlayerVisibleState =
  | 'NO_DOOR_SELECTED'
  | 'LOADING'
  | 'PLAYING'
  | 'ERROR'
  | 'EXPIRED'
  | 'OFFLINE'
  | 'NO_ADDRESS'
  | 'UNSUPPORTED_PROTOCOL';

/* ==================== 导出相关 ==================== */
export type ExportSource = 'RECENT' | 'HISTORY';

export interface ExportPayload {
  source: ExportSource;
  params: EventQueryParams;
}
