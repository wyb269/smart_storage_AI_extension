package com.chinaunicom.smartstorage.admin.config;

import cn.hutool.crypto.SmUtil;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * @author liugang
 * @since 2025/11/24 9:41
 */
@Configuration
public class HikariDataSourceConfiguration {
    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String encryptedPassword;
    @Value("${spring.datasource.hikari.maximum-pool-size}")
    private Integer maximumPoolSize;
    @Value("${spring.datasource.hikari.minimum-idle}")
    private Integer minimumIdle;
    @Value("${spring.datasource.hikari.max-lifetime}")
    private Long maxLifetime;
    @Value("${spring.datasource.hikari.keepalive-time}")
    private Long keepaliveTime;
    @Value("${spring.datasource.hikari.connection-timeout}")
    private Long connectionTimeout;
    @Value("${spring.datasource.hikari.idle-timeout}")
    private Long idleTimeout;
    @Value("${spring.datasource.hikari.connection-test-query}")
    private String connectionTestQuery;
    @Value("${spring.datasource.hikari.leak-detection-threshold}")
    private Long leakDetectionThreshold;
    @Value("${spring.datasource.hikari.data-source-properties.cachePrepStmts}")
    private String cachePrepStmts;
    @Value("${spring.datasource.hikari.data-source-properties.prepStmtCacheSize}")
    private String prepStmtCacheSize;
    @Value("${spring.datasource.hikari.data-source-properties.prepStmtCacheSqlLimit}")
    private String prepStmtCacheSqlLimit;
    @Value("${spring.datasource.hikari.data-source-properties.useServerPrepStmts}")
    private String useServerPrepStmts;
    @Value("${spring.datasource.hikari.data-source-properties.rewriteBatchedStatements}")
    private String rewriteBatchedStatements;
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl(url);
        hikariDataSource.setUsername(username);
        hikariDataSource.setPassword(SmUtil.sm4(SystemConstant.DATASOURCE_SECRET_KEY.getBytes(StandardCharsets.UTF_8))
            .decryptStr(encryptedPassword));
        hikariDataSource.setMaximumPoolSize(maximumPoolSize);
        hikariDataSource.setMinimumIdle(minimumIdle);
        hikariDataSource.setMaxLifetime(maxLifetime);
        hikariDataSource.setKeepaliveTime(keepaliveTime);
        hikariDataSource.setConnectionTimeout(connectionTimeout);
        hikariDataSource.setIdleTimeout(idleTimeout);
        hikariDataSource.setConnectionTestQuery(connectionTestQuery);
        hikariDataSource.setLeakDetectionThreshold(leakDetectionThreshold);
        Properties properties = new Properties();
        properties.setProperty("cachePrepStmts", cachePrepStmts);
        properties.setProperty("prepStmtCacheSize", prepStmtCacheSize);
        properties.setProperty("prepStmtCacheSqlLimit", prepStmtCacheSqlLimit);
        properties.setProperty("useServerPrepStmts", useServerPrepStmts);
        properties.setProperty("rewriteBatchedStatements", rewriteBatchedStatements);
        hikariDataSource.setDataSourceProperties(properties);
        return hikariDataSource;
    }
}
