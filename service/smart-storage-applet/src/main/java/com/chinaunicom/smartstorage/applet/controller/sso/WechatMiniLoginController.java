package com.chinaunicom.smartstorage.applet.controller.sso;

import com.chinaunicom.smartstorage.applet.dto.sso.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinaunicom.smartstorage.applet.aop.PassToken;
import com.chinaunicom.smartstorage.applet.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.applet.component.PwdComponent;
import com.chinaunicom.smartstorage.applet.constant.AppletSystemConstant;
import com.chinaunicom.smartstorage.applet.dto.sso.*;
import com.chinaunicom.smartstorage.applet.service.inter.aggregation.SmsVerifyCodeService;
import com.chinaunicom.smartstorage.applet.service.inter.sso.MemberLoginService;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import cn.hutool.core.text.CharSequenceUtil;
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
@Tag(name = "SSO-微信小程序登录")
@RestController
@Slf4j
@RequestMapping("/sso/applet/wechat/mini/login")
public class WechatMiniLoginController extends BaseController {

    @Resource
    private MemberLoginService memberLoginService;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private SmsVerifyCodeService smsVerifyCodeService;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private PwdComponent pwdComponent;

    @PassToken
    @Operation(summary = "openid登录")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/loginByOpenid", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<WechatMiniLoginRespDTO, String>
        loginByOpenid(@RequestBody @Validated OpenidLoginReqDTO openidLoginReqDTO) {
        return memberLoginService.loginByOpenid(openidLoginReqDTO);
    }

    @PassToken
    @Operation(summary = "绑定手机号码")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/bindMemberByPhone", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<LoginRespDTO, String>
        bindMemberByPhone(@RequestBody @Validated BindMemberByPhoneReqDTO bindMemberByPhoneReqDTO) {
        return memberLoginService.bindMemberByPhone(bindMemberByPhoneReqDTO);
    }

    @PassToken
    @Operation(summary = "账号密码登录")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/loginByPassword", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<LoginRespDTO, String>
        loginByPassword(@RequestBody @Validated PasswordLoginRepDTO passwordLoginRepDTO) {
        final String verCode =
            redisTemplate.opsForValue().get(AppletSystemConstant.VER_CODE_KEY + passwordLoginRepDTO.getKey());
        redisTemplate.delete(AppletSystemConstant.VER_CODE_KEY + passwordLoginRepDTO.getKey());
        if (CharSequenceUtil.isBlank(verCode) || !passwordLoginRepDTO.getVerCode().equalsIgnoreCase(verCode)) {
            return actionResultService.callBackResultFail("验证码错误");
        }
        return memberLoginService.loginByPassword(passwordLoginRepDTO);
    }

    @PassToken
    @Operation(summary = "忘记密码")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/forgetPassword", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String>
        forgetPassword(@RequestBody @Validated ForgetPasswordReqDTO forgetPasswordReqDTO) {
        ActionResult<String, String> actionResult =
            smsVerifyCodeService.checkSmsVerifyCode(forgetPasswordReqDTO.getPhone(), forgetPasswordReqDTO.getCode());
        String regex = "(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*[^a-zA-Z0-9]).{8,15}";
        forgetPasswordReqDTO.setLoginPassword(pwdComponent.getRsaDecrypt(forgetPasswordReqDTO.getLoginPassword()));
        if (actionResult.getStatus().isSuccess()) {
            if (forgetPasswordReqDTO.getLoginPassword().matches(regex)) {
                forgetPasswordReqDTO
                    .setLoginPassword(pwdComponent.getEncryptMd5(forgetPasswordReqDTO.getLoginPassword()));
                forgetPasswordReqDTO.setLoginPasswordSecond(
                    pwdComponent.getRsaDecryptAndEncryptMd5(forgetPasswordReqDTO.getLoginPasswordSecond()));
                return memberLoginService.forgetPassword(forgetPasswordReqDTO);
            } else {
                actionResult = actionResultService.callBackResultFail("密码必须包含大小字母、数字、特殊字符，最少8个字符，最多15个字符");
            }
        }
        return actionResult;
    }


}
