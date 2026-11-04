package com.chinaunicom.smartstorage.common.jwt;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.exception.bean.CustomExceptionCode;
import com.liuchink.chinkutils.date.ChinkDateOperationUtils;
import com.liuchink.chinkutils.date.ChinkDateValueUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 *
 * @author liuchink
 * @since 2021/3/28 9:40
 */
@Slf4j
public class JwtUtils {

    /**
     * Token加密解密的密码
     */
    private final String tokenSecret;

    public JwtUtils(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    public static void main(String[] args) {
        JwtUtils jwtUtils = new JwtUtils(JwtConstant.JWT_ADMIN_SECRET);
        final String token = jwtUtils.createToken("sub");
        log.info("token:{}", token);
    }

    private SecretKey generateKey() {
        return Keys.hmacShaKeyFor(tokenSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建token
     *
     * @param sub token所面向的用户
     * @param aud 接收token的一方
     * @param jti token的唯一身份标识，主要用来作为一次性token,从而回避重放攻击
     * @param iss token签发者
     * @param map 自定义信息的存储
     * @return 加密后的token字符串
     * @author liuchink
     * @since 2021/10/8 12:41
     */
    public String createToken(String iss, String sub, String aud, Date exp, Date nbf, Date iat, String jti,
        Map<String, Object> map) {
        final JwtBuilder builder = Jwts.builder();
        if (exp == null) {
            exp = ChinkDateOperationUtils
                .addSeconds(ChinkDateValueUtils.getNowDateTime().toDate(), JwtConstant.TOKEN_EXPIRE_TIME).toDate();
        }
        if (nbf == null) {
            nbf = ChinkDateValueUtils.getNowDateTime().toDate();
        }
        if (iat == null) {
            iat = ChinkDateValueUtils.getNowDateTime().toDate();
        }
        if (map != null && !map.isEmpty()) {
            builder.claims(map);
        }
        return builder.signWith(generateKey()).subject(sub).audience().add(aud).and().id(jti).issuer(iss).notBefore(nbf)
            .issuedAt(iat).expiration(exp).compact();
    }

    /**
     * 创建token
     *
     * @param sub token所面向的用户
     * @return token 字符串
     * @author liuchink
     * @since 2021/10/8 12:41
     */
    public String createToken(String sub) {
        return createToken(null, sub, null, null, null, null, IdUtil.simpleUUID(), null);
    }

    /**
     * 创建token
     *
     * @param sub token所面向的用户
     * @return token 字符串
     * @author liuchink
     * @since 2021/10/8 12:41
     */
    public String createToken(String iss, String sub) {
        return createToken(iss, sub, null, null, null, null, IdUtil.simpleUUID(), null);
    }

    /**
     * 创建token
     *
     * @param sub token所面向的用户
     * @param aud 接收token的一方
     * @return token 字符串
     * @author liuchink
     * @since 2021/10/8 12:41
     */
    public String createToken(String iss, String sub, String aud) {
        return createToken(iss, sub, aud, null, null, null, IdUtil.simpleUUID(), null);
    }

    /**
     * 创建token
     *
     * @param sub token所面向的用户
     * @param aud 接收token的一方
     * @return token 字符串
     * @author liuchink
     * @since 2021/10/8 12:41
     */
    public String createToken(String iss, String sub, String aud, Date exp) {
        return createToken(iss, sub, aud, exp, null, null, IdUtil.simpleUUID(), null);
    }

    /**
     * 创建token
     *
     * @param sub token所面向的用户
     * @param aud 接收token的一方
     * @return token 字符串
     * @author liuchink
     * @since 2021/10/8 12:41
     */
    public String createToken(String iss, String sub, String aud, Date exp, Date nbf, Date iat) {
        return createToken(iss, sub, aud, exp, nbf, iat, IdUtil.simpleUUID(), null);
    }

    /**
     * 创建token
     *
     * @param sub token所面向的用户
     * @param aud 接收token的一方
     * @return token 字符串
     * @author liuchink
     * @since 2021/10/8 12:41
     */
    public String createToken(String iss, String sub, String aud, Date exp, Date nbf, Date iat, String jti) {
        return createToken(iss, sub, aud, exp, nbf, iat, jti, null);
    }

    /**
     * 创建token
     *
     * @param sub token所面向的用户
     * @param aud 接收token的一方
     * @return token 字符串
     * @author liuchink
     * @since 2021/10/8 12:41
     */
    public String createToken(String iss, String sub, String aud, Map<String, Object> map) {
        return createToken(iss, sub, aud, null, null, null, IdUtil.simpleUUID(), map);
    }

    /**
     * 创建token
     *
     * @param sub token所面向的用户
     * @param aud 接收token的一方
     * @return token 字符串
     * @author liuchink
     * @since 2021/10/8 12:41
     */
    public String createToken(String iss, String sub, String aud, Date exp, Map<String, Object> map) {
        return createToken(iss, sub, aud, exp, null, null, IdUtil.simpleUUID(), map);
    }

    /**
     * 解析token 可根据Jws<Claims> 获取 header|body|getSignature三部分数据
     *
     * @param token token字符串
     * @return Jws
     * @author liuchink
     * @since 2021/10/8 12:41
     */
    public Jws<Claims> parseToken(String token) {
        // 移除 token 前的"Bearer#"字符串
        token = StringUtils.substringAfter(token, JwtConstant.JWT_SEPARATOR);
        if (CharSequenceUtil.isBlank(token)) {
            throw new CustomException(CustomExceptionCode.JWT_HEADER_EXCEPTION_CODE,
                CustomExceptionCode.JWT_HEADER_EXCEPTION_CODE_MESSAGE);
        }
        // 解析 token 字符串
        return Jwts.parser().verifyWith(generateKey()).build().parseSignedClaims(token);
    }

    /**
     * 解析token 可根据Jws<Claims> 获取 header|body|getSignature三部分数据
     *
     * @param token token字符串
     * @return Jws
     * @author liuchink
     * @since 2021/10/8 12:41
     */
    public Jws<Claims> parseTokenNoBearer(String token) {
        if (CharSequenceUtil.isBlank(token)) {
            throw new CustomException(CustomExceptionCode.JWT_HEADER_EXCEPTION_CODE,
                CustomExceptionCode.JWT_HEADER_EXCEPTION_CODE_MESSAGE);
        }
        // 解析 token 字符串
        return Jwts.parser().verifyWith(generateKey()).build().parseSignedClaims(token);
    }
}
