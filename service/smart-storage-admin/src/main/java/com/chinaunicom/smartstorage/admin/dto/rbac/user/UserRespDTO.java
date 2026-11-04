package com.chinaunicom.smartstorage.admin.dto.rbac.user;

import com.chinaunicom.smartstorage.admin.dto.rbac.organization.OrganizationRespDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.region.RegionRespDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.role.RoleGroupRespDTO;
import com.chinaunicom.smartstorage.common.desensitize.Desensitization;
import com.chinaunicom.smartstorage.common.desensitize.DesensitizationTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@Data
public class UserRespDTO {

    /**
     * id
     */
    @Schema(description = "id", nullable = true, example = "12345")
    private String id;

    /**
     * 登陆账号
     */
    @Schema(description = "登陆账号", nullable = true, example = "12345")
    private String loginId;

    /**
     * 姓名
     */
    @Schema(description = "姓名", nullable = true, example = "12345")
    private String name;
    /**
     * 手机号码
     */
    @Desensitization(type = DesensitizationTypeEnum.MOBILE_PHONE, startInclude = 3, endExclude = 7)
    @Schema(description = "手机号码", nullable = true, example = "12345")
    private String phone;
    /**
     * 启用状态1=启用2=禁用
     */
    @Schema(description = "启用状态1=启用2=禁用", nullable = true, example = "12345")
    private Integer showState;
    /**
     * 添加人
     */
    // @Schema(description = "添加人", nullable = true, example = "12345")
    // private String addUser;

    /**
     * 添加时间
     */
    @Schema(description = "添加时间", nullable = true, example = "2020-01-01 00:00:00")
    private LocalDateTime addTime;

    /**
     * 机构信息
     */
    @Schema(description = "机构信息", nullable = true, example = "12345")
    private OrganizationRespDTO organization;
    /**
     * 角色信息
     */
    @Schema(description = "角色组信息", nullable = true, example = "12345")
    private RoleGroupRespDTO roleGroup;
    /**
     * 行政区名称
     */
    @Schema(description = "行政区", nullable = true, example = "12345")
    private RegionRespDTO region;
    /**
     * 机构ID
     */
    @Schema(description = "机构ID", nullable = true, example = "12345")
    private String orgId;
    /**
     * 角色ID
     */
    @Schema(description = "角色组ID", nullable = true, example = "12345")
    private String roleGroupId;
    /**
     * 行政区ID
     */
    @Schema(description = "行政区ID", nullable = true, example = "12345")
    private String regionId;
    @Schema(description = "头像", nullable = true, example = "12345")
    private String headUrl;
    @Schema(description = "用户类型", nullable = true, example = "12345")
    private Integer userType;
    @Schema(description = "在职状态1=在职;2=不在职", nullable = true, example = "12345")
    private Integer employedStatus;
}
