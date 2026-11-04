package com.chinaunicom.smartstorage.admin.dto.rbac.role;

import com.alibaba.fastjson2.annotation.JSONField;
import com.chinaunicom.smartstorage.admin.dto.rbac.menu.MenuRespDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.organization.OrganizationRespDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.region.RegionRespDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色表
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@Data
public class RoleRespDTO {

    /**
     * id
     */
    @Schema(description = "id", nullable = true, example = "12345")
    private String id;
    /**
     * 机构id
     */
    @Schema(description = "机构id", nullable = true, example = "12345")
    private String orgId;
    /**
     * 行政区ID
     */
    @Schema(description = "行政区ID", nullable = true, example = "12345")
    private String regionId;
    /**
     * 名称
     */
    @Schema(description = "名称", nullable = true, example = "12345")
    private String name;

    /**
     * 备注
     */
    @Schema(description = "备注", nullable = true, example = "12345")
    private String remark;

    /**
     * 添加人
     */
    // @Schema(description = "添加人", nullable = true, example = "12345")
    // private String addUser;

    /**
     * 添加时间
     */
    @Schema(description = "添加时间", nullable = true, example = "2020-01-01 00:00:00")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime addTime;

    /**
     * 菜单
     */
    @Schema(description = "菜单", nullable = true, example = "12345")
    List<MenuRespDTO> listMenu;
    /**
     * 权限
     */
    // @Schema(description = "权限", nullable = true, example = "12345")
    // List<RightsRespDTO> listRights;
    /**
     * 修改人
     */
    // @Schema(description = "修改人", nullable = true, example = "12345")
    // private String updateUser;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间", nullable = true, example = "2020-01-01 00:00:00")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    @Schema(description = "机构信息", nullable = true, example = "12345")
    private OrganizationRespDTO organization;
    @Schema(description = "行政区信息", nullable = true, example = "12345")
    private RegionRespDTO region;
    @Schema(description = "角色组信息", nullable = true, example = "12345")
    private List<RoleGroupRespDTO> listRoleGroup;
}
