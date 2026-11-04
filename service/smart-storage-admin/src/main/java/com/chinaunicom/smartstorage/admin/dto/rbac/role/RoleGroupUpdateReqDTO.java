package com.chinaunicom.smartstorage.admin.dto.rbac.role;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleGroupUpdateReqDTO {
    /**
     * id
     */
    @Schema(description = "id", nullable = true, example = "12345")
    @NotBlank(message = "ID不能为空")
    private String id;
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
    @Schema(description = "角色ID", nullable = true, example = "12345")
    private List<String> listRoleIds;
}