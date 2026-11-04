# 粮面虫害智能识别 - Mock 说明

`VITE_USE_MOCK=true`（`npm run dev:mock`）时驱动虫害监测页与历史记录页，不需要后端。
**Mock 通过不等于后端联调通过**，接口形状以 `src/api/pestDetection.ts` 的 `normalize*` 与后端约定为准。

## 目录结构

```
mock/pestDetection/
├── index.ts      7 个接口定义 + 信封 + 筛选/分页/导出
├── fixtures.ts   假数据生成器（时间每次请求基于 Date.now() 现算）
└── README.md     本文件
```

xlsx 生成**复用** `mock/doorAnomaly/xlsx.ts`（`import { buildXlsx } from '../doorAnomaly/xlsx'`），
本目录不复制第二份实现。

## 接口前缀

请求先拼 `VITE_API_URL_PREFIX`（mock 模式 `/api`），再拼 `src/api/pestDetection.ts` 的路径，
因此 handler 注册路径必须是 `/api/pestDetection/...`。改 `.env.mock` 的前缀要同步改 `index.ts` 的 `P`。

## 接口覆盖

| 接口 | Mock 实现 | 验证要点 |
|---|---|---|
| `/api/pestDetection/listMonitorPoint` | 9 个粮面点位 | 正常/告警/离线/未知四态；至少一个 ONLINE + ACTIVE；卡片标题「仓库 · 粮种吨位吨」 |
| `/api/pestDetection/getStreamInfo` | 按点位分支 | 可播 MP4、`CAMERA_OFFLINE`、`NO_ADDRESS`、`URL_EXPIRED`、`UNSUPPORTED_PROTOCOL`、`POINT_NOT_FOUND` |
| `/api/pestDetection/listRecentEvent` | 时间倒序取前 N | 首页最近记录 |
| `/api/pestDetection/listEventByCondition` | 真分页（先 total 再切片） | 30 条 / 每页 10 条 = 3 页；筛选条件与导出共用；参数 `pageNo` |
| `/api/pestDetection/getEventDetail/:eventId` | 含证据与处理记录 | 命中 PENDING 时**读接口就改写内存态**（与后端一致）；派生 `densityExceeded` |
| `/api/pestDetection/markEventViewed/:eventId` | 仅 PENDING 改写为 DONE，其余幂等 | 重复查看不重复计数、不自增版本 |
| `/api/pestDetection/exportEvent` | `rawResponse` 返回真实 xlsx | 12 列表头，文件名 `虫害检测记录_YYYYMMDD.xlsx` |

另有 `listEventByConditionEmpty`、`listMonitorPointEmpty` 两个空态别名（便于验证空态）。

## 与遮挡那版的差异

1. **监测对象是粮面点位（仓房+粮种+吨位），不是摄像头**：`listMonitorPoint` 返回带
   `grainType`/`grainTonnage`/`cameraCode` 的点位，卡片标题由前端拼「仓库 · 粮种吨位吨」。
2. **记录是采样检测结果**：额外带 `samplePosition`（采样位置）、`insectSpecies`（虫种）、
   `density`（密度，头/kg）、`densityThreshold`（判定阈值）。`density > densityThreshold` 即密度超标。
3. **处理状态三态**：未核实 / 处理中 / 已处理（PENDING/PROCESSING/DONE），
   遮挡那版是两态（PENDING/CLOSED）。查看即标记只把 PENDING 推进到 DONE。

## 时间戳约定（最容易踩的坑）

页面 `computeDisplayStatus` 用 **90 分钟心跳阈值**（`shared/constants.HEARTBEAT_TIMEOUT_MS`）判定离线。

- 内存里只存**距今偏移量**（`occurredAgoMs` / `heartbeatAgoSec`），对外时间字符串在**每次请求**时
  由 `Date.now()` 现算，因此进程跑多久都不会出现「整页离线」或「历史为空」。
- 时间格式统一为 `yyyy-MM-dd HH:mm:ss` 字符串，不是 ISO、不是毫秒数。
- 点位分布中 `CAM-008` 特意为 `deviceStatus=ONLINE` + 心跳 126 分钟（超阈值），
  `CAM-005` 为 `deviceStatus=OFFLINE` + 心跳 182 分钟，两条判定链路都能验到。

## 响应信封

```json
{"attached":null,"result":<业务数据>,
 "status":{"code":"200","message":"请求成功","requestId":"<32位hex>","success":true,"timestamp":"yyyy-MM-dd HH:mm:ss"}}
```

失败时 `result` 为 `null`、`success:false`、`code` 为 `-1`（参数类）或 `9103`（数据不存在）。

## 业务覆盖

- 检测结论 4 种（粮面害虫 / 密度超标 / 霉变 / 其他或未知）轮转分配，保证全覆盖。
- 算法状态 7 种（QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK）按序号覆盖；
  非 SUCCEEDED/MOCK 时 `algorithmResult` 为 `null`，不伪造为 NORMAL。
- 置信度约 15% 为 `null`（页面/导出显示「暂无」，不是 0%）；导出列写成 `87.30%` 形式。
- 密度阈值默认 5.00 头/kg（与后端 `PestDetectionConstant.DEFAULT_DENSITY_THRESHOLD` 一致，此处仅 Mock 副本）。
- 记录编号 `PT` + `yyyyMMdd` + 4 位序号，按「同一自然日内采样时间升序」现算，跨页不重复。

## 安全声明

图片与视频地址均为公开占位/测试资源（picsum、test-streams.mux.dev），
不含真实监控画面、摄像头账号、RTSP 凭据或内网地址；`isSimulated` 对 MOCK 算法状态显式为 `true`。
