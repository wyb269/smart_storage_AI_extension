package com.chinaunicom.smartstorage.applet.filter;

import cn.hutool.core.util.IdUtil;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;

import java.io.IOException;

/**
 * @author liugang
 * @since 2025/11/19 15:06
 */
@WebFilter(filterName = "MDCFilter", urlPatterns = "/*", asyncSupported = true)
@Order(1)
@Slf4j
public class MDCFilter implements Filter {
    private static final String REQUEST_ID_HEADER = "request_id";

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        String requestId = httpRequest.getHeader(REQUEST_ID_HEADER);
        if (requestId == null) {
            requestId = IdUtil.simpleUUID();
        }
        MDC.put(SystemConstant.LOG_REQUEST_ID, requestId);
        httpResponse.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    @Override
    public void destroy() {}

}
