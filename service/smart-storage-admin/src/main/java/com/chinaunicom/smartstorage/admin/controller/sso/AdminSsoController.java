package com.chinaunicom.smartstorage.admin.controller.sso;

import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.fastjson2.JSON;
import com.chinaunicom.smartstorage.admin.aop.SecurityIgnore;
import com.chinaunicom.smartstorage.admin.component.CodeGeneratorComponent;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.component.PwdComponent;
import com.chinaunicom.smartstorage.admin.constant.AdminSystemConstant;
import com.chinaunicom.smartstorage.admin.dto.rbac.login.LoginBaseInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.login.LoginMenuRespDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.login.LoginRespDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.*;
import com.chinaunicom.smartstorage.admin.dto.sso.*;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;
import com.chinaunicom.smartstorage.admin.service.inter.sso.AdminSsoService;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.jwt.JwtConstant;
import com.chinaunicom.smartstorage.common.jwt.JwtTokenUtils;
import com.chinaunicom.smartstorage.common.optlog.OptLog;
import com.chinaunicom.smartstorage.common.optlog.OptLogOperatorType;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * token访问
 *
 * @author liuchink
 * @since 2021/10/14 13:36
 */
@Tag(name = "SSO-用户信息")
@RestController
@RequestMapping("/sso/admin/default")
@Slf4j
public class AdminSsoController extends BaseController {
    @Resource
    private AdminSsoService adminSsoService;

    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private PwdComponent pwdComponent;
    @Resource
    private CodeGeneratorComponent codeGeneratorComponent;

    /**
     *
     * 登录
     *
     * @param passwordLoginRepDTO passwordLoginRepDTO
     *
     * @author liuchink
     * @since 2022/1/6 8:57
     */
    @SecurityIgnore
    @Operation(summary = "账号密码登录")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/loginByPassword", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<LoginRespDTO, String>
        loginByPassword(@RequestBody @Validated PasswordLoginRepDTO passwordLoginRepDTO) {
        final String verCode =
            redisTemplate.opsForValue().get(AdminSystemConstant.VER_CODE_KEY + passwordLoginRepDTO.getKey());
        redisTemplate.delete(AdminSystemConstant.VER_CODE_KEY + passwordLoginRepDTO.getKey());
        if (CharSequenceUtil.isBlank(verCode) || !verCode.equalsIgnoreCase(passwordLoginRepDTO.getVerCode())) {
            return actionResultService.callBackResultFail("验证码错误");
        }
        passwordLoginRepDTO.setLoginPwd(pwdComponent.getRsaDecryptAndEncryptMd5(passwordLoginRepDTO.getLoginPwd()));
        return adminSsoService.loginByPassword(passwordLoginRepDTO);
    }

    /**
     *
     * 登录
     *
     * @param phoneLoginRepDTO phoneLoginRepDTO
     *
     * @author liuchink
     * @since 2022/1/6 8:57
     */
    @SecurityIgnore
    @Operation(summary = "手机验证码登录")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/loginByPhone", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<LoginRespDTO, String> loginByPhone(@RequestBody @Validated PhoneLoginRepDTO phoneLoginRepDTO) {
        return adminSsoService.loginByPhone(phoneLoginRepDTO);
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
        String accessTokenKey =
            SystemConstant.ADMIN_LOGIN_ACCESS_TOKEN_KEY + JwtTokenUtils.getSubject(token, JwtConstant.JWT_ADMIN_SECRET);
        String refreshTokenKey = SystemConstant.ADMIN_LOGIN_REFRESH_TOKEN_KEY
            + JwtTokenUtils.getSubjectNoBearer(logoutReqDTO.getRefreshToken(), JwtConstant.JWT_ADMIN_SECRET_REFRESH);
        if (redisTemplate.hasKey(accessTokenKey)) {
            final String accessTokenUserInfoStr = redisTemplate.opsForValue().get(accessTokenKey);
            final User userAccessToken = JSON.parseObject(accessTokenUserInfoStr, User.class);
            redisTemplate.delete(accessTokenKey);
            if (redisTemplate.hasKey(refreshTokenKey)) {
                final String refreshTokenUserInfoStr = redisTemplate.opsForValue().get(refreshTokenKey);
                final User userRefreshToken = JSON.parseObject(refreshTokenUserInfoStr, User.class);
                if (userAccessToken != null && userRefreshToken != null
                    && userAccessToken.getId().equals(userRefreshToken.getId())) {
                    redisTemplate.delete(refreshTokenKey);
                }
            }
        }

        return actionResultService.callBackResult();
    }

    /**
     * 获取用户登录信息
     *
     * @author liuchink
     * @since 2018/8/15 13:41
     */
    @Operation(summary = "获取用户基本信息")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getLoginBaseInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<LoginBaseInfoRespDTO, String> getLoginBaseInfo() {
        return adminSsoService.getLoginBaseInfo();
    }

    /**
     * 修改用户密码
     *
     * @param updateUserPasswordReqDTO updateUserPasswordReqDTO
     * @author liuchink
     * @since 2018/8/15 13:41
     */
    @SecurityIgnore
    @Operation(summary = "修改用户密码-账号")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "用户管理", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/updateUserLoginPasswordByLoginId", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String>
        updateUserLoginPasswordByLoginId(@RequestBody @Validated UpdateUserPasswordReqDTO updateUserPasswordReqDTO) {
        ActionResult<String, String> actionResult =
            actionResultService.callBackResultFail("密码必须包含大小字母、数字、特殊字符，最少8个字符，最多15个字符");
        String regex = "(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*[^a-zA-Z0-9]).{8,15}";
        updateUserPasswordReqDTO.setOldPassword(
            pwdComponent.getEncryptMd5(pwdComponent.getRsaDecrypt(updateUserPasswordReqDTO.getOldPassword())));
        updateUserPasswordReqDTO
            .setLoginPassword(pwdComponent.getRsaDecrypt(updateUserPasswordReqDTO.getLoginPassword()));
        updateUserPasswordReqDTO
            .setLoginPasswordSecond(pwdComponent.getRsaDecrypt(updateUserPasswordReqDTO.getLoginPasswordSecond()));
        if (updateUserPasswordReqDTO.getLoginPassword().matches(regex)) {
            actionResult = adminSsoService.updateUserLoginPasswordByLoginId(updateUserPasswordReqDTO);
        }

        return actionResult;
    }

    /**
     * 修改用户密码
     *
     * @param updateUserPasswordReqDTO updateUserPasswordReqDTO
     * @author liuchink
     * @since 2018/8/15 13:41
     */
    @Operation(summary = "修改用户密码")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "用户管理", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/updateUserLoginPassword", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String>
        updateUserLoginPassword(@RequestBody @Validated UpdateUserPasswordReqDTO updateUserPasswordReqDTO) {
        ActionResult<String, String> actionResult =
            actionResultService.callBackResultFail("密码必须包含大小字母、数字、特殊字符，最少8个字符，最多15个字符");
        String regex = "(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*[^a-zA-Z0-9]).{8,15}";
        updateUserPasswordReqDTO.setOldPassword(
            pwdComponent.getEncryptMd5(pwdComponent.getRsaDecrypt(updateUserPasswordReqDTO.getOldPassword())));
        updateUserPasswordReqDTO
            .setLoginPassword(pwdComponent.getRsaDecrypt(updateUserPasswordReqDTO.getLoginPassword()));
        updateUserPasswordReqDTO
            .setLoginPasswordSecond(pwdComponent.getRsaDecrypt(updateUserPasswordReqDTO.getLoginPasswordSecond()));
        if (updateUserPasswordReqDTO.getLoginPassword().matches(regex)) {
            updateUserPasswordReqDTO.setId(jwtTokenComponent.getUser().getId());
            actionResult = adminSsoService.updateUserLoginPassword(updateUserPasswordReqDTO);
        }
        return actionResult;
    }

    /**
     * 获取用户菜单信息
     *
     * @author liuchink
     * @since 2018/8/15 13:41
     */
    @Operation(summary = "获取用户菜单信息")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getLoginMenuInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<List<LoginMenuRespDTO>, String> getLoginMenuInfo() {
        return adminSsoService.getLoginMenuInfo();
    }

    /**
     * 获取用户角色信息
     *
     * @author liuchink
     * @since 2018/8/15 13:41
     */
    // @Operation(summary = "获取用户权限信息")
    // @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    // @PostMapping(path = "/getLoginRightsInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    // public ActionResult<List<LoginRightsRespDTO>, String> getLoginRightsInfo() {
    // return adminSsoService.getLoginRightsInfo();
    // }

    /**
     * 修改
     *
     * @param userUpdateBaseReqDTO UserUpdateReqDTO
     * @author liuchink
     * @since 2020/4/8 21:24
     */
    @SecurityRequirement(name = HttpHeaders.AUTHORIZATION)
    @Operation(summary = "修改基本信息")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "用户管理", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/updateUserBase", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> updateUserBase(@RequestHeader(JwtConstant.REQUEST_HEAD_TOKEN) String token,
        @RequestBody @Validated UserUpdateBaseReqDTO userUpdateBaseReqDTO) {
        User user = mapperFacade.map(userUpdateBaseReqDTO, User.class);
        user.setId(jwtTokenComponent.getUser().getId());
        user.setLoginId(jwtTokenComponent.getUser().getLoginId());
        user.setUpdateUser(jwtTokenComponent.getUser().getId());
        user.setUpdateTime(LocalDateTime.now());
        return adminSsoService.updateUserBase(token, user);
    }
}
