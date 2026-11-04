package com.chinaunicom.smartstorage.applet.service.impl.sso;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.chinaunicom.smartstorage.applet.dto.member.MemberRespDTO;
import com.chinaunicom.smartstorage.applet.dto.sso.CheckAccessTokenRespDTO;
import com.chinaunicom.smartstorage.applet.dto.sso.CheckRefreshTokenRespDTO;
import com.chinaunicom.smartstorage.applet.dto.sso.GetNewRefreshTokenRespDTO;
import com.chinaunicom.smartstorage.applet.dto.sso.GetNewTokenRespDTO;
import com.chinaunicom.smartstorage.applet.entity.member.Member;
import com.chinaunicom.smartstorage.applet.service.inter.sso.TokenSsoService;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.jwt.JwtConstant;
import com.chinaunicom.smartstorage.common.jwt.JwtTokenUtils;
import com.chinaunicom.smartstorage.common.jwt.JwtUtils;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.liuchink.chinkutils.date.ChinkDateFormatUtils;
import com.liuchink.chinkutils.date.ChinkDateOperationUtils;
import com.liuchink.chinkutils.json.ChinkFastJson2Utils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

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

    @Override
    public ActionResult<CheckAccessTokenRespDTO, String> checkAccessToken(String token) {
        JwtUtils jwtUtils = new JwtUtils(JwtConstant.JWT_APPLET_SECRET);
        final Jws<Claims> claimsJws = jwtUtils.parseToken(token);
        CheckAccessTokenRespDTO checkAccessTokenRespDTO = new CheckAccessTokenRespDTO();
        checkAccessTokenRespDTO
            .setAccessTokenExpiresIn(ChinkDateFormatUtils.toDateTime(claimsJws.getPayload().getExpiration())
                .toString(ChinkDateFormatUtils.DATEFORMAT_YYYY_MM_DD_HH_MM_SS));
        return actionResultService.callBackResult(checkAccessTokenRespDTO);
    }

    @Override
    public ActionResult<CheckRefreshTokenRespDTO, String> checkRefreshToken(String token) {
        JwtUtils jwtUtils = new JwtUtils(JwtConstant.JWT_APPLET_SECRET_REFRESH);
        final Jws<Claims> claimsJws = jwtUtils.parseToken(token);
        CheckRefreshTokenRespDTO checkRefreshTokenRespDTO = new CheckRefreshTokenRespDTO();
        checkRefreshTokenRespDTO
            .setRefreshTokenExpiresIn(ChinkDateFormatUtils.toDateTime(claimsJws.getPayload().getExpiration())
                .toString(ChinkDateFormatUtils.DATEFORMAT_YYYY_MM_DD_HH_MM_SS));
        return actionResultService.callBackResult(checkRefreshTokenRespDTO);
    }

    @Override
    public ActionResult<MemberRespDTO, String> getAccessTokenInfo(String token) {
        final Member member =
            JSON.parseObject(redisTemplate.opsForValue().get(SystemConstant.APPLET_LOGIN_ACCESS_TOKEN_KEY
                + JwtTokenUtils.getSubject(token, JwtConstant.JWT_APPLET_SECRET)), Member.class);
        if (member != null) {
            final MemberRespDTO memberRespDTO = mapperFacade.map(member, MemberRespDTO.class);
            return actionResultService.callBackResult(memberRespDTO);
        }
        return actionResultService.callBackResultFail("token信息不存在");
    }

    @Override
    public ActionResult<GetNewTokenRespDTO, String> getNewAccessToken(String refreshToken) {
        GetNewTokenRespDTO getNewTokenRespDTO = new GetNewTokenRespDTO();
        String refreshTokenKey = SystemConstant.APPLET_LOGIN_REFRESH_TOKEN_KEY
            + JwtTokenUtils.getSubject(refreshToken, JwtConstant.JWT_APPLET_SECRET_REFRESH);
        if (redisTemplate.hasKey(refreshTokenKey)) {
            final Member member = JSON.parseObject(redisTemplate.opsForValue().get(refreshTokenKey), Member.class);
            // accessToken
            String accessTokenSub = IdUtil.fastSimpleUUID();
            JwtUtils jwtUtilsAccessToken = new JwtUtils(JwtConstant.JWT_APPLET_SECRET);
            redisTemplate.opsForValue().set(SystemConstant.APPLET_LOGIN_ACCESS_TOKEN_KEY + accessTokenSub,
                ChinkFastJson2Utils.toJsonString(member), JwtConstant.TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);
            String accessToken =
                jwtUtilsAccessToken.createToken(JwtConstant.JWT_ISS_APPLET, accessTokenSub, JwtConstant.JWT_AUD_APPLET);
            getNewTokenRespDTO.setAccessToken(accessToken);
            getNewTokenRespDTO.setAccessTokenExpiresIn(JwtConstant.TOKEN_EXPIRE_TIME);
            // refreshToken
            // String refreshTokenSub = IdUtil.fastSimpleUUID();
            // JwtUtils jwtUtilsRefreshToken = new JwtUtils(JwtConstant.JWT_APPLET_SECRET_REFRESH);
            // redisTemplate.opsForValue().set(SystemConstant.APPLET_LOGIN_REFRESH_TOKEN_KEY + refreshTokenSub,
            // ChinkFastJson2Utils.toJsonString(member), JwtConstant.TOKEN_REFRESH_EXPIRE_TIME, TimeUnit.SECONDS);
            // String newRefreshToken = jwtUtilsRefreshToken.createToken(JwtConstant.JWT_ISS_APPLET, refreshTokenSub,
            // JwtConstant.JWT_AUD_APPLET,
            // ChinkDateOperationUtils.addSeconds(new Date(), JwtConstant.TOKEN_REFRESH_EXPIRE_TIME).toDate());
            // getNewTokenRespDTO.setRefreshToken(newRefreshToken);
            // getNewTokenRespDTO.setRefreshTokenExpiresIn(JwtConstant.TOKEN_REFRESH_EXPIRE_TIME);
            // redisTemplate.delete(refreshTokenKey);
            return actionResultService.callBackResult(getNewTokenRespDTO);
        }
        return actionResultService.callBackResultFail("token信息不存在");
    }

    @Override
    public ActionResult<GetNewRefreshTokenRespDTO, String> getNewRefreshToken(String refreshToken) {
        GetNewRefreshTokenRespDTO getNewRefreshTokenRespDTO = new GetNewRefreshTokenRespDTO();
        String refreshTokenKey = SystemConstant.APPLET_LOGIN_REFRESH_TOKEN_KEY
            + JwtTokenUtils.getSubject(refreshToken, JwtConstant.JWT_APPLET_SECRET_REFRESH);
        if (redisTemplate.hasKey(refreshTokenKey)) {
            final Member member = JSON.parseObject(redisTemplate.opsForValue().get(refreshTokenKey), Member.class);
            // refreshToken
            String refreshTokenSub = IdUtil.fastSimpleUUID();
            JwtUtils jwtUtilsRefreshToken = new JwtUtils(JwtConstant.JWT_APPLET_SECRET_REFRESH);
            redisTemplate.opsForValue().set(SystemConstant.APPLET_LOGIN_REFRESH_TOKEN_KEY + refreshTokenSub,
                ChinkFastJson2Utils.toJsonString(member), JwtConstant.TOKEN_REFRESH_EXPIRE_TIME, TimeUnit.SECONDS);
            String newRefreshToken = jwtUtilsRefreshToken.createToken(JwtConstant.JWT_ISS_APPLET, refreshTokenSub,
                JwtConstant.JWT_AUD_APPLET,
                ChinkDateOperationUtils.addSeconds(new Date(), JwtConstant.TOKEN_REFRESH_EXPIRE_TIME).toDate());
            getNewRefreshTokenRespDTO.setRefreshToken(newRefreshToken);
            getNewRefreshTokenRespDTO.setRefreshTokenExpiresIn(JwtConstant.TOKEN_REFRESH_EXPIRE_TIME);
            redisTemplate.delete(refreshTokenKey);
            return actionResultService.callBackResult(getNewRefreshTokenRespDTO);
        }
        return actionResultService.callBackResultFail("token信息不存在");
    }

}
