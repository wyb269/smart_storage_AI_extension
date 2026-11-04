package com.chinaunicom.smartstorage.admin.dto.sso;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liuchink
 * @since 2024/02/01 13:08
 */
@Data
public class GetNewRefreshTokenRespDTO {

    @Schema(description = "refreshToken", nullable = true, example = "12345")
    // @JSONField(name = "refresh_token")
    private String refreshToken;
    @Schema(description = "refreshToken过期时间", nullable = true, example = "12345")
    // @JSONField(name = "refresh_token_expires_in")
    private Integer refreshTokenExpiresIn;
}
