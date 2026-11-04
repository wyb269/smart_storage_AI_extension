package com.chinaunicom.smartstorage.admin.component;

import cn.hutool.core.convert.Convert;
import com.chinaunicom.smartstorage.admin.aop.RegionSecurity;
import com.chinaunicom.smartstorage.common.bean.BaseQueryDTO;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author liuchink
 * @since 2023/8/11 8:33
 */
@Aspect
@Component
@Order(4)
@Slf4j
public class RegionSecurityComponent extends BaseController {
    @Resource
    private JwtTokenComponent jwtTokenComponent;

    @Pointcut("@annotation(com.chinaunicom.smartstorage.admin.aop.RegionSecurity)")
    public void annotationContent() {}

    @Around(value = "annotationContent()", argNames = "joinPoint")
    public Object regionSecurity(ProceedingJoinPoint joinPoint) {
        final Object proceed;
        final RegionSecurity regionSecurity = getAnnotationLog(joinPoint);
        Object[] args = joinPoint.getArgs();
        String tokenRegionId = jwtTokenComponent.getUser().getRegionId();
        for (Object arg : args) {
            if (arg instanceof BaseQueryDTO) {
                final BaseQueryDTO baseQueryDTO = Convert.convert(BaseQueryDTO.class, arg);
                baseQueryDTO.setRegionId(getValidRegionId(baseQueryDTO.getRegionId(), tokenRegionId));
            }
        }
        try {
            proceed = joinPoint.proceed(args);
        } catch (Throwable exception) {
            throw new CustomException(exception);
        }
        return proceed;
    }

    private RegionSecurity getAnnotationLog(JoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature)signature;
        Method method = methodSignature.getMethod();
        return method.getAnnotation(RegionSecurity.class);
    }
}
