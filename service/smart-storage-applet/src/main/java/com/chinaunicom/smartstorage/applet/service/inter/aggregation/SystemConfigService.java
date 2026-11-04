package com.chinaunicom.smartstorage.applet.service.inter.aggregation;

import com.chinaunicom.smartstorage.applet.entity.aggregation.SystemConfig;

import java.util.Map;

/**
 * @author liuchink
 * @since 2024/9/29 14:34
 */
public interface SystemConfigService {
    Map<String, String> listSystemConfigByGroupAndKey(SystemConfig systemConfig);

    SystemConfig getSystemConfigByGroupAndKey(SystemConfig systemConfig);

    void cacheSystemConfig();
}
