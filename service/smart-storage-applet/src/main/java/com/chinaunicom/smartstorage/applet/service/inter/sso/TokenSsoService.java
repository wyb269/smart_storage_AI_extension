package com.chinaunicom.smartstorage.applet.service.inter.sso;

import com.chinaunicom.smartstorage.applet.dto.member.MemberRespDTO;
import com.chinaunicom.smartstorage.applet.dto.sso.CheckAccessTokenRespDTO;
import com.chinaunicom.smartstorage.applet.dto.sso.CheckRefreshTokenRespDTO;
import com.chinaunicom.smartstorage.applet.dto.sso.GetNewRefreshTokenRespDTO;
import com.chinaunicom.smartstorage.applet.dto.sso.GetNewTokenRespDTO;
import com.chinaunicom.smartstorage.common.result.ActionResult;

/**
 * @author liuchink
 * @since 2024/01/12 10:23
 */
public interface TokenSsoService {
    /**
     * token校验
     *
     * @param token token
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2021/8/31 13:54
     */
    ActionResult<CheckAccessTokenRespDTO, String> checkAccessToken(String token);

    /**
     * token校验
     *
     * @param token token
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2021/8/31 13:54
     */
    ActionResult<CheckRefreshTokenRespDTO, String> checkRefreshToken(String token);

    /**
     * token信息获取
     *
     * @param token token
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2021/8/31 13:54
     */
    ActionResult<MemberRespDTO, String> getAccessTokenInfo(String token);

    /**
     * token校验
     *
     * @param refreshToken refreshToken
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2021/8/31 13:54
     */
    ActionResult<GetNewTokenRespDTO, String> getNewAccessToken(String refreshToken);

    /**
     * token校验
     *
     * @param refreshToken refreshToken
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2021/8/31 13:54
     */
    ActionResult<GetNewRefreshTokenRespDTO, String> getNewRefreshToken(String refreshToken);
}
