package com.chinaunicom.smartstorage.admin.service.inter.aggregation;

import com.chinaunicom.smartstorage.admin.dto.aggregation.sysconfig.SystemConfigGroupAndKeyDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.sysconfig.SystemConfigQueryDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.SystemConfig;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

/**
 * @author liuchink
 * @since 2024/9/29 14:34
 */
public interface SystemConfigService {
    ActionResult<String, String> updateSystemConfig(SystemConfig systemConfig);

    ActionResult<String, String> updateSystemConfig(List<SystemConfig> listSystemConfig);

    SystemConfig getSystemConfigByGroupAndKeyByDetail(SystemConfig systemConfig);

    ActionResult<SystemConfig, String>
        getSystemConfigByGroupAndKey(SystemConfigGroupAndKeyDTO systemConfigGroupAndKeyDTO);

    List<SystemConfig> listSystemConfigByCondition(SystemConfigQueryDTO systemConfigQueryDTO);

    void cacheSystemConfig();
}
