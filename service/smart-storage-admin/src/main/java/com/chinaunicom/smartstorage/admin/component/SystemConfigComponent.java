package com.chinaunicom.smartstorage.admin.component;

import com.google.common.collect.Maps;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.entity.aggregation.SystemConfig;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.SystemConfigMapper;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.SystemConfigService;
import com.chinaunicom.smartstorage.common.constant.RedisConstant;
import com.chinaunicom.smartstorage.common.constant.SystemConfigConstant;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.exception.bean.CustomExceptionCode;
import com.chinaunicom.smartstorage.common.result.ActionResultService;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import jakarta.annotation.Resource;

import java.util.List;
import java.util.Map;

/**
 * @author liuchink
 * @since 2024/9/29 14:37
 */
@Component
public class SystemConfigComponent {
    @Resource
    private SystemConfigMapper systemConfigMapper;
    @Resource
    private SystemConfigService systemConfigService;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public SystemConfig getAdminResetUserPwd() {
        SystemConfig systemConfig = new SystemConfig();
        systemConfig.setSycGroup(SystemConfigConstant.ADMIN_RESET_USER_PWD_GROUP);
        systemConfig.setSycKey(SystemConfigConstant.ADMIN_RESET_USER_PWD_KEY);
        final SystemConfig systemConfigByGroupAndKey = getSystemConfigByGroupAndKey(systemConfig);
        if (systemConfigByGroupAndKey == null) {
            throw new CustomException(CustomExceptionCode.DEFAULT_EXCEPTION_CODE, "未查询到配置信息");
        }
        return systemConfigByGroupAndKey;
    }

    public SystemConfig getMustUpdatePwdDayAdmin() {
        SystemConfig systemConfig = new SystemConfig();
        systemConfig.setSycGroup(SystemConfigConstant.MUST_UPDATE_PWD_DAY_GROUP);
        systemConfig.setSycKey(SystemConfigConstant.MUST_UPDATE_PWD_DAY_ADMIN_KEY);
        final SystemConfig systemConfigByGroupAndKey = getSystemConfigByGroupAndKey(systemConfig);
        if (systemConfigByGroupAndKey == null) {
            throw new CustomException(CustomExceptionCode.DEFAULT_EXCEPTION_CODE, "未查询到配置信息");
        }
        return systemConfigByGroupAndKey;
    }

    public SystemConfig getAdminLockPwdTime() {
        SystemConfig systemConfig = new SystemConfig();
        systemConfig.setSycGroup(SystemConfigConstant.ADMIN_LOCK_PWD_GROUP);
        systemConfig.setSycKey(SystemConfigConstant.ADMIN_LOCK_PWD_TIME_KEY);
        final SystemConfig systemConfigByGroupAndKey = getSystemConfigByGroupAndKey(systemConfig);
        if (systemConfigByGroupAndKey == null) {
            throw new CustomException(CustomExceptionCode.DEFAULT_EXCEPTION_CODE, "未查询到配置信息");
        }
        return systemConfigByGroupAndKey;
    }

    public SystemConfig getRsaPublicKey() {
        SystemConfig systemConfig = new SystemConfig();
        systemConfig.setSycGroup(SystemConfigConstant.RSA_PUBLIC_KEY_GROUP);
        systemConfig.setSycKey(SystemConfigConstant.RSA_PUBLIC_KEY_KEY);
        final SystemConfig systemConfigByGroupAndKey = getSystemConfigByGroupAndKey(systemConfig);
        if (systemConfigByGroupAndKey == null) {
            throw new CustomException(CustomExceptionCode.DEFAULT_EXCEPTION_CODE, "未查询到配置信息");
        }
        return systemConfigByGroupAndKey;
    }

    public SystemConfig getRsaPrivateKey() {
        SystemConfig systemConfig = new SystemConfig();
        systemConfig.setSycGroup(SystemConfigConstant.RSA_PRIVATE_KEY_GROUP);
        systemConfig.setSycKey(SystemConfigConstant.RSA_PRIVATE_KEY_KEY);
        final SystemConfig systemConfigByGroupAndKey = getSystemConfigByGroupAndKey(systemConfig);
        if (systemConfigByGroupAndKey == null) {
            throw new CustomException(CustomExceptionCode.DEFAULT_EXCEPTION_CODE, "未查询到配置信息");
        }
        return systemConfigByGroupAndKey;
    }

    public SystemConfig getSystemConfigByGroupAndKey(SystemConfig systemConfig) {
        final String sycValue = Convert.toStr(redisTemplate.opsForHash().get(RedisConstant.SYSTEM_CONFIG_KEY,
            systemConfig.getSycGroup() + "_" + systemConfig.getSycKey()));
        if (CharSequenceUtil.isNotBlank(sycValue)) {
            systemConfig.setSycValue(sycValue);
        } else {
            systemConfig = systemConfigMapper.selectOne(
                Wrappers.lambdaQuery(SystemConfig.class).eq(SystemConfig::getSycGroup, systemConfig.getSycGroup())
                    .eq(SystemConfig::getSycKey, systemConfig.getSycKey()));
        }
        return systemConfig;
    }

    public Map<String, String> listSystemConfigByGroupAndKey(SystemConfig systemConfig) {
        final List<SystemConfig> listSystemConfig = systemConfigMapper.selectList(
            Wrappers.lambdaQuery(SystemConfig.class).eq(SystemConfig::getSycGroup, systemConfig.getSycGroup()).eq(
                CharSequenceUtil.isNotBlank(systemConfig.getSycKey()), SystemConfig::getSycKey,
                systemConfig.getSycKey()));
        Map<String, String> map = Maps.newHashMap();
        for (SystemConfig sysconfig : listSystemConfig) {
            map.put(sysconfig.getSycKey(), sysconfig.getSycValue());
        }
        return map;
    }
}
