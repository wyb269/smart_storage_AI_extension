package com.chinaunicom.smartstorage.admin.dto.aggregation.sms;



import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 手机验证码
 *
 * @author liuchink
 * @since 2022/1/30 9:17
 */
@Data
public class CheckSmsVerifyCodeReqDTO  {

    /**
     * 手机号码
     */
    @Schema(description = "手机号码", nullable = true, example = "12345")
    @NotBlank(message = "手机号码不能为空")
    private String phone;

    /**
     * 短信内容
     */
    @Schema(description = "验证码", nullable = true, example = "12345")
    @NotBlank(message = "验证码不能为空")
    private String code;

}
