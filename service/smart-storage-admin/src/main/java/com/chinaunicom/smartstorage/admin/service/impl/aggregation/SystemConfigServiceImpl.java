package com.chinaunicom.smartstorage.admin.service.impl.aggregation;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.component.PwdComponent;
import com.chinaunicom.smartstorage.admin.dto.aggregation.sysconfig.SystemConfigGroupAndKeyDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.sysconfig.SystemConfigQueryDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.SystemConfig;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.SystemConfigMapper;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.SystemConfigService;
import com.chinaunicom.smartstorage.common.constant.RedisConstant;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import ma.glasnost.orika.MapperFacade;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private PwdComponent pwdComponent;
    @Resource
    private MapperFacade mapperFacade;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateSystemConfig(SystemConfig systemConfig) {
        final SystemConfig systemConfigUpdate = systemConfigMapper.selectOne(
            Wrappers.lambdaQuery(SystemConfig.class).eq(SystemConfig::getSycGroup, systemConfig.getSycGroup())
                .eq(SystemConfig::getSycKey, systemConfig.getSycKey()));
        systemConfigMapper.update(systemConfig,
            Wrappers.lambdaQuery(SystemConfig.class).eq(SystemConfig::getSycGroup, systemConfig.getSycGroup())
                .eq(SystemConfig::getSycKey, systemConfig.getSycKey()));
        if (systemConfigUpdate.getNeedCache() == 1) {
            cacheSystemConfig();
        }
        return actionResultService.callBackResult("修改成功");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateSystemConfig(List<SystemConfig> listSystemConfig) {
        boolean isCache = false;
        for (SystemConfig systemConfig : listSystemConfig) {
            if (systemConfig.getNeedCache() == 1) {
                isCache = true;
            }
            systemConfigMapper.update(systemConfig,
                Wrappers.lambdaQuery(SystemConfig.class).eq(SystemConfig::getSycGroup, systemConfig.getSycGroup())
                    .eq(SystemConfig::getSycKey, systemConfig.getSycKey()));
        }
        if (isCache) {
            cacheSystemConfig();
        }
        return actionResultService.callBackResult("修改成功");
    }

    @Override
    public SystemConfig getSystemConfigByGroupAndKeyByDetail(SystemConfig systemConfig) {
        final SystemConfig systemConfigDetail = systemConfigMapper.selectOne(
            Wrappers.lambdaQuery(SystemConfig.class).eq(SystemConfig::getSycGroup, systemConfig.getSycGroup())
                .eq(SystemConfig::getSycKey, systemConfig.getSycKey()));
        if (systemConfigDetail != null && systemConfigDetail.getNeedPwd() == 1) {
            systemConfigDetail.setSycValue("******");
        }
        return systemConfigDetail;
    }

    @Override
    public ActionResult<SystemConfig, String>
        getSystemConfigByGroupAndKey(SystemConfigGroupAndKeyDTO systemConfigGroupAndKeyDTO) {
        final SystemConfig systemConfig = systemConfigMapper.selectOne(Wrappers.lambdaQuery(SystemConfig.class)
            .eq(SystemConfig::getSycGroup, systemConfigGroupAndKeyDTO.getSycGroup())
            .eq(SystemConfig::getSycKey, systemConfigGroupAndKeyDTO.getSycKey()));
        if (systemConfig != null && systemConfig.getNeedPwd() == 1) {
            if (CharSequenceUtil.isNotBlank(systemConfigGroupAndKeyDTO.getLoginPwd())) {
                final ActionResult<String, String> actionResult =
                    pwdComponent.checkUserLoginPassword(systemConfigGroupAndKeyDTO);
                if (!actionResult.getStatus().isSuccess()) {
                    return actionResultService.callBackResultFail(actionResult.getStatus().getCode(),
                        actionResult.getStatus().getMessage());
                }
            } else {
                return actionResultService.callBackResultFail("密码不能为空");
            }

        }
        return actionResultService.callBackResult(systemConfig);
    }

    @Override
    public List<SystemConfig> listSystemConfigByCondition(SystemConfigQueryDTO systemConfigQueryDTO) {
        final List<SystemConfig> listSystemConfig =
            systemConfigMapper.listSystemConfigByCondition(systemConfigQueryDTO);
        for (SystemConfig systemConfig : listSystemConfig) {
            if (systemConfig.getNeedPwd() == 1) {
                systemConfig.setSycValue("******");
            }
        }
        return listSystemConfig;
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
