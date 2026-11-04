package com.chinaunicom.smartstorage.admin.dto.rbac.region;


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
public class RegionQueryReqDTO extends BaseQueryDTO {

    @Schema(description = "id", nullable = true, example = "123456")
    private String id;
    @Schema(description = "名称", nullable = true, example = "123456")
    private String name;
    @Schema(description = "父ID", nullable = true, example = "123456")
    private String parentId;
    @Schema(description = "父名称", nullable = true, example = "123456")
    private String parentName;
    @Schema(description = "级别", nullable = true, example = "123456")
    private Integer levels;
}
