# 项目框架开发使用说明

## 项目概述

chinkframe-boot 是一个基于 Spring Boot 3 的企业级后台管理系统框架，采用前后端分离架构设计。该框架集成了权限控制、JWT
认证、操作日志、敏感词过滤、文件上传等企业级应用功能，为快速开发后台管理系统提供了一套完整的解决方案。

## 技术栈

- **核心框架**: Spring Boot 3.3.12
- **编程语言**: Java 21
- **持久层框架**: MyBatis-Plus 3.5.12
- **增强持久层框架**: MyBatis-Plus-Join 1.5.4
- **安全框架**: Spring Security
- **认证授权**: JWT (JSON Web Token)
- **分布式缓存**: Redisson 3.32.0
- **分页插件**: PageHelper 2.1.0
- **API 文档**: Knife4j 4.5.0 (OpenAPI 3)
- **JSON 处理**: Fastjson2 2.0.51
- **工具库**: Hutool 5.8.31, Guava
- **对象映射**: Orika 3.4.2
- **高性能队列**: Disruptor 3.4.2
- **文件存储**: MinIO
- **ID 生成器**: 百度 UID Generator
- **敏感词过滤**: sensitive-word
- **数据库**: PostgreSQL (默认)

## 项目结构

```
chinkframe-boot
├── chinkframe-admin          # 后台管理模块
│   ├── src/main/java
│   │   ├── component         # 核心组件（JWT、操作日志、敏感词、MinIO等）
│   │   ├── config            # 配置类
│   │   ├── constant          # 常量定义
│   │   ├── controller        # 控制器层
│   │   ├── dto               # 数据传输对象
│   │   ├── entity            # 实体类
│   │   ├── exception         # 异常处理
│   │   ├── filter            # 过滤器
│   │   ├── mapper            # MyBatis Mapper接口
│   │   ├── security          # 安全配置
│   │   ├── service           # 业务逻辑层
│   │   └── utils             # 工具类
│   └── src/main/resources
│       ├── mapper            # MyBatis XML映射文件
│       └── application-*.yml # 配置文件
├── chinkframe-common         # 公共模块
│   ├── src/main/java
│   │   ├── aop               # 面向切面编程组件
│   │   ├── bean              # 公共实体类
│   │   ├── config            # 公共配置
│   │   ├── constant          # 公共常量
│   │   ├── controller        # 基础控制器
│   │   ├── converter         # 类型转换器
│   │   ├── exception         # 公共异常处理
│   │   ├── jwt               # JWT工具类
│   │   ├── operationlog      # 操作日志组件
│   │   ├── page              # 分页组件
│   │   ├── result            # 统一返回结果
│   │   └── utils             # 公共工具类
└── lib                       # 本地依赖库
```

## 核心功能模块

### 1. 权限管理系统 (RBAC)

- 用户管理
- 角色管理
- 权限菜单管理
- 组织机构管理
- 区域管理

### 2. 认证与授权

- JWT Token 认证机制
- Spring Security 安全控制
- 动态权限验证
- 登录失败次数限制

### 3. 文件存储

- MinIO 对象存储集成
- 文件上传下载
- 分片上传支持
- 临时凭证生成

### 4. 操作日志

- 注解式操作日志记录
- 方法级别日志切面
- 日志存储到数据库

### 5. 敏感词过滤

- 敏感词库管理
- 运行时敏感词更新
- 文本内容过滤

### 6. 全局 ID 生成

- 雪花算法 ID 生成器
- 分布式 ID 生成支持

### 7. 分页查询

- PageHelper 分页插件
- 统一分页结果封装

## 快速开始

### 环境要求

- JDK 21
- Maven 3.6+
- Redis
- PostgreSQL
- MinIO (可选)

### 构建项目

```bash
# 克隆项目
git clone <repository-url>

# 进入项目目录
cd chinkframe-boot

# 编译打包
mvn clean package -DskipTests
```

### 配置文件

在 `chinkframe-admin/src/main/resources/` 目录下配置不同环境的配置文件：

- `application-dev.yml` - 开发环境
- `application-site.yml` - 测试环境
- `application-uat.yml` - 生产环境

### 启动项目

```bash
# IDEA 启动需配置
--add-opens=java.base/java.lang=ALL-UNNAMED --spring.profiles.active=dev
```

```bash
# 开发环境启动
java -jar chinkframe-admin/target/chinkframe-admin.jar --add-opens=java.base/java.lang=ALL-UNNAMED --spring.profiles.active=dev
```

## 核心组件说明

### 1. 获取当前请求用户信息

    JwtTokenComponent

### 2. 操作日志组件

    @Slf4j

### 3. MinIO 文件存储组件

    MinioComponent

### 4. 数据脱敏
    @Desensitization(type = DesensitizationTypeEnum.MY_RULE, startInclude = 0, endExclude = 1)
### 5. 雪花 ID 生成器

    @Resource
    private UidGenerator uidGenerator;

### 6. 分页查询

    1对1 自动分页    @QueryByPage
    1对1 手动分页    PageMethod.startPage或者使用MyBatis-Plus分页
    1对多或多对多    数据库手动分页

### 7. 匿名访问

    @SecurityIgnore 管理后台
    @PassToken 移动端

### 8. 接口防抖

    @NoRepeat(timeout = 100)

### 9. 接口限流

    @RequestLimit(key = "key", permitsCount = 3, timeout = 5, timeunit = TimeUnit.SECONDS)

### 10. 异步线程

    @Async(value = "defaultAsync")

### 11. 定时任务

    xxljob

### 12. Excel 导入导出

    fastexcel

### 13. API 访问日志
    @OptLog(title = "", operatorType = OptLogOperatorType.INSERT)

### 14. 分布式锁

    @Resource
    private RedissonClient redissonClient;

    redissonClient.getLock();

### 15. HTTP请求

    @resource(name = "defaultChinkOkHttpV2Utils")
    private ChinkOkHttpV2Utils chinkOkHttpV2Utils

### 16. Redis缓存

    @Resource
    private RedisTemplate<String, String> redisTemplate;

### 17. 对象转换

    @Resource
    private MapperFacade mapperFacade;

    mapperFacade.map()
    mapperFacade.mapAsList()

### 18. 查询对象关系
    TimeBaseQueryDTO->BaseQueryDTO->PageQueryDTO
    TimePageQueryDTO->PageQueryDTO

### 19. 虚拟线程使用 
    @Resource
    private Executor threadPool;

    public void doSomethingAsync() {
        threadPool.execute(() -> {
        // 异步任务，运行在虚拟线程中
        System.out.println("Running on: " + Thread.currentThread());
    });}

## 开发规范

### 1. 包命名规范

- 控制器: `controller`
- 服务层: `service`
- 数据访问: `mapper`
- 实体类: `entity`
- 数据传输对象: `dto`

### 2. 编码规范

- 使用 Lombok 简化实体类
- 统一使用 ActionResult 作为返回结果
- 使用 PageQueryDTO 进行分页查询 分页查询需继承 PageQueryDTO
- 异常处理遵循统一规范

### 3. 数据库设计

- 表名使用下划线命名
- 主键统一使用雪花 ID
- 字段命名遵循驼峰命名规范
-

### 4. 分支

- feature/dev 分支开发
- site 分支测试
- uat 分支生产(预留 后期根据产品线看看如何划分)

## API 文档

项目集成了 Knife4j OpenAPI 文档，启动后访问:

```
http://localhost:8080/chinkframe-boot-admin/doc.html
```
