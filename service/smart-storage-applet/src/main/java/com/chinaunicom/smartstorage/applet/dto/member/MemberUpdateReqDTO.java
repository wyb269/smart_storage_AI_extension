package com.chinaunicom.smartstorage.applet.dto.member;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 会员信息
 *
 * @author liuchink
 */
@Data
public class MemberUpdateReqDTO {
    /**
     * 昵称
     */
    @Schema(description = "昵称", nullable = true, example = "12345")
    @NotBlank(message = "昵称不能为空")
    private String nickname;
    /**
     * 头像
     */
    @Schema(description = "头像", nullable = true, example = "12345")
    @NotBlank(message = "头像不能为空")
    private String icon;
}
