package com.chinaunicom.smartstorage.admin.security;

import cn.hutool.core.convert.Convert;
import com.baidu.fsg.uid.UidGenerator;
import com.google.common.collect.Maps;
import com.chinaunicom.smartstorage.admin.constant.AdminSystemConstant;
import com.chinaunicom.smartstorage.admin.dto.rbac.login.LoginUserRespDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.LoginLog;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;
import com.chinaunicom.smartstorage.admin.mapper.rbac.UserMapper;
import com.chinaunicom.smartstorage.admin.mapper.sso.LoginLogMapper;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.jwt.JwtConstant;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.chinaunicom.smartstorage.common.utils.IpUtils;
import com.liuchink.chinkutils.json.ChinkFastJson2Utils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 登录成功
 *
 * @author liuchink
 * @since 2020/6/11 9:47
 */
@Component
@Slf4j
public class UserLoginAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Resource
    private UserMapper userMapper;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private LoginLogMapper loginLogMapper;
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 登录成功
     *
     * @param httpServletRequest httpServletRequest
     * @param authentication authentication
     * @param httpServletResponse httpServletResponse
     *
     * @author liuchink
     * @since 2021/9/1 13:39
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
        Authentication authentication) {
        httpServletResponse.setContentType("application/json;charset=UTF-8");
        UserDetails userDetails = (UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ActionResult<LoginUserRespDTO, String> ssoActionResult =
            saveLoginCache(userDetails.getUsername(), httpServletRequest);
        ActionResult<LoginUserRespDTO, String> actionResult;
        if (ssoActionResult.getStatus().isSuccess()) {
            String loginFailCountKey =
                "login" + ":" + "fail" + ":" + "count" + ":" + "key" + ":" + userDetails.getUsername();
            redisTemplate.delete(loginFailCountKey);
            actionResult = actionResultService.callBackResult("登录成功", ssoActionResult.getResult());
        } else {
            actionResult = actionResultService.callBackResultFail(ssoActionResult.getStatus().getMessage(),
                ssoActionResult.getResult());
        }
        try {
            httpServletResponse.getWriter().append(ChinkFastJson2Utils.toJsonStringByNoNull(actionResult));
        } catch (IOException ioException) {
            throw new CustomException(ioException);
        }
    }

    /**
     *
     * 缓存登录
     *
     * @param loginId loginId
     * @author liuchink
     * @since 2021/10/14 13:38
     */
    public ActionResult<LoginUserRespDTO, String> saveLoginCache(String loginId,
        HttpServletRequest httpServletRequest) {
        ActionResult<LoginUserRespDTO, String> actionResultResp = actionResultService.callBackResultFail("缓存失败");
        // 记录登录日志
        String id = Convert.toStr(uidGenerator.getUID());
        LoginLog loginLog = new LoginLog();
        loginLog.setId(id);
        loginLog.setLogName("用户登录");
        loginLog.setLoginId(loginId);
        loginLog.setAddTime(LocalDateTime.now());
        loginLog.setIp(IpUtils.getRemoteHost(httpServletRequest));
        Map<String, String> loginLogMessageMap = Maps.newHashMap();
        loginLogMessageMap.put("loginId", loginId);
        loginLog.setMessage(ChinkFastJson2Utils.toJsonString(loginLogMessageMap));
        loginLogMapper.insert(loginLog);
        User userLogin = userMapper.login(loginId);
        if (userLogin != null) {
            // if (userLogin.getRole() != null) {
            // List<Menu> listMenu = userLogin.getRole().getListMenu().stream()
            // .sorted(Comparator.comparing(Menu::getOrderNum, Comparator.nullsLast(Integer::compareTo)))
            // .collect(Collectors.toList());
            // userLogin.getRole().setListMenu(listMenu);
            // }
            LoginUserRespDTO loginUserRespDTO = mapperFacade.map(userLogin, LoginUserRespDTO.class);
            loginUserRespDTO.setId(userLogin.getId());
            redisTemplate.opsForValue().set(AdminSystemConstant.REDIS_MANAG_RIGHT_KEY + loginId,
                ChinkFastJson2Utils.toJsonStringByNoNull(loginUserRespDTO),
                Convert.toInt(JwtConstant.TOKEN_EXPIRE_TIME / 1000), TimeUnit.SECONDS);

            // Map<String, Object> tokenMap = Maps.newHashMap();
            // tokenMap.put(JwtConstant.TOKEN_EXPIRE_TIME, loginUserRespDTO.getId());
            // String token = JwtUtils.createToken(JwtConstant.JWT_USER_SUB, loginId, tokenMap);
            // loginUserRespDTO.setToken(token);
            // loginUserRespDTO.setTokenExpires(JwtConstant.TOKEN_EXPIRE_TIME / 1000);
            actionResultResp = actionResultService.callBackResult("登录成功", loginUserRespDTO);
        }
        return actionResultResp;
    }
}
