# 人员行为监测 - Mock 说明

`VITE_USE_MOCK=true`（`npm run dev:mock`）时驱动人员行为监测页与历史记录页，不需要后端。
**Mock 通过不等于后端联调通过**，接口形状以 `src/api/personBehavior.ts` 的 `normalize*` 与真实后端实测样例为准。

## 目录结构

```
mock/personBehavior/
├── index.ts      7 个业务接口 + 算法 3 端点 + 信封 + 筛选/分页/导出
├── fixtures.ts   假数据生成器（时间每次请求基于 Date.now() 现算）
└── README.md     本文件
```

xlsx 生成**复用** `mock/doorAnomaly/xlsx.ts`（`import { buildXlsx } from '../doorAnomaly/xlsx'`），
本目录不复制第二份实现。

## 接口前缀

请求先拼 `VITE_API_URL_PREFIX`（mock 模式 `/api`），再拼 `src/api/personBehavior.ts` 的路径，
因此 handler 注册路径必须是 `/api/personBehavior/...`。改 `.env.mock` 的前缀要同步改 `index.ts` 的 `P`。

## 接口覆盖

| 接口 | Mock 实现 | 验证要点 |
|---|---|---|
| `/api/personBehavior/listMonitorPoint` | 12 个监测区域 | 正常/人员聚集/未戴安全帽/离线/未知五种卡片文案 |
| `/api/personBehavior/getStreamInfo` | 按区域分支 | 可播 MP4、`CAMERA_OFFLINE`、`NO_ADDRESS`、`URL_EXPIRED`、`UNSUPPORTED_PROTOCOL`、`POINT_NOT_FOUND` |
| `/api/personBehavior/listRecentEvent` | 时间倒序取前 N | 首页最近事件 |
| `/api/personBehavior/listEventByCondition` | 真分页（先 total 再切片） | 200 条 / 每页 10 条 = 20 页；筛选条件与导出共用 |
| `/api/personBehavior/getEventDetail/:eventId` | 含证据与处理记录 | 命中 PENDING 时**读接口就改写内存态**（与后端一致） |
| `/api/personBehavior/markEventViewed/:eventId` | 仅 PENDING 改写，其余幂等 | 重复查看不重复计数、不自增版本；**PROCESSING 不被查看终结** |
| `/api/personBehavior/exportEvent` | `rawResponse` 返回真实 xlsx | 12 列表头与后端模板 `personBehaviorEvent` 一致，文件名 `人员行为记录_YYYYMMDD.xlsx` |
| `/api/personBehavior/rerunAlgorithm/:eventId` | 内存任务表新增 QUEUED 行 | 立即返回 `{taskId, executionStatus:'QUEUED', simulated:true}`，不同步出结果 |
| `/api/personBehavior/algorithmTask/:taskId` | 按轮询次数推进状态 | 第 1 次 RUNNING、第 3 次起 SUCCEEDED 并回写事件算法列；首次查询不会直接成功 |
| `/api/personBehavior/listAlgorithmTasks/:eventId` | 按开始时间倒序，最多 20 条 | 详情接口也已随 `algorithmTasks`/`latestAlgorithmTask` 一次带回 |

另有 `listEventByConditionEmpty`、`listMonitorPointEmpty` 两个空态别名（与 videoOcclusion 一致，便于验证空态）。

## 时间戳约定（最容易踩的坑）

页面 `computeDisplayStatus` 用 **90 分钟心跳阈值**（`shared/constants.HEARTBEAT_TIMEOUT_MS`）判定离线。

- 内存里只存**距今偏移量**（`occurredAgoMs` / `heartbeatAgoSec`），对外时间字符串在**每次请求**时
  由 `Date.now()` 现算，因此进程跑多久都不会出现「整页离线」或「历史为空」。
- `doorAnomaly/fixtures.ts` 的 `BASE_TIME` 是模块加载时算一次的（心跳部分已修正，事件部分仍固化），
  这里连事件时间也一并改为每次现算 —— 与 `videoOcclusion/fixtures.ts` 同口径。
- 时间格式统一为 `yyyy-MM-dd HH:mm:ss` 字符串（后端实测形状），不是 ISO、不是毫秒数。
- 区域分布中 `AREA-008` 特意为 `deviceStatus=ONLINE` + 心跳 126 分钟（超阈值），
  `AREA-002 / AREA-009` 为 `deviceStatus=OFFLINE`，两条判定链路都能验到。

## 响应信封

```json
{"attached":null,"result":<业务数据>,
 "status":{"code":"200","message":"请求成功","requestId":"<32位hex>","success":true,"timestamp":"yyyy-MM-dd HH:mm:ss"}}
```

失败时 `result` 为 `null`、`success:false`、`code` 为 `-1`（参数类）或 `9103`（数据不存在）。

## 与 videoOcclusion mock 的差异

1. 监测对象是**区域**（`pointId` / `pointName`）而不是摄像头；`cameraCode` 降级为区域绑定的属性。
   `getStreamInfo` 入参因此是 `{ pointId }`。
2. 区域对象多一个 `alarmBehaviorType`：共享基座的综合展示态只有一个 `ABNORMAL`，
   而原型要求异常卡片按行为类型分别显示「人员聚集」（橙）与「未戴安全帽」（红），
   这一列就是文案与配色的来源。
3. 处理状态**三值三态**（`PENDING` 未核实 / `PROCESSING` 处理中 / `DONE` 已处理），
   不像遮挡那版把五值折叠成两态，因此筛选下拉里三个值都直接下推给后端。
4. `markViewed` 只改写 `PENDING`；`PROCESSING` 幂等跳过 —— 处理中的事件不能被一次「查看」抢着终结。
5. 事件量 200 条（对齐原型分页「共 200 条」），遮挡那版是 30 条。
6. 事件多一个 `personCount`：只有 `CROWD` 类有值，其余为 `null` 而不是 0
   （0 会被读成「画面里没人」，与「该指标不适用」是两回事）。

## 业务覆盖

- 行为类型 5 种（人员聚集 / 未戴安全帽 / 倒地 / 区域闯入 / 其他）轮转分配，各 40 条，保证全覆盖。
- 算法状态 7 种（QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK）按序号覆盖；
  非 SUCCEEDED/MOCK 时 `algorithmResult` 为 `null`，不伪造为 NORMAL。
- 置信度约 15% 为 `null`（页面/导出显示「暂无」，不是 0%）；导出列写成 `87.30%` 形式。
- 处理状态分布：每 4 条一轮 → PENDING 50 / PROCESSING 50 / DONE 100。
- 事件编号 `PR` + `yyyyMMdd` + 4 位序号，按「同一自然日内发生时间升序」现算，跨页不重复。

## 安全声明

图片与视频地址均为公开占位/测试资源（picsum、test-streams.mux.dev），
不含真实监控画面、摄像头账号、RTSP 凭据或内网地址；`isSimulated` 对 MOCK 算法状态显式为 `true`。
