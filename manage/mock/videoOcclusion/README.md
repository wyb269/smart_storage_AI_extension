# 视频遮挡检测 - Mock 说明

`VITE_USE_MOCK=true`（`npm run dev:mock`）时驱动遮挡监测页与历史记录页，不需要后端。
**Mock 通过不等于后端联调通过**，接口形状以 `src/api/videoOcclusion.ts` 的 `normalize*` 与真实后端实测样例为准。

## 目录结构

```
mock/videoOcclusion/
├── index.ts      10 个接口定义 + 信封 + 筛选/分页/导出
├── fixtures.ts   假数据生成器（时间每次请求基于 Date.now() 现算）
└── README.md     本文件
```

xlsx 生成**复用** `mock/doorAnomaly/xlsx.ts`（`import { buildXlsx } from '../doorAnomaly/xlsx'`），
本目录不复制第二份实现。

## 接口前缀

请求先拼 `VITE_API_URL_PREFIX`（mock 模式 `/api`），再拼 `src/api/videoOcclusion.ts` 的路径，
因此 handler 注册路径必须是 `/api/videoOcclusion/...`。改 `.env.mock` 的前缀要同步改 `index.ts` 的 `P`。

## 接口覆盖

| 接口 | Mock 实现 | 验证要点 |
|---|---|---|
| `/api/videoOcclusion/listCameraStatus` | 12 台摄像头 | 正常/遮挡/离线/未知四态；至少一台 ONLINE + ACTIVE |
| `/api/videoOcclusion/getStreamInfo` | 按摄像头分支 | 可播 MP4、`CAMERA_OFFLINE`、`NO_ADDRESS`、`URL_EXPIRED`、`UNSUPPORTED_PROTOCOL` |
| `/api/videoOcclusion/listRecentEvent` | 时间倒序取前 N | 首页最近事件 |
| `/api/videoOcclusion/listEventByCondition` | 真分页（先 total 再切片） | 30 条 / 每页 10 条 = 3 页；筛选条件与导出共用 |
| `/api/videoOcclusion/getEventDetail/:eventId` | 含证据与处理记录 | 命中 PENDING 时**读接口就改写内存态**（与后端一致） |
| `/api/videoOcclusion/markEventViewed/:eventId` | 仅 PENDING 改写，其余幂等 | 重复查看不重复计数、不自增版本 |
| `/api/videoOcclusion/exportEvent` | `rawResponse` 返回真实 xlsx | 12 列表头与后端模板一致，文件名 `视频遮挡记录_YYYYMMDD.xlsx` |
| `/api/videoOcclusion/rerunAlgorithm/:eventId` | 立即返回 `QUEUED` + 任务号 | 不同步等推理；任务号即轮询参数 |
| `/api/videoOcclusion/algorithmTask/:taskNo` | 第 1 次 `RUNNING`、第 3 次起 `SUCCEEDED` | 成功时回写事件算法列，不动处理状态 |
| `/api/videoOcclusion/listAlgorithmTasks/:eventId` | `started_at` 倒序、最多 20 条 | 与详情内 `algorithmTasks` 同源 |

另有 `listEventByConditionEmpty`、`listCameraStatusEmpty` 两个空态别名（与 doorAnomaly 一致，便于验证空态）。

## 时间戳约定（最容易踩的坑）

页面 `computeDisplayStatus` 用 **90 分钟心跳阈值**（`shared/constants.HEARTBEAT_TIMEOUT_MS`）判定离线。

- 内存里只存**距今偏移量**（`occurredAgoMs` / `heartbeatAgoSec`），对外时间字符串在**每次请求**时
  由 `Date.now()` 现算，因此进程跑多久都不会出现「整页离线」或「历史为空」。
- `doorAnomaly/fixtures.ts` 的 `BASE_TIME` 是模块加载时算一次的（心跳部分已修正，事件部分仍固化），
  这里连事件时间也一并改为每次现算 —— 这是与 doorAnomaly 的唯一实现偏离。
- 时间格式统一为 `yyyy-MM-dd HH:mm:ss` 字符串（后端实测形状），不是 ISO、不是毫秒数。
- 摄像头分布中 `CAM-008` 特意为 `deviceStatus=ONLINE` + 心跳 126 分钟（超阈值），
  `CAM-010` 为 `deviceStatus=OFFLINE` + 心跳 46 分钟（阈值内），两条判定链路都能验到。

## 响应信封

```json
{"attached":null,"result":<业务数据>,
 "status":{"code":"200","message":"请求成功","requestId":"<32位hex>","success":true,"timestamp":"yyyy-MM-dd HH:mm:ss"}}
```

失败时 `result` 为 `null`、`success:false`、`code` 为 `-1`（参数类）或 `9103`（数据不存在）。

## 与 doorAnomaly mock 的差异

1. **只输出驼峰字段**，不再像 `doorAnomaly/index.ts` 的 `toSnake` 那样同时输出下划线别名。
   真实后端实测回来的是驼峰，「与后端同形」优先于双写兼容；前端 `pick()` 两种都能读。
2. 信封带 `attached`、时间用后端格式而非 `toISOString()`、`requestId` 为 32 位 hex。
3. `getEventDetail` 返回**更新前**快照（`processStatus` 仍为 PENDING），同时把内存态写成 CLOSED。
   这样前端「详情返回 PENDING → 调 markEventViewed → 本地刷新为已处理」的既有链路才成立。

## 业务覆盖

- 遮挡类型 5 种（全幅遮挡 / 部分遮挡 / 镜头污损 / 镜头被遮盖 / 其他或未知）轮转分配，保证全覆盖。
- 算法状态 7 种（QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK）按序号覆盖；
  非 SUCCEEDED/MOCK 时 `algorithmResult` 为 `null`，不伪造为 NORMAL。
- 置信度约 15% 为 `null`（页面/导出显示「暂无」，不是 0%）；导出列写成 `87.30%` 形式。
- 处理状态只有 PENDING / CLOSED 两值，页面为「未处理」/「已处理」；初始 30 条中 6 条 PENDING。
- 事件编号 `VD` + `yyyyMMdd` + 4 位序号，按「同一自然日内发生时间升序」现算，跨页不重复。

## 安全声明

图片与视频地址均为公开占位/测试资源（picsum、test-streams.mux.dev），
不含真实监控画面、摄像头账号、RTSP 凭据或内网地址；`isSimulated` 对 MOCK 算法状态显式为 `true`。
