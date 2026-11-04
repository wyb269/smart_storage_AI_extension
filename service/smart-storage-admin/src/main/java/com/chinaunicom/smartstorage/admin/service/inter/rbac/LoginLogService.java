package com.chinaunicom.smartstorage.admin.service.inter.rbac;

import java.util.List;

import com.chinaunicom.smartstorage.admin.dto.rbac.login.LoginLogQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.LoginLog;


/**
 * 组织机构
 *
 * @author liuchink
 * @since 2020/4/5 15:08
 */
public interface LoginLogService {

    List<LoginLog> listLoginLogByConditionOnPage(LoginLogQueryReqDTO loginLogQueryReqDTO);

}
