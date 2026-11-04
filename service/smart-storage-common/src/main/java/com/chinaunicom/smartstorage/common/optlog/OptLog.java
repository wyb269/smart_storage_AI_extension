package com.chinaunicom.smartstorage.common.optlog;

import java.lang.annotation.*;

/**
 * 自定义操作日志记录注解
 *
 * @author liuchink
 * @since 2022/7/6 14:12
 *
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OptLog {
    /**
     * 模块
     */
    String title() default "";

    /**
     * 功能
     */
    OptLogBusinessType businessType() default OptLogBusinessType.MANAGE;

    /**
     * 操作人类别
     */
    OptLogOperatorType operatorType() default OptLogOperatorType.OTHER;

    OptLogStorageType storageType() default OptLogStorageType.DB;

    /**
     * 是否保存请求的参数
     */
    boolean isSaveRequestData() default true;
}
