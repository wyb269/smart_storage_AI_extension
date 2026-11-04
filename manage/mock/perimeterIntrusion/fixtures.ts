/**
 * 周界入侵识别 Mock - 共享假数据生成器
 *
 * 形状来源（三者必须一致，否则切到真实后端会静默出错）：
 * - `src/api/perimeterIntrusion.ts` 的 `normalize*` 函数（页面实际消费的字段）
 * - `src/api/model/perimeterIntrusionModel.ts` 的请求/响应类型
 * - 真实后端约定：字段名驼峰、时间为 `yyyy-MM-dd HH:mm:ss` 字符串
 *
 * ⚠️ 时间戳的生成方式（本文件最重要的一条约定）
 * 存储层只保存**相对当前时刻的偏移量**（`occurredAgoMs` / `heartbeatAgoSec`），
 * 所有对外时间字符串都在每次调用 `materialize*` 时基于 `Date.now()` 现算。
 *
 * 原因：本功能的展示状态用 **90 分钟心跳阈值**（`shared/constants.HEARTBEAT_TIMEOUT_MS`）
 * 判定设备离线。若心跳时间在模块加载时固定一次，dev server 跑久后所有防区段
 * 都会被误判成「离线」，Mock 模式下整页只剩离线，等于没有假数据
 * （仓门那一版踩过这个坑，它的 `BASE_TIME` 就是模块加载时算的）。
 *
 * ⚠️ 「未布防」与「离线」是两条独立的信息
 * defenseStatus=DISARMED 的段特意保持 ONLINE + 心跳新鲜：
 * 这样才验得到本功能相对模板多出来的那条判定优先级
 * （先判布防、再回落共享计算），而不是被离线态盖掉。
 *
 * 安全：图片/视频地址全部是公开占位与测试资源，不含真实监控画面、
 * 摄像头账号、RTSP 凭据或内网地址。
 */

/* ---------- 确定性伪随机（同一 seed 得到同一序列） ---------- */

function createRandom(seed: number) {
  let s = seed;
  return () => {
    s = (s * 9301 + 49297) % 233280;
    return s / 233280;
  };
}

function pickOne<T>(rand: () => number, list: T[]): T {
  return list[Math.floor(rand() * list.length) % list.length];
}

/* ---------- 时间工具 ---------- */

const SECOND = 1_000;
const MINUTE = 60 * SECOND;
const HOUR = 60 * MINUTE;

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

/* ---------- 防区与仓库基础数据 ---------- */

type ZoneCode = 'EAST' | 'WEST' | 'SOUTH' | 'NORTH';

const ZONES: Array<{ zone: ZoneCode; zoneName: string; cn: string }> = [
  { zone: 'EAST', zoneName: '东区', cn: '东' },
  { zone: 'WEST', zoneName: '西区', cn: '西' },
  { zone: 'SOUTH', zoneName: '南区', cn: '南' },
  { zone: 'NORTH', zoneName: '北区', cn: '北' },
];

const WAREHOUSES = [
  { id: 'WH-001', name: '一号粮库' },
  { id: 'WH-002', name: '二号粮库' },
  { id: 'WH-003', name: '三号粮库' },
  { id: 'WH-004', name: '散粮库' },
];

interface PointDef {
  pointId: string;
  pointCode: string;
  /** 卡片标题，形如「东防区-东墙段01」 */
  pointName: string;
  zone: ZoneCode;
  zoneName: string;
  segmentName: string;
  warehouseId: string;
  warehouseName: string;
  cameraCode: string;
  /** 摄像头安装位置，右侧监控标题与事件列表都要用 */
  cameraName: string;
  platformCameraId: string;
  deviceStatus: 'ONLINE' | 'OFFLINE' | 'UNKNOWN';
  alarmStatus: 'NORMAL' | 'ACTIVE' | 'RECOVERED';
  /** 人工布防开关 */
  defenseStatus: 'ARMED' | 'DISARMED';
  /**
   * 心跳距**现在**的秒数。每次请求基于 Date.now() 现算，
   * 因此小于 90 分钟的都是「新鲜心跳」，大于则演示离线判定。
   */
  heartbeatAgoSec: number | null;
  /** 播放分支覆盖，见 createStreamInfo */
  streamBranch?: 'HLS' | 'EXPIRED';
}

/**
 * 12 个防区段（四面防区各 3 段）。
 * 状态分布覆盖页面全部展示态：正常布防 / 入侵报警 / 未布防 / 离线 / 未知。
 */
const POINT_DEFS: PointDef[] = Array.from({ length: 12 }, (_, index) => {
  const i = index + 1;
  const zone = ZONES[Math.floor(index / 3)];
  const seg = (index % 3) + 1;
  const wh = WAREHOUSES[index % WAREHOUSES.length];
  const segmentName = `${zone.cn}墙段${pad2(seg)}`;

  let deviceStatus: PointDef['deviceStatus'] = 'ONLINE';
  let alarmStatus: PointDef['alarmStatus'] = 'NORMAL';
  let defenseStatus: PointDef['defenseStatus'] = 'ARMED';
  let heartbeatAgoSec: number | null = (1 + (i % 5)) * MINUTE;
  let streamBranch: PointDef['streamBranch'] | undefined;

  if (i === 2 || i === 9) {
    // 明确离线：deviceStatus=OFFLINE，且心跳也已陈旧
    deviceStatus = 'OFFLINE';
    heartbeatAgoSec = 182 * MINUTE;
  } else if (i === 12) {
    // UNKNOWN：设备状态无法判定，页面显示「未知」（虚线卡片），不得显示成离线
    deviceStatus = 'UNKNOWN';
    heartbeatAgoSec = null;
  } else if (i === 4 || i === 7) {
    // 在线且入侵告警中
    alarmStatus = 'ACTIVE';
    heartbeatAgoSec = 20 * SECOND;
  } else if (i === 5 || i === 10) {
    // 人工撤防：刻意保持在线且无告警，用来验证「未布防」不是从设备态推出来的
    defenseStatus = 'DISARMED';
  } else if (i === 6) {
    // 在线但地址过期：验证「播放地址已过期」+ 重试按钮
    streamBranch = 'EXPIRED';
  } else if (i === 8) {
    // 一期前端只有原生 <video>，此分支验证「该视频协议暂不支持播放」的明确提示
    streamBranch = 'HLS';
  } else if (i === 11) {
    // 真实故障形态：库里 device_status 仍是 ONLINE，但心跳超过 90 分钟，
    // 页面必须判离线。这条用例专门盯着心跳阈值改动。
    heartbeatAgoSec = 126 * MINUTE;
  }

  return {
    pointId: String(100 + index),
    pointCode: `PD-${pad2(i).padStart(3, '0')}`,
    pointName: `${zone.cn}防区-${segmentName}`,
    zone: zone.zone,
    zoneName: zone.zoneName,
    segmentName,
    warehouseId: wh.id,
    warehouseName: wh.name,
    cameraCode: `CAM-${pad2(i).padStart(3, '0')}`,
    cameraName: `${zone.cn}侧围墙中段`,
    platformCameraId: `C${pad2(i).padStart(3, '0')}`,
    deviceStatus,
    alarmStatus,
    defenseStatus,
    heartbeatAgoSec,
    streamBranch,
  };
});

/**
 * 防区段状态列表。
 *
 * @param now 请求时刻；心跳与最新事件时间都由它推出，缺省取 `Date.now()`
 */
export function createMonitorPointList(now: number = Date.now()): any[] {
  return POINT_DEFS.map((p, index) => {
    const latest = latestEventOf(p.pointId, now);
    return {
      pointId: p.pointId,
      pointCode: p.pointCode,
      pointName: p.pointName,
      zone: p.zone,
      zoneName: p.zoneName,
      segmentName: p.segmentName,
      warehouseId: p.warehouseId,
      warehouseName: p.warehouseName,
      cameraConfigId: p.pointId,
      cameraCode: p.cameraCode,
      cameraName: p.cameraName,
      platformCameraId: p.platformCameraId,
      deviceStatus: p.deviceStatus,
      alarmStatus: p.alarmStatus,
      defenseStatus: p.defenseStatus,
      lastHeartbeatAt: p.heartbeatAgoSec === null ? null : formatDateTime(now - p.heartbeatAgoSec),
      latestEventAt: latest === null ? null : formatDateTime(latest),
      sortNo: index + 1,
    };
  });
}

/** 状态未知的防区段不产生入侵事件，其余都可能 */
function pointEligibleForEvent(def: PointDef): boolean {
  return def.deviceStatus !== 'UNKNOWN';
}

const EVENT_POINTS = POINT_DEFS.filter(pointEligibleForEvent);

/* ---------- 入侵类型 ---------- */

const INTRUSION_TYPES = ['CLIMB', 'CROSS', 'INTRUSION', 'LOITER', 'OTHER'] as const;

const INTRUSION_TYPE_NAME: Record<string, string> = {
  CLIMB: '攀爬',
  CROSS: '翻越',
  INTRUSION: '闯入',
  LOITER: '徘徊',
  OTHER: '其他',
};

/** 未核实事件的下标（每次查看会被真实改写，这里只决定初始分布） */
const PENDING_INDEXES = new Set([0, 3, 7, 12, 19, 26]);

/** 处理中事件的下标：三值状态必须都能在 Mock 里看到 */
const PROCESSING_INDEXES = new Set([5, 14, 22]);

function algorithmStatusOf(index: number): string {
  // 多数是识别成功，其余覆盖 7 种算法状态
  const r = index % 13;
  if (r === 3) return 'MOCK';
  if (r === 5) return 'QUEUED';
  if (r === 7) return 'FAILED';
  if (r === 9) return 'RUNNING';
  if (r === 11) return 'TIMEOUT';
  if (r === 12) return 'SKIPPED';
  return 'SUCCEEDED';
}

/* ---------- 事件存储 ---------- */

/**
 * 事件条目在内存里只存**偏移量**与可变状态，不存绝对时间字符串。
 *
 * `processedAtAbs` 例外：它是「某人真的点了查看」这一动作发生的时间，
 * 记录的是当时的绝对毫秒，天然落在过去，不会随进程运行时长而失真。
 */
interface EventSeed {
  eventId: string;
  pointId: string;
  pointName: string;
  zone: ZoneCode;
  zoneName: string;
  cameraCode: string;
  cameraName: string;
  warehouseId: string;
  warehouseName: string;
  intrusionType: string;
  intrusionTypeName: string;
  algorithmStatus: string;
  algorithmResult: string | null;
  confidence: number | null;
  processStatus: string;
  eventVersion: number;
  /** 发生时间距今毫秒数（生成时确定，展示时每次换算） */
  occurredAgoMs: number;
  /** 已处理事件的处理时间距今毫秒数（历史种子数据用） */
  processedAgoMs: number | null;
  /** 本次运行中被「查看即标记」写下的绝对时间 */
  processedAtAbs: number | null;
  processedBy: string | null;
  processOpinion: string | null;
  durationSeconds: number | null;
  recoveredAgoMs: number | null;
  isSimulated: boolean;
  modelTag: string;
}

const EVENT_COUNT = 30;

function buildEventStore(): EventSeed[] {
  const rand = createRandom(20261028);
  const list: EventSeed[] = [];

  for (let i = 0; i < EVENT_COUNT; i += 1) {
    const point = EVENT_POINTS[i % EVENT_POINTS.length];
    const intrusionType = INTRUSION_TYPES[i % INTRUSION_TYPES.length];
    const algorithmStatus = algorithmStatusOf(i);
    const hasConclusion = algorithmStatus === 'SUCCEEDED' || algorithmStatus === 'MOCK';

    // 算法未成功时结论为 null，不伪造为 NORMAL；约 15% 成功项置信度缺失，验证「暂无」
    let algorithmResult: string | null = null;
    let confidence: number | null = null;
    if (hasConclusion) {
      algorithmResult = pickOne(rand, ['ANOMALY', 'ANOMALY', 'ANOMALY', 'UNCERTAIN', 'NORMAL']);
      confidence = rand() < 0.15 ? null : Number((0.55 + rand() * 0.44).toFixed(4));
    }

    // 最新一条约 8 分钟前，往前每条约 4.7 小时（30 条 ≈ 5.7 天，落在默认 7 天窗口内）
    const occurredAgoMs = 8 * MINUTE + i * (4.7 * HOUR) + Math.floor(rand() * 40) * MINUTE;

    const isPending = PENDING_INDEXES.has(i);
    const isProcessing = !isPending && PROCESSING_INDEXES.has(i);
    const durationSeconds = hasConclusion ? 60 + Math.floor(rand() * 900) : null;

    list.push({
      eventId: String(3001 + i),
      pointId: point.pointId,
      pointName: point.pointName,
      zone: point.zone,
      zoneName: point.zoneName,
      cameraCode: point.cameraCode,
      cameraName: point.cameraName,
      warehouseId: point.warehouseId,
      warehouseName: point.warehouseName,
      intrusionType,
      intrusionTypeName: INTRUSION_TYPE_NAME[intrusionType] ?? '其他',
      algorithmStatus,
      algorithmResult,
      confidence,
      processStatus: isPending ? 'PENDING' : isProcessing ? 'PROCESSING' : 'DONE',
      eventVersion: isPending ? 1 : 2,
      occurredAgoMs,
      processedAgoMs: isPending ? null : Math.max(30 * SECOND, occurredAgoMs - 30 * MINUTE),
      processedAtAbs: null,
      processedBy: isPending ? null : pickOne(rand, ['zhangsan', 'lisi', 'wangwu']),
      processOpinion: isPending
        ? null
        : isProcessing
          ? '已通知巡逻岗前往该防区段核实。'
          : '已现场确认入侵行为并恢复周界布防。',
      durationSeconds,
      // 已恢复的事件在发生之后 durationSeconds 秒恢复
      recoveredAgoMs: durationSeconds === null ? null : Math.max(10 * SECOND, occurredAgoMs - durationSeconds * SECOND),
      isSimulated: algorithmStatus === 'MOCK',
      modelTag: algorithmStatus === 'MOCK' ? 'perimeter-mock-v0' : 'perimeter-intrusion-detector-v1',
    });
  }

  return list;
}

/** 模块级可变存储：查看即标记会改写它 */
export const EVENT_STORE: EventSeed[] = buildEventStore();

/* ---------- 算法任务（历史种子 + 「重新识别」实时任务） ---------- */

/** 与后端 PerimeterIntrusionConstant.DETAIL_ALGORITHM_TASK_LIMIT 一致 */
const ALGORITHM_TASK_LIMIT = 20;

/**
 * 任务种子。历史任务只存偏移量；本次运行提交的任务存绝对毫秒
 * （与事件存储同一套时间约定，避免 dev server 跑久后时间倒挂）。
 */
interface AlgoTaskSeed {
  taskId: string;
  eventId: string;
  taskType: string;
  executionStatus: string;
  conclusion: string | null;
  intrusionType: string | null;
  confidence: number | null;
  modelName: string | null;
  modelVersion: string | null;
  algorithmVersion: string | null;
  configVersion: string;
  inferenceDurationMs: number | null;
  startedAgoMs: number | null;
  finishedAgoMs: number | null;
  startedAbs: number | null;
  finishedAbs: number | null;
  detectedCount: number | null;
  simulated: boolean;
  errorCode: string | null;
  errorMessage: string | null;
  /** 本次运行提交的任务（走 QUEUED/RUNNING → 终态 状态机） */
  live: boolean;
  pollCount: number;
}

/** 历史任务状态：镜像事件的算法状态（MOCK 归一为 SUCCEEDED，模拟由 simulated 表达） */
function seedTaskStatusOf(algorithmStatus: string): string {
  if (algorithmStatus === 'MOCK') return 'SUCCEEDED';
  return algorithmStatus;
}

function buildSeedAlgorithmTasks(): AlgoTaskSeed[] {
  return EVENT_STORE.map((item, idx) => {
    const status = seedTaskStatusOf(item.algorithmStatus);
    const succeeded = status === 'SUCCEEDED';
    const durationMs = succeeded || status === 'FAILED' || status === 'TIMEOUT' ? 24 + ((idx * 47) % 380) : null;
    const conclusion = succeeded ? (item.algorithmResult ?? 'ANOMALY') : null;
    const startedAgoMs = Math.max(30 * SECOND, item.occurredAgoMs - 30 * SECOND);
    return {
      taskId: `PITASK${item.eventId}`,
      eventId: item.eventId,
      taskType: 'PERIMETER_INTRUSION_DETECT',
      executionStatus: status,
      conclusion,
      intrusionType: succeeded ? item.intrusionType : null,
      confidence: succeeded ? item.confidence : null,
      modelName: succeeded ? item.modelTag : null,
      modelVersion: succeeded ? '1.0.0' : null,
      algorithmVersion: succeeded ? 'mock-adapter-1.0.0' : null,
      configVersion: 'perimeter-config-v1',
      inferenceDurationMs: durationMs,
      startedAgoMs,
      finishedAgoMs: durationMs === null ? null : startedAgoMs - durationMs,
      startedAbs: null,
      finishedAbs: null,
      // 与结论自洽：只有判为入侵（ANOMALY）才伪造检出目标
      detectedCount: succeeded ? (conclusion === 'ANOMALY' ? 1 + (idx % 3) : 0) : null,
      simulated: true,
      errorCode: status === 'FAILED' ? 'ALGORITHM_ERROR' : status === 'TIMEOUT' ? 'ALGORITHM_TIMEOUT' : null,
      errorMessage:
        status === 'FAILED'
          ? '模拟算法执行失败（历史样例，非真实故障）'
          : status === 'TIMEOUT'
            ? '模拟算法执行超时（历史样例，非真实故障）'
            : null,
      live: false,
      pollCount: 0,
    };
  });
}

/** 模块级可变存储：历史任务 + 本次运行提交的实时任务 */
export const ALGO_TASK_STORE: AlgoTaskSeed[] = buildSeedAlgorithmTasks();

let algoTaskSeq = 0;

/** 任务行物化：字段与后端 PerimeterAlgorithmTaskRespDTO 同形 */
function materializeAlgoTask(task: AlgoTaskSeed, now: number): any {
  const startedMs =
    task.startedAbs !== null ? task.startedAbs : task.startedAgoMs !== null ? now - task.startedAgoMs : null;
  const finishedMs =
    task.finishedAbs !== null ? task.finishedAbs : task.finishedAgoMs !== null ? now - task.finishedAgoMs : null;
  return {
    taskId: task.taskId,
    taskNo: task.taskId,
    eventId: task.eventId,
    taskType: task.taskType,
    executionStatus: task.executionStatus,
    conclusion: task.conclusion,
    // 周界专用结论列：不是 conclusionType
    intrusionType: task.intrusionType,
    confidence: task.confidence,
    modelName: task.modelName,
    modelVersion: task.modelVersion,
    algorithmVersion: task.algorithmVersion,
    configVersion: task.configVersion,
    startedAt: startedMs === null ? null : formatDateTime(startedMs),
    finishedAt: finishedMs === null ? null : formatDateTime(finishedMs),
    inferenceDurationMs: task.inferenceDurationMs,
    detectedCount: task.detectedCount,
    simulated: task.simulated,
    errorCode: task.errorCode,
    errorMessage: task.errorMessage,
  };
}

/** 某事件的算法任务列表：按开始时间倒序，最多 20 条 */
export function materializeAlgorithmTasks(eventId: string, now: number = Date.now()): any[] {
  const startedMsOf = (t: AlgoTaskSeed): number => {
    if (t.startedAbs !== null) return t.startedAbs;
    return t.startedAgoMs !== null ? now - t.startedAgoMs : -1;
  };
  return ALGO_TASK_STORE.filter((t) => t.eventId === eventId)
    .sort((a, b) => startedMsOf(b) - startedMsOf(a))
    .slice(0, ALGORITHM_TASK_LIMIT)
    .map((t) => materializeAlgoTask(t, now));
}

/**
 * 提交「重新识别」：立即返回 QUEUED 任务号，不同步出结果。
 *
 * 与后端一致：撤防段也先受理，随后由算法侧以 SKIPPED 终态收口
 * （Mock 网关对 DISARMED 不伪造 NORMAL，见后端 MockPerimeterIntrusionAlgorithmGateway）。
 *
 * @returns 与后端 PerimeterAlgorithmRerunRespDTO 同形
 */
export function rerunAlgorithmTask(eventId: string, now: number = Date.now()): any {
  algoTaskSeq += 1;
  const taskId = `MOCK-TASK-${now.toString(36)}${algoTaskSeq}`;
  ALGO_TASK_STORE.push({
    taskId,
    eventId,
    taskType: 'PERIMETER_INTRUSION_DETECT',
    executionStatus: 'QUEUED',
    conclusion: null,
    intrusionType: null,
    confidence: null,
    modelName: null,
    modelVersion: null,
    algorithmVersion: null,
    configVersion: 'perimeter-config-v1',
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
 * 轮询实时任务：QUEUED → RUNNING →（第 3 次起）SUCCEEDED；
 * 撤防段在第 1 次轮询直接 SKIPPED。成功后回写事件的算法列，
 * 不动处理状态与版本号（与后端 persistTaskResult 同语义）。
 */
export function queryAlgorithmTask(taskId: string, now: number = Date.now()): any | null {
  const task = ALGO_TASK_STORE.find((t) => t.taskId === taskId);
  if (!task) return null;

  if (task.live && task.executionStatus !== 'SUCCEEDED' && task.executionStatus !== 'SKIPPED') {
    task.pollCount += 1;
    const event = findEvent(task.eventId);
    const point = POINT_DEFS.find((p) => p.pointId === event?.pointId);

    if (point?.defenseStatus === 'DISARMED') {
      task.executionStatus = 'SKIPPED';
      task.startedAbs = task.startedAbs ?? now;
      task.finishedAbs = now;
      task.inferenceDurationMs = 40 + task.pollCount;
    } else if (task.pollCount === 1) {
      task.executionStatus = 'RUNNING';
    } else if (task.pollCount >= 3) {
      const intrusionType = event?.intrusionType ?? 'INTRUSION';
      task.executionStatus = 'SUCCEEDED';
      task.conclusion = 'ANOMALY';
      task.intrusionType = intrusionType;
      task.confidence = 0.86;
      task.modelName = 'perimeter-intrusion-detector';
      task.modelVersion = 'mock-1.0.0';
      task.algorithmVersion = 'mock-adapter-1.0.0';
      task.inferenceDurationMs = 130 + task.pollCount;
      task.detectedCount = 2;
      task.startedAbs = task.startedAbs ?? now;
      task.finishedAbs = now;

      if (event) {
        event.algorithmStatus = 'SUCCEEDED';
        event.algorithmResult = task.conclusion;
        event.confidence = task.confidence;
        event.intrusionType = intrusionType;
        event.intrusionTypeName = intrusionTypeNameOf(intrusionType);
      }
    }
  }

  return materializeAlgoTask(task, now);
}

/** 某防区段最近一次事件的时刻（用于 latestEventAt），不存在返回 null */
function latestEventOf(pointId: string, now: number): number | null {
  let latest: number | null = null;
  for (const item of EVENT_STORE) {
    if (item.pointId !== pointId) continue;
    const ms = now - item.occurredAgoMs;
    if (latest === null || ms > latest) latest = ms;
  }
  return latest;
}

export function findEvent(eventId: string): EventSeed | undefined {
  return EVENT_STORE.find((e) => e.eventId === eventId);
}

function occurredMsOf(item: EventSeed, now: number): number {
  return now - item.occurredAgoMs;
}

function processedMsOf(item: EventSeed, now: number): number | null {
  if (item.processStatus === 'PENDING') return null;
  if (item.processedAtAbs !== null) return item.processedAtAbs;
  return item.processedAgoMs === null ? null : now - item.processedAgoMs;
}

/* ---------- 事件编号 ---------- */

/**
 * 事件编号：`PD` + `yyyyMMdd` + 4 位序号（例 `PD202610280001`）。
 *
 * 序号按「同一自然日内、发生时间升序」排，且**每次请求整体重算**：
 * 时间基准会随 `Date.now()` 漂移，若只给当前页重算，跨页可能出现重复编号。
 */
function buildEventNoMap(now: number): Map<string, string> {
  const byDate = new Map<string, EventSeed[]>();
  for (const item of EVENT_STORE) {
    const key = formatDateCompact(occurredMsOf(item, now));
    const bucket = byDate.get(key);
    if (bucket) bucket.push(item);
    else byDate.set(key, [item]);
  }

  const map = new Map<string, string>();
  byDate.forEach((bucket, key) => {
    bucket
      .slice()
      .sort((a, b) => occurredMsOf(a, now) - occurredMsOf(b, now))
      .forEach((item, i) => map.set(item.eventId, `PD${key}${String(i + 1).padStart(4, '0')}`));
  });
  return map;
}

/* ---------- 生成对外响应对象 ---------- */

/** 列表项 / 最近事件行（字段与真实后端 listEventByCondition 的 data 元素同形） */
function buildRow(item: EventSeed, eventNo: string, now: number): any {
  const processedMs = processedMsOf(item, now);

  return {
    eventId: item.eventId,
    eventNo,
    occurredAt: formatDateTime(occurredMsOf(item, now)),
    warehouseId: item.warehouseId,
    warehouseName: item.warehouseName,
    // 事件挂在防区段配置表上，后端列名为 point_id
    pointId: item.pointId,
    pointName: item.pointName,
    zone: item.zone,
    zoneName: item.zoneName,
    cameraConfigId: item.pointId,
    cameraCode: item.cameraCode,
    cameraPosition: item.cameraName,
    intrusionType: item.intrusionType,
    intrusionTypeName: item.intrusionTypeName,
    algorithmStatus: item.algorithmStatus,
    algorithmResult: item.algorithmResult,
    // 保留 null：页面据此显示「暂无」而不是 0%
    confidence: item.confidence,
    processStatus: item.processStatus,
    processedBy: item.processedBy,
    processedAt: processedMs === null ? null : formatDateTime(processedMs),
    processOpinion: item.processOpinion,
    eventVersion: item.eventVersion,
  };
}

/** 把内存态整份物化为响应行（时间全部按 now 现算） */
export function materializeEvents(now: number = Date.now()): any[] {
  const noMap = buildEventNoMap(now);
  return EVENT_STORE.map((item) => buildRow(item, noMap.get(item.eventId) ?? '', now));
}

function buildEvidences(item: EventSeed, occurredMs: number, now: number): any[] {
  const evidences: any[] = [
    {
      evidenceId: `${item.eventId}-EV-REF`,
      evidenceType: 'NORMAL_REFERENCE',
      originalUrl: `https://picsum.photos/seed/perim-${item.eventId}-ref/640/360`,
      annotatedUrl: null,
      thumbnailUrl: `https://picsum.photos/seed/perim-${item.eventId}-ref-t/320/180`,
      capturedAt: formatDateTime(occurredMs - 2 * MINUTE),
      cameraCode: item.cameraCode,
      contentHash: `sha256-ref-${item.eventId}`,
      sortNo: 1,
    },
    {
      evidenceId: `${item.eventId}-EV-INS`,
      evidenceType: 'INTRUSION_FRAME',
      originalUrl: `https://picsum.photos/seed/perim-${item.eventId}-frame/640/360`,
      // 标注图与原图分离，不覆盖原图
      annotatedUrl: `https://picsum.photos/seed/perim-${item.eventId}-marked/640/360`,
      thumbnailUrl: `https://picsum.photos/seed/perim-${item.eventId}-frame-t/320/180`,
      capturedAt: formatDateTime(occurredMs),
      cameraCode: item.cameraCode,
      contentHash: `sha256-ins-${item.eventId}`,
      sortNo: 2,
    },
  ];

  const recoveredMs = item.recoveredAgoMs === null ? null : now - item.recoveredAgoMs;
  if (recoveredMs !== null && recoveredMs >= occurredMs) {
    evidences.push({
      evidenceId: `${item.eventId}-EV-REC`,
      evidenceType: 'RECOVERY_IMAGE',
      originalUrl: `https://picsum.photos/seed/perim-${item.eventId}-rec/640/360`,
      annotatedUrl: null,
      thumbnailUrl: `https://picsum.photos/seed/perim-${item.eventId}-rec-t/320/180`,
      capturedAt: formatDateTime(recoveredMs),
      cameraCode: item.cameraCode,
      contentHash: `sha256-rec-${item.eventId}`,
      sortNo: 3,
    });
  }

  return evidences;
}

function buildProcessRecords(item: EventSeed, now: number): any[] {
  const processedMs = processedMsOf(item, now);
  if (processedMs === null) return [];

  const isAutoViewed = item.processedAtAbs !== null;

  return [
    {
      recordId: `${item.eventId}-PR-1`,
      statusBefore: 'PENDING',
      statusAfter: item.processStatus,
      processResult: item.processStatus,
      processOpinion: item.processOpinion ?? '',
      processedBy: item.processedBy ?? '',
      processedAt: formatDateTime(processedMs),
      // 查看即标记由页面触发，其余为历史种子
      source: isAutoViewed ? 'WEB' : 'SEED',
    },
  ];
}

/** 事件详情（含证据、处理记录与算法任务） */
export function buildEventDetail(item: EventSeed, now: number = Date.now()): any {
  const noMap = buildEventNoMap(now);
  const row = buildRow(item, noMap.get(item.eventId) ?? '', now);
  const occurredMs = occurredMsOf(item, now);
  const recoveredMs = item.recoveredAgoMs === null ? null : now - item.recoveredAgoMs;
  // 与后端一致：任务随详情一次带回（按开始时间倒序，最多 20 条），弹窗不再单独请求
  const algorithmTasks = materializeAlgorithmTasks(item.eventId, now);

  return {
    ...row,
    eventDesc:
      `${row.zoneName} ${item.pointName}（${item.cameraName}，${item.cameraCode}）于 ${row.occurredAt} ` +
      `检测到${item.intrusionTypeName}行为。`,
    recoveredAt: recoveredMs === null || recoveredMs < occurredMs ? null : formatDateTime(recoveredMs),
    durationSeconds: item.durationSeconds,
    isSimulated: item.isSimulated,
    modelName: item.modelTag,
    evidences: buildEvidences(item, occurredMs, now),
    processRecords: buildProcessRecords(item, now),
    algorithmTasks,
    latestAlgorithmTask: algorithmTasks.length ? algorithmTasks[0] : null,
  };
}

/**
 * 查看即标记为已处理（与真实后端 `markEventViewed` 同语义）。
 *
 * - 状态、处理人、处理意见、版本号一律由「服务端」决定，不接受前端传入；
 * - 仅 PENDING 会被改写，PROCESSING 说明已有人在跟，不能被「看了一眼」顶掉；
 * - 其余幂等返回，不重复追加审计、不自增版本。
 * @returns 是否发生了实际改写
 */
export function markViewed(item: EventSeed, now: number = Date.now()): boolean {
  if (item.processStatus === 'DONE') return false;

  item.processStatus = 'DONE';
  item.processOpinion = '查看事件详情后自动标记为已处理';
  item.processedBy = 'currentUser';
  item.processedAtAbs = now;
  item.processedAgoMs = null;
  item.eventVersion += 1;
  return true;
}

/* ---------- 播放信息 ---------- */

/** 公开测试流，非真实监控地址 */
const MOCK_MP4_URL = 'https://test-streams.mux.dev/x36xhzz/x36xhzz.mp4';
const MOCK_HLS_URL = 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8';

/** 地址有效期：5 分钟（到期后前端要重新取流） */
const STREAM_TTL_MS = 5 * MINUTE;

/**
 * 短时播放信息。
 *
 * 与真实后端约定同形（注意 `playableUrl` / `expiresAt` 不可播放时是**空串**，
 * 不是 null）：
 * `{"pointId":"100","canPlay":false,"expiresAt":"","platformCameraId":"C001",
 *   "playableUrl":"","protocol":"MP4","unavailableReason":"CAMERA_OFFLINE"}`
 *
 * 分支顺序与后端 Service 一致：先判存在、再判设备态、最后判布防开关，
 * 因此「撤防」的优先级高于「可播」。
 */
export function createStreamInfo(pointId: string, now: number = Date.now()): any {
  const point = POINT_DEFS.find((p) => p.pointId === pointId);

  const base = {
    pointId,
    cameraConfigId: point?.pointId ?? '',
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

  // 撤防的防区段不提供实时画面，与后端 PerimeterIntrusionServiceImpl 同口径
  if (point.defenseStatus === 'DISARMED') {
    return { ...base, unavailableReason: 'POINT_DISARMED' };
  }

  if (point.streamBranch === 'HLS') {
    return {
      ...base,
      playableUrl: MOCK_HLS_URL,
      protocol: 'HLS',
      unavailableReason: 'UNSUPPORTED_PROTOCOL',
    };
  }

  if (point.streamBranch === 'EXPIRED') {
    return { ...base, unavailableReason: 'URL_EXPIRED' };
  }

  return {
    pointId,
    cameraConfigId: point.pointId,
    canPlay: true,
    expiresAt: formatDateTime(now + STREAM_TTL_MS),
    platformCameraId: point.platformCameraId,
    playableUrl: MOCK_MP4_URL,
    protocol: 'MP4',
    unavailableReason: null,
  };
}

/** 入侵类型编码 → 中文名（导出与兜底展示用） */
export function intrusionTypeNameOf(code: string): string {
  return INTRUSION_TYPE_NAME[code] ?? '其他';
}

/** 处理状态编码 → 页面文案（导出用，三值） */
export function processStatusNameOf(code: string): string {
  if (code === 'PENDING') return '未核实';
  if (code === 'PROCESSING') return '处理中';
  if (code === 'DONE') return '已处理';
  return code;
}
