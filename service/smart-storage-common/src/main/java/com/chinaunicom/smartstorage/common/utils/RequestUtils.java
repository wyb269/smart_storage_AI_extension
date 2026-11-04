package com.chinaunicom.smartstorage.common.utils;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.chinaunicom.smartstorage.common.exception.bean.CustomException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @author liugang
 * @since 2025/12/5 14:10
 */
public class RequestUtils {
    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            throw new CustomException("Request not found");
        }
        return ((ServletRequestAttributes)requestAttributes).getRequest();
    }
}
