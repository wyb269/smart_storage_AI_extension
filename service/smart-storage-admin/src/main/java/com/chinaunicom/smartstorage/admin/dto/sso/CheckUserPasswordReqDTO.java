package com.chinaunicom.smartstorage.admin.dto.sso;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户表
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@Data
public class CheckUserPasswordReqDTO {

    @Schema(description = "密码", nullable = true, example = "12345")
    private String loginPwd;

}
