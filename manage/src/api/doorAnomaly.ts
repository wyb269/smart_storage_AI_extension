/**
 * 仓门异动功能 - API 适配层
 *
 * 基线依据：AI_HANDOFF.md 第 21 节「前后端接口基线」
 *
 * 职责（第21.3节）：
 * 1. 本文件是本功能访问后端的**唯一出口**，
 *    页面组件内不得散落 URL 字符串与原始字段转换。
 * 2. 统一处理后端 ActionResult → 页面模型的字段映射。
 * 3. 统一处理项目分页结构（result.data / result.total）。
 *
 * 接口名称严格沿用文档第21.1节，未做任何改名。
 */

import type { EventExportRequest, EventPageRequest, PageResult } from '@/api/model/doorAnomalyModel';
import type {
  AlgorithmTask,
  AnomalyEvent,
  DoorStatus,
  EventDetail,
  RerunAccepted,
  StreamInfo,
} from '@/pages/aiVision/doorAnomaly/types';
import { request } from '@/utils/request';

/**
 * 接口路径常量。
 * 与 AI_HANDOFF.md 第21.1节逐条对应，改动需同步文档。
 */
export const DOOR_ANOMALY_API = {
  /** 仓门状态列表 */
  LIST_DOOR_STATUS: '/doorAnomaly/listDoorStatus',
  /** 获取指定仓门的短时播放信息 */
  GET_STREAM_INFO: '/doorAnomaly/getStreamInfo',
  /** 首页最近事件 */
  LIST_RECENT_EVENT: '/doorAnomaly/listRecentEvent',
  /** 历史记录服务端分页查询 */
  LIST_EVENT_BY_CONDITION: '/doorAnomaly/listEventByCondition',
  /** 查询事件详情、证据和处理记录 */
  GET_EVENT_DETAIL: '/doorAnomaly/getEventDetail',
  /** 查看事件后标记为已处理（取代人工处理入口） */
  MARK_EVENT_VIEWED: '/doorAnomaly/markEventViewed',
  /** 导出当前查询条件下的事件 */
  EXPORT_EVENT: '/doorAnomaly/exportEvent',
  /** 提交「重新识别」（立即返回 taskId，结果轮询 algorithmTask） */
  RERUN_ALGORITHM: '/doorAnomaly/rerunAlgorithm',
  /** 轮询算法任务状态 */
  GET_ALGORITHM_TASK: '/doorAnomaly/algorithmTask',
  /** 事件历史算法任务列表 */
  LIST_ALGORITHM_TASKS: '/doorAnomaly/listAlgorithmTasks',
} as const;

/* ==================== 字段转换 ==================== */

/**
 * 后端可能返回下划线或驼峰，两种都兼容。
 * 第20节要求页面只依赖统一契约，此处做集中归一化。
 *
 * 说明：泛型默认 any，返回值可能为 undefined，
 * 因此各字段统一在赋值处使用 `?? 默认值` 兜底。
 */
function pick(source: any, camelKey: string, snakeKey: string): any {
  if (!source) return undefined;
  if (source[camelKey] !== undefined && source[camelKey] !== null) return source[camelKey];
  if (source[snakeKey] !== undefined && source[snakeKey] !== null) return source[snakeKey];
  return undefined;
}

/** 归一化仓门状态对象（第20.1节） */
function normalizeDoorStatus(raw: any): DoorStatus {
  return {
    doorId: String(pick(raw, 'doorId', 'door_id') ?? ''),
    doorName: pick(raw, 'doorName', 'door_name') ?? '',
    warehouseId: String(pick(raw, 'warehouseId', 'warehouse_id') ?? ''),
    warehouseName: pick(raw, 'warehouseName', 'warehouse_name') ?? '',
    cameraId: pick(raw, 'cameraId', 'camera_id'),
    cameraCode: pick(raw, 'cameraCode', 'camera_code'),
    deviceStatus: pick(raw, 'deviceStatus', 'device_status') ?? 'UNKNOWN',
    doorPhysicalStatus: pick(raw, 'doorPhysicalStatus', 'door_physical_status') ?? 'UNKNOWN',
    alarmStatus: pick(raw, 'alarmStatus', 'alarm_status') ?? 'NORMAL',
    lastHeartbeatAt: pick(raw, 'lastHeartbeatAt', 'last_heartbeat_at'),
    latestEventAt: pick(raw, 'latestEventAt', 'latest_event_at'),
    sortNo: Number(pick(raw, 'sortNo', 'sort_no') ?? 0),
  } as DoorStatus;
}

/** 归一化视频播放信息（第20.2节） */
function normalizeStreamInfo(raw: any): StreamInfo {
  return {
    doorId: String(pick(raw, 'doorId', 'door_id') ?? ''),
    cameraId: pick(raw, 'cameraId', 'camera_id'),
    playableUrl: pick(raw, 'playableUrl', 'playable_url'),
    protocol: pick(raw, 'protocol', 'protocol') ?? 'MP4',
    expiresAt: pick(raw, 'expiresAt', 'expires_at'),
    // 缺省视为不可播放，避免无权限时被误判为可播
    canPlay: pick(raw, 'canPlay', 'can_play') === true,
    unavailableReason: pick(raw, 'unavailableReason', 'unavailable_reason'),
  } as StreamInfo;
}

/** 归一化事件列表项（第20.3节） */
function normalizeEvent(raw: any): AnomalyEvent {
  return {
    eventId: String(pick(raw, 'eventId', 'event_id') ?? ''),
    eventNo: pick(raw, 'eventNo', 'event_no') ?? '',
    occurredAt: pick(raw, 'occurredAt', 'occurred_at') ?? '',
    warehouseId: String(pick(raw, 'warehouseId', 'warehouse_id') ?? ''),
    warehouseName: pick(raw, 'warehouseName', 'warehouse_name') ?? '',
    doorId: String(pick(raw, 'doorId', 'door_id') ?? ''),
    doorName: pick(raw, 'doorName', 'door_name') ?? '',
    cameraId: pick(raw, 'cameraId', 'camera_id'),
    cameraCode: pick(raw, 'cameraCode', 'camera_code'),
    anomalyType: pick(raw, 'anomalyType', 'anomaly_type'),
    anomalyTypeName: pick(raw, 'anomalyTypeName', 'anomaly_type_name'),
    algorithmStatus: pick(raw, 'algorithmStatus', 'algorithm_status'),
    algorithmResult: pick(raw, 'algorithmResult', 'algorithm_result'),
    // 保留 null 以区分「暂无」与 0（第17.3节）
    confidence: pick(raw, 'confidence', 'confidence'),
    processStatus: pick(raw, 'processStatus', 'process_status'),
    processedBy: pick(raw, 'processedBy', 'processed_by'),
    processedAt: pick(raw, 'processedAt', 'processed_at'),
    processOpinion: pick(raw, 'processOpinion', 'process_opinion'),
    eventVersion: Number(pick(raw, 'eventVersion', 'event_version') ?? 0),
  } as AnomalyEvent;
}

/** 归一化算法任务视图（详情 algorithmTasks 与轮询返回共用同一映射） */
function normalizeAlgorithmTask(raw: any): AlgorithmTask {
  return {
    taskId: pick(raw, 'taskId', 'task_id') ?? null,
    taskNo: pick(raw, 'taskNo', 'task_no') ?? '',
    executionStatus: pick(raw, 'executionStatus', 'execution_status') ?? 'QUEUED',
    conclusion: pick(raw, 'conclusion', 'conclusion') ?? null,
    // 仓门异动的「结论类型」在后端算法任务 DTO 里是 anomalyType
    anomalyType: pick(raw, 'anomalyType', 'anomaly_type') ?? null,
    // 保留 null：进行中/失败任务没有置信度，页面显示「暂无」而不是 0%
    confidence: pick(raw, 'confidence', 'confidence') ?? null,
    modelName: pick(raw, 'modelName', 'model_name') ?? null,
    modelVersion: pick(raw, 'modelVersion', 'model_version') ?? null,
    algorithmVersion: pick(raw, 'algorithmVersion', 'algorithm_version') ?? null,
    configVersion: pick(raw, 'configVersion', 'config_version') ?? null,
    inferenceDurationMs: pick(raw, 'inferenceDurationMs', 'inference_duration_ms') ?? null,
    startedAt: pick(raw, 'startedAt', 'started_at') ?? null,
    finishedAt: pick(raw, 'finishedAt', 'finished_at') ?? null,
    detectedCount: pick(raw, 'detectedCount', 'detected_count') ?? null,
    // 缺省按最坏情况处理：未标注的任务一律显示「模拟」标识，直到后端明确 false
    simulated: pick(raw, 'simulated', 'is_simulated') !== false,
    errorCode: pick(raw, 'errorCode', 'error_code') ?? null,
    errorMessage: pick(raw, 'errorMessage', 'error_message') ?? null,
  } as AlgorithmTask;
}

/** 归一化事件详情（第20.4节） */
function normalizeEventDetail(raw: any): EventDetail {
  const base = normalizeEvent(raw);
  const evidences = pick(raw, 'evidences', 'evidenceList') ?? [];
  const processRecords = pick(raw, 'processRecords', 'process_record_list') ?? [];
  const algorithmTasks = pick(raw, 'algorithmTasks', 'algorithm_tasks') ?? [];
  const latestTask = pick(raw, 'latestAlgorithmTask', 'latest_algorithm_task');

  return {
    ...base,
    eventDesc: pick(raw, 'eventDesc', 'event_desc'),
    algorithmTaskNo: pick(raw, 'algorithmTaskNo', 'algorithm_task_no'),
    modelName: pick(raw, 'modelName', 'model_name'),
    modelVersion: pick(raw, 'modelVersion', 'model_version'),
    algorithmVersion: pick(raw, 'algorithmVersion', 'algorithm_version'),
    configVersion: pick(raw, 'configVersion', 'config_version'),
    inferenceStartedAt: pick(raw, 'inferenceStartedAt', 'inference_started_at'),
    inferenceFinishedAt: pick(raw, 'inferenceFinishedAt', 'inference_finished_at'),
    inferenceDurationMs: pick(raw, 'inferenceDurationMs', 'inference_duration_ms'),
    isSimulated: pick(raw, 'isSimulated', 'is_simulated'),
    evidences: evidences.map((e: any) => ({
      evidenceId: String(pick(e, 'evidenceId', 'evidence_id') ?? ''),
      evidenceType: pick(e, 'evidenceType', 'evidence_type'),
      originalUrl: pick(e, 'originalUrl', 'original_url'),
      // 标注图与原图分离（第17.4节不得互相覆盖）
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
    latestAlgorithmTask: latestTask ? normalizeAlgorithmTask(latestTask) : null,
  } as EventDetail;
}

/** 提取项目统一分页结构（result.data / result.total） */
function extractPage<T>(res: any, mapper: (raw: any) => T): PageResult<T> {
  const result = res?.result ?? {};
  const list = Array.isArray(result.data) ? result.data : [];
  return {
    data: list.map(mapper),
    total: Number(result.total ?? 0),
  };
}

/* ==================== 接口实现 ==================== */

/**
 * 仓门状态列表。
 * 返回当前用户可见的仓门；后端负责数据范围过滤。
 */
export async function listDoorStatus(): Promise<DoorStatus[]> {
  const res = await request.post({ url: DOOR_ANOMALY_API.LIST_DOOR_STATUS, data: {} });
  const list = Array.isArray(res?.result) ? res.result : [];
  return list.map(normalizeDoorStatus);
}

/**
 * 获取指定仓门的短时播放信息。
 * 返回的 playableUrl 为短时地址，过期后需重新调用（第26.2节）。
 */
export async function getStreamInfo(doorId: string): Promise<StreamInfo> {
  const res = await request.post({
    url: DOOR_ANOMALY_API.GET_STREAM_INFO,
    data: { doorId },
  });
  return normalizeStreamInfo(res?.result);
}

/**
 * 首页最近事件。
 * limit 默认由页面按第16.5节传 5。
 */
export async function listRecentEvent(limit = 5): Promise<AnomalyEvent[]> {
  const res = await request.post({
    url: DOOR_ANOMALY_API.LIST_RECENT_EVENT,
    data: { limit },
  });
  const list = Array.isArray(res?.result) ? res.result : [];
  return list.map(normalizeEvent);
}

/**
 * 历史记录服务端分页查询。
 * 第18.4节明确禁止前端伪分页，因此此处不做本地切片。
 */
export async function listEventByCondition(params: EventPageRequest): Promise<PageResult<AnomalyEvent>> {
  const res = await request.post({
    url: DOOR_ANOMALY_API.LIST_EVENT_BY_CONDITION,
    data: params,
  });
  return extractPage(res, normalizeEvent);
}

/**
 * 查询事件详情、证据与处理记录。
 */
export async function getEventDetail(eventId: string): Promise<EventDetail> {
  const res = await request.post({
    url: `${DOOR_ANOMALY_API.GET_EVENT_DETAIL}/${eventId}`,
    data: {},
  });
  return normalizeEventDetail(res?.result);
}

/**
 * 查看事件后标记为已处理。
 *
 * 产品语义（2026-10-21 变更）：已取消人工处理入口，
 * 打开详情即视为已处理，状态只有「未处理 / 已处理」两种。
 *
 * 安全要点：
 * - 只传事件标识。处理人、目标状态、处理意见一律由后端决定，
 *   不接受前端传入（第20.6、29.4节）。
 * - 后端对非「未处理」的事件幂等返回，不重复追加审计。
 * - 失败不抛出，由调用方决定提示方式，避免详情弹窗因标记失败而关闭。
 */
export async function markEventViewed(eventId: string): Promise<{ success: boolean; message: string }> {
  try {
    const res = await request.post({
      url: `${DOOR_ANOMALY_API.MARK_EVENT_VIEWED}/${eventId}`,
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

/**
 * 导出事件。
 *
 * 已确认方案 A：与项目既有 ExcelExportController 一致，
 * 服务端直出 xlsx 二进制流，因此关闭响应转换并保留原生响应。
 */
export function exportEvent(data: EventExportRequest) {
  return request.post(
    {
      url: DOOR_ANOMALY_API.EXPORT_EVENT,
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
    url: `${DOOR_ANOMALY_API.RERUN_ALGORITHM}/${eventId}`,
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
    url: `${DOOR_ANOMALY_API.GET_ALGORITHM_TASK}/${encodeURIComponent(taskNo)}`,
    data: {},
  });
  return normalizeAlgorithmTask(res?.result);
}

/** 事件历史算法任务列表（详情弹窗数据已随 getEventDetail 带回，本方法供显式刷新/联调） */
export async function listAlgorithmTasks(eventId: string): Promise<AlgorithmTask[]> {
  const res = await request.post({
    url: `${DOOR_ANOMALY_API.LIST_ALGORITHM_TASKS}/${eventId}`,
    data: {},
  });
  const list = Array.isArray(res?.result) ? res.result : [];
  return list.map(normalizeAlgorithmTask);
}
