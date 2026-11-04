package com.chinaunicom.smartstorage.admin.dto.rbac.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

/**
 * 用户表
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@Data
public class UserUpdateReqDTO {

    /**
     * 主键ID
     */
    @Schema(description = "id", nullable = true, example = "12345")
    @NotBlank(message = "ID不能为空")
    private String id;
    /**
     * 机构id
     */
    @Schema(description = "机构ID", nullable = true, example = "12345")
    @NotBlank(message = "机构ID不能为空")
    private String orgId;
    /**
     * 行政区
     */
    @Schema(description = "行政区ID", nullable = true, example = "12345")
    @NotBlank(message = "行政区ID不能为空")
    private String regionId;
    /**
     * 角色id
     */
    @Schema(description = "角色组ID", nullable = true, example = "12345")
    @NotBlank(message = "角色组ID不能为空")
    private String roleGroupId;

    /**
     * 姓名
     */
    @Schema(description = "姓名", nullable = true, example = "12345")
    @NotBlank(message = "姓名不能为空")
    private String name;

    /**
     * 手机号码
     */
    // @Schema(description = "手机号码", nullable = true, example = "12345")
    // @NotNull(message = "手机号码不能为空")
    // private String phone;
    @Schema(description = "头像", nullable = true, example = "12345")
    private String headUrl;
    /**
     * 1=超级管理员，2=其他
     */
    @Schema(description = "账号类型，1=超级管理员，2=其他", nullable = true, example = "12345")
    @NotNull(message = "账号类型不能为空")
    @Range(min = 1, max = 2, message = "账号类型数据不合法")
    private Integer userType;
}
