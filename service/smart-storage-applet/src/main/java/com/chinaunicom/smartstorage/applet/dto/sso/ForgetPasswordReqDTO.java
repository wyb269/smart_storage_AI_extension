package com.chinaunicom.smartstorage.applet.dto.sso;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 会员信息
 *
 * @author liuchink
 */
@Data
public class ForgetPasswordReqDTO {

    /**
     * 手机号码
     */
    @Schema(description = "phone", nullable = true, example = "12345")
    @NotBlank(message = "手机号码不能为空")
    private String phone;
    /**
     * 登陆密码
     */
    @Schema(description = "新密码", nullable = true, example = "12345")
    @NotBlank(message = "新密码不能为空")
    private String loginPassword;
    @Schema(description = "二次密码", nullable = true, example = "12345")
    @NotBlank(message = "二次密码不能为空")
    private String loginPasswordSecond;
    /**
     * 短信内容
     */
    @Schema(description = "code", nullable = true, example = "12345")
    @NotBlank(message = "验证码不能为空")
    private String code;
}
