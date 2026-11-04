package com.chinaunicom.smartstorage.admin.filter;

import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import jakarta.annotation.Resource;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * @author liuchink
 * @since 2024/06/13 09:54
 */
@WebFilter(filterName = "filterDemo", urlPatterns = "/filterDemo1", asyncSupported = true)
@Order(1)
@Slf4j
public class FilterDemo1 implements Filter {
    @Resource
    private HandlerExceptionResolver handlerExceptionResolver;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws ServletException, IOException {
        try {
            throw new CustomException("filterDemo1异常");
            // filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception exception) {
            log.error(SystemConstant.LOG_ERROR_TITLE, exception.getMessage(), exception);
            handlerExceptionResolver.resolveException((HttpServletRequest)servletRequest,
                (HttpServletResponse)servletResponse, null, exception);
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
