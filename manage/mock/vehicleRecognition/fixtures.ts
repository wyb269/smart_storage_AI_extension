/**
 * 车辆智能识别 Mock - 共享假数据生成器
 *
 * 形状来源（三者必须一致，否则切到真实后端会静默出错）：
 * - `src/api/vehicleRecognition.ts` 的 `normalize*` 函数（页面实际消费的字段）
 * - `src/api/model/vehicleRecognitionModel.ts` 的请求/响应类型
 * - 真实后端 DTO 字段名（驼峰、时间为 `yyyy-MM-dd HH:mm:ss` 字符串）
 *
 * ⚠️ 时间戳的生成方式（本文件最重要的一条约定）
 * 存储层只保存**相对当前时刻的偏移量**（daysAgo / timeOfDay / heartbeatAgoSec），
 * 所有对外时间字符串都在每次调用 `materialize*` / `create*` 时基于 `Date.now()` 现算。
 *
 * 原因有二：
 * 1. 左侧「今日识别车辆」按识别时间落在数据库当天过滤；若固化时间戳，
 *    进程跑久/跨天后今日列表会为空。因此今日记录用
 *    startOfToday + timeOfDay*(now-startOfToday) 保证永远落在 [今天0点, now]。
 * 2. 点位在线判定用 90 分钟心跳阈值（shared/constants.HEARTBEAT_TIMEOUT_MS）；
 *    心跳若固定，dev server 跑久后在线点位会被误判离线。
 *
 * 安全：图片/视频地址全部是公开占位与测试资源，不含真实监控画面、
 * 摄像头账号、RTSP 凭据或内网地址。车牌为演示占位（黑A00000 等），非真实车牌。
 */

/* ---------- 时间工具 ---------- */

const SECOND = 1_000;
const MINUTE = 60 * SECOND;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

const pad2 = (n: number) => String(n).padStart(2, '0');

/**
 * 后端时间格式：`yyyy-MM-dd HH:mm:ss`（空格分隔，不是 ISO，也不是时间戳数字）。
 */
export function formatDateTime(ms: number): string {
  const d = new Date(ms);
  return (
    `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ` +
    `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
  );
}

/** 事件编号里的日期段：`yyyyMMdd` */
function formatDateCompact(ms: number): string {
  const d = new Date(ms);
  return `${d.getFullYear()}${pad2(d.getMonth() + 1)}${pad2(d.getDate())}`;
}

/**
 * 后端时间字符串 → 毫秒（排序用）。
 * 不依赖 `new Date('yyyy-MM-dd HH:mm:ss')` 的引擎差异，手工按本地时区拆解。
 */
function toMs(value: unknown): number {
  if (!value) return -1;
  const m = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})/.exec(String(value));
  if (!m) {
    const n = new Date(String(value)).getTime();
    return Number.isNaN(n) ? -1 : n;
  }
  return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]), Number(m[4]), Number(m[5]), Number(m[6])).getTime();
}

/** 今天 0 点的毫秒 */
function startOfTodayMs(now: number): number {
  const d = new Date(now);
  return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
}

/* ---------- 识别点位 ---------- */

interface PointDef {
  pointId: string;
  pointCode: string;
  pointName: string;
  cameraCode: string;
  cameraName: string;
  platformCameraId: string;
  warehouseId: string;
  warehouseName: string;
  deviceStatus: 'ONLINE' | 'OFFLINE' | 'UNKNOWN';
  alarmStatus: 'NORMAL' | 'ACTIVE' | 'RECOVERED';
  /** 心跳距今秒数；null 表示无心跳（UNKNOWN） */
  heartbeatAgoSec: number | null;
}

const WAREHOUSES = [
  { id: '9001', name: '一号粮库' },
  { id: '9002', name: '二号粮库' },
];

const POINT_DEFS: PointDef[] = [
  {
    pointId: '501',
    pointCode: 'GATE-E-IN',
    pointName: '东大门入口',
    cameraCode: 'CAM-003',
    cameraName: '东大门入口立柱',
    platformCameraId: 'C003',
    warehouseId: WAREHOUSES[0].id,
    warehouseName: WAREHOUSES[0].name,
    deviceStatus: 'ONLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 2 * MINUTE,
  },
  {
    pointId: '502',
    pointCode: 'GATE-E-OUT',
    pointName: '东大门出口',
    cameraCode: 'CAM-004',
    cameraName: '东大门出口横杆',
    platformCameraId: 'C004',
    warehouseId: WAREHOUSES[0].id,
    warehouseName: WAREHOUSES[0].name,
    deviceStatus: 'ONLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 4 * MINUTE,
  },
  {
    pointId: '503',
    pointCode: 'GATE-W-IN',
    pointName: '西大门入口',
    cameraCode: 'CAM-005',
    cameraName: '西大门入口灯杆',
    platformCameraId: 'C005',
    warehouseId: WAREHOUSES[1].id,
    warehouseName: WAREHOUSES[1].name,
    deviceStatus: 'ONLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 6 * MINUTE,
  },
  {
    pointId: '504',
    pointCode: 'GATE-W-OUT',
    pointName: '西大门出口',
    cameraCode: 'CAM-006',
    cameraName: '西大门出口雨棚',
    platformCameraId: 'C006',
    warehouseId: WAREHOUSES[1].id,
    warehouseName: WAREHOUSES[1].name,
    // 明确离线：取流返回 CAMERA_OFFLINE
    deviceStatus: 'OFFLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 200 * MINUTE,
  },
  {
    pointId: '505',
    pointCode: 'GATE-S-IN',
    pointName: '南大门入口',
    cameraCode: 'CAM-007',
    cameraName: '南大门入口门柱',
    platformCameraId: 'C007',
    warehouseId: WAREHOUSES[0].id,
    warehouseName: WAREHOUSES[0].name,
    deviceStatus: 'ONLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 3 * MINUTE,
  },
  {
    pointId: '506',
    pointCode: 'GATE-N-OUT',
    pointName: '北大门出口',
    cameraCode: 'CAM-008',
    cameraName: '北大门出口临时杆',
    platformCameraId: 'C008',
    warehouseId: WAREHOUSES[1].id,
    warehouseName: WAREHOUSES[1].name,
    // UNKNOWN：设备状态无法判定，取流返回 NO_ADDRESS
    deviceStatus: 'UNKNOWN',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: null,
  },
];

function pointOf(index: number): PointDef {
  return POINT_DEFS[index % POINT_DEFS.length];
}

function pointById(id: string): PointDef | undefined {
  return POINT_DEFS.find((p) => p.pointId === id);
}

function directionOfPoint(point: PointDef): 'IN' | 'OUT' {
  return point.pointName.includes('出口') ? 'OUT' : 'IN';
}

function directionLabel(direction: 'IN' | 'OUT'): string {
  return direction === 'IN' ? '入库' : '出库';
}

/* ---------- 识别记录存储（只存相对偏移与可变状态） ---------- */

const PLATES = ['黑A00000', '黑A00001', '黑A12345', '黑B67890'];
const VEHICLE_TYPES = ['柴油货车', '空车', '客车车型', '新能源轿车', '未识别车型'];
const COLORS = ['白色', '黑色', '银色'];

interface EventSeed {
  eventId: string;
  plateNo: string;
  vehicleType: string;
  vehicleColor: string;
  direction: 'IN' | 'OUT';
  pointIndex: number;
  /** 0 = 今天；>=1 表示历史天数 */
  daysAgo: number;
  /** 当天内的时间比例 0~1 */
  timeOfDay: number;
  confidence: number | null;
  /** 算法执行状态；「重新识别」成功后回写 */
  algorithmStatus: string;
  verifyStatus: 'PENDING' | 'VERIFIED' | 'REJECTED';
  eventVersion: number;
  processedBy: string | null;
  /** 本次运行中被「查看即标记」写下的绝对毫秒 */
  processedAtAbs: number | null;
  /** 历史种子里已核实记录的处理时间距今毫秒（相对偏移，展示时换算） */
  processedAgoMs: number | null;
  verifyOpinion: string | null;
  isSimulated: boolean;
}

const EVENT_COUNT = 30;

function buildEventStore(): EventSeed[] {
  const list: EventSeed[] = [];
  for (let i = 0; i < EVENT_COUNT; i += 1) {
    const point = pointOf(i);
    const daysAgo = i < 12 ? 0 : ((i - 12) % 6) + 1;
    const timeOfDay = (i % 12) / 12;
    // 每 3 条留 1 条待核实
    const isPending = i % 3 === 0;
    list.push({
      eventId: String(7001 + i),
      plateNo: PLATES[i % PLATES.length],
      vehicleType: VEHICLE_TYPES[i % VEHICLE_TYPES.length],
      vehicleColor: COLORS[i % COLORS.length],
      direction: directionOfPoint(point),
      pointIndex: i % POINT_DEFS.length,
      daysAgo,
      timeOfDay,
      // 约 1/7 置信度缺失，验证页面「暂无」
      confidence: i % 7 === 3 ? null : 0.85,
      algorithmStatus: 'SUCCEEDED',
      verifyStatus: isPending ? 'PENDING' : 'VERIFIED',
      eventVersion: isPending ? 1 : 2,
      processedBy: isPending ? null : '系统',
      processedAtAbs: null,
      processedAgoMs: isPending ? null : 60 * SECOND,
      verifyOpinion: isPending ? null : '查看识别记录后自动标记为已核实',
      isSimulated: true,
    });
  }
  return list;
}

/** 模块级可变存储：查看即标记会改写它 */
export const EVENT_STORE: EventSeed[] = buildEventStore();

export function findEvent(eventId: string): EventSeed | undefined {
  return EVENT_STORE.find((e) => e.eventId === eventId);
}

function recognizedMsOf(item: EventSeed, now: number): number {
  const startToday = startOfTodayMs(now);
  if (item.daysAgo === 0) {
    const elapsed = now - startToday;
    return startToday + Math.floor(item.timeOfDay * elapsed);
  }
  return startToday - item.daysAgo * DAY + Math.floor(item.timeOfDay * DAY);
}

function processedMsOf(item: EventSeed, now: number): number | null {
  if (item.verifyStatus === 'PENDING') return null;
  if (item.processedAtAbs !== null) return item.processedAtAbs;
  return item.processedAgoMs === null ? null : recognizedMsOf(item, now) + item.processedAgoMs;
}

function buildEventNo(item: EventSeed, now: number): string {
  return `VC${formatDateCompact(recognizedMsOf(item, now))}${item.eventId.slice(-4)}`;
}

/* ---------- 今日识别车辆（左侧列表） ---------- */

export function createTodayVehicleList(now: number = Date.now()): any[] {
  const startToday = startOfTodayMs(now);
  return EVENT_STORE.filter((item) => recognizedMsOf(item, now) >= startToday)
    .map((item) => {
      const point = pointOf(item.pointIndex);
      return {
        eventId: item.eventId,
        plateNo: item.plateNo,
        vehicleType: item.vehicleType,
        vehicleColor: item.vehicleColor,
        direction: item.direction,
        directionName: directionLabel(item.direction),
        pointId: point.pointId,
        gateName: point.pointName,
        recognizedAt: formatDateTime(recognizedMsOf(item, now)),
        verifyStatus: item.verifyStatus,
      };
    })
    .sort((a, b) => new Date(b.recognizedAt).getTime() - new Date(a.recognizedAt).getTime());
}

/* ---------- 出入事件记录行（列表 / 历史 / 导出） ---------- */

function buildRow(item: EventSeed, now: number): any {
  const point = pointOf(item.pointIndex);
  const processedMs = processedMsOf(item, now);
  return {
    eventId: item.eventId,
    eventNo: buildEventNo(item, now),
    plateNo: item.plateNo,
    vehicleType: item.vehicleType,
    vehicleColor: item.vehicleColor,
    direction: item.direction,
    directionName: directionLabel(item.direction),
    pointId: point.pointId,
    gateName: point.pointName,
    recognizedAt: formatDateTime(recognizedMsOf(item, now)),
    warehouseId: point.warehouseId,
    warehouseName: point.warehouseName,
    cameraCode: point.cameraCode,
    algorithmStatus: item.algorithmStatus,
    // 保留 null：页面据此显示「暂无」而不是 0%
    confidence: item.confidence,
    verifyStatus: item.verifyStatus,
    processedBy: item.processedBy,
    processedAt: processedMs === null ? null : formatDateTime(processedMs),
    verifyOpinion: item.verifyOpinion,
    eventVersion: item.eventVersion,
  };
}

/** 把内存态整份物化为响应行（时间全部按 now 现算） */
export function materializeEvents(now: number = Date.now()): any[] {
  return EVENT_STORE.map((item) => buildRow(item, now)).sort(
    (a, b) => new Date(b.recognizedAt).getTime() - new Date(a.recognizedAt).getTime(),
  );
}

/* ---------- 详情 ---------- */

function buildEvidences(item: EventSeed, occurredMs: number): any[] {
  return [
    {
      evidenceId: `${item.eventId}-EV-FULL`,
      evidenceType: 'VEHICLE_IMAGE',
      originalUrl: `https://picsum.photos/seed/veh-${item.eventId}-full/640/360`,
      annotatedUrl: null,
      thumbnailUrl: `https://picsum.photos/seed/veh-${item.eventId}-full-t/320/180`,
      capturedAt: formatDateTime(occurredMs),
      cameraCode: pointOf(item.pointIndex).cameraCode,
      contentHash: `sha256-full-${item.eventId}`,
      sortNo: 1,
    },
    {
      evidenceId: `${item.eventId}-EV-PLATE`,
      evidenceType: 'PLATE_CROP',
      originalUrl: `https://picsum.photos/seed/veh-${item.eventId}-plate/640/360`,
      // 标注图与原图分离，不覆盖原图
      annotatedUrl: `https://picsum.photos/seed/veh-${item.eventId}-plate-marked/640/360`,
      thumbnailUrl: `https://picsum.photos/seed/veh-${item.eventId}-plate-t/320/180`,
      capturedAt: formatDateTime(occurredMs),
      cameraCode: pointOf(item.pointIndex).cameraCode,
      contentHash: `sha256-plate-${item.eventId}`,
      sortNo: 2,
    },
  ];
}

function buildProcessRecords(item: EventSeed, now: number): any[] {
  const processedMs = processedMsOf(item, now);
  if (processedMs === null) return [];
  const isAutoViewed = item.processedAtAbs !== null;
  return [
    {
      recordId: `${item.eventId}-PR-1`,
      statusBefore: 'PENDING',
      statusAfter: 'VERIFIED',
      processResult: 'VERIFIED',
      processOpinion: item.verifyOpinion ?? '',
      processedBy: item.processedBy ?? '',
      processedAt: formatDateTime(processedMs),
      source: isAutoViewed ? 'WEB' : 'SEED',
    },
  ];
}

/** 记录详情（含证据、核实记录与算法任务） */
export function buildEventDetail(item: EventSeed, now: number = Date.now()): any {
  const row = buildRow(item, now);
  const point = pointOf(item.pointIndex);
  // 算法任务历史随详情一次带回（与后端 getEventDetail 扩展同形）
  const algorithmTasks = materializeAlgorithmTasks(item.eventId, now);
  return {
    ...row,
    eventDesc: `${point.pointName}（${point.cameraCode}）于 ${row.recognizedAt} 捕获车辆并识别车牌、车型与颜色。`,
    isSimulated: item.isSimulated,
    evidences: buildEvidences(item, recognizedMsOf(item, now)),
    processRecords: buildProcessRecords(item, now),
    algorithmTasks,
    latestAlgorithmTask: algorithmTasks.length ? algorithmTasks[0] : null,
  };
}

/**
 * 查看即标记为已核实（与真实后端 `markEventViewed` 同语义）。
 *
 * - 状态、核实人、核实意见、版本号一律由「服务端」决定，不接受前端传入；
 * - 仅 PENDING 会被改写，其余幂等返回，不重复追加审计、不自增版本；
 * @returns 是否发生了实际改写
 */
export function markViewed(item: EventSeed, now: number = Date.now()): boolean {
  if (item.verifyStatus !== 'PENDING') return false;
  item.verifyStatus = 'VERIFIED';
  item.verifyOpinion = '查看识别记录后自动标记为已核实';
  item.processedBy = 'currentUser';
  item.processedAtAbs = now;
  item.processedAgoMs = null;
  item.eventVersion += 1;
  return true;
}

/* ---------- 算法任务（「重新识别」） ---------- */

/**
 * 算法任务内存模型。
 *
 * 形状对齐后端 `VehicleAlgorithmTaskRespDTO`（驼峰字段、时间 `yyyy-MM-dd HH:mm:ss`）：
 * - 无 algorithm_result 列，结论由任务行给出（RECOGNIZED/NO_VEHICLE/UNCERTAIN）；
 * - 结论类型字段是 `vehicleType`（不是 conclusionType）；
 * - 车牌只回传掩码 `plateMasked`，绝不出现原文。
 *
 * 生命周期（前端轮询逻辑必须能在 Mock 下被验证）：
 *   提交 → QUEUED；第 1、2 次查询 → RUNNING；第 3 次起 → SUCCEEDED。
 * **不允许一查询就成功。**
 */
interface AlgoTaskSeed {
  taskId: string;
  eventId: string;
  pointId: string;
  taskType: string;
  executionStatus: string;
  conclusion: string | null;
  vehicleType: string | null;
  direction: 'IN' | 'OUT' | null;
  plateMasked: string | null;
  plateRef: string | null;
  confidence: number | null;
  modelName: string | null;
  modelVersion: string | null;
  algorithmVersion: string | null;
  configVersion: string | null;
  inferenceDurationMs: number | null;
  /** 历史任务：距 now 的毫秒偏移 */
  startedAgoMs: number | null;
  finishedAgoMs: number | null;
  /** 实时任务：绝对毫秒 */
  startedAbs: number | null;
  finishedAbs: number | null;
  detectedCount: number | null;
  simulated: boolean;
  errorCode: string | null;
  errorMessage: string | null;
  /** 本次运行提交的任务（走轮询状态机） */
  live: boolean;
  pollCount: number;
}

/** 车牌掩码：只留前 2 位与后 2 位，中间打码（与后端口径一致） */
function maskPlateNo(plateNo: string): string {
  if (!plateNo) return '';
  if (plateNo.length <= 4) return `${plateNo.slice(0, 1)}***`;
  return `${plateNo.slice(0, 2)}***${plateNo.slice(-2)}`;
}

/** 事件车型决定结论：未识别车型 → NO_VEHICLE（不伪造检出） */
function conclusionOf(item: EventSeed): 'RECOGNIZED' | 'NO_VEHICLE' {
  return item.vehicleType === '未识别车型' ? 'NO_VEHICLE' : 'RECOGNIZED';
}

function buildLegacyAlgorithmTasks(): AlgoTaskSeed[] {
  const now = Date.now();
  return EVENT_STORE.map((item, idx) => {
    const point = pointOf(item.pointIndex);
    const concluded = conclusionOf(item) === 'RECOGNIZED';
    const durationMs = 80 + ((idx * 37) % 260);
    // 任务在识别后 30 秒左右跑完，时间随 now 现算
    const startedAgoMs = Math.max(30 * SECOND, now - recognizedMsOf(item, now) + 30 * SECOND);
    return {
      taskId: `VCAT20200101${String(9000 + idx)}`,
      eventId: item.eventId,
      pointId: point.pointId,
      taskType: 'VEHICLE_RECOGNIZE',
      executionStatus: 'SUCCEEDED',
      conclusion: concluded ? 'RECOGNIZED' : 'NO_VEHICLE',
      vehicleType: concluded ? item.vehicleType : null,
      direction: item.direction,
      plateMasked: maskPlateNo(item.plateNo),
      plateRef: `event:${item.eventId}`,
      confidence: concluded ? item.confidence : null,
      modelName: 'mock-vehicle-detector',
      modelVersion: 'mock-1.0.0',
      algorithmVersion: 'mock-adapter-1.0.0',
      configVersion: 'mock-config-1',
      inferenceDurationMs: durationMs,
      startedAgoMs,
      finishedAgoMs: startedAgoMs - durationMs,
      startedAbs: null,
      finishedAbs: null,
      detectedCount: concluded ? 2 : 0,
      simulated: true,
      errorCode: null,
      errorMessage: null,
      live: false,
      pollCount: 0,
    };
  });
}

/** 模块级可变存储：历史任务 + 本次运行「重新识别」提交的实时任务 */
export const ALGO_TASK_STORE: AlgoTaskSeed[] = buildLegacyAlgorithmTasks();

let algoTaskSeq = 0;

/** 任务行物化：时间按 now 现算，字段与后端 VehicleAlgorithmTaskRespDTO 同形 */
function materializeAlgoTask(task: AlgoTaskSeed, now: number): any {
  const startedMs =
    task.startedAbs !== null ? task.startedAbs : task.startedAgoMs !== null ? now - task.startedAgoMs : null;
  const finishedMs =
    task.finishedAbs !== null ? task.finishedAbs : task.finishedAgoMs !== null ? now - task.finishedAgoMs : null;
  return {
    taskId: task.taskId,
    taskNo: task.taskId,
    eventId: task.eventId,
    pointId: task.pointId,
    taskType: task.taskType,
    executionStatus: task.executionStatus,
    conclusion: task.conclusion,
    vehicleType: task.vehicleType,
    direction: task.direction,
    plateMasked: task.plateMasked,
    plateRef: task.plateRef,
    confidence: task.confidence,
    modelName: task.modelName,
    modelVersion: task.modelVersion,
    algorithmVersion: task.algorithmVersion,
    configVersion: task.configVersion,
    inferenceDurationMs: task.inferenceDurationMs,
    startedAt: startedMs === null ? null : formatDateTime(startedMs),
    finishedAt: finishedMs === null ? null : formatDateTime(finishedMs),
    detectedCount: task.detectedCount,
    simulated: task.simulated,
    errorCode: task.errorCode,
    errorMessage: task.errorMessage,
  };
}

/** 某事件的算法任务列表：按开始时间倒序，最多 20 条 */
export function materializeAlgorithmTasks(eventId: string, now: number = Date.now()): any[] {
  return ALGO_TASK_STORE.filter((t) => t.eventId === eventId)
    .map((t) => ({ seed: t, row: materializeAlgoTask(t, now) }))
    .sort((a, b) => {
      const av = toMs(a.row.startedAt);
      const bv = toMs(b.row.startedAt);
      return bv - av;
    })
    .slice(0, 20)
    .map((x) => x.row);
}

/**
 * 提交「重新识别」：立即返回 QUEUED 任务号，不同步出结果。
 * @returns 与后端 VehicleAlgorithmRerunRespDTO 同形
 */
export function rerunAlgorithmTask(eventId: string, now: number = Date.now()): any {
  const event = findEvent(eventId);
  algoTaskSeq += 1;
  const taskId = `VCAT${formatDateCompact(now)}${String(algoTaskSeq).padStart(3, '0')}`;
  ALGO_TASK_STORE.push({
    taskId,
    eventId,
    pointId: event ? pointOf(event.pointIndex).pointId : '',
    taskType: 'VEHICLE_RECOGNIZE',
    executionStatus: 'QUEUED',
    conclusion: null,
    vehicleType: null,
    direction: null,
    plateMasked: event ? maskPlateNo(event.plateNo) : null,
    plateRef: `event:${eventId}`,
    confidence: null,
    modelName: null,
    modelVersion: null,
    algorithmVersion: null,
    configVersion: 'mock-config-1',
    inferenceDurationMs: null,
    startedAgoMs: null,
    finishedAgoMs: null,
    startedAbs: now,
    finishedAbs: null,
    detectedCount: null,
    simulated: true,
    errorCode: null,
    errorMessage: null,
    live: true,
    pollCount: 0,
  });
  return { taskId, executionStatus: 'QUEUED', simulated: true };
}

/**
 * 轮询实时任务：QUEUED → RUNNING → （第 3 次起）SUCCEEDED，成功后回写事件的
 * 算法状态与置信度，**不动核实状态**。
 */
export function queryAlgorithmTask(taskId: string, now: number = Date.now()): any | null {
  const task = ALGO_TASK_STORE.find((t) => t.taskId === taskId);
  if (!task) return null;

  if (task.live && task.executionStatus !== 'SUCCEEDED') {
    task.pollCount += 1;
    if (task.pollCount === 1) {
      task.executionStatus = 'RUNNING';
    } else if (task.pollCount >= 3) {
      const event = findEvent(task.eventId);
      const conclusion = event ? conclusionOf(event) : 'RECOGNIZED';
      task.executionStatus = 'SUCCEEDED';
      task.conclusion = conclusion;
      task.vehicleType = event && conclusion === 'RECOGNIZED' ? event.vehicleType : null;
      task.direction = event?.direction ?? null;
      task.confidence = conclusion === 'RECOGNIZED' ? 0.91 : null;
      task.modelName = 'mock-vehicle-detector';
      task.modelVersion = 'mock-1.0.0';
      task.algorithmVersion = 'mock-adapter-1.0.0';
      task.inferenceDurationMs = 90 + task.pollCount;
      task.detectedCount = conclusion === 'RECOGNIZED' ? 2 : 0;
      task.startedAbs = task.startedAbs ?? now;
      task.finishedAbs = now;
      if (event) {
        // 与后端「成功才回写」同语义：只回写算法列与置信度
        event.algorithmStatus = 'SUCCEEDED';
        event.confidence = task.confidence;
      }
    }
  }

  return materializeAlgoTask(task, now);
}

/* ---------- 播放信息 ---------- */

/** 公开测试流，非真实监控地址 */
const MOCK_MP4_URL = 'https://test-streams.mux.dev/x36xhzz/x36xhzz.mp4';

/** 地址有效期：5 分钟（到期后前端要重新取流） */
const STREAM_TTL_MS = 5 * MINUTE;

/**
 * 短时播放信息（按点位）。
 *
 * 与真实后端同形：不可播放时 playableUrl / expiresAt 为空串，unavailableReason 给原因。
 */
export function createStreamInfo(pointId: string, now: number = Date.now()): any {
  const point = pointById(pointId);

  const base = {
    pointId,
    canPlay: false as boolean,
    expiresAt: '',
    platformCameraId: point?.platformCameraId ?? '',
    playableUrl: '',
    protocol: 'MP4',
    unavailableReason: '',
  };

  if (!point) {
    return { ...base, unavailableReason: 'POINT_NOT_FOUND' };
  }
  if (point.deviceStatus === 'OFFLINE') {
    return { ...base, unavailableReason: 'CAMERA_OFFLINE' };
  }
  if (point.deviceStatus === 'UNKNOWN') {
    return { ...base, unavailableReason: 'NO_ADDRESS' };
  }

  return {
    pointId,
    canPlay: true,
    expiresAt: formatDateTime(now + STREAM_TTL_MS),
    platformCameraId: point.platformCameraId,
    playableUrl: MOCK_MP4_URL,
    protocol: 'MP4',
    unavailableReason: null,
  };
}

/** 点位列表（供需要点位网格的场景使用；车辆首页不展示，但保留生成器） */
export function createPointList(now: number = Date.now()): any[] {
  return POINT_DEFS.map((p, index) => ({
    pointId: p.pointId,
    pointCode: p.pointCode,
    pointName: p.pointName,
    warehouseId: p.warehouseId,
    warehouseName: p.warehouseName,
    cameraCode: p.cameraCode,
    cameraName: p.cameraName,
    platformCameraId: p.platformCameraId,
    deviceStatus: p.deviceStatus,
    alarmStatus: p.alarmStatus,
    lastHeartbeatAt: p.heartbeatAgoSec === null ? null : formatDateTime(now - p.heartbeatAgoSec),
    latestEventAt: null,
    sortNo: index + 1,
  }));
}
