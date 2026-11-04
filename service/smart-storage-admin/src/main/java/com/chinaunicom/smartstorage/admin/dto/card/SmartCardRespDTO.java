package com.chinaunicom.smartstorage.admin.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author liugang
 * @since 2026/1/22 14:00
 */
@Data
public class SmartCardRespDTO {
    @Schema(description = "ID", nullable = true, example = "12345")
    private String id;
    @Schema(description = "智能卡编号", nullable = true, example = "12345")
    private String cardNo;
    @Schema(description = "物理卡编号", nullable = true, example = "12345")
    private String physicalCardNumber;
    @Schema(description = "发卡状态1=已发卡；2=未发卡", nullable = true, example = "12345")
    private Integer state;
    @Schema(description = "添加人", nullable = true, example = "12345")
    private String addUser;
    @Schema(description = "添加时间", nullable = true, example = "12345")
    private LocalDateTime addTime;
    @Schema(description = "发卡人", nullable = true, example = "12345")
    private String taskFlowAddUserName;
    @Schema(description = "发卡时间", nullable = true, example = "12345")
    private LocalDateTime taskFlowAddTime;
    @Schema(description = "作业编号", nullable = true, example = "12345")
    private String taskFlowJobNo;
    @Schema(description = "作业类型", nullable = true, example = "12345")
    private Integer taskFlowJobType;
    @Schema(description = "作业状态", nullable = true, example = "12345")
    private String taskFlowJobStatus;
}
