package com.chinaunicom.smartstorage.applet.dto.sso;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

/**
 * @author liuchink
 * @since 2023/11/4 15:19
 */
@Data
@ToString(callSuper = true)
public class LoginRespDTO {
    @Schema(description = "accessToken", nullable = true, example = "12345")
    // @JSONField(name = "access_token")
    private String accessToken;
    @Schema(description = "accessToken过期时间", nullable = true, example = "12345")
    // @JSONField(name = "access_token_expires_in")
    private Integer accessTokenExpiresIn;
    @Schema(description = "refreshToken", nullable = true, example = "12345")
    // @JSONField(name = "refresh_token")
    private String refreshToken;
    @Schema(description = "refreshToken过期时间", nullable = true, example = "12345")
    // @JSONField(name = "refresh_token_expires_in")
    private Integer refreshTokenExpiresIn;
}
