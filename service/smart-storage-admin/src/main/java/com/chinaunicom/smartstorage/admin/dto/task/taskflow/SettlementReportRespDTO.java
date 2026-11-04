package com.chinaunicom.smartstorage.admin.dto.task.taskflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SettlementReportRespDTO {

    @Schema(name = "id", description = "工作单id", example = "")
    private String id;
    /**
     * 作业编号
     */
    @Schema(name = "jobNo", description = "工作单编号", example = "")
    private String jobNo;

    /**
     * 客户名称
     */
    @Schema(name = "customerName", description = "客户名称", example = "")
    private String customerName;

    /**
     * 粮食品种
     */
    @Schema(name = "grainVariety", description = "粮食品种", example = "")
    private String grainVariety;

    /**
     * 粮食等级
     */
    @Schema(name = "grainLevel", description = "粮食等级", example = "")
    private String grainLevel;

    /**
     * 毛重(KG)
     */
    @Schema(name = "grossWeight", description = "毛重(KG)", example = "")
    private BigDecimal grossWeight;

    /**
     * 皮重(KG)
     */
    @Schema(name = "tareWeight", description = "皮重(KG)", example = "")
    private BigDecimal tareWeight;

    /**
     * 净重(KG)
     */
    @Schema(name = "netWeight", description = "净重(KG)", example = "")
    private BigDecimal netWeight;

    /**
     * 作业类型（出库作业/入库作业等）
     */
    @Schema(name = "jobType", description = "作业类型（出库作业/入库作业等）", example = "")
    private String jobType;

    /**
     * 结算重量(KG)
     */
    @Schema(name = "settleWeight", description = "结算重量(KG)", example = "")
    private BigDecimal settleWeight;

    /**
     * 结算单价(元)
     */
    @Schema(name = "settleUnitPrice", description = "结算单价(元)", example = "")
    private BigDecimal settleUnitPrice;

    /**
     * 结算金额(元)
     */
    @Schema(name = "settleAmount", description = "结算金额(元)", example = "")
    private BigDecimal settleAmount;

    /**
     * 结算时间
     */
    @Schema(name = "settleTime", description = "结算时间", example = "")
    private LocalDateTime settleTime;
}
