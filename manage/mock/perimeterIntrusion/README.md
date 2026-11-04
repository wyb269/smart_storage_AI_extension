# 周界入侵识别 - Mock 说明

`VITE_USE_MOCK=true`（`npm run dev:mock`）时驱动周界监测页与历史记录页，不需要后端。
**Mock 通过不等于后端联调通过**，接口形状以 `src/api/perimeterIntrusion.ts` 的 `normalize*` 与后端约定为准。

## 目录结构

```
mock/perimeterIntrusion/
├── index.ts      7 个接口定义 + 信封 + 筛选/分页/导出
├── fixtures.ts   假数据生成器（时间每次请求基于 Date.now() 现算）
└── README.md     本文件
```

xlsx 生成**复用** `mock/doorAnomaly/xlsx.ts`（`import { buildXlsx } from '../doorAnomaly/xlsx'`），
本目录不复制第二份实现。

## 接口前缀

请求先拼 `VITE_API_URL_PREFIX`（mock 模式 `/api`），再拼 `src/api/perimeterIntrusion.ts` 的路径，
因此 handler 注册路径必须是 `/api/perimeterIntrusion/...`。改 `.env.mock` 的前缀要同步改 `index.ts` 的 `P`。

## 接口覆盖

| 接口 | Mock 实现 | 验证要点 |
|---|---|---|
| `/api/perimeterIntrusion/listMonitorPoint` | 12 个防区段（四面防区各 3 段） | 正常布防/入侵报警/未布防/离线/未知五态；至少一段 ONLINE + ACTIVE |
| `/api/perimeterIntrusion/getStreamInfo` | 按防区段分支 | 可播 MP4、`CAMERA_OFFLINE`、`NO_ADDRESS`、`POINT_DISARMED`、`URL_EXPIRED`、`UNSUPPORTED_PROTOCOL` |
| `/api/perimeterIntrusion/listRecentEvent` | 时间倒序取前 N | 首页最近事件 |
| `/api/perimeterIntrusion/listEventByCondition` | 真分页（先 total 再切片） | 30 条 / 每页 10 条 = 3 页；`zone` 参与筛选且影响 total |
| `/api/perimeterIntrusion/getEventDetail/:eventId` | 含证据与处理记录 | 命中 PENDING 时**读接口就改写内存态**（与后端一致） |
| `/api/perimeterIntrusion/markEventViewed/:eventId` | 仅 PENDING 改写，其余幂等 | PROCESSING 不被「看了一眼」顶掉；重复查看不重复计数 |
| `/api/perimeterIntrusion/exportEvent` | `rawResponse` 返回真实 xlsx | 14 列表头与历史页一致，文件名 `周界入侵记录_YYYYMMDD.xlsx` |

另有 `listEventByConditionEmpty`、`listMonitorPointEmpty` 两个空态别名（与 videoOcclusion 一致，便于验证空态）。

## 「未布防」是怎么造出来的（本功能与模板最大的差异）

后端返回的 `defenseStatus` 是**独立于设备态与告警态**的一维。共享层的
`computeDisplayStatus` 只认 device + alarm + 心跳三样，表达不出这一态，
因此本功能在 `src/pages/aiVision/perimeterIntrusion/composables/usePerimeterDisplayStatus.ts`
做了一层本地包装：先判 `DISARMED`，其余回落到共享计算。**共享文件未做任何修改。**

Mock 里第 5、10 段（`PD-005` / `PD-010`）刻意做成 `ONLINE` + `NORMAL` + `DISARMED`：
如果这条判定被改坏（比如把未布防排到离线之后、或干脆漏判），
这两段会立刻变成「正常布防」，一眼就能看出来。

## 时间戳约定（最容易踩的坑）

页面用 **90 分钟心跳阈值**（`shared/constants.HEARTBEAT_TIMEOUT_MS`）判定离线。

- 内存里只存**距今偏移量**（`occurredAgoMs` / `heartbeatAgoSec`），对外时间字符串在**每次请求**时
  由 `Date.now()` 现算，因此进程跑多久都不会出现「整页离线」或「历史为空」。
- 防区段分布中第 11 段特意为 `deviceStatus=ONLINE` + 心跳 126 分钟（超阈值），
  第 9 段为 `deviceStatus=OFFLINE` + 心跳 182 分钟（超阈值），
  两条判定链路都能验到；第 2 段 `OFFLINE` + 182 分钟。
- 时间格式统一为 `yyyy-MM-dd HH:mm:ss` 字符串（后端约定形状），不是 ISO、不是毫秒数。

## 响应信封

```json
{"attached":null,"result":<业务数据>,
 "status":{"code":"200","message":"请求成功","requestId":"<32位hex>","success":true,"timestamp":"yyyy-MM-dd HH:mm:ss"}}
```

失败时 `result` 为 `null`、`success:false`、`code` 为 `-1`（参数类）或 `9103`（数据不存在）。

## 与 videoOcclusion mock 的差异

1. **监测主体**由摄像头换成防区段：卡片主键 `pointId`、标题 `pointName`（东防区-东墙段01），
   摄像头降级为段上绑定的 `cameraCode` / `cameraName`。
2. **多一维 `defenseStatus`**，取流分支因此多一条 `POINT_DISARMED`。
3. **历史查询多 `zone` 参数**，与首页网格下拉同一个取值域（EAST/WEST/SOUTH/NORTH，空为全部）。
4. **处理状态三值**（PENDING 未核实 / PROCESSING 处理中 / DONE 已处理），
   没有遮挡那版的「PROCESSED 合并态」，因此 `queryEvents` 里没有合并分支。
5. 事件编号前缀 `PD`（遮挡是 `VD`）。

## 业务覆盖

- 入侵类型 5 种（攀爬 / 翻越 / 闯入 / 徘徊 / 其他）轮转分配，保证全覆盖。
- 算法状态 7 种（QUEUED/RUNNING/SUCCEEDED/FAILED/TIMEOUT/SKIPPED/MOCK）按序号覆盖；
  非 SUCCEEDED/MOCK 时 `algorithmResult` 为 `null`，不伪造为 NORMAL。
- 置信度约 15% 为 `null`（页面/导出显示「暂无」，不是 0%）；导出列写成 `87.30%` 形式。
- 处理状态初始 30 条中 6 条 PENDING、3 条 PROCESSING、21 条 DONE。
- 事件编号 `PD` + `yyyyMMdd` + 4 位序号，按「同一自然日内发生时间升序」现算，跨页不重复。

## 安全声明

图片与视频地址均为公开占位/测试资源（picsum、test-streams.mux.dev），
不含真实监控画面、摄像头账号、RTSP 凭据或内网地址；`isSimulated` 对 MOCK 算法状态显式为 `true`。
