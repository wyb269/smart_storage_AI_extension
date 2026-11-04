package com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WarehouseQueryRespDTO {
    @Schema(name ="id", description = "id")
    private String id;
    /**
     * 仓库名
     */
    @Schema(name ="warehouseName", description = "仓库名")
    private String warehouseName;
    /**
     * 仓库类型（1平房仓 2立筒仓 3浅圆仓 4罩棚仓）
     */
    @Schema(name ="warehouseType", description = "仓库类型（1平房仓 2立筒仓 3浅圆仓 4罩棚仓）")
    private Integer warehouseType;
    /**
     * 库管员
     */
    @Schema(name ="keeper", description = "库管员")
    private String keeper;
    /**
     * 当前容量
     */
    @Schema(name ="currentWeight", description = "当前容量")
    private BigDecimal currentWeight;
    /**
     * 仓库容量
     */
    @Schema(name ="maxWeight", description = "仓库容量")
    private BigDecimal maxWeight;
    /**
     * 粮食品种
     */
    @Schema(name ="grainVariety", description = "粮食品种")
    private String grainVariety;
    /**
     * 状态（1启用 2禁用）
     */
    @Schema(name ="state", description = "状态（1启用 2禁用）")
    private Integer state;

}
