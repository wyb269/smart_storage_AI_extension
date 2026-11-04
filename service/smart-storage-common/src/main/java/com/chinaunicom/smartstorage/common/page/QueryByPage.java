package com.chinaunicom.smartstorage.common.page;

import java.lang.annotation.*;

/**
 * @author liuchink
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface QueryByPage {
    String value() default "";

    String returnClass() default "";
}
