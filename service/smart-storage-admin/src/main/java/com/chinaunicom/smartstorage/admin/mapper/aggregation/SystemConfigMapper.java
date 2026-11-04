package com.chinaunicom.smartstorage.admin.mapper.aggregation;


import java.util.List;

import com.github.yulichang.base.MPJBaseMapper;
import com.chinaunicom.smartstorage.admin.dto.aggregation.sysconfig.SystemConfigQueryDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.SystemConfig;

/**
 * @author liuchink
 * @since 2024/9/29 14:34
 */

public interface SystemConfigMapper extends MPJBaseMapper<SystemConfig> {
    List<SystemConfig> listSystemConfigByCondition(SystemConfigQueryDTO systemConfigQueryDTO);
}
