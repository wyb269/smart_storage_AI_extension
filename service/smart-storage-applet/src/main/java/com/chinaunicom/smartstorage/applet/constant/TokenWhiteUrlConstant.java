package com.chinaunicom.smartstorage.applet.constant;

import com.google.common.collect.ImmutableSet;

/**
 * @author liuchink
 * @since 2022/2/9 10:35
 */
public class TokenWhiteUrlConstant {

    /**
     * knife4j资源路径
     */
    public static final ImmutableSet<String> KNIFE4J_PATH = ImmutableSet.of("/static/**", "/webjars/**",
        "/swagger-resources", "/doc.html", "/v2/api-docs/**", "/favicon.ico", "/v3/api-docs/**");
    /**
     * token不检查
     */
    public static final ImmutableSet<String> TOKEN_PASS_PATH = ImmutableSet.of("/error");

    private TokenWhiteUrlConstant() {

    }
}
