package com.chinaunicom.smartstorage.admin.dto.rbac.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户表
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@Data
public class UserUpdateEmployedStatusReqDTO {

    /**
     * 主键ID
     */
    @Schema(description = "id", nullable = true, example = "12345")
    @NotBlank(message = "ID不能为空")
    private String id;
    @NotNull(message = "在职状态不能为空")
    @Schema(description = "在职状态1=在职;2=不在职", nullable = true, example = "12345")
    private Integer employedStatus;

}
