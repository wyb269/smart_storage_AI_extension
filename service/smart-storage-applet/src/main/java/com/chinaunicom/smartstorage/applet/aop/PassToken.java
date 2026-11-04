package com.chinaunicom.smartstorage.applet.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JWT忽略
 *
 * @author liuchink
 * @since 2021/9/1 15:16
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface PassToken {
	boolean required() default true;
}
