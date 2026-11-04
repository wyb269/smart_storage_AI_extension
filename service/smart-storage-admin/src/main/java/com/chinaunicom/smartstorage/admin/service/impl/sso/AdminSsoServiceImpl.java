package com.chinaunicom.smartstorage.admin.service.impl.sso;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.component.*;
import com.chinaunicom.smartstorage.admin.dto.rbac.login.*;
import com.chinaunicom.smartstorage.admin.component.*;
import com.chinaunicom.smartstorage.admin.dto.rbac.login.*;
import com.chinaunicom.smartstorage.admin.dto.sso.PasswordLoginRepDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.PhoneLoginRepDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.UpdateUserPasswordReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.menu.Menu;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;
import com.chinaunicom.smartstorage.admin.mapper.rbac.MenuMapper;
import com.chinaunicom.smartstorage.admin.mapper.rbac.RightsMapper;
import com.chinaunicom.smartstorage.admin.mapper.rbac.UserMapper;
import com.chinaunicom.smartstorage.admin.mapper.sso.LoginLogMapper;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.SmsVerifyCodeService;
import com.chinaunicom.smartstorage.admin.service.inter.sso.AdminSsoService;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.jwt.JwtConstant;
import com.chinaunicom.smartstorage.common.jwt.JwtUtils;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.liuchink.chinkutils.date.ChinkDateOperationUtils;
import com.liuchink.chinkutils.json.ChinkFastJson2Utils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author liuchink
 * @since 2023/7/13 9:24
 */
@Service
@Slf4j
public class AdminSsoServiceImpl implements AdminSsoService {
    private static final String LOGIN_FAIL_COUNT_KEY = "admin_login:fail:count:key:";
    private static final String LOGIN_FAIL_LOCK_KEY = "admin_login:fail:lock:key:";
    private static final int LOGIN_FAIL_COUNT = 5;
    private static final int LOGIN_FAIL_COUNT_TIME = 60 * 5;
    @Resource
    private UserMapper userMapper;
    @Resource
    private MenuMapper menuMapper;
    @Resource
    private RightsMapper rightsMapper;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private LoginLogMapper loginLogMapper;
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private SmsVerifyCodeService smsVerifyCodeService;
    @Resource
    private HttpServletRequest httpServletRequest;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private SystemConfigComponent systemConfigComponent;
    @Resource
    private PwdComponent pwdComponent;
    @Resource
    private LoginComponent loginComponent;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<LoginRespDTO, String> loginByPassword(PasswordLoginRepDTO passwordLoginRepDTO) {
        ActionResult<LoginRespDTO, String> actionResultResp;
        String loginFailCountKey = LOGIN_FAIL_COUNT_KEY + passwordLoginRepDTO.getLoginId();
        String loginFailLockKey = LOGIN_FAIL_LOCK_KEY + passwordLoginRepDTO.getLoginId();
        User userLogin = new User();
        userLogin.setLoginId(passwordLoginRepDTO.getLoginId());
        if (redisTemplate.hasKey(loginFailLockKey)) {
            redisTemplate.delete(loginFailCountKey);
            redisTemplate.opsForValue().set(loginFailLockKey, passwordLoginRepDTO.getLoginId(),
                Convert.toInt(systemConfigComponent.getAdminLockPwdTime().getSycValue()), TimeUnit.MINUTES);
            return actionResultService.callBackResultFail("输入错误次数过多，请稍后再试");
        } else {
            userLogin = userMapper.loginInfoByLoginId(passwordLoginRepDTO.getLoginId());
            if (userLogin != null) {
                if (userLogin.getShowState() == 1) {
                    if (userLogin.getMustUpdatePwd() == 2 || ChinkDateOperationUtils
                        .betweenDays(userLogin.getLastUpdatePwd(), LocalDateTime.now()) < Convert
                            .toInt(systemConfigComponent.getMustUpdatePwdDayAdmin().getSycValue())) {
                        if (userLogin.getLoginPwd().equals(passwordLoginRepDTO.getLoginPwd())) {
                            final LoginRespDTO loginRespDTO = new LoginRespDTO();
                            String accessToken = loginComponent.createAccessToken(userLogin);
                            loginRespDTO.setAccessToken(accessToken);
                            loginRespDTO.setAccessTokenExpiresIn(JwtConstant.TOKEN_EXPIRE_TIME);
                            String refreshToken = loginComponent.createRefreshToken(userLogin);
                            loginRespDTO.setRefreshToken(refreshToken);
                            loginRespDTO.setRefreshTokenExpiresIn(JwtConstant.TOKEN_REFRESH_EXPIRE_TIME);
                            loginComponent.updateLastLoginTime(userLogin.getId());
                            redisTemplate.delete(loginFailLockKey);
                            redisTemplate.delete(loginFailCountKey);
                            loginComponent.saveLoginRightsCache(userLogin);
                            actionResultResp = actionResultService.callBackResult("登录成功", loginRespDTO);
                        } else {
                            Long incr = redisTemplate.opsForValue().increment(loginFailCountKey);
                            redisTemplate.expire(loginFailCountKey, LOGIN_FAIL_COUNT_TIME, TimeUnit.SECONDS);
                            if (incr != null && incr >= LOGIN_FAIL_COUNT) {
                                redisTemplate.opsForValue().set(loginFailLockKey, passwordLoginRepDTO.getLoginId(),
                                    Convert.toInt(systemConfigComponent.getAdminLockPwdTime().getSycValue()),
                                    TimeUnit.MINUTES);
                            }
                            actionResultResp = actionResultService.callBackResultFail("账号或密码错误");
                        }
                    } else {
                        actionResultResp = actionResultService.callBackResultFail("PASSWORD_EXPIRED", "密码已过期，请修改密码");
                    }
                } else {
                    actionResultResp = actionResultService.callBackResultFail("账号已被禁用");
                }
            } else {
                userLogin = new User();
                userLogin.setLoginId(passwordLoginRepDTO.getLoginId());
                actionResultResp = actionResultService.callBackResultFail("账号或密码错误");
            }
        }
        if (actionResultResp.getStatus().isSuccess()) {
            loginComponent.saveLoginLog(userLogin, "账号密码登录", passwordLoginRepDTO, 1, "登录成功");
        } else {
            loginComponent.saveLoginLog(userLogin, "账号密码登录", passwordLoginRepDTO, 2,
                actionResultResp.getStatus().getMessage());
        }
        return actionResultResp;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<LoginRespDTO, String> loginByPhone(PhoneLoginRepDTO phoneLoginRepDTO) {
        ActionResult<LoginRespDTO, String> actionResultResp;
        String loginFailCountKey = LOGIN_FAIL_COUNT_KEY + phoneLoginRepDTO.getPhone();
        String loginFailLockKey = LOGIN_FAIL_LOCK_KEY + phoneLoginRepDTO.getPhone();
        User userLogin = new User();
        userLogin.setLoginId(phoneLoginRepDTO.getPhone());
        if (redisTemplate.hasKey(loginFailLockKey)) {
            redisTemplate.delete(loginFailCountKey);
            redisTemplate.opsForValue().set(loginFailLockKey, phoneLoginRepDTO.getPhone(),
                Convert.toInt(systemConfigComponent.getAdminLockPwdTime().getSycValue()), TimeUnit.MINUTES);
            return actionResultService.callBackResultFail("输入错误次数过多，请稍后再试");
        } else {
            final ActionResult<String, String> smsValidateCodeAction =
                smsVerifyCodeService.checkSmsVerifyCode(phoneLoginRepDTO.getPhone(), phoneLoginRepDTO.getCode());
            if (smsValidateCodeAction.getStatus().isSuccess()) {
                userLogin = userMapper.loginInfoByPhone(phoneLoginRepDTO.getPhone());
                if (userLogin != null) {
                    if (userLogin.getShowState() == 1) {
                        final LoginRespDTO loginRespDTO = mapperFacade.map(userLogin, LoginRespDTO.class);
                        String accessToken = loginComponent.createAccessToken(userLogin);
                        loginRespDTO.setAccessToken(accessToken);
                        loginRespDTO.setAccessTokenExpiresIn(JwtConstant.TOKEN_EXPIRE_TIME);
                        String refreshToken = loginComponent.createRefreshToken(userLogin);
                        loginRespDTO.setRefreshToken(refreshToken);
                        loginRespDTO.setRefreshTokenExpiresIn(JwtConstant.TOKEN_REFRESH_EXPIRE_TIME);
                        loginComponent.updateLastLoginTime(userLogin.getId());
                        loginComponent.saveLoginLog(userLogin, "手机账号登录", phoneLoginRepDTO, 1, "登录成功");
                        redisTemplate.delete(loginFailLockKey);
                        redisTemplate.delete(loginFailCountKey);
                        loginComponent.saveLoginRightsCache(userLogin);
                        actionResultResp = actionResultService.callBackResult("登录成功", loginRespDTO);
                    } else {
                        actionResultResp = actionResultService.callBackResultFail("账号已被禁用");
                    }
                } else {
                    userLogin = new User();
                    userLogin.setLoginId(phoneLoginRepDTO.getPhone());
                    actionResultResp = actionResultService.callBackResultFail("账号或密码错误");
                }
            } else {
                Long incr = redisTemplate.opsForValue().increment(loginFailCountKey);
                redisTemplate.expire(loginFailCountKey, LOGIN_FAIL_COUNT_TIME, TimeUnit.SECONDS);
                if (incr != null && incr >= LOGIN_FAIL_COUNT) {
                    redisTemplate.opsForValue().set(loginFailLockKey, phoneLoginRepDTO.getPhone(),
                        Convert.toInt(systemConfigComponent.getAdminLockPwdTime().getSycValue()), TimeUnit.MINUTES);
                }
                actionResultResp = actionResultService.callBackResultFail("验证码错误");
            }
        }
        if (actionResultResp.getStatus().isSuccess()) {
            loginComponent.saveLoginLog(userLogin, "手机账号登录", phoneLoginRepDTO, 1, "登录成功");
        } else {
            loginComponent.saveLoginLog(userLogin, "手机账号登录", phoneLoginRepDTO, 2,
                actionResultResp.getStatus().getMessage());
        }
        return actionResultResp;
    }

    @Override
    public ActionResult<LoginBaseInfoRespDTO, String> getLoginBaseInfo() {
        String userId = jwtTokenComponent.getUser().getId();
        final User loginInfo = userMapper.getLoginBaseInfo(userId);
        if (loginInfo != null) {
            LoginUserRespDTO loginUserRespDTO = mapperFacade.map(loginInfo, LoginUserRespDTO.class);
            LoginOrganizationRespDTO loginOrganizationRespDTO =
                mapperFacade.map(loginInfo.getOrganization(), LoginOrganizationRespDTO.class);
            LoginRoleGroupRespDTO loginRoleGroupRespDTO =
                mapperFacade.map(loginInfo.getRoleGroup(), LoginRoleGroupRespDTO.class);
            LoginRegionRespDTO loginRegionRespDTO = mapperFacade.map(loginInfo.getRegion(), LoginRegionRespDTO.class);
            LoginBaseInfoRespDTO loginBaseInfoRespDTO = new LoginBaseInfoRespDTO();
            loginBaseInfoRespDTO.setLoginUserRespDTO(loginUserRespDTO);
            loginBaseInfoRespDTO.setLoginOrganizationRespDTO(loginOrganizationRespDTO);
            loginBaseInfoRespDTO.setLoginRoleGroupRespDTO(loginRoleGroupRespDTO);
            loginBaseInfoRespDTO.setLoginRegionRespDTO(loginRegionRespDTO);
            return actionResultService.callBackResult(loginBaseInfoRespDTO);
        } else {
            return actionResultService.callBackResultFail("用户信息不存在");
        }
    }

    @Override
    public ActionResult<List<LoginMenuRespDTO>, String> getLoginMenuInfo() {
        String roleGroupId = jwtTokenComponent.getUser().getRoleGroupId();
        List<Menu> listMenu = menuMapper.listMenuByGroupId(roleGroupId);
        MenuTreeComponent menuTreeComponent = new MenuTreeComponent(listMenu);
        listMenu = menuTreeComponent.builTree();
        List<LoginMenuRespDTO> listLoginMenuRespDTO = mapperFacade.mapAsList(listMenu, LoginMenuRespDTO.class);
        return actionResultService.callBackResult(listLoginMenuRespDTO);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String>
        updateUserLoginPasswordByLoginId(UpdateUserPasswordReqDTO updateUserPasswordReqDTO) {
        ActionResult<String, String> actionResult;
        if (StrUtil.equals(updateUserPasswordReqDTO.getLoginPassword(),
            updateUserPasswordReqDTO.getLoginPasswordSecond())) {
            User userByLoginId = userMapper.selectOne(
                Wrappers.lambdaQuery(User.class).eq(User::getLoginId, updateUserPasswordReqDTO.getLoginId()));
            if (userByLoginId != null) {
                if (StrUtil.equals(updateUserPasswordReqDTO.getOldPassword(), userByLoginId.getLoginPwd())) {
                    userByLoginId.setLoginPwd(pwdComponent.getEncryptMd5(updateUserPasswordReqDTO.getLoginPassword()));
                    userByLoginId.setLastUpdatePwd(LocalDateTime.now());
                    userByLoginId.setMustUpdatePwd(2);
                    userMapper.updateUserLoginPassword(userByLoginId);
                    actionResult = actionResultService.callBackResult("修改密码成功");
                } else {
                    actionResult = actionResultService.callBackResultFail("原始密码错误");
                }
            } else {
                actionResult = actionResultService.callBackResultFail("原始密码错误");
            }
        } else {
            actionResult = actionResultService.callBackResultFail("两次密码输入不一致");
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateUserLoginPassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO) {
        ActionResult<String, String> actionResult;
        if (StrUtil.equals(updateUserPasswordReqDTO.getLoginPassword(),
            updateUserPasswordReqDTO.getLoginPasswordSecond())) {
            User userById = userMapper.selectById(updateUserPasswordReqDTO.getId());
            if (userById != null) {
                if (StrUtil.equals(updateUserPasswordReqDTO.getOldPassword(), userById.getLoginPwd())) {
                    userById.setLoginPwd(pwdComponent.getEncryptMd5(updateUserPasswordReqDTO.getLoginPassword()));
                    userById.setLastUpdatePwd(LocalDateTime.now());
                    userMapper.updateUserLoginPassword(userById);
                    actionResult = actionResultService.callBackResult("修改密码成功");
                } else {
                    actionResult = actionResultService.callBackResultFail("原始密码错误");
                }
            } else {
                actionResult = actionResultService.callBackResultFail("原始密码错误");
            }
        } else {
            actionResult = actionResultService.callBackResultFail("两次密码输入不一致");
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateUserBase(String token, User user) {
        JwtUtils jwtUtils = new JwtUtils(JwtConstant.JWT_ADMIN_SECRET);
        String tokenSub = jwtUtils.parseToken(token).getPayload().getSubject();
        String key = SystemConstant.ADMIN_LOGIN_ACCESS_TOKEN_KEY + tokenSub;
        userMapper.updateById(user);
        if (redisTemplate.hasKey(key)) {
            final User userLogin = userMapper.loginInfoByLoginId(user.getLoginId());
            redisTemplate.opsForValue().set(key, ChinkFastJson2Utils.toJsonString(userLogin),
                JwtConstant.TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
        }
        return actionResultService.callBackResult("修改成功");
    }

}
