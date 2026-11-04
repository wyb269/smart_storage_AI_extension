package com.chinaunicom.smartstorage.admin.config;

import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.*;

/**
 * @author liuchink
 * @since 2022/2/14 15:52
 */
@Configuration
public class AsyncConfiguration {
    private final int processNum = Runtime.getRuntime().availableProcessors();
    private final int corePoolSize = (int)(processNum / (1 - 0.2));
    private final int maxPoolSize = (int)(processNum / (1 - 0.5));
    private static final int KEEP_ALIVE_SECONDS = 300;

    @Bean(value = "defaultAsync")
    public AsyncTaskExecutor defaultAsyncTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        // 核心池大小
        threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
        // 最大线程数
        threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
        // 队列程度
        threadPoolTaskExecutor.setQueueCapacity(maxPoolSize * 1000);
        threadPoolTaskExecutor.setThreadPriority(Thread.MAX_PRIORITY);
        threadPoolTaskExecutor.setDaemon(false);
        // 线程空闲时间
        threadPoolTaskExecutor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        threadPoolTaskExecutor.setThreadNamePrefix("default-async-");
        threadPoolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        threadPoolTaskExecutor.setTaskDecorator(taskDecorator());
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    @Bean
    public TaskDecorator taskDecorator() {
        return new TaskDecorator() {
            @Override
            public @NonNull Runnable decorate(@NonNull Runnable runnable) {
                RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
                return () -> {
                    try {
                        RequestContextHolder.setRequestAttributes(requestAttributes, true);
                        runnable.run();
                    } finally {
                        RequestContextHolder.resetRequestAttributes();
                    }
                };
            }
        };
    }

    @Bean(value = "defaultExecutorService", destroyMethod = "shutdown")
    public ExecutorService executorService() {
        ThreadFactory threadFactory = new CustomizableThreadFactory("default-executor-");
        return new ThreadPoolExecutor(corePoolSize, maxPoolSize, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(maxPoolSize * 1000), threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
