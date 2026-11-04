package com.chinaunicom.smartstorage.admin.dto.rbac.role;



import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;


import lombok.Data;

/**
 * 角色表
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@Data
public class RoleRightsReqDTO  {


    /**
     * id
     */
    @Schema(description = "角色ID", nullable = true, example = "12345")
    @NotBlank(message = "角色ID不能为空")
    private String id;

    /**
     * 权限ID列表
     */
    @Schema(description = "权限ID", nullable = true, example = "12345")
    @NotBlank(message = "权限ID不能为空")
    private String rightsId;

}
