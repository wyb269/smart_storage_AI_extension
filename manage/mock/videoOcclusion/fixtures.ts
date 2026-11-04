/**
 * 视频遮挡检测 Mock - 共享假数据生成器
 *
 * 形状来源（三者必须一致，否则切到真实后端会静默出错）：
 * - `src/api/videoOcclusion.ts` 的 `normalize*` 函数（页面实际消费的字段）
 * - `src/api/model/videoOcclusionModel.ts` 的请求/响应类型
 * - 真实后端实测回来的响应样例（字段名驼峰、时间为 `yyyy-MM-dd HH:mm:ss` 字符串）
 *
 * ⚠️ 时间戳的生成方式（本文件最重要的一条约定）
 * 存储层只保存**相对当前时刻的偏移量**（`occurredAgoMs` 等），
 * 所有对外时间字符串都在每次调用 `materialize*` 时基于 `Date.now()` 现算。
 *
 * 原因：`shared/displayStatus.computeDisplayStatus` 用 **90 分钟心跳阈值**
 * （`shared/constants.HEARTBEAT_TIMEOUT_MS`）判定设备离线。若心跳时间在模块
 * 加载时固定一次，dev server 跑久后所有摄像头都会被误判成「离线」，
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

/* ---------- 摄像头基础数据 ---------- */

const WAREHOUSES = [
  { id: 'WH-001', name: '一号粮库' },
  { id: 'WH-002', name: '二号粮库' },
  { id: 'WH-003', name: '三号粮库' },
  { id: 'WH-004', name: '散粮库' },
];

interface CameraDef {
  cameraId: string;
  cameraCode: string;
  cameraName: string;
  /** 摄像头安装位置，事件列表与导出都要用 */
  position: string;
  warehouseId: string;
  warehouseName: string;
  platformCameraId: string;
  deviceStatus: 'ONLINE' | 'OFFLINE' | 'UNKNOWN';
  /** 遮挡告警状态，复用 AlarmStatus 语义（ACTIVE = 页面「遮挡」） */
  occlusionStatus: 'NORMAL' | 'ACTIVE' | 'RECOVERED';
  /**
   * 心跳距**现在**的秒数。每次请求基于 Date.now() 现算，
   * 因此小于 90 分钟的都是「新鲜心跳」，大于则演示离线判定。
   */
  heartbeatAgoSec: number | null;
  /** 播放分支覆盖，见 createStreamInfo */
  streamBranch?: 'HLS' | 'EXPIRED';
}

/**
 * 12 台摄像头（与真实库量级一致）。
 * 状态分布覆盖页面全部展示态：正常 / 遮挡 / 离线 / 未知。
 */
const CAMERA_DEFS: CameraDef[] = [
  {
    cameraId: '101',
    cameraCode: 'CAM-001',
    cameraName: '1号仓东门全景',
    position: '1号仓东门上方3米',
    warehouseId: WAREHOUSES[0].id,
    warehouseName: WAREHOUSES[0].name,
    platformCameraId: 'C001',
    deviceStatus: 'ONLINE',
    occlusionStatus: 'NORMAL',
    heartbeatAgoSec: 45 * SECOND,
  },
  {
    cameraId: '102',
    cameraCode: 'CAM-002',
    cameraName: '1号仓西门全景',
    position: '1号仓西门立柱',
    warehouseId: WAREHOUSES[0].id,
    warehouseName: WAREHOUSES[0].name,
    platformCameraId: 'C002',
    // 在线且遮挡告警中：页面必须出现「遮挡」卡片，且默认选中逻辑要跳过它之前先取到在线项
    deviceStatus: 'ONLINE',
    occlusionStatus: 'ACTIVE',
    heartbeatAgoSec: 20 * SECOND,
  },
  {
    cameraId: '103',
    cameraCode: 'CAM-003',
    cameraName: '1号仓南墙摄像机',
    position: '1号仓南墙中部',
    warehouseId: WAREHOUSES[0].id,
    warehouseName: WAREHOUSES[0].name,
    platformCameraId: 'C003',
    deviceStatus: 'ONLINE',
    occlusionStatus: 'NORMAL',
    heartbeatAgoSec: 130 * SECOND,
  },
  {
    cameraId: '104',
    cameraCode: 'CAM-004',
    cameraName: '2号仓东门摄像机',
    position: '2号仓东门雨棚下',
    warehouseId: WAREHOUSES[1].id,
    warehouseName: WAREHOUSES[1].name,
    platformCameraId: 'C004',
    deviceStatus: 'ONLINE',
    occlusionStatus: 'RECOVERED',
    heartbeatAgoSec: 190 * SECOND,
    // 一期前端只有原生 <video>，此分支验证「该视频协议暂不支持播放」的明确提示
    streamBranch: 'HLS',
  },
  {
    cameraId: '105',
    cameraCode: 'CAM-005',
    // 明确离线：deviceStatus=OFFLINE，且心跳也已陈旧
    cameraName: '2号仓西门摄像机',
    position: '2号仓西门墙角',
    warehouseId: WAREHOUSES[1].id,
    warehouseName: WAREHOUSES[1].name,
    platformCameraId: 'C005',
    deviceStatus: 'OFFLINE',
    occlusionStatus: 'NORMAL',
    heartbeatAgoSec: 182 * MINUTE,
  },
  {
    cameraId: '106',
    cameraCode: 'CAM-006',
    cameraName: '2号仓罩棚顶部',
    position: '2号仓罩棚脊线',
    warehouseId: WAREHOUSES[1].id,
    warehouseName: WAREHOUSES[1].name,
    platformCameraId: 'C006',
    deviceStatus: 'ONLINE',
    occlusionStatus: 'NORMAL',
    heartbeatAgoSec: 60 * SECOND,
    // 在线但地址过期：验证「播放地址已过期」+ 重试按钮
    streamBranch: 'EXPIRED',
  },
  {
    cameraId: '107',
    cameraCode: 'CAM-007',
    cameraName: '3号仓东门摄像机',
    position: '3号仓东门灯杆',
    warehouseId: WAREHOUSES[2].id,
    warehouseName: WAREHOUSES[2].name,
    platformCameraId: 'C007',
    deviceStatus: 'ONLINE',
    occlusionStatus: 'NORMAL',
    heartbeatAgoSec: 210 * SECOND,
  },
  {
    cameraId: '108',
    cameraCode: 'CAM-008',
    // 真实故障形态：库里 device_status 仍是 ONLINE，但心跳超过 90 分钟，
    // 页面必须判离线。这条用例专门盯着心跳阈值改动。
    cameraName: '3号仓西门摄像机',
    position: '3号仓西门砖墙',
    warehouseId: WAREHOUSES[2].id,
    warehouseName: WAREHOUSES[2].name,
    platformCameraId: 'C008',
    deviceStatus: 'ONLINE',
    occlusionStatus: 'NORMAL',
    heartbeatAgoSec: 126 * MINUTE,
  },
  {
    cameraId: '109',
    cameraCode: 'CAM-009',
    cameraName: '3号仓北侧装卸口',
    position: '装卸口立柱',
    warehouseId: WAREHOUSES[2].id,
    warehouseName: WAREHOUSES[2].name,
    platformCameraId: 'C009',
    deviceStatus: 'ONLINE',
    occlusionStatus: 'NORMAL',
    heartbeatAgoSec: 30 * SECOND,
  },
  {
    cameraId: '110',
    cameraCode: 'CAM-010',
    // 明确 OFFLINE，但心跳仍在 90 分钟阈值内：离线只由 deviceStatus 判定
    cameraName: '4号仓大门摄像机',
    position: '大门门柱内侧',
    warehouseId: WAREHOUSES[3].id,
    warehouseName: WAREHOUSES[3].name,
    platformCameraId: 'C010',
    deviceStatus: 'OFFLINE',
    occlusionStatus: 'NORMAL',
    heartbeatAgoSec: 46 * MINUTE,
  },
  {
    cameraId: '111',
    cameraCode: 'CAM-011',
    cameraName: '4号仓西站房',
    position: '站房屋檐下',
    warehouseId: WAREHOUSES[3].id,
    warehouseName: WAREHOUSES[3].name,
    platformCameraId: 'C011',
    deviceStatus: 'ONLINE',
    occlusionStatus: 'NORMAL',
    heartbeatAgoSec: 15 * SECOND,
  },
  {
    cameraId: '112',
    cameraCode: 'CAM-012',
    // UNKNOWN：设备状态无法判定，页面显示「未知」（虚线卡片），不得显示成离线
    cameraName: '散粮仓东侧临时机',
    position: '散粮仓东侧临时杆',
    warehouseId: WAREHOUSES[3].id,
    warehouseName: WAREHOUSES[3].name,
    platformCameraId: 'C012',
    deviceStatus: 'UNKNOWN',
    occlusionStatus: 'NORMAL',
    heartbeatAgoSec: null,
  },
];

/**
 * 摄像头状态列表。
 *
 * @param now 请求时刻；心跳与最新事件时间都由它推出，缺省取 `Date.now()`
 */
export function createCameraStatusList(now: number = Date.now()): any[] {
  return CAMERA_DEFS.map((c, index) => {
    const latest = latestEventOf(c.cameraId, now);
    return {
      cameraId: c.cameraId,
      cameraCode: c.cameraCode,
      cameraName: c.cameraName,
      warehouseId: c.warehouseId,
      warehouseName: c.warehouseName,
      platformCameraId: c.platformCameraId,
      deviceStatus: c.deviceStatus,
      occlusionStatus: c.occlusionStatus,
      lastHeartbeatAt: c.heartbeatAgoSec === null ? null : formatDateTime(now - c.heartbeatAgoSec),
      latestEventAt: latest === null ? null : formatDateTime(latest),
      sortNo: index + 1,
    };
  });
}

/** 离线与未知的摄像头不产生遮挡事件，其余都可能 */
function cameraEligibleForEvent(def: CameraDef): boolean {
  return def.deviceStatus !== 'UNKNOWN';
}

const EVENT_CAMERAS = CAMERA_DEFS.filter(cameraEligibleForEvent);

/* ---------- 遮挡类型 ---------- */

const OCCLUSION_TYPES = ['FULL', 'PARTIAL', 'PAINT', 'LENS_COVER', 'OTHER'] as const;

const OCCLUSION_TYPE_NAME: Record<string, string> = {
  FULL: '全幅遮挡',
  PARTIAL: '部分遮挡',
  PAINT: '镜头污损',
  LENS_COVER: '镜头被遮盖',
  OTHER: '其他或未知',
};

/** 未处理事件的下标（每次查看会被真实改写，这里只决定初始分布） */
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
  cameraId: string;
  cameraCode: string;
  cameraName: string;
  cameraPosition: string;
  warehouseId: string;
  warehouseName: string;
  occlusionType: string;
  occlusionTypeName: string;
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
  const rand = createRandom(20261023);
  const list: EventSeed[] = [];

  for (let i = 0; i < EVENT_COUNT; i += 1) {
    const camera = EVENT_CAMERAS[i % EVENT_CAMERAS.length];
    const occlusionType = OCCLUSION_TYPES[i % OCCLUSION_TYPES.length];
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
    const durationSeconds = hasConclusion ? 60 + Math.floor(rand() * 900) : null;

    list.push({
      eventId: String(3001 + i),
      cameraId: camera.cameraId,
      cameraCode: camera.cameraCode,
      cameraName: camera.cameraName,
      cameraPosition: camera.position,
      warehouseId: camera.warehouseId,
      warehouseName: camera.warehouseName,
      occlusionType,
      occlusionTypeName: OCCLUSION_TYPE_NAME[occlusionType] ?? '其他或未知',
      algorithmStatus,
      algorithmResult,
      confidence,
      processStatus: isPending ? 'PENDING' : 'CLOSED',
      eventVersion: isPending ? 1 : 2,
      occurredAgoMs,
      processedAgoMs: isPending ? null : Math.max(30 * SECOND, occurredAgoMs - 30 * MINUTE),
      processedAtAbs: null,
      processedBy: isPending ? null : pickOne(rand, ['zhangsan', 'lisi', 'wangwu']),
      processOpinion: isPending ? null : '已通知现场确认遮挡原因并恢复镜头视野。',
      durationSeconds,
      // 已恢复的事件在发生之后 durationSeconds 秒恢复
      recoveredAgoMs: durationSeconds === null ? null : Math.max(10 * SECOND, occurredAgoMs - durationSeconds * SECOND),
      isSimulated: algorithmStatus === 'MOCK',
      modelTag: algorithmStatus === 'MOCK' ? 'occlusion-mock-v0' : 'occlusion-detector-v1',
    });
  }

  return list;
}

/** 模块级可变存储：查看即标记会改写它 */
export const EVENT_STORE: EventSeed[] = buildEventStore();

/** 某摄像头最近一次事件的时刻（用于 latestEventAt），不存在返回 null */
function latestEventOf(cameraId: string, now: number): number | null {
  let latest: number | null = null;
  for (const item of EVENT_STORE) {
    if (item.cameraId !== cameraId) continue;
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
 * 事件编号：`VD` + `yyyyMMdd` + 4 位序号（例 `VD202610230001`）。
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
      .forEach((item, i) => map.set(item.eventId, `VD${key}${String(i + 1).padStart(4, '0')}`));
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
    // 事件挂在摄像头配置表上，后端列名为 camera_config_id
    cameraConfigId: item.cameraId,
    cameraCode: item.cameraCode,
    cameraPosition: item.cameraPosition,
    occlusionType: item.occlusionType,
    occlusionTypeName: item.occlusionTypeName,
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
      originalUrl: `https://picsum.photos/seed/occ-${item.eventId}-ref/640/360`,
      annotatedUrl: null,
      thumbnailUrl: `https://picsum.photos/seed/occ-${item.eventId}-ref-t/320/180`,
      capturedAt: formatDateTime(occurredMs - 2 * MINUTE),
      cameraCode: item.cameraCode,
      contentHash: `sha256-ref-${item.eventId}`,
      sortNo: 1,
    },
    {
      evidenceId: `${item.eventId}-EV-OCC`,
      evidenceType: 'OCCLUDED_FRAME',
      originalUrl: `https://picsum.photos/seed/occ-${item.eventId}-frame/640/360`,
      // 标注图与原图分离，不覆盖原图
      annotatedUrl: `https://picsum.photos/seed/occ-${item.eventId}-marked/640/360`,
      thumbnailUrl: `https://picsum.photos/seed/occ-${item.eventId}-frame-t/320/180`,
      capturedAt: formatDateTime(occurredMs),
      cameraCode: item.cameraCode,
      contentHash: `sha256-occ-${item.eventId}`,
      sortNo: 2,
    },
  ];

  const recoveredMs = item.recoveredAgoMs === null ? null : now - item.recoveredAgoMs;
  if (recoveredMs !== null && recoveredMs >= occurredMs) {
    evidences.push({
      evidenceId: `${item.eventId}-EV-REC`,
      evidenceType: 'RECOVERY_IMAGE',
      originalUrl: `https://picsum.photos/seed/occ-${item.eventId}-rec/640/360`,
      annotatedUrl: null,
      thumbnailUrl: `https://picsum.photos/seed/occ-${item.eventId}-rec-t/320/180`,
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
      statusAfter: 'CLOSED',
      processResult: 'CLOSED',
      processOpinion: item.processOpinion ?? '',
      processedBy: item.processedBy ?? '',
      processedAt: formatDateTime(processedMs),
      // 查看即标记由页面触发，其余为历史种子
      source: isAutoViewed ? 'WEB' : 'SEED',
    },
  ];
}

/** 事件详情（含证据、处理记录与算法任务历史） */
export function buildEventDetail(item: EventSeed, now: number = Date.now()): any {
  const noMap = buildEventNoMap(now);
  const row = buildRow(item, noMap.get(item.eventId) ?? '', now);
  const occurredMs = occurredMsOf(item, now);
  const recoveredMs = item.recoveredAgoMs === null ? null : now - item.recoveredAgoMs;
  // 与后端 getEventDetail 一致：任务随详情一次带回，按 started_at 倒序、最多 20 条
  const algorithmTasks = materializeAlgorithmTasks(item.eventId, now);

  return {
    ...row,
    eventDesc:
      `${item.cameraCode} ${item.cameraName}（${item.cameraPosition}）于 ${row.occurredAt} ` +
      `检测到${item.occlusionTypeName}。`,
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
 * - 仅 PENDING 会被改写，其余幂等返回，不重复追加审计、不自增版本；
 * @returns 是否发生了实际改写
 */
export function markViewed(item: EventSeed, now: number = Date.now()): boolean {
  if (item.processStatus !== 'PENDING') return false;

  item.processStatus = 'CLOSED';
  item.processOpinion = '查看事件详情后自动标记为已处理';
  item.processedBy = 'currentUser';
  item.processedAtAbs = now;
  item.processedAgoMs = null;
  item.eventVersion += 1;
  return true;
}

/* ---------- 算法任务存储 ---------- */

/**
 * 算法任务条目，字段与后端 `OcclusionAlgorithmTaskRespDTO` 逐一对应。
 *
 * 与事件一样只存偏移量（`startedAgoMs` / `finishedAgoMs`）或「真的发生过」的绝对时刻
 * （`startedAbs` / `finishedAbs`），时间字符串在 `materializeAlgorithmTask` 里现算。
 *
 * `live` 标记本次运行通过「重新识别」提交的任务：它按 `pollCount` 推进
 * QUEUED → RUNNING → SUCCEEDED，模拟真实网关需要多次查询才出结果。
 */
interface AlgoTaskSeed {
  /** 任务主键（字符串），与 taskNo 不同但一一对应 */
  taskId: string;
  taskNo: string;
  eventId: string;
  executionStatus: string;
  conclusion: string | null;
  occlusionType: string | null;
  confidence: number | null;
  modelName: string | null;
  modelVersion: string | null;
  algorithmVersion: string | null;
  configVersion: string | null;
  inferenceDurationMs: number | null;
  startedAgoMs: number | null;
  finishedAgoMs: number | null;
  startedAbs: number | null;
  finishedAbs: number | null;
  detectedCount: number | null;
  simulated: boolean;
  errorCode: string | null;
  errorMessage: string | null;
  retryCount: number;
  live: boolean;
  pollCount: number;
}

/** 每个事件补一条与算法列一致的历史任务，便于弹窗开箱即有内容 */
function buildLegacyAlgorithmTasks(): AlgoTaskSeed[] {
  return EVENT_STORE.map((item, idx) => {
    const status = item.algorithmStatus;
    const hasConclusion = status === 'SUCCEEDED' || status === 'MOCK';
    const failed = status === 'FAILED';
    const timeout = status === 'TIMEOUT';
    const durationMs = hasConclusion ? 180 + (idx % 7) * 35 : null;
    const startedAgoMs = Math.max(0, item.occurredAgoMs - 30 * SECOND);

    return {
      taskId: String(7100 + idx),
      taskNo: `OCT-${String(8100 + idx)}`,
      eventId: item.eventId,
      executionStatus: status,
      conclusion: hasConclusion ? item.algorithmResult : null,
      occlusionType: hasConclusion ? item.occlusionType : null,
      confidence: hasConclusion ? item.confidence : null,
      modelName: hasConclusion ? item.modelTag : null,
      modelVersion: hasConclusion ? '1.4.2' : null,
      algorithmVersion: hasConclusion ? 'adapter-1.0.3' : null,
      configVersion: 'cfg-occlusion-1',
      inferenceDurationMs: durationMs,
      startedAgoMs,
      finishedAgoMs: durationMs === null ? null : Math.max(0, startedAgoMs - durationMs),
      startedAbs: null,
      finishedAbs: null,
      detectedCount: hasConclusion ? (item.algorithmResult === 'ANOMALY' ? 1 : 0) : null,
      simulated: status === 'MOCK',
      errorCode: failed ? 'ALGORITHM_ERROR' : timeout ? 'ALGORITHM_TIMEOUT' : null,
      errorMessage: failed
        ? '模拟算法执行失败（历史样例，非真实故障）'
        : timeout
          ? '模拟算法执行超时（历史样例，非真实故障）'
          : null,
      retryCount: 0,
      live: false,
      pollCount: 0,
    };
  });
}

/** 模块级可变存储：历史任务 + 本次运行「重新识别」提交的实时任务 */
export const ALGO_TASK_STORE: AlgoTaskSeed[] = buildLegacyAlgorithmTasks();

let algoTaskSeq = 0;

/** 任务行物化：时间按 now 现算，字段与后端 OcclusionAlgorithmTaskRespDTO 同形 */
function materializeAlgorithmTask(task: AlgoTaskSeed, now: number): any {
  const startedMs =
    task.startedAbs !== null ? task.startedAbs : task.startedAgoMs !== null ? now - task.startedAgoMs : null;
  const finishedMs =
    task.finishedAbs !== null ? task.finishedAbs : task.finishedAgoMs !== null ? now - task.finishedAgoMs : null;

  return {
    // 后端 taskId 为字符串主键、taskNo 为对外编号；前端轮询用 taskNo
    taskId: task.taskId,
    taskNo: task.taskNo,
    eventId: task.eventId,
    executionStatus: task.executionStatus,
    conclusion: task.conclusion,
    occlusionType: task.occlusionType,
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
    retryCount: task.retryCount,
  };
}

/** 某事件的算法任务列表：按开始时间倒序，最多 20 条 */
export function materializeAlgorithmTasks(eventId: string, now: number = Date.now()): any[] {
  return ALGO_TASK_STORE.filter((t) => t.eventId === eventId)
    .map((t) => ({ seed: t, row: materializeAlgorithmTask(t, now) }))
    .sort((a, b) => {
      const av = a.row.startedAt ? (toMs(a.row.startedAt) ?? -1) : -1;
      const bv = b.row.startedAt ? (toMs(b.row.startedAt) ?? -1) : -1;
      return bv - av;
    })
    .slice(0, 20)
    .map((x) => x.row);
}

/** 后端时间字符串 → 毫秒；仅本文件内排序用，解析失败返回 null */
function toMs(value: string): number | null {
  const m = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})/.exec(value);
  if (!m) {
    const n = new Date(value).getTime();
    return Number.isNaN(n) ? null : n;
  }
  return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]), Number(m[4]), Number(m[5]), Number(m[6])).getTime();
}

/**
 * 提交「重新识别」：立即返回 QUEUED 任务号，不同步出结果。
 * @returns 与后端 RerunAlgorithmRespDTO 同形（taskId 即轮询用的 taskNo）
 */
export function rerunAlgorithmTask(eventId: string, now: number = Date.now()): any {
  algoTaskSeq += 1;
  const taskNo = `OCT-${now.toString(36).toUpperCase()}${algoTaskSeq}`;
  ALGO_TASK_STORE.push({
    taskId: `9${now.toString(36).toUpperCase()}${algoTaskSeq}`,
    taskNo,
    eventId,
    executionStatus: 'QUEUED',
    conclusion: null,
    occlusionType: null,
    confidence: null,
    modelName: null,
    modelVersion: null,
    algorithmVersion: null,
    configVersion: 'cfg-occlusion-1',
    inferenceDurationMs: null,
    startedAgoMs: null,
    finishedAgoMs: null,
    startedAbs: now,
    finishedAbs: null,
    detectedCount: null,
    simulated: true,
    errorCode: null,
    errorMessage: null,
    retryCount: 0,
    live: true,
    pollCount: 0,
  });
  return { taskId: taskNo, executionStatus: 'QUEUED', simulated: true };
}

/**
 * 轮询实时任务：QUEUED → RUNNING →（第 3 次起）SUCCEEDED。
 *
 * 成功时按后端语义回写事件的算法列（algorithmStatus / algorithmResult / confidence），
 * **不动处理状态**，也不触碰证据。
 */
export function queryAlgorithmTask(taskNo: string, now: number = Date.now()): any | null {
  const task = ALGO_TASK_STORE.find((t) => t.taskNo === taskNo);
  if (!task) return null;

  if (task.live && task.executionStatus !== 'SUCCEEDED') {
    task.pollCount += 1;
    if (task.pollCount === 1) {
      task.executionStatus = 'RUNNING';
    } else if (task.pollCount >= 3) {
      const event = findEvent(task.eventId);
      const occlusionType = event?.occlusionType ?? 'PARTIAL';
      task.executionStatus = 'SUCCEEDED';
      task.conclusion = event?.algorithmResult ?? 'ANOMALY';
      task.occlusionType = occlusionType;
      task.confidence = 0.87;
      task.modelName = 'occlusion-detector-v1';
      task.modelVersion = '1.4.2';
      task.algorithmVersion = 'adapter-1.0.3';
      task.inferenceDurationMs = 120 + task.pollCount * 10;
      task.startedAbs = task.startedAbs ?? now;
      task.finishedAbs = now;
      task.detectedCount = task.conclusion === 'ANOMALY' ? 1 : 0;
      if (event) {
        // 与后端「成功才回写结论列」同语义
        event.algorithmStatus = 'SUCCEEDED';
        event.algorithmResult = task.conclusion;
        event.confidence = task.confidence;
      }
    }
  }

  return materializeAlgorithmTask(task, now);
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
 * 与真实后端实测样例同形（注意 `playableUrl` / `expiresAt` 不可播放时是**空串**，
 * 不是 null）：
 * `{"cameraId":"101","canPlay":false,"expiresAt":"","platformCameraId":"C001",
 *   "playableUrl":"","protocol":"MP4","unavailableReason":"VIDEO_GATEWAY_NOT_READY"}`
 */
export function createStreamInfo(cameraId: string, now: number = Date.now()): any {
  const camera = CAMERA_DEFS.find((c) => c.cameraId === cameraId);

  const base = {
    cameraId,
    canPlay: false as boolean,
    expiresAt: '',
    platformCameraId: camera?.platformCameraId ?? '',
    playableUrl: '',
    protocol: 'MP4',
    unavailableReason: '',
  };

  if (!camera) {
    return { ...base, unavailableReason: 'CAMERA_NOT_FOUND' };
  }

  if (camera.deviceStatus === 'OFFLINE') {
    return { ...base, unavailableReason: 'CAMERA_OFFLINE' };
  }

  if (camera.deviceStatus === 'UNKNOWN') {
    return { ...base, unavailableReason: 'NO_ADDRESS' };
  }

  if (camera.streamBranch === 'HLS') {
    return {
      ...base,
      playableUrl: MOCK_HLS_URL,
      protocol: 'HLS',
      unavailableReason: 'UNSUPPORTED_PROTOCOL',
    };
  }

  if (camera.streamBranch === 'EXPIRED') {
    return { ...base, unavailableReason: 'URL_EXPIRED' };
  }

  return {
    cameraId,
    canPlay: true,
    expiresAt: formatDateTime(now + STREAM_TTL_MS),
    platformCameraId: camera.platformCameraId,
    playableUrl: MOCK_MP4_URL,
    protocol: 'MP4',
    unavailableReason: null,
  };
}

/** 遮挡类型编码 → 中文名（导出与兜底展示用） */
export function occlusionTypeNameOf(code: string): string {
  return OCCLUSION_TYPE_NAME[code] ?? '其他或未知';
}
