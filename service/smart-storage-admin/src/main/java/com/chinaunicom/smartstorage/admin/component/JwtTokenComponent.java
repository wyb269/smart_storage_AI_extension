package com.chinaunicom.smartstorage.admin.component;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.exception.bean.CustomExceptionCode;
import com.chinaunicom.smartstorage.common.jwt.JwtConstant;
import com.chinaunicom.smartstorage.common.jwt.JwtTokenUtils;

import cn.hutool.core.text.CharSequenceUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * @author liuchink
 * @since 2024/08/22 10:35
 */
@Component
@Slf4j
public class JwtTokenComponent {
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private HttpServletRequest httpServletRequest;

    public User getUser() {
        final String userInfo =
            redisTemplate.opsForValue().get(SystemConstant.ADMIN_LOGIN_ACCESS_TOKEN_KEY + JwtTokenUtils.getSubject(
                httpServletRequest.getHeader(JwtConstant.REQUEST_HEAD_TOKEN), JwtConstant.JWT_ADMIN_SECRET));
        if (CharSequenceUtil.isBlank(userInfo)) {
            throw new CustomException(CustomExceptionCode.USER_LOGIN_EXPIRE_EXCEPTION_CODE,
                CustomExceptionCode.USER_LOGIN_EXPIRE_EXCEPTION_CODE_MESSAGE);
        }
        return JSON.parseObject(userInfo, User.class);
    }
}
