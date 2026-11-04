/**
 * 仓门异动功能 - Mock 说明
 *
 * 本目录为前端独立开发提供模拟数据，**不代表后端已联调通过**。
 */

## 目录结构

```
mock/doorAnomaly/
├── index.ts      Mock 接口定义
└── fixtures.ts   假数据生成器（时间按当前时刻动态生成）
```

## 启用方式

Mock 由环境变量 `VITE_USE_MOCK` 控制，**不是硬编码常开**：

| 命令 | mode | VITE_USE_MOCK | 行为 |
|---|---|---|---|
| `npm run dev:mock` | `mock`（读 `.env.mock`） | `true` | 命中本目录 Mock |
| `npm run dev` | `development` | `false` | 走真实后端 `/smart-storage-admin` |

切换后需重启 dev server。开关实现位于 `vite.config.ts` 的 `viteMockServe.enable`。

## 接口前缀

请求会先拼接 `VITE_API_URL_PREFIX`，再拼接 `src/api/doorAnomaly.ts` 中的路径。

- mock 模式：前缀为 `/api`，因此 handler 注册路径必须为 **`/api/doorAnomaly/...`**
- 项目既有 `mock/index.ts` 同样使用 `/api` 前缀，两套 Mock 规则一致

> 若修改 `.env.mock` 的 `VITE_API_URL_PREFIX`，必须同步修改本目录 `index.ts`
> 中的 `P` 常量，否则请求无法命中 handler（表现为「Mock 模式无数据」）。

## 接口覆盖

| 接口 | Mock 实现 | 验证要点 |
|---|---|---|
| `/api/doorAnomaly/listDoorStatus` | 16 个仓门 | 正常/异常/离线/未知/选中五态 |
| `/api/doorAnomaly/getStreamInfo` | 离线仓门 `canPlay:false` | 离线分支、无地址分支、协议分支 |
| `/api/doorAnomaly/listRecentEvent` | 按时间倒序取前 N 条 | 首页最近事件 |
| `/api/doorAnomaly/listEventByCondition` | **真分页**（先算 total 再切片） | 避免前端伪分页掩盖问题 |
| `/api/doorAnomaly/getEventDetail/:eventId` | 含证据与处理记录 | 参考图/证据图/采集时间/版本快照 |
| `/api/doorAnomaly/markEventViewed/:eventId` | 仅 `PENDING` 写成 `CLOSED`，其余幂等 | 与后端 `markEventViewed` 同一语义：查看即标记为已处理 |

> 2026-10-21 产品变更：已取消人工处理入口（原 `/processEvent/:eventId`
> 与 `/processEventFail/:eventId` 两个 Mock 已随之移除）。
> 处理状态只保留「未处理 / 已处理」两种，打开详情即自动标记。
> 后端 `processEvent` 接口仍保留但前端不再调用。
| `/api/doorAnomaly/listEventByConditionEmpty` | 空列表 | 验证分页空态 |
| `/api/doorAnomaly/listDoorStatusEmpty` | 空仓门 | 验证「暂无可监控仓门」 |

导出接口 `/api/doorAnomaly/exportEvent` 已提供 Mock，
用 `rawResponse` 直接返回真实 xlsx 二进制（由 `./xlsx.ts` 零依赖生成，
表头与后端 `ExcelExportConfig` 的 `doorAnomalyEvent` 模板逐列对齐）。
> 注意：`response` 走的是 `JSON.stringify`，二进制必须用 `rawResponse`。
> 真实后端 POI 产出的那份 xlsx 仍未验证（需凭据启动后端）。

## 数据可复现性

`fixtures.ts` 使用固定种子的确定性伪随机，保证同一模块实例内多次调用结果一致。

事件时间以**模块加载时刻**为基准动态生成（不再使用固定日期），
使历史页默认「最近 7 天」查询在任何时候运行都有数据；
若使用固定日期，页面运行数天后所有事件都会落到查询窗口之外，
表现为「历史为空」，且难以与真实缺陷区分。

## 状态覆盖

- 算法任务状态：7 种全覆盖（QUEUED / RUNNING / SUCCEEDED / FAILED / TIMEOUT / SKIPPED / MOCK）
- 算法结论：仅在 SUCCEEDED 与 MOCK 时出现，其余为 `null`（**不伪造为 NORMAL**）
- 置信度：约 15% 概率为 `null`，用于验证「暂无」而非 0% 的展示
- 人工处理状态：5 种全覆盖，并含多条追加式处理记录

## 视频协议

一期前端使用**原生 `<video>`**，仅支持浏览器原生可播放的 MP4/WebM，
**不引入任何播放器依赖**。

Mock 默认返回 MP4 以便真实验证播放；固定 `DOOR-003` 返回 HLS，
用于验证「该协议本期不支持」的明确提示。

## 安全声明

- 全部图片与视频地址均为**公开占位/测试资源**，不含任何真实监控画面。
- 不含摄像头账号、密码、RTSP 凭据、内部地址。
- `isSimulated` 字段对 MOCK 状态显式标记为 `true`。

## 与后端契约的关系

Mock 返回字段**同时携带驼峰与下划线两种形式**，
前端 `api/doorAnomaly.ts` 的 `pick()` 已兼容两种形式，
因此联调时无需改动页面代码。