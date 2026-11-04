package com.chinaunicom.smartstorage.admin.dto.task.registration.out;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
@Data
public class TaskFlowOutSaveDTO {
    @NotBlank(message = "工作单ID不能为空")
    @Schema(name = "taskId", description = "工作单ID", example = "123123123")
    private String taskId;
    @NotNull(message = "停留时间不能为空")
    @Schema(name = "stayTime", description = "停留时间", example = "70")
    private Integer stayTime;
    @NotBlank(message = "智能卡ID不能为空")
    @Schema(name = "smartCardId", description = "智能卡ID", example = "123123123")
    private String smartCardId;
}
