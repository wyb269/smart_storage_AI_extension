/**
 * 人员行为监测功能 - Mock 接口
 *
 * 用途：`VITE_USE_MOCK=true` 时驱动前端页面开发与自测，不需要后端。
 * 注意：Mock 通过不代表后端联调通过，两者的空值/分页语义仍需实测核对。
 *
 * 形状依据（真实后端实测确认）：
 * - 信封：`{"attached":null,"result":<业务数据>,"status":{...}}`
 * - 时间：`yyyy-MM-dd HH:mm:ss` 字符串（不是 ISO，不是毫秒数）
 * - 分页：`result.data` / `result.total`，服务端真分页
 *
 * 覆盖能力：
 * - 7 个接口 + 算法 3 端点（rerunAlgorithm / algorithmTask / listAlgorithmTasks）全覆盖
 * - 正常 / 人员聚集 / 未戴安全帽 / 离线 / 未知 五种区域展示态
 * - 5 种行为类型、7 种算法状态、置信度缺失（「暂无」而非 0%）
 * - 处理状态三态（未核实 / 处理中 / 已处理）
 * - 查看即标记为已处理（含幂等，且不动 PROCESSING）
 * - 算法任务「先 QUEUED/RUNNING、多次查询后才 SUCCEEDED」的轮询状态机
 * - 导出返回真实 xlsx 二进制（复用 `mock/doorAnomaly/xlsx.ts`，不复制实现）
 *
 * ⚠️ 时间戳：所有假数据的时间都由 `fixtures.ts` 在**每次请求时**基于
 * `Date.now()` 现算。页面按 90 分钟心跳阈值判定离线，固化时间戳会让
 * dev server 跑久后整页误判为「离线」（仓门那一版踩过，详见 fixtures 头部注释）。
 */

import type { MockMethod } from 'vite-plugin-mock';

import { buildXlsx } from '../doorAnomaly/xlsx';
import {
  behaviorTypeNameOf,
  buildEventDetail,
  createMonitorPointList,
  createStreamInfo,
  findEvent,
  markViewed,
  materializeAlgorithmTasks,
  materializeEvents,
  processStatusNameOf,
  queryAlgorithmTask,
  rerunAlgorithmTask,
} from './fixtures';

/** 与后端 ExcelExportController 一致的 OOXML MIME */
const XLSX_MIME = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

/**
 * Mock 接口注册前缀。
 *
 * 前端请求先拼 `VITE_API_URL_PREFIX`（mock 模式为 `/api`），再拼
 * `src/api/personBehavior.ts` 里的路径，因此 handler 必须带同一前缀，
 * 否则请求打不到 handler（表现为「Mock 模式无数据」）。
 */
const P = '/api/personBehavior';

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
 * 处理状态三值都是库里真实取值，直接等值比较，
 * 不像遮挡那版需要额外支持页面组合概念 `PROCESSED`。
 */
function queryEvents(body: any, now: number): any[] {
  const { eventNo, warehouseId, pointId, cameraCode, behaviorType, processStatus, startTime, endTime } = body ?? {};

  const startMs = toMs(startTime);
  const endMs = toMs(endTime);

  let list = materializeEvents(now);

  if (eventNo) list = list.filter((e) => String(e.eventNo).includes(String(eventNo)));
  if (warehouseId) list = list.filter((e) => e.warehouseId === String(warehouseId));
  if (pointId) list = list.filter((e) => e.pointId === String(pointId));
  if (cameraCode) list = list.filter((e) => String(e.cameraCode).includes(String(cameraCode)));
  if (behaviorType) list = list.filter((e) => e.behaviorType === behaviorType);
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
 * 表头与真实后端导出模板逐列对齐（12 列，顺序不可变）。
 * 后端模板 key 是 `personBehaviorEvent`，注册在 ExcelExportConfig.config。
 */
const EXPORT_HEADER = [
  '事件编号',
  '发生时间',
  '仓库名称',
  '行为类型',
  '摄像头',
  '监测区域',
  '算法结论',
  '置信度',
  '处理状态',
  '处理人',
  '处理时间',
  '处理意见',
];

const ALGORITHM_RESULT_NAME: Record<string, string> = {
  NORMAL: '正常',
  ANOMALY: '行为异常',
  UNCERTAIN: '无法判断',
};

/** 置信度：null 写「暂无」，避免被读成 0% */
function confidenceText(value: unknown): string {
  if (value === null || value === undefined) return '暂无';
  return `${(Number(value) * 100).toFixed(2)}%`;
}

function exportFileName(now: number): string {
  const d = new Date(now);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `人员行为记录_${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}.xlsx`;
}

/* ==================== 接口注册 ==================== */

export default [
  /* ============ 1. 监测区域状态列表 ============ */
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
      if (!pointId) return fail('监测区域标识不能为空');
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

      return ok(detail);
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

      // 幂等：详情接口通常已经把它写成 DONE，这里不再重复追加审计与版本
      const changed = markViewed(event, Date.now());
      return ok(changed ? '已标记为已处理' : '该事件无需重复标记');
    },
  },

  /* ============ 7. 导出事件（真实 xlsx 二进制） ============ */
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
          e.behaviorTypeName ?? behaviorTypeNameOf(e.behaviorType),
          e.cameraCode,
          e.areaName,
          e.algorithmResult ? (ALGORITHM_RESULT_NAME[e.algorithmResult] ?? e.algorithmResult) : '暂无',
          confidenceText(e.confidence),
          // 与页面一致：未核实 / 处理中 / 已处理 三态
          processStatusNameOf(e.processStatus),
          e.processedBy ?? '',
          e.processedAt ?? '',
          e.processOpinion ?? '',
        ]);

        const xlsx = buildXlsx('人员行为记录', [EXPORT_HEADER, ...data]);
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

  /* ============ 8. 空数据场景（验证空态不报错） ============ */
  {
    url: `${P}/listEventByConditionEmpty`,
    method: 'post',
    response: () => ok({ data: [], total: 0 }),
  },

  /* ============ 9. 无区域场景（「暂无可监测区域」） ============ */
  {
    url: `${P}/listMonitorPointEmpty`,
    method: 'post',
    response: () => ok([]),
  },

  /* ============ 10. 提交「重新识别」（立即返回 taskId） ============ */
  {
    url: `${P}/rerunAlgorithm/:eventId`,
    method: 'post',
    response: (opt: any) => {
      const eventId = pathParam(opt, 'rerunAlgorithm');
      if (!eventId) return fail('事件标识不能为空');

      const event = findEvent(eventId);
      if (!event) return fail('事件不存在或无权查看', '9103');

      return ok(rerunAlgorithmTask(eventId, Date.now()));
    },
  },

  /* ============ 11. 轮询算法任务（QUEUED → RUNNING → SUCCEEDED） ============ */
  {
    url: `${P}/algorithmTask/:taskId`,
    method: 'post',
    response: (opt: any) => {
      const taskId = pathParam(opt, 'algorithmTask');
      if (!taskId) return fail('任务标识不能为空');

      const task = queryAlgorithmTask(taskId, Date.now());
      if (!task) return fail('算法任务不存在', '9104');

      return ok(task);
    },
  },

  /* ============ 12. 事件历史算法任务列表 ============ */
  {
    url: `${P}/listAlgorithmTasks/:eventId`,
    method: 'post',
    response: (opt: any) => {
      const eventId = pathParam(opt, 'listAlgorithmTasks');
      if (!eventId) return fail('事件标识不能为空');

      return ok(materializeAlgorithmTasks(eventId, Date.now()));
    },
  },
] as MockMethod[];
