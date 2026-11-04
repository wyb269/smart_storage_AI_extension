package com.chinaunicom.smartstorage.admin.service.inter.sso;

import com.chinaunicom.smartstorage.admin.dto.rbac.login.LoginBaseInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.CheckAccessTokenRespDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.CheckRefreshTokenRespDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.GetNewRefreshTokenRespDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.GetNewTokenRespDTO;
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
    ActionResult<LoginBaseInfoRespDTO, String> getAccessTokenInfo(String token);

    /**
     * token校验 refreshToken
     *
     * @param refreshToken token
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
