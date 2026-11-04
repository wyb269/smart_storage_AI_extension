package com.chinaunicom.smartstorage.admin.dto.task.weight;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 称重信息表(WeightItem)表实体类
 *
 * @author liuchink
 * @since 2026-01-26 10:45:29
 */
@Data
public class WeightItemOutFormRespDTO {
    @Schema(description = "承运人", nullable = true, example = "12345")
    private String carrierName;
    @Schema(description = "车牌号", nullable = true, example = "12345")
    private String licensePlate;
    @Schema(description = "智能卡编号", nullable = true)
    private String smartCardNo;
    @Schema(description = "客户名称", nullable = true, example = "12345")
    private String cusName;
    @Schema(description = "仓库名称", nullable = true, example = "12345")
    private String warehouseName;
    @Schema(description = "粮食品种", nullable = true, example = "12345")
    private String grainVariety;
    @Schema(description = "粮食等级", nullable = true, example = "12345")
    private String grainGrade;
}
