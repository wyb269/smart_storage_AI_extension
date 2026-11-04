package com.chinaunicom.smartstorage.admin.exception;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.exception.bean.CustomExceptionCode;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import lombok.extern.slf4j.Slf4j;

/**
 * @author liuchink
 * @since 2023/12/25 09:56
 */
@Slf4j
@ControllerAdvice
public class LocalExceptionHandler extends BaseController {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    public void accessDeniedException(AccessDeniedException accessDeniedException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, accessDeniedException.getMessage(), accessDeniedException);
        ActionResult<String, String> actionResult = actionResultService.callBackResult(false,
            CustomExceptionCode.ACCESS_DENIED_EXCEPTION_CODE, CustomExceptionCode.ACCESS_DENIED_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseBody
    public void authenticationException(AuthenticationException authenticationException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, authenticationException.getMessage(), authenticationException);
        ActionResult<String, String> actionResult = actionResultService.callBackResult(false,
            CustomExceptionCode.ACCESS_DENIED_EXCEPTION_CODE, CustomExceptionCode.ACCESS_DENIED_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }
}
