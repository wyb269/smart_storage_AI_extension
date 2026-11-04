package com.chinaunicom.smartstorage.admin.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.liuchink.chinkutils.json.ChinkFastJson2Utils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 登出
 *
 * @author liuchink
 * @since 2020/6/11 9:32
 */
@Component
@Slf4j
public class UserLogoutSuccessHandler implements LogoutSuccessHandler {
    @Resource
    private ActionResultService actionResultService;

    @Override
    public void onLogoutSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
        Authentication authentication) {
        httpServletResponse.setContentType("application/json;charset=UTF-8");
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setHeader("Content-Type", "application/json");
        try {
            ActionResult<String, String> actionResult =
                actionResultService.callBackResult(true, SystemConstant.SUCCESS_STRING_0, "登出成功");
            httpServletResponse.getWriter().append(ChinkFastJson2Utils.toJsonStringByNoNull(actionResult));
        } catch (IOException ioException) {
            throw new CustomException(ioException);
        }
    }
}
