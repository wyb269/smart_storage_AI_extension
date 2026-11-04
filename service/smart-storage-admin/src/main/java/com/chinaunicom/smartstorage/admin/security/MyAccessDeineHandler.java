package com.chinaunicom.smartstorage.admin.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.exception.bean.CustomExceptionCode;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证过的用户访问无权限资源
 *
 * @author liuchink
 * @since 2021/3/22 16:37
 */
@Component
@Slf4j
public class MyAccessDeineHandler extends BaseController implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
        AccessDeniedException accessDeniedException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, accessDeniedException.getMessage(), accessDeniedException);
        ActionResult<String, String> actionResult = actionResultService.callBackResult(false,
            CustomExceptionCode.ACCESS_DENIED_EXCEPTION_CODE, CustomExceptionCode.ACCESS_DENIED_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }
}
