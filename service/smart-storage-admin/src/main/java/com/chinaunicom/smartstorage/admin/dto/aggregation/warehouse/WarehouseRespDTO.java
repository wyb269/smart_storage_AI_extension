package com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 仓库管理-详情返回对象
 */
@Data
public class WarehouseRespDTO {
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

    @Schema(name ="grainVarietyLabel", description = "粮食品种label")
    private String grainVarietyLabel;

    @Schema(name ="grainYear", description = "粮食年期")
    private String grainYear;

    @Schema(name ="grainYearLabel", description = "粮食年期label")
    private String grainYearLabel;
    /**
     * 粮食等级
     */
    @Schema(name ="grainGrade", description = "粮食等级")
    private Integer grainGrade;

    @Schema(name ="grainGradeLabel", description = "粮食等级label")
    private String grainGradeLabel;
    /**
     * 状态（1启用 2禁用）
     */
    @Schema(name ="state", description = "状态（1启用 2禁用）")
    private Integer state;
    /**
     * 备注
     */
    @Schema(name ="remarks", description = "备注")
    private String remarks;
}
