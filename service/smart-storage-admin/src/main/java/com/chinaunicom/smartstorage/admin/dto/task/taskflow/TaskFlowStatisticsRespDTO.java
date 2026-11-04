package com.chinaunicom.smartstorage.admin.dto.task.taskflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class TaskFlowStatisticsRespDTO {
    @Schema(name = "logIn", description = "待仟样", example = "1")
    private Integer logIn;
    @Schema(name = "sampled", description = "待质检", example = "1")
    private Integer sampled;
    @Schema(name = "inspected", description = "待称重", example = "1")
    private Integer inspected;
    @Schema(name = "weighed", description = "待值仓", example = "1")
    private Integer weighed;
    @Schema(name = "warehoused", description = "待称皮", example = "1")
    private Integer warehoused;
    @Schema(name = "tareWeighed", description = "待结算", example = "1")
    private Integer tareWeighed;
    @Schema(name = "pendCard", description = "待出门销卡", example = "1")
    private Integer pendCard;

}
