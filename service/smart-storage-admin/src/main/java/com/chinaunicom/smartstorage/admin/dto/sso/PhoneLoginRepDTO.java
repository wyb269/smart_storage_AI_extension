package com.chinaunicom.smartstorage.admin.dto.sso;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

/**
 * 手机号码
 *
 * @author liuchink
 * @since 2021/10/14 15:20
 */
@Data
public class PhoneLoginRepDTO {
    /**
     * 登录账号
     */
    @Schema(description = "手机号码", nullable = true, example = "12345")
    @NotBlank(message = "手机号码不能为空")
    private String phone;
    /**
     * 验证码
     */
    @Schema(description = "验证码", nullable = true, example = "12345")
    @NotBlank(message = "验证码不能为空")
    private String code;
}
