/**
 * 皮带机下站人监测功能 - API 适配层
 *
 * 职责：
 * 1. 本文件是本功能访问后端的**唯一出口**，
 *    页面组件内不得散落 URL 字符串与原始字段转换。
 * 2. 统一处理后端 ActionResult → 页面模型的字段映射。
 * 3. 统一处理项目分页结构（result.data / result.total）。
 */

import type { EventExportRequest, EventPageRequest, PageResult } from '@/api/model/beltMachineModel';
import type {
  AlgorithmTask,
  BeltMachineEvent,
  BeltMachineEventDetail,
  MonitorPoint,
  RerunAccepted,
  StreamInfo,
} from '@/pages/aiVision/beltMachine/types';
import { request } from '@/utils/request';

/** 接口路径常量 */
export const BELT_MACHINE_API = {
  /** 皮带机监测点位状态列表 */
  LIST_MONITOR_POINT: '/beltMachine/listMonitorPoint',
  /** 获取指定点位绑定摄像头的短时播放信息 */
  GET_STREAM_INFO: '/beltMachine/getStreamInfo',
  /** 首页最近事件 */
  LIST_RECENT_EVENT: '/beltMachine/listRecentEvent',
  /** 历史记录服务端分页查询 */
  LIST_EVENT_BY_CONDITION: '/beltMachine/listEventByCondition',
  /** 查询事件详情、证据和处理记录 */
  GET_EVENT_DETAIL: '/beltMachine/getEventDetail',
  /** 查看事件后标记为已处理 */
  MARK_EVENT_VIEWED: '/beltMachine/markEventViewed',
  /** 导出当前查询条件下的事件 */
  EXPORT_EVENT: '/beltMachine/exportEvent',
  /** 发起重新识别（受理后立即返回任务标识，不等待算法） */
  RERUN_ALGORITHM: '/beltMachine/rerunAlgorithm',
  /** 查询算法任务（「重新识别」轮询接口） */
  GET_ALGORITHM_TASK: '/beltMachine/algorithmTask',
  /** 查询事件的算法任务列表 */
  LIST_ALGORITHM_TASKS: '/beltMachine/listAlgorithmTasks',
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

/** 归一化皮带机点位对象 */
function normalizeMonitorPoint(raw: any): MonitorPoint {
  return {
    pointId: String(pick(raw, 'pointId', 'point_id') ?? ''),
    deviceCode: pick(raw, 'deviceCode', 'device_code') ?? '',
    deviceName: pick(raw, 'deviceName', 'device_name') ?? '',
    areaName: pick(raw, 'areaName', 'area_name') ?? '',
    warehouseId: String(pick(raw, 'warehouseId', 'warehouse_id') ?? ''),
    warehouseName: pick(raw, 'warehouseName', 'warehouse_name') ?? '',
    // cameraId 为空是「未部署」的判定输入之一，必须原样保留 null，不得兜成 ''
    cameraId: pick(raw, 'cameraId', 'camera_id') ?? null,
    cameraCode: pick(raw, 'cameraCode', 'camera_code') ?? null,
    cameraName: pick(raw, 'cameraName', 'camera_name') ?? null,
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
    cameraId: pick(raw, 'cameraId', 'camera_id'),
    cameraCode: pick(raw, 'cameraCode', 'camera_code'),
    cameraName: pick(raw, 'cameraName', 'camera_name'),
    playableUrl: pick(raw, 'playableUrl', 'playable_url'),
    protocol: pick(raw, 'protocol', 'protocol') ?? 'MP4',
    expiresAt: pick(raw, 'expiresAt', 'expires_at'),
    // 缺省视为不可播放，避免无权限时被误判为可播
    canPlay: pick(raw, 'canPlay', 'can_play') === true,
    unavailableReason: pick(raw, 'unavailableReason', 'unavailable_reason'),
  } as StreamInfo;
}

/** 归一化事件列表项 */
function normalizeEvent(raw: any): BeltMachineEvent {
  return {
    eventId: String(pick(raw, 'eventId', 'event_id') ?? ''),
    eventNo: pick(raw, 'eventNo', 'event_no') ?? '',
    occurredAt: pick(raw, 'occurredAt', 'occurred_at') ?? '',
    warehouseId: String(pick(raw, 'warehouseId', 'warehouse_id') ?? ''),
    warehouseName: pick(raw, 'warehouseName', 'warehouse_name') ?? '',
    pointId: String(pick(raw, 'pointId', 'point_id') ?? ''),
    deviceCode: pick(raw, 'deviceCode', 'device_code') ?? '',
    deviceName: pick(raw, 'deviceName', 'device_name') ?? '',
    cameraCode: pick(raw, 'cameraCode', 'camera_code') ?? '',
    conclusionType: pick(raw, 'conclusionType', 'conclusion_type'),
    conclusionTypeName: pick(raw, 'conclusionTypeName', 'conclusion_type_name'),
    // 保留 null 以区分「算法未给出」与 0 人
    personCount: pick(raw, 'personCount', 'person_count'),
    algorithmStatus: pick(raw, 'algorithmStatus', 'algorithm_status'),
    algorithmResult: pick(raw, 'algorithmResult', 'algorithm_result'),
    // 保留 null 以区分「暂无」与 0
    confidence: pick(raw, 'confidence', 'confidence'),
    processStatus: pick(raw, 'processStatus', 'process_status'),
    processedBy: pick(raw, 'processedBy', 'processed_by'),
    processedAt: pick(raw, 'processedAt', 'processed_at'),
    processOpinion: pick(raw, 'processOpinion', 'process_opinion'),
    eventVersion: Number(pick(raw, 'eventVersion', 'event_version') ?? 0),
  } as BeltMachineEvent;
}

/** 归一化算法任务视图（详情 algorithmTasks 与轮询返回共用同一映射） */
function normalizeAlgorithmTask(raw: any): AlgorithmTask {
  return {
    taskId: pick(raw, 'taskId', 'task_id') ?? null,
    taskNo: pick(raw, 'taskNo', 'task_no') ?? '',
    executionStatus: pick(raw, 'executionStatus', 'execution_status') ?? 'QUEUED',
    conclusion: pick(raw, 'conclusion', 'conclusion') ?? null,
    conclusionType: pick(raw, 'conclusionType', 'conclusion_type') ?? null,
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
    // 缺省按最坏情况处理：未标注的任务一律显示「模拟」标识，直到后端明确 FALSE
    simulated: pick(raw, 'simulated', 'is_simulated') !== false,
    errorCode: pick(raw, 'errorCode', 'error_code') ?? null,
    errorMessage: pick(raw, 'errorMessage', 'error_message') ?? null,
  } as AlgorithmTask;
}

/** 归一化事件详情 */
function normalizeEventDetail(raw: any): BeltMachineEventDetail {
  const base = normalizeEvent(raw);
  const evidences = pick(raw, 'evidences', 'evidenceList') ?? [];
  const processRecords = pick(raw, 'processRecords', 'process_record_list') ?? [];
  const algorithmTasksRaw = pick(raw, 'algorithmTasks', 'algorithm_tasks') ?? [];
  const latestTaskRaw = pick(raw, 'latestAlgorithmTask', 'latest_algorithm_task');

  return {
    ...base,
    eventDesc: pick(raw, 'eventDesc', 'event_desc'),
    recoveredAt: pick(raw, 'recoveredAt', 'recovered_at'),
    durationSeconds: pick(raw, 'durationSeconds', 'duration_seconds'),
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
    algorithmTasks: (Array.isArray(algorithmTasksRaw) ? algorithmTasksRaw : []).map((t: any) =>
      normalizeAlgorithmTask(t),
    ),
    latestAlgorithmTask: latestTaskRaw ? normalizeAlgorithmTask(latestTaskRaw) : null,
  } as BeltMachineEventDetail;
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

/** 皮带机点位状态列表；后端负责数据范围过滤 */
export async function listMonitorPoint(): Promise<MonitorPoint[]> {
  const res = await request.post({ url: BELT_MACHINE_API.LIST_MONITOR_POINT, data: {} });
  const list = Array.isArray(res?.result) ? res.result : [];
  return list.map(normalizeMonitorPoint);
}

/** 获取指定点位绑定摄像头的短时播放信息；地址过期后需重新调用 */
export async function getStreamInfo(pointId: string): Promise<StreamInfo> {
  const res = await request.post({
    url: BELT_MACHINE_API.GET_STREAM_INFO,
    data: { pointId },
  });
  return normalizeStreamInfo(res?.result);
}

/** 首页最近事件 */
export async function listRecentEvent(limit = 5): Promise<BeltMachineEvent[]> {
  const res = await request.post({
    url: BELT_MACHINE_API.LIST_RECENT_EVENT,
    data: { limit },
  });
  const list = Array.isArray(res?.result) ? res.result : [];
  return list.map(normalizeEvent);
}

/** 历史记录服务端分页查询；禁止前端伪分页 */
export async function listEventByCondition(params: EventPageRequest): Promise<PageResult<BeltMachineEvent>> {
  const res = await request.post({
    url: BELT_MACHINE_API.LIST_EVENT_BY_CONDITION,
    data: params,
  });
  return extractPage(res, normalizeEvent);
}

/** 查询事件详情、证据与处理记录 */
export async function getEventDetail(eventId: string): Promise<BeltMachineEventDetail> {
  const res = await request.post({
    url: `${BELT_MACHINE_API.GET_EVENT_DETAIL}/${eventId}`,
    data: {},
  });
  return normalizeEventDetail(res?.result);
}

/**
 * 查看事件后标记为已处理。
 *
 * 安全要点：
 * - 只传事件标识。处理人、目标状态、处理意见一律由后端决定。
 * - 后端对非「未核实」的事件幂等返回，不重复追加审计。
 * - 失败不抛出，由调用方决定提示方式，避免详情弹窗因标记失败而关闭。
 */
export async function markEventViewed(eventId: string): Promise<{ success: boolean; message: string }> {
  try {
    const res = await request.post({
      url: `${BELT_MACHINE_API.MARK_EVENT_VIEWED}/${eventId}`,
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

/** 导出事件：服务端直出 xlsx 二进制流 */
export function exportEvent(data: EventExportRequest) {
  return request.post(
    {
      url: BELT_MACHINE_API.EXPORT_EVENT,
      data,
      responseType: 'blob',
    },
    {
      isReturnNativeResponse: true,
      isTransformResponse: false,
    },
  );
}

/* ==================== 算法任务（重新识别链路） ==================== */

/**
 * 发起「重新识别」。
 *
 * 后端只受理不等待：成功即返回 {taskId, executionStatus, simulated}，
 * 进度由调用方轮询 getAlgorithmTask。业务失败（事件不存在 /
 * 未部署点位 CAMERA_NOT_BOUND）由请求层抛出，调用方负责提示。
 */
export async function rerunAlgorithm(eventId: string): Promise<RerunAccepted> {
  const res = await request.post({
    url: `${BELT_MACHINE_API.RERUN_ALGORITHM}/${eventId}`,
    data: {},
  });
  const raw = res?.result ?? {};
  return {
    taskId: String(pick(raw, 'taskId', 'task_id') ?? ''),
    executionStatus: pick(raw, 'executionStatus', 'execution_status') ?? 'QUEUED',
    simulated: pick(raw, 'simulated', 'is_simulated') !== false,
  } as RerunAccepted;
}

/** 查询算法任务最新状态（轮询接口，与详情内 algorithmTasks 元素同形） */
export async function getAlgorithmTask(taskId: string): Promise<AlgorithmTask> {
  const res = await request.post({
    url: `${BELT_MACHINE_API.GET_ALGORITHM_TASK}/${taskId}`,
    data: {},
  });
  return normalizeAlgorithmTask(res?.result);
}

/** 查询事件的算法任务列表（详情弹窗首屏已随 getEventDetail 返回，本方法供独立刷新） */
export async function listAlgorithmTasks(eventId: string): Promise<AlgorithmTask[]> {
  const res = await request.post({
    url: `${BELT_MACHINE_API.LIST_ALGORITHM_TASKS}/${eventId}`,
    data: {},
  });
  const list = Array.isArray(res?.result) ? res.result : [];
  return list.map((t: any) => normalizeAlgorithmTask(t));
}
