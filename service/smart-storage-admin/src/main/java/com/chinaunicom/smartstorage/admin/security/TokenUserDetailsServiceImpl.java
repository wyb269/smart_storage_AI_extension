package com.chinaunicom.smartstorage.admin.security;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.fastjson2.JSON;
import com.chinaunicom.smartstorage.admin.constant.AdminSystemConstant;
import com.chinaunicom.smartstorage.admin.dto.rbac.login.LoginRightsRespDTO;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.exception.bean.CustomExceptionCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户身份认证
 *
 * @author liuchink
 * @since 2020/6/9 15:30
 */
@Component("tokenUserDetailsServiceImpl")
@Slf4j
public class TokenUserDetailsServiceImpl implements UserDetailsService {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public UserDetails loadUserByUsername(String loginId) {
        String loginRightsStr = redisTemplate.opsForValue().get(AdminSystemConstant.REDIS_MANAG_RIGHT_KEY + loginId);
        if (CharSequenceUtil.isNotBlank(loginRightsStr)) {
            List<LoginRightsRespDTO> listLoginRightsRespDTO = JSON.parseArray(loginRightsStr, LoginRightsRespDTO.class);
            List<GrantedAuthority> authList = getAuthorities(listLoginRightsRespDTO);
            return new User(loginId, "", true, true, true, true, authList);
        } else {
            throw new CustomException(CustomExceptionCode.ACCESS_DENIED_EXCEPTION_CODE, "未找到用户授权信息信息，请先进行授权操作");
        }
        // return new User(loginId, "", true, true, true, true, Lists.newArrayList());
    }

    /**
     * 权限信息
     *
     * @param listLoginRightsRespDTO listLoginRightsRespDTO
     * @return java.util.List
     * @author liuchink
     * @since 2020/9/4 16:07
     */
    private List<GrantedAuthority> getAuthorities(List<LoginRightsRespDTO> listLoginRightsRespDTO) {
        List<GrantedAuthority> authList = new ArrayList<>();
        listLoginRightsRespDTO.forEach(right -> authList.add(new SimpleGrantedAuthority(Convert.toStr(right.getId()))));
        return authList;
    }
}
