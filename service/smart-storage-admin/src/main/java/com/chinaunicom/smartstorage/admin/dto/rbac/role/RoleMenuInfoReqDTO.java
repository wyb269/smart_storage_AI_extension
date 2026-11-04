package com.chinaunicom.smartstorage.admin.dto.rbac.role;

import com.chinaunicom.smartstorage.admin.dto.rbac.rights.RightsReqDTO;
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
public class RoleMenuInfoReqDTO {

    /**
     * 菜单ID列表
     */
    @Schema(description = "菜单ID", nullable = true, example = "12345")
    @NotBlank(message = "菜单ID不能为空")
    private Integer id;
    /**
     * 菜单ID列表
     */
    @Schema(description = "权限信息", nullable = true, example = "12345")
    private List<RightsReqDTO> listRights;

}
