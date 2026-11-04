/**
 * 地面积水监测功能 - Mock 接口
 *
 * 用途：`VITE_USE_MOCK=true` 时驱动前端页面开发与自测，不需要后端。
 * 注意：Mock 通过不代表后端联调通过，两者的空值/分页语义仍需实测核对。
 *
 * 形状依据（与真实后端约定一致）：
 * - 信封：`{"attached":null,"result":<业务数据>,"status":{...}}`
 * - 时间：`yyyy-MM-dd HH:mm:ss` 字符串（不是 ISO，不是毫秒数）
 * - 分页：`result.data` / `result.total`，服务端真分页，参数名 `pageNo`
 *
 * 覆盖能力：
 * - 全部接口覆盖（含重新识别三端点）
 * - 正常 / 积水 / 离线 / 未知 四种点位展示态
 * - 4 种积水结论类型、7 种算法状态、置信度缺失（「暂无」而非 0%）
 * - 处理状态三态（未核实 / 处理中 / 已处理）
 * - 查看即标记为已处理（含幂等，且只推进 PENDING）
 * - 算法任务：提交只落 QUEUED，轮询若干次后才 SUCCEEDED（模拟结果恒为 true）
 * - 导出返回真实 xlsx 二进制（复用 `mock/doorAnomaly/xlsx.ts`，不复制实现）
 *
 * ⚠️ 时间戳：所有假数据的时间都由 `fixtures.ts` 在**每次请求时**基于
 * `Date.now()` 现算。页面按 90 分钟心跳阈值判定离线，固化时间戳会让
 * dev server 跑久后整页误判为「离线」（仓门那一版踩过，详见 fixtures 头部注释）。
 */

import type { MockMethod } from 'vite-plugin-mock';

import { buildXlsx } from '../doorAnomaly/xlsx';
import {
  buildEventDetail,
  createMonitorPointList,
  createStreamInfo,
  findEvent,
  formatDateTime,
  markViewed,
  materializeEvents,
  waterTypeNameOf,
} from './fixtures';

/** 与后端 ExcelExportController 一致的 OOXML MIME */
const XLSX_MIME = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

/**
 * Mock 接口注册前缀。
 *
 * 前端请求先拼 `VITE_API_URL_PREFIX`（mock 模式为 `/api`），再拼
 * `src/api/groundWater.ts` 里的路径，因此 handler 必须带同一前缀，
 * 否则请求打不到 handler（表现为「Mock 模式无数据」）。
 */
const P = '/api/groundWater';

/* ==================== 响应信封 ==================== */

/** 32 位十六进制请求 ID，与真实后端的 requestId 形状一致 */
function newRequestId(): string {
  let hex = '';
  while (hex.length < 32) {
    hex += Math.floor(Math.random() * 0xffffffff)
      .toString(16)
      .padStart(8, '0');
  }
  return hex.slice(0, 32);
}

/** 后端时间字符串格式 `yyyy-MM-dd HH:mm:ss` */
function nowText(): string {
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}

/** 成功信封（`attached` 恒为 null，与后端 ActionResult 同形） */
function ok(result: unknown) {
  return {
    attached: null as null, // 显式标注：裸 null 在 noImplicitAny 下会推成隐式 any
    result,
    status: {
      code: '200',
      message: '请求成功',
      requestId: newRequestId(),
      success: true,
      timestamp: nowText(),
    },
  };
}

/** 失败信封：`result` 为 null，`success` 为 false */
function fail(message: string, code = '-1') {
  return {
    attached: null as null, // 显式标注：裸 null 在 noImplicitAny 下会推成隐式 any
    result: null as unknown,
    status: {
      code,
      message,
      requestId: newRequestId(),
      success: false,
      timestamp: nowText(),
    },
  };
}

/* ==================== 查询条件 ==================== */

/**
 * 解析后端时间字符串为毫秒。
 * 不依赖 `new Date('yyyy-MM-dd HH:mm:ss')` 的引擎差异，手工按本地时区拆解。
 */
function toMs(value: unknown): number | null {
  if (!value) return null;
  const m = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})/.exec(String(value));
  if (!m) {
    const n = new Date(String(value)).getTime();
    return Number.isNaN(n) ? null : n;
  }
  return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]), Number(m[4]), Number(m[5]), Number(m[6])).getTime();
}

/**
 * 按条件筛选并物化事件，默认发生时间倒序。
 *
 * 历史分页与导出共用同一实现：两者条件不同步的话，
 * 「导出的是筛过的、列表是另一套」这类问题在 Mock 阶段发现不了。
 *
 * <p>处理状态三态（PENDING/PROCESSING/DONE）与后端列取值一一对应，
 * 因此这里不再有遮挡那版的组合概念 `PROCESSED`。</p>
 */
function queryEvents(body: any, now: number): any[] {
  const { eventNo, warehouseId, pointId, cameraCode, waterType, processStatus, startTime, endTime } = body ?? {};

  const startMs = toMs(startTime);
  const endMs = toMs(endTime);

  let list = materializeEvents(now);

  if (eventNo) list = list.filter((e) => String(e.eventNo).includes(String(eventNo)));
  if (warehouseId) list = list.filter((e) => e.warehouseId === warehouseId);
  if (pointId) list = list.filter((e) => e.pointId === String(pointId));
  if (cameraCode) list = list.filter((e) => String(e.cameraCode).includes(String(cameraCode)));
  if (waterType) list = list.filter((e) => e.waterType === waterType);
  if (processStatus) {
    list = list.filter((e) =>
      processStatus === 'PENDING' ? e.processStatus !== 'DONE' : e.processStatus === processStatus,
    );
  }
  if (startMs !== null) list = list.filter((e) => toMs(e.occurredAt)! >= startMs);
  if (endMs !== null) list = list.filter((e) => toMs(e.occurredAt)! <= endMs);

  return list.sort((a, b) => toMs(b.occurredAt)! - toMs(a.occurredAt)!);
}

/**
 * 从请求上下文取路径参数。
 *
 * vite-plugin-mock@3 在 dev 模式传给 handler 的是 `{ url, body, query, headers }`，
 * `url` 是原始请求 URL，**不含解析后的 params**，所以 `:eventId` 必须自己截取。
 */
function pathParam(opt: any, prefix: string): string {
  const fromParams = opt?.params?.eventId;
  if (fromParams) return String(fromParams);

  const path = String(opt?.url ?? '').split('?')[0];
  const segments = path.split('/').filter(Boolean);
  const last = segments[segments.length - 1] ?? '';
  if (!last || last === prefix || last.startsWith(':')) return '';
  return decodeURIComponent(last);
}

/* ==================== 导出 ==================== */

/**
 * 表头需与后端 `ExcelExportConfig` 里 `groundWaterEvent` 的 headers 逐列对齐。
 * 顺序不可变：Mock 与真实导出对不上，验收时会误判成后端 bug。
 */
const EXPORT_HEADER = [
  '事件编号',
  '发生时间',
  '仓库名称',
  '监测位置',
  '摄像头',
  '积水类型',
  '算法结论',
  '置信度',
  '处理状态',
  '处理人',
  '处理时间',
  '处理意见',
];

const ALGORITHM_RESULT_NAME: Record<string, string> = {
  NORMAL: '干燥',
  ANOMALY: '积水',
  UNCERTAIN: '无法判断',
};

const PROCESS_STATUS_NAME: Record<string, string> = {
  PENDING: '未核实',
  PROCESSING: '未处理',
  DONE: '已处理',
};

/** 置信度：null 写「暂无」，避免被读成 0% */
function confidenceText(value: unknown): string {
  if (value === null || value === undefined) return '暂无';
  return `${(Number(value) * 100).toFixed(2)}%`;
}

function exportFileName(now: number): string {
  const d = new Date(now);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `地面积水记录_${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}.xlsx`;
}

/* ==================== 算法任务（「重新识别」） ==================== */

/**
 * 内存任务表：Mock 进程存活期间有效，重启后回到空表（与真实后端落库不同，仅用于联调）。
 *
 * 状态推进**不在提交时一步到位**：提交只落 QUEUED，
 * 之后每次 `algorithmTask/{taskId}` 轮询才向前推一格（约 800ms 一次），
 * 保证前端能真实看到「等待识别 → 识别中 → 识别成功」的三种态。
 */
interface MockAlgorithmTask {
  taskId: string;
  taskNo: string;
  eventId: string;
  executionStatus: string;
  conclusion: string | null;
  waterType: string | null;
  confidence: number | null;
  modelName: string | null;
  modelVersion: string | null;
  algorithmVersion: string | null;
  configVersion: string | null;
  startedAtMs: number;
  finishedAtMs: number | null;
  inferenceDurationMs: number | null;
  detectedCount: number | null;
  /** 铁律：模拟结果必须显式标记，页面据此展示「模拟」标签 */
  simulated: boolean;
  errorCode: string | null;
  errorMessage: string | null;
  /** 已被轮询的次数，驱动状态推进 */
  polls: number;
}

const ALGORITHM_TASK_MAP = new Map<string, MockAlgorithmTask>();
/** 每个事件已完成任务的时间基准，保证列表按 started_at 倒序稳定 */
let algorithmTaskSeq = 0;

/** 任务视图（与后端 WaterAlgorithmTaskRespDTO 同形） */
function algorithmTaskView(task: MockAlgorithmTask): any {
  return {
    taskId: task.taskId,
    taskNo: task.taskNo,
    eventId: task.eventId,
    executionStatus: task.executionStatus,
    conclusion: task.conclusion,
    // 地面积水的结论类型列叫 waterType，不是 conclusionType
    waterType: task.waterType,
    confidence: task.confidence,
    modelName: task.modelName,
    modelVersion: task.modelVersion,
    algorithmVersion: task.algorithmVersion,
    configVersion: task.configVersion,
    startedAt: formatDateTime(task.startedAtMs),
    finishedAt: task.finishedAtMs === null ? null : formatDateTime(task.finishedAtMs),
    inferenceDurationMs: task.inferenceDurationMs,
    detectedCount: task.detectedCount,
    simulated: task.simulated,
    errorCode: task.errorCode,
    errorMessage: task.errorMessage,
  };
}

/** 某事件的任务列表（started_at 倒序，最多 20 条，与后端一致） */
function algorithmTasksOf(eventId: string): any[] {
  return Array.from(ALGORITHM_TASK_MAP.values())
    .filter((t) => t.eventId === eventId)
    .sort((a, b) => b.startedAtMs - a.startedAtMs)
    .slice(0, 20)
    .map(algorithmTaskView);
}

/** 详情接口把任务历史一并带回（真实后端 getEventDetail 同语义） */
function withAlgorithmTasks(eventId: string, detail: any): any {
  const tasks = algorithmTasksOf(eventId);
  return { ...detail, algorithmTasks: tasks, latestAlgorithmTask: tasks[0] ?? null };
}

/**
 * 推进任务状态：第 1/2 次轮询 QUEUED/RUNNING，第 3 次起 SUCCEEDED。
 * 成功后顺手把事件行的算法列写成最新结果，列表刷新才能看到变化。
 */
function advanceTask(task: MockAlgorithmTask): void {
  task.polls += 1;
  if (task.polls < 2) {
    task.executionStatus = 'QUEUED';
    return;
  }
  if (task.polls < 3) {
    task.executionStatus = 'RUNNING';
    return;
  }
  if (task.executionStatus === 'SUCCEEDED') return;

  const now = Date.now();
  task.executionStatus = 'SUCCEEDED';
  task.finishedAtMs = now;
  task.inferenceDurationMs = 640 + (algorithmTaskSeq % 5) * 37;
  task.conclusion = 'ANOMALY';
  task.confidence = 0.87;
  task.detectedCount = 1 + (algorithmTaskSeq % 3);

  const event = findEvent(task.eventId);
  if (event) {
    task.waterType = event.waterType;
    event.algorithmStatus = 'SUCCEEDED';
    event.algorithmResult = task.conclusion;
    event.confidence = task.confidence;
  }
}

/* ==================== 接口注册 ==================== */

export default [
  /* ============ 1. 监测点位状态列表 ============ */
  {
    url: `${P}/listMonitorPoint`,
    method: 'post',
    response: () => ok(createMonitorPointList()),
  },

  /* ============ 2. 短时播放信息 ============ */
  {
    url: `${P}/getStreamInfo`,
    method: 'post',
    response: (opt: any) => {
      // 契约：pointId 走 JSON 请求体（与后端 @RequestBody 对齐）
      const pointId = opt?.body?.pointId;
      if (!pointId) return fail('监测点位标识不能为空');
      return ok(createStreamInfo(String(pointId)));
    },
  },

  /* ============ 3. 首页最近事件 ============ */
  {
    url: `${P}/listRecentEvent`,
    method: 'post',
    response: (opt: any) => {
      const limit = Number(opt?.body?.limit ?? 5) || 5;
      const list = queryEvents(opt?.body ?? {}, Date.now()).slice(0, Math.max(0, limit));
      return ok(list);
    },
  },

  /* ============ 4. 历史记录服务端分页 ============ */
  {
    url: `${P}/listEventByCondition`,
    method: 'post',
    response: (opt: any) => {
      const now = Date.now();
      const body = opt?.body ?? {};
      const pageNo = Math.max(1, Number(body.pageNo ?? 1) || 1);
      const pageSize = Math.max(1, Number(body.pageSize ?? 10) || 10);
      const list = queryEvents(body, now);

      // 真分页：先算 total，再按页切片（禁止前端切片假分页）
      const total = list.length;
      const start = (pageNo - 1) * pageSize;

      return ok({
        data: list.slice(start, start + pageSize),
        total,
      });
    },
  },

  /* ============ 5. 事件详情（查看即标记） ============ */
  /**
   * 与真实后端一致：命中 PENDING 事件时，**读接口本身**就会把它标记为已处理
   * （写 process_status / processed_by / processed_at /
   * process_opinion='查看事件详情后自动标记为已处理' 并 event_version+1）。
   *
   * 返回的是**更新前**的快照（processStatus 仍为 PENDING），与后端一致：
   * 前端据此再调用一次 markEventViewed 并刷新列表，页面才能立刻从「未核实」
   * 变成「已处理」。第二次进入同一详情就返回 DONE，且不再重复计数。
   */
  {
    url: `${P}/getEventDetail/:eventId`,
    method: 'post',
    response: (opt: any) => {
      const eventId = pathParam(opt, 'getEventDetail');
      if (!eventId) return fail('事件标识不能为空');

      const event = findEvent(eventId);
      if (!event) return fail('事件不存在或无权查看', '9103');

      const now = Date.now();
      const detail = buildEventDetail(event, now);

      // 先在快照上定格「更新前」状态，再改写内存态
      if (event.processStatus !== 'DONE') markViewed(event, now);

      return ok(withAlgorithmTasks(eventId, detail));
    },
  },

  /* ============ 6. 查看后标记为已处理 ============ */
  {
    url: `${P}/markEventViewed/:eventId`,
    method: 'post',
    response: (opt: any) => {
      const eventId = pathParam(opt, 'markEventViewed');
      if (!eventId) return fail('事件标识不能为空');

      const event = findEvent(eventId);
      if (!event) return fail('事件不存在或无权查看', '9103');

      // 幂等：详情接口通常已经把它写成 DONE，这里不再重复追加审计与版本。
      // PROCESSING 同样不被改写 —— 已有人在现场跟进。
      const changed = markViewed(event, Date.now());
      return ok(changed ? '已标记为已处理' : '该事件无需自动标记');
    },
  },

  /* ============ 7. 提交重新识别（只受理，返回 taskId） ============ */
  {
    url: `${P}/rerunAlgorithm/:eventId`,
    method: 'post',
    response: (opt: any) => {
      const eventId = pathParam(opt, 'rerunAlgorithm');
      if (!eventId) return fail('事件标识不能为空');

      const event = findEvent(eventId);
      if (!event) return fail('事件不存在或无权查看', '9103');

      algorithmTaskSeq += 1;
      const taskId = String(9000 + algorithmTaskSeq);
      const task: MockAlgorithmTask = {
        taskId,
        taskNo: `MOCK-TASK-${String(algorithmTaskSeq).padStart(4, '0')}`,
        eventId,
        executionStatus: 'QUEUED',
        conclusion: null,
        waterType: null,
        confidence: null,
        modelName: event.modelTag,
        modelVersion: 'mock-1.0.0',
        algorithmVersion: 'gw-algo-2.3.1',
        configVersion: 'cfg-2026.10',
        startedAtMs: Date.now(),
        finishedAtMs: null,
        inferenceDurationMs: null,
        detectedCount: null,
        simulated: true,
        errorCode: null,
        errorMessage: null,
        polls: 0,
      };
      ALGORITHM_TASK_MAP.set(taskId, task);

      return ok({
        taskId: task.taskId,
        executionStatus: task.executionStatus,
        simulated: task.simulated,
      });
    },
  },

  /* ============ 8. 轮询算法任务状态 ============ */
  {
    url: `${P}/algorithmTask/:taskId`,
    method: 'post',
    response: (opt: any) => {
      const taskId = pathParam(opt, 'algorithmTask');
      if (!taskId) return fail('算法任务编号不能为空');

      const task = ALGORITHM_TASK_MAP.get(taskId);
      if (!task) return fail('算法任务不存在', '9104');

      // 每次轮询推进一格，避免「提交即成功」这种假同步
      advanceTask(task);
      return ok(algorithmTaskView(task));
    },
  },

  /* ============ 9. 事件算法任务列表 ============ */
  {
    url: `${P}/listAlgorithmTasks/:eventId`,
    method: 'post',
    response: (opt: any) => {
      const eventId = pathParam(opt, 'listAlgorithmTasks');
      if (!eventId) return fail('事件标识不能为空');

      const event = findEvent(eventId);
      if (!event) return fail('事件不存在或无权查看', '9103');

      return ok(algorithmTasksOf(eventId));
    },
  },

  /* ============ 10. 导出事件（真实 xlsx 二进制） ============ */
  {
    url: `${P}/exportEvent`,
    method: 'post',
    rawResponse(req, res) {
      void (async () => {
        const body = (await this.parseJson()) ?? {};
        const now = Date.now();
        const limit = Math.min(Number(body.limit ?? 5000) || 5000, 5000);
        const rows = queryEvents(body, now).slice(0, limit);

        const data = rows.map((e) => [
          e.eventNo,
          e.occurredAt,
          e.warehouseName,
          e.pointName,
          e.cameraCode,
          e.waterTypeName ?? waterTypeNameOf(e.waterType),
          e.algorithmResult ? (ALGORITHM_RESULT_NAME[e.algorithmResult] ?? e.algorithmResult) : '暂无',
          confidenceText(e.confidence),
          // 三态直接展示，与页面口径一致
          PROCESS_STATUS_NAME[e.processStatus] ?? e.processStatus,
          e.processedBy ?? '',
          e.processedAt ?? '',
          e.processOpinion ?? '',
        ]);

        const xlsx = buildXlsx('地面积水记录', [EXPORT_HEADER, ...data]);
        const fileName = exportFileName(now);

        res.statusCode = 200;
        res.setHeader('Content-Type', XLSX_MIME);
        res.setHeader('Content-Disposition', `attachment;filename=${encodeURIComponent(fileName)}`);
        res.setHeader('Content-Length', String(xlsx.length));
        res.end(xlsx);
      })().catch(() => {
        res.statusCode = 500;
        res.end();
      });
    },
  },

  /* ============ 11. 空数据场景（验证空态不报错） ============ */
  {
    url: `${P}/listEventByConditionEmpty`,
    method: 'post',
    response: () => ok({ data: [], total: 0 }),
  },

  /* ============ 12. 无点位场景（「暂无可监测点位」） ============ */
  {
    url: `${P}/listMonitorPointEmpty`,
    method: 'post',
    response: () => ok([]),
  },
] as MockMethod[];
