package com.chinaunicom.smartstorage.admin.controller.sso;

import cn.hutool.core.util.IdUtil;
import com.chinaunicom.smartstorage.admin.aop.SecurityIgnore;
import com.chinaunicom.smartstorage.admin.constant.AdminSystemConstant;
import com.chinaunicom.smartstorage.admin.dto.sso.VerCodeRespDTO;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 滑动验证码
 *
 * @author liuchink
 * @since 2022/11/10 9:33
 */
@Tag(name = "SSO-验证码")
@Slf4j
@RestController
@RequestMapping("/sso/admin/default/captcha")
public class CaptchaController extends BaseController {
    public final static int WIDTH = 120;
    public final static int HEIGTH = 120;
    public final static int STR_LEN = 5;
    public final static int ARI_LEN = 3;
    public final static int CHI_LEN = 4;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     *
     * 验证码
     *
     * @author liuchink
     * @since 2021/8/31 13:54
     */
    @SecurityIgnore
    @Operation(summary = "获取字符验证码")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getVerCode", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<VerCodeRespDTO, String> getVerCode() {
        VerCodeRespDTO verCodeRespDTO = new VerCodeRespDTO();
        Captcha captcha = new SpecCaptcha(WIDTH, HEIGTH, STR_LEN);
        captcha.setCharType(Captcha.TYPE_ONLY_CHAR);
        String verCode = captcha.text().toLowerCase();
        verCodeRespDTO.setKey(IdUtil.fastSimpleUUID());
        verCodeRespDTO.setVerCode(verCode);
        setRedisValue(verCodeRespDTO);
        verCodeRespDTO.setVerCode(captcha.toBase64());
        log.debug("验证码：{}", verCode);
        return actionResultService.callBackResult("请求成功", verCodeRespDTO);
    }

    private void setRedisValue(VerCodeRespDTO verCodeRespDTO) {
        redisTemplate.opsForValue().set(AdminSystemConstant.VER_CODE_KEY + verCodeRespDTO.getKey(),
            verCodeRespDTO.getVerCode(), AdminSystemConstant.VER_CODE_KEY_TIME, TimeUnit.SECONDS);
    }
}
