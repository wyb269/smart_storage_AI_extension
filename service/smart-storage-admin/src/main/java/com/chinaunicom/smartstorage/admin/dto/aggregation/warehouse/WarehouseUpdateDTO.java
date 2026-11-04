package com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WarehouseUpdateDTO {
    @NotBlank(message = "id不能为空")
    @Schema(name ="id", description = "id", required = true)
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
     * 库管员
     */
    @Schema(name ="keeper", description = "库管员", required = false)
    private String keeper;

    /**
     * 仓库容量
     */
    @Schema(name ="maxWeight", description = "仓库容量", required = false)
    private BigDecimal maxWeight;


    /**
     * 状态（1启用 2禁用）
     */
    @Schema(name ="state", description = "状态（1启用 2禁用）", required = false)
    private Integer state;
    /**
     * 备注
     */
    @Schema(name ="remarks", description = "备注", required = false)
    private String remarks;
}
