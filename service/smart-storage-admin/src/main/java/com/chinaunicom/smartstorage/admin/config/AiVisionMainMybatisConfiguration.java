package com.chinaunicom.smartstorage.admin.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.autoconfigure.SqlSessionFactoryBeanCustomizer;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

/**
 * 主库 MyBatis 装配的<b>显式接管</b>，仅在仓门异动独立数据源启用时生效。
 *
 * <p><b>为什么必须存在</b>（结论来自 mybatis-plus-spring-boot3-starter 3.5.15
 * 字节码，不是推测）：{@code MybatisPlusAutoConfiguration} 的两个 Bean 都是
 * {@code @ConditionalOnMissingBean}：</p>
 * <pre>
 * &#64;Bean &#64;ConditionalOnMissingBean SqlSessionFactory sqlSessionFactory(DataSource)
 * &#64;Bean &#64;ConditionalOnMissingBean SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory)
 * </pre>
 * <p>仓门功能为了物理隔离必须注册自己的 {@code SqlSessionFactory} 与
 * {@code SqlSessionTemplate}，于是主库这两个自动装配<b>整体退让</b>：
 * 主库 Mapper 没有任何已绑定语句，启动即报
 * {@code Invalid bound statement (not found): WorkerNodeMapper.addWorkerNode}。
 * 给主库 DataSource 标 {@code @Primary}、或把仓门 DataSource 改成非 Bean
 * 都<b>不能</b>解决（均实测失败），因为退让条件是 SqlSessionFactory 类型本身。</p>
 *
 * <p><b>影响面控制</b>：本类带
 * {@code @ConditionalOnProperty(enabled = true)}，与仓门数据源同生同灭。
 * {@code enabled=false}（当前默认、也是既有部署形态）时本类<b>完全不参与装配</b>，
 * 主库仍由 MyBatis-Plus 自动装配，其余 30+ 模块行为零变化。</p>
 *
 * <p><b>MPJ 兼容</b>：{@code mybatis-plus-join} 通过
 * {@link SqlSessionFactoryBeanCustomizer} 给工厂注入联表能力。
 * 自动装配退让后没人调用这些定制器，所有 {@code MPJBaseMapper} 会失效。
 * 因此这里必须显式应用容器内的全部定制器。</p>
 *
 * <p><b>mapper-locations 必须排除本功能目录</b>：主库配置是 classpath 下
 * mapper 目录的递归通配，会把本功能的 XML 目录也一并吞进来，
 * 与本功能的 SqlSessionFactory 重复注册同一批语句。</p>
 *
 * @author 仓门异动功能
 */
@Configuration
@ConditionalOnProperty(
    prefix = "ai-vision.datasource",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
public class AiVisionMainMybatisConfiguration {

    /** 独立库专用目录，主库工厂必须整体排除；其下每个子功能一个子目录 */
    private static final String FEATURE_MAPPER_DIR = "/mapper/aivision/";

    /**
     * 主库 SqlSessionFactory，接管自动装配的产物。
     *
     * @param dataSource      主库数据源（按名注入，避免与本功能数据源混淆）
     * @param properties      沿用 application.yml 中的 mybatis-plus 配置
     * @param interceptorProvider 既有的 MybatisPlusInterceptor（分页 + 乐观锁）
     * @param customizers     MPJ 等框架注册的工厂定制器，必须全部应用
     */
    @Bean(name = "sqlSessionFactory")
    @Primary
    public SqlSessionFactory sqlSessionFactory(
            @Qualifier("dataSource") DataSource dataSource,
            MybatisPlusProperties properties,
            ObjectProvider<MybatisPlusInterceptor> interceptorProvider,
            ObjectProvider<SqlSessionFactoryBeanCustomizer> customizers) throws Exception {

        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        // 与自动装配一致：CoreConfiguration 只是设置项 POJO，
        // 必须通过 applyTo 落到真正的 MybatisConfiguration 上
        // （MP 自己的 applyConfiguration 就是这么做的）。
        MybatisConfiguration configuration = new MybatisConfiguration();
        MybatisPlusProperties.CoreConfiguration coreConfiguration = properties.getConfiguration();
        if (coreConfiguration != null) {
            coreConfiguration.applyTo(configuration);
        }
        factory.setConfiguration(configuration);

        if (StringUtils.hasText(properties.getTypeAliasesPackage())) {
            factory.setTypeAliasesPackage(properties.getTypeAliasesPackage());
        }
        if (properties.getGlobalConfig() != null) {
            factory.setGlobalConfig(properties.getGlobalConfig());
        }

        List<MybatisPlusInterceptor> interceptors =
            interceptorProvider.orderedStream().toList();
        if (!interceptors.isEmpty()) {
            factory.setPlugins(interceptors.toArray(new MybatisPlusInterceptor[0]));
        }

        factory.setMapperLocations(mainMapperLocations(properties));

        // 关键：MPJ 等依赖此扩展点，漏掉会让所有 MPJBaseMapper 失去联表能力
        for (SqlSessionFactoryBeanCustomizer customizer : customizers.orderedStream().toList()) {
            customizer.customize(factory);
        }

        return factory.getObject();
    }

    /**
     * 主库 SqlSessionTemplate。自动装配已退让，这里补上并标 @Primary，
     * 保证未指定 ref 的应用级 {@code @MapperScan} 按类型装配时拿到的是主库模板。
     */
    @Bean(name = "sqlSessionTemplate")
    @Primary
    public SqlSessionTemplate sqlSessionTemplate(
            @Qualifier("sqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * 解析主库 mapper XML，并剔除本功能目录。
     */
    private Resource[] mainMapperLocations(MybatisPlusProperties properties) throws java.io.IOException {
        String[] locations = properties.getMapperLocations();
        List<Resource> resolved = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        if (locations != null) {
            for (String location : locations) {
                Resource[] found = resolver.getResources(location);
                for (Resource resource : found) {
                    if (isFeatureMapper(resource)) {
                        continue;
                    }
                    resolved.add(resource);
                }
            }
        }
        return resolved.toArray(new Resource[0]);
    }

    /**
     * 判断某个 XML 是否属于本功能目录（用 URL 路径判断，不依赖文件名）。
     */
    private boolean isFeatureMapper(Resource resource) {
        try {
            String url = resource.getURL().toString().replace('\\', '/');
            return url.contains(FEATURE_MAPPER_DIR);
        } catch (Exception e) {
            // 取不到 URL 时保守处理：保留该资源，不静默丢语句
            return false;
        }
    }
}
