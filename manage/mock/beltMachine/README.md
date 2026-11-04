# 皮带机下站人监测 - Mock 说明

`VITE_USE_MOCK=true`（`npm run dev:mock`）时驱动皮带机监测页与历史记录页，不需要后端。
**Mock 通过不等于后端联调通过**，接口形状以 `src/api/beltMachine.ts` 的 `normalize*` 与真实后端实测样例为准。

## 目录结构

```
mock/beltMachine/
├── index.ts      7 个接口定义 + 信封 + 筛选/分页/导出
├── fixtures.ts   假数据生成器（时间每次请求基于 Date.now() 现算）
└── README.md     本文件
```

xlsx 生成**复用** `mock/doorAnomaly/xlsx.ts`（`import { buildXlsx } from '../doorAnomaly/xlsx'`），
本目录不复制第二份实现。

## 接口前缀

请求先拼 `VITE_API_URL_PREFIX`（mock 模式 `/api`），再拼 `src/api/beltMachine.ts` 的路径，
因此 handler 注册路径必须是 `/api/beltMachine/...`。改 `.env.mock` 的前缀要同步改 `index.ts` 的 `P`。

## 接口覆盖

| 接口 | Mock 实现 | 验证要点 |
|---|---|---|
| `/api/beltMachine/listMonitorPoint` | 12 台皮带机 | 正常/报警/离线/未部署四态；至少两台 ONLINE + ACTIVE |
| `/api/beltMachine/getStreamInfo` | 按点位分支 | 可播 MP4、`CAMERA_NOT_BOUND`、`CAMERA_OFFLINE`、`POINT_NOT_FOUND`、`URL_EXPIRED`、`UNSUPPORTED_PROTOCOL` |
| `/api/beltMachine/listRecentEvent` | 时间倒序取前 N | 首页最近事件 |
| `/api/beltMachine/listEventByCondition` | 真分页（先 total 再切片） | 30 条 / 每页 10 条 = 3 页；筛选条件与导出共用 |
| `/api/beltMachine/getEventDetail/:eventId` | 含证据与处理记录 | 命中 PENDING 时**读接口就改写内存态**（与后端一致） |
| `/api/beltMachine/markEventViewed/:eventId` | 仅 PENDING 改写为 DONE，其余幂等 | 重复查看不重复计数、不自增版本 |
| `/api/beltMachine/exportEvent` | `rawResponse` 返回真实 xlsx | 13 列表头与后端模板一致，文件名 `皮带机闲人记录_YYYYMMDD.xlsx` |

另有 `listEventByConditionEmpty`、`listMonitorPointEmpty` 两个空态别名（与 doorAnomaly / videoOcclusion 一致，便于验证空态）。

## 时间戳约定（最容易踩的坑）

页面 `computeDisplayStatus` 用 **90 分钟心跳阈值**（`shared/constants.HEARTBEAT_TIMEOUT_MS`）判定离线。
`fixtures.ts` 只保存**相对偏移量**，所有时间字符串在每次请求时用 `Date.now()` 现算。
若在模块加载时固定一次，dev server 跑久后整页会被判成「离线」，
正常/报警两态永远演示不出来（仓门那一版踩过）。

## 展示态与后端字段的对应

| 页面文案 | 判定条件 |
|---|---|
| 正常 | `deviceStatus='ONLINE'` 且 `alarmStatus≠'ACTIVE'` 且心跳在 90 分钟内 |
| 报警 | `deviceStatus='ONLINE'` 且 `alarmStatus='ACTIVE'` 且心跳在 90 分钟内 |
| 离线 | `deviceStatus='OFFLINE'`，或 ONLINE 但心跳超过 90 分钟 |
| 未部署 | `deviceStatus='UNKNOWN'` 且 `cameraId` 为空（该皮带机没绑定摄像头） |

⚠️ 共享层 `DisplayStatus` 只有 `NORMAL/ABNORMAL/OFFLINE/UNKNOWN` 四档，
拆不出「未部署」。拆分逻辑放在本功能的
`src/pages/aiVision/beltMachine/constants/index.ts` 的 `pointDisplayStatus`，
**不要**为此去改 `shared/displayStatus.ts`。

## 处理状态三值

本功能按原型用三值：`PENDING` 未核实 / `PROCESSING` 处理中 / `DONE` 已处理。
与视频遮挡那版的五值（`PENDING/PROCESSING/VERIFIED_TRUE/VERIFIED_FALSE/CLOSED`）
**不是同一套编码**，两边的标签映射表不可互相复用。

## 已知未覆盖

- 未模拟多用户并发处理（乐观锁冲突）路径，需要真实后端验证。
- 未模拟数据范围过滤：Mock 一律返回全量点位与事件。
