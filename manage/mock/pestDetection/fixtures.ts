/**
 * 粮面虫害智能识别 Mock - 共享假数据生成器
 *
 * 形状来源（三者必须一致，否则切到真实后端会静默出错）：
 * - `src/api/pestDetection.ts` 的 `normalize*` 函数（页面实际消费的字段）
 * - `src/api/model/pestDetectionModel.ts` 的请求/响应类型
 * - 真实后端约定（字段名驼峰、时间为 `yyyy-MM-dd HH:mm:ss` 字符串）
 *
 * ⚠️ 时间戳的生成方式（本文件最重要的一条约定）
 * 存储层只保存**相对当前时刻的偏移量**（`occurredAgoMs` 等），
 * 所有对外时间字符串都在每次调用 `materialize*` 时基于 `Date.now()` 现算。
 *
 * 原因：`shared/displayStatus.computeDisplayStatus` 用 **90 分钟心跳阈值**
 * 判定设备离线。若心跳时间在模块加载时固定一次，dev server 跑久后所有点位
 * 都会被误判成「离线」，Mock 模式下整页只剩离线，等于没有假数据。
 *
 * 密度判定：density > densityThreshold 即密度超标。阈值默认值 5.00 头/kg
 * 与后端 PestDetectionConstant.DEFAULT_DENSITY_THRESHOLD 保持一致（此处仅为 Mock）。
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

/* ---------- 密度判定阈值默认（与后端常量一致，仅 Mock 副本） ---------- */

const DEFAULT_DENSITY_THRESHOLD = 5.0;

/* ---------- 粮面点位基础数据 ---------- */

const WAREHOUSES = [
  { id: '9001', name: '一号仓' },
  { id: '9002', name: '二号仓' },
  { id: '9003', name: '三号仓' },
  { id: '9004', name: '四号仓' },
];

const GRAIN_TYPES = ['小麦', '玉米', '稻谷'];
const GRAIN_TONNAGES = [5000, 3000, 6000, 8000];

interface PointDef {
  pointId: string;
  pointName: string;
  warehouseId: string;
  warehouseName: string;
  grainType: string;
  grainTonnage: number;
  cameraCode: string;
  platformCameraId: string;
  deviceStatus: 'ONLINE' | 'OFFLINE' | 'UNKNOWN';
  /** 告警状态，复用 AlarmStatus 语义（ACTIVE = 页面「告警」） */
  alarmStatus: 'NORMAL' | 'ACTIVE' | 'RECOVERED';
  /**
   * 心跳距**现在**的秒数。每次请求基于 Date.now() 现算，
   * 因此小于 90 分钟的都是「新鲜心跳」，大于则演示离线判定。
   */
  heartbeatAgoSec: number | null;
  /** 播放分支覆盖，见 createStreamInfo */
  streamBranch?: 'HLS' | 'EXPIRED';
}

/**
 * 9 个粮面点位（与真实库量级一致）。
 * 状态分布覆盖页面全部展示态：正常 / 告警 / 离线 / 未知。
 */
const POINT_DEFS: PointDef[] = [
  {
    pointId: '101',
    pointName: '一号仓粮面',
    warehouseId: WAREHOUSES[0].id,
    warehouseName: WAREHOUSES[0].name,
    grainType: GRAIN_TYPES[0],
    grainTonnage: GRAIN_TONNAGES[0],
    cameraCode: 'CAM-001',
    platformCameraId: 'C001',
    deviceStatus: 'ONLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 45 * SECOND,
  },
  {
    pointId: '102',
    pointName: '一号仓粮面东',
    warehouseId: WAREHOUSES[0].id,
    warehouseName: WAREHOUSES[0].name,
    grainType: GRAIN_TYPES[1],
    grainTonnage: GRAIN_TONNAGES[1],
    cameraCode: 'CAM-002',
    platformCameraId: 'C002',
    // 在线且告警中：页面必须出现「告警」卡片
    deviceStatus: 'ONLINE',
    alarmStatus: 'ACTIVE',
    heartbeatAgoSec: 20 * SECOND,
  },
  {
    pointId: '103',
    pointName: '二号仓粮面',
    warehouseId: WAREHOUSES[1].id,
    warehouseName: WAREHOUSES[1].name,
    grainType: GRAIN_TYPES[2],
    grainTonnage: GRAIN_TONNAGES[2],
    cameraCode: 'CAM-003',
    platformCameraId: 'C003',
    deviceStatus: 'ONLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 130 * SECOND,
  },
  {
    pointId: '104',
    pointName: '二号仓粮面西',
    warehouseId: WAREHOUSES[1].id,
    warehouseName: WAREHOUSES[1].name,
    grainType: GRAIN_TYPES[0],
    grainTonnage: GRAIN_TONNAGES[3],
    cameraCode: 'CAM-004',
    platformCameraId: 'C004',
    deviceStatus: 'ONLINE',
    alarmStatus: 'RECOVERED',
    heartbeatAgoSec: 190 * SECOND,
    // 一期前端只有原生 <video>，此分支验证「该视频协议暂不支持播放」的明确提示
    streamBranch: 'HLS',
  },
  {
    pointId: '105',
    pointName: '三号仓粮面',
    warehouseId: WAREHOUSES[2].id,
    warehouseName: WAREHOUSES[2].name,
    grainType: GRAIN_TYPES[0],
    grainTonnage: GRAIN_TONNAGES[0],
    cameraCode: 'CAM-005',
    platformCameraId: 'C005',
    // 明确离线：deviceStatus=OFFLINE，且心跳也已陈旧
    deviceStatus: 'OFFLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 182 * MINUTE,
  },
  {
    pointId: '106',
    pointName: '三号仓粮面中',
    warehouseId: WAREHOUSES[2].id,
    warehouseName: WAREHOUSES[2].name,
    grainType: GRAIN_TYPES[1],
    grainTonnage: GRAIN_TONNAGES[1],
    cameraCode: 'CAM-006',
    platformCameraId: 'C006',
    deviceStatus: 'ONLINE',
    alarmStatus: 'ACTIVE',
    heartbeatAgoSec: 60 * SECOND,
    // 在线但地址过期：验证「播放地址已过期」+ 重试按钮
    streamBranch: 'EXPIRED',
  },
  {
    pointId: '107',
    pointName: '四号仓粮面',
    warehouseId: WAREHOUSES[3].id,
    warehouseName: WAREHOUSES[3].name,
    grainType: GRAIN_TYPES[2],
    grainTonnage: GRAIN_TONNAGES[2],
    cameraCode: 'CAM-007',
    platformCameraId: 'C007',
    deviceStatus: 'ONLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 210 * SECOND,
  },
  {
    pointId: '108',
    pointName: '四号仓粮面北',
    warehouseId: WAREHOUSES[3].id,
    warehouseName: WAREHOUSES[3].name,
    grainType: GRAIN_TYPES[0],
    grainTonnage: GRAIN_TONNAGES[3],
    cameraCode: 'CAM-008',
    platformCameraId: 'C008',
    // 真实故障形态：库里 device_status 仍是 ONLINE，但心跳超过 90 分钟，
    // 页面必须判离线。这条用例专门盯着心跳阈值改动。
    deviceStatus: 'ONLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 126 * MINUTE,
  },
  {
    pointId: '109',
    pointName: '四号仓粮面临时',
    warehouseId: WAREHOUSES[3].id,
    warehouseName: WAREHOUSES[3].name,
    grainType: GRAIN_TYPES[1],
    grainTonnage: GRAIN_TONNAGES[0],
    cameraCode: 'CAM-009',
    platformCameraId: 'C009',
    // UNKNOWN：设备状态无法判定，页面显示「未知」（虚线卡片），不得显示成离线
    deviceStatus: 'UNKNOWN',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: null,
  },
];

/**
 * 粮面点位状态列表。
 *
 * @param now 请求时刻；心跳与最新记录时间都由它推出，缺省取 `Date.now()`
 */
export function createMonitorPointList(now: number = Date.now()): any[] {
  return POINT_DEFS.map((p, index) => {
    const latest = latestEventOf(p.pointId, now);
    return {
      pointId: p.pointId,
      pointName: p.pointName,
      warehouseId: p.warehouseId,
      warehouseName: p.warehouseName,
      grainType: p.grainType,
      grainTonnage: p.grainTonnage,
      cameraCode: p.cameraCode,
      platformCameraId: p.platformCameraId,
      deviceStatus: p.deviceStatus,
      alarmStatus: p.alarmStatus,
      lastHeartbeatAt: p.heartbeatAgoSec === null ? null : formatDateTime(now - p.heartbeatAgoSec),
      latestEventAt: latest === null ? null : formatDateTime(latest),
      sortNo: index + 1,
    };
  });
}

/** 未知的点位不产生检测记录，其余都可能 */
function pointEligibleForEvent(def: PointDef): boolean {
  return def.deviceStatus !== 'UNKNOWN';
}

const EVENT_POINTS = POINT_DEFS.filter(pointEligibleForEvent);

/* ---------- 采样位置 / 虫种 / 结论类型 ---------- */

const SAMPLE_POSITIONS = ['东墙地上层', '中墙下层', '西北墙中层', '东墙混上层', '南墙表层', '中央取样点'];
const INSECT_SPECIES = ['玉米象', '赤拟谷盗', '谷蠹', '麦蛾'];

const CONCLUSION_TYPES = ['GRAIN_INSECT', 'DENSITY_HIGH', 'MOLD', 'OTHER'] as const;

const CONCLUSION_TYPE_NAME: Record<string, string> = {
  GRAIN_INSECT: '粮面害虫',
  DENSITY_HIGH: '密度超标',
  MOLD: '霉变',
  OTHER: '其他或未知',
};

/** 未核实记录的下标（每次查看会被真实改写，这里只决定初始分布） */
const PENDING_INDEXES = new Set([0, 3, 7, 12, 19]);

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

/* ---------- 记录存储 ---------- */

/**
 * 记录条目在内存里只存**偏移量**与可变状态，不存绝对时间字符串。
 *
 * `processedAtAbs` 例外：它是「某人真的点了查看」这一动作发生的时间，
 * 记录的是当时的绝对毫秒，天然落在过去，不会随进程运行时长而失真。
 */
interface EventSeed {
  eventId: string;
  pointId: string;
  cameraCode: string;
  pointName: string;
  samplePosition: string;
  warehouseId: string;
  warehouseName: string;
  grainType: string;
  conclusionType: string;
  conclusionTypeName: string;
  insectSpecies: string | null;
  density: number;
  densityThreshold: number;
  algorithmStatus: string;
  algorithmResult: string | null;
  confidence: number | null;
  processStatus: string;
  eventVersion: number;
  /** 采样时间距今毫秒数（生成时确定，展示时每次换算） */
  occurredAgoMs: number;
  /** 已处理记录的处理时间距今毫秒数（历史种子数据用） */
  processedAgoMs: number | null;
  /** 本次运行中被「查看即标记」写下的绝对时间 */
  processedAtAbs: number | null;
  processedBy: string | null;
  processOpinion: string | null;
  isSimulated: boolean;
  modelTag: string;
}

const EVENT_COUNT = 30;

function buildEventStore(): EventSeed[] {
  const rand = createRandom(20261029);
  const list: EventSeed[] = [];

  for (let i = 0; i < EVENT_COUNT; i += 1) {
    const point = EVENT_POINTS[i % EVENT_POINTS.length];
    const conclusionType = CONCLUSION_TYPES[i % CONCLUSION_TYPES.length];
    const algorithmStatus = algorithmStatusOf(i);
    const hasConclusion = algorithmStatus === 'SUCCEEDED' || algorithmStatus === 'MOCK';

    // 密度与阈值：密度超标类 density > 阈值，其余 <= 阈值。阈值固定用默认值。
    const densityThreshold = DEFAULT_DENSITY_THRESHOLD;
    const density =
      conclusionType === 'DENSITY_HIGH'
        ? Number((densityThreshold + 1 + rand() * 3).toFixed(2))
        : Number((rand() * densityThreshold).toFixed(2));

    // 虫种：霉变/其他无虫种，害虫/密度超标给出虫种
    const insectSpecies =
      conclusionType === 'MOLD' || conclusionType === 'OTHER' ? null : pickOne(rand, INSECT_SPECIES);

    // 算法未成功时结论为 null，不伪造为 NORMAL；约 15% 成功项置信度缺失，验证「暂无」
    let algorithmResult: string | null = null;
    let confidence: number | null = null;
    if (hasConclusion) {
      algorithmResult = conclusionType === 'OTHER' ? 'UNCERTAIN' : 'ANOMALY';
      confidence = rand() < 0.15 ? null : Number((0.55 + rand() * 0.44).toFixed(4));
    }

    // 最新一条约 8 分钟前，往前每条约 4.7 小时（30 条 ≈ 5.7 天，落在默认 7 天窗口内）
    const occurredAgoMs = 8 * MINUTE + i * (4.7 * HOUR) + Math.floor(rand() * 40) * MINUTE;

    const isPending = PENDING_INDEXES.has(i);
    // 三态分布：未核实 / 处理中 / 已处理
    let processStatus: string;
    if (isPending) processStatus = 'PENDING';
    else if (i % 3 === 1) processStatus = 'PROCESSING';
    else processStatus = 'DONE';

    list.push({
      eventId: String(3001 + i),
      pointId: point.pointId,
      cameraCode: point.cameraCode,
      pointName: point.pointName,
      samplePosition: SAMPLE_POSITIONS[i % SAMPLE_POSITIONS.length],
      warehouseId: point.warehouseId,
      warehouseName: point.warehouseName,
      grainType: point.grainType,
      conclusionType,
      conclusionTypeName: CONCLUSION_TYPE_NAME[conclusionType] ?? '其他或未知',
      insectSpecies,
      density,
      densityThreshold,
      algorithmStatus,
      algorithmResult,
      confidence,
      processStatus,
      eventVersion: processStatus === 'PENDING' ? 1 : 2,
      occurredAgoMs,
      processedAgoMs: processStatus === 'PENDING' ? null : Math.max(30 * SECOND, occurredAgoMs - 30 * MINUTE),
      processedAtAbs: null,
      processedBy: processStatus === 'PENDING' ? null : pickOne(rand, ['zhangsan', 'lisi', 'wangwu']),
      processOpinion: processStatus === 'PENDING' ? null : '已通知粮情员现场复核并记录处置。',
      isSimulated: algorithmStatus === 'MOCK',
      modelTag: algorithmStatus === 'MOCK' ? 'pest-mock-v0' : 'pest-detector-v1',
    });
  }

  return list;
}

/** 模块级可变存储：查看即标记会改写它 */
export const EVENT_STORE: EventSeed[] = buildEventStore();

/** 某点位最近一次记录的时刻（用于 latestEventAt），不存在返回 null */
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

/* ---------- 记录编号 ---------- */

/**
 * 记录编号：`PT` + `yyyyMMdd` + 4 位序号（例 `PT202607290001`）。
 *
 * 序号按「同一自然日内、采样时间升序」排，且**每次请求整体重算**：
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
      .forEach((item, i) => map.set(item.eventId, `PT${key}${String(i + 1).padStart(4, '0')}`));
  });
  return map;
}

/* ---------- 生成对外响应对象 ---------- */

/** 列表项 / 最近记录行（字段与真实后端 listEventByCondition 的 data 元素同形） */
function buildRow(item: EventSeed, eventNo: string, now: number): any {
  const processedMs = processedMsOf(item, now);

  return {
    eventId: item.eventId,
    eventNo,
    occurredAt: formatDateTime(occurredMsOf(item, now)),
    warehouseId: item.warehouseId,
    warehouseName: item.warehouseName,
    // 记录挂在粮面点位上，后端列名为 point_id
    pointId: item.pointId,
    cameraCode: item.cameraCode,
    samplePosition: item.samplePosition,
    grainType: item.grainType,
    conclusionType: item.conclusionType,
    conclusionTypeName: item.conclusionTypeName,
    insectSpecies: item.insectSpecies,
    density: item.density,
    densityThreshold: item.densityThreshold,
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

function buildEvidences(item: EventSeed, occurredMs: number): any[] {
  return [
    {
      evidenceId: `${item.eventId}-EV-REF`,
      evidenceType: 'NORMAL_REFERENCE',
      originalUrl: `https://picsum.photos/seed/pest-${item.eventId}-ref/640/360`,
      annotatedUrl: null,
      thumbnailUrl: `https://picsum.photos/seed/pest-${item.eventId}-ref-t/320/180`,
      capturedAt: formatDateTime(occurredMs - 2 * MINUTE),
      cameraCode: item.cameraCode,
      contentHash: `sha256-ref-${item.eventId}`,
      sortNo: 1,
    },
    {
      evidenceId: `${item.eventId}-EV-PEST`,
      evidenceType: 'PEST_FRAME',
      originalUrl: `https://picsum.photos/seed/pest-${item.eventId}-frame/640/360`,
      // 标注图与原图分离，不覆盖原图
      annotatedUrl: `https://picsum.photos/seed/pest-${item.eventId}-marked/640/360`,
      thumbnailUrl: `https://picsum.photos/seed/pest-${item.eventId}-frame-t/320/180`,
      capturedAt: formatDateTime(occurredMs),
      cameraCode: item.cameraCode,
      contentHash: `sha256-pest-${item.eventId}`,
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

/** 记录详情（含证据、处理记录与算法任务） */
export function buildEventDetail(item: EventSeed, now: number = Date.now()): any {
  const noMap = buildEventNoMap(now);
  const row = buildRow(item, noMap.get(item.eventId) ?? '', now);
  const occurredMs = occurredMsOf(item, now);

  // 算法任务历史随详情一次带回（与后端 getEventDetail 扩展同形）
  const algorithmTasks = materializeAlgorithmTasks(item.eventId, now);

  return {
    ...row,
    eventDesc:
      `${item.cameraCode} ${item.pointName}（${item.samplePosition}）于 ${row.occurredAt} ` +
      `检出${item.conclusionTypeName}，密度 ${item.density} 头/kg，阈值 ${item.densityThreshold} 头/kg。`,
    recoveredAt: null,
    durationSeconds: null,
    isSimulated: item.isSimulated,
    // 密度是否超标：后端派生量，Mock 同样现算
    densityExceeded: item.density > item.densityThreshold,
    modelName: item.modelTag,
    evidences: buildEvidences(item, occurredMs),
    processRecords: buildProcessRecords(item, now),
    algorithmTasks,
    latestAlgorithmTask: algorithmTasks.length ? algorithmTasks[0] : null,
  };
}

/**
 * 查看即标记为已处理（与真实后端 `markEventViewed` 同语义）。
 *
 * - 状态、处理人、处理意见、版本号一律由「服务端」决定，不接受前端传入；
 * - 仅 PENDING 会被改写为终态 DONE，其余幂等返回，不重复追加审计、不自增版本；
 * @returns 是否发生了实际改写
 */
export function markViewed(item: EventSeed, now: number = Date.now()): boolean {
  if (item.processStatus === 'DONE') return false;

  item.processStatus = 'DONE';
  item.processOpinion = '查看检测记录详情后自动标记为已处理';
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
 * 形状对齐后端 `PestAlgorithmTaskRespDTO`（驼峰字段、时间 `yyyy-MM-dd HH:mm:ss`）。
 * 与事件同一套时间约定：历史任务只存**偏移量**，每次请求基于 now 现算；
 * 本次运行提交的实时任务存绝对毫秒（提交动作真实发生）。
 *
 * 生命周期（对齐契约 §4，前端轮询逻辑必须能在 Mock 下被验证）：
 *   提交 → QUEUED；第 1、2 次查询 → RUNNING；第 3 次起 → SUCCEEDED。
 * **不允许一查询就成功。**
 */
interface AlgoTaskSeed {
  taskId: string;
  eventId: string;
  pointId: string;
  cameraCode: string;
  warehouseId: string;
  taskType: string;
  mediaType: string;
  mediaRefs: string;
  executionStatus: string;
  conclusion: string | null;
  conclusionType: string | null;
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
  detections: string | null;
  simulated: boolean;
  errorCode: string | null;
  errorMessage: string | null;
  traceId: string;
  /** 本次运行提交的任务（走轮询状态机） */
  live: boolean;
  pollCount: number;
}

/** 密度超标类计数必须 > 阈值；其余至少 1 个，OTHER（无法判断）不伪造检出 */
function detectCountOf(item: EventSeed): number {
  if (item.conclusionType === 'DENSITY_HIGH') {
    return Math.max(Math.round(item.density), Math.round(item.densityThreshold) + 2);
  }
  if (item.conclusionType === 'OTHER') return 0;
  return Math.max(Math.round(item.density), 1);
}

/** detections JSON：虫种 + 计数（1kg 采样口径，count 即密度头数），与事件结论列自洽 */
function buildDetectionsJson(item: EventSeed, count: number): string {
  if (item.conclusionType === 'OTHER') return '[]';
  if (item.conclusionType === 'MOLD') {
    return JSON.stringify([{ category: 'mold_patch', count, confidence: 0.88, bbox: '[300,150,900,600]' }]);
  }
  return JSON.stringify([
    {
      category: 'grain_insect',
      species: item.insectSpecies ?? '玉米象',
      count,
      confidence: 0.9,
      bbox: '[120,80,460,420]',
    },
  ]);
}

/** 历史任务状态：镜像事件的算法状态（MOCK 归一为 SUCCEEDED，模拟由 simulated 表达） */
function legacyTaskStatusOf(algorithmStatus: string): string {
  if (algorithmStatus === 'MOCK') return 'SUCCEEDED';
  return algorithmStatus;
}

function legacyMediaRefs(eventId: string): string {
  return JSON.stringify([`mock://pest/${eventId}/frame`, `mock://pest/${eventId}/normal`]);
}

function buildLegacyAlgorithmTasks(): AlgoTaskSeed[] {
  return EVENT_STORE.map((item, idx) => {
    const status = legacyTaskStatusOf(item.algorithmStatus);
    const succeeded = status === 'SUCCEEDED';
    const count = detectCountOf(item);
    const durationMs = succeeded || status === 'FAILED' || status === 'TIMEOUT' ? 18 + ((idx * 53) % 403) : null;
    return {
      taskId: `PSTASK${item.eventId}`,
      eventId: item.eventId,
      pointId: item.pointId,
      cameraCode: item.cameraCode,
      warehouseId: item.warehouseId,
      taskType: 'PEST_DETECT',
      mediaType: 'SINGLE_IMAGE',
      mediaRefs: legacyMediaRefs(item.eventId),
      executionStatus: status,
      conclusion: succeeded ? (item.conclusionType === 'OTHER' ? 'UNCERTAIN' : 'ANOMALY') : null,
      conclusionType: succeeded ? item.conclusionType : null,
      confidence: succeeded ? item.confidence : null,
      modelName: succeeded ? 'mock-pest-detector' : null,
      modelVersion: succeeded ? 'mock-1.0.0' : null,
      algorithmVersion: succeeded ? 'mock-adapter-1.0.0' : null,
      configVersion: 'mock-config-1',
      inferenceDurationMs: durationMs,
      startedAgoMs: item.occurredAgoMs - 30 * SECOND,
      finishedAgoMs: succeeded && durationMs !== null ? item.occurredAgoMs - 30 * SECOND + durationMs : null,
      startedAbs: null,
      finishedAbs: null,
      detections: succeeded ? buildDetectionsJson(item, count) : null,
      simulated: true,
      errorCode: status === 'FAILED' ? 'ALGORITHM_ERROR' : status === 'TIMEOUT' ? 'ALGORITHM_TIMEOUT' : null,
      errorMessage:
        status === 'FAILED'
          ? '模拟算法执行失败（历史样例，非真实故障）'
          : status === 'TIMEOUT'
            ? '模拟算法执行超时（历史样例，非真实故障）'
            : null,
      traceId: `seedtask-${String(idx + 1).padStart(4, '0')}`,
      live: false,
      pollCount: 0,
    };
  });
}

/** 模块级可变存储：历史任务 + 本次运行「重新识别」提交的实时任务 */
export const ALGO_TASK_STORE: AlgoTaskSeed[] = buildLegacyAlgorithmTasks();

let algoTaskSeq = 0;

/** 任务行物化：时间按 now 现算，字段与后端 PestAlgorithmTaskRespDTO 同形 */
function materializeAlgoTask(task: AlgoTaskSeed, now: number): any {
  const startedMs =
    task.startedAbs !== null ? task.startedAbs : task.startedAgoMs !== null ? now - task.startedAgoMs : null;
  const finishedMs =
    task.finishedAbs !== null ? task.finishedAbs : task.finishedAgoMs !== null ? now - task.finishedAgoMs : null;
  const boxes = (() => {
    try {
      const parsed = JSON.parse(task.detections ?? '[]');
      return Array.isArray(parsed) ? parsed.length : null;
    } catch {
      return null;
    }
  })();
  return {
    taskId: task.taskId,
    id: `mock-${task.taskId}`,
    taskNo: task.taskId,
    eventId: task.eventId,
    pointId: task.pointId,
    cameraCode: task.cameraCode,
    taskType: task.taskType,
    executionStatus: task.executionStatus,
    conclusion: task.conclusion,
    conclusionType: task.conclusionType,
    confidence: task.confidence,
    confidenceThreshold: 0.75,
    modelName: task.modelName,
    modelVersion: task.modelVersion,
    algorithmVersion: task.algorithmVersion,
    configVersion: task.configVersion,
    startedAt: startedMs === null ? null : formatDateTime(startedMs),
    finishedAt: finishedMs === null ? null : formatDateTime(finishedMs),
    inferenceDurationMs: task.inferenceDurationMs,
    detectedCount: task.detections === null ? null : boxes,
    detections: task.detections,
    mediaRefs: task.mediaRefs,
    simulated: task.simulated,
    errorCode: task.errorCode,
    errorMessage: task.errorMessage,
    traceId: task.traceId,
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
 * @returns 与后端 PestAlgorithmRerunRespDTO 同形
 */
export function rerunAlgorithmTask(eventId: string, now: number = Date.now()): any {
  const event = findEvent(eventId);
  algoTaskSeq += 1;
  const taskId = `MOCK-TASK-${now.toString(36)}${algoTaskSeq}`;
  ALGO_TASK_STORE.push({
    taskId,
    eventId,
    pointId: event?.pointId ?? '',
    cameraCode: event?.cameraCode ?? '',
    warehouseId: event?.warehouseId ?? '',
    taskType: 'PEST_DETECT',
    mediaType: 'SINGLE_IMAGE',
    mediaRefs: legacyMediaRefs(eventId),
    executionStatus: 'QUEUED',
    conclusion: null,
    conclusionType: null,
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
    detections: null,
    simulated: true,
    errorCode: null,
    errorMessage: null,
    traceId: `rerun-${algoTaskSeq}`,
    live: true,
    pollCount: 0,
  });
  return { taskId, executionStatus: 'QUEUED', simulated: true };
}

/**
 * 轮询实时任务：QUEUED → RUNNING → （第 3 次起）SUCCEEDED，成功后回写事件
 * 的 algorithm_* 与结论列（虫种/密度/结论），**不动处理状态**。
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
      const succeededConclusionType = event?.conclusionType ?? 'GRAIN_INSECT';
      task.executionStatus = 'SUCCEEDED';
      task.conclusion = succeededConclusionType === 'OTHER' ? 'UNCERTAIN' : 'ANOMALY';
      task.conclusionType = succeededConclusionType;
      task.confidence = 0.86;
      task.modelName = 'mock-pest-detector';
      task.modelVersion = 'mock-1.0.0';
      task.algorithmVersion = 'mock-adapter-1.0.0';
      task.inferenceDurationMs = 120 + task.pollCount;
      task.startedAbs = task.startedAbs ?? now;
      task.finishedAbs = now;
      if (event) {
        const count = detectCountOf(event);
        task.detections = buildDetectionsJson(event, count);
        // 回写事件：与后端「成功才回写结论列」同语义
        event.algorithmStatus = 'SUCCEEDED';
        event.algorithmResult = task.conclusion;
        event.confidence = task.confidence;
        if (succeededConclusionType !== 'OTHER') {
          event.density = count;
          if (succeededConclusionType !== 'MOLD') {
            event.insectSpecies = event.insectSpecies ?? '玉米象';
          }
        }
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
 * 与真实后端约定同形（注意 `playableUrl` / `expiresAt` 不可播放时是**空串**，不是 null）。
 */
export function createStreamInfo(pointId: string, now: number = Date.now()): any {
  const point = POINT_DEFS.find((p) => p.pointId === pointId);

  const base = {
    pointId,
    canPlay: false as boolean,
    expiresAt: '',
    platformCameraId: point?.platformCameraId ?? '',
    cameraCode: point?.cameraCode ?? '',
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
    canPlay: true,
    expiresAt: formatDateTime(now + STREAM_TTL_MS),
    platformCameraId: point.platformCameraId,
    cameraCode: point.cameraCode,
    playableUrl: MOCK_MP4_URL,
    protocol: 'MP4',
    unavailableReason: null,
  };
}

/** 结论类型编码 → 中文名（导出与兜底展示用） */
export function conclusionTypeNameOf(code: string): string {
  return CONCLUSION_TYPE_NAME[code] ?? '其他或未知';
}
