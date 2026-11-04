package com.chinaunicom.smartstorage.admin.security;

import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.liuchink.chinkutils.json.ChinkFastJson2Utils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 登录失败
 *
 * @author liuchink
 * @since 2020/6/11 9:32
 */
@Component
@Slf4j
public class UserLoginAuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Resource
    private ActionResultService actionResultService;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void onAuthenticationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
        AuthenticationException authenticationException) {
        httpServletResponse.setContentType("application/json;charset=UTF-8");
        ActionResult<String, String> actionResult;
        String loginId = httpServletRequest.getParameter("loginId");
        String loginFailCountKey = "login" + ":" + "fail" + ":" + "count" + ":" + "key" + ":" + loginId;
        String loginFailLockKey = "login" + ":" + "fail" + ":" + "lock" + ":" + "key" + ":" + loginId;
        String errorMessage = getAuthenticationException(authenticationException);
        if (redisTemplate.hasKey(loginFailCountKey)) {
            redisTemplate.delete(loginFailCountKey);
            actionResult = actionResultService.callBackResultFail("密码输入错误次数过多，请稍后再试");

        } else {
            Long incr = redisTemplate.opsForValue().increment(loginFailCountKey);
            if (incr != null && incr >= 3) {
                redisTemplate.opsForValue().set(loginFailLockKey, loginId, 30, TimeUnit.SECONDS);
            }
            actionResult = actionResultService.callBackResultFail(errorMessage);
        }
        try {
            httpServletResponse.getWriter().append(ChinkFastJson2Utils.toJsonStringByNoNull(actionResult));
        } catch (IOException ioException) {
            throw new CustomException(ioException);
        }
    }

    /**
     * 登录异常转换
     *
     * @param authenticationException authenticationException
     * @return java.lang.String
     * @author liuchink
     * @since 2020/12/26 21:09
     */
    private String getAuthenticationException(AuthenticationException authenticationException) {
        if (authenticationException instanceof BadCredentialsException) {
            return "用户名或密码错误";
        }
        if (authenticationException instanceof AccountExpiredException) {
            return "账号已过期，请联系管理员";
        }
        if (authenticationException instanceof LockedException) {
            return "账号已被锁定，请联系管理员";
        }
        if (authenticationException instanceof DisabledException) {
            return "账号已被停用，请联系管理员";
        }
        if (authenticationException instanceof UsernameNotFoundException) {
            return authenticationException.getMessage();
        }
        return "登录验证异常";
    }
}
