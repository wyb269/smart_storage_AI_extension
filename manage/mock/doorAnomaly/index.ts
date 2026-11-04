/**
 * 仓门异动功能 - Mock 接口
 *
 * 基线依据：AI_HANDOFF.md 第 21.1 节接口基线
 *
 * 用途：后端接口未就绪时驱动前端页面开发与自测。
 * 注意：Mock 通过不代表后端联调成功（第 11.2 节）。
 *
 * 覆盖能力：
 * - 服务端真分页（按 pageNo/pageSize 切片并返回 total）
 * - 全部 7 种算法状态、5 种人工处理状态
 * - 置信度缺失场景（验证「暂无」而非 0%）
 * - 查看即标记为已处理（2026-10-21 产品变更后的人工处理入口已取消）
 * - 导出返回真实 xlsx 二进制
 */

import type { MockMethod } from 'vite-plugin-mock';

import { createDoorStatusList, createEventDetail, createEventList, createStreamInfo } from './fixtures';
import { buildXlsx } from './xlsx';

/** 与后端 ExcelExportController 一致的 OOXML MIME */
const XLSX_MIME = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

/**
 * 模拟数据存储。
 * 使用模块级变量，使处理提交能真正改变状态，
 * 从而验证前端的状态刷新与并发控制逻辑。
 */
const EVENT_STORE: any[] = createEventList(120, 1);

/** 成功响应包装，模拟后端 ActionResult 结构 */
function ok(result: unknown) {
  return {
    status: {
      success: true,
      code: '200',
      message: '请求成功',
      requestId: 'mock-request-id',
      timestamp: new Date().toISOString(),
    },
    result,
  };
}

/** 失败响应包装 */
function fail(message: string, code = '4001') {
  return {
    status: {
      success: false,
      code,
      message,
      requestId: 'mock-request-id',
      timestamp: new Date().toISOString(),
    },
    result: null as unknown,
  };
}

/** 字段归一化：兼容前端 pick() 的驼峰/下划线双查找 */
function toSnake(source: Record<string, unknown>) {
  const result: Record<string, unknown> = {};
  Object.keys(source).forEach((key) => {
    const snake = key.replace(/[A-Z]/g, (m) => `_${m.toLowerCase()}`);
    result[key] = source[key];
    result[snake] = source[key];
  });
  return result;
}

/**
 * 按查询条件筛选事件，并默认按发生时间倒序。
 *
 * 历史分页与导出共用同一份筛选实现：两者条件不同步的话，
 * 「导出的是筛过的、列表是另一套」这种问题在 Mock 阶段就发现不了。
 */
function filterEvents(body: any): any[] {
  const { eventNo, warehouseId, doorId, cameraCode, anomalyType, processStatus, startTime, endTime } = body ?? {};

  let list = [...EVENT_STORE];

  // 逐项过滤，模拟后端 where 条件
  if (eventNo) list = list.filter((e) => e.eventNo.includes(eventNo));
  if (warehouseId) list = list.filter((e) => e.warehouseId === warehouseId);
  if (doorId) list = list.filter((e) => e.doorId === doorId);
  if (cameraCode) list = list.filter((e) => e.cameraCode?.includes(cameraCode));
  if (anomalyType) list = list.filter((e) => e.anomalyType === anomalyType);
  if (processStatus) list = list.filter((e) => e.processStatus === processStatus);
  if (startTime) {
    list = list.filter((e) => new Date(e.occurredAt) >= new Date(startTime));
  }
  if (endTime) {
    list = list.filter((e) => new Date(e.occurredAt) <= new Date(endTime));
  }

  return list.sort((a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime());
}

/**
 * 从请求上下文中取出路径参数。
 *
 * <p><b>为什么不能直接用 {@code params}</b>：本项目使用的
 * {@code vite-plugin-mock@3} 在 dev 模式下传给 handler 的是
 * {@code { url, body, query, headers }}，其中 {@code url} 为<b>原始请求 URL</b>，
 * <b>不包含解析后的 params</b>。因此 {@code :eventId} 这类路径参数必须自行从
 * URL 中截取，否则详情与处理接口会一律返回「事件不存在」。</p>
 *
 * <p>优先使用 params（兼容未来版本或 prod mock 可能提供的解析结果），
 * 缺失时回退到 URL 截取。</p>
 *
 * @param opt    handler 收到的请求上下文
 * @param name   路径参数名，如 eventId
 * @param prefix 路径前缀（去掉查询串后的路径起点）
 */
function pathParam(opt: any, name: string, prefix: string): string {
  // 1. 优先读取框架解析出的 params
  const fromParams = opt?.params?.[name];
  if (fromParams) return String(fromParams);

  // 2. 回退：从原始 URL 中截取路径末段
  const rawUrl = String(opt?.url ?? '');
  // 去掉查询串
  const path = rawUrl.split('?')[0];
  // 取最后一段非空路径
  const segments = path.split('/').filter(Boolean);
  const last = segments[segments.length - 1] ?? '';
  // 末段既不是前缀也不是占位符时才认为是参数值
  if (!last || last === prefix || last.startsWith(':')) return '';
  return decodeURIComponent(last);
}

/**
 * Mock 接口注册前缀。
 *
 * <p>前端请求会先拼接 {@code import.meta.env.VITE_API_URL_PREFIX}（mock 模式为
 * {@code /api}），再拼接 {@code src/api/doorAnomaly.ts} 中的路径，因此
 * handler 的注册路径<b>必须包含同一前缀</b>，否则请求无法命中 handler
 * （表现为「Mock 模式仍无数据」）。</p>
 *
 * <p>项目既有 mock（{@code mock/index.ts}）同样使用 {@code /api} 前缀，
 * 此处保持一致，不引入第二套前缀规则。</p>
 */
const P = '/api/doorAnomaly';

/* ==================== 算法任务（「重新识别」） ==================== */

/** 后端时间字符串格式 `yyyy-MM-dd HH:mm:ss`（算法任务接口约定） */
function formatDateTime(ms: number): string {
  const d = new Date(ms);
  const pad = (n: number) => String(n).padStart(2, '0');
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}

interface MockAlgoTask {
  taskId: string;
  taskNo: string;
  eventId: string;
  executionStatus: string;
  conclusion: string | null;
  /** 仓门异动特有：标准化异动类型（后端字段名即 anomalyType） */
  anomalyType: string | null;
  confidence: number | null;
  modelName: string | null;
  modelVersion: string | null;
  algorithmVersion: string | null;
  configVersion: string | null;
  inferenceDurationMs: number | null;
  /** 历史任务：距 now 的毫秒偏移（每次请求现算，避免时间固化） */
  startedAgoMs: number | null;
  finishedAgoMs: number | null;
  /** 本次运行提交的实时任务：绝对毫秒 */
  startedAbs: number | null;
  finishedAbs: number | null;
  detectedCount: number | null;
  simulated: boolean;
  errorCode: string | null;
  errorMessage: string | null;
  /** 本次运行「重新识别」提交的任务（走轮询状态机） */
  live: boolean;
  pollCount: number;
}

/** 每个事件预置一条历史任务，状态镜像事件的算法状态（MOCK 归一为 SUCCEEDED） */
function buildLegacyTasks(): MockAlgoTask[] {
  return EVENT_STORE.map((event, idx) => {
    const status = event.algorithmStatus === 'MOCK' ? 'SUCCEEDED' : event.algorithmStatus;
    const succeeded = status === 'SUCCEEDED';
    const startedAgoMs = (idx + 1) * 60_000;
    const durationMs = succeeded ? 120 + ((idx * 37) % 480) : null;
    return {
      taskId: `MOCK-DA-${event.eventId}`,
      taskNo: `MOCK-DA-${event.eventId}`,
      eventId: event.eventId,
      executionStatus: status,
      conclusion: succeeded ? (event.algorithmResult ?? 'ANOMALY') : null,
      anomalyType: succeeded ? event.anomalyType : null,
      confidence: succeeded ? event.confidence : null,
      modelName: succeeded ? 'mock-door-detector' : null,
      modelVersion: succeeded ? 'mock-1.0.0' : null,
      algorithmVersion: succeeded ? 'mock-adapter-1.0.0' : null,
      configVersion: 'mock-config-1',
      inferenceDurationMs: durationMs,
      startedAgoMs,
      finishedAgoMs: succeeded && durationMs !== null ? startedAgoMs - durationMs : null,
      startedAbs: null,
      finishedAbs: null,
      detectedCount: succeeded ? (event.algorithmResult === 'NORMAL' ? 0 : 1) : null,
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

/** 模块级内存任务表：历史任务 + 本次运行提交的实时任务 */
const ALGO_TASK_STORE: MockAlgoTask[] = buildLegacyTasks();

let algoTaskSeq = 0;

function materializeAlgoTask(task: MockAlgoTask, now: number): any {
  const startedMs =
    task.startedAbs !== null ? task.startedAbs : task.startedAgoMs !== null ? now - task.startedAgoMs : null;
  const finishedMs =
    task.finishedAbs !== null ? task.finishedAbs : task.finishedAgoMs !== null ? now - task.finishedAgoMs : null;
  return {
    taskId: task.taskId,
    taskNo: task.taskNo,
    executionStatus: task.executionStatus,
    conclusion: task.conclusion,
    anomalyType: task.anomalyType,
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
function listTasksOf(eventId: string, now: number): any[] {
  return ALGO_TASK_STORE.filter((t) => t.eventId === eventId)
    .map((t) => materializeAlgoTask(t, now))
    .sort((a, b) => String(b.startedAt ?? '').localeCompare(String(a.startedAt ?? '')))
    .slice(0, 20);
}

/**
 * 实时任务状态机：QUEUED → 第 1、2 次查询 RUNNING → 第 3 次起 SUCCEEDED。
 * 不允许「一查询就成功」，否则前端轮询逻辑在 Mock 下无法被验证。
 */
function advanceLiveTask(task: MockAlgoTask, now: number): void {
  if (!task.live || task.executionStatus === 'SUCCEEDED') return;
  task.pollCount += 1;
  if (task.pollCount === 1) {
    task.executionStatus = 'RUNNING';
  } else if (task.pollCount >= 3) {
    task.executionStatus = 'SUCCEEDED';
    task.conclusion = 'ANOMALY';
    task.confidence = 0.88;
    task.modelName = 'mock-door-detector';
    task.modelVersion = 'mock-1.0.0';
    task.algorithmVersion = 'mock-adapter-1.0.0';
    task.inferenceDurationMs = 120 + task.pollCount;
    task.startedAbs = task.startedAbs ?? now;
    task.finishedAbs = now;
    task.detectedCount = 1;

    // 与后端「成功才回写结论列」同语义，不动处理状态
    const event = EVENT_STORE.find((e) => e.eventId === task.eventId);
    if (event) {
      event.algorithmStatus = 'SUCCEEDED';
      event.algorithmResult = task.conclusion;
      event.confidence = task.confidence;
    }
  }
}

export default [
  /* ============ 1. 仓门状态列表 ============ */
  {
    url: `${P}/listDoorStatus`,
    method: 'post',
    response: () => ok(createDoorStatusList(16).map(toSnake)),
  },

  /* ============ 2. 视频播放信息 ============ */
  {
    url: `${P}/getStreamInfo`,
    method: 'post',
    response: (opt: any) => {
      // 契约：doorId 通过 JSON 请求体传递（P0-2，与后端 @RequestBody 对齐）
      const doorId = opt?.body?.doorId;
      if (!doorId) return fail('仓门标识不能为空');
      const cameraId = createDoorStatusList(16).find((d) => d.doorId === doorId)?.cameraId;
      return ok(toSnake(createStreamInfo(doorId, cameraId)));
    },
  },

  /* ============ 3. 首页最近事件 ============ */
  {
    url: `${P}/listRecentEvent`,
    method: 'post',
    response: (opt: any) => {
      const limit = Number(opt?.body?.limit ?? 5);
      // 第16.5节：按发生时间倒序
      const list = [...EVENT_STORE]
        .sort((a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime())
        .slice(0, limit);
      return ok(list.map(toSnake));
    },
  },

  /* ============ 4. 历史记录服务端分页 ============ */
  {
    url: `${P}/listEventByCondition`,
    method: 'post',
    response: (opt: any) => {
      const { pageNo = 1, pageSize = 10 } = opt?.body ?? {};
      const list = filterEvents(opt?.body ?? {});

      // 真实服务端分页：先算 total，再切片
      const total = list.length;
      const start = (pageNo - 1) * pageSize;
      const pageData = list.slice(start, start + pageSize);

      return ok({
        data: pageData.map(toSnake),
        total,
      });
    },
  },

  /* ============ 5. 事件详情 ============ */
  {
    url: `${P}/getEventDetail/:eventId`,
    method: 'post',
    response: (opt: any) => {
      const eventId = pathParam(opt, 'eventId', 'getEventDetail');
      if (!eventId) return fail('事件标识不能为空');
      const event = EVENT_STORE.find((e) => e.eventId === eventId);
      if (!event) return fail('事件不存在');
      const detail = createEventDetail(event) as any;
      // 算法任务历史随详情一次带回（契约 §1），最新一条供摘要行使用
      const tasks = listTasksOf(eventId, Date.now()).map((t) => toSnake(t));
      detail.algorithmTasks = tasks;
      detail.latestAlgorithmTask = tasks[0] ?? null;
      return ok(toSnake(detail));
    },
  },

  /* ============ 6. 查看后标记为已处理 ============ */
  /**
   * 2026-10-21 产品变更：取消人工处理入口，打开详情即标记为已处理。
   *
   * 与后端 `markEventViewed` 保持同一语义，否则 Mock 通过但真实后端行为不同：
   * - 只接受事件标识，状态与处理人由服务端决定，不接受前端传入；
   * - 仅 PENDING 会被写成 CLOSED，其余幂等返回不重复改版本；
   * - 版本号自增，与后端乐观锁行为一致。
   */
  {
    url: `${P}/markEventViewed/:eventId`,
    method: 'post',
    response: (opt: any) => {
      const eventId = pathParam(opt, 'eventId', 'markEventViewed');
      if (!eventId) return fail('事件标识不能为空');

      const event = EVENT_STORE.find((e) => e.eventId === eventId);
      if (!event) return fail('事件不存在或无权查看', '4040');

      // 已是「已处理」：幂等返回，不追加审计、不自增版本
      if (event.processStatus !== 'PENDING') {
        return ok('该事件已是已处理状态');
      }

      event.processStatus = 'CLOSED';
      event.processOpinion = '查看事件详情后自动标记为已处理';
      event.processedBy = 'currentUser';
      event.processedAt = new Date().toISOString();
      event.eventVersion += 1;

      return ok('已标记为已处理');
    },
  },

  /* ============ 7. 导出事件（返回真实 xlsx 二进制） ============ */
  /**
   * 之前**没有**这个 handler，Mock 模式下点导出会被 vite 代理转发到
   * 空的后端地址，返回 500。
   *
   * 用 `rawResponse` 而不是 `response`：后者会被
   * `res.end(JSON.stringify(...))` 序列化，二进制无法通过它返回。
   *
   * 列与表头对齐后端 `ExcelExportConfig` 里注册的 `doorAnomalyEvent` 模板，
   * 这样 Mock 与真实后端导出的内容一致，验收时不会只验到一半。
   */
  {
    url: `${P}/exportEvent`,
    method: 'post',
    rawResponse(req, res) {
      void (async () => {
        const body = (await this.parseJson()) ?? {};
        const limit = Math.min(Number(body.limit ?? 5000) || 5000, 5000);
        const rows = filterEvents(body).slice(0, limit);

        const header = [
          '事件编号',
          '发生时间',
          '仓库名称',
          '仓门位置',
          '摄像头',
          '异动类型',
          '算法状态',
          '算法结论',
          '置信度',
          '处理状态',
          '处理人',
          '处理时间',
          '处理意见',
        ];
        const data = rows.map((e) => [
          e.eventNo,
          e.occurredAt,
          e.warehouseName,
          e.doorName,
          e.cameraCode,
          e.anomalyTypeName ?? e.anomalyType,
          e.algorithmStatus,
          e.algorithmResult,
          e.confidence,
          // 与页面一致：只呈现「未处理 / 已处理」两态
          e.processStatus === 'PENDING' ? '未处理' : '已处理',
          e.processedBy,
          e.processedAt,
          e.processOpinion,
        ]);

        const xlsx = buildXlsx('仓门异动记录', [header, ...data]);
        const fileName = `仓门异动记录_${Date.now()}.xlsx`;

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

  /* ============ 算法：提交重新识别 ============ */
  /**
   * 只受理并立即返回 taskId（QUEUED），绝不同步等推理结果；
   * 前端按 800ms 轮询下方 algorithmTask 拿进度。
   */
  {
    url: `${P}/rerunAlgorithm/:eventId`,
    method: 'post',
    response: (opt: any) => {
      const eventId = pathParam(opt, 'eventId', 'rerunAlgorithm');
      if (!eventId) return fail('事件标识不能为空');
      const event = EVENT_STORE.find((e) => e.eventId === eventId);
      if (!event) return fail('事件不存在或无权查看', '4040');

      algoTaskSeq += 1;
      const now = Date.now();
      const taskId = `MOCK-DA-TASK-${now.toString(36)}${algoTaskSeq}`;
      ALGO_TASK_STORE.push({
        taskId,
        taskNo: taskId,
        eventId,
        executionStatus: 'QUEUED',
        conclusion: null,
        anomalyType: null,
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
      return ok({ taskId, executionStatus: 'QUEUED', simulated: true });
    },
  },

  /* ============ 算法：轮询任务状态 ============ */
  {
    url: `${P}/algorithmTask/:taskId`,
    method: 'post',
    response: (opt: any) => {
      const taskId = pathParam(opt, 'taskId', 'algorithmTask');
      if (!taskId) return fail('任务标识不能为空');
      const task = ALGO_TASK_STORE.find((t) => t.taskId === taskId || t.taskNo === taskId);
      if (!task) return fail('算法任务不存在', '4040');
      advanceLiveTask(task, Date.now());
      return ok(toSnake(materializeAlgoTask(task, Date.now())));
    },
  },

  /* ============ 算法：事件历史任务列表 ============ */
  {
    url: `${P}/listAlgorithmTasks/:eventId`,
    method: 'post',
    response: (opt: any) => {
      const eventId = pathParam(opt, 'eventId', 'listAlgorithmTasks');
      if (!eventId) return fail('事件标识不能为空');
      return ok(listTasksOf(eventId, Date.now()).map((t) => toSnake(t)));
    },
  },

  /* ============ 8. 空数据场景（验证空态不报错） ============ */
  {
    url: `${P}/listEventByConditionEmpty`,
    method: 'post',
    response: () => ok({ data: [], total: 0 }),
  },

  /* ============ 9. 空仓门场景（第31节「没有仓门」） ============ */
  {
    url: `${P}/listDoorStatusEmpty`,
    method: 'post',
    response: () => ok([]),
  },
] as MockMethod[];
