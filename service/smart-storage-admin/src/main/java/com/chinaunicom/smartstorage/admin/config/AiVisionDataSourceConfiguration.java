package com.chinaunicom.smartstorage.admin.config;

import jakarta.annotation.PreDestroy;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 仓门异动功能独立数据源。
 *
 * <p><b>设计原则（AI_HANDOFF.md 第 39.2 节）</b>：</p>
 * <ul>
 *   <li>本功能及后续扩展功能的表<b>只落在 smart_storage_ai_extension</b>；</li>
 *   <li>绝不访问 smart-storage-site 或任何其他数据库；</li>
 *   <li>不修改、不影响项目原有的 {@code spring.datasource} 配置。</li>
 * </ul>
 *
 * <p><b>实现方式</b>：连接池在本类内部构造（<b>不注册为 DataSource Bean</b>），
 * 配合独立的 SqlSessionFactory 与 MapperScan，与主库物理隔离。
 * 之所以不做成 Bean，见 {@link #obtainDataSource()} 上的实测说明。</p>
 *
 * <p><b>密码处理（第 39.5 节）</b>：密码从环境变量注入，
 * 配置文件中不出现明文，也不做加密存储（与主库的 Sm4 方案不同，
 * 因为这里不需要写入仓库）。</p>
 *
 * <p><b>降级策略</b>：未配置密码或显式关闭时（{@code enabled=false}），
 * 本数据源不创建，仓门功能自动回退到内存实现，
 * 其余功能完全不受影响。</p>
 *
 * @author 仓门异动功能
 */
@Slf4j
@Configuration
@ConditionalOnProperty(
    prefix = "ai-vision.datasource",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
public class AiVisionDataSourceConfiguration {

    /** 连接 URL 固定使用目标库名，不接受外部数据库名配置。 */
    private static final String ALLOWED_DATABASE = "smart_storage_ai_extension";

    @Value("${ai-vision.datasource.host}")
    private String host;

    @Value("${ai-vision.datasource.port}")
    private String port;

    @Value("${ai-vision.datasource.username:}")
    private String username;

    @Value("${ai-vision.datasource.password:}")
    private String password;

    @Value("${ai-vision.datasource.hikari.maximum-pool-size:10}")
    private Integer maximumPoolSize;

    @Value("${ai-vision.datasource.hikari.minimum-idle:2}")
    private Integer minimumIdle;

    @Value("${ai-vision.datasource.hikari.max-lifetime:1200000}")
    private Long maxLifetime;

    @Value("${ai-vision.datasource.hikari.keepalive-time:60000}")
    private Long keepaliveTime;

    @Value("${ai-vision.datasource.hikari.connection-timeout:20000}")
    private Long connectionTimeout;

    @Value("${ai-vision.datasource.hikari.idle-timeout:300000}")
    private Long idleTimeout;

    @Value("${ai-vision.datasource.hikari.pool-name:AiVisionHikariPool}")
    private String poolName;

    /**
     * 仓门异动功能专用数据源。
     *
     * <p><b>刻意不注册成 Spring Bean</b>（原先是
     * {@code @Bean(name = "doorAnomalyDataSource")}）。原因实测过：
     * 容器里一旦出现第二个 {@code DataSource} Bean，
     * {@code MybatisPlusAutoConfiguration} 的
     * {@code @ConditionalOnSingleCandidate(DataSource.class)} 就会退让，
     * 主库 {@code SqlSessionFactory} 不再创建，全部主库 Mapper 报
     * {@code Invalid bound statement (not found)}，应用起不来。
     * 给主库 DataSource 标 {@code @Primary} 也救不回来（MP 3.5.15 实测无效）。</p>
     *
     * <p>改为进程内单例后，容器里始终只有一个 {@code DataSource} Bean，
     * 主库自动装配完全不受影响；本功能仍拥有物理独立的连接池与
     * SqlSessionFactory，跨库隔离目标不变（第 39.2 节）。</p>
     *
     * <p>生命周期由本类的 {@link #closeDataSource()} 负责，
     * 容器关闭时释放连接池。</p>
     */
    public synchronized DataSource obtainDataSource() {
        if (this.dataSourceHolder == null) {
            this.dataSourceHolder = createDataSource();
        }
        return this.dataSourceHolder;
    }

    private volatile DataSource dataSourceHolder;

    private DataSource createDataSource() {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                "仓门异动功能数据库账号未配置。请设置环境变量 "
                + "AI_VISION_DB_USER 与 AI_VISION_DB_PASSWORD 后再启动。");
        }

        final String url = "jdbc:postgresql://" + host + ":" + port + "/" + ALLOWED_DATABASE
            + "?currentSchema=public"
            + "&serverTimezone=Asia/Shanghai"
            + "&useUnicode=true&characterEncoding=utf-8"
            + "&zeroDateTimeBehavior=convertToNull"
            + "&transformedBitIsBoolean=true"
            + "&rewriteBatchedStatements=true"
            + "&useSSL=false";

        // 日志脱敏（第29.1节）：只记录库名与端口，不记录账号、密码或完整连接串
        log.info("[仓门异动] 创建独立数据源 database={} port={}", ALLOWED_DATABASE, port);

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setPoolName(poolName);
        ds.setMaximumPoolSize(maximumPoolSize);
        ds.setMinimumIdle(minimumIdle);
        ds.setMaxLifetime(maxLifetime);
        ds.setKeepaliveTime(keepaliveTime);
        ds.setConnectionTimeout(connectionTimeout);
        ds.setIdleTimeout(idleTimeout);
        return ds;
    }

    /**
     * 仓门异动功能专用事务管理器。
     *
     * <p>保证 {@code @Transactional} 在本功能内作用于独立数据源，
     * 不会与主库事务混淆。</p>
     */
    @Bean(name = "aiVisionTransactionManager")
    public PlatformTransactionManager aiVisionTransactionManager() {
        return new DataSourceTransactionManager(obtainDataSource());
    }

    /**
     * 容器关闭时释放连接池。
     *
     * <p>数据源不再是 Spring Bean，不会被自动销毁，必须自己关。</p>
     */
    @PreDestroy
    public void closeDataSource() {
        if (this.dataSourceHolder instanceof HikariDataSource hikari) {
            hikari.close();
            log.info("[仓门异动] 独立数据源连接池已关闭");
        }
    }
}
