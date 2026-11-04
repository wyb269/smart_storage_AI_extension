package com.chinaunicom.smartstorage.admin.dto.sso;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 账号密码登录
 *
 * @author liuchink
 * @since 2021/10/14 15:20
 */
@Data
public class PasswordLoginRepDTO {

    /**
     * 登录账号
     */
    @Schema(description = "账号", nullable = true, example = "12345")
    @NotBlank(message = "登录账号不能为空")
    private String loginId;
    /**
     * 登录密码
     */
    @Schema(description = "密码", nullable = true, example = "12345")
    @NotBlank(message = "密码不能为空")
    private String loginPwd;
    /**
     * 验证码
     */
    @Schema(description = "验证码", nullable = true, example = "12345")
    @NotBlank(message = "验证码不能为空")
    private String verCode;
    /**
     * key
     */
    @Schema(description = "key", nullable = true, example = "12345")
    @NotBlank(message = "key不能为空")
    private String key;
}
