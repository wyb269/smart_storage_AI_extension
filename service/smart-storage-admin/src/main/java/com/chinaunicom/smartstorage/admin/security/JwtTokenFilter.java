package com.chinaunicom.smartstorage.admin.security;

import cn.hutool.core.text.CharSequenceUtil;
import com.google.common.collect.Lists;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.config.SecurityIgnoreConfiguration;
import com.chinaunicom.smartstorage.admin.constant.TokenWhiteUrlConstant;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.exception.bean.CustomExceptionCode;
import com.chinaunicom.smartstorage.common.jwt.JwtConstant;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.liuchink.chinkutils.json.ChinkFastJson2Utils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

/**
 * token过滤器
 *
 * @author liuchink
 * @since 2020/9/11 13:24
 */
@Component
@Order(2)
@Slf4j
public class JwtTokenFilter extends OncePerRequestFilter {
    @Resource
    private ActionResultService actionResultService;
    @Resource(name = "tokenUserDetailsServiceImpl")
    private UserDetailsService userDetailsService;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest httpServletRequest,
        @NonNull HttpServletResponse httpServletResponse, @NonNull FilterChain filterChain) {
        httpServletResponse.setContentType("application/json;charset=UTF-8");
        ActionResult<String, String> actionResult;
        try {
            String token = httpServletRequest.getHeader(JwtConstant.REQUEST_HEAD_TOKEN);
            if (CharSequenceUtil.isBlank(token)) {
                actionResult = actionResultService.callBackResultFail(CustomExceptionCode.JWT_HEADER_EXCEPTION_CODE,
                    CustomExceptionCode.JWT_HEADER_EXCEPTION_CODE_MESSAGE);
                httpServletResponse.getWriter().append(ChinkFastJson2Utils.toJsonStringByNoNull(actionResult));
                return;
            }
            User user = jwtTokenComponent.getUser();
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getLoginId());
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } catch (Exception exception) {
            log.error(SystemConstant.LOG_ERROR_TITLE, exception.getMessage(), exception);
            handlerExceptionResolver.resolveException(httpServletRequest, httpServletResponse, null, exception);
        }

    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest httpServletRequest) {
        List<String> pathList = Lists.newArrayList();
        pathList.addAll(TokenWhiteUrlConstant.KNIFE4J_PATH);
        pathList.addAll(TokenWhiteUrlConstant.TOKEN_PASS_PATH);
        pathList.addAll(SecurityIgnoreConfiguration.getSecurityIgnorePaths());
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        for (String url : pathList) {
            if (antPathMatcher.match(url, httpServletRequest.getServletPath())) {
                return true;
            }
        }
        return false;
    }
}
