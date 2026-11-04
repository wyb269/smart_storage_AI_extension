package com.chinaunicom.smartstorage.admin.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.chinaunicom.smartstorage.admin.entity.rbac.Rights;
import com.chinaunicom.smartstorage.admin.entity.rbac.menu.Menu;
import com.chinaunicom.smartstorage.admin.entity.rbac.role.Role;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;
import com.chinaunicom.smartstorage.admin.mapper.rbac.UserMapper;
import com.chinaunicom.smartstorage.common.controller.BaseController;

import cn.hutool.core.convert.Convert;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户身份认证
 *
 * @author liuchink
 * @since 2020/6/9 15:30
 */
@Component("loginUserDetailsServiceImpl")
@Slf4j
public class LoginUserDetailsServiceImpl extends BaseController implements UserDetailsService {
    @Resource
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String loginId) {
        User userLogin = userMapper.login(loginId);
        if (userLogin == null) {
            throw new UsernameNotFoundException("用户或密码错误");
        }
        List<GrantedAuthority> authList = getAuthorities(userLogin);
        return new org.springframework.security.core.userdetails.User(loginId, userLogin.getLoginPwd(),
            userLogin.getShowState() == 1, true, true, true, authList);
    }

    /**
     * 权限信息
     *
     * @param user user
     * @return java.util.List
     * @author liuchink
     * @since 2020/9/4 16:07
     */
    private List<GrantedAuthority> getAuthorities(User user) {
        List<GrantedAuthority> authList = new ArrayList<>();
        for (Role role : user.getRoleGroup().getListRole()) {
            for (Menu menu : role.getListMenu()) {
                for (Rights rightsInfo : menu.getListRights()) {
                    authList.add(new SimpleGrantedAuthority(Convert.toStr(rightsInfo.getId())));
                }
            }
        }
        return authList;
    }
}
