package com.chinaunicom.smartstorage.admin.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * 仓门异动功能独立 MyBatis 配置。
 *
 * <p><b>隔离目标（AI_HANDOFF.md 第 39.2 节）</b>：
 * 本功能的所有 Mapper 绑定到 {@code smart_storage_ai_extension}，
 * <b>绝不</b>复用主库的 SqlSessionFactory。</p>
 *
 * <p><b>启用条件</b>：仅当 {@code ai-vision.datasource.enabled=true}
 * 且账号密码已配置时才生效。条件不满足时，
 * 仓门功能自动回退到内存实现，其余功能完全不受影响。</p>
 *
 * <p><b>Mapper 扫描范围</b>：仅
 * {@code com.chinaunicom.smartstorage.admin.mapper.aivision.doorAnomaly}，
 * 不会重复扫描主库 Mapper。</p>
 *
 * @author 仓门异动功能
 */
@Configuration
@ConditionalOnProperty(
    prefix = "ai-vision.datasource",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
@MapperScan(
    basePackages = "com.chinaunicom.smartstorage.admin.mapper.aivision",
    sqlSessionTemplateRef = "aiVisionSqlSessionTemplate")
public class AiVisionMybatisConfiguration {

    /**
     * AI 视觉类功能共用的 SqlSessionFactory。
     *
     * <p>绑定独立数据源，配置与主库相同但物理隔离。</p>
     *
     * <p>数据源从 {@link AiVisionDataSourceConfiguration#obtainDataSource()} 取，
     * <b>不能</b>按 {@code @Qualifier} 注入成 Bean：容器里一旦出现第二个
     * {@code DataSource}，MyBatis-Plus 的主库自动装配会退让。
     * 详见该方法上的说明。</p>
     */
    @Bean(name = "aiVisionSqlSessionFactory")
    public SqlSessionFactory aiVisionSqlSessionFactory(
            AiVisionDataSourceConfiguration dataSourceConfiguration) throws Exception {

        DataSource aiVisionDataSource = dataSourceConfiguration.obtainDataSource();

        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(aiVisionDataSource);

        // XML 映射文件：aivision 下每个子功能一个目录，新增功能无需改这里
        factoryBean.setMapperLocations(
            new PathMatchingResourcePatternResolver()
                .getResources("classpath*:/mapper/aivision/*/*.xml"));

        // MyBatis-Plus 专用配置类：开启驼峰映射
        com.baomidou.mybatisplus.core.MybatisConfiguration configuration =
            new com.baomidou.mybatisplus.core.MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);

        // MyBatis-Plus 插件
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        factoryBean.setPlugins(interceptor);

        return factoryBean.getObject();
    }

    /**
     * 仓门异动功能专用 SqlSessionTemplate。
     *
     * <p>{@code @MapperScan} 通过此名称关联到本功能的 SqlSessionFactory。</p>
     */
    @Bean(name = "aiVisionSqlSessionTemplate")
    public SqlSessionTemplate aiVisionSqlSessionTemplate(
            @Qualifier("aiVisionSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    // 这里原先还有一个 doorAnomalyTransactionManagerRef Bean，
    // 只是 aiVisionTransactionManager 的同类型别名，全仓无人引用。
    // 它的存在让容器里出现两个同类型事务管理器，导致
    // 「A component required a single bean, but 2 were found」启动失败；
    // 真正的主库事务管理器已补在 HikariDataSourceConfiguration。
}