package com.chinaunicom.smartstorage.applet.service.impl.sso;

import cn.hutool.core.convert.Convert;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.applet.component.LoginComponent;
import com.chinaunicom.smartstorage.applet.component.PwdComponent;
import com.chinaunicom.smartstorage.applet.component.SystemConfigComponent;
import com.chinaunicom.smartstorage.applet.component.WeChatMiniComponent;
import com.chinaunicom.smartstorage.applet.dto.sso.*;
import com.chinaunicom.smartstorage.applet.dto.sso.*;
import com.chinaunicom.smartstorage.applet.dto.wechat.WeChatMiniGetUserPhoneNumberRespDTO;
import com.chinaunicom.smartstorage.applet.entity.member.Member;
import com.chinaunicom.smartstorage.applet.mapper.member.MemberMapper;
import com.chinaunicom.smartstorage.applet.service.inter.aggregation.SmsVerifyCodeService;
import com.chinaunicom.smartstorage.applet.service.inter.sso.MemberLoginService;
import com.chinaunicom.smartstorage.common.jwt.JwtConstant;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.liuchink.chinkutils.wechat.mini.login.bean.WeChatMiniJsCode2SessionResp;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * @author liuchink
 * @since 2023/11/4 15:24
 */
@Service
@Slf4j
public class MemberLoginServiceImpl implements MemberLoginService {
    private static final String LOGIN_FAIL_COUNT_KEY = "applet_login:fail:count:key:";
    private static final String LOGIN_FAIL_LOCK_KEY = "applet_login:fail:lock:key:";
    private static final int LOGIN_FAIL_COUNT = 5;
    private static final int LOGIN_FAIL_COUNT_TIME = 60 * 5;
    @Resource
    private MemberMapper memberMapper;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private SmsVerifyCodeService smsVerifyCodeService;
    @Resource
    private WeChatMiniComponent weChatMiniComponent;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private SystemConfigComponent systemConfigComponent;
    @Resource
    private LoginComponent loginComponent;
    @Resource
    private PwdComponent pwdComponent;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<WechatMiniLoginRespDTO, String> loginByOpenid(OpenidLoginReqDTO openidLoginReqDTO) {
        final ActionResult<WeChatMiniJsCode2SessionResp, String> getOpenidByCode =
            weChatMiniComponent.getOpenidByCode(openidLoginReqDTO.getCode());
        if (getOpenidByCode.getStatus().isSuccess()) {
            final WeChatMiniJsCode2SessionResp weChatMiniJsCode2SessionResp = getOpenidByCode.getResult();
            WechatMiniLoginRespDTO wechatMiniLoginRespDTO = new WechatMiniLoginRespDTO();
            wechatMiniLoginRespDTO.setUnionid(weChatMiniJsCode2SessionResp.getUnionid());
            wechatMiniLoginRespDTO.setOpenid(weChatMiniJsCode2SessionResp.getOpenId());
            wechatMiniLoginRespDTO.setSessionKey(weChatMiniJsCode2SessionResp.getSessionKey());
            Member memberByOpenid = memberMapper.selectOne(Wrappers.lambdaQuery(Member.class)
                .eq(Member::getWechatMiniOpenid, weChatMiniJsCode2SessionResp.getOpenId()));
            if (memberByOpenid == null) {
                return actionResultService.callBackResultFail("TO_REGISTER", "openid不存在，请先进行注册",
                    wechatMiniLoginRespDTO);
            } else {
                if (memberByOpenid.getShowState() != 1) {
                    return actionResultService.callBackResultFail("当前用户已被禁用，无法登录");
                } else {
                    memberByOpenid.setWechatMiniOpenid(weChatMiniJsCode2SessionResp.getOpenId());
                    String accessToken = loginComponent.createAccessToken(memberByOpenid);
                    String refreshToken = loginComponent.createRefreshToken(memberByOpenid);
                    wechatMiniLoginRespDTO.setAccessToken(accessToken);
                    wechatMiniLoginRespDTO.setAccessTokenExpiresIn(JwtConstant.TOKEN_EXPIRE_TIME);
                    wechatMiniLoginRespDTO.setRefreshToken(refreshToken);
                    wechatMiniLoginRespDTO.setRefreshTokenExpiresIn(JwtConstant.TOKEN_REFRESH_EXPIRE_TIME);
                    Member memberLastLoginTime = new Member();
                    memberLastLoginTime.setLastLoginTime(LocalDateTime.now());
                    memberMapper.update(memberLastLoginTime, Wrappers.lambdaQuery(Member.class)
                        .eq(Member::getWechatMiniOpenid, memberByOpenid.getWechatMiniOpenid()));
                    return actionResultService.callBackResult(wechatMiniLoginRespDTO);
                }

            }
        } else {
            return actionResultService.callBackResultFail(getOpenidByCode.getStatus().getCode(),
                getOpenidByCode.getStatus().getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<LoginRespDTO, String> bindMemberByPhone(BindMemberByPhoneReqDTO bindMemberByPhoneReqDTO) {
        final ActionResult<WeChatMiniGetUserPhoneNumberRespDTO, String> actionResult =
            weChatMiniComponent.wechatMiniGetUserPhone(bindMemberByPhoneReqDTO.getCode());
        if (actionResult.getStatus().isSuccess()) {
            final WeChatMiniGetUserPhoneNumberRespDTO weChatMiniGetUserPhoneNumberRespDTO = actionResult.getResult();
            Member memberByOpenid = memberMapper.selectOne(Wrappers.lambdaQuery(Member.class).eq(Member::getUsername,
                weChatMiniGetUserPhoneNumberRespDTO.getPhoneInfo().getPhoneNumber()));
            if (memberByOpenid == null) {
                return loginComponent.bindNewMember(bindMemberByPhoneReqDTO, weChatMiniGetUserPhoneNumberRespDTO);
            } else {
                return loginComponent.bindOldMember(bindMemberByPhoneReqDTO, weChatMiniGetUserPhoneNumberRespDTO,
                    memberByOpenid);
            }
        } else {
            return actionResultService.callBackResultFail(actionResult.getStatus().getCode(),
                actionResult.getStatus().getMessage());
        }
    }

    @Override
    public ActionResult<LoginRespDTO, String> loginByPassword(PasswordLoginRepDTO passwordLoginRepDTO) {
        ActionResult<LoginRespDTO, String> actionResultResp;
        String loginFailCountKey = LOGIN_FAIL_COUNT_KEY + passwordLoginRepDTO.getLoginId();
        String loginFailLockKey = LOGIN_FAIL_LOCK_KEY + passwordLoginRepDTO.getLoginId();
        if (redisTemplate.hasKey(loginFailLockKey)) {
            redisTemplate.delete(loginFailCountKey);
            redisTemplate.opsForValue().set(loginFailLockKey, passwordLoginRepDTO.getLoginId(),
                Convert.toInt(systemConfigComponent.getAppletLockPwdTime().getSycValue()), TimeUnit.MINUTES);
            return actionResultService.callBackResultFail("输入错误次数过多，请稍后再试");

        }
        final Member member = memberMapper
            .selectOne(Wrappers.lambdaQuery(Member.class).eq(Member::getUsername, passwordLoginRepDTO.getLoginId()));
        if (member != null) {
            if (member.getShowState() == 1) {
                if (member.getPassword().equals(passwordLoginRepDTO.getLoginPassword())) {
                    final LoginRespDTO loginRespDTO = new LoginRespDTO();
                    String accessToken = loginComponent.createAccessToken(member);
                    loginRespDTO.setAccessToken(accessToken);
                    loginRespDTO.setAccessTokenExpiresIn(JwtConstant.TOKEN_EXPIRE_TIME);
                    String refreshToken = loginComponent.createRefreshToken(member);
                    loginRespDTO.setRefreshToken(refreshToken);
                    loginRespDTO.setRefreshTokenExpiresIn(JwtConstant.TOKEN_REFRESH_EXPIRE_TIME);
                    redisTemplate.delete(loginFailLockKey);
                    redisTemplate.delete(loginFailCountKey);
                    // getLoginRightsInfo(JwtConstant.JWT_SEPARATOR + accessToken);
                    actionResultResp = actionResultService.callBackResult("登录成功", loginRespDTO);
                } else {
                    Long incr = redisTemplate.opsForValue().increment(loginFailCountKey);
                    redisTemplate.expire(loginFailCountKey, LOGIN_FAIL_COUNT_TIME, TimeUnit.SECONDS);
                    if (incr != null && incr >= LOGIN_FAIL_COUNT) {
                        redisTemplate.opsForValue().set(loginFailLockKey, passwordLoginRepDTO.getLoginId(),
                            Convert.toInt(systemConfigComponent.getAppletLockPwdTime().getSycValue()),
                            TimeUnit.MINUTES);
                    }
                    actionResultResp = actionResultService.callBackResultFail("账号或密码错误");
                }
            } else {
                actionResultResp = actionResultService.callBackResultFail("账号已被禁用");
            }
        } else {
            actionResultResp = actionResultService.callBackResultFail("账号或密码错误");
        }
        return actionResultResp;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> forgetPassword(ForgetPasswordReqDTO forgetPasswordReqDTO) {
        if (!forgetPasswordReqDTO.getLoginPassword().equals(forgetPasswordReqDTO.getLoginPasswordSecond())) {
            return actionResultService.callBackResultFail("两次密码输入不一致");
        }
        Member member = new Member();
        member.setUsername(forgetPasswordReqDTO.getPhone());
        member.setPassword(forgetPasswordReqDTO.getLoginPassword());
        memberMapper.update(member,
            Wrappers.lambdaQuery(Member.class).eq(Member::getUsername, forgetPasswordReqDTO.getPhone()));
        return actionResultService.callBackResult();
    }
}
