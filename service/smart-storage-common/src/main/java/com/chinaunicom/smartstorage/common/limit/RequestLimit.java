package com.chinaunicom.smartstorage.common.limit;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author liuchink
 * @since 2023/9/5 13:16
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface RequestLimit {
    /*** 资源的key,唯一* 作用：不同的接口，不同的流量控制 */
    String key() default "";

    /*** 最多的访问限制次数 */
    double permitsCount();

    /*** 获取令牌最大等待时间 */
    long timeout();

    /*** 获取令牌最大等待时间,单位(例:分钟/秒/毫秒) 默认:毫秒 */
    TimeUnit timeunit() default TimeUnit.MILLISECONDS;

    /*** 得不到令牌的提示语 */
    String message() default "请求过于频繁，请稍后再试";
}
