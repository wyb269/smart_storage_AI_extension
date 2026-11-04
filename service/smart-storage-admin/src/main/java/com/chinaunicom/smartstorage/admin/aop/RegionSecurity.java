package com.chinaunicom.smartstorage.admin.aop;

import java.lang.annotation.*;

/**
 * 查询请求防止行政区域越权注解 需要满足以下几点： 1、在web层生效 2、业务需要是通过行政区域查询或者统计数据 3、请求参数需要有token和行政区域id（兼容orgId、regionId、region）
 * 4、请求VO实体类需要继承BaseQueryDto
 *
 * @author liuchink
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RegionSecurity {
    // String tokenSecret();

}
