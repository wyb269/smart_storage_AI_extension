package com.chinaunicom.smartstorage.admin.dto.rbac.login;



import com.alibaba.fastjson2.annotation.JSONField;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liuchink
 * @since 2024/02/01 13:08
 */
@Data
public class LoginBaseInfoRespDTO  {

    @Schema( description = "用户", nullable = true, example = "12345")
    @JSONField(name = "user")
    private LoginUserRespDTO loginUserRespDTO;
    @Schema( description = "机构", nullable = true, example = "12345")
    @JSONField(name = "organization")
    private LoginOrganizationRespDTO loginOrganizationRespDTO;
    @Schema( description = "角色组", nullable = true, example = "12345")
    @JSONField(name = "roleGroup")
    private LoginRoleGroupRespDTO loginRoleGroupRespDTO;
    @Schema( description = "行政区", nullable = true, example = "12345")
    @JSONField(name = "region")
    private LoginRegionRespDTO loginRegionRespDTO;
}
