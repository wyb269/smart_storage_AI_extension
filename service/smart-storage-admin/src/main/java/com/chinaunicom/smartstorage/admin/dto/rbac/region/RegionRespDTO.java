package com.chinaunicom.smartstorage.admin.dto.rbac.region;



import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liuchink
 * @since 2023/7/31 9:15
 */
@Data
public class RegionRespDTO  {


    @Schema(description = "ID", nullable = true, example = "12345")
    private String id;
    @Schema(description = "父ID", nullable = true, example = "12345")
    private String parentId;
    @Schema(description = "父名称", nullable = true, example = "12345")
    private String parentName;
    @Schema(description = "名称", nullable = true, example = "12345")
    private String name;
    @Schema(description = "编码", nullable = true, example = "12345")
    private String code;
    @Schema(description = "级别", nullable = true, example = "12345")
    private Integer levels;
    @Schema(description = "全名", nullable = true, example = "12345")
    private String fullName;
    @Schema(description = "全ID", nullable = true, example = "12345")
    private String fullId;

}
