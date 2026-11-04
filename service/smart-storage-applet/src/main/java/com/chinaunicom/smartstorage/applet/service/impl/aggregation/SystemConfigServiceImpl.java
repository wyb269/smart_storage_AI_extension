package com.chinaunicom.smartstorage.applet.service.impl.aggregation;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Maps;
import com.chinaunicom.smartstorage.applet.entity.aggregation.SystemConfig;
import com.chinaunicom.smartstorage.applet.mapper.aggregation.SystemConfigMapper;
import com.chinaunicom.smartstorage.applet.service.inter.aggregation.SystemConfigService;
import com.chinaunicom.smartstorage.common.constant.RedisConstant;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author liuchink
 * @since 2024/9/29 14:37
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {
    @Resource
    private SystemConfigMapper systemConfigMapper;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Override
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

    @Override
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

    @Override
    public void cacheSystemConfig() {
        final List<SystemConfig> listSystemConfig = systemConfigMapper.selectList(Wrappers.lambdaQuery());
        for (SystemConfig systemConfig : listSystemConfig) {
            if (systemConfig.getNeedCache() == 1) {
                redisTemplate.opsForHash().put(RedisConstant.SYSTEM_CONFIG_KEY,
                    systemConfig.getSycGroup() + "_" + systemConfig.getSycKey(), systemConfig.getSycValue());
            }
        }
    }
}
