package com.chinaunicom.smartstorage.applet.aop;

import java.lang.annotation.*;

/**
 * @author liugang
 * @since 2025/11/19 13:02
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoRepeat {
    // 默认失效时间
    long timeout() default 10;
}
