package com.chinaunicom.smartstorage.admin.dto.rbac.login;



import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户登录
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@Data
public class LoginRoleGroupRespDTO  {

    /**
     * 角色ID
     */
    @Schema(description = "角色ID", nullable = true, example = "12345")
    private String id;
    /**
     * 名称
     */
    @Schema(description = "名称", nullable = true, example = "12345")
    private String name;
}
