package com.chinaunicom.smartstorage.applet.dto.member;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户表
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@Data
public class MemberUpdatePasswordReqDTO {

    /**
     * 主键ID
     */
    @Schema(description = "用户ID", nullable = true, hidden = true, example = "12345")
    private String id;
    /**
     * 旧密码
     */
    @Schema(description = "旧密码", nullable = true, example = "12345")
    @NotBlank(message = "原始密码不能为空")
    private String oldPassword;
    /**
     * 登陆密码
     */
    @Schema(description = "新密码", nullable = true, example = "12345")
    @NotBlank(message = "新密码不能为空")
    private String loginPassword;
    /**
     * 登陆密码
     */
    @Schema(description = "新密码确认", nullable = true, example = "12345")
    @NotBlank(message = "新密码确认不能为空")
    private String loginPasswordSecond;

}
