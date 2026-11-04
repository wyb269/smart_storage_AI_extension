package com.chinaunicom.smartstorage.quartz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.session.FlushMode;
import org.springframework.session.MapSession;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 启动器
 *
 * @author liuchink
 * @since 2018/8/8 20:38
 */

@SpringBootApplication(scanBasePackages = {"com.chinaunicom.smartstorage"})
@MapperScan(basePackages = {"com.chinaunicom.smartstorage.quartz.mapper"})
@ServletComponentScan(basePackages = {"com.chinaunicom.smartstorage"})
@EnableAsync
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableTransactionManagement(proxyTargetClass = true)
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS * 2 * 24,
    redisNamespace = "smart-storage-quartz", flushMode = FlushMode.IMMEDIATE)
@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
public class WebQuartzApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(WebQuartzApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(WebQuartzApplication.class);
    }

}
