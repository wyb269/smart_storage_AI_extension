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
public class BindMemberByPhoneReqDTO {

    /**
     * 手机号码
     */
    @Schema(description = "openid", nullable = true, example = "12345")
    @NotBlank(message = "openid不能为空")
    private String openid;
    @Schema(description = "二维码参数", nullable = true, example = "12345")
    private String scene;
    /**
     * 短信内容
     */
    @Schema(description = "code", nullable = true, example = "12345")
    @NotBlank(message = "code不能为空")
    private String code;
}
