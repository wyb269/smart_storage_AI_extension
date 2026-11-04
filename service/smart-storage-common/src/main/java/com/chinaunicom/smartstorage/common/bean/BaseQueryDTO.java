package com.chinaunicom.smartstorage.common.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 条件查询基础类
 *
 * @author liuchink
 * @since 2017/12/18 10:57
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class BaseQueryDTO extends PageQueryDTO {
    @Schema(description = "userId", nullable = true, hidden = true, example = "12345")
    private String userId;
    @Schema(description = "regionId", nullable = true, hidden = true, example = "12345")
    private String regionId;
    @Schema(description = "orgId", nullable = true, hidden = true, example = "12345")
    private String orgId;

}
