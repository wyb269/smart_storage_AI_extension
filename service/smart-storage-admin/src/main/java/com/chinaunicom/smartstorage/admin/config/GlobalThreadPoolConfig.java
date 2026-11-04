package com.chinaunicom.smartstorage.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Configuration
@Component
public class GlobalThreadPoolConfig {

    @Bean
    public Executor threadPool() {
        return Thread.ofVirtual()
                .name("virtual-", 0)
                .factory()::newThread;
    }
}
