package com.chinaunicom.smartstorage.admin.dto.rbac.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 角色表
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@Data
public class RoleMenuReqDTO {

    /**
     * id
     */
    @Schema(description = "角色ID", nullable = true, example = "12345")
    @NotBlank(message = "角色ID不能为空")
    private String id;

    /**
     * 菜单ID列表
     */
    @Schema(description = "菜单信息", nullable = true, example = "12345")
    private List<RoleMenuInfoReqDTO> listMenu;

}
