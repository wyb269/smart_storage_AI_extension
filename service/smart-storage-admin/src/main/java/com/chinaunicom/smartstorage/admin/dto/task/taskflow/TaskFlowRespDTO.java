package com.chinaunicom.smartstorage.admin.dto.task.taskflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskFlowRespDTO {
    @Schema(name = "id", description = "主键id", example = "")
    private String id;

    @Schema(name = "jobNo", description = "工作单编号", example = "")
    private String jobNo;

    @Schema(name = "jobStatus",
            description = "作业单状态（logIn已登入、sampled已仟样、inspected已质检、weighed已称重、warehoused已值仓、" +
                    "tareWeighed已称皮、settled已结算、logout已登出、pendCard待销卡）", example = "logIn")
    private String jobStatus;

    @Schema(name = "customerName", description = "客户名称", example = "张三")
    private String customerName;
    @Schema(name = "carrierName", description = "承接人姓名", example = "张三")
    private String carrierName;
    @Schema(name = "licensePlate", description = "车牌号", example = "黑A2VB51")
    private String licensePlate;
    @Schema(name = "jobStartTime", description = "作业开始时间", example = "")
    private LocalDateTime jobStartTime;
    @Schema(name = "jobEndTime", description = "作业结束时间", example = "")
    private LocalDateTime jobEndTime;

    @Schema(name = "jobType", description = "作业类型（如：1入库、2出库）", example = "1")
    private String jobType;

    @Schema(name = "netWeigh", description = "净重", example = "")
    private Double netWeigh;
    @Schema(name = "tareWeigh", description = "皮重", example = "")
    private Double tareWeigh;
    @Schema(name = "grossWeight", description = "毛重", example = "")
    private Double grossWeight;
    @Schema(name = "warehouse", description = "粮仓号", example = "")
    private String warehouse;
}
