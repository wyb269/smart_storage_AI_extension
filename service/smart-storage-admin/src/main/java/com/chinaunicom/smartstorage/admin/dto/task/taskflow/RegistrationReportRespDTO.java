package com.chinaunicom.smartstorage.admin.dto.task.taskflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RegistrationReportRespDTO {
    private String id;

    // 作业编号
    @Schema(name = "jobNo", description = "作业编号", example = "ZY20260123001")
    private String jobNo;

    // 作业类型（入库/出库）
    @Schema(name = "jobType", description = "作业类型", example = "入库")
    private String jobType;

    // 客户名称
    @Schema(name = "customerName", description = "客户名称", example = "张三")
    private String customerName;

    // 承运人
    @Schema(name = "carrier", description = "承运人", example = "张三")
    private String carrier;

    // 车牌号
    @Schema(name = "carNo", description = "车牌号", example = "黑A123456")
    private String carNo;

    // 粮食品种
    @Schema(name = "grainVariety", description = "粮食品种", example = "玉米")
    private String grainVariety;

    // 仓库名称
    @Schema(name = "warehouseName", description = "仓库名称", example = "1号仓库1号仓")
    private String warehouseName;

    // 流程状态
    @Schema(name = "jobStatus", description = "流程状态", example = "待扦样")
    private String jobStatus;

    // 毛重
    @Schema(name = "grossWeight", description = "毛重", example = "200")
    private BigDecimal grossWeight;

    // 皮重
    @Schema(name = "tareWeight", description = "皮重", example = "200")
    private BigDecimal tareWeight;

    // 净重
    @Schema(name = "netWeight", description = "净重", example = "180")
    private BigDecimal netWeight;

    // 结算重量(千克)
    @Schema(name = "settleWeight", description = "结算重量", example = "200")
    private BigDecimal settleWeight;

    // 结束时间
    @Schema(name = "endTime", description = "结束时间", example = "2026-02-11 12:12:12")
    private LocalDateTime endTime;
    @Schema(name = "deductWeightTotal", description = "总扣重", example = "")
    private BigDecimal deductWeightTotal;
}
