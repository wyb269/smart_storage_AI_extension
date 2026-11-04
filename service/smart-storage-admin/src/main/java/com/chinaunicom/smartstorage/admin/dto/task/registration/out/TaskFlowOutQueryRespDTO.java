package com.chinaunicom.smartstorage.admin.dto.task.registration.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskFlowOutQueryRespDTO {
    @Schema(name = "id", description = "主键ID", example = "")
    private String id;

    @Schema(name = "carrierName", description = "承接人姓名", example = "张三")
    private String carrierName;
    @Schema(name = "licensePlate", description = "车牌号", example = "黑A2VB51")
    private String licensePlate;
    @Schema(name = "outNo", description = "登出编号", example = "DJ20260202001")
    private String outNo;
    @Schema(name = "jobType", description = "作业类型（如：1入库、2出库）", example = "1")
    private Integer jobType;
    @Schema(name = "pic", description = "登记图片", example = "123123123")
    private String pic;
    @Schema(name = "stayTime", description = "停留时间", example = "50")
    private Integer stayTime;
    @Schema(name = "registrationUser", description = "登记人", example = "123123123")
    private String registrationUser;
    @Schema(name = "addTime", description = "登出时间", example = "")
    private LocalDateTime addTime;
}
