/**
 * 粮面虫害智能识别功能 - API 适配层
 *
 * 职责：
 * 1. 本文件是本功能访问后端的**唯一出口**，
 *    页面组件内不得散落 URL 字符串与原始字段转换。
 * 2. 统一处理后端 ActionResult → 页面模型的字段映射。
 * 3. 统一处理项目分页结构（result.data / result.total）。
 */

import type { EventExportRequest, EventPageRequest, PageResult } from '@/api/model/pestDetectionModel';
import type {
  AlgorithmRerunResult,
  MonitorPoint,
  PestAlgorithmTask,
  PestEvent,
  PestEventDetail,
  StreamInfo,
} from '@/pages/aiVision/pestDetection/types';
import { request } from '@/utils/request';

/** 接口路径常量 */
export const PEST_DETECTION_API = {
  /** 粮面点位状态列表 */
  LIST_MONITOR_POINT: '/pestDetection/listMonitorPoint',
  /** 获取指定粮面点位绑定摄像头的短时播放信息 */
  GET_STREAM_INFO: '/pestDetection/getStreamInfo',
  /** 首页最近检测记录 */
  LIST_RECENT_EVENT: '/pestDetection/listRecentEvent',
  /** 历史记录服务端分页查询 */
  LIST_EVENT_BY_CONDITION: '/pestDetection/listEventByCondition',
  /** 查询记录详情、证据和处理记录 */
  GET_EVENT_DETAIL: '/pestDetection/getEventDetail',
  /** 查看记录后标记为已处理 */
  MARK_EVENT_VIEWED: '/pestDetection/markEventViewed',
  /** 导出当前查询条件下的记录 */
  EXPORT_EVENT: '/pestDetection/exportEvent',
  /** 提交「重新识别」（立即返回 taskId，结果轮询 algorithmTask） */
  RERUN_ALGORITHM: '/pestDetection/rerunAlgorithm',
  /** 轮询算法任务状态 */
  ALGORITHM_TASK: '/pestDetection/algorithmTask',
  /** 事件历史算法任务列表 */
  LIST_ALGORITHM_TASKS: '/pestDetection/listAlgorithmTasks',
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

/** 归一化粮面点位对象 */
function normalizeMonitorPoint(raw: any): MonitorPoint {
  return {
    pointId: String(pick(raw, 'pointId', 'point_id') ?? ''),
    pointName: pick(raw, 'pointName', 'point_name') ?? '',
    warehouseId: String(pick(raw, 'warehouseId', 'warehouse_id') ?? ''),
    warehouseName: pick(raw, 'warehouseName', 'warehouse_name') ?? '',
    grainType: pick(raw, 'grainType', 'grain_type') ?? '',
    grainTonnage: pick(raw, 'grainTonnage', 'grain_tonnage') ?? 0,
    cameraCode: pick(raw, 'cameraCode', 'camera_code') ?? '',
    platformCameraId: pick(raw, 'platformCameraId', 'platform_camera_id'),
    deviceStatus: pick(raw, 'deviceStatus', 'device_status') ?? 'UNKNOWN',
    alarmStatus: pick(raw, 'alarmStatus', 'alarm_status') ?? 'NORMAL',
    lastHeartbeatAt: pick(raw, 'lastHeartbeatAt', 'last_heartbeat_at'),
    latestEventAt: pick(raw, 'latestEventAt', 'latest_event_at'),
    sortNo: Number(pick(raw, 'sortNo', 'sort_no') ?? 0),
  } as MonitorPoint;
}

/** 归一化播放信息 */
function normalizeStreamInfo(raw: any): StreamInfo {
  return {
    pointId: String(pick(raw, 'pointId', 'point_id') ?? ''),
    cameraCode: pick(raw, 'cameraCode', 'camera_code'),
    platformCameraId: pick(raw, 'platformCameraId', 'platform_camera_id'),
    playableUrl: pick(raw, 'playableUrl', 'playable_url'),
    protocol: pick(raw, 'protocol', 'protocol') ?? 'MP4',
    expiresAt: pick(raw, 'expiresAt', 'expires_at'),
    // 缺省视为不可播放，避免无权限时被误判为可播
    canPlay: pick(raw, 'canPlay', 'can_play') === true,
    unavailableReason: pick(raw, 'unavailableReason', 'unavailable_reason'),
  } as StreamInfo;
}

/** 归一化检测记录列表项 */
function normalizeEvent(raw: any): PestEvent {
  return {
    eventId: String(pick(raw, 'eventId', 'event_id') ?? ''),
    eventNo: pick(raw, 'eventNo', 'event_no') ?? '',
    occurredAt: pick(raw, 'occurredAt', 'occurred_at') ?? '',
    warehouseId: String(pick(raw, 'warehouseId', 'warehouse_id') ?? ''),
    warehouseName: pick(raw, 'warehouseName', 'warehouse_name') ?? '',
    pointId: String(pick(raw, 'pointId', 'point_id') ?? ''),
    cameraCode: pick(raw, 'cameraCode', 'camera_code') ?? '',
    samplePosition: pick(raw, 'samplePosition', 'sample_position') ?? '',
    grainType: pick(raw, 'grainType', 'grain_type'),
    conclusionType: pick(raw, 'conclusionType', 'conclusion_type'),
    conclusionTypeName: pick(raw, 'conclusionTypeName', 'conclusion_type_name'),
    insectSpecies: pick(raw, 'insectSpecies', 'insect_species'),
    // 密度/阈值保留原始值（后端为 NUMERIC，可能是字符串），null 不伪造为 0
    density: pick(raw, 'density', 'density'),
    densityThreshold: pick(raw, 'densityThreshold', 'density_threshold'),
    algorithmStatus: pick(raw, 'algorithmStatus', 'algorithm_status'),
    algorithmResult: pick(raw, 'algorithmResult', 'algorithm_result'),
    // 保留 null 以区分「暂无」与 0
    confidence: pick(raw, 'confidence', 'confidence'),
    processStatus: pick(raw, 'processStatus', 'process_status'),
    processedBy: pick(raw, 'processedBy', 'processed_by'),
    processedAt: pick(raw, 'processedAt', 'processed_at'),
    processOpinion: pick(raw, 'processOpinion', 'process_opinion'),
    eventVersion: Number(pick(raw, 'eventVersion', 'event_version') ?? 0),
  } as PestEvent;
}

/** 归一化算法任务行（字段映射照既有 pick 风格，下划线/驼峰都兼容） */
function normalizeAlgorithmTask(raw: any): PestAlgorithmTask {
  return {
    taskId: String(pick(raw, 'taskId', 'task_id') ?? pick(raw, 'taskNo', 'task_no') ?? ''),
    id: pick(raw, 'id', 'id'),
    taskNo: pick(raw, 'taskNo', 'task_no') ?? '',
    eventId: pick(raw, 'eventId', 'event_id'),
    pointId: pick(raw, 'pointId', 'point_id'),
    cameraCode: pick(raw, 'cameraCode', 'camera_code'),
    taskType: pick(raw, 'taskType', 'task_type'),
    executionStatus: pick(raw, 'executionStatus', 'execution_status') ?? 'QUEUED',
    conclusion: pick(raw, 'conclusion', 'conclusion'),
    conclusionType: pick(raw, 'conclusionType', 'conclusion_type'),
    // 置信度保留 null：任务表据此显示「暂无」而不是 0%
    confidence: pick(raw, 'confidence', 'confidence'),
    confidenceThreshold: pick(raw, 'confidenceThreshold', 'confidence_threshold'),
    modelName: pick(raw, 'modelName', 'model_name'),
    modelVersion: pick(raw, 'modelVersion', 'model_version'),
    algorithmVersion: pick(raw, 'algorithmVersion', 'algorithm_version'),
    configVersion: pick(raw, 'configVersion', 'config_version'),
    startedAt: pick(raw, 'startedAt', 'started_at'),
    finishedAt: pick(raw, 'finishedAt', 'finished_at'),
    inferenceDurationMs: pick(raw, 'inferenceDurationMs', 'inference_duration_ms'),
    detectedCount: pick(raw, 'detectedCount', 'detected_count'),
    detections: pick(raw, 'detections', 'detections'),
    mediaRefs: pick(raw, 'mediaRefs', 'media_refs'),
    simulated: pick(raw, 'simulated', 'is_simulated') ?? pick(raw, 'isSimulated', 'simulated'),
    errorCode: pick(raw, 'errorCode', 'error_code'),
    errorMessage: pick(raw, 'errorMessage', 'error_message'),
    traceId: pick(raw, 'traceId', 'trace_id'),
  } as PestAlgorithmTask;
}

/** 归一化记录详情 */
function normalizeEventDetail(raw: any): PestEventDetail {
  const base = normalizeEvent(raw);
  const evidences = pick(raw, 'evidences', 'evidenceList') ?? [];
  const processRecords = pick(raw, 'processRecords', 'process_record_list') ?? [];
  const algorithmTasks = pick(raw, 'algorithmTasks', 'algorithm_tasks') ?? [];

  return {
    ...base,
    eventDesc: pick(raw, 'eventDesc', 'event_desc'),
    recoveredAt: pick(raw, 'recoveredAt', 'recovered_at'),
    durationSeconds: pick(raw, 'durationSeconds', 'duration_seconds'),
    isSimulated: pick(raw, 'isSimulated', 'is_simulated'),
    densityExceeded: pick(raw, 'densityExceeded', 'density_exceeded'),
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
    // 算法任务历史随详情一次带回，弹窗任务表不再单独发请求
    algorithmTasks: (Array.isArray(algorithmTasks) ? algorithmTasks : []).map(normalizeAlgorithmTask),
    latestAlgorithmTask: (() => {
      const latest = pick(raw, 'latestAlgorithmTask', 'latest_algorithm_task');
      return latest ? normalizeAlgorithmTask(latest) : null;
    })(),
  } as PestEventDetail;
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

/** 粮面点位状态列表；后端负责数据范围过滤 */
export async function listMonitorPoint(): Promise<MonitorPoint[]> {
  const res = await request.post({ url: PEST_DETECTION_API.LIST_MONITOR_POINT, data: {} });
  const list = Array.isArray(res?.result) ? res.result : [];
  return list.map(normalizeMonitorPoint);
}

/** 获取指定粮面点位绑定摄像头的短时播放信息；地址过期后需重新调用 */
export async function getStreamInfo(pointId: string): Promise<StreamInfo> {
  const res = await request.post({
    url: PEST_DETECTION_API.GET_STREAM_INFO,
    data: { pointId },
  });
  return normalizeStreamInfo(res?.result);
}

/** 首页最近检测记录 */
export async function listRecentEvent(limit = 5): Promise<PestEvent[]> {
  const res = await request.post({
    url: PEST_DETECTION_API.LIST_RECENT_EVENT,
    data: { limit },
  });
  const list = Array.isArray(res?.result) ? res.result : [];
  return list.map(normalizeEvent);
}

/** 历史记录服务端分页查询；禁止前端伪分页 */
export async function listEventByCondition(params: EventPageRequest): Promise<PageResult<PestEvent>> {
  const res = await request.post({
    url: PEST_DETECTION_API.LIST_EVENT_BY_CONDITION,
    data: params,
  });
  return extractPage(res, normalizeEvent);
}

/** 查询记录详情、证据与处理记录 */
export async function getEventDetail(eventId: string): Promise<PestEventDetail> {
  const res = await request.post({
    url: `${PEST_DETECTION_API.GET_EVENT_DETAIL}/${eventId}`,
    data: {},
  });
  return normalizeEventDetail(res?.result);
}

/**
 * 查看记录后标记为已处理。
 *
 * 安全要点：
 * - 只传记录标识。处理人、目标状态、处理意见一律由后端决定。
 * - 后端对非「未核实」的记录幂等返回，不重复追加审计。
 * - 失败不抛出，由调用方决定提示方式，避免详情弹窗因标记失败而关闭。
 */
export async function markEventViewed(eventId: string): Promise<{ success: boolean; message: string }> {
  try {
    const res = await request.post({
      url: `${PEST_DETECTION_API.MARK_EVENT_VIEWED}/${eventId}`,
    });
    return {
      success: res?.status?.success === true || res?.status?.code === '200',
      message: res?.status?.message ?? '已标记为已处理',
    };
  } catch (error: any) {
    return {
      success: false,
      message: error?.message ?? '标记已处理失败',
    };
  }
}

/** 导出记录：服务端直出 xlsx 二进制流 */
export function exportEvent(data: EventExportRequest) {
  return request.post(
    {
      url: PEST_DETECTION_API.EXPORT_EVENT,
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
export async function rerunAlgorithm(eventId: string): Promise<AlgorithmRerunResult> {
  const res = await request.post({
    url: `${PEST_DETECTION_API.RERUN_ALGORITHM}/${eventId}`,
    data: {},
  });
  const raw = res?.result ?? {};
  return {
    taskId: String(pick(raw, 'taskId', 'task_id') ?? ''),
    executionStatus: pick(raw, 'executionStatus', 'execution_status') ?? 'QUEUED',
    simulated: pick(raw, 'simulated', 'is_simulated') === true,
  } as AlgorithmRerunResult;
}

/** 轮询单个算法任务：未进终态时后端会向网关取最新并落库 */
export async function getAlgorithmTask(taskId: string): Promise<PestAlgorithmTask> {
  const res = await request.post({
    url: `${PEST_DETECTION_API.ALGORITHM_TASK}/${encodeURIComponent(taskId)}`,
    data: {},
  });
  return normalizeAlgorithmTask(res?.result);
}

/** 事件历史算法任务列表（详情弹窗数据已随 getEventDetail 带回，本方法供显式刷新/联调） */
export async function listAlgorithmTasks(eventId: string): Promise<PestAlgorithmTask[]> {
  const res = await request.post({
    url: `${PEST_DETECTION_API.LIST_ALGORITHM_TASKS}/${eventId}`,
    data: {},
  });
  const list = Array.isArray(res?.result) ? res.result : [];
  return list.map(normalizeAlgorithmTask);
}
