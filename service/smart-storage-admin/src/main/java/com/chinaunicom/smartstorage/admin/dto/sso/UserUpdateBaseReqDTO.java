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
public class UserUpdateBaseReqDTO {

    /**
     * 姓名
     */
    @Schema(description = "姓名", nullable = true, example = "12345")
    // @NotBlank(message = "姓名不能为空")
    private String name;
    @Schema(description = "头像", nullable = true, example = "12345")
    private String headUrl;
}
