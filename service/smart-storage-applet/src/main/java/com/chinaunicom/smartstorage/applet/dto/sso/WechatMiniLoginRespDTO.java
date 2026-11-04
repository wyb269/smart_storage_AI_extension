package com.chinaunicom.smartstorage.applet.dto.sso;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author liuchink
 * @since 2023/11/4 15:19
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class WechatMiniLoginRespDTO extends LoginRespDTO {
    @Schema(description = "openId", nullable = true, example = "12345")
    private String openid;
    @Schema(description = "sessionKey", nullable = true, example = "12345")
    private String sessionKey;
    @Schema(description = "unionid", nullable = true, example = "12345")
    private String unionid;
}
