/**
 * 仓门异动 Mock - 共享假数据生成器
 *
 * 用途：在后端接口未就绪时，为前端页面提供稳定、可复现的模拟数据。
 *
 * 约束（依据 AI_HANDOFF.md）：
 * - 第24.4节：模拟结果必须标记 MOCK，不得混入真实生产统计。
 * - 第29.1节：测试优先使用合成数据与脱敏图片。
 * - 不得出现任何真实摄像头地址、账号、密码或内部地址。
 *
 * 不引入 mockjs，纯函数生成，保证多次调用结果可复现。
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

/* ---------- 时间基准 ---------- */

/**
 * 事件时间戳基准：**以模块加载时刻为起点回溯**。
 *
 * <p>为什么不用固定日期：历史页默认查询最近 7 天（第18.2节）。
 * 若事件时间写死为某个历史日期，页面运行几天后就会全部落到查询窗口之外，
 * 表现为「历史为空」，而这与真实缺陷（分页/契约）难以区分。</p>
 *
 * <p>因此这里取「模块初始化时刻」作为基准并按偏移量回溯，
 * 使默认 7 天查询在任何时候运行都有数据。</p>
 */
const BASE_TIME = Date.now();

/**
 * 心跳时间：**每次调用时**以当前时刻为基准回溯。
 *
 * <p><b>为什么不能在模块初始化时固定</b>：心跳超时判断（第19.4节，
 * 阈值 90 分钟）依赖「现在」与「上次心跳」的差值。若心跳基准在模块
 * 初始化时固定，dev server 长时间运行后（例如启动 2 小时再打开页面），
 * 所有仓门的心跳都会超过 90 分钟阈值，导致<b>全部仓门被误判为离线</b>，
 * 正常/异常状态无法演示。</p>
 *
 * <p>因此心跳时间必须在每次生成时基于 {@link Date.now()} 计算，
 * 保证「1~5 分钟前有心跳」这一语义恒定成立。</p>
 */
function heartbeatIso(offsetMinutes: number): string {
  const d = new Date(Date.now() - offsetMinutes * 60_000);
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}

function toIso(offsetMinutes: number): string {
  const d = new Date(BASE_TIME - offsetMinutes * 60_000);
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}

/* ---------- 合成图片地址 ---------- */

/**
 * 生成占位图地址。
 * 使用公开的占位图服务，不含任何真实监控画面或内网地址。
 */
let placeholderSeq = 0;
function placeholderUrl(kind: string): string {
  placeholderSeq += 1;
  return `https://picsum.photos/seed/dooranomaly-${kind}-${placeholderSeq}/640/360`;
}

/* ---------- 仓门数据 ---------- */

export const DOOR_NAMES = [
  '1号仓东门',
  '1号仓西门',
  '2号仓东门',

  '2号仓西门',
  '3号仓南门',
  '3号仓北门',
  '4号仓东门',

  '4号仓西门',
  '5号仓大门',
  '6号仓东门',
  '6号仓西门',

  '7号仓北门',
  '8号仓东门',
  '8号仓西门',
  '9号仓南门',

  '散粮仓西门',
];

const WAREHOUSES = [
  { id: 'WH-001', name: '一号粮库' },
  { id: 'WH-002', name: '二号粮库' },
  { id: 'WH-003', name: '三号粮库' },
  { id: 'WH-004', name: '散粮库' },
];

/**
 * 生成仓门状态列表。
 *
 * 覆盖第16.3节要求的全部展示状态：
 * - 正常（deviceStatus ONLINE + 无告警）
 * - 异常（ONLINE + ACTIVE 告警）
 * - 离线（OFFLINE 或心跳超时）
 * - 未知（UNKNOWN）
 * 第一个仓门固定为在线，保证默认选中逻辑可验证。
 */
export function createDoorStatusList(count = 16): any[] {
  const rand = createRandom(20261016);
  const list: any[] = [];

  for (let i = 0; i < count; i += 1) {
    const wh = WAREHOUSES[i % WAREHOUSES.length];
    const cameraId = `CAM-${String(i + 1).padStart(3, '0')}`;

    // 状态分布：多数在线，少量离线与未知
    let deviceStatus = 'ONLINE';
    let alarmStatus = 'NORMAL';
    let heartOffset = 1 + Math.floor(rand() * 5); // 1~5 分钟内有心跳

    if (i === 5) {
      // 离线
      deviceStatus = 'OFFLINE';
      heartOffset = 45;
    } else if (i === 9) {
      // 离线（心跳超时）
      deviceStatus = 'OFFLINE';
      heartOffset = 120;
    } else if (i === 12) {
      // 未知
      deviceStatus = 'UNKNOWN';
      heartOffset = 200;
    } else if (i % 4 === 1) {
      // 异常告警中
      alarmStatus = 'ACTIVE';
    } else if (i % 7 === 3) {
      // 已恢复
      alarmStatus = 'RECOVERED';
    }

    list.push({
      doorId: `DOOR-${String(i + 1).padStart(3, '0')}`,
      doorName: DOOR_NAMES[i] ?? `仓门${i + 1}`,
      warehouseId: wh.id,
      warehouseName: wh.name,
      cameraId,
      cameraCode: cameraId,
      deviceStatus,
      doorPhysicalStatus: deviceStatus === 'OFFLINE' ? 'UNKNOWN' : pickOne(rand, ['OPEN', 'CLOSED', 'CLOSED']),
      alarmStatus,
      lastHeartbeatAt: deviceStatus === 'UNKNOWN' ? null : heartbeatIso(heartOffset),
      latestEventAt: i % 3 === 0 ? toIso(30 + i) : null,
      sortNo: i + 1,
    });
  }

  return list;
}

/* ---------- 事件数据 ---------- */

/**
 * 算法任务状态取值池，覆盖第17.5节全部 7 种。
 */
export const ALGO_STATUS_POOL = [
  'SUCCEEDED',
  'SUCCEEDED',

  'SUCCEEDED',
  'MOCK',

  'MOCK',
  'QUEUED',

  'RUNNING',
  'FAILED',
  'TIMEOUT',

  'SKIPPED',
];

/**
 * 人工处理状态取值池，覆盖第19.7节全部 5 种。
 */
export const PROCESS_STATUS_POOL = [
  'PENDING',
  'PENDING',

  'PENDING',
  'PROCESSING',
  'VERIFIED_TRUE',
  'VERIFIED_FALSE',

  'CLOSED',
];

const ANOMALY_TYPE_POOL = ['ILLEGAL_OPEN', 'LONG_TIME_OPEN', 'ABNORMAL_MOVE', 'DEVICE_OFFLINE', 'OTHER'];

const ANOMALY_TYPE_NAME: Record<string, string> = {
  ILLEGAL_OPEN: '非法开启',
  LONG_TIME_OPEN: '长时间未关闭',
  ABNORMAL_MOVE: '异常移动',
  DEVICE_OFFLINE: '设备离线',
  OTHER: '其他或未知',
};

/**
 * 生成事件列表。
 *
 * @param count 生成条数
 * @param seedOffset 种子偏移，用于生成不同批次（如首页最近5条与历史列表）
 */
export function createEventList(count: number, seedOffset = 0): any[] {
  const rand = createRandom(20261016 + seedOffset);
  const doors = createDoorStatusList();
  const list: any[] = [];

  for (let i = 0; i < count; i += 1) {
    const door = doors[i % doors.length];
    const algorithmStatus = pickOne(rand, ALGO_STATUS_POOL);
    const processStatus = pickOne(rand, PROCESS_STATUS_POOL);
    const anomalyType = pickOne(rand, ANOMALY_TYPE_POOL);

    // 算法未成功或无需识别时，结论为 null，不得伪造为 NORMAL（第17.5节）
    let algorithmResult: string | null = null;
    let confidence: number | null = null;

    if (algorithmStatus === 'SUCCEEDED' || algorithmStatus === 'MOCK') {
      algorithmResult = pickOne(rand, ['NORMAL', 'ANOMALY', 'ANOMALY', 'UNCERTAIN']);
      // 约 15% 概率缺失置信度，用于验证「暂无」而非 0% 的展示
      confidence = rand() < 0.15 ? null : Number((0.55 + rand() * 0.44).toFixed(4));
    }

    const eventNo = `DA${String(100000 + seedOffset * 1000 + i)}`;

    list.push({
      eventId: `EVT-${seedOffset}-${i + 1}`,
      eventNo,
      occurredAt: toIso(60 + i * 37),
      warehouseId: door.warehouseId,
      warehouseName: door.warehouseName,
      doorId: door.doorId,
      doorName: door.doorName,
      cameraId: door.cameraId,
      cameraCode: door.cameraCode,
      anomalyType,
      anomalyTypeName: ANOMALY_TYPE_NAME[anomalyType] ?? '其他或未知',
      algorithmStatus,
      algorithmResult,
      confidence,
      processStatus,
      eventVersion: 1,
    });
  }

  return list;
}

/* ---------- 事件详情 ---------- */

/**
 * 为事件生成证据列表。
 *
 * 第17.4节一期要求：至少一张正常参考图与一张异常证据图，
 * 每张图必须有采集时间，原始资源与标注图分离。
 */
export function createEvidences(eventId: string, cameraCode?: string): any[] {
  const rand = createRandom(eventId.length * 7919);

  const evidences: any[] = [
    {
      evidenceId: `${eventId}-EV-NORMAL`,
      evidenceType: 'NORMAL_REFERENCE',
      originalUrl: placeholderUrl('normal'),
      annotatedUrl: null,
      thumbnailUrl: placeholderUrl('normal-thumb'),
      capturedAt: toIso(5 + Math.floor(rand() * 10)),
      cameraCode,
      contentHash: `sha256-normal-${eventId}`,
      sortNo: 1,
    },
  ];

  // 非 SKIPPED 事件才有异常证据
  evidences.push({
    evidenceId: `${eventId}-EV-ANOMALY`,
    evidenceType: 'ABNORMAL_KEY_FRAME',
    originalUrl: placeholderUrl('anomaly'),
    // 标注图与原图分离，不覆盖
    annotatedUrl: placeholderUrl('anomaly-annotated'),
    thumbnailUrl: placeholderUrl('anomaly-thumb'),
    capturedAt: toIso(Math.floor(rand() * 5)),
    cameraCode,
    contentHash: `sha256-anomaly-${eventId}`,
    sortNo: 2,
  });

  return evidences;
}

/**
 * 生成处理记录列表（追加式，含全部历史）。
 */
export function createProcessRecords(event: any): any[] {
  const { processStatus } = event;
  if (processStatus === 'PENDING') return [];

  const records: any[] = [
    {
      recordId: `${event.eventId}-PR-1`,
      statusBefore: 'PENDING',
      statusAfter: 'PROCESSING',
      processResult: 'VERIFIED_TRUE',
      processOpinion: '已通知现场核实，等待反馈。',
      processedBy: 'zhangsan',
      processedAt: toIso(30),
      source: 'WEB',
    },
  ];

  if (['VERIFIED_TRUE', 'VERIFIED_FALSE', 'CLOSED'].includes(processStatus)) {
    const isTrue = processStatus === 'VERIFIED_TRUE';
    records.push({
      recordId: `${event.eventId}-PR-2`,
      statusBefore: 'PROCESSING',
      statusAfter: processStatus,
      processResult: isTrue ? 'VERIFIED_TRUE' : 'VERIFIED_FALSE',
      processOpinion: isTrue ? '现场确认仓门被非作业人员开启，已按流程上报。' : '经核实为正常作业期间开启，算法误报。',
      processedBy: 'lisi',
      processedAt: toIso(10),
      source: 'WEB',
    });
  }

  return records;
}

/**
 * 生成事件详情。
 */
export function createEventDetail(event: any): any {
  const isMockAlgo = event.algorithmStatus === 'MOCK';

  return {
    ...event,
    eventDesc: `${event.doorName} 于 ${event.occurredAt} 触发${event.anomalyTypeName}。`,
    algorithmTaskNo: `TASK-${event.eventId}`,
    modelName: isMockAlgo ? 'mock-door-detector' : 'door-anomaly-v1',
    modelVersion: isMockAlgo ? 'mock-1.0.0' : '1.2.3',
    algorithmVersion: isMockAlgo ? 'mock-adapter-1.0.0' : 'algo-2.0.1',
    configVersion: 'cfg-20261001',
    inferenceStartedAt: toIso(120),
    inferenceFinishedAt: toIso(119),
    inferenceDurationMs: 180 + Math.floor(Math.random() * 400),
    // 第39.6节：模拟数据必须明确标识
    isSimulated: isMockAlgo,
    evidences: createEvidences(event.eventId, event.cameraCode),
    processRecords: createProcessRecords(event),
  };
}

/* ---------- 播放信息 ---------- */

/**
 * 公开测试视频流（MP4）。
 *
 * <p>安全约束（第29.1节）：只使用公开测试流，
 * 不含任何真实摄像头地址、账号、密码或内部地址。</p>
 */
const MOCK_MP4_URL = 'https://test-streams.mux.dev/x36xhzz/x36xhzz.mp4';

/**
 * 生成视频播放信息。
 *
 * 第26.1节：Mock 阶段使用浏览器原生可播放的 MP4。
 * 一期采用原生 {@code <video>}（不引入播放器依赖），
 * 因此 Mock 默认返回 MP4，可被浏览器直接播放。
 *
 * 覆盖分支：
 * - 离线仓门 → canPlay:false + CAMERA_OFFLINE（第31节）
 * - 无播放地址 → canPlay:false + NO_ADDRESS
 * - HLS 协议 → canPlay:true，用于验证「协议不支持」的明确提示
 */
export function createStreamInfo(doorId: string, cameraId?: string): any {
  const doors = createDoorStatusList();
  const door = doors.find((d) => d.doorId === doorId);

  // 离线仓门返回不可播放，验证第31节的离线分支
  if (door && door.deviceStatus === 'OFFLINE') {
    return {
      doorId,
      cameraId,
      playableUrl: null,
      protocol: 'MP4',
      expiresAt: null,
      canPlay: false,
      unavailableReason: 'CAMERA_OFFLINE',
    };
  }

  // 第31节：无播放地址场景（无地址应明确提示，不应伪装成播放中）
  if (door && door.deviceStatus === 'UNKNOWN') {
    return {
      doorId,
      cameraId,
      playableUrl: null,
      protocol: 'MP4',
      expiresAt: null,
      canPlay: false,
      unavailableReason: 'NO_ADDRESS',
    };
  }

  // 固定一个仓门返回 HLS，用于验证「本期不支持该协议」的明确提示
  if (doorId === 'DOOR-003') {
    return {
      doorId,
      cameraId,
      playableUrl: 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8',
      protocol: 'HLS',
      expiresAt: heartbeatIso(-5),
      canPlay: true,
      unavailableReason: null,
    };
  }

  return {
    doorId,
    cameraId,
    // 占位视频地址，非真实监控流
    playableUrl: MOCK_MP4_URL,
    protocol: 'MP4',
    expiresAt: heartbeatIso(-5), // 5 分钟后失效
    canPlay: true,
    unavailableReason: null,
  };
}
