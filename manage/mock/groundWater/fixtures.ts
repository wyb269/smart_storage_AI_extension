/**
 * 地面积水监测 Mock - 共享假数据生成器
 *
 * 形状来源（三者必须一致，否则切到真实后端会静默出错）：
 * - `src/api/groundWater.ts` 的 `normalize*` 函数（页面实际消费的字段）
 * - `src/api/model/groundWaterModel.ts` 的请求/响应类型
 * - 真实后端约定：字段名驼峰、时间为 `yyyy-MM-dd HH:mm:ss` 字符串
 *
 * ⚠️ 时间戳的生成方式（本文件最重要的一条约定）
 * 存储层只保存**相对当前时刻的偏移量**（`occurredAgoMs` 等），
 * 所有对外时间字符串都在每次调用 `materialize*` 时基于 `Date.now()` 现算。
 *
 * 原因：`shared/displayStatus.computeDisplayStatus` 用 **90 分钟心跳阈值**
 * （`shared/constants.HEARTBEAT_TIMEOUT_MS`）判定设备离线。若心跳时间在模块
 * 加载时固定一次，dev server 跑久后所有点位都会被误判成「离线」，
 * Mock 模式下整页只剩离线，等于没有假数据（仓门那一版踩过这个坑，
 * 它的 `BASE_TIME` 就是模块加载时算的）。这里连事件时间也一并改成每次现算，
 * 保证默认「最近 7 天」查询窗口与心跳新鲜度在任何运行时刻都成立。
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
 * 与真实后端返回的 `"lastHeartbeatAt":"2026-10-23 13:18:02"` 逐字符一致。
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

/* ---------- 监测点位基础数据 ---------- */

const WAREHOUSES = [
  { id: 'WH-001', name: '一号粮库' },
  { id: 'WH-002', name: '二号粮库' },
  { id: 'WH-003', name: '三号粮库' },
  { id: 'WH-004', name: '散粮库' },
];

interface PointDef {
  pointId: string;
  /** 卡片标题，原型口径是点位名而不是摄像头名 */
  pointName: string;
  cameraCode: string;
  warehouseId: string;
  warehouseName: string;
  platformCameraId: string;
  deviceStatus: 'ONLINE' | 'OFFLINE' | 'UNKNOWN';
  /**
   * 积水告警状态，复用 AlarmStatus 语义。
   * ⚠️ 必须是 ACTIVE 而不是 ALARM —— 共享层 isActiveAlarm 硬编码判 ACTIVE。
   */
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
 * 12 个监测点位（与真实库种子量级一致）。
 * 状态分布覆盖页面全部展示态：干燥 / 积水 / 离线 / 未知。
 */
const POINT_DEFS: PointDef[] = [
  {
    pointId: '201',
    pointName: '主通廊东侧',
    cameraCode: 'CAM-001',
    warehouseId: WAREHOUSES[0].id,
    warehouseName: WAREHOUSES[0].name,
    platformCameraId: 'C001',
    deviceStatus: 'ONLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 45 * SECOND,
  },
  {
    pointId: '202',
    pointName: '主通廊西侧',
    cameraCode: 'CAM-002',
    warehouseId: WAREHOUSES[0].id,
    warehouseName: WAREHOUSES[0].name,
    platformCameraId: 'C002',
    // 明确离线：deviceStatus=OFFLINE，且心跳也已陈旧
    deviceStatus: 'OFFLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 182 * MINUTE,
  },
  {
    pointId: '203',
    pointName: '主通廊南段',
    cameraCode: 'CAM-003',
    warehouseId: WAREHOUSES[0].id,
    warehouseName: WAREHOUSES[0].name,
    platformCameraId: 'C003',
    deviceStatus: 'ONLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 130 * SECOND,
  },
  {
    pointId: '204',
    pointName: '主通廊北段',
    cameraCode: 'CAM-004',
    warehouseId: WAREHOUSES[1].id,
    warehouseName: WAREHOUSES[1].name,
    platformCameraId: 'C004',
    // 在线且正在积水告警：页面必须出现「积水」卡片
    deviceStatus: 'ONLINE',
    alarmStatus: 'ACTIVE',
    heartbeatAgoSec: 20 * SECOND,
  },
  {
    pointId: '205',
    pointName: '主通廊东南',
    cameraCode: 'CAM-005',
    warehouseId: WAREHOUSES[1].id,
    warehouseName: WAREHOUSES[1].name,
    platformCameraId: 'C005',
    deviceStatus: 'ONLINE',
    alarmStatus: 'RECOVERED',
    heartbeatAgoSec: 190 * SECOND,
    // 一期前端只有原生 <video>，此分支验证「该视频协议暂不支持播放」的明确提示
    streamBranch: 'HLS',
  },
  {
    pointId: '206',
    pointName: '主通廊西南',
    cameraCode: 'CAM-006',
    warehouseId: WAREHOUSES[1].id,
    warehouseName: WAREHOUSES[1].name,
    platformCameraId: 'C006',
    deviceStatus: 'ONLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 60 * SECOND,
    // 在线但地址过期：验证「播放地址已过期」+ 重试按钮
    streamBranch: 'EXPIRED',
  },
  {
    pointId: '207',
    pointName: '北通廊东侧',
    cameraCode: 'CAM-007',
    warehouseId: WAREHOUSES[2].id,
    warehouseName: WAREHOUSES[2].name,
    platformCameraId: 'C007',
    deviceStatus: 'ONLINE',
    alarmStatus: 'ACTIVE',
    heartbeatAgoSec: 210 * SECOND,
  },
  {
    pointId: '208',
    pointName: '北通廊西侧',
    cameraCode: 'CAM-008',
    warehouseId: WAREHOUSES[2].id,
    warehouseName: WAREHOUSES[2].name,
    platformCameraId: 'C008',
    // 真实故障形态：库里 device_status 仍是 ONLINE，但心跳超过 90 分钟，
    // 页面必须判离线。这条用例专门盯着心跳阈值改动。
    deviceStatus: 'ONLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 126 * MINUTE,
  },
  {
    pointId: '209',
    pointName: '北通廊南段',
    cameraCode: 'CAM-009',
    warehouseId: WAREHOUSES[2].id,
    warehouseName: WAREHOUSES[2].name,
    platformCameraId: 'C009',
    deviceStatus: 'OFFLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 46 * MINUTE,
  },
  {
    pointId: '210',
    pointName: '南通廊东侧',
    cameraCode: 'CAM-010',
    warehouseId: WAREHOUSES[3].id,
    warehouseName: WAREHOUSES[3].name,
    platformCameraId: 'C010',
    deviceStatus: 'ONLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 30 * SECOND,
  },
  {
    pointId: '211',
    pointName: '卸粮坑东侧',
    cameraCode: 'CAM-011',
    warehouseId: WAREHOUSES[3].id,
    warehouseName: WAREHOUSES[3].name,
    platformCameraId: 'C011',
    deviceStatus: 'ONLINE',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: 15 * SECOND,
  },
  {
    pointId: '212',
    pointName: '工作塔一层',
    cameraCode: 'CAM-012',
    warehouseId: WAREHOUSES[3].id,
    warehouseName: WAREHOUSES[3].name,
    platformCameraId: 'C012',
    // UNKNOWN：设备状态无法判定，页面显示「未知」（虚线卡片），不得显示成离线
    deviceStatus: 'UNKNOWN',
    alarmStatus: 'NORMAL',
    heartbeatAgoSec: null,
  },
];

/**
 * 监测点位状态列表。
 *
 * @param now 请求时刻；心跳与最新事件时间都由它推出，缺省取 `Date.now()`
 */
export function createMonitorPointList(now: number = Date.now()): any[] {
  return POINT_DEFS.map((p, index) => {
    const latest = latestEventOf(p.pointId, now);
    return {
      pointId: p.pointId,
      pointName: p.pointName,
      cameraCode: p.cameraCode,
      platformCameraId: p.platformCameraId,
      warehouseId: p.warehouseId,
      warehouseName: p.warehouseName,
      deviceStatus: p.deviceStatus,
      alarmStatus: p.alarmStatus,
      lastHeartbeatAt: p.heartbeatAgoSec === null ? null : formatDateTime(now - p.heartbeatAgoSec),
      latestEventAt: latest === null ? null : formatDateTime(latest),
      sortNo: index + 1,
    };
  });
}

/** 设备的点位不产生积水事件，其余都可能 */
function pointEligibleForEvent(def: PointDef): boolean {
  return def.deviceStatus !== 'UNKNOWN';
}

const EVENT_POINTS = POINT_DEFS.filter(pointEligibleForEvent);

/* ---------- 积水结论类型 ---------- */

const WATER_TYPES = ['PONDING', 'LEAK', 'ICE', 'OTHER'] as const;

const WATER_TYPE_NAME: Record<string, string> = {
  PONDING: '积水',
  LEAK: '渗漏',
  ICE: '结冰',
  OTHER: '其他',
};

/**
 * 未核实事件的下标。
 * 处理中（PROCESSING）与已处理（DONE）按下标奇偶分配，
 * 保证三态在首页与历史页都能同时看到。
 */
const PENDING_INDEXES = new Set([0, 3, 7, 12, 19, 26]);

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
  cameraCode: string;
  warehouseId: string;
  warehouseName: string;
  waterType: string;
  waterTypeName: string;
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
  const rand = createRandom(20261029);
  const list: EventSeed[] = [];

  for (let i = 0; i < EVENT_COUNT; i += 1) {
    const point = EVENT_POINTS[i % EVENT_POINTS.length];
    const waterType = WATER_TYPES[i % WATER_TYPES.length];
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

    // 三态分布：未核实走固定下标集合，其余按奇偶分成处理中 / 已处理
    let processStatus: string;
    if (PENDING_INDEXES.has(i)) processStatus = 'PENDING';
    else if (i % 2 === 0) processStatus = 'PROCESSING';
    else processStatus = 'DONE';
    const isPending = processStatus === 'PENDING';

    const durationSeconds = hasConclusion ? 60 + Math.floor(rand() * 900) : null;

    list.push({
      eventId: String(3001 + i),
      pointId: point.pointId,
      pointName: point.pointName,
      cameraCode: point.cameraCode,
      warehouseId: point.warehouseId,
      warehouseName: point.warehouseName,
      waterType,
      waterTypeName: WATER_TYPE_NAME[waterType] ?? '其他',
      algorithmStatus,
      algorithmResult,
      confidence,
      processStatus,
      eventVersion: isPending ? 1 : 2,
      occurredAgoMs,
      processedAgoMs: isPending ? null : Math.max(30 * SECOND, occurredAgoMs - 30 * MINUTE),
      processedAtAbs: null,
      processedBy: isPending ? null : pickOne(rand, ['zhangsan', 'lisi', 'wangwu']),
      processOpinion: isPending ? null : '已安排现场排水并检查通廊上方管线渗漏点。',
      durationSeconds,
      // 已恢复的事件在发生之后 durationSeconds 秒恢复
      recoveredAgoMs: durationSeconds === null ? null : Math.max(10 * SECOND, occurredAgoMs - durationSeconds * SECOND),
      // MOCK 算法状态显式标记为模拟数据；其余行由真实库口径给出，
      // 这里保持与后端一致：只有模拟产出的行才为 true
      isSimulated: algorithmStatus === 'MOCK',
      modelTag: algorithmStatus === 'MOCK' ? 'ground-water-mock-v0' : 'ground-water-detector-v1',
    });
  }

  return list;
}

/** 模块级可变存储：查看即标记会改写它 */
export const EVENT_STORE: EventSeed[] = buildEventStore();

/** 某点位最近一次事件的时刻（用于 latestEventAt），不存在返回 null */
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
 * 事件编号：`WP` + `yyyyMMdd` + 4 位序号（例 `WP202607290001`）。
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
      .forEach((item, i) => map.set(item.eventId, `WP${key}${String(i + 1).padStart(4, '0')}`));
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
    // 事件挂在监测点位上，后端列名为 point_id
    pointId: item.pointId,
    pointName: item.pointName,
    cameraCode: item.cameraCode,
    waterType: item.waterType,
    waterTypeName: item.waterTypeName,
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
      originalUrl: `https://picsum.photos/seed/water-${item.eventId}-ref/640/360`,
      annotatedUrl: null,
      thumbnailUrl: `https://picsum.photos/seed/water-${item.eventId}-ref-t/320/180`,
      capturedAt: formatDateTime(occurredMs - 2 * MINUTE),
      cameraCode: item.cameraCode,
      contentHash: `sha256-ref-${item.eventId}`,
      sortNo: 1,
    },
    {
      evidenceId: `${item.eventId}-EV-WATER`,
      evidenceType: 'WATER_FRAME',
      originalUrl: `https://picsum.photos/seed/water-${item.eventId}-frame/640/360`,
      // 标注图与原图分离，不覆盖原图
      annotatedUrl: `https://picsum.photos/seed/water-${item.eventId}-marked/640/360`,
      thumbnailUrl: `https://picsum.photos/seed/water-${item.eventId}-frame-t/320/180`,
      capturedAt: formatDateTime(occurredMs),
      cameraCode: item.cameraCode,
      contentHash: `sha256-water-${item.eventId}`,
      sortNo: 2,
    },
  ];

  const recoveredMs = item.recoveredAgoMs === null ? null : now - item.recoveredAgoMs;
  if (recoveredMs !== null && recoveredMs >= occurredMs) {
    evidences.push({
      evidenceId: `${item.eventId}-EV-REC`,
      evidenceType: 'RECOVERY_IMAGE',
      originalUrl: `https://picsum.photos/seed/water-${item.eventId}-rec/640/360`,
      annotatedUrl: null,
      thumbnailUrl: `https://picsum.photos/seed/water-${item.eventId}-rec-t/320/180`,
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
      // 查看即标记由后端按登录态自动写入，来源标 SYSTEM；其余为历史种子
      source: isAutoViewed ? 'SYSTEM' : 'SEED',
    },
  ];
}

/** 事件详情（含证据与处理记录） */
export function buildEventDetail(item: EventSeed, now: number = Date.now()): any {
  const noMap = buildEventNoMap(now);
  const row = buildRow(item, noMap.get(item.eventId) ?? '', now);
  const occurredMs = occurredMsOf(item, now);
  const recoveredMs = item.recoveredAgoMs === null ? null : now - item.recoveredAgoMs;

  return {
    ...row,
    eventDesc:
      `${item.cameraCode} ${item.pointName} 于 ${row.occurredAt} ` + `检测到${item.waterTypeName}，湿滑面积超过阈值。`,
    recoveredAt: recoveredMs === null || recoveredMs < occurredMs ? null : formatDateTime(recoveredMs),
    durationSeconds: item.durationSeconds,
    isSimulated: item.isSimulated,
    modelName: item.modelTag,
    evidences: buildEvidences(item, occurredMs, now),
    processRecords: buildProcessRecords(item, now),
  };
}

/**
 * 查看即标记为已处理（与真实后端 `markEventViewed` 同语义）。
 *
 * - 状态、处理人、处理意见、版本号一律由「服务端」决定，不接受前端传入；
 * - **三态下只有 PENDING 会被改写为 DONE**，PROCESSING 不覆盖
 *   （已有人在现场跟进，一次「点开看看」不该冲掉处置进度），其余幂等返回；
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
 * `{"pointId":"201","canPlay":false,"expiresAt":"","platformCameraId":"C001",
 *   "playableUrl":"","protocol":"MP4","unavailableReason":"VIDEO_GATEWAY_NOT_READY"}`
 */
export function createStreamInfo(pointId: string, now: number = Date.now()): any {
  const point = POINT_DEFS.find((p) => p.pointId === pointId);

  const base = {
    pointId,
    pointName: point?.pointName ?? '',
    cameraCode: point?.cameraCode ?? '',
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
    pointName: point.pointName,
    cameraCode: point.cameraCode,
    canPlay: true,
    expiresAt: formatDateTime(now + STREAM_TTL_MS),
    platformCameraId: point.platformCameraId,
    playableUrl: MOCK_MP4_URL,
    protocol: 'MP4',
    unavailableReason: null,
  };
}

/** 积水类型编码 → 中文名（导出与兜底展示用） */
export function waterTypeNameOf(code: string): string {
  return WATER_TYPE_NAME[code] ?? '其他';
}
