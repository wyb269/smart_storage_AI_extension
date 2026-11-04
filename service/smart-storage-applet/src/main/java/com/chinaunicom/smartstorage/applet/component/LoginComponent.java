package com.chinaunicom.smartstorage.applet.component;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.applet.dto.sso.BindMemberByPhoneReqDTO;
import com.chinaunicom.smartstorage.applet.dto.sso.LoginRespDTO;
import com.chinaunicom.smartstorage.applet.dto.wechat.WeChatMiniGetUserPhoneNumberRespDTO;
import com.chinaunicom.smartstorage.applet.entity.member.Member;
import com.chinaunicom.smartstorage.applet.mapper.member.MemberMapper;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.jwt.JwtConstant;
import com.chinaunicom.smartstorage.common.jwt.JwtUtils;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.liuchink.chinkutils.json.ChinkFastJson2Utils;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * @author liuchink
 * @since 2025/3/14 11:01
 */
@Component
public class LoginComponent {
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private MemberMapper memberMapper;
    @Resource
    private ActionResultService actionResultService;

    public String createAccessToken(Member member) {
        String tokenSub = IdUtil.fastSimpleUUID();
        JwtUtils accessTokenJwtUtils = new JwtUtils(JwtConstant.JWT_APPLET_SECRET);
        redisTemplate.opsForValue().set(SystemConstant.APPLET_LOGIN_ACCESS_TOKEN_KEY + tokenSub,
            ChinkFastJson2Utils.toJsonString(member), JwtConstant.TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
        return accessTokenJwtUtils.createToken(JwtConstant.JWT_ISS_APPLET, tokenSub, JwtConstant.JWT_AUD_APPLET);
    }

    public String createRefreshToken(Member member) {
        String tokenSub = IdUtil.fastSimpleUUID();
        JwtUtils refreshTokenJwtUtils = new JwtUtils(JwtConstant.JWT_APPLET_SECRET_REFRESH);
        redisTemplate.opsForValue().set(SystemConstant.APPLET_LOGIN_REFRESH_TOKEN_KEY + tokenSub,
            ChinkFastJson2Utils.toJsonString(member), JwtConstant.TOKEN_REFRESH_EXPIRE_TIME, TimeUnit.SECONDS);
        return refreshTokenJwtUtils.createToken(JwtConstant.JWT_ISS_APPLET, tokenSub, JwtConstant.JWT_AUD_APPLET);
    }

    public ActionResult<LoginRespDTO, String> bindNewMember(BindMemberByPhoneReqDTO bindMemberByPhoneReqDTO,
        WeChatMiniGetUserPhoneNumberRespDTO weChatMiniGetUserPhoneNumberRespDTO) {
        LoginRespDTO loginRespDTO = new LoginRespDTO();
        // 注册
        Member member = new Member();
        member.setId(Convert.toStr(uidGenerator.getUID()));
        member.setWechatMiniOpenid(bindMemberByPhoneReqDTO.getOpenid());
        member.setShowState(1);
        member.setAddTime(LocalDateTime.now());
        member.setPhone(weChatMiniGetUserPhoneNumberRespDTO.getPhoneInfo().getPhoneNumber());
        member.setUsername(weChatMiniGetUserPhoneNumberRespDTO.getPhoneInfo().getPhoneNumber());
        member.setPassword(SecureUtil.md5(weChatMiniGetUserPhoneNumberRespDTO.getPhoneInfo().getPhoneNumber()));
        member.setSourceType(1);
        member.setLastLoginTime(LocalDateTime.now());
        memberMapper.insert(member);
        member.setWechatMiniOpenid(bindMemberByPhoneReqDTO.getOpenid());
        String accessToken = createAccessToken(member);
        String refreshToken = createRefreshToken(member);
        loginRespDTO.setAccessToken(accessToken);
        loginRespDTO.setAccessTokenExpiresIn(JwtConstant.TOKEN_EXPIRE_TIME);
        loginRespDTO.setRefreshToken(refreshToken);
        loginRespDTO.setRefreshTokenExpiresIn(JwtConstant.TOKEN_REFRESH_EXPIRE_TIME);
        return actionResultService.callBackResult(loginRespDTO);
    }

    public ActionResult<LoginRespDTO, String> bindOldMember(BindMemberByPhoneReqDTO bindMemberByPhoneReqDTO,
        WeChatMiniGetUserPhoneNumberRespDTO weChatMiniGetUserPhoneNumberRespDTO, Member memberByOpenid) {
        LoginRespDTO loginRespDTO = new LoginRespDTO();
        // 绑定手机号码
        Member member = new Member();
        member.setLastLoginTime(LocalDateTime.now());
        member.setWechatMiniOpenid(bindMemberByPhoneReqDTO.getOpenid());
        // member.setPhone(weChatMiniGetUserPhoneNumberRespDTO.getPhoneInfo().getPhoneNumber());
        memberMapper.update(member, Wrappers.lambdaQuery(Member.class).eq(Member::getUsername,
            weChatMiniGetUserPhoneNumberRespDTO.getPhoneInfo().getPhoneNumber()));
        memberByOpenid.setWechatMiniOpenid(bindMemberByPhoneReqDTO.getOpenid());
        String accessToken = createAccessToken(memberByOpenid);
        String refreshToken = createRefreshToken(memberByOpenid);
        loginRespDTO.setAccessToken(accessToken);
        loginRespDTO.setAccessTokenExpiresIn(JwtConstant.TOKEN_EXPIRE_TIME);
        loginRespDTO.setRefreshToken(refreshToken);
        loginRespDTO.setRefreshTokenExpiresIn(JwtConstant.TOKEN_REFRESH_EXPIRE_TIME);
        return actionResultService.callBackResult(loginRespDTO);
    }
}
