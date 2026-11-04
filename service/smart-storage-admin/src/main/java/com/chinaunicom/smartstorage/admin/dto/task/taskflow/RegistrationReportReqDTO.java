package com.chinaunicom.smartstorage.admin.dto.task.taskflow;


import com.chinaunicom.smartstorage.common.bean.BaseQueryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RegistrationReportReqDTO extends BaseQueryDTO {

    // 作业编号
    @Schema(name = "jobNo", description = "作业编号", example = "")
    private String jobNo;

    // 作业类型（1-入库 2-出库）
    @Schema(name = "jobType", description = "作业类型（1-入库 2-出库）", example = "")
    private Integer jobType;

    // 客户名称
    @Schema(name = "customerName", description = "客户名称", example = "")
    private String customerName;

    // 承运人
    @Schema(name = "carrier", description = "承运人", example = "")
    private String carrier;

    // 所属组织ID
//    @Schema(name = "orgId", description = "所属组织ID", example = "")
//    private Long orgId;

    // 粮仓号ID
    @Schema(name = "granary", description = "粮仓号", example = "")
    private String granary;

    // 流程状态（字典值）
    @Schema(name = "jobStatus", description = "流程状态", example = "")
    private String jobStatus;

    // 粮食品种
    @Schema(name = "grainVariety", description = "粮食品种", example = "")
    private String grainVariety;
}
