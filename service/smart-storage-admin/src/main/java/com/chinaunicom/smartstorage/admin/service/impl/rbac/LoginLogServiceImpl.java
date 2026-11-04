package com.chinaunicom.smartstorage.admin.service.impl.rbac;

import java.util.List;

import org.springframework.stereotype.Service;

import com.chinaunicom.smartstorage.admin.dto.rbac.login.LoginLogQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.LoginLog;
import com.chinaunicom.smartstorage.admin.mapper.sso.LoginLogMapper;
import com.chinaunicom.smartstorage.admin.service.inter.rbac.LoginLogService;

import jakarta.annotation.Resource;

/**
 * @author liuchink
 * @since 2024/08/14 10:56
 */
@Service
public class LoginLogServiceImpl implements LoginLogService {
    @Resource
    private LoginLogMapper loginLogMapper;

    @Override
    public List<LoginLog> listLoginLogByConditionOnPage(LoginLogQueryReqDTO loginLogQueryReqDTO) {
        return loginLogMapper.listLoginLogByConditionOnPage(loginLogQueryReqDTO);
    }
}
