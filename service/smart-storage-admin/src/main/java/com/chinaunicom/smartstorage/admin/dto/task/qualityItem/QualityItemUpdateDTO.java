package com.chinaunicom.smartstorage.admin.dto.task.qualityItem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
/**
 * @Description  质量检验
 * @author zhangy
 * @Date 2026-01-29
 */
@Data
public class QualityItemUpdateDTO {
    /**
     * 主键
     */
    @NotBlank(message = "id不能为空")
    @Schema(name ="id", description = "唯一标识", required = true)
    private String id;

    @Schema(name ="grainYear", description = "粮食年期（数据字典GRAIN_YEAR）", required = false)
    private String grainYear;
    /**
     * 粮食等级（数据字典GRAIN_GRADE）
     */
    @Schema(name ="grainGrade", description = "粮食等级（数据字典GRAIN_GRADE）", required = false)
    private Integer grainGrade;
    /**
     * 粮食单价
     */
    @Schema(name ="grainPrice", description = "粮食单价", required = false)
    private BigDecimal grainPrice;
    /**
     * 每千克扣量
     */
    @Schema(name ="deduction", description = "每千克扣量", required = false)
    private BigDecimal deduction;
    /**
     * 每千克扣价
     */
    @Schema(name ="deductionPrice", description = "每千克扣价", required = false)
    private BigDecimal deductionPrice;
    /**
     * 仓库ID(数据字典STORAGE_WAREHOUSE_NUMBER)
     */
    @Schema(name ="warehouseId", description = "仓库ID", required = false)
    private String warehouseId;
    /**
     * 质检结果 1合格,2不合格
     */
    @Schema(name ="qualityResult", description = "质检结果 1合格,2不合格", required = false)
    private Integer qualityResult;
    /**
     * 备注
     */
    @Schema(name ="remarks", description = "备注", required = false)
    private String remarks;
}
