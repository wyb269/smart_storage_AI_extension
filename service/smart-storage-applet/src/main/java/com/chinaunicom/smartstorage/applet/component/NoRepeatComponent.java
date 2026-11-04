package com.chinaunicom.smartstorage.applet.component;

import com.chinaunicom.smartstorage.applet.aop.NoRepeat;
import com.chinaunicom.smartstorage.common.constant.RedisConstant;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import jakarta.annotation.Resource;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @author liugang
 * @since 2025/11/19 13:06
 */
@Aspect
@Component
public class NoRepeatComponent {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private JwtTokenComponent jwtTokenComponent;

    @Pointcut("@annotation(com.chinaunicom.smartstorage.applet.aop.NoRepeat) || @within(com.chinaunicom.smartstorage.applet.aop.NoRepeat)")
    public void pointCut() {}

    @Around("pointCut()")
    private Object around(ProceedingJoinPoint proceedingJoinPoint) {
        try {
            MethodSignature signature = (MethodSignature)proceedingJoinPoint.getSignature();
            Method method = signature.getMethod();
            String methodName = method.getName();
            String userId = jwtTokenComponent.getMember().getId();
            String noRepeatKey = RedisConstant.NO_REPEAT_APPLET_KEY + methodName + "_" + userId;
            if (redisTemplate.hasKey(noRepeatKey)) {
                throw new CustomException("请勿重复提交");
            }
            NoRepeat annotation = method.getAnnotation(NoRepeat.class);
            long timeout = annotation.timeout();
            redisTemplate.opsForValue().set(noRepeatKey, noRepeatKey, timeout, TimeUnit.SECONDS);
            return proceedingJoinPoint.proceed();
        } catch (Throwable throwable) {
            throw new CustomException(throwable.getMessage());
        }
    }
}
