# 车辆智能识别 - Mock 说明

`VITE_USE_MOCK=true`（`npm run dev:mock`）时驱动车辆识别监测页与历史记录页，不需要后端。
**Mock 通过不等于后端联调通过**，接口形状以 `src/api/vehicleRecognition.ts` 的 `normalize*` 与后端 DTO 为准。

## 目录结构

```
mock/vehicleRecognition/
├── index.ts      7 个接口定义 + 信封 + 筛选/分页/导出
├── fixtures.ts   假数据生成器（时间每次请求基于 Date.now() 现算）
└── README.md     本文件
```

xlsx 生成**复用** `mock/doorAnomaly/xlsx.ts`（`import { buildXlsx } from '../doorAnomaly/xlsx'`），
本目录不复制第二份实现。

## 接口前缀

请求先拼 `VITE_API_URL_PREFIX`（mock 模式 `/api`），再拼 `src/api/vehicleRecognition.ts` 的路径，
因此 handler 注册路径必须是 `/api/vehicleRecognition/...`。改 `.env.mock` 的前缀要同步改 `index.ts` 的 `P`。

## 接口覆盖

| 接口 | Mock 实现 | 验证要点 |
|---|---|---|
| `/api/vehicleRecognition/listTodayVehicle` | 今日识别车辆卡片 | 车牌/车型·颜色/时间/大门 + 入库·出库 tag；只返回今天的记录 |
| `/api/vehicleRecognition/getStreamInfo` | 按点位分支 | 可播 MP4、`CAMERA_OFFLINE`、`NO_ADDRESS`、`POINT_NOT_FOUND` |
| `/api/vehicleRecognition/listRecentEvent` | 时间倒序取前 N | 首页最近识别记录 |
| `/api/vehicleRecognition/listEventByCondition` | 真分页（先 total 再切片） | 30 条 / 每页 10 条 = 3 页；筛选条件与导出共用 |
| `/api/vehicleRecognition/getEventDetail/:eventId` | 含证据与核实记录 | 命中 PENDING 时**读接口就改写内存态**（与后端一致） |
| `/api/vehicleRecognition/markEventViewed/:eventId` | 仅 PENDING 改写，其余幂等 | 重复查看不重复计数、不自增版本 |
| `/api/vehicleRecognition/exportEvent` | `rawResponse` 返回真实 xlsx | 表头与页面列一致，文件名 `车辆识别记录_YYYYMMDD.xlsx` |

另有 `listEventByConditionEmpty`、`listTodayVehicleEmpty` 两个空态别名（与遮挡一致，便于验证空态）。

## 与遮挡 mock 的结构性差异

1. **左侧不是设备状态网格**：遮挡的 `listCameraStatus` 在这里换成 `listTodayVehicle`，
   返回的是「今天被识别到的车」，字段是车牌/车型/颜色/方向/点位/识别时间/核实状态。
   点位仍保留在 `fixtures.ts` 的 `POINT_DEFS` 里，用于取流在线判定与「记录→摄像头」映射。
2. **状态字段是 `verify_status`**（待核实/已核实/误报），不是遮挡的 `process_status`（未处理/已处理）。
3. **取流入参是 `pointId`**（选中一条识别记录后播放它所属点位的摄像头），不是 `cameraId`。

## 时间戳约定（最容易踩的坑）

- 内存里只存**相对偏移**（`daysAgo` / `timeOfDay` / `heartbeatAgoSec`），对外时间字符串在**每次请求**时
  由 `Date.now()` 现算。
- 今日记录用 `startOfToday + timeOfDay*(now-startOfToday)` 保证永远落在 `[今天0点, now]`，
  否则进程跨天后左侧「今日识别车辆」会空。
- 点位在线判定用 **90 分钟心跳阈值**（`shared/constants.HEARTBEAT_TIMEOUT_MS`），
  心跳必须相对 `now` 生成。`504` 点位 `deviceStatus=OFFLINE`、`506` 为 `UNKNOWN`，
  分别演示 `CAMERA_OFFLINE` 与 `NO_ADDRESS` 取流分支。
- 时间格式统一为 `yyyy-MM-dd HH:mm:ss` 字符串（后端形状），不是 ISO、不是毫秒数。

## 响应信封

```json
{"attached":null,"result":<业务数据>,
 "status":{"code":"200","message":"请求成功","requestId":"<32位hex>","success":true,"timestamp":"yyyy-MM-dd HH:mm:ss"}}
```

失败时 `result` 为 `null`、`success:false`、`code` 为 `-1`（参数类）或 `9103`（数据不存在）。

## 业务覆盖

- 行驶方向：入口点位记 `IN`（入库）、出口点位记 `OUT`（出库），与点位语义一致。
- 车辆类型 5 种（柴油货车 / 空车 / 客车车型 / 新能源轿车 / 未识别车型）轮转分配。
- 置信度约 1/7 为 `null`（页面显示「暂无」，不是 0%）。
- 核实状态每 3 条留 1 条 `PENDING`，其余 `VERIFIED`；`REJECTED`（误报）为预留值，Mock 不主动产出。
- 事件编号 `VC` + `yyyyMMdd` + 4 位序号，按识别时间现算。

## 安全声明

图片与视频地址均为公开占位/测试资源（picsum、test-streams.mux.dev），
不含真实监控画面、摄像头账号、RTSP 凭据或内网地址；`isSimulated` 对每条记录显式为 `true`。
车牌为演示占位（黑A00000 等），非真实车牌；真实后端日志严禁打印完整车牌（PII），详见 Service 掩码说明。
