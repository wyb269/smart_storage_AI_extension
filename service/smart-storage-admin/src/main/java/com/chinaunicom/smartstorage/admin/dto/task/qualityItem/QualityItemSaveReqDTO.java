package com.chinaunicom.smartstorage.admin.dto.task.qualityItem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
/**
 * @Description  质量检验
 * @author zhangy
 * @Date 2026-01-29
 */
@Data
public class QualityItemSaveReqDTO {
    @NotBlank(message = "作业单ID不能为空")
    @Schema(name ="taskId", description = "作业单ID", required = true)
    private String taskId;

    @NotBlank(message = "粮食年期不能为空")
    @Schema(name ="grainYear", description = "粮食年期（数据字典GRAIN_YEAR）", required = true)
    private String grainYear;
    /**
     * 粮食等级（数据字典GRAIN_GRADE）
     */
    @NotNull(message = "粮食等级不能为空")
    @Schema(name ="grainGrade", description = "粮食等级（数据字典GRAIN_GRADE）", required = true)
    private Integer grainGrade;
    /**
     * 粮食单价
     */
    @NotNull(message = "粮食单价不能为空")
    @Schema(name ="grainPrice", description = "粮食单价（元/千克）", required = true)
    private BigDecimal grainPrice;
    /**
     * 每千克扣量
     */
    @Schema(name ="deduction", description = "每千克扣量（千克）", required = false)
    private BigDecimal deduction;
    /**
     * 每千克扣价
     */
    @Schema(name ="deductionPrice", description = "每千克扣价（元）", required = false)
    private BigDecimal deductionPrice;
    /**
     * 仓库ID
     */
    @Schema(name ="warehouseId", description = "仓库id", required = false)
    private String warehouseId;
    /**
     * 质检结果 1合格,2不合格
     */
    @NotNull(message = "质检结果不能为空")
    @Schema(name ="qualityResult", description = "质检结果 1合格,2不合格", required = true)
    private Integer qualityResult;
    /**
     * 备注
     */
    @Schema(name ="remarks", description = "备注", required = false)
    private String remarks;
}
