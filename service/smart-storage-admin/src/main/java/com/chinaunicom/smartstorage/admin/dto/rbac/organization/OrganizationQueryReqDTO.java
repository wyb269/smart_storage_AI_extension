package com.chinaunicom.smartstorage.admin.dto.rbac.organization;

import com.chinaunicom.smartstorage.common.bean.BaseQueryDTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 组织机构查询条件
 *
 * @author liuchink
 * @since 2017/12/18 11:54
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class OrganizationQueryReqDTO extends BaseQueryDTO {
    @Schema(description = "机构ID", nullable = true, example = "123456")
    private String orgId;
    /**
     * 机构名称
     */
    @Schema(description = "机构名称", nullable = true, example = "123456")
    private String name;
}
