package com.chinaunicom.smartstorage.admin.aop;

import java.lang.annotation.*;

/**
 * @author liuchink
 * @since 2025/3/11 15:19
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SecurityIgnore {
}
