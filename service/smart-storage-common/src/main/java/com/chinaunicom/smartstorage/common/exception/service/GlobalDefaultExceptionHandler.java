package com.chinaunicom.smartstorage.common.exception.service;

import cn.hutool.core.io.resource.NoResourceException;
import com.alibaba.fastjson2.JSONException;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.exception.bean.CustomExceptionCode;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.liuchink.chinkutils.json.ChinkFastJson2Utils;
import com.liuchink.chinkutils.net.ChinkHttpMediaType;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 全局异常处理
 *
 * @author liuchink
 * @since 2018/8/23 11:28
 */
@ControllerAdvice
@Slf4j
public class GlobalDefaultExceptionHandler {
    @Resource
    private HttpServletResponse httpServletResponse;
    @Resource
    private HttpServletRequest httpServletRequest;
    @Resource
    private ActionResultService actionResultService;

    /**
     * 转换成JSON字符串回写到页面
     *
     * @param object object
     * @author liuchink
     * @since 2017/05/18 11:28
     */
    protected void writerToPageByJson(Object object) {
        writerToPageByJson(ChinkFastJson2Utils.toJsonString(object));
    }

    /**
     * 将字符串回写到页面
     *
     * @param str str
     * @author liuchink
     * @since 2017/05/18 11:31
     */

    protected void writerToPageByJson(String str) {
        httpServletResponse.setContentType(ChinkHttpMediaType.APPLICATION_JSON.toString());
        try (ServletOutputStream servletOutputStream = httpServletResponse.getOutputStream()) {
            servletOutputStream.write(str.getBytes(StandardCharsets.UTF_8));

        } catch (IOException ioException) {
            throw new CustomException(ioException);
        }
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public void defultExcepitonHandler(Exception exception) {
        log.error(SystemConstant.LOG_ERROR_TITLE, exception.getMessage(), exception);
        ActionResult<String, String> actionResult = actionResultService.callBackResultFail(
            CustomExceptionCode.DEFAULT_EXCEPTION_CODE, CustomExceptionCode.DEFAULT_EXCEPTION_MESSAGE);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(CustomException.class)
    @ResponseBody
    public void customException(CustomException customException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, customException.getErrorMsg(), customException);
        ActionResult<String, String> actionResult =
            actionResultService.callBackResultFail(customException.getErrorCode(), customException.getErrorMsg());
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseBody
    public void missingRequestHeaderException(Exception exception) {
        log.error(SystemConstant.LOG_ERROR_TITLE, exception.getMessage(), exception);
        ActionResult<String, String> actionResult =
            actionResultService.callBackResultFail(CustomExceptionCode.MISSING_REQUEST_HEADER_EXCEPTION_CODE,
                CustomExceptionCode.MISSING_REQUEST_HEADER_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    @ResponseBody
    public void expiredJwtException(Exception exception) {
        log.error(SystemConstant.LOG_ERROR_TITLE, exception.getMessage(), exception);
        ActionResult<String, String> actionResult = actionResultService.callBackResultFail(
            CustomExceptionCode.JWT_EXPIRED_EXCEPTION_CODE, CustomExceptionCode.JWT_EXPIRED_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(JwtException.class)
    @ResponseBody
    public void jwtException(Exception exception) {
        log.error(SystemConstant.LOG_ERROR_TITLE, exception.getMessage(), exception);
        ActionResult<String, String> actionResult = actionResultService
            .callBackResultFail(CustomExceptionCode.JWT_EXCEPTION_CODE, CustomExceptionCode.JWT_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public void methodArgumentNotValidException(MethodArgumentNotValidException methodArgumentNotValidException) {
        String errorMsg = "参数校验未定义错误信息";
        log.error(SystemConstant.LOG_ERROR_TITLE, methodArgumentNotValidException.getMessage(),
            methodArgumentNotValidException);
        BindingResult bindingResult = methodArgumentNotValidException.getBindingResult();
        if (bindingResult.getFieldError() != null) {
            errorMsg = bindingResult.getFieldError().getDefaultMessage();
        }
        ActionResult<String, String> actionResult = actionResultService
            .callBackResultFail(CustomExceptionCode.METHOD_ARGUMENT_NOT_VALID_EXCEPTION_CODE, errorMsg);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public void illegalArgumentException(Exception exception) {
        log.error(SystemConstant.LOG_ERROR_TITLE, exception.getMessage(), exception);
        ActionResult<String, String> actionResult =
            actionResultService.callBackResultFail(CustomExceptionCode.ILLEGAL_ARGUMENT_EXCEPTION_CODE,
                CustomExceptionCode.ILLEGAL_ARGUMENT_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(JSONException.class)
    @ResponseBody
    public void fastJsonException(JSONException jsonException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, jsonException.getMessage(), jsonException);
        ActionResult<String, String> actionResult = actionResultService.callBackResultFail(
            CustomExceptionCode.JSON_PARSE_EXCEPTION_CODE, CustomExceptionCode.JSON_PARSE_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public void httpMessageNotReadableException(HttpMessageNotReadableException httpMessageNotReadableException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, httpMessageNotReadableException.getMessage(),
            httpMessageNotReadableException);
        ActionResult<String, String> actionResult = actionResultService.callBackResultFail(
            CustomExceptionCode.HTTP_MESSAGE_EXCEPTION_CODE, CustomExceptionCode.HTTP_MESSAGE_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseBody
    public void noHandlerFoundException(NoHandlerFoundException noHandlerFoundException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, noHandlerFoundException.getMessage(), noHandlerFoundException);
        ActionResult<String, String> actionResult =
            actionResultService.callBackResultFail(CustomExceptionCode.NO_HANDLER_FOUND_EXCEPTION_CODE,
                CustomExceptionCode.NO_HANDLER_FOUND_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseBody
    public void nullPointerException(NullPointerException nullPointerException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, nullPointerException.getMessage(), nullPointerException);
        ActionResult<String, String> actionResult = actionResultService.callBackResultFail(
            CustomExceptionCode.NULL_POINTER_EXCEPTION_CODE, CustomExceptionCode.NULL_POINTER_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(IndexOutOfBoundsException.class)
    @ResponseBody
    public void indexOutOfBoundsException(IndexOutOfBoundsException indexOutOfBoundsException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, indexOutOfBoundsException.getMessage(), indexOutOfBoundsException);
        ActionResult<String, String> actionResult =
            actionResultService.callBackResultFail(CustomExceptionCode.INDEX_OUT_OF_BOUNDS_EXCEPTION_CODE,
                CustomExceptionCode.INDEX_OUT_OF_BOUNDS_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(FileNotFoundException.class)
    @ResponseBody
    public void fileNotFoundException(FileNotFoundException fileNotFoundException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, fileNotFoundException.getMessage(), fileNotFoundException);
        ActionResult<String, String> actionResult =
            actionResultService.callBackResultFail(CustomExceptionCode.FILE_NOT_FOUND_EXCEPTION_CODE,
                CustomExceptionCode.FILE_NOT_FOUND_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseBody
    public void dataIntegrityViolationException(DataIntegrityViolationException dataIntegrityViolationException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, dataIntegrityViolationException.getMessage(),
            dataIntegrityViolationException);
        ActionResult<String, String> actionResult =
            actionResultService.callBackResultFail(CustomExceptionCode.DATA_INTEGRITY_VIOLATION_EXCEPTION_CODE,
                CustomExceptionCode.DATA_INTEGRITY_VIOLATION_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseBody
    public void noResourceFoundException(NoResourceFoundException noResourceFoundException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, noResourceFoundException.getMessage(), noResourceFoundException);
        ActionResult<String, String> actionResult =
            actionResultService.callBackResultFail(CustomExceptionCode.NO_HANDLER_FOUND_EXCEPTION_CODE,
                CustomExceptionCode.NO_HANDLER_FOUND_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseBody
    public void handlerMethodValidationException(HandlerMethodValidationException handlerMethodValidationException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, handlerMethodValidationException.getMessage());
        ActionResult<String,
            String> actionResult =
                actionResultService
                    .callBackResultFail(CustomExceptionCode.METHOD_ARGUMENT_NOT_VALID_EXCEPTION_CODE,
                        handlerMethodValidationException.getParameterValidationResults().stream().findFirst()
                            .map(validationResult -> validationResult.getResolvableErrors().stream().findFirst()
                                .map(MessageSourceResolvable::getDefaultMessage).orElse("未配置校验错误信息"))
                            .orElse("未配置校验错误信息"));

        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    public void httpRequestMethodNotSupportedException(
        HttpRequestMethodNotSupportedException httpRequestMethodNotSupportedException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, httpRequestMethodNotSupportedException.getMessage());
        ActionResult<String,
            String> actionResult = actionResultService.callBackResultFail(
                CustomExceptionCode.HTTP_PREQUEST_METHOD_NOT_SUPPORTED_EXCEPTION_CODE,
                CustomExceptionCode.HTTP_PREQUEST_METHOD_NOT_SUPPORTED_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(NoResourceException.class)
    @ResponseBody
    public void noResourceException(NoResourceException noResourceException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, noResourceException.getMessage(), noResourceException);
        ActionResult<String, String> actionResult =
            actionResultService.callBackResultFail(CustomExceptionCode.NO_HANDLER_FOUND_EXCEPTION_CODE,
                CustomExceptionCode.NO_HANDLER_FOUND_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public void constraintViolationException(ConstraintViolationException constraintViolationException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, constraintViolationException.getMessage(),
            constraintViolationException);
        ActionResult<String, String> actionResult =
            actionResultService.callBackResultFail(CustomExceptionCode.METHOD_ARGUMENT_NOT_VALID_EXCEPTION_CODE,
                constraintViolationException.getConstraintViolations().stream().map(ConstraintViolation::getMessage)
                    .findFirst().orElse("未配置校验错误信息"));
        writerToPageByJson(actionResult);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public void
        methodArgumentTypeMismatchException(MethodArgumentTypeMismatchException methodArgumentTypeMismatchException) {
        log.error(SystemConstant.LOG_ERROR_TITLE, methodArgumentTypeMismatchException.getMessage(),
            methodArgumentTypeMismatchException);
        ActionResult<String, String> actionResult =
            actionResultService.callBackResultFail(CustomExceptionCode.ILLEGAL_ARGUMENT_EXCEPTION_CODE,
                CustomExceptionCode.ILLEGAL_ARGUMENT_EXCEPTION_CODE_MESSAGE);
        writerToPageByJson(actionResult);
    }
    // @ExceptionHandler(AccessDeniedException.class)
    // @ResponseBody
    // public void accessDeniedException(AccessDeniedException accessDeniedException) {
    // log.error(SystemConstant.LOG_ERROR_TITLE, accessDeniedException.getMessage(), accessDeniedException);
    // ActionResult<String, String> actionResult = actionResultService.callBackResult(false,
    // CustomExceptionCode.ACCESS_DENIED_EXCEPTION_CODE, CustomExceptionCode.ACCESS_DENIED_EXCEPTION_CODE_MESSAGE);
    // writerToPageByJson(actionResult);
    // }
}
