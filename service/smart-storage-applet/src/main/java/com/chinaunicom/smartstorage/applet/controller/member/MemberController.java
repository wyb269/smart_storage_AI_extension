package com.chinaunicom.smartstorage.applet.controller.member;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.alibaba.fastjson2.JSON;
import com.chinaunicom.smartstorage.applet.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.applet.component.PwdComponent;
import com.chinaunicom.smartstorage.applet.dto.member.MemberRespDTO;
import com.chinaunicom.smartstorage.applet.dto.member.MemberUpdatePasswordReqDTO;
import com.chinaunicom.smartstorage.applet.dto.member.MemberUpdatePhoneReqDTO;
import com.chinaunicom.smartstorage.applet.dto.member.MemberUpdateReqDTO;
import com.chinaunicom.smartstorage.applet.dto.sso.LogoutReqDTO;
import com.chinaunicom.smartstorage.applet.entity.member.Member;
import com.chinaunicom.smartstorage.applet.service.inter.aggregation.SmsVerifyCodeService;
import com.chinaunicom.smartstorage.applet.service.inter.member.MemberService;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.jwt.JwtConstant;
import com.chinaunicom.smartstorage.common.jwt.JwtTokenUtils;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;

/**
 * 会员
 *
 * @author liuchink
 * @since 2023/11/3 16:04
 */
@Tag(name = "会员管理-会员信息")
@RestController
@Slf4j
@RequestMapping("/member")
public class MemberController extends BaseController {

    @Resource
    private MemberService memberService;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private SmsVerifyCodeService smsVerifyCodeService;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private PwdComponent pwdComponent;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Operation(summary = "修改信息")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/updateMember", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> updateMember(@RequestBody @Validated MemberUpdateReqDTO memberUpdateReqDTO) {
        String id = jwtTokenComponent.getMember().getId();
        final Member member = mapperFacade.map(memberUpdateReqDTO, Member.class);
        member.setId(id);
        return memberService.updateMember(member);
    }

    @Operation(summary = "查看会员详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/displayMemberInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<MemberRespDTO, String> displayMemberInfo() {
        String memberId = jwtTokenComponent.getMember().getId();
        final Member memberByOpenId = memberService.displayMemberById(memberId);
        final MemberRespDTO memberRespDTO = mapperFacade.map(memberByOpenId, MemberRespDTO.class);
        return actionResultService.callBackResult(memberRespDTO);
    }

    @Operation(summary = "修改手机号码")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/updateMemberPhone", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String>
        updateMemberPhone(@RequestBody @Validated MemberUpdatePhoneReqDTO memberUpdatePhoneReqDTO) {
        final ActionResult<String, String> actionResult = smsVerifyCodeService
            .checkSmsVerifyCode(memberUpdatePhoneReqDTO.getPhone(), memberUpdatePhoneReqDTO.getCode());
        if (actionResult.getStatus().isSuccess()) {
            Member member = mapperFacade.map(memberUpdatePhoneReqDTO, Member.class);
            member.setId(jwtTokenComponent.getMember().getId());
            member.setUsername(member.getPhone());
            return memberService.updateMemberPhone(member);
        }
        return actionResultService.callBackResultFail(actionResult.getStatus().getCode(),
            actionResult.getStatus().getMessage());
    }

    @Operation(summary = "解绑微信openid")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/unBindMemberByWechatMiniOpenid", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> unBindMemberByWechatMiniOpenid() {
        String memberId = jwtTokenComponent.getMember().getId();
        Member member = new Member();
        member.setId(memberId);
        member.setWechatMiniOpenid("");
        return memberService.updateMember(member);
    }

    @Operation(summary = "修改用户密码")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/updateMemberLoginPassword", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String>
        updateMemberLoginPassword(@RequestBody @Validated MemberUpdatePasswordReqDTO memberUpdatePasswordReqDTO) {
        ActionResult<String, String> actionResult =
            actionResultService.callBackResultFail("密码必须包含大小字母、数字、特殊字符，最少8个字符，最多15个字符");
        String regex = "(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*[^a-zA-Z0-9]).{8,15}";
        memberUpdatePasswordReqDTO.setOldPassword(
            pwdComponent.getEncryptMd5(pwdComponent.getRsaDecrypt(memberUpdatePasswordReqDTO.getOldPassword())));
        memberUpdatePasswordReqDTO
            .setLoginPassword(pwdComponent.getRsaDecrypt(memberUpdatePasswordReqDTO.getLoginPassword()));
        if (memberUpdatePasswordReqDTO.getLoginPassword().matches(regex)) {
            memberUpdatePasswordReqDTO
                .setLoginPassword(pwdComponent.getEncryptMd5(memberUpdatePasswordReqDTO.getLoginPassword()));
            memberUpdatePasswordReqDTO.setLoginPasswordSecond(
                pwdComponent.getRsaDecryptAndEncryptMd5(memberUpdatePasswordReqDTO.getLoginPasswordSecond()));
            memberUpdatePasswordReqDTO.setId(jwtTokenComponent.getMember().getId());
            actionResult = memberService.updateMemberLoginPassword(memberUpdatePasswordReqDTO);
        }
        return actionResult;
    }

    /**
     *
     * 登出
     *
     *
     * @author liuchink
     * @since 2022/1/6 8:57
     */
    @Operation(summary = "登出")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> logout(@RequestHeader(JwtConstant.REQUEST_HEAD_TOKEN) String token,
        @RequestBody @Validated LogoutReqDTO logoutReqDTO) {
        String accessTokenKey = SystemConstant.APPLET_LOGIN_ACCESS_TOKEN_KEY
            + JwtTokenUtils.getSubject(token, JwtConstant.JWT_APPLET_SECRET);
        String refreshTokenKey = SystemConstant.APPLET_LOGIN_REFRESH_TOKEN_KEY
            + JwtTokenUtils.getSubjectNoBearer(logoutReqDTO.getRefreshToken(), JwtConstant.JWT_APPLET_SECRET_REFRESH);
        if (redisTemplate.hasKey(accessTokenKey)) {
            final String accessTokenUserInfoStr = redisTemplate.opsForValue().get(accessTokenKey);
            final Member userAccessToken = JSON.parseObject(accessTokenUserInfoStr, Member.class);
            redisTemplate.delete(accessTokenKey);
            if (redisTemplate.hasKey(refreshTokenKey)) {
                final String refreshTokenUserInfoStr = redisTemplate.opsForValue().get(refreshTokenKey);
                final Member userRefreshToken = JSON.parseObject(refreshTokenUserInfoStr, Member.class);
                if (userAccessToken != null && userRefreshToken != null
                    && userAccessToken.getId().equals(userRefreshToken.getId())) {
                    redisTemplate.delete(refreshTokenKey);
                }
            }
        }

        return actionResultService.callBackResult();
    }
}
