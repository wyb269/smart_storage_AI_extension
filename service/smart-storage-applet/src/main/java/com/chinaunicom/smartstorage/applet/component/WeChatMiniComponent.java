package com.chinaunicom.smartstorage.applet.component;

import cn.hutool.core.convert.Convert;
import com.alibaba.fastjson2.JSON;
import com.chinaunicom.smartstorage.applet.dto.wechat.WeChatMiniAccessTokenRespDTO;
import com.chinaunicom.smartstorage.applet.dto.wechat.WeChatMiniGetUserPhoneNumberRespDTO;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.chinaunicom.smartstorage.common.utils.CommonUtils;
import com.liuchink.chinkutils.json.ChinkFastJson2Utils;
import com.liuchink.chinkutils.wechat.mini.login.ChinkWeChatMiniLoginUtils;
import com.liuchink.chinkutils.wechat.mini.login.bean.WeChatMiniJsCode2SessionResp;
import com.liuchink.chinkutils.wechat.mini.qrcode.ChinkWeChatMiniQrCodeUtils;
import com.liuchink.chinkutils.wechat.mini.qrcode.bean.WeChatMiniGetUnlimitedQrCodeReq;
import com.liuchink.chinkutils.wechat.mini.qrcode.bean.WeChatMiniGetUnlimitedQrCodeResp;
import com.liuchink.chinkutils.wechat.mini.user.ChinkWeChatMiniUserUtils;
import com.liuchink.chinkutils.wechat.mini.user.bean.WeChatMiniGetUserPhoneNumberPhoneInfoResp;
import com.liuchink.chinkutils.wechat.mini.user.bean.WeChatMiniGetUserPhoneNumberResp;
import com.liuchink.chinkutils.wechat.security.ChinkWeChatAccessTokenUtils;
import com.liuchink.chinkutils.wechat.security.bean.WeChatAccessTokenResp;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * 微信
 *
 * @author liuchink
 * @since 2023/11/3 15:54
 */
@Slf4j
@Component
public class WeChatMiniComponent {
    private static final String WECHAT_MINI_ACCESSTOKEN_KEY = "wechat:mini:accesstoken:";
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private SystemConfigComponent systemConfigComponent;

    /**
     * 获取accessToken
     *
     * @author liuchink
     * @since 2025/3/25 13:26
     */
    public ActionResult<WeChatMiniAccessTokenRespDTO, String> getAccessToken() {
        String appId = Convert.toStr(systemConfigComponent.getWeChatMiniAppId().getSycValue());
        String appSecret = Convert.toStr(systemConfigComponent.getWeChatMiniAppSecret().getSycValue());
        final String accessTokenKey = WECHAT_MINI_ACCESSTOKEN_KEY + appId;
        if (stringRedisTemplate.hasKey(accessTokenKey)) {
            final String accessToken = stringRedisTemplate.opsForValue().get(accessTokenKey);
            final WeChatMiniAccessTokenRespDTO weChatMiniAccessTokenRespDTO =
                JSON.parseObject(accessToken, WeChatMiniAccessTokenRespDTO.class);
            return actionResultService.callBackResult(weChatMiniAccessTokenRespDTO);
        } else {
            try {

                final WeChatAccessTokenResp weChatAccessTokenResp =
                    ChinkWeChatAccessTokenUtils.createAccessToken(appId, appSecret);
                if (weChatAccessTokenResp.getErrorInfo().getErrCode().equals(SystemConstant.SUCCESS_STRING_0)) {
                    final WeChatMiniAccessTokenRespDTO weChatMiniAccessTokenRespDTO =
                        mapperFacade.map(weChatAccessTokenResp, WeChatMiniAccessTokenRespDTO.class);
                    stringRedisTemplate.opsForValue().set(accessTokenKey,
                        ChinkFastJson2Utils.toJsonString(weChatAccessTokenResp), 5400, TimeUnit.SECONDS);
                    return actionResultService.callBackResult(weChatMiniAccessTokenRespDTO);
                } else {
                    return actionResultService.callBackResultFail(weChatAccessTokenResp.getErrorInfo().getErrCode(),
                        weChatAccessTokenResp.getErrorInfo().getErrMsg());
                }
            } catch (IOException | NoSuchAlgorithmException | KeyManagementException | InstantiationException
                | IllegalAccessException e) {
                throw new CustomException(e);
            }
        }
    }

    /**
     * 获取手机号码
     *
     * @param code code
     * @author liuchink
     * @since 2025/3/25 13:26
     */
    public ActionResult<WeChatMiniGetUserPhoneNumberRespDTO, String> wechatMiniGetUserPhone(String code) {
        final ActionResult<WeChatMiniAccessTokenRespDTO, String> accessTokenActionResult = getAccessToken();
        if (accessTokenActionResult.getStatus().isSuccess()) {
            try {
                final WeChatMiniGetUserPhoneNumberResp weChatMiniGetUserPhoneNumberResp = ChinkWeChatMiniUserUtils
                    .getUserPhoneNumber(accessTokenActionResult.getResult().getAccessToken(), code);
                // 测试
                if (CommonUtils.checkActiveProfiles(SystemConstant.ACTIVE_PROFILE_DEV)) {
                    WeChatMiniGetUserPhoneNumberPhoneInfoResp weChatMiniGetUserPhoneNumberPhoneInfoResp =
                        new WeChatMiniGetUserPhoneNumberPhoneInfoResp();
                    weChatMiniGetUserPhoneNumberPhoneInfoResp.setPhoneNumber("18646006388");
                    weChatMiniGetUserPhoneNumberResp.setPhoneInfo(weChatMiniGetUserPhoneNumberPhoneInfoResp);
                    weChatMiniGetUserPhoneNumberResp.getErrorInfo().setErrCode(SystemConstant.SUCCESS_STRING_0);
                    weChatMiniGetUserPhoneNumberResp.getErrorInfo().setErrMsg("请求成功");
                }
                if (weChatMiniGetUserPhoneNumberResp.getErrorInfo().getErrCode()
                    .equals(SystemConstant.SUCCESS_STRING_0)) {
                    final WeChatMiniGetUserPhoneNumberRespDTO weChatMiniGetUserPhoneNumberRespDTO =
                        mapperFacade.map(weChatMiniGetUserPhoneNumberResp, WeChatMiniGetUserPhoneNumberRespDTO.class);
                    return actionResultService.callBackResult(weChatMiniGetUserPhoneNumberRespDTO);
                } else {
                    return actionResultService.callBackResultFail(
                        weChatMiniGetUserPhoneNumberResp.getErrorInfo().getErrCode(),
                        weChatMiniGetUserPhoneNumberResp.getErrorInfo().getErrMsg());
                }
            } catch (IOException | NoSuchAlgorithmException | KeyManagementException | InstantiationException
                | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new CustomException(e);
            }
        } else {
            return actionResultService.callBackResultFail(accessTokenActionResult.getStatus().getCode(),
                accessTokenActionResult.getStatus().getMessage());
        }
    }

    /**
     * 获取openid
     *
     * @param code code
     * @author liuchink
     * @since 2025/3/25 13:26
     */
    public ActionResult<WeChatMiniJsCode2SessionResp, String> getOpenidByCode(String code) {
        String appId = Convert.toStr(systemConfigComponent.getWeChatMiniAppId().getSycValue());
        String appSecret = Convert.toStr(systemConfigComponent.getWeChatMiniAppSecret().getSycValue());
        try {
            final WeChatMiniJsCode2SessionResp weChatMiniJsCode2SessionResp =
                ChinkWeChatMiniLoginUtils.createJsCode2Session(appId, appSecret, code);
            // 测试
            if (CommonUtils.checkActiveProfiles(SystemConstant.ACTIVE_PROFILE_DEV)) {
                weChatMiniJsCode2SessionResp.setSessionKey("oWzMb7XVVkUxrDFYkzZO_lP50yyE");
                weChatMiniJsCode2SessionResp.setUnionid("oWzMb7XVVkUxrDFYkzZO_lP50yyE");
                weChatMiniJsCode2SessionResp.setOpenId("oWzMb7XVVkUxrDFYkzZO_lP50yyE");
                weChatMiniJsCode2SessionResp.getErrorInfo().setErrCode(SystemConstant.SUCCESS_STRING_0);
                weChatMiniJsCode2SessionResp.getErrorInfo().setErrMsg("请求成功");
            }
            if (weChatMiniJsCode2SessionResp.getErrorInfo().getErrCode().equals(SystemConstant.SUCCESS_STRING_0)) {
                return actionResultService.callBackResult(weChatMiniJsCode2SessionResp);
            } else {
                return actionResultService.callBackResultFail(weChatMiniJsCode2SessionResp.getErrorInfo().getErrCode(),
                    weChatMiniJsCode2SessionResp.getErrorInfo().getErrMsg());
            }
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException | InstantiationException
            | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new CustomException(e);
        }
    }

    /**
     * 获取小程序二维码
     *
     * @param weChatMiniGetUnlimitedQrCodeReq weChatMiniGetUnlimitedQrCodeReq
     * @author liuchink
     * @since 2025/3/25 13:34
     */
    public ActionResult<WeChatMiniGetUnlimitedQrCodeResp, String>
        getQrCode(WeChatMiniGetUnlimitedQrCodeReq weChatMiniGetUnlimitedQrCodeReq) {
        try {
            final ActionResult<WeChatMiniAccessTokenRespDTO, String> accessTokenActionResult = getAccessToken();
            if (accessTokenActionResult.getStatus().isSuccess()) {
                final WeChatMiniGetUnlimitedQrCodeResp unlimitedQrCodeActionResult =
                    ChinkWeChatMiniQrCodeUtils.getUnlimitedQrCode(accessTokenActionResult.getResult().getAccessToken(),
                        weChatMiniGetUnlimitedQrCodeReq);
                return actionResultService.callBackResult(unlimitedQrCodeActionResult);
            } else {
                return actionResultService.callBackResultFail(accessTokenActionResult.getStatus().getCode(),
                    accessTokenActionResult.getStatus().getMessage());
            }
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new CustomException(e);
        }
    }
}
