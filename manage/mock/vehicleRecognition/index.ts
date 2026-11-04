/**
 * 车辆智能识别功能 - Mock 接口
 *
 * 用途：`VITE_USE_MOCK=true` 时驱动前端页面开发与自测，不需要后端。
 * 注意：Mock 通过不代表后端联调通过，两者的空值/分页语义仍需实测核对。
 *
 * 形状依据（真实后端 DTO 确认）：
 * - 信封：`{"attached":null,"result":<业务数据>,"status":{...}}`
 * - 时间：`yyyy-MM-dd HH:mm:ss` 字符串（不是 ISO，不是毫秒数）
 * - 分页：`result.data` / `result.total`，服务端真分页
 *
 * 覆盖能力：
 * - 7 个接口全覆盖（listTodayVehicle / getStreamInfo / listRecentEvent /
 *   listEventByCondition / getEventDetail / markEventViewed / exportEvent）
 * - 算法 3 端点（rerunAlgorithm / algorithmTask / listAlgorithmTasks）全覆盖，
 *   含 QUEUED → RUNNING → SUCCEEDED 的轮询状态机
 * - 今日识别车辆卡片（车牌/车型·颜色/时间/大门 + 入库·出库 tag）
 * - 出入事件记录列：车牌号 / 车辆类型 / 行驶方向 / 识别地点 / 状态 / 操作
 * - 核实状态 PENDING 待核实 / VERIFIED 已核实（REJECTED 预留）
 * - 查看即标记为已核实（含幂等）
 * - 导出返回真实 xlsx 二进制（复用 `mock/doorAnomaly/xlsx.ts`，不复制实现）
 *
 * ⚠️ 时间戳：所有假数据的时间都由 `fixtures.ts` 在**每次请求时**基于
 * `Date.now()` 现算。左侧「今日识别」按今天过滤、点位按 90 分钟心跳阈值判离线，
 * 固化时间戳会让 dev server 跑久后今日列表为空、在线点位误判离线。
 */

import type { MockMethod } from 'vite-plugin-mock';

import { buildXlsx } from '../doorAnomaly/xlsx';
import {
  buildEventDetail,
  createStreamInfo,
  createTodayVehicleList,
  findEvent,
  markViewed,
  materializeAlgorithmTasks,
  materializeEvents,
  queryAlgorithmTask,
  rerunAlgorithmTask,
} from './fixtures';

/** 与后端 ExcelExportController 一致的 OOXML MIME */
const XLSX_MIME = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

/**
 * Mock 接口注册前缀。
 *
 * 前端请求先拼 `VITE_API_URL_PREFIX`（mock 模式为 `/api`），再拼
 * `src/api/vehicleRecognition.ts` 里的路径，因此 handler 必须带同一前缀，
 * 否则请求打不到 handler（表现为「Mock 模式无数据」）。
 */
const P = '/api/vehicleRecognition';

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
 * 按条件筛选并物化识别记录，默认识别时间倒序。
 *
 * 历史分页与导出共用同一实现：两者条件不同步的话，
 * 「导出的是筛过的、列表是另一套」这类问题在 Mock 阶段发现不了。
 */
function queryEvents(body: any, now: number): any[] {
  const { plateNo, vehicleType, direction, gateName, verifyStatus, warehouseId, startTime, endTime } = body ?? {};

  const startMs = toMs(startTime);
  const endMs = toMs(endTime);

  let list = materializeEvents(now);

  if (plateNo) list = list.filter((e) => String(e.plateNo).includes(String(plateNo)));
  if (vehicleType) list = list.filter((e) => e.vehicleType === vehicleType);
  if (direction) list = list.filter((e) => e.direction === direction);
  if (gateName) list = list.filter((e) => String(e.gateName).includes(String(gateName)));
  if (verifyStatus) {
    list = list.filter((e) =>
      verifyStatus === 'VERIFIED' ? e.verifyStatus !== 'PENDING' : e.verifyStatus === verifyStatus,
    );
  }
  if (warehouseId) list = list.filter((e) => e.warehouseId === warehouseId);
  if (startMs !== null) list = list.filter((e) => toMs(e.recognizedAt)! >= startMs);
  if (endMs !== null) list = list.filter((e) => toMs(e.recognizedAt)! <= endMs);

  return list;
}

/**
 * 从请求上下文取路径参数。
 *
 * vite-plugin-mock@3 在 dev 模式传给 handler 的是 `{ url, body, query, headers }`，
 * `url` 是原始请求 URL，**不含解析后的 params**，所以 `:eventId` 必须自己截取。
 */
function pathParam(opt: any): string {
  const path = String(opt?.url ?? '').split('?')[0];
  const segments = path.split('/').filter(Boolean);
  const last = segments[segments.length - 1] ?? '';
  if (!last || last.startsWith(':')) return '';
  return decodeURIComponent(last);
}

/* ==================== 导出 ==================== */

/**
 * 表头与真实后端导出模板逐列对齐（列 = 表格列，顺序不可变）。
 */
const EXPORT_HEADER = [
  '车牌号',
  '车辆类型',
  '识别地点',
  '识别时间',
  '仓库',
  '核实状态',
  '核实人',
  '核实时间',
  '核实意见',
];

const VERIFY_STATUS_NAME: Record<string, string> = {
  PENDING: '待核实',
  VERIFIED: '已核实',
  REJECTED: '已处理',
};

function exportFileName(now: number): string {
  const d = new Date(now);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `车辆识别记录_${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}.xlsx`;
}

/* ==================== 接口注册 ==================== */

export default [
  /* ============ 1. 今日识别车辆（左侧列表） ============ */
  {
    url: `${P}/listTodayVehicle`,
    method: 'post',
    response: () => {
      // 今日列表按今天过滤，时间每次现算
      return ok(createTodayVehicleList(Date.now()));
    },
  },

  /* ============ 2. 短时播放信息（按点位） ============ */
  {
    url: `${P}/getStreamInfo`,
    method: 'post',
    response: (opt: any) => {
      // 契约：pointId 走 JSON 请求体（与后端 @RequestBody 对齐）
      const pointId = opt?.body?.pointId;
      if (!pointId) return fail('识别点位标识不能为空');
      return ok(createStreamInfo(String(pointId)));
    },
  },

  /* ============ 3. 首页最近识别记录 ============ */
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

  /* ============ 5. 记录详情（查看即标记） ============ */
  /**
   * 与真实后端一致：命中 PENDING 记录时，**读接口本身**就会把它标记为已核实
   * （写 verify_status / processed_by / processed_at / verify_opinion 并 event_version+1）。
   *
   * 返回的是**更新前**的快照（verifyStatus 仍为 PENDING），与后端一致：
   * 前端据此再调用一次 markEventViewed 并刷新列表，页面才能立刻从「待核实」
   * 变成「已核实」。第二次进入同一详情就返回 VERIFIED，且不再重复计数。
   */
  {
    url: `${P}/getEventDetail/:eventId`,
    method: 'post',
    response: (opt: any) => {
      const eventId = pathParam(opt);
      if (!eventId) return fail('识别记录标识不能为空');

      const event = findEvent(eventId);
      if (!event) return fail('识别记录不存在或无权查看', '9103');

      const now = Date.now();
      const detail = buildEventDetail(event, now);

      // 先在快照上定格「更新前」状态，再改写内存态
      if (event.verifyStatus === 'PENDING') markViewed(event, now);

      return ok(detail);
    },
  },

  /* ============ 6. 查看后标记为已核实 ============ */
  {
    url: `${P}/markEventViewed/:eventId`,
    method: 'post',
    response: (opt: any) => {
      const eventId = pathParam(opt);
      if (!eventId) return fail('识别记录标识不能为空');

      const event = findEvent(eventId);
      if (!event) return fail('识别记录不存在或无权查看', '9103');

      // 幂等：详情接口通常已经把它写成 VERIFIED，这里不再重复追加审计与版本
      const changed = markViewed(event, Date.now());
      return ok(changed ? '已标记为已核实' : '该记录已是核实状态');
    },
  },

  /* ============ 7. 导出记录（真实 xlsx 二进制） ============ */
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
          e.plateNo,
          e.vehicleType,
          e.gateName,
          e.recognizedAt,
          e.warehouseName,
          VERIFY_STATUS_NAME[e.verifyStatus] ?? e.verifyStatus,
          e.processedBy ?? '',
          e.processedAt ?? '',
          e.verifyOpinion ?? '',
        ]);

        const xlsx = buildXlsx('车辆识别记录', [EXPORT_HEADER, ...data]);
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

  /* ============ 9. 今日无识别场景（「今日暂无识别记录」） ============ */
  {
    url: `${P}/listTodayVehicleEmpty`,
    method: 'post',
    response: () => ok([]),
  },

  /* ============ 10. 提交重新识别（受理即返回 taskId） ============ */
  /**
   * 只受理、不同步等结果：返回 QUEUED 任务号，前端按 800ms 轮询 algorithmTask。
   * 与后端一致：车牌只以掩码形式出现在任务行里。
   */
  {
    url: `${P}/rerunAlgorithm/:eventId`,
    method: 'post',
    response: (opt: any) => {
      const eventId = pathParam(opt);
      if (!eventId) return fail('识别记录标识不能为空');
      if (!findEvent(eventId)) return fail('识别记录不存在或无权查看', '9103');
      return ok(rerunAlgorithmTask(eventId, Date.now()));
    },
  },

  /* ============ 11. 轮询算法任务状态 ============ */
  {
    url: `${P}/algorithmTask/:taskId`,
    method: 'post',
    response: (opt: any) => {
      const taskId = pathParam(opt);
      if (!taskId) return fail('算法任务编号不能为空');
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
      const eventId = pathParam(opt);
      if (!eventId) return fail('识别记录标识不能为空');
      if (!findEvent(eventId)) return fail('识别记录不存在或无权查看', '9103');
      return ok(materializeAlgorithmTasks(eventId, Date.now()));
    },
  },
] as MockMethod[];
