/**
 * 皮带机下站人监测 Mock - 共享假数据生成器
 *
 * 形状来源（三者必须一致，否则切到真实后端会静默出错）：
 * - `src/api/beltMachine.ts` 的 `normalize*` 函数（页面实际消费的字段）
 * - `src/api/model/beltMachineModel.ts` 的请求/响应类型
 * - 真实后端响应样例（字段名驼峰、时间为 `yyyy-MM-dd HH:mm:ss` 字符串）
 *
 * ⚠️ 时间戳的生成方式（本文件最重要的一条约定）
 * 存储层只保存**相对当前时刻的偏移量**（`occurredAgoMs` 等），
 * 所有对外时间字符串都在每次调用 `materialize*` 时基于 `Date.now()` 现算。
 *
 * 原因：`shared/displayStatus.computeDisplayStatus` 用 **90 分钟心跳阈值**
 * （`shared/constants.HEARTBEAT_TIMEOUT_MS`）判定设备离线。若心跳时间在模块
 * 加载时固定一次，dev server 跑久后所有点位都会被误判成「离线」，
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

/* ---------- 基础数据 ---------- */

const WAREHOUSES = [
  { id: 'WH-001', name: '一号粮库' },
  { id: 'WH-002', name: '二号粮库' },
  { id: 'WH-003', name: '三号粮库' },
  { id: 'WH-004', name: '散粮库' },
];

const CN_NUM = ['一号', '二号', '三号', '四号', '五号', '六号', '七号', '八号', '九号', '十号', '十一号', '十二号'];

const AREAS = [
  '一号仓库作业区',
  '二号仓库作业区',
  '三号仓库作业区',

  '罩棚作业区',
  '工作塔一层',
  '卸粮坑',
  '主通廊东段',

  '主通廊西段',
  '铁路专用线',
  '汽车接收棚',
  '烘干塔进料口',

  '浅圆仓B区',
];

interface PointDef {
  pointId: string;
  deviceCode: string;
  /** 卡片标题：一号皮带机 / 二号皮带机 … */
  deviceName: string;
  areaName: string;
  warehouseId: string;
  warehouseName: string;
  /**
   * 绑定摄像头的平台标识。
   * null = 未绑定，与 deviceStatus='UNKNOWN' 一起构成页面的「未部署」。
   */
  cameraId: string | null;
  cameraCode: string | null;
  deviceStatus: 'ONLINE' | 'OFFLINE' | 'UNKNOWN';
  /** 行为告警状态，复用 AlarmStatus 语义（ACTIVE = 页面「报警」） */
  alarmStatus: 'NORMAL' | 'ACTIVE' | 'RECOVERED';
  /**
   * 心跳距**现在**的毫秒数。每次请求基于 Date.now() 现算，
   * 因此小于 90 分钟的都是「新鲜心跳」，大于则演示离线判定。
   */
  heartbeatAgoMs: number | null;
  /** 播放分支覆盖，见 createStreamInfo */
  streamBranch?: 'HLS' | 'EXPIRED';
}

/**
 * 12 台皮带机（与真实库种子量级一致）。
 * 状态分布覆盖原型图全部四种展示态：
 * 正常 / 报警 / 离线 / 未部署。
 */
const POINT_DEFS: PointDef[] = CN_NUM.map((cn, index) => {
  const i = index + 1;
  const warehouse = WAREHOUSES[index % WAREHOUSES.length];
  const notDeployed = i === 10 || i === 12;
  const offline = i === 5 || i === 8;
  return {
    pointId: String(100 + i),
    deviceCode: `BELT-${pad2(i).padStart(3, '0')}`,
    deviceName: `${cn}皮带机`,
    areaName: AREAS[index],
    warehouseId: warehouse.id,
    warehouseName: warehouse.name,
    cameraId: notDeployed ? null : `C${String(i).padStart(3, '0')}`,
    cameraCode: notDeployed ? null : `CAM-${String(i).padStart(3, '0')}`,
    deviceStatus: notDeployed ? 'UNKNOWN' : offline ? 'OFFLINE' : 'ONLINE',
    alarmStatus: i === 3 || i === 6 ? 'ACTIVE' : 'NORMAL',
    // 在线 1~5 分钟内；离线 200 分钟（超过 90 分钟阈值）；未部署无心跳
    heartbeatAgoMs: notDeployed ? null : offline ? 200 * MINUTE : (1 + (i % 5)) * MINUTE,
    // 第 4 台走 HLS 分支，验证「该视频协议暂不支持播放」的明确提示
    streamBranch: i === 4 ? 'HLS' : undefined,
  } as PointDef;
});

// 第 7 台在线但地址过期：验证「播放地址已过期」+ 重试按钮
const expiredPoint = POINT_DEFS.find((p) => p.pointId === '107');
if (expiredPoint) expiredPoint.streamBranch = 'EXPIRED';

/**
 * 皮带机点位状态列表。
 *
 * @param now 请求时刻；心跳与最新事件时间都由它推出，缺省取 `Date.now()`
 */
export function createMonitorPointList(now: number = Date.now()): any[] {
  return POINT_DEFS.map((p, index) => {
    const latest = latestEventOf(p.pointId, now);
    return {
      pointId: p.pointId,
      deviceCode: p.deviceCode,
      deviceName: p.deviceName,
      areaName: p.areaName,
      warehouseId: p.warehouseId,
      warehouseName: p.warehouseName,
      // cameraId 为空必须原样输出 null，前端「未部署」判定依赖它
      cameraId: p.cameraId,
      cameraCode: p.cameraCode,
      cameraName: p.cameraId ? p.areaName : null,
      deviceStatus: p.deviceStatus,
      alarmStatus: p.alarmStatus,
      lastHeartbeatAt: p.heartbeatAgoMs === null ? null : formatDateTime(now - p.heartbeatAgoMs),
      latestEventAt: latest === null ? null : formatDateTime(latest),
      sortNo: index + 1,
    };
  });
}

/** 离线与未部署的点位不产生事件，其余都可能 */
function pointEligibleForEvent(def: PointDef): boolean {
  return def.deviceStatus === 'ONLINE';
}

const EVENT_POINTS = POINT_DEFS.filter(pointEligibleForEvent);

/* ---------- 结论类型 ---------- */

const CONCLUSION_TYPES = ['STANDING_UNDER', 'CROSSING', 'LOITERING', 'OTHER'] as const;

const CONCLUSION_TYPE_NAME: Record<string, string> = {
  STANDING_UNDER: '皮带机下站人',
  CROSSING: '跨越运行设备',
  LOITERING: '区域徘徊',
  OTHER: '其他',
};

/**
 * 未核实事件的下标（每次查看会被真实改写，这里只决定初始分布）。
 * 另外每 7 条留 1 条 PROCESSING，验证三值处理状态都能渲染出来。
 */
const PENDING_INDEXES = new Set([0, 4, 8, 12, 16, 20, 24, 28]);
const PROCESSING_INDEXES = new Set([2, 9, 16, 23]);

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
  deviceCode: string;
  deviceName: string;
  cameraCode: string | null;
  warehouseId: string;
  warehouseName: string;
  conclusionType: string;
  conclusionTypeName: string;
  personCount: number | null;
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
    const conclusionType = CONCLUSION_TYPES[i % CONCLUSION_TYPES.length];
    const algorithmStatus = algorithmStatusOf(i);
    const hasConclusion = algorithmStatus === 'SUCCEEDED' || algorithmStatus === 'MOCK';

    // 算法未成功时结论为 null，不伪造为 NORMAL；约 15% 成功项置信度缺失，验证「暂无」
    let algorithmResult: string | null = null;
    let confidence: number | null = null;
    let personCount: number | null = null;
    if (hasConclusion) {
      algorithmResult = pickOne(rand, ['ANOMALY', 'ANOMALY', 'ANOMALY', 'UNCERTAIN', 'NORMAL']);
      confidence = rand() < 0.15 ? null : Number((0.55 + rand() * 0.44).toFixed(4));
      // 约 1/6 的行人数留空，验证页面显示「暂无」而不是 0 人
      personCount = rand() < 0.16 ? null : 1 + Math.floor(rand() * 3);
    }

    // 最新一条约 8 分钟前，往前每条约 4.7 小时（30 条 ≈ 5.7 天，落在默认 7 天窗口内）
    const occurredAgoMs = 8 * MINUTE + i * (4.7 * HOUR) + Math.floor(rand() * 40) * MINUTE;

    const isPending = PENDING_INDEXES.has(i);
    const isProcessing = !isPending && PROCESSING_INDEXES.has(i);
    const durationSeconds = hasConclusion ? 60 + Math.floor(rand() * 900) : null;

    let processStatus = 'DONE';
    if (isPending) processStatus = 'PENDING';
    else if (isProcessing) processStatus = 'PROCESSING';

    list.push({
      eventId: String(3001 + i),
      pointId: point.pointId,
      deviceCode: point.deviceCode,
      deviceName: point.deviceName,
      cameraCode: point.cameraCode,
      warehouseId: point.warehouseId,
      warehouseName: point.warehouseName,
      conclusionType,
      conclusionTypeName: CONCLUSION_TYPE_NAME[conclusionType] ?? '其他',
      personCount,
      algorithmStatus,
      algorithmResult,
      confidence,
      processStatus,
      eventVersion: isPending ? 1 : 2,
      occurredAgoMs,
      processedAgoMs: isPending ? null : Math.max(30 * SECOND, occurredAgoMs - 30 * MINUTE),
      processedAtAbs: null,
      processedBy: isPending ? null : pickOne(rand, ['zhangsan', 'lisi', 'wangwu']),
      processOpinion: isPending
        ? null
        : isProcessing
          ? '已通知现场管理人员前往皮带机急停位置处置。'
          : '查看事件详情后自动标记为已处理',
      durationSeconds,
      // 已恢复的事件在发生之后 durationSeconds 秒恢复
      recoveredAgoMs:
        durationSeconds === null || isPending ? null : Math.max(10 * SECOND, occurredAgoMs - durationSeconds * SECOND),
      isSimulated: algorithmStatus === 'MOCK',
      modelTag: algorithmStatus === 'MOCK' ? 'belt-person-mock-v0' : 'belt-person-detector-v1',
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
 * 事件编号：`BC` + `yyyyMMdd` + 4 位序号（例 `BC202607290001`）。
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
      .forEach((item, i) => map.set(item.eventId, `BC${key}${String(i + 1).padStart(4, '0')}`));
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
    // 事件挂在皮带机点位表上，后端列名为 point_id
    pointId: item.pointId,
    deviceCode: item.deviceCode,
    deviceName: item.deviceName,
    cameraCode: item.cameraCode,
    conclusionType: item.conclusionType,
    conclusionTypeName: item.conclusionTypeName,
    // 保留 null：页面据此显示「暂无」而不是 0 人
    personCount: item.personCount,
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
      originalUrl: `https://picsum.photos/seed/belt-${item.eventId}-ref/640/360`,
      annotatedUrl: null,
      thumbnailUrl: `https://picsum.photos/seed/belt-${item.eventId}-ref-t/320/180`,
      capturedAt: formatDateTime(occurredMs - 2 * MINUTE),
      cameraCode: item.cameraCode,
      contentHash: `sha256-ref-${item.eventId}`,
      sortNo: 1,
    },
    {
      evidenceId: `${item.eventId}-EV-ANO`,
      evidenceType: 'ANOMALY_FRAME',
      originalUrl: `https://picsum.photos/seed/belt-${item.eventId}-frame/640/360`,
      // 标注图与原图分离，不覆盖原图
      annotatedUrl: `https://picsum.photos/seed/belt-${item.eventId}-marked/640/360`,
      thumbnailUrl: `https://picsum.photos/seed/belt-${item.eventId}-frame-t/320/180`,
      capturedAt: formatDateTime(occurredMs),
      cameraCode: item.cameraCode,
      contentHash: `sha256-ano-${item.eventId}`,
      sortNo: 2,
    },
  ];

  const recoveredMs = item.recoveredAgoMs === null ? null : now - item.recoveredAgoMs;
  if (recoveredMs !== null && recoveredMs >= occurredMs) {
    evidences.push({
      evidenceId: `${item.eventId}-EV-REC`,
      evidenceType: 'ANNOTATED_FRAME',
      originalUrl: `https://picsum.photos/seed/belt-${item.eventId}-rec/640/360`,
      annotatedUrl: `https://picsum.photos/seed/belt-${item.eventId}-rec-marked/640/360`,
      thumbnailUrl: `https://picsum.photos/seed/belt-${item.eventId}-rec-t/320/180`,
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
      // 查看即标记由系统按口径自动写入，其余为历史种子
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
      `${item.deviceName}（${item.cameraCode ?? '未绑定摄像头'}）于 ${row.occurredAt} ` +
      `检测到${item.conclusionTypeName}。`,
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
 * - 仅 PENDING 会被改写为 DONE，其余幂等返回，不重复追加审计、不自增版本；
 * - 审计来源写 SYSTEM，与人工处理的 WEB 区分（与后端 BeltMachineConstant 一致）。
 *
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
 * 与真实后端同形（注意 `playableUrl` / `expiresAt` 不可播放时是**空串**，
 * 不是 null）：
 * `{"pointId":"101","canPlay":false,"expiresAt":"","cameraId":"C001",
 *   "playableUrl":"","protocol":"MP4","unavailableReason":"CAMERA_NOT_BOUND"}`
 */
export function createStreamInfo(pointId: string, now: number = Date.now()): any {
  const point = POINT_DEFS.find((p) => p.pointId === pointId);

  const base = {
    pointId,
    canPlay: false as boolean,
    expiresAt: '',
    cameraId: point?.cameraId ?? '',
    cameraCode: point?.cameraCode ?? '',
    cameraName: point?.cameraId ? point.areaName : '',
    playableUrl: '',
    protocol: 'MP4',
    unavailableReason: '',
  };

  if (!point) {
    return { ...base, unavailableReason: 'POINT_NOT_FOUND' };
  }

  // 未部署必须在离线判断之前：未部署点位的 deviceStatus 也是 UNKNOWN，
  // 先判离线会让用户以为设备掉线，实际是根本没装摄像头。
  if (!point.cameraId) {
    return { ...base, unavailableReason: 'CAMERA_NOT_BOUND' };
  }

  if (point.deviceStatus === 'OFFLINE') {
    return { ...base, unavailableReason: 'CAMERA_OFFLINE' };
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
    cameraId: point.cameraId,
    cameraCode: point.cameraCode,
    cameraName: point.areaName,
    playableUrl: MOCK_MP4_URL,
    protocol: 'MP4',
    unavailableReason: null,
  };
}

/** 结论类型编码 → 中文名（导出与兜底展示用） */
export function conclusionTypeNameOf(code: string): string {
  return CONCLUSION_TYPE_NAME[code] ?? '其他';
}
