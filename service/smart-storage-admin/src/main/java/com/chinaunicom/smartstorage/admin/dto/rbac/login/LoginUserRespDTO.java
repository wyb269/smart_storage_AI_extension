package com.chinaunicom.smartstorage.admin.dto.rbac.login;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户登录
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@Data
public class LoginUserRespDTO {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", nullable = true, example = "12345")
    private String id;
    /**
     * 登录ID
     */
    @Schema(description = "登录ID", nullable = true, example = "12345")
    private String loginId;
    @Schema(description = "名称", nullable = true, example = "12345")
    private String name;
    @Schema(description = "手机号码", nullable = true, example = "12345")
    private String phone;
    /**
     * 1=超级管理员，2=其他
     */
    @Schema(description = "用户类型 1=超级管理员，2=其他", nullable = true, example = "12345")
    private Integer userType;
    @Schema(description = "头像", nullable = true, example = "12345")
    private String headUrl;
    @Schema(description = "在职状态1=在职;2=不在职", nullable = true, example = "12345")
    private Integer employedStatus;
}
