package com.chinaunicom.smartstorage.applet.dto.sso;



import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SSO登录
 *
 * @author liuchink
 * @since 2021/10/14 15:20
 */
@Data
public class AccessTokenInfoRespDTO  {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", nullable = true, example = "12345")
    private String userId;
}
