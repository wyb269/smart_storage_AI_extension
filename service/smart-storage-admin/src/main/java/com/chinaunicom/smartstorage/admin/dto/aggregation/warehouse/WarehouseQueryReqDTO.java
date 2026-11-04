package com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class WarehouseQueryReqDTO {
    @Schema(name ="id", description = "id", required = false)
    private String id;
    /**
     * 仓库名
     */
    @Schema(name ="warehouseName", description = "仓库名", required = false)
    private String warehouseName;
    /**
     * 仓库类型（1平房仓 2立筒仓 3浅圆仓 4罩棚仓）
     */
    @Schema(name ="warehouseType", description = "仓库类型（1平房仓 2立筒仓 3浅圆仓 4罩棚仓）", required = false)
    private Integer warehouseType;
    /**
     * 粮食品种
     */
    @Schema(name ="grainVariety", description = "粮食品种", required = false)
    private String grainVariety;
    /**
     * 状态（1启用 2禁用）
     */
    @Schema(name ="state", description = "状态（1启用 2禁用）", required = false)
    private Integer state;
}
