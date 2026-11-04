package com.chinaunicom.smartstorage.admin.dto.sso;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liuchink
 * @since 2024/08/26 14:09
 */
@Data
public class VerCodeRespDTO {
    @Schema(description = "验证码", nullable = true, example = "12345")
    private String verCode;
    @Schema(description = "key", nullable = true, example = "12345")
    private String key;
}
