/**
 * 人员行为监测 Mock - 共享假数据生成器
 *
 * 形状来源（三者必须一致，否则切到真实后端会静默出错）：
 * - `src/api/personBehavior.ts` 的 `normalize*` 函数（页面实际消费的字段）
 * - `src/api/model/personBehaviorModel.ts` 的请求/响应类型
 * - 真实后端实测回来的响应样例（字段名驼峰、时间为 `yyyy-MM-dd HH:mm:ss` 字符串）
 *
 * ⚠️ 时间戳的生成方式（本文件最重要的一条约定）
 * 存储层只保存**相对当前时刻的偏移量**（`occurredAgoMs` 等），
 * 所有对外时间字符串都在每次调用 `materialize*` 时基于 `Date.now()` 现算。
 *
 * 原因：`shared/displayStatus.computeDisplayStatus` 用 **90 分钟心跳阈值**
 * （`shared/constants.HEARTBEAT_TIMEOUT_MS`）判定设备离线。若心跳时间在模块
 * 加载时固定一次，dev server 跑久后所有区域都会被误判成「离线」，
 * Mock 模式下整页只剩离线，等于没有假数据（仓门那一版踩过这个坑）。
 * 这里连事件时间也一并每次现算，保证默认「最近 7 天」查询窗口
 * 与心跳新鲜度在任何运行时刻都成立。
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

/* ---------- 监测区域基础数据 ---------- */

const WAREHOUSES = [
  { id: '9001', name: '一号仓库' },
  { id: '9002', name: '二号仓库' },
  { id: '9003', name: '三号仓库' },
  { id: '9004', name: '散粮仓库' },
];

/** 区域名后缀，与仓库名拼成原型上的「一号仓库前作业区」这类标题 */
const AREA_SUFFIXES = [
  '前作业区',
  '门口作业区',
  '卸粮坑作业区',
  '工作塔一层',
  '罩棚下作业区',
  '北侧装卸区',
  '输送廊道',
  '西门值守区',
];

interface PointDef {
  pointId: string;
  pointCode: string;
  pointName: string;
  cameraId: string;
  cameraCode: string;
  platformCameraId: string;
  warehouseId: string;
  warehouseName: string;
  deviceStatus: 'ONLINE' | 'OFFLINE' | 'UNKNOWN';
  alarmStatus: 'NORMAL' | 'ACTIVE' | 'RECOVERED';
  /**
   * 当前告警对应的行为类型。
   * 共享基座的综合展示态只有一个 ABNORMAL，异常卡片要显示
   * 「人员聚集」还是「未戴安全帽」全靠这一列还原。
   */
  alarmBehaviorType: string | null;
  /**
   * 心跳距**现在**的秒数。每次请求基于 Date.now() 现算，
   * 因此小于 90 分钟的都是「新鲜心跳」，大于则演示离线判定。
   */
  heartbeatAgoSec: number | null;
  /** 播放分支覆盖，见 createStreamInfo */
  streamBranch?: 'HLS' | 'EXPIRED';
}

/**
 * 12 个区域（与真实库种子量级一致）。
 * 状态分布覆盖原型上的五种卡片文案：
 * 正常 / 人员聚集 / 未戴安全帽 / 离线 / 未知。
 */
const POINT_DEFS: PointDef[] = Array.from({ length: 12 }, (_, index) => {
  const i = index + 1;
  const warehouse = WAREHOUSES[index % WAREHOUSES.length];
  const def: PointDef = {
    pointId: String(100 + i),
    pointCode: `AREA-${pad2(i).padStart(3, '0')}`,
    pointName: `${warehouse.name}${AREA_SUFFIXES[index % AREA_SUFFIXES.length]}`,
    cameraId: `C${pad2(i).padStart(3, '0')}`,
    cameraCode: `CAM-${pad2(i).padStart(3, '0')}`,
    platformCameraId: `C${pad2(i).padStart(3, '0')}`,
    warehouseId: warehouse.id,
    warehouseName: warehouse.name,
    deviceStatus: 'ONLINE',
    alarmStatus: 'NORMAL',
    alarmBehaviorType: null,
    heartbeatAgoSec: (1 + (i % 5)) * MINUTE,
  };

  // 第 2、9 个区域明确离线，且心跳也已陈旧
  if (i === 2 || i === 9) {
    def.deviceStatus = 'OFFLINE';
    def.heartbeatAgoSec = 182 * MINUTE;
    return def;
  }
  // 第 12 个区域状态无法判定：页面显示「未知」（虚线卡片），不得显示成离线
  if (i === 12) {
    def.deviceStatus = 'UNKNOWN';
    def.heartbeatAgoSec = null;
    return def;
  }
  // 第 3、7 个区域在线且人员聚集告警
  if (i === 3 || i === 7) {
    def.alarmStatus = 'ACTIVE';
    def.alarmBehaviorType = 'CROWD';
    return def;
  }
  // 第 5、10 个区域在线且未戴安全帽告警
  if (i === 5 || i === 10) {
    def.alarmStatus = 'ACTIVE';
    def.alarmBehaviorType = 'NO_HELMET';
    return def;
  }
  // 第 8 个区域：库里 ONLINE 但心跳超过 90 分钟 —— 真实故障形态，
  // 页面必须判离线。这条用例专门盯着心跳阈值改动。
  if (i === 8) {
    def.heartbeatAgoSec = 126 * MINUTE;
    return def;
  }
  // 第 4 个区域：在线但协议不支持，验证「该视频协议暂不支持播放」
  if (i === 4) {
    def.streamBranch = 'HLS';
    def.heartbeatAgoSec = 190 * SECOND;
    return def;
  }
  // 第 6 个区域：在线但地址过期，验证「播放地址已过期」+ 重试按钮
  if (i === 6) {
    def.streamBranch = 'EXPIRED';
    def.heartbeatAgoSec = 60 * SECOND;
    return def;
  }
  return def;
});

/**
 * 区域状态列表。
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
      warehouseId: p.warehouseId,
      warehouseName: p.warehouseName,
      cameraId: p.cameraId,
      cameraCode: p.cameraCode,
      platformCameraId: p.platformCameraId,
      deviceStatus: p.deviceStatus,
      alarmStatus: p.alarmStatus,
      alarmBehaviorType: p.alarmBehaviorType,
      lastHeartbeatAt: p.heartbeatAgoSec === null ? null : formatDateTime(now - p.heartbeatAgoSec),
      latestEventAt: latest === null ? null : formatDateTime(latest),
      sortNo: index + 1,
    };
  });
}

/** UNKNOWN 的区域不产生事件，其余都可能 */
const EVENT_POINTS = POINT_DEFS.filter((p) => p.deviceStatus !== 'UNKNOWN');

/* ---------- 行为类型 ---------- */

const BEHAVIOR_TYPES = ['CROWD', 'NO_HELMET', 'FALL', 'INTRUSION', 'OTHER'] as const;

const BEHAVIOR_TYPE_NAME: Record<string, string> = {
  CROWD: '人员聚集',
  NO_HELMET: '未戴安全帽',
  FALL: '倒地',
  INTRUSION: '区域闯入',
  OTHER: '其他',
};

/**
 * 未核实事件的下标。
 * 每 4 条一轮：0 → PENDING、1 → PROCESSING、其余 → DONE，
 * 与真实库种子 04 脚本的分布一致。
 */
function processStatusOf(index: number): string {
  const r = index % 4;
  if (r === 0) return 'PENDING';
  if (r === 1) return 'PROCESSING';
  return 'DONE';
}

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
  cameraCode: string;
  areaName: string;
  warehouseId: string;
  warehouseName: string;
  behaviorType: string;
  behaviorTypeName: string;
  algorithmStatus: string;
  algorithmResult: string | null;
  confidence: number | null;
  personCount: number | null;
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

const EVENT_COUNT = 200;

function buildEventStore(): EventSeed[] {
  const rand = createRandom(20261029);
  const list: EventSeed[] = [];

  for (let i = 0; i < EVENT_COUNT; i += 1) {
    const point = EVENT_POINTS[i % EVENT_POINTS.length];
    const behaviorType = BEHAVIOR_TYPES[i % BEHAVIOR_TYPES.length];
    const algorithmStatus = algorithmStatusOf(i);
    const hasConclusion = algorithmStatus === 'SUCCEEDED' || algorithmStatus === 'MOCK';

    // 算法未成功时结论为 null，不伪造为 NORMAL；约 15% 成功项置信度缺失，验证「暂无」
    let algorithmResult: string | null = null;
    let confidence: number | null = null;
    if (hasConclusion) {
      algorithmResult = pickOne(rand, ['ANOMALY', 'ANOMALY', 'ANOMALY', 'UNCERTAIN', 'NORMAL']);
      confidence = rand() < 0.15 ? null : Number((0.55 + rand() * 0.44).toFixed(4));
    }

    // 最新一条约 8 分钟前，往前每条约 50 分钟（200 条 ≈ 6.9 天，落在默认 7 天窗口内）
    const occurredAgoMs = 8 * MINUTE + i * (50 * MINUTE) + Math.floor(rand() * 20) * MINUTE;

    const processStatus = processStatusOf(i);
    const durationSeconds = hasConclusion ? 60 + Math.floor(rand() * 900) : null;

    list.push({
      eventId: String(3001 + i),
      pointId: point.pointId,
      cameraCode: point.cameraCode,
      areaName: point.pointName,
      warehouseId: point.warehouseId,
      warehouseName: point.warehouseName,
      behaviorType,
      behaviorTypeName: BEHAVIOR_TYPE_NAME[behaviorType] ?? '其他',
      algorithmStatus,
      algorithmResult,
      confidence,
      // 聚集人数只有 CROWD 有；其余留 null 而不是 0，
      // 0 会被读成「画面里没人」，与「该指标不适用」是两回事
      personCount: behaviorType === 'CROWD' ? 5 + (i % 6) : null,
      processStatus,
      eventVersion: processStatus === 'PENDING' ? 1 : 2,
      occurredAgoMs,
      processedAgoMs: processStatus === 'PENDING' ? null : Math.max(30 * SECOND, occurredAgoMs - 30 * MINUTE),
      processedAtAbs: null,
      processedBy: processStatus === 'PENDING' ? null : pickOne(rand, ['zhangsan', 'lisi', 'wangwu']),
      processOpinion:
        processStatus === 'PENDING'
          ? null
          : processStatus === 'PROCESSING'
            ? '已通知现场班组长跟进'
            : '查看事件详情后自动标记为已处理',
      durationSeconds,
      // 已恢复的事件在发生之后 durationSeconds 秒恢复
      recoveredAgoMs: durationSeconds === null ? null : Math.max(10 * SECOND, occurredAgoMs - durationSeconds * SECOND),
      isSimulated: algorithmStatus === 'MOCK',
      modelTag: algorithmStatus === 'MOCK' ? 'person-behavior-mock-v0' : 'person-behavior-detector-v1',
    });
  }

  return list;
}

/** 模块级可变存储：查看即标记会改写它 */
export const EVENT_STORE: EventSeed[] = buildEventStore();

/** 某区域最近一次事件的时刻（用于 latestEventAt），不存在返回 null */
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
 * 事件编号：`PR` + `yyyyMMdd` + 4 位序号（例 `PR202610290001`）。
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
      .forEach((item, i) => map.set(item.eventId, `PR${key}${String(i + 1).padStart(4, '0')}`));
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
    pointId: item.pointId,
    cameraCode: item.cameraCode,
    areaName: item.areaName,
    behaviorType: item.behaviorType,
    behaviorTypeName: item.behaviorTypeName,
    algorithmStatus: item.algorithmStatus,
    algorithmResult: item.algorithmResult,
    // 保留 null：页面据此显示「暂无」而不是 0%
    confidence: item.confidence,
    personCount: item.personCount,
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
      originalUrl: `https://picsum.photos/seed/person-${item.eventId}-ref/640/360`,
      annotatedUrl: null,
      thumbnailUrl: `https://picsum.photos/seed/person-${item.eventId}-ref-t/320/180`,
      capturedAt: formatDateTime(occurredMs - 2 * MINUTE),
      cameraCode: item.cameraCode,
      contentHash: `sha256-ref-${item.eventId}`,
      sortNo: 1,
    },
    {
      evidenceId: `${item.eventId}-EV-BHV`,
      evidenceType: 'BEHAVIOR_FRAME',
      originalUrl: `https://picsum.photos/seed/person-${item.eventId}-frame/640/360`,
      // 标注图与原图分离，不覆盖原图
      annotatedUrl: `https://picsum.photos/seed/person-${item.eventId}-marked/640/360`,
      thumbnailUrl: `https://picsum.photos/seed/person-${item.eventId}-frame-t/320/180`,
      capturedAt: formatDateTime(occurredMs),
      cameraCode: item.cameraCode,
      contentHash: `sha256-bhv-${item.eventId}`,
      sortNo: 2,
    },
  ];

  const recoveredMs = item.recoveredAgoMs === null ? null : now - item.recoveredAgoMs;
  if (recoveredMs !== null && recoveredMs >= occurredMs) {
    evidences.push({
      evidenceId: `${item.eventId}-EV-REC`,
      evidenceType: 'RECOVERY_IMAGE',
      originalUrl: `https://picsum.photos/seed/person-${item.eventId}-rec/640/360`,
      annotatedUrl: null,
      thumbnailUrl: `https://picsum.photos/seed/person-${item.eventId}-rec-t/320/180`,
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

  return [
    {
      recordId: `${item.eventId}-PR-1`,
      statusBefore: 'PENDING',
      statusAfter: item.processStatus,
      processResult: item.processStatus,
      processOpinion: item.processOpinion ?? '',
      processedBy: item.processedBy ?? '',
      processedAt: formatDateTime(processedMs),
      // 与后端同口径：PROCESSING 是人工跟进记 WEB，
      // DONE 是「查看即标记」的自动记录记 SYSTEM，两者不能混成一个值
      source: item.processStatus === 'PROCESSING' ? 'WEB' : 'SYSTEM',
    },
  ];
}

/** 事件详情（含证据、处理记录与算法任务） */
export function buildEventDetail(item: EventSeed, now: number = Date.now()): any {
  const noMap = buildEventNoMap(now);
  const row = buildRow(item, noMap.get(item.eventId) ?? '', now);
  const occurredMs = occurredMsOf(item, now);
  const recoveredMs = item.recoveredAgoMs === null ? null : now - item.recoveredAgoMs;
  const algorithmTasks = materializeAlgorithmTasks(item.eventId, now);

  return {
    ...row,
    eventDesc: `${item.areaName}（${item.cameraCode}）于 ${row.occurredAt} 检测到${item.behaviorTypeName}，已超过判定阈值。`,
    recoveredAt: recoveredMs === null || recoveredMs < occurredMs ? null : formatDateTime(recoveredMs),
    durationSeconds: item.durationSeconds,
    isSimulated: item.isSimulated,
    modelName: item.modelTag,
    evidences: buildEvidences(item, occurredMs, now),
    processRecords: buildProcessRecords(item, now),
    // 算法任务历史随详情一次带回（按开始时间倒序，最多 20 条）
    algorithmTasks,
    latestAlgorithmTask: algorithmTasks.length ? algorithmTasks[0] : null,
  };
}

/**
 * 查看即标记为已处理（与真实后端 `markEventViewed` 同语义）。
 *
 * - 状态、处理人、处理意见、版本号一律由「服务端」决定，不接受前端传入；
 * - 仅 PENDING 会被改写，PROCESSING / DONE 幂等返回（处理中的事件
 *   不能被一次「查看」抢着终结），不重复追加审计、不自增版本；
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

/* ==================== 算法任务（「重新识别」） ==================== */

/**
 * 算法任务内存模型。
 *
 * 形状对齐后端 `PersonAlgorithmTaskRespDTO`（驼峰字段、时间 `yyyy-MM-dd HH:mm:ss`），
 * 人员行为侧的结论类型列是 behaviorType（不是虫害那版的 conclusionType）。
 * 与事件同一套时间约定：历史任务只存**偏移量**，每次请求基于 now 现算；
 * 本次运行提交的实时任务存绝对毫秒（提交动作真实发生）。
 *
 * 生命周期（前端轮询逻辑必须能在 Mock 下被验证）：
 *   提交 → QUEUED；第 1、2 次查询 → RUNNING；第 3 次起 → SUCCEEDED。
 * **不允许一查询就成功。**
 */
interface AlgoTaskSeed {
  taskId: string;
  eventId: string;
  pointId: string;
  cameraCode: string;
  executionStatus: string;
  conclusion: string | null;
  behaviorType: string | null;
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

/** 历史任务状态：镜像事件的算法状态（MOCK 归一为 SUCCEEDED，模拟由 simulated 表达） */
function legacyTaskStatusOf(algorithmStatus: string): string {
  if (algorithmStatus === 'MOCK') return 'SUCCEEDED';
  return algorithmStatus;
}

function buildLegacyAlgorithmTasks(): AlgoTaskSeed[] {
  return EVENT_STORE.map((item, idx) => {
    const status = legacyTaskStatusOf(item.algorithmStatus);
    const succeeded = status === 'SUCCEEDED';
    const durationMs = succeeded || status === 'FAILED' || status === 'TIMEOUT' ? 18 + ((idx * 53) % 403) : null;
    return {
      taskId: `PATASK${item.eventId}`,
      eventId: item.eventId,
      pointId: item.pointId,
      cameraCode: item.cameraCode,
      executionStatus: status,
      conclusion: succeeded ? (item.algorithmResult ?? 'ANOMALY') : null,
      behaviorType: succeeded ? item.behaviorType : null,
      confidence: succeeded ? item.confidence : null,
      modelName: succeeded ? 'mock-person-detector' : null,
      modelVersion: succeeded ? 'mock-1.0.0' : null,
      algorithmVersion: succeeded ? 'mock-adapter-1.0.0' : null,
      configVersion: 'mock-config-1',
      inferenceDurationMs: durationMs,
      startedAgoMs: item.occurredAgoMs - 30 * SECOND,
      finishedAgoMs: succeeded && durationMs !== null ? item.occurredAgoMs - 30 * SECOND + durationMs : null,
      startedAbs: null,
      finishedAbs: null,
      detectedCount: succeeded ? Math.max(1, item.personCount ?? 1) : null,
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

/** 模块级可变存储：历史任务 + 本次运行「重新识别」提交的实时任务 */
export const ALGO_TASK_STORE: AlgoTaskSeed[] = buildLegacyAlgorithmTasks();

let algoTaskSeq = 0;

/** 任务行物化：时间按 now 现算，字段与后端 PersonAlgorithmTaskRespDTO 同形 */
function materializeAlgoTask(task: AlgoTaskSeed, now: number): any {
  const startedMs =
    task.startedAbs !== null ? task.startedAbs : task.startedAgoMs !== null ? now - task.startedAgoMs : null;
  const finishedMs =
    task.finishedAbs !== null ? task.finishedAbs : task.finishedAgoMs !== null ? now - task.finishedAgoMs : null;

  return {
    taskId: task.taskId,
    taskNo: task.taskId,
    eventId: task.eventId,
    executionStatus: task.executionStatus,
    conclusion: task.conclusion,
    behaviorType: task.behaviorType,
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
  return ALGO_TASK_STORE.filter((t) => t.eventId === eventId)
    .map((t) => ({ seed: t, row: materializeAlgoTask(t, now) }))
    .sort((a, b) => {
      const av = a.row.startedAt ? new Date(a.row.startedAt.replace(' ', 'T')).getTime() : -1;
      const bv = b.row.startedAt ? new Date(b.row.startedAt.replace(' ', 'T')).getTime() : -1;
      return bv - av;
    })
    .slice(0, 20)
    .map((x) => x.row);
}

/**
 * 提交「重新识别」：立即返回 QUEUED 任务号，不同步出结果。
 * @returns 与后端 PersonAlgorithmRerunRespDTO 同形
 */
export function rerunAlgorithmTask(eventId: string, now: number = Date.now()): any {
  const event = findEvent(eventId);
  algoTaskSeq += 1;
  const taskId = `MOCK-PERSON-TASK-${now.toString(36)}${algoTaskSeq}`;
  ALGO_TASK_STORE.push({
    taskId,
    eventId,
    pointId: event?.pointId ?? '',
    cameraCode: event?.cameraCode ?? '',
    executionStatus: 'QUEUED',
    conclusion: null,
    behaviorType: null,
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
 * 算法列（algorithmStatus / algorithmResult / confidence），**不动处理状态**。
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
      task.executionStatus = 'SUCCEEDED';
      task.conclusion = event?.algorithmResult ?? 'ANOMALY';
      task.behaviorType = event?.behaviorType ?? null;
      task.confidence = 0.86;
      task.modelName = 'mock-person-detector';
      task.modelVersion = 'mock-1.0.0';
      task.algorithmVersion = 'mock-adapter-1.0.0';
      task.inferenceDurationMs = 120 + task.pollCount;
      task.startedAbs = task.startedAbs ?? now;
      task.finishedAbs = now;
      task.detectedCount = Math.max(1, event?.personCount ?? 1);
      if (event) {
        // 回写事件：与后端「成功才回写算法列」同语义
        event.algorithmStatus = 'SUCCEEDED';
        event.algorithmResult = task.conclusion;
        event.confidence = task.confidence;
      }
    }
  }

  return materializeAlgoTask(task, now);
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
 * 与真实后端同形（注意 `playableUrl` / `expiresAt` 不可播放时是**空串**，
 * 不是 null）：
 * `{"pointId":"101","canPlay":false,"expiresAt":"","platformCameraId":"C001",
 *   "playableUrl":"","protocol":"MP4","cameraCode":"CAM-001",
 *   "unavailableReason":"CAMERA_OFFLINE"}`
 */
export function createStreamInfo(pointId: string, now: number = Date.now()): any {
  const point = POINT_DEFS.find((p) => p.pointId === pointId);

  const base = {
    pointId,
    cameraCode: point?.cameraCode ?? '',
    platformCameraId: point?.platformCameraId ?? '',
    canPlay: false as boolean,
    expiresAt: '',
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
    cameraCode: point.cameraCode,
    platformCameraId: point.platformCameraId,
    canPlay: true,
    expiresAt: formatDateTime(now + STREAM_TTL_MS),
    playableUrl: MOCK_MP4_URL,
    protocol: 'MP4',
    unavailableReason: null,
  };
}

/** 行为类型编码 → 中文名（导出与兜底展示用） */
export function behaviorTypeNameOf(code: string): string {
  return BEHAVIOR_TYPE_NAME[code] ?? '其他';
}

/** 处理状态编码 → 中文名（导出用，与页面三态文案一致） */
export function processStatusNameOf(code: string): string {
  if (code === 'PENDING') return '未核实';
  if (code === 'PROCESSING') return '处理中';
  if (code === 'DONE') return '已处理';
  return code ?? '';
}
