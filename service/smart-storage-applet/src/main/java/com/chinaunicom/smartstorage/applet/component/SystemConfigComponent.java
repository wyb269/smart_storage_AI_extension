package com.chinaunicom.smartstorage.applet.component;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Maps;
import com.chinaunicom.smartstorage.applet.entity.aggregation.SystemConfig;
import com.chinaunicom.smartstorage.applet.mapper.aggregation.SystemConfigMapper;
import com.chinaunicom.smartstorage.common.constant.RedisConstant;
import com.chinaunicom.smartstorage.common.constant.SystemConfigConstant;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.exception.bean.CustomExceptionCode;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

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
    private ActionResultService actionResultService;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public SystemConfig getWeChatMiniAppId() {
        SystemConfig systemConfig = new SystemConfig();
        systemConfig.setSycGroup(SystemConfigConstant.WECHAT_GROUP);
        systemConfig.setSycKey(SystemConfigConstant.WECHAT_MINI_APPID_KEY);
        final SystemConfig systemConfigByGroupAndKey = getSystemConfigByGroupAndKey(systemConfig);
        if (systemConfigByGroupAndKey == null) {
            throw new CustomException(CustomExceptionCode.DEFAULT_EXCEPTION_CODE, "未查询到相关参数配置");
        }
        return systemConfigByGroupAndKey;
    }

    public SystemConfig getWeChatMiniAppSecret() {
        SystemConfig systemConfig = new SystemConfig();
        systemConfig.setSycGroup(SystemConfigConstant.WECHAT_GROUP);
        systemConfig.setSycKey(SystemConfigConstant.WECHAT_MINI_APPSECRET_KEY);
        final SystemConfig systemConfigByGroupAndKey = getSystemConfigByGroupAndKey(systemConfig);
        if (systemConfigByGroupAndKey == null) {
            throw new CustomException(CustomExceptionCode.DEFAULT_EXCEPTION_CODE, "未查询到相关参数配置");
        }
        return systemConfigByGroupAndKey;
    }

    public SystemConfig getAppletLockPwdTime() {
        SystemConfig systemConfig = new SystemConfig();
        systemConfig.setSycGroup(SystemConfigConstant.APPLET_LOCK_PWD_GROUP);
        systemConfig.setSycKey(SystemConfigConstant.APPLET_LOCK_PWD_TIME_KEY);
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
