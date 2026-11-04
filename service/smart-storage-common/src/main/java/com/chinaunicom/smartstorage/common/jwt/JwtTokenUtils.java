package com.chinaunicom.smartstorage.common.jwt;

import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Set;

/**
 * @author liuchink
 * @since 2023/9/28 13:25
 */
@Slf4j
public class JwtTokenUtils {
    private JwtTokenUtils() {

    }

    public static String getIssuer(String token, String tokenSecret) {
        JwtUtils jwtUtils = new JwtUtils(tokenSecret);
        return jwtUtils.parseToken(token).getPayload().getIssuer();
    }

    public static String getSubject(String token, String tokenSecret) {
        JwtUtils jwtUtils = new JwtUtils(tokenSecret);
        return jwtUtils.parseToken(token).getPayload().getSubject();
    }

    public static String getSubjectNoBearer(String token, String tokenSecret) {
        JwtUtils jwtUtils = new JwtUtils(tokenSecret);
        return jwtUtils.parseTokenNoBearer(token).getPayload().getSubject();
    }

    public static Set<String> getAudience(String token, String tokenSecret) {
        JwtUtils jwtUtils = new JwtUtils(tokenSecret);
        return jwtUtils.parseToken(token).getPayload().getAudience();
    }

    public static Date getExpiration(String token, String tokenSecret) {
        JwtUtils jwtUtils = new JwtUtils(tokenSecret);
        return jwtUtils.parseToken(token).getPayload().getExpiration();
    }

    public static Date getIssuedAt(String token, String tokenSecret) {
        JwtUtils jwtUtils = new JwtUtils(tokenSecret);
        return jwtUtils.parseToken(token).getPayload().getIssuedAt();
    }

    public static Date getNotBefore(String token, String tokenSecret) {
        JwtUtils jwtUtils = new JwtUtils(tokenSecret);
        return jwtUtils.parseToken(token).getPayload().getNotBefore();
    }

    public static String getJti(String token, String tokenSecret) {
        JwtUtils jwtUtils = new JwtUtils(tokenSecret);
        return jwtUtils.parseToken(token).getPayload().getId();
    }

}
