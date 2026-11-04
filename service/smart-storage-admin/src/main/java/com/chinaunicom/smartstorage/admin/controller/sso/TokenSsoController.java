package com.chinaunicom.smartstorage.admin.controller.sso;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinaunicom.smartstorage.admin.aop.SecurityIgnore;
import com.chinaunicom.smartstorage.admin.dto.rbac.login.LoginBaseInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.CheckAccessTokenRespDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.CheckRefreshTokenRespDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.GetNewRefreshTokenRespDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.GetNewTokenRespDTO;
import com.chinaunicom.smartstorage.admin.service.inter.sso.TokenSsoService;
import com.chinaunicom.smartstorage.common.jwt.JwtConstant;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;

/**
 * @author liuchink
 * @since 2024/01/12 10:23
 */
@Tag(name = "SSO-TOKEN")
@RestController
@RequestMapping("/sso/admin/default/token")
@Slf4j
public class TokenSsoController {
    @Resource
    private TokenSsoService tokenSsoService;

    @Resource
    private MapperFacade mapperFacade;

    /**
     * token校验
     *
     * @param token token
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2021/8/31 13:54
     *
     */
    @Operation(summary = "accessToken校验")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/checkAccessToken", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<CheckAccessTokenRespDTO, String>
        checkAccessToken(@RequestHeader(JwtConstant.REQUEST_HEAD_TOKEN) String token) {
        return tokenSsoService.checkAccessToken(token);
    }

    /**
     * token校验
     *
     * @param token token
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2021/8/31 13:54
     *
     */
    @SecurityIgnore
    @Operation(summary = "refreshToken校验")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/checkRefreshToken", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<CheckRefreshTokenRespDTO, String>
        checkRefreshToken(@RequestHeader(JwtConstant.REQUEST_HEAD_TOKEN) String token) {
        return tokenSsoService.checkRefreshToken(token);
    }

    /**
     * token信息获取
     *
     * @param token token
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2021/8/31 13:54
     *
     */
    @Operation(summary = "accessToken信息获取")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getAccessTokenInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<LoginBaseInfoRespDTO, String>
        getAccessTokenInfo(@RequestHeader(JwtConstant.REQUEST_HEAD_TOKEN) String token) {
        return tokenSsoService.getAccessTokenInfo(token);
    }

    /**
     * token校验
     *
     * @param refreshToken refreshToken
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2021/8/31 13:54
     *
     */
    @SecurityIgnore
    @Operation(summary = "换取新-accessToken")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getNewAccessToken", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<GetNewTokenRespDTO, String>
        getNewAccessToken(@RequestHeader(JwtConstant.REQUEST_HEAD_TOKEN) String refreshToken) {
        return tokenSsoService.getNewAccessToken(refreshToken);
    }

    /**
     * token校验
     *
     * @param refreshToken refreshToken
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2021/8/31 13:54
     *
     */
    @SecurityIgnore
    @Operation(summary = "换取新-refreshToken")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getNewRefreshToken", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<GetNewRefreshTokenRespDTO, String>
        getNewRefreshToken(@RequestHeader(JwtConstant.REQUEST_HEAD_TOKEN) String refreshToken) {
        return tokenSsoService.getNewRefreshToken(refreshToken);
    }
}
