package com.chinaunicom.smartstorage.applet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.liuchink.chinkutils.net.ChinkOkHttpV2Utils;
import com.liuchink.chinkutils.net.bean.HttpProxy;

/**
 * @author liuchink
 * @since 2023/12/20 08:49
 */
@Configuration
public class ChinkOkHttpV2UtilsConfiguration {
    @Value(("${chinkframe.chinkOkHttp.proxy.hostname:}"))
    private String hostName;
    @Value(("${chinkframe.chinkOkHttp.proxy.port:}"))
    private Integer port;

    // 超时常量定义 (单位：毫秒)
    private static final int TIMEOUT_5S = 5000;
    private static final int TIMEOUT_10S = 10000;
    private static final int TIMEOUT_15S = 15000;
    private static final int TIMEOUT_20S = 20000;
    private static final int TIMEOUT_25S = 25000;
    private static final int TIMEOUT_30S = 30000;
    private static final int TIMEOUT_35S = 35000;
    private static final int TIMEOUT_40S = 40000;
    private static final int TIMEOUT_45S = 45000;
    private static final int TIMEOUT_50S = 50000;
    private static final int TIMEOUT_55S = 55000;
    private static final int TIMEOUT_60S = 60000;

    // ==================== 无代理配置 ====================

    @Bean(value = "timeout5sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils timeout5sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_5S, null);
    }

    @Bean(value = "timeout10sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils timeout10sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_10S, null);
    }

    @Bean(value = "timeout15sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils timeout15sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_15S, null);
    }

    @Bean(value = "timeout20sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils timeout20sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_20S, null);
    }

    @Bean(value = "timeout25sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils timeout25sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_25S, null);
    }

    @Bean(value = "timeout30sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils timeout30sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_30S, null);
    }

    @Bean(value = "timeout35sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils timeout35sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_35S, null);
    }

    @Bean(value = "timeout40sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils timeout40sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_40S, null);
    }

    @Bean(value = "timeout45sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils timeout45sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_45S, null);
    }

    @Bean(value = "timeout50sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils timeout50sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_50S, null);
    }

    @Bean(value = "timeout55sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils timeout55sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_55S, null);
    }

    @Bean(value = "timeout60sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils timeout60sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_60S, null);
    }

    // ==================== 代理配置 ====================

    @Bean(value = "proxyTimeout5sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils proxyTimeout5sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_5S, createProxy());
    }

    @Bean(value = "proxyTimeout10sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils proxyTimeout10sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_10S, createProxy());
    }

    @Bean(value = "proxyTimeout15sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils proxyTimeout15sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_15S, createProxy());
    }

    @Bean(value = "proxyTimeout20sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils proxyTimeout20sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_20S, createProxy());
    }

    @Bean(value = "proxyTimeout25sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils proxyTimeout25sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_25S, createProxy());
    }

    @Bean(value = "proxyTimeout30sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils proxyTimeout30sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_30S, createProxy());
    }

    @Bean(value = "proxyTimeout35sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils proxyTimeout35sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_35S, createProxy());
    }

    @Bean(value = "proxyTimeout40sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils proxyTimeout40sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_40S, createProxy());
    }

    @Bean(value = "proxyTimeout45sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils proxyTimeout45sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_45S, createProxy());
    }

    @Bean(value = "proxyTimeout50sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils proxyTimeout50sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_50S, createProxy());
    }

    @Bean(value = "proxyTimeout55sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils proxyTimeout55sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_55S, createProxy());
    }

    @Bean(value = "proxyTimeout60sChinkOkHttpV2Utils")
    public ChinkOkHttpV2Utils proxyTimeout60sChinkOkHttpV2Utils() {
        return new ChinkOkHttpV2Utils(TIMEOUT_60S, createProxy());
    }

    /**
     * 创建 HTTP 代理配置
     */
    private HttpProxy createProxy() {
        HttpProxy httpProxy = new HttpProxy();
        httpProxy.setHostName(hostName);
        httpProxy.setPort(port);
        return httpProxy;
    }

}
