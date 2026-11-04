package com.chinaunicom.smartstorage.applet.dto.sso;

import java.io.Serial;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liuchink
 * @since 2023/11/4 15:19
 */
@Data
public class MemberLoginRespDTO  {

    @Schema(description = "openId")
    private String openId;
    @Schema(description = "sessionKey")
    private String sessionKey;
    @Schema(description = "unionid")
    private String unionid;
    @Schema(description = "token")
    private String token;
}
