package com.chinaunicom.smartstorage.common.limit;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author liuchink
 * @since 2023/9/5 13:19
 */
@Slf4j
@Aspect
@Component
public class RequestLimitAcpect {
    @Resource
    private ActionResultService actionResultService;
    /*** 不同的接口，不同的流量控制* map的key为 Limiter.key */
    private final Map<String, RateLimiter> limitMap = Maps.newConcurrentMap();

    @Around("@annotation(com.chinaunicom.smartstorage.common.limit.RequestLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        Method method = signature.getMethod();
        // 拿limit的注解Limit
        RequestLimit requestLimit = method.getAnnotation(RequestLimit.class);
        if (requestLimit != null) {
            // key作用：不同的接口，不同的流量控制
            String key = requestLimit.key();
            RateLimiter rateLimiter;
            // 验证缓存是否有命中key
            if (!limitMap.containsKey(key)) {
                // 创建令牌桶
                rateLimiter = RateLimiter.create(requestLimit.permitsCount());
                limitMap.put(key, rateLimiter);
                log.debug("新建了令牌桶={}，容量={}", key, requestLimit.permitsCount());
            }
            rateLimiter = limitMap.get(key);
            // 拿令牌
            boolean acquire = rateLimiter.tryAcquire(requestLimit.timeout(), requestLimit.timeunit());
            if (!acquire) {
                log.debug("令牌桶={}，获取令牌失败", key);
                return actionResultService.callBackResultFail(requestLimit.message());
            }
        }
        return joinPoint.proceed();
    }
}
