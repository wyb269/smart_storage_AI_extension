# 功能脚手架复用指南（以「仓门异动监测」为唯一基准实现）

> **给接手这个文档的 AI / 工程师：**
> 本文档描述的是**已经真实联调通过、已提交并推送到 `feature/ys` 分支**的一套完整功能骨架。
> 你的任务是**复制这套骨架，只替换名称、业务字段、文案和 UI 形式**，
> 而不是重新设计。
>
> **三条铁律：**
> 1. 第 6 节「硬约束清单」中的每一条都来自一次真实故障或一次真实踩坑。**不得省略、不得"简化"、不得"优化掉"。**
> 2. 动手前先跑第 2 节的「基准验证」，确认你看到的代码和本文档描述一致。
> 3. 每完成一节（第 8 节「实施顺序」中的一个 Step）就执行对应的验证动作，**不要攒到最后一次性验证**。
>
> 本文档不描述业务语义（那是 `DELIVERY_HANDOFF.md` 的职责），只描述**结构与不变量**。

---

## 0. ⚠️ 基座已共享化（2026-10-23 起，先读这节再读第 3、4 节）

第二个功能「视频遮挡检测 `videoOcclusion`」落地时，把仓门那一版的**按功能装配**改成了**按数据库共享**。
本文档其余部分写的还是共享化之前的标识名，照抄会把数据源复制出第二份。

| 项 | 共享化前（本文正文里的写法） | 现在的事实 |
|---|---|---|
| 数据源配置类 | `DoorAnomalyDataSourceConfiguration` | `AiVisionDataSourceConfiguration`（**一个进程内单例，所有落 `smart_storage_ai_extension` 的功能共用**） |
| MyBatis 装配 | `DoorAnomalyMybatisConfiguration` | `AiVisionMybatisConfiguration` + `AiVisionMainMybatisConfiguration` |
| 配置前缀 / yml | `door-anomaly.*` / `application-door-anomaly.yml` | `ai-vision.*` / `application-ai-vision.yml` |
| 环境变量 | `AI_DOOR_DB_*` | `AI_VISION_DB_{ENABLED,HOST,PORT,USER,PASSWORD}` |
| 权限属性 / Checker | `DoorAnomalyPermissionProperties` / `@Component("doorAnomalyPermission")` | `AiVisionPermissionProperties` / `AiVisionPermissionChecker.has(feature, operation)` |
| 数据范围解析 | `DoorAnomalyScopeResolver` | `AiVisionScopeResolver` |
| 媒体网关 | `service/impl/doorAnomaly/media/` | `service/impl/aivision/media/` |
| Mapper 包与 XML 目录 | `mapper/doorAnomaly/` | `mapper/aivision/{feature}/`、`resources/mapper/aivision/{feature}/` |

**第三个及以后的功能不要再复制这些**（它们已经是共享的）：
数据源配置类、MyBatis 两套装配、主库排除目录、`PermissionProperties`、`PermissionChecker`、
`ScopeResolver`、`MediaGateway` 及其 Mock/S3 实现、`usePolling` / `usePollingHost` /
`useRequestRaceGuard` / `displayStatus.ts`（前端在 `src/pages/aiVision/shared/`）。

**每个新功能仍然要新写**：`{feature}` 包下的 Controller/Service/Repository（真库 + InMemory 双实现）/
实体 / DTO / Mapper+XML / `Constant` / `StatusEnum`、算法网关接缝与 Mock 适配器与其任务表、
前端 `pages/aiVision/{feature}/` 两页与组件、`api/{feature}.ts`、`mock/{feature}/`、
路由节点、`application-ai-vision.yml` 里 `permission.rights.{feature}` 一段、
以及本库的建表/种子/回滚 SQL。

**权限键新增即改配置**：`AI_VISION_PERMISSION_ENABLED=true` 时，某功能某操作没配 ID 会
**fail-closed 直接拒绝**，不会退化成「登录即放行」——所以新功能上线前必须先把
`AI_{FEATURE}_RIGHT_{VIEW,EXPORT,PROCESS}` 三个 ID 落进主库并注入环境变量，
否则页面全 403。参照 `service/docs/door-anomaly/sql/05_rbac_provision.sql` 为主库写脚本，
但**主库默认禁止写入**，脚本交付后由用户执行。

第二个功能的完整落地记录见
[AI_HANDOFF_VIDEO_OCCLUSION.md](file:///D:/liangku/AI_HANDOFF_VIDEO_OCCLUSION.md) 与
`service/docs/video-occlusion/sql/`，它比本文更适合当「复制模板」——
因为它已经是共享基座之上的产物。

---

---

## 目录

0. [⚠️ 基座已共享化（先读）](#0-⚠️-基座已共享化2026-10-23-起先读这节再读第-34-节)
1. [基准坐标与提交位置](#1-基准坐标与提交位置)
2. [基准验证：动手前先跑](#2-基准验证动手前先跑)
3. [改名映射表](#3-改名映射表)
4. [完整文件清单](#4-完整文件清单)
5. [分层调用链](#5-分层调用链)
6. [硬约束清单（47 条，不得违反）](#6-硬约束清单47-条不得违反)
7. [关键代码骨架](#7-关键代码骨架)
8. [实施顺序](#8-实施顺序)
9. [验证门禁](#9-验证门禁)
10. [提交与推送规范](#10-提交与推送规范)
11. [故障速查表](#11-故障速查表)
12. [新功能需求收集清单](#12-新功能需求收集清单)

---

## 1. 基准坐标与提交位置

| 项 | 值 |
|---|---|
| 工作区 | `D:\liangku`（**根目录不是 git 仓库**） |
| 前端仓库 | `D:\liangku\manage`，分支 `feature/ys`，提交 `339464f` |
| 后端仓库 | `D:\liangku\service`，分支 `feature/ys`，提交 `3868996` |
| 后端规模 | 55 个文件，+7307 行 |
| 前端规模 | 28 个文件，+6085 行 |
| 交付文档 | [service/docs/door-anomaly/DELIVERY_HANDOFF.md](file:///D:/liangku/service/docs/door-anomaly/DELIVERY_HANDOFF.md) |
| SQL 脚本 | `service/docs/door-anomaly/sql/`（01 建表 / 02 回滚 / 04 种子 / 05 RBAC / 06 ID 对齐） |
| 目标数据库 | `smart_storage_ai_extension`（**唯一允许写入的库**） |
| 主数据库 | `smart-storage-site`（**只读，通过主库 Mapper 访问，禁止联表、禁止 DDL/DML**） |
| 后端端口 | `42000`，context-path `/smart-storage-admin` |
| 前端端口 | `3002`，代理前缀 `/smart-storage-admin` → `http://127.0.0.1:42000` |

**基准源文件绝对路径前缀：**
```
后端  D:\liangku\service\smart-storage-admin\src\main\java\com\chinaunicom\smartstorage\admin\
      D:\liangku\service\smart-storage-admin\src\main\resources\
前端  D:\liangku\manage\src\
      D:\liangku\manage\mock\
```

---

## 2. 基准验证：动手前先跑

在开始复制之前，执行以下命令确认基准完好。任何一条不符，先停下来报告，不要继续。

```bash
# 2.1 后端提交存在
cd /d/liangku/service && git log --oneline -1 feature/ys

# 2.2 前端提交存在
cd /d/liangku/manage && git log --oneline -1 feature/ys

# 2.3 后端编译通过
cd /d/liangku/service && mvn compile -q

# 2.4 前端新功能路径 0 类型错误（全仓基线 391 个历史错误，见第 9 节）
cd /d/liangku/manage && npx vue-tsc --noEmit 2>&1 \
  | grep -c "src/pages/aiVision\|src/api/doorAnomaly"
# 期望输出：0

# 2.5 数据范围块数量（本文档第 6.6 条）
cd /d/liangku/service && grep -c "AND 1 = 0" \
  smart-storage-admin/src/main/resources/mapper/doorAnomaly/AiAnomalyEventMapper.xml
# 期望输出：6（5 处真实 SQL + 1 处文件头注释）
```

> ⚠️ **已知源文档缺陷**：`AiAnomalyEventMapper.xml` 第 16–17 行的头注释写「仓库范围条件共出现 **4 处**」，
> 实际是 **5 处**（第 144、184、208、266、294 行）。
> 复制时**以代码为准，不要相信注释里的计数**。

---

## 3. 改名映射表

复制功能时，全仓替换以下 6 个标识。它们必须**完全一致地**贯穿所有层，任何一处漏改都会导致运行期失败。

| # | 标识 | 基准值（仓门异动） | 新功能填写 | 出现位置 |
|---|---|---|---|---|
| 1 | Java 包名段（小驼峰） | `doorAnomaly` | ______ | `controller/`、`service/inter/`、`service/impl/`、`mapper/`、`dto/`、`entity/` 下的子包 |
| 2 | Java 类名前缀（大驼峰） | `DoorAnomaly` | ______ | 所有 `{X}Controller` / `{X}Service` / `{X}Repository` / `{X}Constant` / `{X}Config` / `{X}StatusEnum` 等 |
| 3 | Spring 配置前缀 | `door-anomaly` | ______ | `@ConditionalOnProperty(prefix=...)`、`@ConfigurationProperties(prefix=...)`、yml 根节点 |
| 4 | 权限 Checker 的 Bean 名 | `doorAnomalyPermission` | ______ | `@Component("...")` 与所有 `@PreAuthorize("@....has(...)")` — **两处必须字面相同** |
| 5 | 导出模板 key | `doorAnomalyEvent` | ______ | `{X}Constant.EXPORT_BIZ_TYPE` 与 `ExcelExportConfig.config.put(...)` — **两处必须字面相同** |
| 6 | 前端路由与页面目录 | `/aiVision/doorAnomaly` | ______ | `src/router/modules/{module}.ts` 的 `path`、`src/pages/{module}/{feature}/` |

**另外这些也要改：**

| 项 | 基准值 | 说明 |
|---|---|---|
| 环境变量前缀 | `AI_DOOR_DB_*` / `AI_DOOR_PERMISSION_ENABLED` / `AI_DOOR_RIGHT_*` | 见 `application-door-anomaly.yml` |
| HikariCP 池名 | `DoorAnomalyHikariPool` | 只影响日志，但要唯一 |
| 事务管理器 Bean 名 | `doorAnomalyTransactionManager` | `@Transactional(transactionManager=...)` 与 `@Bean(name=...)` **两处字面相同** |
| SqlSessionFactory / Template Bean 名 | `doorAnomalySqlSessionFactory` / `doorAnomalySqlSessionTemplate` | `@MapperScan(sqlSessionTemplateRef=...)` 三处联动 |
| 主库要排除的 mapper 目录 | `/mapper/doorAnomaly/` | `MainMybatisConfiguration.FEATURE_MAPPER_DIR`，见第 6.2 条 |
| 独立库 mapper XML 目录 | `resources/mapper/{feature}/` | 与上面配对 |
| 日志前缀 | `[DoorAnomaly]` / `[仓门异动]` | 便于 grep |
| 表名前缀 | `ai_` | 见第 6.31 条 |

---

## 4. 完整文件清单

### 4.1 后端（55 个文件）

**配置与鉴权（5 个，★ = 不可省略、不可自行简化）**

| 文件 | 职责 |
|---|---|
| ★ `config/{X}DataSourceConfiguration.java` | 独立数据源。**进程内单例，不注册为 Bean**。含库名白名单硬校验、密码缺失报错、`@PreDestroy` 关池 |
| ★ `config/{X}MybatisConfiguration.java` | 独立 `SqlSessionFactory` + `SqlSessionTemplate` + `@MapperScan`，只扫本功能包与 XML 目录 |
| ★ `config/MainMybatisConfiguration.java` | **主库装配显式接管**。因为本功能注册了自己的 Factory，主库自动装配会整体退让 |
| `config/{X}PermissionProperties.java` | `@ConfigurationProperties`：`enabled` 开关 + `Map<操作键, 数字权限ID>` |
| `component/{X}PermissionChecker.java` | `@Component("{x}Permission")`，`has(String operation)`，fail-closed |

**契约与常量（2 个）**

| 文件 | 职责 |
|---|---|
| `constant/{X}Constant.java` | 权限操作键、URL 有效期、导出上限、导出模板 key、最近事件条数上下限、审计固定文案 |
| `enumbean/{X}StatusEnum.java` | 全部状态枚举 + 每个枚举的 `isProcessable()` 之类的行为方法 |

（数据范围对象 `{X}DataScope.java` 在 `dto/{feature}/` 下，见下面的 DTO 清单）

**Controller（1 个，8 个端点）**

`controller/{feature}/{X}Controller.java`

| 端点 | 权限键 | 说明 |
|---|---|---|
| `POST /listDoorStatus` | VIEW | 无请求体 |
| `POST /getStreamInfo` | VIEW | 请求体缺省时返回业务错误，不 NPE |
| `POST /listRecentEvent` | VIEW | `limit` 缺省取常量 |
| `POST /listEventByCondition` | VIEW | `@QueryByPage(returnClass=...)`，返回类型写 `Object` |
| `POST /getEventDetail/{eventId}` | VIEW | `@PathVariable` + `@NotBlank` |
| `POST /markEventViewed/{eventId}` | VIEW | `@OptLog`，只有 eventId 一个参数 |
| `POST /processEvent/{eventId}` | PROCESS | 保留但前端已不调用；仍受独立权限保护 |
| `POST /exportEvent` | EXPORT | `void` 返回，直接写 `HttpServletResponse` |

**Service 层（5 接口 + 7 实现）**

| 文件 | 职责 |
|---|---|
| `service/inter/{feature}/{X}Service.java` | 业务接口 |
| `service/inter/{feature}/{X}Repository.java` | ★ 数据访问接口。**写方法签名必须同时含 scope 与 version** |
| `service/inter/{feature}/algorithm/AlgorithmGateway.java` | 算法网关接口 |
| `service/inter/{feature}/algorithm/MockAlgorithmProperties.java` | 算法 Mock 配置 |
| `service/inter/{feature}/media/MediaGateway.java` | 媒体网关接口（含 `PlaybackRequest` / `PlaybackGrant` / `EvidenceGrant` record） |
| `service/impl/{feature}/{X}ServiceImpl.java` | 业务实现。★ **不加 `@Transactional`** |
| ★ `service/impl/{feature}/{X}ScopeResolver.java` | 数据范围解析：登录态 → 主库组织树 → 授权仓库集合 + 60 秒缓存 + PageHelper 保护 |
| `service/impl/{feature}/MyBatis{X}Repository.java` | ★ 真实库实现，**事务声明在这里** |
| `service/impl/{feature}/InMemory{X}Repository.java` | 内存兜底实现，`enabled=false` 时生效 |
| `service/impl/{feature}/algorithm/MockAlgorithmGateway.java` | 算法网关 Mock 实现 |
| `service/impl/{feature}/media/MockMediaGateway.java` | 媒体网关 Mock 实现（签发短时地址） |
| `service/impl/{feature}/media/S3MediaGateway.java` | 媒体网关 S3 实现 |

**Mapper 层（3 接口 + 3 XML）**

```
mapper/{feature}/{X}EventMapper.java        + resources/mapper/{feature}/{X}EventMapper.xml
mapper/{feature}/{X}EvidenceMapper.java     + resources/mapper/{feature}/{X}EvidenceMapper.xml
mapper/{feature}/{X}ProcessRecordMapper.java+ resources/mapper/{feature}/{X}ProcessRecordMapper.xml
```

主 Mapper 方法（全部 `@Param` 标注）：
`listDoorStatus` / `listRecentEvent` / `listEventByCondition` / `listEventForExport` /
`getEventSummaryInScope` / `getEventDetailInScope` / `updateProcessWithVersion`

**Entity（6 个，对应 6 张表）**

```
entity/{feature}/{X}DoorConfig.java          → ai_door_config
entity/{feature}/{X}CameraMapping.java       → ai_camera_mapping
entity/{feature}/{X}AlgorithmTask.java       → ai_algorithm_task
entity/{feature}/{X}AnomalyEvent.java        → ai_anomaly_event      主事件表
entity/{feature}/{X}EventEvidence.java       → ai_event_evidence
entity/{feature}/{X}EventProcessRecord.java  → ai_event_process_record
```

统一写法：`@Data` + `@TableName("...")` + `implements Serializable` + `serialVersionUID` +
`@TableId(type = IdType.INPUT)`（**不用自增，ID 由雪花生成**）。

**DTO（14 个）**

```
dto/{feature}/{X}DataScope.java          ★ 数据范围（scope 字段在查询 DTO 里标 @JsonIgnore）
dto/{feature}/{X}DoorStatusRespDTO.java   仓门状态响应
dto/{feature}/{X}StreamInfoReqDTO.java    播放请求
dto/{feature}/{X}StreamInfoRespDTO.java   播放响应（canPlay / playableUrl / expiresAt / unavailableReason）
dto/{feature}/{X}RecentEventReqDTO.java   最近事件请求（limit）
dto/{feature}/{X}EventQueryReqDTO.java    分页查询条件（含 scopeUnrestricted / scopeWarehouseIds）
dto/{feature}/{X}EventDetailRespDTO.java  详情（extends 列表字段 + 算法字段 + evidences + processRecords）
dto/{feature}/{X}EventEvidenceRespDTO.java
dto/{feature}/{X}EventProcessReqDTO.java  处理请求（含 eventVersion，不含处理人）
dto/{feature}/{X}ProcessRecordRespDTO.java
dto/{feature}/{X}EventExportReqDTO.java   导出请求（source + 筛选条件 + limit）
dto/{feature}/algorithm/AlgorithmTaskInputDTO.java
dto/{feature}/algorithm/AlgorithmTaskOutputDTO.java
```

**配置与交付**

```
resources/application-{feature}.yml        ★ 独立数据源 + 权限配置，全部走环境变量
resources/mapper/{feature}/*.xml
docs/{feature}/DELIVERY_HANDOFF.md
docs/{feature}/sql/01_create_{feature}_tables.sql     ★ 建表（含强制门禁）
docs/{feature}/sql/02_rollback_{feature}_tables.sql
docs/{feature}/sql/04_seed_test_data.sql
docs/{feature}/sql/05_rbac_provision.sql              【未执行】主库写权限行/菜单行
docs/{feature}/sql/06_warehouse_id_alignment.sql      【未执行】仓库 ID 对齐
```

**修改的既有文件（3 个）**

| 文件 | 改动 |
|---|---|
| `resources/application.yml` | 加 `spring.config.import: optional:classpath:application-{feature}.yml` |
| `config/ExcelExportConfig.java` | `config.put("{x}Event", new ExportConfig(标题, LinkedHashMap<中文列名, DTO字段>))` |
| ~~`pom.xml`~~ | ❌ **不提交**，见第 10 节 |

### 4.2 前端（28 个文件）

```
src/api/{feature}.ts                     ★ 唯一后端出口 + 字段归一化 + 分页提取 + 导出 blob
src/api/model/{feature}Model.ts          请求模型 / PageResult<T>

src/pages/{module}/{feature}/
  index.vue                              监测首页（左面板 + 右视频 + 底部最近事件 + 详情弹窗）
  history.vue                            历史记录页（筛选 + 分页表格 + 导出 + 详情弹窗）
  components/
    DoorStatusPanel.vue                  仓门状态网格（3 列 + 颜色语义 + 呼吸灯 + 选中角标）
    VideoMonitor.vue                     视频区（原生 <video>，8 种可见状态）
    RecentEventTable.vue                 首页最近事件表
    HistoryEventTable.vue                历史分页表（v-model:page-no / page-size）
    EventDetailDialog.vue                详情弹窗（含 t-image-viewer）
  composables/
    use{X}State.ts                       ★ 状态编排：数据 + loading + error + 竞态 + 轮询装配
    use{X}Polling.ts                     ★ 轮询器：running 跳过 / 指数退避 / canSchedule 双校验
    use{X}PollingHost.ts                 ★ 生命周期宿主：单一启动入口 + visibilitychange
    useRequestRaceGuard.ts               ★ 竞态守卫：begin / isCurrent / invalidate
  constants/index.ts                     ★ 枚举值集 + 中文标签 + 颜色 + TDesign theme + 轮询间隔 + 分页默认
  types/index.ts                         ★ 数据契约（严格对齐后端 DTO 字段名）
  utils/displayStatus.ts                 ★ computeDisplayStatus / pickDefaultDoor / shouldAutoPlay

src/router/modules/{module}.ts           ★ 路由模块（icon 必须是字符串名）

mock/
  auth/index.ts                          ★ 登录 Mock（Mock 模式下随便填账号密码即可进入）
  {feature}/
    index.ts                             Mock 端点（真分页 / 全状态覆盖 / 导出真 xlsx 二进制）
    fixtures.ts                          ★ 确定性伪随机 + 时间基准（见第 6.40 条）
    xlsx.ts                              手写 OOXML 生成器
    README.md                            Mock 开关说明
```

**修改的既有文件（5 个）**

| 文件 | 改动 |
|---|---|
| `.env` `.env.development` `.env.site` `.env.test` | 加 `VITE_USE_MOCK = false` + 注释 |
| `.env.mock` | **新增**，`VITE_USE_MOCK = true`，供 `npm run dev:mock` |
| `vite.config.ts` | `viteMockServe({ enable: VITE_USE_MOCK === 'true' })` |

---

## 5. 分层调用链

```
【读路径】
浏览器
  → src/api/{feature}.ts                      唯一出口，字段归一化
    → vite proxy /smart-storage-admin → :42000
      → {X}Controller                          参数校验 + @PreAuthorize + callBackResult 包装
        → {X}ServiceImpl.resolveDataScope()    ┐
          → {X}ScopeResolver.resolve()         │ 主库：JwtTokenComponent.getUser()
                                                 │       → OrganizationMapper.listOrganizationChildById(orgId)
                                                 │         （递归 CTE，含自身节点）
                                                 │       → 过滤 type=2 → warehouseIds
                                                 ┘  60 秒 TimedCache
        → {X}Repository.listXxx(scope, ...)
          → MyBatis{X}Repository               把 scope 摊平成 (unrestricted, warehouseIds)
            → {X}Mapper                        全部 @Param
              → XML                             数据范围三态下推到 SQL

【写路径】
{X}Controller → {X}ServiceImpl
  → requireAuthenticatedScope()               未认证直接抛 CustomException
  → {X}Repository.processEvent(...)           ★ @Transactional("{x}TransactionManager")
      ├─ getEventSummaryInScope               范围校验（越权/不存在 → 0 行）
      ├─ updateProcessWithVersion             乐观锁，affected=0 立即 return 0（不写审计）
      └─ appendProcessRecord                  追加审计，同事务
```

**关键分层原则：**
- Controller **只做**参数校验 + 统一响应包装，不写业务判断。
- ServiceImpl 负责业务组合、状态判定、错误文案，**不声明事务**。
- Repository 是**唯一**声明事务的地方，且**必须**按名指定本功能的事务管理器。
- 数据范围**只能在 SQL 里生效**。取回整行后在 Java 判断是错的（详情列可能没 SELECT 出 `org_id`/`region_id`，比较会静默空转）。

---

## 6. 硬约束清单（47 条，不得违反）

> 每条格式：**约束** → *为什么* → *违反后果*

### 后端 · 装配与数据源

**6.1 ★ 独立数据源不得注册为 Spring Bean**
用 `synchronized obtainDataSource()` 返回进程内单例，只在 `{X}MybatisConfiguration` 里取用。
*为什么*：容器里一旦出现第二个 `DataSource` Bean，`MybatisPlusAutoConfiguration` 的
`@ConditionalOnSingleCandidate(DataSource.class)` 就会退让。
*违反后果*：主库 `SqlSessionFactory` 不再创建，全部主库 Mapper 报
`Invalid bound statement (not found)`，**应用起不来**。给主库 DataSource 标 `@Primary` 也救不回来
（MyBatis-Plus 3.5.15 实测无效）。

**6.2 ★ `MainMybatisConfiguration` 必须存在且必须做三件事**
*为什么*：本功能注册了自己的 `SqlSessionFactory`/`SqlSessionTemplate`，而 MP 的自动装配这两个 Bean 都带
`@ConditionalOnMissingBean` → **主库自动装配整体退让**。
必须做：
1. `@Bean("sqlSessionFactory") @Primary` + `@Bean("sqlSessionTemplate") @Primary`；
2. **应用容器内全部 `SqlSessionFactoryBeanCustomizer`** —— `mybatis-plus-join` 靠它注入联表能力，漏掉则所有 `MPJBaseMapper` 失效；
3. **解析主库 mapper XML 时剔除本功能目录**（`FEATURE_MAPPER_DIR`）—— 主库配置是 `mapper` 递归通配，会把本功能 XML 一起吞进来，造成语句重复注册。

另外必须 `coreConfiguration.applyTo(configuration)` —— `MybatisPlusProperties.CoreConfiguration` 只是 POJO，不 apply 就等于所有 `mybatis-plus.configuration.*` 设置全部丢失。

*违反后果*：启动即 `Invalid bound statement`；或 MPJ 联表全部报无此语句；或 `Mapped Statements collection already contains value for ...`。

**6.3 ★ 三个 config 类共用同一个 `@ConditionalOnProperty`**
`{X}DataSourceConfiguration` / `{X}MybatisConfiguration` / `MainMybatisConfiguration` 都必须是
`prefix="{feature}.datasource", name="enabled", havingValue="true", matchIfMissing=false`。
*为什么*：同生同灭。`enabled=false`（默认）时三个类**完全不参与装配**，主库仍走 MP 自动装配，其余 30+ 模块零变化。
*违反后果*：`MainMybatisConfiguration` 单独生效时会覆盖主库自动装配但拿不到本功能数据源，行为不可预期。

**6.4 ★ 目标库名写死为常量，不接受环境变量**
`private static final String ALLOWED_DATABASE = "smart_storage_ai_extension";`，
连接 URL 直接使用此常量，不提供可覆盖的数据库名配置。
*为什么*：连接目标只有一个来源，避免重复配置不一致，也防止误连到主库。
*违反后果*：若 URL 改为使用外部数据库名，配置笔误可能连接到其他库。

**6.5 密码/账号缺失必须显式报错，且报错文案要指出环境变量名**
```java
if (username == null || username.isBlank() || password == null || password.isBlank()) {
    throw new IllegalStateException("...请设置环境变量 AI_DOOR_DB_USER 与 AI_DOOR_DB_PASSWORD 后再启动。");
}
```
*为什么*：静默回退到内存实现会让人以为联调成功了。
*违反后果*：花几小时排查"为什么数据全是 Mock"。

### 后端 · 数据范围与事务

**6.6 ★ 数据范围三态在 XML 中形态必须完全一致，且要数清出现次数**
```xml
<if test="!unrestricted">
    <choose>
        <when test="warehouseIds != null and warehouseIds.size() > 0">
            AND <别名>.warehouse_id IN
            <foreach collection="warehouseIds" item="wid" open="(" close=")" separator=",">#{wid}</foreach>
        </when>
        <otherwise>AND 1 = 0</otherwise>
    </choose>
</if>
```
三态：非全域+有仓库 → `IN(...)`；非全域+无仓库 → `AND 1 = 0`；全域 → 不加条件。
基准文件里有 **5 处**（`EventFilters`、`listDoorStatus`、`listRecentEvent`、`getEventSummaryInScope`、
`getEventDetailInScope`）。新功能每加一个查询就要加一处。
*为什么*：把「取不到授权维度」当成「不限制」= 放弃越权防护。这正是原报告 ⑥-3 记录的缺口。
*违反后果*：**跨组织越权读取**，且不会有任何报错——静默泄露。
⚠️ 基准 XML 第 16–17 行注释写"4 处"，是错的，以 `grep -c "AND 1 = 0"` 为准。

**6.7 ★ 事务必须声明在 Repository 层，Service 上刻意不加 `@Transactional`**
```java
@Transactional(transactionManager = "{x}TransactionManager", rollbackFor = Exception.class)
public int processEvent(...)   // 在 MyBatis{X}Repository 里
```
*为什么*：① 默认事务管理器绑**主库**，本功能读写**独立库**，跨事务管理器时内层不会加入外层事务 ——
Service 上一个 `@Transactional` 看似修复了原子性，实际仍是两次独立提交，还每次白占一个主库连接
（`leak-detection-threshold=60s`）。
② 也不能在 Service 上按名指定 `{x}TransactionManager` —— 该 Bean 只在 `enabled=true` 时存在，
内存兜底模式下首次调用即抛 `NoSuchBeanDefinitionException`。
*违反后果*：「状态已变但无审计记录」的脏数据；或内存模式下 500。

**6.8 ★ `{X}ScopeResolver` 必须保护 PageHelper 的 ThreadLocal**
```java
private List<Organization> queryOrgSubtreeUnpaged(String orgId) {
    final Page<?> pending = PageMethod.getLocalPage();
    if (pending == null) return mapper.listOrganizationChildById(orgId);
    final int pageNum = pending.getPageNum(), pageSize = pending.getPageSize();
    final boolean count = pending.isCount();
    PageMethod.clearPage();
    try { return mapper.listOrganizationChildById(orgId); }
    finally { PageMethod.startPage(pageNum, pageSize, count); }
}
```
*为什么*：`@QueryByPage` 切面在 `startPage()` **之后**才 `proceed()` 进业务，ScopeResolver 恰好处在两者之间。
*违反后果*：两个故障叠加 —— ① 组织树查询被当待分页查询按 `pageSize`（常为 10）截断，
**授权仓库集合被削短**，且残缺结果缓存 60 秒；② 真正的事件查询此时已无分页上下文，
**不带 LIMIT 全量返回**，分页总数退化为列表长度。
表现为"偶发看不到部分仓库、翻页无效"，且只在缓存未命中时出现，极易误判成前端问题。

**6.9 读路径失败即返回空集，写路径失败即抛异常**
`resolve()` 解析不出登录态 → `DoorAnomalyDataScope.denyAll()`（读→空集）；
`requireAuthenticatedScope()` 未认证 → `throw new CustomException("未登录或登录已失效，请重新登录后再操作")`。
*为什么*：绝不因解析失败而放宽为全量可见；也绝不回退成 `anonymous` 处理人。
*违反后果*：越权，或审计里出现 `anonymous` 处理人。

**6.10 「不存在」与「越权」不得区分**
详情查不到时统一返回 `null` / 抛「事件不存在或无权查看」。
*为什么*：区分就等于告诉攻击者"这个 ID 存在但你不配看"。
*违反后果*：对象存在性泄露。

**6.11 全域判定两个信号任一命中即可**
`userType == 1`（超级管理员）**或** `regionId.equals("0")`（区划码 0 = 全域）。
*为什么*：此前该判定被硬编码为 `false`，导致合法的全域管理员也拿不到数据。

**6.12 授权缓存 60 秒，且异常路径也要写缓存**
`TimedCache` 复用既有 hutool，不新增依赖。组织树查询抛异常时 `grantCache.put(orgId, List.of())` 后返回空。
*为什么*：前端 10–15 秒轮询，每次跑递归 CTE 会给主库无谓压力。授权变更最多 60 秒生效，可接受。

### 后端 · SQL 与 MyBatis

**6.13 ★ Mapper 单参数方法必须加 `@Param`**
```java
List<...> listEventByCondition(@Param("query") EventQueryReqDTO query);
```
*为什么*：XML 内以 `query.xxx` 引用属性。单参数不加注解时 MyBatis 把实体直接作为 OGNL 根对象，
`query.eventNo` 解析不到。
*违反后果*：整个 `<where>` 退化，筛选条件全部失效，且**不报错**。

**6.14 ★ BIGINT 列只能用 `Long` 绑定**
`warehouseId` / `doorId` 在 DTO 里必须是 `Long`，XML 里只判 `!= null` 不判 `!= ''`。
`{X}DataScope.warehouseIds` 必须是 `List<Long>`。
*为什么*：PostgreSQL 会报 `operator does not exist: bigint = character varying`。
*违反后果*：SQL 运行期异常 → 500。

**6.15 ★ 时间列在 SQL 侧格式化，排序用裸列**
```sql
to_char(e.occurred_at, 'YYYY-MM-DD HH24:MI:SS') AS occurred_at
...
ORDER BY e.occurred_at DESC   -- 不是 ORDER BY occurred_at（格式化后的别名）
```
*为什么*：PostgreSQL TIMESTAMP 经 MyBatis 转 String 会带 6 位小数（实测 `2026-10-19 15:17:21.195589`），
前端原样显示难看，且与 Mock 输出不同形，联调期看不出契约不一致。
*违反后果*：导出/列表出现微秒尾巴；或排序按字符串比较导致跨月错序。

**6.16 乐观锁 UPDATE 必须做四件事**
```sql
UPDATE {主表}
SET process_status = #{processStatus}, processed_by = #{processedBy},
    processed_at = #{updatedAt}, process_opinion = #{processOpinion},
    event_version = event_version + 1, updated_by = #{processedBy}, updated_at = #{updatedAt}
WHERE id = #{eventId}
  AND event_version = #{eventVersion}
  AND process_status NOT IN ('VERIFIED_TRUE','VERIFIED_FALSE','CLOSED')
```
① `WHERE event_version = ?`；② **不触碰** `algorithm_status` / `algorithm_result`；
③ `event_version = event_version + 1`；④ 终态 `NOT IN` 保护。
*为什么*：禁止用覆盖算法结果的方式完成人工处理（业务红线）。
*违反后果*：人工结论污染算法结论，且无法回滚。

**6.17 `affected != 1` 一律按并发冲突处理，且绝不写审计**
```java
if (affected == 0) return 0;   // Repository 里，直接 return，不执行 append
```
Service 里 `if (affected != 1) throw new CustomException("事件已被其他用户处理，请刷新后重试");`
*为什么*：状态没变就写审计，会让审计与状态自相矛盾。

**6.18 审计记录只追加，不覆盖不删除**
`appendProcessRecord(...)` 用新雪花 ID 插入，永不 UPDATE 历史行。

**6.19 ★ UPDATE 语句里的参数名必须与 Mapper 接口声明逐字一致**
基准里曾把 `#{processedAt}` 写进 SQL，而接口只有 `updatedAt` 参数。
*违反后果*：`Parameter 'processedAt' not found`，**写路径从来没有成功过一次**，
而且只在真正提交时才暴露。
→ 复制后**必须真实跑一次写路径**，不能只靠编译和读接口验证。

### 后端 · 权限

**6.20 ★ authority 是 `base_rights_info.id` 的数字字符串，不是权限码**
本项目 `base_rights_info` 只有 `id` 与 `name` 两列，**没有 `code`/`perms` 列**。
登录时 `TokenUserDetailsServiceImpl` 装配的是 `new SimpleGrantedAuthority(Convert.toStr(right.getId()))`。
*为什么*：所以 `hasAuthority('doorAnomaly:event:export')` 这类写法**永远不可能命中**。
*违反后果*：权限看似接入了，实际要么全放行要么全拒绝。

**6.21 操作键 → 数字 ID 的映射必须是配置项，不能写死常量**
*为什么*：权限 ID 由目标环境主库自增分配，测试环境与生产环境数值不同，代码不应预知。

**6.22 fail-closed：`enabled=true` 但某操作没配 ID → 拒绝**
```java
if (!StringUtils.hasText(rightId)) { log.warn("...按拒绝处理"); return false; }
```
*违反后果*：悄悄退化成「只要登录就放行」，且毫无提示。

**6.23 `enabled` 默认必须为 `false`**
*为什么*：权限行尚未入库时（当前部署形态即如此），默认 true 会让所有人被拒。
`enabled=false` 时行为与接入权限前完全一致（只要求已登录 + 数据范围下推）。

**6.24 写接口即使前端不再调用，也必须保留权限保护**
基准里 `processEvent` 前端已无按钮，但接口保留且受 `PROCESS` 权限保护。
*为什么*：避免留下一个无权限约束的写接口。

### 后端 · 其他

**6.25 ★ 导出模板 key 必须用常量，不能用 `source`**
`{X}Constant.EXPORT_BIZ_TYPE` 才是 `ExcelExportConfig.config` 的 key；
`exportReqDTO.getSource()` 是**发起导出的页面**（`HISTORY` / `RECENT`）。
*违反后果*：`config.get("HISTORY")` 返回 null → 立刻 NPE，**导出功能没有任何一次成功的可能**。

**6.26 媒体只接受受控协议前缀**
```java
private boolean isControlledReference(String ref) {
    if (ref == null || ref.isBlank()) return false;
    String lower = ref.toLowerCase();
    if (lower.startsWith("http://") || lower.startsWith("https://")) return false;
    if (ref.contains("..") || ref.startsWith("/") || ref.contains("\\")) return false;
    return ref.contains("://");
}
```
*为什么*：防止把任意外部地址或本地路径喂给签名逻辑。

**6.27 日志脱敏：只打印数量与标识名，不打印值**
不打印：token、SQL 语句、播放地址、组织 ID、仓库 ID、用户 ID。
只打印：`count=`、`operation=`、`requiredRightId=`（配置值非用户数据）、`doorId=`（业务标识可打）、`canPlay=`。

**6.28 请求体缺省必须给可理解的业务错误，不能 NPE**
```java
if (req == null || req.getXxx() == null || req.getXxx().isBlank())
    return actionResultService.callBackResultFail("xxx不能为空");
```
`@RequestBody(required = false)` 时尤其重要。

**6.29 时间/ID 字符串解析失败必须抛异常，不能返回 null**
```java
private LocalDateTime parseTime(String value) {
    if (value == null || value.isBlank()) return null;      // 空 = 不加筛选，合法
    try { return LocalDateTime.parse(value.replace(' ', 'T')); }
    catch (Exception e) { throw new CustomException("时间格式不正确，应为 yyyy-MM-dd HH:mm:ss"); }
}
```
*为什么*：格式错时返回 null 会让时间条件整体消失 —— 用户以为导出的是筛过的 7 天，
实际拿到全量历史（最多 5000 条），**数据错了 yet 毫无提示**。

**6.30 存量脏数据不得导致 500**
```java
try { current = ProcessStatusEnum.valueOf(existing.getProcessStatus()); }
catch (IllegalArgumentException | NullPointerException e) {
    log.warn("...状态非法"); throw new CustomException("事件处理状态异常，请联系管理员");
}
```

**6.31 ★ 建表脚本头部强制门禁，且重复执行防护不能用模式匹配**
```sql
-- 步骤 1：SELECT current_database();  -- 必须严格等于目标库，否则停止
-- 步骤 2：显式列出全部表名，不用 LIKE
WHERE table_name IN ('ai_door_config','ai_camera_mapping', ... )  -- 逐个列
```
*为什么*：本功能 6 张表前缀不统一（`ai_door_` / `ai_camera_` / `ai_algorithm_` / `ai_anomaly_` / `ai_event_`）。
用 `LIKE 'ai_door%'` 只命中 1 张表，导致其余 5 张表的重复执行防护**完全失效**。

**6.32 `spring.config.import` 必须带 `optional:` 前缀**
```yaml
spring:
  config:
    import: optional:classpath:application-{feature}.yml
```
*为什么*：该文件缺失时应用仍要能正常启动，不能阻断其余功能。

### 前端（编号接续，同样不得违反）

**6.33 ★ `src/api/{feature}.ts` 是访问后端的唯一出口**
组件内不得散落 URL 字符串与原始字段转换。
统一 `pick(raw, camelKey, snakeKey)` 双命名兼容 + `?? 默认值` 兜底。
分页固定 `result.data` / `result.total`（`extractPage`）。

**6.34 导出必须三个选项同时配**
```ts
request.post({ url, data, responseType: 'blob' },
             { isReturnNativeResponse: true, isTransformResponse: false })
```

**6.35 ★ 每条异步路径都要竞态守卫**
`const token = guard.begin()` → 响应后 `if (!guard.isCurrent(token)) return`。
切换实体时先 `guard.invalidate()`。
*为什么*：快速点击不同对象时，慢的旧请求会覆盖新选择。

**6.36 ★ 轮询单一启动入口**
宿主的 `onMounted` 只调 `resume()`（**不** `refreshNow()`）；首屏数据由页面自己的 `onMounted` 初始化请求负责。
*违反后果*：「首次 tick 与 onMounted 重复加载同一批数据」。

**6.37 ★ 轮询器必须做四件事**
① `running` 时跳过本次触发；② 连续失败指数退避
（`RETRY_BASE_DELAY * 2 ** (n-1)`，上限 `RETRY_MAX_DELAY`）；
③ **重调度前再次校验** `paused`/`disposed`（请求可能耗时很久，期间页面已隐藏或已卸载）；
④ `dispose()` 后不再写任何响应式状态。
`onBeforeUnmount` 里 `dispose()` 作为兜底。

**6.38 ★ 展示状态由前端计算，不依赖后端单一字符串**
`computeDisplayStatus` 优先级：
```
0. !deviceStatus || deviceStatus==='UNKNOWN'      → UNKNOWN   （「不知道」≠「已掉线」）
1. !isOnline || isHeartbeatTimeout(heartbeat, now) → OFFLINE
2. isActiveAlarm('ACTIVE')                         → ABNORMAL
3. else                                            → NORMAL
```
*违反后果*：把「不知道」误报成「已掉线」。

**6.39 ⚠️ 心跳超时阈值 90 分钟是真实故障源**
`HEARTBEAT_TIMEOUT_MINUTES = 90`。
**基准实测踩过的坑**：数据库里 13 个仓门 `device_status='ONLINE'`，但 `last_heartbeat_at` 是 3 天前的种子值，
于是 `computeDisplayStatus` 全部判为 `OFFLINE` —— 页面显示"16 个仓门 15 个离线"，
而右侧视频区却显示"摄像头 CAM-001 **在线**"（因为 `VideoMonitor` 直接看 `deviceStatus === 'ONLINE'`）。
→ **新功能若有类似"新鲜度"判定，种子数据的时间戳必须每次调用时基于 `Date.now()` 计算**（见 6.40），
并且自测时必须交叉核对两处判定是否一致。

**6.40 ★ Mock fixtures 的心跳/时间戳必须每次调用时算，不能模块初始化固定**
```ts
function heartbeatIso(offsetMinutes: number) {
  const d = new Date(Date.now() - offsetMinutes * 60_000);   // ✅ Date.now() 每次调用取
  ...
}
const BASE_TIME = Date.now();   // 事件时间可以固定（回溯 7 天窗口用）
```
*为什么*：若心跳基准在模块初始化时固定，dev server 跑 2 小时后再打开页面，
所有心跳都超过 90 分钟阈值 → **全部仓门误判为离线**，正常/异常状态永远演示不出来。
*注意区分*：**事件发生时间**用 `BASE_TIME` 固定是对的（保证默认 7 天查询窗口内始终有数据）；
**心跳时间**必须每次重算。

**6.41 ★ 路由 `meta.icon` 必须是字符串名，不能是组件对象**
```ts
icon: 'video-camera-2',        // ✅
// icon: VideoCamera2Icon,     // ❌
```
*为什么*：菜单数据会经过 store 的 JSON 持久化往返，组件对象反序列化后变成普通对象。
*违反后果*：`MenuContent` 的 `<component :is>` 报
`Component is missing template or render function`，侧边栏图标**渲染空白**。

**6.42 枚举/标签/颜色/TDesign theme 全部集中在 `constants/index.ts`**
组件内不得写 if-else 判状态。theme 映射用
`as const satisfies Record<X, string>` 保留字面量类型（否则赋给组件属性时类型不匹配）。

**6.43 TDesign 图片预览组件名是 `t-image-viewer` + `:images`**
```html
<t-image-viewer v-model:visible="previewVisible" :images="previewList" :z-index="10000" />
```
不是 `t-image-preview` / `:preview-list` —— 后者不存在，会报组件解析失败。

**6.44 单个区域失败不得阻塞其他区域**
每块独立 `xxxError` ref + `t-alert` + 重试 `t-link`。一块失败不清空其他块数据。

**6.45 可空数值不得渲染成 0**
`confidence === null` → 显示「暂无」，不是 `0%`。

**6.46 Mock 与真实库响应必须同形**
包括时间格式（后端 SQL 侧 `to_char` 已保证无微秒）、字段命名（前端 `pick` 双兼容）。
*为什么*：不同形的话，Mock 通过但联调必炸，且 Mock 阶段完全发现不了。

**6.47 Mock 由环境变量控制，不是硬编码常开**
`viteMockServe({ enable: VITE_USE_MOCK === 'true' })`；
`npm run dev` → `false`（真实后端）；`npm run dev:mock` → `.env.mock` → `true`。
**改 `VITE_USE_MOCK` 后必须重启 dev server**（它是 `loadEnv` 在配置阶段读的）。

---

## 7. 关键代码骨架

> 以下是最容易写错的部分，直接照抄结构、替换名称。
> 其余文件（DTO/Entity/普通组件）按第 4 节清单和基准文件对照复制即可。

### 7.1 `{X}DataSourceConfiguration`（骨架）

```java
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "{feature}.datasource", name = "enabled",
                       havingValue = "true", matchIfMissing = false)
public class {X}DataSourceConfiguration {

    private static final String ALLOWED_DATABASE = "smart_storage_ai_extension";  // 写死

    @Value("{feature}.datasource.host")     private String host;
    @Value("{feature}.datasource.port")     private String port;
    @Value("{feature}.datasource.username:}") private String username;
    @Value("{feature}.datasource.password:}") private String password;
    // ... hikari 各项 @Value，全部带默认值

    /** ★ 刻意不注册成 Bean，见硬约束 6.1 */
    public synchronized DataSource obtainDataSource() {
        if (this.dataSourceHolder == null) this.dataSourceHolder = createDataSource();
        return this.dataSourceHolder;
    }
    private volatile DataSource dataSourceHolder;

    private DataSource createDataSource() {
        if (username == null || username.isBlank() || password == null || password.isBlank())
            throw new IllegalStateException("...请设置环境变量 ..._DB_USER 与 ..._DB_PASSWORD 后再启动。");

        final String url = "jdbc:postgresql://" + host + ":" + port + "/" + ALLOWED_DATABASE
            + "?currentSchema=public&serverTimezone=Asia/Shanghai"
            + "&useUnicode=true&characterEncoding=utf-8"
            + "&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true"
            + "&rewriteBatchedStatements=true&useSSL=false";

        log.info("[{功能}] 创建独立数据源 database={} port={}", ALLOWED_DATABASE, port);  // 脱敏

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url); ds.setUsername(username); ds.setPassword(password);
        ds.setPoolName(poolName); ds.setMaximumPoolSize(maximumPoolSize);
        ds.setMinimumIdle(minimumIdle); ds.setMaxLifetime(maxLifetime);
        ds.setKeepaliveTime(keepaliveTime); ds.setConnectionTimeout(connectionTimeout);
        ds.setIdleTimeout(idleTimeout);
        return ds;
    }

    @Bean(name = "{x}TransactionManager")
    public PlatformTransactionManager {x}TransactionManager() {
        return new DataSourceTransactionManager(obtainDataSource());
    }

    @PreDestroy
    public void closeDataSource() {          // 不是 Bean，必须自己关
        if (this.dataSourceHolder instanceof HikariDataSource hikari) {
            hikari.close();
            log.info("[{功能}] 独立数据源连接池已关闭");
        }
    }
}
```

### 7.2 `{X}MybatisConfiguration`（骨架）

```java
@Configuration
@ConditionalOnProperty(prefix = "{feature}.datasource", name = "enabled",
                       havingValue = "true", matchIfMissing = false)
@MapperScan(basePackages = "com.chinaunicom.smartstorage.admin.mapper.{feature}",
            sqlSessionTemplateRef = "{x}SqlSessionTemplate")
public class {X}MybatisConfiguration {

    @Bean(name = "{x}SqlSessionFactory")
    public SqlSessionFactory {x}SqlSessionFactory({X}DataSourceConfiguration cfg) throws Exception {
        MybatisSqlSessionFactoryBean fb = new MybatisSqlSessionFactoryBean();
        fb.setDataSource(cfg.obtainDataSource());                    // ★ 不按 Qualifier 注入 Bean
        fb.setMapperLocations(new PathMatchingResourcePatternResolver()
            .getResources("classpath*:/mapper/{feature}/*.xml"));
        com.baomidou.mybatisplus.core.MybatisConfiguration conf =
            new com.baomidou.mybatisplus.core.MybatisConfiguration();
        conf.setMapUnderscoreToCamelCase(true);
        fb.setConfiguration(conf);
        fb.setPlugins(new MybatisPlusInterceptor());
        return fb.getObject();
    }

    @Bean(name = "{x}SqlSessionTemplate")
    public SqlSessionTemplate {x}SqlSessionTemplate(
            @Qualifier("{x}SqlSessionFactory") SqlSessionFactory f) {
        return new SqlSessionTemplate(f);
    }
    // ★ 不要再加第二个同类型事务管理器别名 Bean，见 6.2 与基准文件末尾注释
}
```

### 7.3 `MainMybatisConfiguration`（**整类照抄，只改 3 处**）

改动点：① `@ConditionalOnProperty` 的 prefix；② `FEATURE_MAPPER_DIR` 常量；③ 类名与 Javadoc。
其余逻辑（`applyTo`、`typeAliasesPackage`、`globalConfig`、`interceptors`、
`customizers` 循环、`mainMapperLocations` 剔除）**一行都不要动**，见 6.2。

### 7.4 `{X}ScopeResolver`（骨架）

```java
@Slf4j @Component
public class {X}ScopeResolver {
    private static final int ORG_TYPE_WAREHOUSE = 2;      // base_organization_info.type: 1=公司 2=仓库
    private static final int USER_TYPE_SUPER_ADMIN = 1;
    private static final String GLOBAL_REGION = "0";
    private static final long GRANT_CACHE_MILLIS = 60_000L;

    @Resource private JwtTokenComponent jwtTokenComponent;
    @Resource private OrganizationMapper organizationMapper;      // 主库 Mapper
    private final TimedCache<String, List<Long>> grantCache = CacheUtil.newTimedCache(GRANT_CACHE_MILLIS);

    public {X}DataScope resolve() {
        User user;
        try { user = jwtTokenComponent.getUser(); }
        catch (Exception e) { log.warn("[{功能}] 无法解析登录态，按无数据权限处理");
                              return {X}DataScope.denyAll(); }
        if (user == null) { log.warn("[{功能}] 登录态为空，按无数据权限处理");
                            return {X}DataScope.denyAll(); }

        {X}DataScope scope = new {X}DataScope();
        scope.setAuthenticated(true);
        scope.setUserId(user.getId());   scope.setUserName(user.getName());
        scope.setOrgId(user.getOrgId()); scope.setRegionId(user.getRegionId());
        if (isGlobalAccess(user)) { scope.setUnrestricted(true); return scope; }
        scope.setUnrestricted(false);
        scope.setWarehouseIds(resolveWarehouseIds(user.getOrgId()));
        return scope;
    }

    private boolean isGlobalAccess(User u) {
        if (u.getUserType() != null && u.getUserType() == USER_TYPE_SUPER_ADMIN) return true;
        return GLOBAL_REGION.equals(u.getRegionId());
    }

    private List<Long> resolveWarehouseIds(String orgId) {
        if (orgId == null || orgId.isBlank()) { log.warn("[{功能}] 用户未绑定组织，无仓库授权");
                                                return List.of(); }
        List<Long> cached = grantCache.get(orgId);
        if (cached != null) return cached;

        List<Long> ids = new ArrayList<>(); int unparsable = 0;
        try {
            List<Organization> subtree = queryOrgSubtreeUnpaged(orgId);   // ★ 含自身节点
            if (subtree != null) for (Organization org : subtree) {
                if (org == null || !Integer.valueOf(ORG_TYPE_WAREHOUSE).equals(org.getType())) continue;
                Long wid = toWarehouseId(org.getId());
                if (wid == null) unparsable++;
                else if (!ids.contains(wid)) ids.add(wid);
            }
        } catch (Exception e) {
            log.warn("[{功能}] 组织树解析失败，按无仓库授权处理");
            grantCache.put(orgId, List.of());       // ★ 异常也缓存，避免每次请求都重试
            return List.of();
        }
        if (unparsable > 0) log.warn("[{功能}] 仓库组织标识无法映射为数值主键，已跳过 count={}", unparsable);
        if (ids.isEmpty())  log.warn("[{功能}] 组织子树内不存在仓库节点，该用户可见数据为空");

        List<Long> immutable = List.copyOf(ids);
        grantCache.put(orgId, immutable);
        return immutable;
    }

    /** ★★ PageHelper ThreadLocal 保护，见硬约束 6.8，不可省略 */
    private List<Organization> queryOrgSubtreeUnpaged(String orgId) {
        final Page<?> pending = PageMethod.getLocalPage();
        if (pending == null) return organizationMapper.listOrganizationChildById(orgId);
        final int pageNum = pending.getPageNum();
        final int pageSize = pending.getPageSize();
        final boolean count = pending.isCount();
        PageMethod.clearPage();
        try { return organizationMapper.listOrganizationChildById(orgId); }
        finally { PageMethod.startPage(pageNum, pageSize, count); }
    }

    private Long toWarehouseId(String organizationId) {   // VARCHAR → BIGINT，见 6.14
        if (organizationId == null || organizationId.isBlank()) return null;
        try { return Long.parseLong(organizationId.trim()); }
        catch (NumberFormatException e) { return null; }
    }
}
```

### 7.5 Mapper XML 数据范围块（**照抄，改表别名**）

```xml
<!-- 见硬约束 6.6。每个查询都要带，别名跟着改（e / d / ...） -->
<if test="!unrestricted">
    <choose>
        <when test="warehouseIds != null and warehouseIds.size() > 0">
            AND e.warehouse_id IN
            <foreach collection="warehouseIds" item="wid" open="(" close=")" separator=",">
                #{wid}
            </foreach>
        </when>
        <otherwise>AND 1 = 0</otherwise>
    </choose>
</if>
```

在**查询 DTO 里承载 scope** 时（`listEventByCondition` 走 `#{query.xxx}`），
scope 字段必须 `@JsonIgnore`，否则前端可以伪造：
```java
@JsonIgnore private boolean scopeUnrestricted;
@JsonIgnore private List<Long> scopeWarehouseIds;
```

### 7.6 Controller 权限注解

```java
@PreAuthorize("@{x}Permission.has('" + {X}Constant.Permission.VIEW + "')")
```
读接口全用 `VIEW`，导出用 `EXPORT`，人工处理用 `PROCESS`。
`@OptLog(title="...", operatorType = OptLogOperatorType.UPDATE / EXPORT)` 加在写接口与导出接口上。

分页接口返回类型必须是 `Object` 并带 `@QueryByPage`：
```java
@PostMapping(path = "/listEventByCondition", produces = MediaType.APPLICATION_JSON_VALUE)
@QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.{feature}.{X}EventRespDTO")
public Object listEventByCondition(@RequestBody @Validated {X}EventQueryReqDTO req) {
    return {x}Service.listEventByCondition(req);
}
```
（`returnClass` 是**全限定名字符串**，写错不会编译报错，只会在运行期炸。）

### 7.7 前端 `api/{feature}.ts` 骨架

```ts
export const {X}_API = { LIST_XXX: '/{feature}/listXxx', ... } as const;

function pick(source: any, camelKey: string, snakeKey: string): any {
  if (!source) return undefined;
  if (source[camelKey] !== undefined && source[camelKey] !== null) return source[camelKey];
  if (source[snakeKey] !== undefined && source[snakeKey] !== null) return source[snakeKey];
  return undefined;
}

function normalizeXxx(raw: any): Xxx {
  return {
    id: String(pick(raw, 'id', 'id') ?? ''),
    status: pick(raw, 'status', 'status') ?? 'UNKNOWN',   // ★ 每个字段都要 ?? 兜底
    ...
  } as Xxx;
}

function extractPage<T>(res: any, mapper: (raw: any) => T): PageResult<T> {
  const result = res?.result ?? {};
  const list = Array.isArray(result.data) ? result.data : [];
  return { data: list.map(mapper), total: Number(result.total ?? 0) };
}

export async function listXxx(): Promise<Xxx[]> {
  const res = await request.post({ url: {X}_API.LIST_XXX, data: {} });
  const list = Array.isArray(res?.result) ? res.result : [];
  return list.map(normalizeXxx);
}

/** 写接口：失败不抛出，返回 {success, message}，由调用方决定提示方式 */
export async function markXxxViewed(id: string): Promise<{ success: boolean; message: string }> {
  try {
    const res = await request.post({ url: `${{X}_API.MARK}/${id}` });
    return { success: res?.status?.success === true || res?.status?.code === '200',
             message: res?.status?.message ?? '已标记' };
  } catch (error: any) {
    return { success: false, message: error?.message ?? '标记失败' };
  }
}

/** 导出：三个选项缺一不可，见 6.34 */
export function exportXxx(data: ExportRequest) {
  return request.post({ url: {X}_API.EXPORT, data, responseType: 'blob' },
                       { isReturnNativeResponse: true, isTransformResponse: false });
}
```

### 7.8 前端 `utils/displayStatus.ts`（**整文件照抄，只改枚举值**）

`computeDisplayStatus` 的四级优先级顺序、`isHeartbeatTimeout` 的
「无心跳时间返回 false」语义（**无数据 ≠ 超时**）、`pickDefaultDoor` 的
「优先在线，全离线则选第一个但不自动播放」—— 全部保留，见 6.38 / 6.39。

### 7.9 前端 composables（**三个文件整体照抄，只改类名/日志前缀**）

- `useRequestRaceGuard.ts` —— 与功能无关，**一字不改**，直接复制。
- `use{X}Polling.ts` —— 只有 `console.warn('[{feature}] ...')` 前缀要改。
- `use{X}PollingHost.ts` —— 只有函数名要改。
- `use{X}State.ts` —— 这个要按新功能的实际数据块改写，但**结构照抄**：
  每块数据 = `ref` + `loading` + `error` + 独立 `guard`，
  最后 `return` 里把 `pollings` / `refreshXxx` / `failureCount` 一起暴露出去。

```ts
export function use{X}State(options: { recentEnabled?: boolean } = {}) {
  const { recentEnabled = true } = options;
  const doors = ref<T[]>([]); const doorsLoading = ref(false); const doorsError = ref('');
  const doorsGuard = useRequestRaceGuard();

  async function loadDoors() {
    const token = doorsGuard.begin();
    doorsLoading.value = true; doorsError.value = '';
    try {
      const list = await listDoorStatus();
      if (!doorsGuard.isCurrent(token)) return;          // ★ 过期结果直接丢弃
      doors.value = list;
      if (!selectedDoorId.value || !list.some(d => d.doorId === selectedDoorId.value))
        selectedDoorId.value = pickDefaultDoor(list).selected?.doorId ?? '';
    } catch (error: any) {
      if (doorsGuard.isCurrent(token)) doorsError.value = error?.message ?? '加载失败';
      throw error;                                       // ★ 必须 rethrow，轮询靠它计数退避
    } finally {
      if (doorsGuard.isCurrent(token)) doorsLoading.value = false;
    }
  }

  const doorPolling = createPolling({ interval: POLLING.DOOR_INTERVAL, task: loadDoors });
  const eventPolling = recentEnabled ? createPolling({ interval: POLLING.EVENT_INTERVAL, task: loadEvents }) : null;
  const activePollings = eventPolling ? [doorPolling, eventPolling] : [doorPolling];

  onBeforeUnmount(() => { doorsGuard.invalidate(); activePollings.forEach(p => p.dispose()); });

  return { /* 数据 / loading / error / 行为 / pollings / refreshXxx / failureCount */ };
}
```

页面里：
```ts
const state = use{X}State({ recentEnabled: true });
const { doors, doorsLoading, ... } = state;            // ★ 解构出 ref，模板自动解包
use{X}PollingHost({ pollings: state.pollings });        // ★ 单一入口
onMounted(async () => { await Promise.allSettled([loadDoors(), loadEvents()]); ... });
```

### 7.10 `constants/index.ts`（结构）

每个枚举都要齐 4 件套：
```ts
export const XXX_STATUS = { A: 'A', B: 'B' } as const;              // 1. 值集合
export const XXX_STATUS_LABEL: Record<XxxStatus, string> = { A: '甲', B: '乙' };  // 2. 中文标签
export const XXX_STATUS_THEME = { A: 'success', B: 'danger' }
  as const satisfies Record<XxxStatus, string>;                     // 3. TDesign theme
export const XXX_STATUS_COLOR: Record<XxxStatus, string> = { A: '#00a870', B: '#d54941' };  // 4. 色值
```
再加 `POLLING` / `PAGINATION_DEFAULT` / 各类 `*_OPTIONS`（给 `t-select` 用）。

⚠️ 基准的 `constants/index.ts` 里遗留了一个 `PERMISSION_CODE` 常量块（5 个权限码字符串），
**后端已不再使用权限码**（见 6.20），它是死代码。
新功能**不要复制这一块**；若要给前端做按钮级可见性，应该新增一个"当前用户权限 ID 集合"的 store 查询，
而不是权限码字符串。

---

## 8. 实施顺序

> 每个 Step 后面是**必须执行的验证动作**。不要跳过。

### Step 0 — 收集需求
用第 12 节的清单问清楚。缺任何一项都不要开工。

### Step 1 — 数据模型与建表脚本
1. 设计表（沿用 `ai_` 前缀，每表带 `warehouse_id BIGINT NOT NULL` + `region_id` + `org_id` +
   `created_by/created_at/updated_by/updated_at` + 乐观锁 `xxx_version INTEGER` 如需写路径）。
2. 写 `docs/{feature}/sql/01_create_*.sql`，**头部带 6.31 的两步门禁**。
3. 写 `02_rollback_*.sql`（`DROP TABLE` 逆序，带同样门禁）。

**验证**：`SELECT current_database()` 手工确认；脚本在**目标库**上执行前向用户申请授权。

### Step 2 — 后端装配三件套（★ 最容易炸的一步）
按 7.1 / 7.2 / 7.3 建三个 config 类 + `application-{feature}.yml` + 主 `application.yml` 的 `optional:` import。

**验证**（必须两个方向都过）：
```bash
# 2.1 enabled=false（默认）：应用必须能正常启动，主库 30+ 模块功能不受影响
cd /d/liangku/service && mvn compile -q   # 然后启动，检查无 Invalid bound statement

# 2.2 enabled=true：注入环境变量后启动，本功能连真实库，
#     同时主库 Mapper 仍必须正常（这是 6.1/6.2 的验收点）
```
启动日志里必须能看到 `[功能] 创建独立数据源 database=... port=...`。

### Step 3 — Entity / DTO / 枚举
6 个 Entity + 13 个 DTO + 1 个 StatusEnum。
**注意**：`{X}DataScope` 必须含 `allowsWarehouse()` 与 `denyAll()`；查询 DTO 的 scope 字段必须 `@JsonIgnore`。

**验证**：`mvn compile -q`。

### Step 4 — Mapper 接口 + XML
按 7.5 给**每个**查询加数据范围块。所有方法加 `@Param`。时间列 `to_char`。

**验证**：
```bash
grep -c "AND 1 = 0" resources/mapper/{feature}/*.xml    # 数量 = 查询数 + 注释数
```
再用一个临时 Java 工具类跑一次裸 SQL，确认列名与表结构一致（见第 11 节"裸 JDBC 验不了动态标签"）。

### Step 5 — Repository（接口 + MyBatis 实现 + 内存实现）
按 6.7 在 MyBatis 实现上声明事务。写路径三步：范围校验 → 乐观锁更新 → 追加审计。

**验证**：`mvn compile -q`。

### Step 6 — ScopeResolver
按 7.4 照抄，**`queryOrgSubtreeUnpaged` 一个字符都不能少**（6.8）。

**验证**：见 Step 8 的分页与越权联合测试。

### Step 7 — ServiceImpl + Controller + 权限
按 7.6 加 `@PreAuthorize`。ServiceImpl **不加** `@Transactional`。
`ExcelExportConfig` 加模板 + `{X}Constant.EXPORT_BIZ_TYPE`（6.25）。

**验证**：`mvn compile -q` + 启动成功。

### Step 8 — 后端真实联调自测（★ 不可省略）
```bash
# 8.1 未认证拦截：匿名 POST 必须返回 9109
curl -s -X POST http://127.0.0.1:42000/smart-storage-admin/{feature}/listXxx \
     -H "Content-Type: application/json" -d '{}'

# 8.2 带 token 读接口：必须返回真实数据，不是空数组
# 8.3 分页：pageNo=1/2/3 各返回 pageSize 条，total 恒定
#     （这一步同时验证 6.8 —— 若 total == 列表长度，说明分页上下文丢了）
# 8.4 写路径：必须真实提交一次，确认无「Parameter not found」（6.19）
# 8.5 幂等：同一写请求连发两次，第二次不追加审计
# 8.6 乐观锁：故意传旧 version，必须返回冲突提示且数据库无变化
# 8.7 导出：HTTP 200 + MIME 是 OOXML + 魔数 PK\x03\x04 + 用 ZIP 工具能打开且行数正确
# 8.8 权限双向：配一个不存在的权限 ID → 必须 403；配正确 ID → 必须 200
```

### Step 9 — 前端 api 层 + types + constants + utils
按 7.7 / 7.8 / 7.10。

**验证**：`npx vue-tsc --noEmit 2>&1 | grep "src/api/{feature}\|src/pages/{module}/{feature}" | wc -l` → 0

### Step 10 — 前端 composables
`useRequestRaceGuard.ts` 直接复制；两个 polling 文件复制改前缀；`use{X}State.ts` 按结构改写。

### Step 11 — 页面与组件
按 UI 原型写 `components/*.vue` 与 `index.vue` / `history.vue`。
**注意**：`meta.icon` 用字符串（6.41）；预览用 `t-image-viewer`（6.43）；单区域失败不阻塞（6.44）。

### Step 12 — 路由
`src/router/modules/{module}.ts`，`path` 与后端接口路径保持语义一致。

### Step 13 — Mock
`mock/{feature}/{index,fixtures,xlsx}.ts`。
**fixtures 时间戳按 6.40 写**（心跳每次 `Date.now()`，事件时间可固定）。
Mock 与真实响应必须同形（6.46）。

### Step 14 — 前端真实联调自测（★ 必须关 Mock 跑）
```bash
cd /d/liangku/manage
# 确认 .env.development 里 VITE_USE_MOCK = false，然后重启 dev server（6.47）
# 浏览器 Network 面板确认请求打到后端且返回真实数据
# 逐项验证：读 / 写 / 幂等 / 分页 / 导出下载 / 空态 / 错误态重试 / 控制台 0 error
```
**特别检查 6.39 那个坑**：若页面显示的状态与后端 `deviceStatus` 不一致，
十有八九是新鲜度阈值（心跳超时）挡住了 —— 看 `lastHeartbeatAt` 是否够新。

### Step 15 — 交付文档
`docs/{feature}/DELIVERY_HANDOFF.md`：状态表 / 已验证项 / **未验证项与已知风险（按严重度）** /
待办（分「接手人必须完成」与「可选」）/ 提交范围。
**诚实标注哪些是运行时证据、哪些只是代码实现。** 基准文档里就出现过
「声称已全部删除但实际文件还在」这类错误 —— 结论要与历史日志交叉核对。

---

## 9. 验证门禁

### 9.1 TypeScript 基线豁免策略

| 项 | 值 |
|---|---|
| 全仓历史错误数 | **会变动**。截至 `339464f`：**402 个错误 / 32 个文件**；本功能提交前是 391 / 29 —— 远程提交 `2fe361b`（新增 4 个 aiAssist 页面）带入了 11 个新错误 |
| 新功能路径要求 | **0 错误** |
| 结论 | ✅ **允许作为基线豁免**，采用增量门 |

> ⚠️ **不要把基线数字写死在任何脚本、文档或提交信息里。**
> 多人共用 `feature/ys` 分支，别人推一个新页面基线就会涨。
> **唯一可靠的做法是每次自己实测一遍**，并只断言「我的路径 = 0」。
> 基线错误集中在：`src/api/*.ts`（隐式 any）、`src/components/common-table/index.vue`、
> `src/layouts/components/*.vue`、`src/pages/**/components/add.vue`、`src/store/modules/permission.ts`、
> `src/utils/request/index.ts`、`src/pages/aiAssist/**`。

```bash
cd /d/liangku/manage
npx vue-tsc --noEmit 2>&1 | grep "error TS" > /tmp/tsc.txt
echo "基线总数: $(wc -l < /tmp/tsc.txt)"
echo "基线文件数: $(cut -d'(' -f1 /tmp/tsc.txt | sort -u | wc -l)"
# ★ 唯一的硬门禁：新功能路径必须为 0
grep -c "src/pages/{module}/{feature}\|src/api/{feature}" /tmp/tsc.txt
# 期望输出：0
# 定向检查单文件
npx vue-tsc --noEmit src/pages/{module}/{feature}/components/Xxx.vue 2>&1
```

⚠️ **`package.json` 的 build 脚本冲突（未解决，需产品/架构决策）**：
实测脚本表：
```
"build":       "vue-tsc --noEmit && vite build --mode release"   ← 会被基线错误挡死
"build:site":  "vue-tsc --noEmit && vite build --mode site"      ← 同上
"build:test":  "vite build --mode test"                          ← 不带类型检查，可用
"build:type":  "vue-tsc --noEmit"                                ← 单独类型检查
"dev":         "vite --open --mode development"                  ← VITE_USE_MOCK=false
"dev:mock":    "vite --open --mode mock"                         ← 读 .env.mock
```
基线不为 0 时 `npm run build` **直接失败**。两个选项：
① 从 `build` / `build:site` 里移除 `vue-tsc --noEmit`（把类型检查交给 CI 的增量门）；
② 清完全部基线错误。
**这超出单功能交付范围，动手前必须找用户确认。**

### 9.2 后端门禁

```bash
cd /d/liangku/service && mvn compile -q      # 必须无输出（BUILD SUCCESS）
```
编译通过 ≠ 运行期正确。6.13（`@Param`）、6.19（参数名）、6.14（BIGINT 类型）
都是**只在真实调用时才暴露**的错误，必须跑 Step 8。

### 9.3 前端 lint

```bash
cd /d/liangku/manage && npx eslint src/pages/{module}/{feature} src/api/{feature}.ts
# 期望 EXIT=0
```

---

## 10. 提交与推送规范

### 10.1 必须排除的文件

| 文件 | 原因 |
|---|---|
| `service/pom.xml` | `chinkutils.version` 从 `0.0.18-SNAPSHOT` 改成 `0.0.18.RELEASE` 是**用户预存变更**，与本功能无关 |
| `docs/dbtool/*` | 临时调试工具（`.java`/`.class`/`.arg`/`.js`/`.py`），不属于交付包。注意 `crun.arg` 是启动后端要用的，**别删** |
| 任何 `.env` 里的真实凭据 | 密码只能走环境变量，不得写入文件 |

### 10.2 用显式文件列表，不要 `git add .`

```bash
# 后端
cd /d/liangku/service
git add smart-storage-admin/src/main/java/com/chinaunicom/smartstorage/admin/{component,config,constant,enumbean}/*{X}*
git add smart-storage-admin/src/main/java/com/chinaunicom/smartstorage/admin/{controller,dto,entity,mapper,service}/{feature}/
git add smart-storage-admin/src/main/resources/{application-{feature}.yml,mapper/{feature}/,application.yml}
git add smart-storage-admin/src/main/java/.../config/ExcelExportConfig.java
git add docs/{feature}/
git status --short            # ★ 提交前必看：确认 pom.xml 是 " M" 而不是 "M "

# 前端
cd /d/liangku/manage
git add .env .env.development .env.site .env.test .env.mock vite.config.ts
git add src/api/{feature}.ts src/api/model/{feature}Model.ts
git add src/pages/{module}/ src/router/modules/{module}.ts
git add mock/{feature}/
```

### 10.3 提交信息结构

```
feat: {功能名}完整实现

- 前后端联调跑通（读/写/幂等/审计/导出）
- 功能权限接入（VIEW/EXPORT/PROCESS 三级，fail-closed）
- 服务端分页 + 乐观锁 + 审计追加
- Excel 导出实测通过（OOXML 格式验证）
- TypeScript 增量门：功能路径 0 错误（基线历史错误本次实测 N 个，未新增）
- 交付文档 + SQL 脚本纳入版本管理
- {本次特有的 UI/行为要点}

注意：pom.xml 的 {xxx} 变更为用户预存，未包含在本次提交
```

### 10.4 推送（远程可能有别人的提交）

```bash
cd /d/liangku/service
git pull --rebase origin feature/ys        # 若报 unstaged changes：
git stash push -m "user pre-existing pom.xml change" pom.xml
git pull --rebase origin feature/ys
git push origin feature/ys
git stash pop                              # ★ 推完立刻恢复，别忘

cd /d/liangku/manage
git pull --rebase origin feature/ys && git push origin feature/ys
```
**禁止 `--force`。禁止 `--no-verify`。禁止 `git commit --amend` 已推送的提交。**

---

## 11. 故障速查表

| 症状 | 根因 | 定位与修复 |
|---|---|---|
| 启动即 `Invalid bound statement (not found)`，且报错的是**主库** Mapper | 6.1 / 6.2 | 检查是否把独立数据源注册成了 Bean；检查 `MainMybatisConfiguration` 是否存在且带 `@Primary` |
| 启动报 `A component required a single bean, but 2 were found`（事务管理器） | 多了一个同类型 `PlatformTransactionManager` 别名 Bean | 删掉 `{X}MybatisConfiguration` 里的别名 Bean |
| 所有主库 `MPJBaseMapper` 联表查询失效 | 6.2 第 2 点 | `MainMybatisConfiguration` 必须循环应用全部 `SqlSessionFactoryBeanCustomizer` |
| `Mapped Statements collection already contains value for ...` | 6.2 第 3 点 | 主库 `mainMapperLocations` 必须剔除本功能 mapper 目录 |
| 列表筛选条件全部失效，但**不报错** | 6.13 | Mapper 单参数方法漏 `@Param("query")` |
| SQL 报 `operator does not exist: bigint = character varying` | 6.14 | 用 `Long` 绑定 BIGINT 列 |
| 历史页翻页无效 / 部分仓库偶发看不到 / 结果条数 = pageSize | 6.8 | `ScopeResolver` 缺 PageHelper 保护 |
| 时间字段带 6 位微秒 | 6.15 | SQL 侧 `to_char` |
| 导出 500（NPE） | 6.25 | 模板 key 用常量，不能用 `source` |
| 提交处理时报 `Parameter 'xxx' not found` | 6.19 | UPDATE 里的参数名与 Mapper 接口逐字核对 |
| 权限配了但永远不命中 | 6.20 | authority 是数字 ID 字符串，不是权限码 |
| 权限开了全被拒 | 6.22 | 某操作键没配 ID（fail-closed 生效）。检查 `{feature}.permission.rights` |
| **数据库 `device_status='ONLINE'` 但页面显示"离线"** | 6.39 | 心跳时间超过 90 分钟阈值。查 `lastHeartbeatAt` 是否够新；种子数据时间戳必须每次 `Date.now()` 算（6.40） |
| 侧边栏图标渲染空白 + 控制台 `Component is missing template or render function` | 6.41 | `meta.icon` 改成字符串名 |
| 控制台报组件无法解析（图片预览） | 6.43 | 用 `t-image-viewer` + `:images` |
| 改了 `VITE_USE_MOCK` 但行为没变 | 6.47 | 必须重启 dev server |
| Mock 跑得好，联调就炸 | 6.46 | Mock 与真实响应不同形（时间格式、字段命名） |
| 首屏同一批数据被请求两次 | 6.36 | 宿主 `onMounted` 只能 `resume()`，不能 `refreshNow()` |
| 页面切走后仍在轮询 / 隐藏后残留定时器 | 6.37 第 ③ 点 | `finally` 重调度前必须再校验 `paused`/`disposed` |
| 快速切换对象时旧请求覆盖新选择 | 6.35 | 每条异步路径都要竞态守卫 |
| `npm run build` 失败在 `vue-tsc` | 9.1 | 391 个历史错误。需决策：移除 build 里的 vue-tsc，或清历史错误 |
| 裸 JDBC 验证说 SQL 没问题，MyBatis 跑出来不对 | — | 裸 JDBC 验不了 MyBatis 动态标签（`<if>`/`<choose>`/`<foreach>`）。必须走真实 Mapper 调用链验证 |
| 交付文档说"已删除"但文件还在 | — | 文档结论必须用 `ls` 实际输出交叉核对，不能凭记忆写 |

---

## 12. 新功能需求收集清单

开工前必须问清楚这 12 项。缺任何一项都会导致返工。

| # | 问题 | 基准答案（仓门异动） |
|---|---|---|
| 1 | 功能名（中文）+ 英文标识（包名/类名/配置前缀）？ | 仓门异动监测 / `doorAnomaly` |
| 2 | 挂在哪个一级菜单下？路由 path？ | `/aiVision/doorAnomaly` |
| 3 | 有几个页面？每个页面形态？ | 2 个：监测首页（左面板+右视频+底部表格）、历史记录页 |
| 4 | 需要几张表？表名与字段？ | 6 张 `ai_*` |
| 5 | 有哪些状态维度？每个状态的取值与中文标签与颜色？ | 设备/物理/告警/算法任务/算法结论/处理/异动类型/展示综合 共 8 维 |
| 6 | 有没有"新鲜度"类判定（心跳/超时/有效期）？阈值多少？ | 心跳超时 90 分钟 —— **见 6.39 的坑** |
| 7 | 有写路径吗？需要乐观锁吗？需要审计吗？ | 需要；`event_version`；追加式 `ai_event_process_record` |
| 8 | 需要导出吗？导出列有哪些（中文名 → 字段名）？ | 需要，13 列 |
| 9 | 需要媒体（图片/视频）吗？一期支持哪些协议？ | 需要；一期只支持原生 `<video>` 可播的 MP4/WebM |
| 10 | 功能权限分几级？ | 3 级：VIEW / EXPORT / PROCESS |
| 11 | 数据范围按什么维度过滤？ | 仓库（主库组织树 `type=2` 子树） |
| 12 | UI 原型图在哪？要复刻哪些视觉细节？ | 原型图（3 列网格 / 颜色语义 / 选中角标 / 虚线未知态 / 呼吸灯） |

**额外必须向用户确认的授权边界**（基准会话里就卡在这几条上）：

- [ ] 是否授权在 `smart_storage_ai_extension` 上执行 DDL？
- [ ] 是否授权启动后端（应用会连**主库**）？
- [ ] 是否授权向数据库**写入**测试数据？（基准默认「只跑查询，零写入」）
- [ ] 是否授权 `git commit`？是否授权 `git push`？
- [ ] 主库的权限行/菜单行由谁执行？（基准：**禁止写主库**，脚本交付但标注未执行）
- [ ] 有没有第二个不同组织的非管理员测试账号？（越权实测必需）

---

## 附：本文档的来源与可信度

- 所有约束都来自 `D:\liangku\service` + `D:\liangku\manage` 两个仓库 `feature/ys` 分支上
  **已提交代码的实际内容**（83 个文件已逐个读取），以及基准会话中的真实故障记录。
- 已验证的运行时证据（不是推测）：后端 `BUILD SUCCESS`、真实库读写跑通、
  乐观锁与幂等实测、导出 OOXML 解包核对、权限双向 403/200 实测、
  TS 基线 391 / 功能路径 0、lint EXIT=0。
- 第 2.5 条与第 6.6 条中的「5 处 vs 注释说 4 处」是本次读取时**新发现的源文档缺陷**，
  已在文中显式标注。
- **未验证项**（复制时若涉及需自行确认）：跨组织多账号越权（缺第二个非管理员账号）；
  主库 RBAC 权限行入库后的真实鉴权行为；仓库 ID 对齐后的非超管可见性
  （基准实测：主库当前**没有任何 `type=2` 组织节点**）。
