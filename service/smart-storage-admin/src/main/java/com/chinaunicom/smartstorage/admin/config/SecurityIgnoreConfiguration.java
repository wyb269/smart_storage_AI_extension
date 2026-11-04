package com.chinaunicom.smartstorage.admin.config;

import com.google.common.collect.ImmutableSet;
import com.chinaunicom.smartstorage.admin.aop.SecurityIgnore;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;
import java.util.Set;

/**
 * @author liuchink
 * @since 2025/3/11 15:36
 */

@Configuration
public class SecurityIgnoreConfiguration {
    @Resource
    private RequestMappingHandlerMapping requestMappingHandlerMapping;
    @Getter
    private static volatile ImmutableSet<String> securityIgnorePaths = ImmutableSet.of();

    public Set<String> initSecurityIgnore() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
        handlerMethods.forEach((info, method) -> {
            SecurityIgnore annotation = method.getMethodAnnotation(SecurityIgnore.class);
            if (annotation == null) {
                annotation = method.getBeanType().getAnnotation(SecurityIgnore.class);
            }
            if (annotation != null && info.getPatternsCondition() != null) {
                builder.addAll(info.getPatternsCondition().getPatterns());
            }
        });
        securityIgnorePaths = builder.build();
        return securityIgnorePaths;
    }

}
