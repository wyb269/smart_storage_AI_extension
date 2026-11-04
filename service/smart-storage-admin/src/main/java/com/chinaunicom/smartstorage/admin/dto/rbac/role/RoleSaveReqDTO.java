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
public class RoleSaveReqDTO {

    /**
     * 机构id
     */
    @Schema(description = "机构id", nullable = true, example = "12345")
    @NotBlank(message = "机构id不能为空")
    private String orgId;
    /**
     * 行政区ID
     */
    @Schema(description = "行政区ID", nullable = true, example = "12345")
    @NotBlank(message = "行政区ID不能为空")
    private String regionId;
    /**
     * 名称
     */
    @Schema(description = "名称", nullable = true, example = "12345")
    @NotBlank(message = "名称不能为空")
    private String name;

    /**
     * 备注
     */
    @Schema(description = "备注", nullable = true, example = "12345")
    private String remark;
    // @Schema(description = "菜单ID", nullable = true, example = "12345")
    // private List<Integer> listMenuIds;
    // @Schema(description = "权限ID", nullable = true, example = "12345")
    // private List<Integer> listRightsIds;
}
