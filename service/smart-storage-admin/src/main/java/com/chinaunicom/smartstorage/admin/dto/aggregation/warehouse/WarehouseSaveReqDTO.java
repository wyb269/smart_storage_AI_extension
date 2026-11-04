package com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WarehouseSaveReqDTO {
    /**
     * 仓库名
     */
    @NotBlank(message = "仓库名不能为空")
    @Schema(name ="warehouseName", description = "仓库名", required = true)
    private String warehouseName;
    /**
     * 仓库类型（1平房仓 2立筒仓 3浅圆仓 4罩棚仓）
     */
    @NotNull(message = "仓库类型不能为空")
    @Schema(name ="warehouseType", description = "仓库类型（1平房仓 2立筒仓 3浅圆仓 4罩棚仓）", required = true)
    private Integer warehouseType;
    /**
     * 库管员
     */
    @NotBlank(message = "库管员不能为空")
    @Schema(name ="keeper", description = "库管员", required = true)
    private String keeper;
    /**
     * 当前容量
     */
    @NotNull(message = "当前容量不能为空")
    @Schema(name ="currentWeight", description = "当前容量", required = true)
    private BigDecimal currentWeight;
    /**
     * 仓库容量
     */
    @NotNull(message = "仓库容量不能为空")
    @Schema(name ="maxWeight", description = "仓库容量", required = true)
    private BigDecimal maxWeight;
    /**
     * 粮食品种
     */
    @NotBlank(message = "粮食品种不能为空")
    @Schema(name ="grainVariety", description = "粮食品种", required = true)
    private String grainVariety;

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
