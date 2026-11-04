package com.chinaunicom.smartstorage.admin.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.chinaunicom.smartstorage.admin.constant.AdminSystemConstant;
import com.chinaunicom.smartstorage.common.exception.bean.VerifyCodeException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 验证码过滤器
 *
 * @author liuchink
 * @since 2020/9/11 13:24
 */
@Component
@Slf4j
public class VerifyCodeFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest httpServletRequest,
        @NonNull HttpServletResponse httpServletResponse, @NonNull FilterChain filterChain)
        throws ServletException, IOException {
        // if (SystemConstant.MANAGE_LOGIN_URL.equals(httpServletRequest.getServletPath())
        // && "post".equalsIgnoreCase(httpServletRequest.getMethod())) {
        // try {
        // validate(httpServletRequest);
        // } catch (VerifyCodeException verifyCodeException) {
        // ActionResult<String, String> actionResult =
        // actionResultService.callBackResultFail( verifyCodeException.getMessage());
        // try {
        // httpServletResponse.setContentType("application/json;charset=UTF-8");
        // httpServletResponse.getWriter().append(ChinkFastJson2Utils.toJsonStringByNoNull(actionResult));
        // } catch (IOException ioException) {
        // log.error(SystemConstant.LOG_ERROR_TITLE, ioException.getMessage(), ioException);
        // throw new CustomException(ioException);
        // }
        // return;
        // }
        // }
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    /**
     * 验证码核对
     *
     * @param request request
     * @author liuchink
     * @since 2020/9/11 15:06
     */
    private void validate(HttpServletRequest request) throws ServletRequestBindingException, VerifyCodeException {
        String loginValidateCode =
            ServletRequestUtils.getStringParameter(request, AdminSystemConstant.SESSION_VER_CODE);
        String sessionValidateCode =
            (String)request.getSession().getAttribute(AdminSystemConstant.SESSION_VER_CODE);
        // 测试专用
        // String[] activeProfiles = springUtils.getApplicationContext()
        // .getEnvironment().getActiveProfiles();
        // for (String activeProfile : activeProfiles) {
        // if ("dev".equals(activeProfile)) {
        // sessionValidateCode = "123";
        // break;
        // }
        // }
        if (sessionValidateCode == null || !sessionValidateCode.equalsIgnoreCase(loginValidateCode)) {
            request.getSession().removeAttribute(AdminSystemConstant.SESSION_VER_CODE);
            throw new VerifyCodeException();
        }
        request.getSession().removeAttribute(AdminSystemConstant.SESSION_VER_CODE);
    }
}
