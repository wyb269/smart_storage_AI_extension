package com.chinaunicom.smartstorage.admin.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.exception.bean.CustomExceptionCode;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 匿名用户访问无权限资源
 *
 * @author liuchink
 * @since 2021/3/22 16:32
 */
@Component
@Slf4j
public class MyAuthenticationEntryPoint extends BaseController implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
        AuthenticationException authenticationException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, authenticationException.getMessage(), authenticationException);
        ActionResult<String, String> actionResult = actionResultService.callBackResult(false,
            CustomExceptionCode.ACCESS_DENIED_EXCEPTION_CODE, CustomExceptionCode.ACCESS_DENIED_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }
}
