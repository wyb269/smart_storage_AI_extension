package com.chinaunicom.smartstorage.admin.dto.task.taskflow;

import com.chinaunicom.smartstorage.common.bean.BaseQueryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class SettlementReportReqDTO extends BaseQueryDTO {
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
     * 结算时间 - 开始时间
     */
    @Schema(name = "settleTimeStart", description = "开始时间", example = "")
    private LocalDateTime settleTimeStart;

    /**
     * 结算时间 - 结束时间
     */
    @Schema(name = "settleTimeEnd", description = "结束时间", example = "")
    private LocalDateTime settleTimeEnd;

    /**
     * 作业类型
     */
    @Schema(name = "jobType", description = "作业类型", example = "")
    private Integer jobType;

}
