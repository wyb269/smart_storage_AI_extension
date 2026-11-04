/**
 * 车辆智能识别功能 - API 适配层
 *
 * 职责：
 * 1. 本文件是本功能访问后端的**唯一出口**，
 *    页面组件内不得散落 URL 字符串与原始字段转换。
 * 2. 统一处理后端 ActionResult → 页面模型的字段映射。
 * 3. 统一处理项目分页结构（result.data / result.total）。
 */

import type { EventExportRequest, EventPageRequest, PageResult } from '@/api/model/vehicleRecognitionModel';
import type {
  AlgorithmTask,
  RerunAccepted,
  StreamInfo,
  TodayVehicle,
  VehicleEvent,
  VehicleEventDetail,
} from '@/pages/aiVision/vehicleRecognition/types';
import { request } from '@/utils/request';

/** 接口路径常量 */
export const VEHICLE_RECOGNITION_API = {
  /** 今日识别车辆列表 */
  LIST_TODAY_VEHICLE: '/vehicleRecognition/listTodayVehicle',
  /** 获取指定识别点位摄像头的短时播放信息 */
  GET_STREAM_INFO: '/vehicleRecognition/getStreamInfo',
  /** 首页最近识别记录 */
  LIST_RECENT_EVENT: '/vehicleRecognition/listRecentEvent',
  /** 历史记录服务端分页查询 */
  LIST_EVENT_BY_CONDITION: '/vehicleRecognition/listEventByCondition',
  /** 查询记录详情、证据和核实记录 */
  GET_EVENT_DETAIL: '/vehicleRecognition/getEventDetail',
  /** 查看记录后标记为已核实 */
  MARK_EVENT_VIEWED: '/vehicleRecognition/markEventViewed',
  /** 导出当前查询条件下的记录 */
  EXPORT_EVENT: '/vehicleRecognition/exportEvent',
  /** 提交「重新识别」（立即返回 taskId，结果轮询 algorithmTask） */
  RERUN_ALGORITHM: '/vehicleRecognition/rerunAlgorithm',
  /** 轮询算法任务状态 */
  ALGORITHM_TASK: '/vehicleRecognition/algorithmTask',
  /** 事件历史算法任务列表 */
  LIST_ALGORITHM_TASKS: '/vehicleRecognition/listAlgorithmTasks',
} as const;

/* ==================== 字段转换 ==================== */

/**
 * 后端可能返回下划线或驼峰，两种都兼容。
 * 泛型默认 any，返回值可能为 undefined，因此各字段统一用 `?? 默认值` 兜底。
 */
function pick(source: any, camelKey: string, snakeKey: string): any {
  if (!source) return undefined;
  if (source[camelKey] !== undefined && source[camelKey] !== null) return source[camelKey];
  if (source[snakeKey] !== undefined && source[snakeKey] !== null) return source[snakeKey];
  return undefined;
}

/** 归一化播放信息 */
function normalizeStreamInfo(raw: any): StreamInfo {
  return {
    pointId: String(pick(raw, 'pointId', 'point_id') ?? ''),
    platformCameraId: pick(raw, 'platformCameraId', 'platform_camera_id'),
    playableUrl: pick(raw, 'playableUrl', 'playable_url'),
    protocol: pick(raw, 'protocol', 'protocol') ?? 'MP4',
    expiresAt: pick(raw, 'expiresAt', 'expires_at'),
    // 缺省视为不可播放，避免无权限时被误判为可播
    canPlay: pick(raw, 'canPlay', 'can_play') === true,
    unavailableReason: pick(raw, 'unavailableReason', 'unavailable_reason'),
  } as StreamInfo;
}

/** 归一化今日识别车辆卡片 */
function normalizeTodayVehicle(raw: any): TodayVehicle {
  return {
    eventId: String(pick(raw, 'eventId', 'event_id') ?? ''),
    plateNo: pick(raw, 'plateNo', 'plate_no') ?? '',
    vehicleType: pick(raw, 'vehicleType', 'vehicle_type') ?? '',
    vehicleColor: pick(raw, 'vehicleColor', 'vehicle_color') ?? '',
    direction: pick(raw, 'direction', 'direction') ?? 'IN',
    directionName: pick(raw, 'directionName', 'direction_name') ?? '',
    pointId: String(pick(raw, 'pointId', 'point_id') ?? ''),
    gateName: pick(raw, 'gateName', 'gate_name') ?? '',
    recognizedAt: pick(raw, 'recognizedAt', 'recognized_at') ?? '',
    verifyStatus: pick(raw, 'verifyStatus', 'verify_status') ?? 'PENDING',
  } as TodayVehicle;
}

/** 归一化识别记录列表项 */
function normalizeEvent(raw: any): VehicleEvent {
  return {
    eventId: String(pick(raw, 'eventId', 'event_id') ?? ''),
    eventNo: pick(raw, 'eventNo', 'event_no') ?? '',
    plateNo: pick(raw, 'plateNo', 'plate_no') ?? '',
    vehicleType: pick(raw, 'vehicleType', 'vehicle_type') ?? '',
    vehicleColor: pick(raw, 'vehicleColor', 'vehicle_color') ?? '',
    direction: pick(raw, 'direction', 'direction') ?? 'IN',
    directionName: pick(raw, 'directionName', 'direction_name') ?? '',
    pointId: String(pick(raw, 'pointId', 'point_id') ?? ''),
    gateName: pick(raw, 'gateName', 'gate_name') ?? '',
    recognizedAt: pick(raw, 'recognizedAt', 'recognized_at') ?? '',
    warehouseId: String(pick(raw, 'warehouseId', 'warehouse_id') ?? ''),
    warehouseName: pick(raw, 'warehouseName', 'warehouse_name') ?? '',
    cameraCode: pick(raw, 'cameraCode', 'camera_code') ?? '',
    algorithmStatus: pick(raw, 'algorithmStatus', 'algorithm_status'),
    // 保留 null 以区分「暂无」与 0
    confidence: pick(raw, 'confidence', 'confidence'),
    verifyStatus: pick(raw, 'verifyStatus', 'verify_status'),
    processedBy: pick(raw, 'processedBy', 'processed_by'),
    processedAt: pick(raw, 'processedAt', 'processed_at'),
    verifyOpinion: pick(raw, 'verifyOpinion', 'verify_opinion'),
    eventVersion: Number(pick(raw, 'eventVersion', 'event_version') ?? 0),
  } as VehicleEvent;
}

/**
 * 归一化算法任务行（字段对齐后端 VehicleAlgorithmTaskRespDTO）。
 *
 * 注意：
 * - 车辆侧没有 algorithm_result 列，任务结论是 conclusion（RECOGNIZED/NO_VEHICLE/UNCERTAIN），
 *   结论类型字段是 `vehicleType`（不是 conclusionType）。
 * - 任务不携带检测框明细，车牌只有掩码 `plateMasked`；本层不得拼出完整车牌。
 */
function normalizeAlgorithmTask(raw: any): AlgorithmTask {
  return {
    taskId: String(pick(raw, 'taskId', 'task_id') ?? pick(raw, 'taskNo', 'task_no') ?? ''),
    taskNo: pick(raw, 'taskNo', 'task_no') ?? '',
    eventId: pick(raw, 'eventId', 'event_id'),
    pointId: pick(raw, 'pointId', 'point_id'),
    taskType: pick(raw, 'taskType', 'task_type'),
    executionStatus: pick(raw, 'executionStatus', 'execution_status') ?? 'QUEUED',
    conclusion: pick(raw, 'conclusion', 'conclusion'),
    // 后端 DTO 只有 vehicleType；兼容旧字段名 conclusionType 但不以它为准
    vehicleType: pick(raw, 'vehicleType', 'vehicle_type') ?? pick(raw, 'conclusionType', 'conclusion_type'),
    direction: pick(raw, 'direction', 'direction'),
    // 掩码车牌：后端已脱敏，前端只展示，不得与事件车牌互相覆盖
    plateMasked: pick(raw, 'plateMasked', 'plate_masked'),
    plateRef: pick(raw, 'plateRef', 'plate_ref'),
    // 置信度保留 null：任务表据此显示「暂无」而不是 0%
    confidence: pick(raw, 'confidence', 'confidence'),
    modelName: pick(raw, 'modelName', 'model_name'),
    modelVersion: pick(raw, 'modelVersion', 'model_version'),
    algorithmVersion: pick(raw, 'algorithmVersion', 'algorithm_version'),
    configVersion: pick(raw, 'configVersion', 'config_version'),
    startedAt: pick(raw, 'startedAt', 'started_at'),
    finishedAt: pick(raw, 'finishedAt', 'finished_at'),
    inferenceDurationMs: pick(raw, 'inferenceDurationMs', 'inference_duration_ms'),
    detectedCount: pick(raw, 'detectedCount', 'detected_count'),
    simulated: pick(raw, 'simulated', 'is_simulated') ?? pick(raw, 'isSimulated', 'simulated'),
    errorCode: pick(raw, 'errorCode', 'error_code'),
    errorMessage: pick(raw, 'errorMessage', 'error_message'),
  } as AlgorithmTask;
}

/** 归一化记录详情 */
function normalizeEventDetail(raw: any): VehicleEventDetail {
  const base = normalizeEvent(raw);
  const evidences = pick(raw, 'evidences', 'evidenceList') ?? [];
  const processRecords = pick(raw, 'processRecords', 'process_record_list') ?? [];
  const algorithmTasks = pick(raw, 'algorithmTasks', 'algorithm_tasks') ?? [];

  return {
    ...base,
    eventDesc: pick(raw, 'eventDesc', 'event_desc'),
    isSimulated: pick(raw, 'isSimulated', 'is_simulated'),
    evidences: evidences.map((e: any) => ({
      evidenceId: String(pick(e, 'evidenceId', 'evidence_id') ?? ''),
      evidenceType: pick(e, 'evidenceType', 'evidence_type'),
      originalUrl: pick(e, 'originalUrl', 'original_url'),
      // 标注图与原图分离，不得互相覆盖
      annotatedUrl: pick(e, 'annotatedUrl', 'annotated_url'),
      thumbnailUrl: pick(e, 'thumbnailUrl', 'thumbnail_url'),
      capturedAt: pick(e, 'capturedAt', 'captured_at') ?? '',
      cameraCode: pick(e, 'cameraCode', 'camera_code'),
      contentHash: pick(e, 'contentHash', 'content_hash'),
      sortNo: Number(pick(e, 'sortNo', 'sort_no') ?? 0),
    })),
    processRecords: processRecords.map((r: any) => ({
      recordId: String(pick(r, 'recordId', 'record_id') ?? ''),
      statusBefore: pick(r, 'statusBefore', 'status_before'),
      statusAfter: pick(r, 'statusAfter', 'status_after'),
      processResult: pick(r, 'processResult', 'process_result'),
      processOpinion: pick(r, 'processOpinion', 'process_opinion') ?? '',
      processedBy: pick(r, 'processedBy', 'processed_by') ?? '',
      processedAt: pick(r, 'processedAt', 'processed_at') ?? '',
      source: pick(r, 'source', 'source'),
    })),
    // 算法任务历史随详情一次带回（开始时间倒序，最多 20 条），弹窗任务表不再单独请求
    algorithmTasks: (Array.isArray(algorithmTasks) ? algorithmTasks : []).map(normalizeAlgorithmTask),
    latestAlgorithmTask: (() => {
      const latest = pick(raw, 'latestAlgorithmTask', 'latest_algorithm_task');
      return latest ? normalizeAlgorithmTask(latest) : null;
    })(),
  } as VehicleEventDetail;
}

/** 提取项目统一分页结构 */
function extractPage<T>(res: any, mapper: (raw: any) => T): PageResult<T> {
  const result = res?.result ?? {};
  const list = Array.isArray(result.data) ? result.data : [];
  return {
    data: list.map(mapper),
    total: Number(result.total ?? 0),
  };
}

/* ==================== 接口实现 ==================== */

/** 今日识别车辆列表；后端负责数据范围过滤 */
export async function listTodayVehicle(limit = 50): Promise<TodayVehicle[]> {
  const res = await request.post({ url: VEHICLE_RECOGNITION_API.LIST_TODAY_VEHICLE, data: { limit } });
  const list = Array.isArray(res?.result) ? res.result : [];
  return list.map(normalizeTodayVehicle);
}

/** 获取指定识别点位摄像头的短时播放信息；地址过期后需重新调用 */
export async function getStreamInfo(pointId: string): Promise<StreamInfo> {
  const res = await request.post({
    url: VEHICLE_RECOGNITION_API.GET_STREAM_INFO,
    data: { pointId },
  });
  return normalizeStreamInfo(res?.result);
}

/** 首页最近识别记录 */
export async function listRecentEvent(limit = 5): Promise<VehicleEvent[]> {
  const res = await request.post({
    url: VEHICLE_RECOGNITION_API.LIST_RECENT_EVENT,
    data: { limit },
  });
  const list = Array.isArray(res?.result) ? res.result : [];
  return list.map(normalizeEvent);
}

/** 历史记录服务端分页查询；禁止前端伪分页 */
export async function listEventByCondition(params: EventPageRequest): Promise<PageResult<VehicleEvent>> {
  const res = await request.post({
    url: VEHICLE_RECOGNITION_API.LIST_EVENT_BY_CONDITION,
    data: params,
  });
  return extractPage(res, normalizeEvent);
}

/** 查询记录详情、证据与核实记录 */
export async function getEventDetail(eventId: string): Promise<VehicleEventDetail> {
  const res = await request.post({
    url: `${VEHICLE_RECOGNITION_API.GET_EVENT_DETAIL}/${eventId}`,
    data: {},
  });
  return normalizeEventDetail(res?.result);
}

/**
 * 查看记录后标记为已核实。
 *
 * 安全要点：
 * - 只传记录标识。核实人、目标状态、核实意见一律由后端决定。
 * - 后端对非「待核实」的记录幂等返回，不重复追加审计。
 * - 失败不抛出，由调用方决定提示方式，避免详情弹窗因标记失败而关闭。
 */
export async function markEventViewed(eventId: string): Promise<{ success: boolean; message: string }> {
  try {
    const res = await request.post({
      url: `${VEHICLE_RECOGNITION_API.MARK_EVENT_VIEWED}/${eventId}`,
    });
    return {
      success: res?.status?.success === true || res?.status?.code === '200',
      message: res?.status?.message ?? '已标记为已核实',
    };
  } catch (error: any) {
    return {
      success: false,
      message: error?.message ?? '标记已核实失败',
    };
  }
}

/** 导出记录：服务端直出 xlsx 二进制流 */
export function exportEvent(data: EventExportRequest) {
  return request.post(
    {
      url: VEHICLE_RECOGNITION_API.EXPORT_EVENT,
      data,
      responseType: 'blob',
    },
    {
      isReturnNativeResponse: true,
      isTransformResponse: false,
    },
  );
}

/* ==================== 算法任务（「重新识别」） ==================== */

/**
 * 提交重新识别：后端只受理并返回 taskId，绝不在请求里同步等推理结果。
 * 失败向上抛出，由弹窗展示可理解错误。
 */
export async function rerunAlgorithm(eventId: string): Promise<RerunAccepted> {
  const res = await request.post({
    url: `${VEHICLE_RECOGNITION_API.RERUN_ALGORITHM}/${encodeURIComponent(eventId)}`,
    data: {},
  });
  const raw = res?.result ?? {};
  return {
    taskId: String(pick(raw, 'taskId', 'task_id') ?? ''),
    executionStatus: pick(raw, 'executionStatus', 'execution_status') ?? 'QUEUED',
    simulated: pick(raw, 'simulated', 'is_simulated') === true,
  } as RerunAccepted;
}

/** 轮询单个算法任务：未进终态时后端会向网关取最新并落库 */
export async function getAlgorithmTask(taskNo: string): Promise<AlgorithmTask> {
  const res = await request.post({
    url: `${VEHICLE_RECOGNITION_API.ALGORITHM_TASK}/${encodeURIComponent(taskNo)}`,
    data: {},
  });
  return normalizeAlgorithmTask(res?.result);
}

/** 事件历史算法任务列表（详情已随 getEventDetail 带回，本方法供显式刷新/联调） */
export async function listAlgorithmTasks(eventId: string): Promise<AlgorithmTask[]> {
  const res = await request.post({
    url: `${VEHICLE_RECOGNITION_API.LIST_ALGORITHM_TASKS}/${encodeURIComponent(eventId)}`,
    data: {},
  });
  const list = Array.isArray(res?.result) ? res.result : [];
  return list.map(normalizeAlgorithmTask);
}
