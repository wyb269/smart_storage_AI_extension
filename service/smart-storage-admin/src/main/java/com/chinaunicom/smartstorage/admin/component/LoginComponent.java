package com.chinaunicom.smartstorage.admin.component;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.constant.AdminSystemConstant;
import com.chinaunicom.smartstorage.admin.entity.rbac.Rights;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.LoginLog;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;
import com.chinaunicom.smartstorage.admin.mapper.rbac.RightsMapper;
import com.chinaunicom.smartstorage.admin.mapper.rbac.UserMapper;
import com.chinaunicom.smartstorage.admin.mapper.sso.LoginLogMapper;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.jwt.JwtConstant;
import com.chinaunicom.smartstorage.common.jwt.JwtUtils;
import com.chinaunicom.smartstorage.common.utils.IpUtils;
import com.liuchink.chinkutils.date.ChinkDateOperationUtils;
import com.liuchink.chinkutils.json.ChinkFastJson2Utils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author liuchink
 * @since 2024/08/22 10:35
 */
@Component
@Slf4j
public class LoginComponent {
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private HttpServletRequest httpServletRequest;
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private UserMapper userMapper;
    @Resource
    private LoginLogMapper loginLogMapper;
    @Resource
    private RightsMapper rightsMapper;

    public void saveLoginRightsCache(User user) {
        List<Rights> listRights = rightsMapper.listRightsByRoleGroupId(user.getRoleGroupId());
        if (listRights != null && !listRights.isEmpty()) {
            String key = AdminSystemConstant.REDIS_MANAG_RIGHT_KEY + user.getLoginId();
            redisTemplate.opsForValue().set(key, ChinkFastJson2Utils.toJsonString(listRights),
                JwtConstant.TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
        }
    }

    public String createAccessToken(User user) {
        String tokenSub = IdUtil.fastSimpleUUID();
        JwtUtils accessTokenJwtUtils = new JwtUtils(JwtConstant.JWT_ADMIN_SECRET);
        redisTemplate.opsForValue().set(SystemConstant.ADMIN_LOGIN_ACCESS_TOKEN_KEY + tokenSub,
            ChinkFastJson2Utils.toJsonString(user), JwtConstant.TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
        return accessTokenJwtUtils.createToken(JwtConstant.JWT_ISS_ADMIN, tokenSub, JwtConstant.JWT_AUD_ADMIN);
    }

    public String createRefreshToken(User user) {
        String tokenSub = IdUtil.fastSimpleUUID();
        JwtUtils refreshTokenJwtUtils = new JwtUtils(JwtConstant.JWT_ADMIN_SECRET_REFRESH);
        redisTemplate.opsForValue().set(SystemConstant.ADMIN_LOGIN_REFRESH_TOKEN_KEY + tokenSub,
            ChinkFastJson2Utils.toJsonString(user), JwtConstant.TOKEN_REFRESH_EXPIRE_TIME, TimeUnit.SECONDS);
        return refreshTokenJwtUtils.createToken(JwtConstant.JWT_ISS_ADMIN, tokenSub, JwtConstant.JWT_AUD_ADMIN,
            ChinkDateOperationUtils.addSeconds(new Date(), JwtConstant.TOKEN_REFRESH_EXPIRE_TIME).toDate());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateLastLoginTime(String userId) {
        User userLastLogin = new User();
        userLastLogin.setId(userId);
        userLastLogin.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(userLastLogin);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveLoginLog(User user, String logName, Object loginInfo, int loginState, String stateMessage) {
        LoginLog loginLog = new LoginLog();
        loginLog.setId(Convert.toStr(uidGenerator.getUID()));
        loginLog.setUserId(user.getId());
        loginLog.setLoginId(user.getLoginId());
        loginLog.setAddTime(LocalDateTime.now());
        loginLog.setLogName(logName);
        loginLog.setName(user.getName());
        if (user.getOrganization() != null) {
            loginLog.setOrgId(user.getOrganization().getId());
        }
        if (user.getRegion() != null) {
            loginLog.setRegionId(user.getRegion().getId());
        }
        if (user.getRoleGroup() != null) {
            loginLog.setRoleGroupId(user.getRoleGroup().getId());
        }
        loginLog.setMessage(ChinkFastJson2Utils.toJsonString(loginInfo));
        loginLog.setIp(IpUtils.getRemoteHost(httpServletRequest));
        loginLog.setLoginState(loginState);
        loginLog.setStateMessage(stateMessage);
        loginLogMapper.insert(loginLog);
    }
}
