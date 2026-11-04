# 仓门异动监测 · 交付交接文档

- 日期：2026-10-21（2026-10-22 更新：真实联调、功能权限、UI 复刻与提交推送）
- 工作区：`D:\liangku`（`manage` 前端 + `service` 后端，两个**独立 Git 仓库**；本文件所在根目录不在任一仓库内）
- 需求依据：`AI_HANDOFF.md`、`CODE_REVIEW_REPORT.md`
- 历史过程记录：`DOOR_ANOMALY_FIX_REPORT.md`（①~⑩，含每轮结论更正）
- **Git 状态：已提交并推送到两个仓库的 `feature/ys` 分支。**
  后端 `3868996`（55 文件 / +7307 行）、前端 `339464f`（28 文件 / +6085 行）。
  `service/pom.xml` 的 `chinkutils.version` 变更为用户预存，**刻意未纳入提交**，仍留在工作区。
  未部署。
- 📐 **要把这套结构复用到新功能上**：见同目录
  [FEATURE_SCAFFOLD_GUIDE.md](file:///D:/liangku/service/docs/door-anomaly/FEATURE_SCAFFOLD_GUIDE.md)
  —— 该文档自包含，含文件清单、改名映射、47 条硬约束（每条附踩坑原因与违反后果）、
  关键代码骨架、15 步实施顺序、验证门禁与故障速查表。

---

## 1. 一分钟看懂现在是什么状态

| 事项 | 状态 |
|---|---|
| 前端 Mock 模式浏览 | ✅ 可用，`npm run dev:mock` → 登录页随便填账号密码即可进入 |
| 后端编译 | ✅ `BUILD SUCCESS`（含功能权限一轮改动） |
| 前端定向 lint / 类型增量 | ✅ lint EXIT=0；vue-tsc 391 与基线一致，本模块 0 错误（详见第 10 节） |
| **后端运行时（真实库）** | ✅ **2026-10-22 已跑通**：启动成功、读接口返回真实数据、`markEventViewed` 写入与幂等实测通过（详见 3B） |
| **真实前端 → 真实后端 → 真实库** | ✅ **2026-10-22 关 Mock 全链路跑通**：真实登录、监测页、历史分页、查看即标记、导出（详见 3C.4） |
| 真实后端导出的 Excel | ✅ 已解 ZIP 逐单元格核对，含筛选下推验证（详见 3C.6） |
| 功能权限（谁能查看/导出） | 🟡 **代码已接入并运行时实测**（放行/拒绝双向验证），但**主库权限行未入库 → 开关默认 false → 当前部署形态下仍是任何登录用户均可**（详见 3C，解除方式见第 7 节第 1 条） |
| 仓库 ID 与主库组织对齐 | ⛔ **无法执行**：主库 `type=2`（仓库）组织节点数为 **0**，没有可对齐的真实 ID；脚本已交付未执行（详见第 6 节风险 3） |
| 多组织越权实测 | ⚠️ **部分**：超管→全域可见已验证；未认证拦截已验证；功能权限拒绝已验证；**跨组织隔离仍需第二个不同组织的非管理员账号**（SQL 层已离线断言通过） |
| SQL / 交付说明可跟踪 | ✅ 已迁入 `service/docs/door-anomaly/`（详见 8.6） |

> **交付口径**：本功能的读、写、鉴权（登录态 + 功能权限机制）、分页、乐观锁、
> 审计追加、导出二进制均已有**真实运行时证据**，可交付评审。
> **上线前仍必须完成两件事**：① 执行主库 RBAC 授权脚本并打开权限开关；
> ② 平台侧先建仓库组织（type=2），再回写对齐本库 `warehouse_id`。
> 两件都卡在**主库禁写**这一交付约束上，不是代码欠账。

---

## 2. 本次产品变更（2026-10-21，最重要）

**取消人工处理入口，改为"查看即已处理"，状态只保留两种。**

| 变更前 | 变更后 |
|---|---|
| 操作列：查看 + 处理 | 操作列：**仅查看** |
| 详情弹窗底部：关闭 + 去处理 | 详情弹窗底部：**仅关闭** |
| 独立处理弹窗 `EventProcessDialog.vue` | **已删除** |
| 5 种处理状态各自展示 | 只有 **未处理 / 已处理**（`PENDING`=未处理，其余=已处理） |
| 点查看是只读 | **点查看即写入 `CLOSED` 并追加一条审计** |

### 实现要点

- 新增后端端点 `POST /doorAnomaly/markEventViewed/{eventId}`，**只接受事件标识**，
  目标状态、处理人、处理意见一律由后端决定，不接受前端传入。
- 幂等：仅 `PENDING` 会被写成 `CLOSED`；已是其它状态时直接返回成功、
  **不重复改版本号、不重复追加审计**。
- 审计仍保留：`ai_event_process_record` 追加一条，处理意见固定为
  `查看事件详情后自动标记为已处理`（`DoorAnomalyConstant.AUTO_VIEW_OPINION`），
  以便与真实人工结论区分。
- 不新增枚举值、不改表结构：复用既有终态 `CLOSED`，
  存量 5 态数据自动归入"已处理"（前端标签映射本就是这样）。
- 前端在详情读取成功后才调用标记，**标记失败不关闭详情**（用户要看的内容已在屏幕上）。

### ⚠️ 与需求文档的冲突（必须知悉）

本变更**反转了 `AI_HANDOFF.md` 第 17.1 节原文**：

> 详情采用只读设计。人工处理使用独立的处理弹窗。

以及第 16.5 节按"未处理 / 处理中 / 已核实 / 已关闭"分别规定操作的规则。
这是用户 2026-10-21 明确决定的产品口径调整，**代码已按新口径实现，文档未同步更新**。
若后续要回到旧口径，需恢复：处理按钮、`EventProcessDialog.vue`、
`processEvent` 前端调用链（后端 `processEvent` 接口**一直保留未删**，可直接复用）。

### 后端 `processEvent` 接口的现状

**保留但前端已不调用**。它仍做完整的认证校验、数据范围校验、
乐观锁与终态拦截，并新增了 `@Pattern` 白名单校验（见 3.4）。
保留理由：真实人工核实结论在未来大概率仍需要，删除属不可逆且无收益。

---

## 3. 同日修复的缺陷（共 14 项）

> 3.1~3.12 与 3.14 为代码评审/审计发现；3.13 是用户在浏览器实测导出时报出来的。
> 除 3.13 外均为代码级或离线验证；**3.13 有浏览器与 curl 实测证据**。

### 3.1 🔴 分页被抢（**本轮自己引入的回归**）
`common/page/PageAcpect.java:67-71` 先 `startPage()` 再 `proceed()`；
而新增的数据范围解析在被包裹的调用内跑了一条主库 MyBatis 查询。
PageHelper 的 ThreadLocal 由**第一条被拦截的 SQL** 消费 →
组织树被按 `pageSize` 截断（**授权仓库集合被削短**且缓存 60 秒），
真正的事件查询**不带 LIMIT 全量返回**。
**修法**：`DoorAnomalyScopeResolver.queryOrgSubtreeUnpaged()` 查询前 `clearPage()`、
`finally` 原样归还。

### 3.2 🔴 导出功能必然失败，从未成功过一次
控制器把 `source`（前端传 `HISTORY`/`RECENT`，表示发起页面）当作
`ExcelExportConfig.config` 的模板 key，而该表只注册了 `settlementReport`
/ `registrationReport` → `config.get()` 返回 null → NPE。
前端 `responseType:'blob'` 且不校验内容，用户会**下载到损坏的 xlsx 并看到"导出成功"**。
**修法**：新增 `DoorAnomalyConstant.EXPORT_BIZ_TYPE = "doorAnomalyEvent"`
并在 `ExcelExportConfig` 注册 13 列表头。

### 3.3 🔴 `listEventByCondition` 缺 `@Param("query")`
实测 `ParamNameResolver` 得 `names=[arg0]`、`wrappedAsMap=false`
（实体被直接当 OGNL 根），XML 里的 `query.eventNo` 全部解析不到 →
**整个 `<where>` 静默退化，历史分页与导出在真实库上从未工作过**。
此前浏览器验收全走前端 Mock，因此从未暴露。

### 3.4 🔴 `processResult` 无白名单，可永久锁死事件
只有 `@NotBlank`（`@Schema.allowableValues` 是文档不是校验）。
传 `PENDING` 可重开终态事件；传任意字符串会让该事件的
`ProcessStatusEnum.valueOf` 永久解析失败，卡在"事件处理状态异常"且无恢复路径。
**修法**：`@Pattern(regexp="VERIFIED_TRUE|VERIFIED_FALSE")`。

### 3.5 🔴 数据范围防护实际未生效
`warehouseIds` 从未被填充，且 `inScope()` 写的是
`scope.allowsOrg(scope.getOrgId())` —— **自己和自己比，恒为 true**，
与 orgId 是否为空无关（原报告把根因归为"orgId 为空"，判断不准）。
**修法**：授权来源改为组织树 `type=2` 推导 + 语义改为**失败即拒绝**
+ 详情与写前校验改为 SQL 级 `getEventSummaryInScope`。详见 3.7。

### 3.6 🟠 P1-7 事务原子性不成立
Service 的 `@Transactional` 绑**主库**事务管理器，仓储方法绑
`doorAnomalyTransactionManager`，跨 TM 不加入外层 → 状态更新与审计记录
是**两次独立提交**，"状态已变但无审计"仍会发生。
**修法**：两个仓储方法合并为 `DoorAnomalyRepository.processEvent(...)`，
事务声明下沉到 `MyBatisDoorAnomalyRepository` 内部（与该 TM 同一条件注解，
生命周期一致），Service 去掉 `@Transactional`。
**不能只改注解名**：该 Bean 仅在 `enabled=true` 时存在，而默认 false，
按名解析会在首次调用抛 `NoSuchBeanDefinitionException`。

### 3.7 🟠 数据范围授权来源（本轮功能补齐）
`User` 实体无仓库字段，全仓无 user↔仓库关联表。
授权来源确定为 **`base_organization_info` 组织树**：`type` 字段
`1=公司、2=仓库`，取 `user.orgId` 及其全部后代中 `type=2` 的节点。
- 全域判定：`userType==1` 或 `regionId=="0"`（此前被硬编码 `false`）
- 60 秒 TTL 缓存（前端 10/15 秒轮询，不缓存会给主库压递归 CTE）
- **跨库禁止联表**（AI_HANDOFF 第 39.2/39.3 节）：仓库集合在 Java 侧
  经主库 `OrganizationMapper` 解析，再以参数传入仓门库 SQL
- 统一三分支（4 处 SQL 完全一致，改一处必须同步其余）：
  ```
  非全域 + 有授权仓库  ->  AND <别名>.warehouse_id IN (...)
  非全域 + 无授权仓库  ->  AND 1 = 0        （失败即拒绝）
  全域                 ->  不追加仓库条件
  ```

### 3.8 🟠 String 绑定到 BIGINT 列
JDBC URL 未设 `stringtype=unspecified`，用 String 绑定会以 varchar 发送，
PostgreSQL 报 `operator does not exist: bigint = character varying`。
影响 `scopeWarehouseIds`、业务筛选 `warehouseId`/`doorId`。
**修法**：全部改 `Long`；导出路径的字符串入参经 `parseId()` 显式转换，
非数值抛可读业务异常而不是变成 500。

### 3.9 🟠 结束日期被整天排除
`t-date-range-picker` 未开时间选择，用户交互后只返回 `yyyy-MM-dd`，
而 SQL 是闭区间 `occurred_at <= endTime` → 选中的最后一天 0 行。
默认 7 天窗口由前端拼完整时间串，所以首次加载看不出问题。
**修法**：`toBoundaryTime(value,'start'|'end')`，日期粒度补 `00:00:00` / `23:59:59`。

### 3.10 🟠 导出时间筛选静默失效
`parseTime()` 捕获解析异常后返回 null → 时间条件整体消失，
用户以为导的是筛选 7 天，实际拿到最多 5000 条全量历史且毫无提示。
**修法**：非空但格式非法时抛 `CustomException`。

### 3.11 🟠 内存实现交出库存活对象
`InMemoryDoorAnomalyRepository.getEventDetail` 直接返回 `detailStore` 里的对象，
而 Service 会把证据地址**就地**替换成短时地址：
`MockMediaGateway` 下每次查看再拼一个 `?Expires=`（无限增长）；
`S3MediaGateway` 下受控键被覆盖成 https 预签名地址后
`isControlledReference` 不再认可 → 证据图片永久失效。
**该实现是默认 Bean**（`matchIfMissing=true`）。
**修法**：返回深拷贝，证据列表逐条复制。

### 3.12 🟡 `listProcessRecords` 声明 scope 却完全不用
当前调用链（先 `getEventDetail` 校验）不会泄露，
但"签名承诺、实现忽略"是后续新增调用点的静默越权隐患。已改为 SQL 级校验。

### 3.13 🔴 导出按钮 500 + 损坏文件静默报成功（2026-10-21 收尾修复）

两个独立缺陷叠在一起：

1. **Mock 模式没有 `exportEvent` handler** → 请求被 vite 代理转发到空的
   `VITE_API_URL`，返回 500。这就是用户看到的报错。
2. **前端不校验响应内容**（真实后端也会踩）：后端异常时
   `GlobalDefaultExceptionHandler` 以 **HTTP 200** 返回 JSON，而本请求是
   `responseType:'blob'`，JSON 被包成 Blob；旧代码只判 `!res?.data`，
   Blob 恒为真值 → 用户**下载到一个内容为 JSON 的 .xlsx 并看到「导出成功」**。

**修法**：
- 新增 `mock/doorAnomaly/xlsx.ts`：零依赖的最小 XLSX 生成器
  （手写 STORED-ZIP + CRC32 + 最小 OOXML，单元格用 `inlineStr`）。
  项目 `node_modules` 里没有任何 Excel/ZIP 库，且边界约束要求不新增依赖；
  用 CSV/HTML 冒充 xlsx 会让 Excel 弹「格式与扩展名不符」，比报错更误导验收。
- `mock/doorAnomaly/index.ts` 新增 `exportEvent` handler，用 `rawResponse`
  （`response` 会被 `JSON.stringify` 序列化，二进制走不了那条路），
  表头与后端 `ExcelExportConfig` 的 `doorAnomalyEvent` 模板逐列对齐。
- 抽出 `filterEvents()` 供历史分页与导出共用，避免两侧筛选条件漂移。
- 前端改为按**响应头 content-type** 判断（不能看重建后的 Blob，
  其 type 已被强制成 xlsx），是 JSON/text 就解析出 `status.message` 抛错；
  同时补了零字节响应检查。
- 生成器对以 `= + - @ TAB CR` 开头的单元格值前缀单引号，防公式注入。

**验证**（实测，非推断）：
`curl` 得到 HTTP 200、MIME 为 OOXML、51642 字节、魔数 `PK\x03\x04`；
.NET `ZipFile` 可正常打开并列出 5 个条目；sheet 共 51 行（1 表头 + 50 数据），
含「处理状态」列且呈现两态文案；浏览器点真实按钮 → toast「导出成功」，
控制台 0 个 error。

> 仍然**未验证**的是真实后端 POI 产出的那份 xlsx（需要凭据启动后端）。

---

### 3.14 🟡 DDL 脚本自检门禁失效
`01_create_door_anomaly_tables.sql` 的前后置检查都写 `LIKE 'ai_door%'`，
而 6 张表前缀不统一，**只命中 1 张表** →
重复执行防护对其余 5 表失效，"预期返回 6 张表"的断言永不成立。
已改为显式 6 表名清单。（只改脚本内的 SELECT，未动任何表结构）

---

## 3B. 真实联调轮（2026-10-22，第一次跑通后端 + 真实库）

用户提供了仓门库凭据与超管 token，本功能**第一次**完成真实后端 + 真实 PostgreSQL 联调。
以下是这一轮的成果，全部有运行时证据。

### 3B.1 🔴 架构级阻塞：独立数据源导致应用根本起不来（已修）

**根因（读 mybatis-plus-spring-boot3-starter 3.5.15 字节码确认，非推测）**：

```java
@Bean @ConditionalOnMissingBean SqlSessionFactory sqlSessionFactory(DataSource)
@Bean @ConditionalOnMissingBean SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory)
```

仓门功能为物理隔离必须注册自己的 `SqlSessionFactory` / `SqlSessionTemplate`，
于是**主库这两个自动装配整体退让**，主库 Mapper 没有任何已绑定语句：

```
Invalid bound statement (not found): WorkerNodeMapper.addWorkerNode
```

**两条被实测否证的错误思路**（后来者不要再走）：
- 给主库 `dataSource` 加 `@Primary` → **无效**，退让条件不是 DataSource
- 把 `doorAnomalyDataSource` 改成非 Bean → **无效**，同理

**实际修法**：新增 `config/MainMybatisConfiguration.java`，显式声明主库
`SqlSessionFactory` + `SqlSessionTemplate`（`@Primary`），并满足三个要点：

1. 带 `@ConditionalOnProperty(enabled = true)`，**与仓门数据源同生同灭**。
   `enabled=false`（既有默认与现网形态）时本类完全不参与装配，
   其余 30+ 模块行为零变化 —— 这是影响面控制的关键。
2. 必须手动应用容器内全部 `SqlSessionFactoryBeanCustomizer`。
   `mybatis-plus-join` 靠这个扩展点注入联表能力，漏了则所有
   `MPJBaseMapper` 静默失效。
3. 主库 `mapper-locations`（`classpath*:/mapper/**/*.xml`）必须**排除
   `/mapper/doorAnomaly/`**，否则与本功能工厂重复注册同一批语句。

配置项从 `MybatisPlusProperties` 读取，并用 `CoreConfiguration.applyTo(...)`
落到 `MybatisConfiguration`（`getConfiguration()` 返回的是设置 POJO，不是配置对象）。

### 3B.2 🔴 P0：人工处理写路径从来没成功过（已修）

`updateProcessWithVersion` 的 XML 写了 `#{processedAt}`，
而 Mapper 接口声明的参数是 `updatedAt`，容器里根本没有 `processedAt`：

```
BindingException: Parameter 'processedAt' not found.
Available parameters are [eventId, processStatus, eventVersion, processedBy, processOpinion, updatedAt]
```

**这是先于本轮就存在的缺陷**，任何一次 `processEvent` 都会失败。
此前所有验证（编译、裸 JDBC 抄 SQL、动态 SQL 渲染）都只覆盖 `<select>`，
所以一直没暴露 —— **只有真跑一次写才抓得到**。
修法：`processed_at` 与 `updated_at` 取同一个时间戳，统一用 `#{updatedAt}`。

### 3B.3 🟠 详情接口缺"仅详情"字段（已修，风险项 4 关闭）

新增 `getEventDetailInScope`：`LEFT JOIN ai_algorithm_task` 补齐
`task_no / model_name / model_version / algorithm_version / config_version /
started_at / finished_at / inference_duration_ms`，并直接选出
`event_desc`、`is_simulated`。
仓储里那段 20 行手工字段拷贝（漏字段的根源）已删除。
用 `LEFT JOIN` 是刻意的：算法任务缺失或尚未跑完时，事件本身仍须可见。

### 3B.4 🟠 时间戳把微秒直接吐给前端（已修）

实测 `occurredAt = "2026-10-19 15:17:21.195589"`。
PostgreSQL TIMESTAMP 经 MyBatis 转 String 会带 6 位小数，
与 Mock 输出不一致且前端原样显示。
修法：SQL 侧统一 `to_char(..., 'YYYY-MM-DD HH24:MI:SS')`，
覆盖事件、仓门心跳、证据采集时间、处理记录时间。
排序仍用裸列，不用格式化后的字符串。

### 3B.5 本轮实测结果（全部为真实后端 + 真实库）

| 项 | 结果 |
|---|---|
| 后端启动（`enabled=true`） | ✅ `Started WebAdminApplication in 13.683s` |
| MPJ 定制器覆盖 | ✅ 日志显示 `doorAnomalySqlSessionFactory` 与 `sqlSessionFactory` 两个工厂都被处理 |
| 主库连通 | ✅ `系统配置缓存完成`（启动时真实 SELECT 主库） |
| `listDoorStatus` | ✅ 16 条真实数据，仓库 9001/9002/9003/9004 |
| `listRecentEvent` | ✅ 5 条 |
| `listEventByCondition` | ✅ 16 条，total=16（分页正常，反证 3.1 的 PageHelper 修复无副作用） |
| `getEventDetail/4001`（SUCCEEDED） | ✅ 模型 `door-anomaly-v1 / 1.2.3 / algo-2.0.1`、推理起止、耗时 150ms、置信度 0.9231、2 条证据 |
| `getEventDetail/4009`（RUNNING） | ✅ 模型与结束时间为空 —— 任务未跑完，属正确语义而非缺陷 |
| 时间戳格式 | ✅ 全部 `YYYY-MM-DD HH:MM:SS`，无微秒 |
| **`markEventViewed` 写入** | ✅ `PENDING/version=0/记录=0` → `CLOSED/version=1/记录=1`，审计内容 `PENDING -> CLOSED \| 查看事件详情后自动标记为已处理`，处理人 `超级管理员` |
| **`markEventViewed` 幂等** | ✅ 第二次调用返回"该事件已是已处理状态"，version 仍为 1、记录仍为 1 条，未重复自增或重复追加 |
| 未认证拦截 | ✅ 三个端点均返回 9109 |

**写入影响范围**：按用户授权，仅改动了种子事件 **4009** 一条
（`PENDING → CLOSED`，`event_version 0 → 1`，新增 1 条审计记录）。

### 3B.6 使用要点（部署与联调必读）

1. **JDK 21 必须加 JVM 参数**，否则 Orika 报错启动失败：
   `--add-opens java.base/java.lang=ALL-UNNAMED`
   （`java.util` / `java.time` 建议一并加）。
   `smart-storage-admin/pom.xml` 里硬写了 `<jvmArguments>-Dfile.encoding=utf-8</jvmArguments>`，
   会**覆盖** `-Dspring-boot.run.jvmArguments`，所以 `mvn spring-boot:run` 传不进去。
   本轮是绕开 maven、用 `java -cp` 直接启动的。**要写进正式启动脚本。**
2. **token 必须带 `Bearer ` 前缀**。裸 JWT 会被 `JwtUtils.parseToken` 拒 → 9109。
   前端 `userStore.setToken` 本来就是 `"Bearer " + accessToken`，一致。
3. 主库密码是 **SM4 加密存储、运行时解密**
   （`HikariDataSourceConfiguration:57`）。
   历史文档记的"主库凭据 28P01"是错的 —— 那是拿密文当明文去连。
   **不要用环境变量把明文塞进 `spring.datasource.password`**，应用会去解密它。
4. 启用仓门库的环境变量：
   `AI_DOOR_DB_ENABLED / HOST / PORT / USER / PASSWORD`
   （目标库名在代码里写死为 `smart_storage_ai_extension`，配错直接启动失败）。

---

## 3C. 第二轮：关 Mock 全链路 + 功能权限接入（2026-10-22）

本轮七项目标的达成情况，先给结论再给证据。

| # | 目标 | 结论 |
|---|---|---|
| 1 | 关 Mock，真实前端 → 真实后端 → 真实库 | ✅ 已跑通 |
| 2 | 接入查看/导出/处理功能权限 | 🟡 代码已接入并实测；**权限行未入库，开关默认 false** |
| 3 | 仓门库仓库 ID 与主库真实仓库组织 ID 对齐 | ⛔ **无法执行**：主库没有任何 type=2 组织 |
| 4 | 验证真实后端导出的 Excel | ✅ 已逐单元格验证 |
| 5 | 整理两仓提交范围，排除无关 pom.xml | ✅ 见第 11 节 |
| 6 | SQL 与交付说明纳入可跟踪仓库 | ✅ 已迁入 `service/docs/door-anomaly/` |
| 7 | TS 历史错误基线豁免 | ✅ 见第 10 节 |

### 3C.1 功能权限：为什么不能用权限码

实测证据（全部通过后端自身接口取得，未直连主库）：

- `base_rights_info` 实体只有 `id` 与 `name` 两列，**没有 code/perms/url 列**
  （`entity/rbac/Rights.java:15-26`）。
- 登录时 authority 被装配成 `new SimpleGrantedAuthority(Convert.toStr(right.getId()))`
  （`TokenUserDetailsServiceImpl:59`）—— **authority 就是数字权限 ID 的字符串**。
- 超管实际被授予的 9 个权限：`10101 添加`、`10102 修改`、`10201 添加`、
  `10202 修改`、`10203 删除`、`10302 添加`、`10303 修改`、`10304 删除`、`10403 删除`。
- 全仓 `@PreAuthorize` 只有两行**注释掉的**数字 ID 写法
  （`UserController:66`、`OrganizationController:64`）。除此之外，
  `WebSecurityConfig` 只有 `anyRequest().authenticated()`。
  **本仓库不存在任何生效的功能级授权。**

结论：`hasAuthority('doorAnomaly:event:export')` 这类写法在本项目**永远不可能命中**。
原先声明的 5 个权限码字符串（`DoorAnomalyConstant.Permission`）因此被删除，
改为 3 个**操作键** `VIEW / EXPORT / PROCESS`，必须映射到目标环境实际分配的
数字 ID 才生效。

### 3C.2 实现方式

- `config/DoorAnomalyPermissionProperties`：`door-anomaly.permission.{enabled, rights}`。
  ID 做成配置而非写死，因为权限 ID 由目标环境主库分配，测试与生产数值不同。
- `component/DoorAnomalyPermissionChecker`（bean 名 `doorAnomalyPermission`）：
  `enabled=false` 时恒放行（与接入前行为一致）；`enabled=true` 但某操作没配 ID 时
  **一律拒绝**，不会偷偷退化成"只要登录就放行"。
- 控制器 8 个端点全部加 `@PreAuthorize("@doorAnomalyPermission.has('...')")`：
  读接口与 `markEventViewed` 走 `VIEW`，`exportEvent` 走 `EXPORT`，
  保留的 `processEvent` 走 `PROCESS`（**不给它留一个无权限约束的写接口**）。
- 表达式里的操作键用 `DoorAnomalyConstant.Permission` 常量拼接，
  不是裸字符串 —— 保证常量再次变成"零处使用"时编译期就能发现。

### 3C.3 权限机制实测（真实后端运行时）

把 `VIEW` 临时映射到一个**不存在**的 ID（99999），`EXPORT` 映射到超管**确实持有**的
`10101`，同一次启动内双向验证：

```
POST /doorAnomaly/listDoorStatus  (VIEW->99999)
  -> {"status":{"code":"403","message":"没有访问权限",...}}
  日志：[DoorAnomaly] 越权拒绝 operation=VIEW requiredRightId=99999 loginId=18646006388

POST /doorAnomaly/exportEvent     (EXPORT->10101)
  -> HTTP 200, 5344 bytes, PK\x03\x04 (真实 XLSX)
```

放行与拒绝都按预期发生，机制本身是**可验证的**；缺的只是权限行。
补齐步骤见 `service/docs/door-anomaly/sql/05_rbac_provision.sql`（**未执行**，主库禁写）。

⚠️ 该脚本里记录了一个必须知道的授权模型限制：
登录取权限的 SQL 按 `menu_id` 取该菜单**全部**权限，**不按角色区分**。
所以"查看"和"导出"挂在同一菜单时无法拆开授权 —— 给了菜单就两个都有。
若要真正拆分，需要单独的不显示菜单行，或改造 `RightsMapper.listRightsByRoleGroupId`。

### 3C.4 关闭 Mock 的真实前端全链路

`.env.development` 里 `VITE_USE_MOCK = false`（本轮把开关做成环境变量，
默认即真实后端）。`vite --mode development` 启动，浏览器实测：

- 真实登录：`loginByPassword`（RSA + 真实图形验证码）→ `getLoginBaseInfo`
  → `getLoginMenuInfo`，全部打到 42000。
- 监测页：16 个仓门、最近 5 条事件，数据来自真实 `smart_storage_ai_extension`。
- 历史记录：默认 5 天窗口 **15 条**（正确排除了 10-12 的 DA20260008 之外的
  DA20260012），分页 2 页，`listEventByCondition` 走服务端分页。
- 查看即标记：点 DA20260008 的"查看" → `getEventDetail/4008` → `markEventViewed/4008`
  → 下一次轮询 `listRecentEvent` 显示已处理。**再独立用 curl 读服务端确认**：
  `DA20260008 | CLOSED | v1 | 超级管理员 | 查看事件详情后自动标记为已处理`
  —— 不是前端本地改状态。
- 导出：页面"导出记录"按钮 → `exportEvent` 200 +
  `mimeType=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`。
- 处理状态列全库只剩**未处理 / 已处理**两态，遗留的
  `PROCESSING / VERIFIED_TRUE / VERIFIED_FALSE` 正确折叠为"已处理"。

> 本轮把种子事件 **4008** 也写成了 CLOSED（原先只有 4009）。
> 需要复原时重跑 `04_seed_test_data_v2.sql` 即可。

### 3C.5 本轮顺带修掉的 3 个真实缺陷

1. 🔴 **侧边栏图标渲染失败**。`router/modules/aiVision.ts` 原来把
   `VideoCamera2Icon` **组件对象**放进 `meta.icon`。菜单数据经过 store 的
   JSON 持久化往返后组件对象退化成普通对象，控制台报
   `Component is missing template or render function: <VideoCamera2Icon>`，
   图标位置空白。改为仓库既有的字符串写法 `'video-camera-2'`，
   实测渲染出 `class="t-icon t-icon-video-camera-2"`。
   （`homepage.ts` 里 `shallowRef(DashboardIcon)` 那种写法有同样隐患。）
2. 🔴 **证据图片放大从来没生效过**。`EventDetailDialog.vue` 用了
   `<t-image-preview :preview-list>` —— TDesign **没有这个组件**，
   控制台一直报 `Failed to resolve component: t-image-preview`，
   点击放大静默无效。改为仓库既有的 `<t-image-viewer :images>`
   （参照 `entry/wareHouseIndex/components/uploadCom.vue`）。
   **验证到什么程度**：控制台报错已消失、组件可解析。
   但**放大动作本身没能实测** —— 当前证据卡片全部显示"无图"，
   因为媒体网关没有可用的对象存储地址（`MockMediaGateway`/`S3MediaGateway`
   未接真实存储），弹窗里根本没有 `<img>` 元素可点。
   等有真实证据 URL 后需再点一次确认。
3. 🟠 **导出表处理状态是英文枚举**。真实 Excel 里 J 列原本是
   `CLOSED / PENDING / PROCESSING / VERIFIED_TRUE`，与同表 F 列
   「异动类型」已翻译成中文不一致，也不符合两态产品口径。
   新增 `processStatusName`（仅导出链路填充，接口仍返回原始编码由前端映射）。

修复后控制台仅剩 `inject() can only be used inside setup()` 与
`computed value is readonly` 两条**登录页也会出现**的既有告警，与本功能无关。

### 3C.6 真实后端 Excel 逐单元格验证

不再依赖"看起来像 xlsx"，而是解 ZIP + 读 OOXML 断言内容：

```
全量导出  .real.xlsx  5384 bytes
  9 个 OOXML 条目（DEFLATE），sharedStrings uniqueCount=131
  行数 17 = 1 表头 + 16 数据   ← 与 listEventByCondition total=16 一致
  表头 13 列，与 ExcelExportConfig.doorAnomalyEvent 逐列一致：
    事件编号|发生时间|仓库名称|仓门位置|摄像头|异动类型|算法状态|
    算法结论|置信度|处理状态|处理人|处理时间|处理意见
  时间列渲染为 2026-10-19 15:17:21（无微秒尾巴）
  Content-Disposition: filename=%E4%BB%93%E9%97%A8... = 仓门异动记录_20261022.xlsx

带筛选导出 warehouseId=9003  .real2.xlsx  4046 bytes
  行数 4 = 1 表头 + 3 数据，且 3 行仓库名称全是「三号粮库」
  ← 证明数据范围/筛选条件真的下推到了 SQL，不是全量再截断
```

修复后（3C.5 第 3 条）重新导出：J 列变为 `已处理 / 未处理`，行数仍 17。


---

## 4. 如何运行

### 4.1 前端 Mock（无需后端即可完整浏览）
```bash
cd D:/liangku/manage
npm run dev:mock          # 端口 3002，VITE_USE_MOCK=true
```
- 打开 http://localhost:3002/ → 登录页**账号、密码、验证码随便填**
  （验证码图片显示 `DEMO`）
- 监测页 `http://localhost:3002/aiVision/doorAnomaly/monitor`
- 历史页 `http://localhost:3002/aiVision/doorAnomaly/history`
- 路由是 `createWebHistory`（**非 hash**），`#/xxx` 会落到 404

登录之所以能通，是因为新增了 `mock/auth/index.ts`（4 个 handler）。
它只在 `VITE_USE_MOCK === 'true'` 时被 `viteMockServe` 装载
（`vite.config.ts:51`），联调与生产构建不注册，**不构成线上认证绕过**。

### 4.2 后端编译
```bash
cd D:/liangku/service
JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot" \
  mvn -o -s C:/Users/15411/.m2/settings.xml -pl smart-storage-admin -am -DskipTests compile
```

### 4.3 后端启动（尚未验证过，需要凭据）
需要三类凭据，全部经环境变量注入，不写入文件：
- 仓门库 `AI_DOOR_DB_HOST/PORT/USER/PASSWORD` → 目标库固定
  `smart_storage_ai_extension`（代码里有白名单常量强制校验，配错直接启动失败）
- 主库 `smart-storage-site`（后端启动必需；既有 `application-dev.yml`
  凭据此前返回 PG `28P01` 认证失败）
- Redis（`redisson-dev.yml` 有配置；登录态与菜单缓存都在这里）

`door-anomaly.datasource.enabled` **默认 false** → 不创建独立数据源，
回退内存实现（内存实现无数据装载入口，所以列表为空）。

### 4.4 离线验证工具（零凭据、不连库）
```bash
cd D:/liangku/docs/dbtool
python ddl_diff.py        # 6 实体 ↔ 6 表逐字段比对
# ScopeSqlVerify：加载真实 Mapper XML，取 BoundSql 断言动态 SQL 分支
#   1) mvn -o -pl smart-storage-admin dependency:build-classpath -Dmdep.outputFile=cp.txt
#   2) python make_cp_args.py <cp.txt 绝对路径>
#   3) javac -encoding UTF-8 -proc:none @cp.arg ScopeSqlVerify.java
#   4) java @crun.arg -Dfile.encoding=UTF-8 ScopeSqlVerify <AiAnomalyEventMapper.xml 绝对路径>
```
`ScopeSqlVerify` 的存在理由：此前的 `VerifyMapperSql.java` 是把 SQL 手抄成
字符串走裸 JDBC，**完全绕过 MyBatis 参数绑定与动态标签**，
所以 3.3 那个 `@Param` 缺陷不可能被它发现。

---

## 5. 验证结论（本轮实测，非推断）

| 检查 | 结果 |
|---|---|
| 后端 `mvn compile` | **BUILD SUCCESS** |
| `ScopeSqlVerify` 动态 SQL 渲染 | **全部断言通过**（含 3 分支范围条件、`@Param` 解析、BIGINT 数值绑定） |
| `ddl_diff.py` 实体↔表 | 6 张表**逐字段完全一致**（29/21/36/13/12/11 列） |
| 定向 eslint（门模块 + api + mock + vite.config） | **EXIT=0** |
| `vue-tsc --noEmit` 全仓 | **391 个错误 = 历史基线**，本模块 **0** |
| 浏览器：操作列 | 只有"查看"，**处理按钮已消失** |
| 浏览器：详情弹窗按钮 | `[原始资源, 原始资源, 算法标注, 关闭]`，**"去处理"已消失** |
| 浏览器：点查看后行状态 | **未处理 → 已处理**（DA101000 实测） |
| 浏览器：重复查看幂等 | 第二次查看仍"已处理"，弹窗正常 |
| 导出（curl） | HTTP 200、MIME 为 OOXML、51642 字节、魔数 `PK\x03\x04` |
| 导出（ZIP 校验） | .NET `ZipFile` 正常打开，5 个条目齐全；sheet 51 行（1 表头 + 50 数据） |
| 导出（浏览器点真实按钮） | toast「导出成功」，**不再 500** |
| 控制台 error 级消息 | **0** |

### ⚠️ 旧验收矩阵已部分失效
`DOOR_ANOMALY_FIX_REPORT.md` ⑤ 那份"24 项 23 通过"是在本次产品变更**之前**跑的，
其中至少 3 项断言现在与新口径矛盾，**不能再作为本次交付依据**：
- "事件查看为只读，不改变处理状态" → 现在**故意会改变**
- "详情底部有独立去处理" → 该按钮**已删除**
- "历史页行内独立处理入口" → 该入口**已删除**

需要一份反映新口径的验收矩阵，见第 7 节待办。

---

## 6. 未验证 / 已知风险（按严重度）

1. ~~🔴 **后端运行时证据为零**~~ → **已关闭**（2026-10-22 真实联调，见 3B）。
   读接口、写接口、乐观锁、审计追加、幂等、未认证拦截均已有运行时证据。
   ~~**仍未覆盖**：真实后端 POI 产出的导出 xlsx~~ → 已关闭（见 3C.6）。
   **仍未覆盖**：跨组织多账号越权（缺第二个非管理员账号）。
2. 🟠 ~~**功能权限完全未接入**~~ → **代码已接入并运行时实测**（见 3C.2/3C.3）。
   残留风险改为：**权限行未入库，`door-anomaly.permission.enabled` 只能保持 false**，
   所以**当前部署形态下仍然是"任何登录用户都能查看、标记已处理、批量导出"**。
   解除方式 = 执行 `docs/door-anomaly/sql/05_rbac_provision.sql`（主库，本次未执行）
   + 注入 3 个环境变量。**这是上线前必须完成的一步，不是可选项。**
3. 🔴 **上线前置条件：仓库 ID 必须同源 —— 且现在连可对齐的组织都不存在**。
   实测确认 `ai_*.warehouse_id` 现有值为 **9001~9004**（手工编的种子值），
   而数据范围是按主库 `base_organization_info` 中 `type=2` 节点的真实组织 ID 过滤。
   两者不同源 → **非全域账号查不到任何数据**（不是报错，是空集）。
   超管能看到全部，正是因为它走 `unrestricted` 分支绕过了该过滤。
   **2026-10-22 追加实测**：主库 `base_organization_info` 全表仅 3 行
   （`0 总部公司`、`69440756366229552 齐齐哈尔`、`197976620592889895 123`），
   **type 全部为 1（公司），type=2（仓库）节点数为 0**。
   所以第 3 项被**双重阻塞**：既要平台侧先建仓库组织，
   又要本库回写对齐（脚本 `06_warehouse_id_alignment.sql`，护栏已加，未执行）。
   在此之前，**任何非全域账号必然看到空列表** —— 这与代码无关，是数据现状。
4. ~~🟠 详情接口缺"仅详情"字段~~ → **已修并实测通过**（见 3B.3）。
5. 🟠 **`@JsonIgnore` 不是实际防护机制**：项目注册的是
   `FastJsonHttpMessageConverter`（fastjson2），不是 Jackson。
   防护实际靠 `applyScope()` 无条件覆盖 scope 字段，**目前成立**，
   但 DTO 注释描述了一个它并未提供的保证。
6. ~~🟠 `t-image-preview` 组件未全局注册~~ → **已修**（2026-10-22，见 3C.5 第 2 条）。
   根因不是"未注册"，而是 **TDesign 根本没有这个组件**，正确标签是 `t-image-viewer`。
   同时修掉侧边栏图标渲染失败（3C.5 第 1 条）。
   **新发现**：`homepage.ts:226` 的 `shallowRef(DashboardIcon)` 与
   `user.ts:28` 的 `shallowRef(LogoutIcon)` 属于同一类隐患
   —— 路由 `meta.icon` 放组件对象会经 store JSON 持久化退化，
   只有字符串名是安全的。本轮只改了本功能的 `aiVision.ts`，
   **既有模块的同源问题未动**（超出授权范围）。
7. 🟡 **登录态失效被降级成"无数据"**：`resolve()` 捕获了包含
   `USER_LOGIN_EXPIRE` 的异常，导致应用统一的 `9102` 跳登录对本功能不生效，
   "登录过期"会表现为"没有数据"。
8. 🟡 **`snowflake.worker-id` 默认 1、`datacenterId` 硬编码 1** →
   多副本部署时审计记录主键相撞，合法请求会被回滚。
9. 🟡 **独立库 JDBC URL 拼的是 MySQL 风格参数**（`serverTimezone` 等），
   pgjdbc 全部忽略，预期的时区固定实际未生效。
10. 🟡 **`uk_ae_task_id_sim UNIQUE(algorithm_task_id, is_simulated)` 建模错误**：
    PG 中 NULL 互不相等，且同一 task 可各存 TRUE/FALSE 一行，
    达不成幂等目标。需连库 `ALTER`，未做。
11. 🟡 `ai_door_config` 缺 `warehouse_id` 索引（表小，非阻塞）。
12. 🟡 视频 `PLAYING`/`LOADING`/`EXPIRED` 三态从未被实际触发。
13. ~~🟡 真实后端 POI 产出的 xlsx 未验证~~ → **已关闭**（2026-10-22，见 3C.6）。
     已解 ZIP 逐单元格核对：9 个 OOXML 条目、13 列表头与模板逐列一致、
     全量 17 行（1 表头 + 16 数据，与接口 total 相符）、
     带 `warehouseId=9003` 筛选 4 行且仓库名称全为「三号粮库」，
     证明筛选/数据范围确实下推到 SQL 而非取回后截断。
     Mock 与真实导出是两套生成实现，此前**不能互相替代**，现在各自都验过了。
14. 🟡 `@RegionSecurity` 切面（`aop/RegionSecurity.java` +
    `component/RegionSecurityComponent.java`）**全仓 0 处使用**，是死代码。


---

## 7. 接手人待办（建议顺序）

已完成项不再列出（原第 2、4、5 条分别由 3B/3C.6、3B.3、3C.5 关闭）。

1. 🔴 **让功能权限真正生效** —— 唯一阻塞上线的一条：
   ① 请 DBA 在主库执行 `docs/door-anomaly/sql/05_rbac_provision.sql`（本次未执行）；
   ② 用脚本尾部查询取到实际权限 ID，注入
   `AI_DOOR_PERMISSION_ENABLED / AI_DOOR_RIGHT_VIEW / AI_DOOR_RIGHT_EXPORT / AI_DOOR_RIGHT_PROCESS`；
   ③ 用一个未绑定菜单的账号复测 403。
   **注意 3C.3 末尾的授权粒度限制**：同菜单权限不可按角色拆分。
2. 🔴 **建仓库组织并对齐 ID** —— 主库当前 `type=2` 节点数为 0（见第 6 节第 3 条）。
   平台侧建好组织后，执行 `06_warehouse_id_alignment.sql` 回写仓门库。
3. 🔴 **补第二个非管理员账号做跨组织越权实测**（唯一还没有运行时证据的安全路径）。
4. 🟠 定前端按钮级权限显隐。本轮**只做了后端鉴权**，
   前端 `constants` 里的 5 个权限码仍是死代码（后端已删，前端未同步删，
   因为前端接入方式要等第 1 条的 ID 落地后一起定）。
5. 🟠 决定 `package.json` 的 `build` 脚本与 391 条历史类型错误如何共存（见第 10.4 节）。
   **不改的话 `npm run build` 一定失败。**
6. 🟡 按新口径重写验收矩阵（去掉处理按钮断言，新增"查看即标记 + 幂等"断言）。
7. 🟡 与业务确认后同步更新 `AI_HANDOFF.md` 第 16.5 / 17.1 节。
8. 🟡 第 6 节第 5、7~12、14 条（`@JsonIgnore` 语义、登录态降级、雪花 ID、
   JDBC 参数、唯一键建模、索引、视频三态、`@RegionSecurity` 死代码）逐条决定是否处理。

---

## 8. 文件清单

### 8.1 本轮新增/删除
```
新增  manage/mock/auth/index.ts                                    登录链路 Mock（4 个 handler）
新增  manage/mock/doorAnomaly/xlsx.ts                              零依赖最小 XLSX 生成器（导出用）
新增  service/.../config/MainMybatisConfiguration.java             显式主库 SqlSessionFactory/Template
                                                                  （仅 enabled=true 时生效，见 3B.1）
新增  service/.../constant/DoorAnomalyConstant.java 中的
      EXPORT_BIZ_TYPE / AUTO_VIEW_OPINION 常量
删除  manage/src/pages/aiVision/doorAnomaly/components/EventProcessDialog.vue
```

### 8.2 本轮（产品变更）修改
```
后端  service/.../service/inter/doorAnomaly/DoorAnomalyService.java          +markEventViewed
      service/.../service/impl/doorAnomaly/DoorAnomalyServiceImpl.java       +markEventViewed 实现
      service/.../controller/doorAnomaly/DoorAnomalyController.java          +端点、导出模板 key 修正
前端  manage/src/api/doorAnomaly.ts                                          +markEventViewed，-processEvent
      manage/src/pages/aiVision/doorAnomaly/types/index.ts                  -EventProcessPayload
      manage/src/pages/aiVision/doorAnomaly/utils/displayStatus.ts          -canProcessEvent
      manage/src/pages/aiVision/doorAnomaly/index.vue                       删处理接线，查看后标记
      manage/src/pages/aiVision/doorAnomaly/history.vue                     同上 + 时间边界修正
      manage/src/pages/aiVision/doorAnomaly/components/RecentEventTable.vue 删处理按钮
      manage/src/pages/aiVision/doorAnomaly/components/HistoryEventTable.vue 删处理按钮
      manage/src/pages/aiVision/doorAnomaly/components/EventDetailDialog.vue 删去处理按钮
Mock  manage/mock/doorAnomaly/index.ts                                       +markEventViewed，-processEvent/-processEventFail
      manage/mock/doorAnomaly/README.md                                      同步
```

### 8.3 同日缺陷修复涉及的其他文件
```
后端  dto/doorAnomaly/DoorAnomalyDataScope.java        两态语义、List<Long>、失败即拒绝
      dto/doorAnomaly/EventQueryReqDTO.java            scope 字段重构、Long 类型
      service/impl/doorAnomaly/DoorAnomalyScopeResolver.java   【新增】授权来源
      service/impl/doorAnomaly/MyBatisDoorAnomalyRepository.java   事务下沉、SQL 级校验
      service/impl/doorAnomaly/InMemoryDoorAnomalyRepository.java  同语义、深拷贝
      service/inter/doorAnomaly/DoorAnomalyRepository.java   两方法合并为 processEvent
      mapper/doorAnomaly/AiAnomalyEventMapper.java/.xml      范围下推、@Param、BIGINT
      config/ExcelExportConfig.java                          新增 doorAnomalyEvent 模板
      dto/doorAnomaly/EventProcessReqDTO.java                @Pattern 白名单
文档  D:/liangku/docs/sql/01_create_door_anomaly_tables.sql  自检门禁修正（未动表结构）
工具  D:/liangku/docs/dbtool/{ScopeSqlVerify.java,ddl_diff.py,make_cp_args.py}
```

### 8.4 明确未触碰
- `service/pom.xml`（`chinkutils.version` 是**用户既有修改**，回滚时切勿包含）
- `application-dev.yml`、`application-site.yml`、`smart-storage-common`
- `package.json` / lockfile、全局样式、部署与安全策略配置
- 未执行任何数据库迁移、未连任何库、未执行 Git 写操作

### 8.5 工作区遗留物与清理状态

> ⚠️ **勘误**：本节此前声称 `docs/dbtool/{cp.txt,cp.arg,crun.arg,*.class}`
> "已全部删除"，**该说法是错的** —— `crun.arg` 正是本轮启动后端所用的文件，
> 一直都在。旧结论未复核就写进文档，是本轮交付里第二次出现同类问题
> （第一次是把不存在的"建表 SQL 尚未存在"当成待办）。
> 下面按 2026-10-22 实际 `ls` 结果重写。

**已迁入仓库**（仓库内副本为**权威版本**，后续以它为准）：

```
docs/sql/01_create_door_anomaly_tables.sql  -> service/docs/door-anomaly/sql/
docs/sql/02_rollback_door_anomaly_tables.sql-> service/docs/door-anomaly/sql/
docs/sql/04_seed_test_data_v2.sql           -> service/docs/door-anomaly/sql/
DELIVERY_HANDOFF.md                         -> service/docs/door-anomaly/（见 8.6）
```

> `D:\liangku\docs\sql\` 下的**原件暂未删除**：`DOOR_ANOMALY_FIX_REPORT.md`、
> `START_GUIDE.md`、`docs/door-anomaly-db-plan.md`、`docs/dbtool/ddl_diff.py`
> 与 `.workbuddy/memory/` 两篇日志都按旧路径引用它们，删了会断掉历史报告的证据链。
> 本文件（根目录副本）已改成指向仓库内权威版本的指针，避免出现两份会分叉的正文。

**本轮新产生的临时物（已删除）**：

```
.xlsxcheck.mjs .zipdump.mjs        XLSX 解包断言脚本（结论已抄进 3C.6）
.real.xlsx .real2.xlsx .perm.xlsx  三次真实导出的下载件
.eh.txt .eh2.txt                   导出响应头
.export-error.json                 那次被误判成导出缺陷的 500 响应体
.tok                               超管 JWT（用完即删，未写入任何脚本或文档）
.menu.json .org0.json .orgall.json 主库菜单/权限/组织实测原始返回
.tsc.txt                           vue-tsc 全量输出
.captcha.png                       图形验证码截图
```

**仍在工作区、需接手人拍板是否删除**：

```
D:\liangku\verify-final\        上一轮验收截图（旧报告引用为证据，删了会断链）
D:\liangku\verify-final.txt     上一轮验收输出
D:\liangku\cdp-verify.mjs       上一轮 CDP 验收脚本
D:\liangku\.cdp-dg .cdp-dp .cdp-f2~f9 .cdp-final .cdp-g1 .cdp-g2   Chrome 临时 profile
D:\liangku\docs\sql\03_seed_test_data.sql   已被 v2 取代的旧种子脚本
D:\liangku\docs\dbtool\                     一次性验证工具（见下）
D:\liangku\.start-backend.sh                本地启动脚本（凭据仍由环境变量注入）
```

`docs/dbtool/` 里是**离线/连库验证工具**（`ScopeSqlVerify`、`DbMapperSelect`、
`DbScopeCheck`、`ddl_diff.py`、`make_cp_args.py` 等）与编译产物。
它们不是交付物，但**是本轮若干结论的唯一可复现手段**
（例如 P0 的 `#{processedAt}` 参数缺失只有真跑一次写路径才会暴露）。
建议：要么整理进 `service/docs/door-anomaly/tools/` 并去掉 `.class`，
要么明确放弃复现能力后整目录删除。**本轮未擅自决定。**

### 8.6 交付物在仓库中的位置

```
service/docs/door-anomaly/
├── DELIVERY_HANDOFF.md                 本文件
└── sql/
    ├── 01_create_door_anomaly_tables.sql   建表（已在 smart_storage_ai_extension 执行）
    ├── 02_rollback_door_anomaly_tables.sql 回滚
    ├── 04_seed_test_data_v2.sql            种子数据（可重跑复原 4008/4009 的写入）
    ├── 05_rbac_provision.sql               主库授权【未执行，主库禁写】
    └── 06_warehouse_id_alignment.sql       仓库 ID 对齐【未执行，缺 type=2 组织】
```

`05`/`06` 两个脚本从头到尾**没有跑过一次**，属于交付给平台侧的待执行项，
不要当成已验证内容。

---

## 9. 回滚指引

**产品变更（查看即已处理）回滚**需恢复：`EventProcessDialog.vue`（需重建，已删）、
两个表格的处理按钮与 `process` emit、详情弹窗 `can-process`/`去处理`、
`api/doorAnomaly.ts` 的 `processEvent`、`types` 的 `EventProcessPayload`、
`displayStatus.ts` 的 `canProcessEvent`，并把两个页面的 `openDetail`
里的 `markViewed` 调用去掉。后端 `processEvent` 接口未删，可直接复用。

**数据范围（3.5/3.6）不建议回滚** —— 回滚等于把越权防护退回"不拦截"状态。

按阶段回滚的完整文件映射见 `DOOR_ANOMALY_FIX_REPORT.md` ⑦。
禁止使用 `git reset --hard` / `git checkout -- .`（会连带清掉用户自己的改动）。

---

## 10. 全仓 TypeScript 历史错误：允许作为基线豁免

**结论：允许豁免，但只允许按"增量门禁"的方式豁免，不允许当作"类型检查已通过"。**

### 10.1 实测数字（2026-10-22，本轮改动之后）

```
命令：cd manage && npx vue-tsc --noEmit
退出码：2（失败）
error TS 总数：391
涉及文件数：29
本功能（路径含 doorAnomaly / aiVision）：0
```

391 与本轮改动之前**完全相同**，即本功能新增 23 个前端文件、
修改 5 个既有文件，**没有引入任何一条新的类型错误**。
典型历史错误示例（与本次交付无关）：
`src/utils/request/index.ts:194` `Property 'rfToken' does not exist on type 'AxiosRequestConfig<any>'`。

### 10.2 豁免的准确含义

| 说法 | 是否成立 |
|---|---|
| "全仓 391 个类型错误已修完" | ❌ 不成立，一个都没修 |
| "本功能可以通过类型检查" | ❌ 不成立，`vue-tsc` 整体退出码仍是 2 |
| "本功能不新增类型错误" | ✅ 成立，已实测 |
| "构建产物可用" | ✅ 成立，`vite build` 不跑 `vue-tsc`；但 `npm run build` **会**跑，见 10.4 |

### 10.3 建议的门禁写法

把基线固化成数字，只卡增量，不要求清零：

```bash
npx vue-tsc --noEmit 2>&1 | grep -cE "error TS"   # 与基线比较
npx vue-tsc --noEmit 2>&1 | grep -E "error TS" \
  | grep -iE "doorAnomaly|aiVision" && exit 1      # 本功能必须恒为 0 条
```

基线值 **391 / 29 个文件** 应由前端负责人确认后写入 CI。
谁修掉一批历史错误，就把基线数字调小；**调大基线必须单独说明原因**。

### 10.4 一个必须先解决的现实冲突

`manage/package.json` 里 `"build": "vue-tsc --noEmit && vite build --mode release"`。
也就是说**只要保留这 391 条历史错误，`npm run build` 就必然失败**。
所以"豁免"不能只写在文档里，必须二选一：

1. 把 `vue-tsc` 从 `build` 脚本里摘出去，改为独立的 `type-check` 脚本 + CI 增量门禁；或
2. 先清完 391 条。

本轮**没有擅自改 `package.json`**（超出"只改这个功能"的授权范围）。
这是接手人要替前端负责人拍板的第一个问题。

### 10.5 偿还责任

391 条历史错误不属于本功能，本功能也不背这个债。
建议由前端负责人按文件归属排期，与本功能交付解耦。

---

## 11. 提交范围

两个仓库的完整改动清单。**均未执行 `git add` / `commit` / `push`**（未获授权）。

### 11.1 service 仓库（3 个修改 + 51 个新增）

 **必须排除**：

```
pom.xml     ← 用户自己的 chinkutils.version 改动，与本功能无关，
              不属于本次交付，不得混入本功能的提交
```

✅ 属于本功能的既有文件修改（2 个）：

```
smart-storage-admin/src/main/java/.../config/ExcelExportConfig.java
    注册 doorAnomalyEvent 导出模板（13 列）
smart-storage-admin/src/main/resources/application.yml
    增加 spring.config.import: optional:classpath:application-door-anomaly.yml
```

✅ 新增 —— 按提交意图分 4 组，比一坨 51 个文件好审：

| 组 | 内容 | 文件 |
|---|---|---|
| A 数据访问与双数据源 | 让本功能连上独立库且不破坏主库装配 | `config/DoorAnomalyDataSourceConfiguration`、`config/DoorAnomalyMybatisConfiguration`、`config/MainMybatisConfiguration`、`resources/application-door-anomaly.yml`、`entity/doorAnomaly/*`（6）、`mapper/doorAnomaly/*`（3）、`resources/mapper/doorAnomaly/*`（3） |
| B 业务功能 | 接口、DTO、服务、数据范围 | `controller/doorAnomaly/`、`dto/doorAnomaly/*`（14）、`service/inter/doorAnomaly/*`（4）、`service/impl/doorAnomaly/*`（6）、`constant/DoorAnomalyConstant`、`enumbean/DoorAnomalyStatusEnum` |
| C 功能权限 | 3C 节 | `config/DoorAnomalyPermissionProperties`、`component/DoorAnomalyPermissionChecker` |
| D 交付物 | 第 6 项要求 | `docs/door-anomaly/**`（5 个 SQL + 本文件） |

> C 组依赖 A/B 才能编译，但**功能上独立**：`enabled` 默认 false，
> 单独回滚 C 组不影响 A/B。

### 11.2 manage 仓库（5 个修改 + 23 个新增）

✅ 修改（全部与 Mock 开关有关，无夹带）：

```
.env  .env.development  .env.site  .env.test   各加一行 VITE_USE_MOCK = false
                                                  （顺带补了文件末尾缺失的换行）
vite.config.ts                                    viteMockServe enable 改由该变量控制
```

✅ 新增：

```
src/pages/aiVision/**          12 个（2 页面 + 5 组件 + 4 composable + constants/types/utils）
src/api/doorAnomaly.ts
src/api/model/doorAnomalyModel.ts
src/router/modules/aiVision.ts
mock/doorAnomaly/*             4 个（index / fixtures / xlsx / README）
mock/auth/index.ts             仅 Mock 模式生效；真实联调下不参与
.env.mock                      --mode mock 的独立配置
```

⚠️ 提交前须知：`mock/auth/index.ts` 与 `.env.mock` 只为"无后端也能浏览"服务。
如果团队不接受 `mock/` 下出现非本功能的账号桩，删掉这两个即可，
代价是 `VITE_USE_MOCK=true` 时无法登录（真实联调不受影响）。

### 11.3 建议的提交顺序

```
1) service  A 组   （双数据源 + 主库装配修复 —— 风险最高，单独一个提交便于回滚）
2) service  B 组   （业务功能）
3) service  C 组   （功能权限，默认关闭）
4) service  D 组   （docs/door-anomaly：SQL + 交付说明）
5) manage   前端功能 + Mock 开关
6) manage   3C.5 的 3 个真实缺陷修复（图标 / 图片预览 / 导出中文状态）
```

第 1 条提交单独存在，是因为 `MainMybatisConfiguration` 会影响
**其他 30+ 模块**的 SqlSessionFactory 装配路径（虽然 `enabled=false` 时完全不装配）。
它值得一次独立 review 和一次独立 revert。

