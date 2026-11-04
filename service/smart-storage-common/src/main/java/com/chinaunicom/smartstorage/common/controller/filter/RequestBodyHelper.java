package com.chinaunicom.smartstorage.common.controller.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.chinaunicom.smartstorage.common.exception.bean.CustomException;

import cn.hutool.core.text.CharSequenceUtil;
import jakarta.servlet.ServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * RequestBodyHelper
 *
 * @author liuchink
 * @since 2018/11/26 14:17
 */
@Slf4j
public class RequestBodyHelper {

    private RequestBodyHelper() {

    }

    /**
     * 通过request流读取body体内容
     *
     * @param request request
     * @return java.lang.String
     * @author liuchink
     * @since 2018/11/26 14:17
     */
    public static String getBodyString(ServletRequest request) {
        String contentType = request.getContentType();
        String bodyString = "";
        StringBuilder sb = new StringBuilder();
        if (CharSequenceUtil.isNotBlank(contentType)
            && (contentType.contains("multipart/form-data") || contentType.contains("x-www-form-urlencoded"))) {
            Map<String, String[]> parameterMap = request.getParameterMap();
            for (Map.Entry<String, String[]> next : parameterMap.entrySet()) {
                String[] values = next.getValue();
                String value = null;
                if (values != null) {
                    if (values.length == 1) {
                        value = values[0];
                    } else {
                        value = Arrays.toString(values);
                    }
                }
                sb.append(next.getKey()).append("=").append(value).append("&");
            }
            if (sb.length() > 0) {
                bodyString = sb.substring(0, sb.toString().length() - 1);
            }
            return bodyString;
        } else {
            try {
                return IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
            } catch (IOException ioException) {
                throw new CustomException(ioException);
            }
        }
    }
}
