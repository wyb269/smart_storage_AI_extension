package com.chinaunicom.smartstorage.applet.interceptor;

import cn.hutool.core.text.CharSequenceUtil;
import com.google.common.collect.Lists;
import com.chinaunicom.smartstorage.applet.aop.PassToken;
import com.chinaunicom.smartstorage.applet.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.applet.constant.TokenWhiteUrlConstant;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.exception.bean.CustomExceptionCode;
import com.chinaunicom.smartstorage.common.jwt.JwtConstant;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.liuchink.chinkutils.json.ChinkFastJson2Utils;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * JWT拦截器
 *
 * @author liuchink
 * @since 2021/9/1 15:21
 */
@Slf4j
@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private JwtTokenComponent jwtTokenComponent;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
        @NonNull Object handler) {
        List<String> pathList = Lists.newArrayList();
        pathList.addAll(TokenWhiteUrlConstant.KNIFE4J_PATH);
        pathList.addAll(TokenWhiteUrlConstant.TOKEN_PASS_PATH);
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        for (String url : pathList) {
            if (antPathMatcher.match(url, request.getServletPath())) {
                return true;
            }
        }
        ActionResult<String, String> actionResult;
        response.setContentType("application/json;charset=UTF-8");
        try {
            String token = request.getHeader(JwtConstant.REQUEST_HEAD_TOKEN);
            if (!(handler instanceof HandlerMethod handlerMethod)) {
                return true;
            }
            Method method = handlerMethod.getMethod();
            // 检查是否有passtoken注释，有则跳过认证
            if (method.isAnnotationPresent(PassToken.class)) {
                PassToken passToken = method.getAnnotation(PassToken.class);
                if (passToken.required()) {
                    return true;
                }
            }
            if (CharSequenceUtil.isBlank(token)) {
                actionResult = actionResultService.callBackResultFail(CustomExceptionCode.JWT_HEADER_EXCEPTION_CODE,
                    CustomExceptionCode.JWT_HEADER_EXCEPTION_CODE_MESSAGE);
                response.getWriter().append(ChinkFastJson2Utils.toJsonStringByNoNull(actionResult));
                return false;
            }
            jwtTokenComponent.getMember();
        } catch (CustomException | ExpiredJwtException customException) {
            log.error(SystemConstant.LOG_ERROR_TITLE, customException.getMessage(), customException);
            throw new CustomException(CustomExceptionCode.JWT_EXPIRED_EXCEPTION_CODE,
                CustomExceptionCode.JWT_EXPIRED_EXCEPTION_CODE_MESSAGE);
        } catch (JwtException jwtException) {
            log.error(SystemConstant.LOG_ERROR_TITLE, jwtException.getMessage(), jwtException);
            throw new CustomException(CustomExceptionCode.JWT_EXCEPTION_CODE,
                CustomExceptionCode.JWT_EXCEPTION_CODE_MESSAGE);
        } catch (IOException ioException) {
            log.error(SystemConstant.LOG_ERROR_TITLE, ioException.getMessage(), ioException);
            throw new CustomException(ioException);
        }
        return true;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
        @NonNull Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
        @NonNull Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
