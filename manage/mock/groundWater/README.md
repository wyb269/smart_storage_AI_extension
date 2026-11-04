# 地面积水监测 - Mock 说明

`VITE_USE_MOCK=true`（`npm run dev:mock`）时驱动积水监测页与历史记录页，不需要后端。
**Mock 通过不等于后端联调通过**，接口形状以 `src/api/groundWater.ts` 的 `normalize*` 与后端约定为准。

## 目录结构

```
mock/groundWater/
├── index.ts      7 个接口定义 + 信封 + 筛选/分页/导出
├── fixtures.ts   假数据生成器（时间每次请求基于 Date.now() 现算）
└── README.md     本文件
```

xlsx 生成**复用** `mock/doorAnomaly/xlsx.ts`（`import { buildXlsx } from '../doorAnomaly/xlsx'`），
本目录不复制第二份实现。

## 接口前缀

请求先拼 `VITE_API_URL_PREFIX`（mock 模式 `/api`），再拼 `src/api/groundWater.ts` 的路径，
因此 handler 注册路径必须是 `/api/groundWater/...`。改 `.env.mock` 的前缀要同步改 `index.ts` 的 `P`。

## 接口覆盖

| 接口 | Mock 实现 | 验证要点 |
|---|---|---|
| `/api/groundWater/listMonitorPoint` | 12 个监测点位 | 干燥/积水/离线/未知四态；至少一台 ONLINE + ACTIVE |
| `/api/groundWater/getStreamInfo` | 按点位分支 | 可播 MP4、`POINT_NOT_FOUND`、`CAMERA_OFFLINE`、`NO_ADDRESS`、`URL_EXPIRED`、`UNSUPPORTED_PROTOCOL` |
| `/api/groundWater/listRecentEvent` | 时间倒序取前 N | 首页最近事件 |
| `/api/groundWater/listEventByCondition` | 真分页（先 total 再切片） | 30 条 / 每页 10 条 = 3 页；筛选条件与导出共用；参数名 `pageNo` |
| `/api/groundWater/getEventDetail/:eventId` | 含证据与处理记录 | 命中 PENDING 时**读接口就改写内存态**（与后端一致） |
| `/api/groundWater/markEventViewed/:eventId` | 仅 PENDING 改写，其余幂等 | 重复查看不重复计数、不自增版本；PROCESSING 不被覆盖 |
| `/api/groundWater/exportEvent` | `rawResponse` 返回真实 xlsx | 12 列表头与后端模板一致，文件名 `地面积水记录_YYYYMMDD.xlsx` |

另有 `listEventByConditionEmpty`、`listMonitorPointEmpty` 两个空态别名（与 doorAnomaly / videoOcclusion 一致，便于验证空态）。

## 时间戳约定（最容易踩的坑）

页面 `computeDisplayStatus` 用 **90 分钟心跳阈值**（`shared/constants.HEARTBEAT_TIMEOUT_MS`）判定离线。

- 内存里只存**距今偏移量**（`occurredAgoMs` / `heartbeatAgoSec`），对外时间字符串在**每次请求**时
  由 `Date.now()` 现算，因此进程跑多久都不会出现「整页离线」或「历史为空」。
- `doorAnomaly/fixtures.ts` 的 `BASE_TIME` 是模块加载时算一次的（心跳部分已修正，事件部分仍固化），
  这里连事件时间也一并改为每次现算 —— 与 videoOcclusion 同口径。
- 时间格式统一为 `yyyy-MM-dd HH:mm:ss` 字符串（后端约定形状），不是 ISO、不是毫秒数。
- 点位分布中 `CAM-008`（北通廊西侧）特意为 `deviceStatus=ONLINE` + 心跳 126 分钟（超阈值），
  `CAM-009`（北通廊南段）为 `deviceStatus=OFFLINE` + 心跳 46 分钟（阈值内），
  两条判定链路都能验到。

## 响应信封

```json
{"attached":null,"result":<业务数据>,
 "status":{"code":"200","message":"请求成功","requestId":"<32位hex>","success":true,"timestamp":"yyyy-MM-dd HH:mm:ss"}}
```

失败时 `result` 为 `null`、`success:false`、`code` 为 `-1`（参数类）或 `9103`（数据不存在）。

## 与 videoOcclusion mock 的差异

1. **主体从摄像头换成监测点位**：状态接口叫 `listMonitorPoint`，卡片标题是点位名
   （主通廊东侧 / 北通廊南段 …），摄像头编号退为次要信息；取流入参是 `pointId`。
2. **告警字段叫 `alarmStatus`，取值仍是 `NORMAL/ACTIVE/RECOVERED`**。
   规范文档 §3 写的是 `NORMAL/ALARM`，但共享层 `displayStatus.ts` 的 `isActiveAlarm`
   硬编码判 `'ACTIVE'` 且不允许修改 —— 存 `ALARM` 会让左侧网格永远出不来「积水」态。
   本目录与后端 DTO、建表脚本三处统一采用 `ACTIVE`，差异已在交付报告中标出。
3. **处理状态三态**（PENDING 未核实 / PROCESSING 处理中 / DONE 已处理），
   不再有遮挡那版的页面组合概念 `PROCESSED`，筛选值可直接下推。
   自动标记只推进 `PENDING → DONE`，`PROCESSING` 不覆盖。
4. 展示态文案：`NORMAL` 叫**干燥**（不叫「正常」），`ABNORMAL` 叫**积水**。
5. 自动标记写审计记录时 `source` 为 `SYSTEM`（遮挡那版是 `WEB`），与规范 §2 一致。

## 业务覆盖

- 积水结论类型 4 种（积水 / 渗漏 / 结冰 / 其他）轮转分配，保证全覆盖。
- 算法状态 7 种（QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK）按序号覆盖；
  非 SUCCEEDED/MOCK 时 `algorithmResult` 为 `null`，不伪造为 NORMAL。
- 置信度约 15% 为 `null`（页面/导出显示「暂无」，不是 0%）；导出列写成 `87.30%` 形式。
- 处理状态三态：30 条中 6 条 PENDING、12 条 PROCESSING、12 条 DONE。
- 事件编号 `WP` + `yyyyMMdd` + 4 位序号，按「同一自然日内发生时间升序」现算，跨页不重复。

## 安全声明

图片与视频地址均为公开占位/测试资源（picsum、test-streams.mux.dev），
不含真实监控画面、摄像头账号、RTSP 凭据或内网地址；
`isSimulated` 对 MOCK 算法状态显式为 `true`，其余行为 `false`，与后端列口径一致。
