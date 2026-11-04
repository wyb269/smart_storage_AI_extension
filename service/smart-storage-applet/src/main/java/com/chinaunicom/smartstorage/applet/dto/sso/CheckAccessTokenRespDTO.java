package com.chinaunicom.smartstorage.applet.dto.sso;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liuchink
 * @since 2023/8/30 16:47
 */
@Data
public class CheckAccessTokenRespDTO {
    @Schema(description = "accessToken过期时间", nullable = true, example = "12345")
    // @JSONField(name = "access_token_expires_in")
    private String accessTokenExpiresIn;
}
