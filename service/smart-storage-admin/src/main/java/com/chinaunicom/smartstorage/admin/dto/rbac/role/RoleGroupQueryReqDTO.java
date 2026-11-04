package com.chinaunicom.smartstorage.admin.dto.rbac.role;


import com.chinaunicom.smartstorage.common.bean.BaseQueryDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 角色信息RoleCondition
 *
 * @author liuchink
 * @since 2017/12/18 11:54
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class RoleGroupQueryReqDTO extends BaseQueryDTO {

    /**
     * 名称
     */
    @Schema(description = "名称", nullable = true, example = "123456")
    private String name;

}
