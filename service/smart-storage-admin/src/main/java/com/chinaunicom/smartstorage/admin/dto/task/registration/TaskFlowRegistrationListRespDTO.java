package com.chinaunicom.smartstorage.admin.dto.task.registration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskFlowRegistrationListRespDTO {

    @Schema(name = "id", description = "id", example = "")
    private String id;

    @Schema(name = "registrationNo", description = "登记编号", example = "DJ20250610001")
    private String registrationNo;

    @Schema(name = "carrierName", description = "承接人姓名", example = "张三")
    private String carrierName;

    @Schema(name = "licensePlate", description = "车牌号", example = "黑A78886")
    private String licensePlate;
    @Schema(name = "customerName", description = "客户名称", example = "123123123")
    private String customerName;
    @Schema(name = "jobType", description = "作业类型（如：1入库、2出库）", example = "1")
    private Integer jobType;

    @Schema(name = "smartCardID", description = "智能卡编号", example = "")
    private String smartCardID;

    @Schema(name = "registrationTime", description = "登记时间", example = "")
    private LocalDateTime registrationTime;
}
