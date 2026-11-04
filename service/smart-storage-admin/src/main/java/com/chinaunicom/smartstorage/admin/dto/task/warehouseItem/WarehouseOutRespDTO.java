package com.chinaunicom.smartstorage.admin.dto.task.warehouseItem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WarehouseOutRespDTO {
    @Schema(name ="id", description = "工作单id")
    private String id;

    @Schema(name ="weight", description = "空车称重")
    private BigDecimal weight;

    @Schema(name ="warehouseName", description = "仓库名")
    private String warehouseName;

    @Schema(name ="keeper", description = "库管员")
    private String keeper;

    @Schema(name ="grainVariety", description = "粮食品种")
    private String grainVariety;

    @Schema(name ="grainGrade", description = "粮食等级")
    private Integer grainGrade;
}
