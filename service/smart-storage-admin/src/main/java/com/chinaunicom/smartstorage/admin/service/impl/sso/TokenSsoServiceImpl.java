package com.chinaunicom.smartstorage.admin.service.impl.sso;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.chinaunicom.smartstorage.admin.dto.rbac.login.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.chinaunicom.smartstorage.admin.component.LoginComponent;
import com.chinaunicom.smartstorage.admin.dto.rbac.login.*;
import com.chinaunicom.smartstorage.admin.dto.sso.CheckAccessTokenRespDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.CheckRefreshTokenRespDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.GetNewRefreshTokenRespDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.GetNewTokenRespDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;
import com.chinaunicom.smartstorage.admin.service.inter.sso.TokenSsoService;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.jwt.JwtConstant;
import com.chinaunicom.smartstorage.common.jwt.JwtTokenUtils;
import com.chinaunicom.smartstorage.common.jwt.JwtUtils;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.liuchink.chinkutils.date.ChinkDateFormatUtils;
import com.liuchink.chinkutils.date.ChinkDateOperationUtils;
import com.liuchink.chinkutils.json.ChinkFastJson2Utils;

import cn.hutool.core.util.IdUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;

/**
 * @author liuchink
 * @since 2024/01/12 10:23
 */
@Service
@Slf4j
public class TokenSsoServiceImpl implements TokenSsoService {
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private LoginComponent loginComponent;

    @Override
    public ActionResult<CheckAccessTokenRespDTO, String> checkAccessToken(String token) {
        JwtUtils jwtUtils = new JwtUtils(JwtConstant.JWT_ADMIN_SECRET);
        final Jws<Claims> claimsJws = jwtUtils.parseToken(token);
        CheckAccessTokenRespDTO checkAccessTokenRespDTO = new CheckAccessTokenRespDTO();
        checkAccessTokenRespDTO
            .setAccessTokenExpiresIn(ChinkDateFormatUtils.toDateTime(claimsJws.getPayload().getExpiration())
                .toString(ChinkDateFormatUtils.DATEFORMAT_YYYY_MM_DD_HH_MM_SS));
        return actionResultService.callBackResult(checkAccessTokenRespDTO);
    }

    @Override
    public ActionResult<CheckRefreshTokenRespDTO, String> checkRefreshToken(String token) {
        JwtUtils jwtUtils = new JwtUtils(JwtConstant.JWT_ADMIN_SECRET_REFRESH);
        final Jws<Claims> claimsJws = jwtUtils.parseToken(token);
        CheckRefreshTokenRespDTO checkRefreshTokenRespDTO = new CheckRefreshTokenRespDTO();
        checkRefreshTokenRespDTO
            .setRefreshTokenExpiresIn(ChinkDateFormatUtils.toDateTime(claimsJws.getPayload().getExpiration())
                .toString(ChinkDateFormatUtils.DATEFORMAT_YYYY_MM_DD_HH_MM_SS));
        return actionResultService.callBackResult(checkRefreshTokenRespDTO);
    }

    @Override
    public ActionResult<LoginBaseInfoRespDTO, String> getAccessTokenInfo(String token) {
        final User user = JSON.parseObject(redisTemplate.opsForValue().get(SystemConstant.ADMIN_LOGIN_ACCESS_TOKEN_KEY
            + JwtTokenUtils.getSubject(token, JwtConstant.JWT_ADMIN_SECRET)), User.class);
        if (user != null) {
            LoginUserRespDTO loginUserRespDTO = mapperFacade.map(user, LoginUserRespDTO.class);
            LoginOrganizationRespDTO loginOrganizationRespDTO =
                mapperFacade.map(user.getOrganization(), LoginOrganizationRespDTO.class);
            LoginRoleGroupRespDTO loginRoleGroupRespDTO =
                mapperFacade.map(user.getRoleGroup(), LoginRoleGroupRespDTO.class);
            LoginRegionRespDTO loginRegionRespDTO = mapperFacade.map(user.getRegion(), LoginRegionRespDTO.class);
            LoginBaseInfoRespDTO loginBaseInfoRespDTO = new LoginBaseInfoRespDTO();
            loginBaseInfoRespDTO.setLoginUserRespDTO(loginUserRespDTO);
            loginBaseInfoRespDTO.setLoginOrganizationRespDTO(loginOrganizationRespDTO);
            loginBaseInfoRespDTO.setLoginRoleGroupRespDTO(loginRoleGroupRespDTO);
            loginBaseInfoRespDTO.setLoginRegionRespDTO(loginRegionRespDTO);
            return actionResultService.callBackResult(loginBaseInfoRespDTO);
        }
        return actionResultService.callBackResultFail("token信息不存在");
    }

    @Override
    public ActionResult<GetNewTokenRespDTO, String> getNewAccessToken(String refreshToken) {
        GetNewTokenRespDTO getNewTokenRespDTO = new GetNewTokenRespDTO();
        String refreshTokenKey = SystemConstant.ADMIN_LOGIN_REFRESH_TOKEN_KEY
            + JwtTokenUtils.getSubject(refreshToken, JwtConstant.JWT_ADMIN_SECRET_REFRESH);
        if (redisTemplate.hasKey(refreshTokenKey)) {
            final User user = JSON.parseObject(redisTemplate.opsForValue().get(refreshTokenKey), User.class);
            if (user != null) {
                loginComponent.saveLoginRightsCache(user);
                // accessToken
                String accessTokenSub = IdUtil.fastSimpleUUID();
                JwtUtils jwtUtilsAccessToken = new JwtUtils(JwtConstant.JWT_ADMIN_SECRET);
                redisTemplate.opsForValue().set(SystemConstant.ADMIN_LOGIN_ACCESS_TOKEN_KEY + accessTokenSub,
                    ChinkFastJson2Utils.toJsonString(user), JwtConstant.TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
                String accessToken = jwtUtilsAccessToken.createToken(JwtConstant.JWT_ISS_ADMIN, accessTokenSub,
                    JwtConstant.JWT_AUD_ADMIN);
                getNewTokenRespDTO.setAccessToken(accessToken);
                getNewTokenRespDTO.setAccessTokenExpiresIn(JwtConstant.TOKEN_EXPIRE_TIME);
                // refreshToken
                // String refreshTokenSub = IdUtil.fastSimpleUUID();
                // JwtUtils jwtUtilsRefreshToken = new JwtUtils(JwtConstant.JWT_ADMIN_SECRET_REFRESH);
                // redisTemplate.opsForValue().set(SystemConstant.ADMIN_LOGIN_REFRESH_TOKEN_KEY + refreshTokenSub,
                // ChinkFastJson2Utils.toJsonString(user), JwtConstant.TOKEN_REFRESH_EXPIRE_TIME, TimeUnit.SECONDS);
                // String newRefreshToken = jwtUtilsRefreshToken.createToken(JwtConstant.JWT_ISS_ADMIN, refreshTokenSub,
                // JwtConstant.JWT_AUD_ADMIN,
                // ChinkDateOperationUtils.addSeconds(new Date(), JwtConstant.TOKEN_REFRESH_EXPIRE_TIME).toDate());
                // getNewTokenRespDTO.setRefreshToken(newRefreshToken);
                // getNewTokenRespDTO.setRefreshTokenExpiresIn(JwtConstant.TOKEN_REFRESH_EXPIRE_TIME);
                // redisTemplate.delete(refreshTokenKey);
                return actionResultService.callBackResult(getNewTokenRespDTO);
            }
        }
        return actionResultService.callBackResultFail("token信息不存在");

    }

    @Override
    public ActionResult<GetNewRefreshTokenRespDTO, String> getNewRefreshToken(String refreshToken) {
        GetNewRefreshTokenRespDTO getNewRefreshTokenRespDTO = new GetNewRefreshTokenRespDTO();
        String refreshTokenKey = SystemConstant.ADMIN_LOGIN_REFRESH_TOKEN_KEY
            + JwtTokenUtils.getSubject(refreshToken, JwtConstant.JWT_ADMIN_SECRET_REFRESH);
        if (redisTemplate.hasKey(refreshTokenKey)) {
            final User user = JSON.parseObject(redisTemplate.opsForValue().get(refreshTokenKey), User.class);
            if (user != null) {
                // refreshToken
                String refreshTokenSub = IdUtil.fastSimpleUUID();
                JwtUtils jwtUtilsRefreshToken = new JwtUtils(JwtConstant.JWT_ADMIN_SECRET_REFRESH);
                redisTemplate.opsForValue().set(SystemConstant.ADMIN_LOGIN_REFRESH_TOKEN_KEY + refreshTokenSub,
                    ChinkFastJson2Utils.toJsonString(user), JwtConstant.TOKEN_REFRESH_EXPIRE_TIME, TimeUnit.SECONDS);
                String newRefreshToken = jwtUtilsRefreshToken.createToken(JwtConstant.JWT_ISS_ADMIN, refreshTokenSub,
                    JwtConstant.JWT_AUD_ADMIN,
                    ChinkDateOperationUtils.addSeconds(new Date(), JwtConstant.TOKEN_REFRESH_EXPIRE_TIME).toDate());
                getNewRefreshTokenRespDTO.setRefreshToken(newRefreshToken);
                getNewRefreshTokenRespDTO.setRefreshTokenExpiresIn(JwtConstant.TOKEN_REFRESH_EXPIRE_TIME);
                redisTemplate.delete(refreshTokenKey);
                return actionResultService.callBackResult(getNewRefreshTokenRespDTO);
            }

        }
        return actionResultService.callBackResultFail("token信息不存在");
    }

}
