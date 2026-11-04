package com.chinaunicom.smartstorage.admin.mapper.sso;


import com.github.yulichang.base.MPJBaseMapper;
import com.chinaunicom.smartstorage.admin.dto.rbac.login.LoginLogQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.LoginLog;

import java.util.List;

/**
 * 菜单信息
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */

public interface LoginLogMapper extends MPJBaseMapper<LoginLog> {
    List<LoginLog> listLoginLogByConditionOnPage(LoginLogQueryReqDTO loginLogQueryReqDTO);
}
