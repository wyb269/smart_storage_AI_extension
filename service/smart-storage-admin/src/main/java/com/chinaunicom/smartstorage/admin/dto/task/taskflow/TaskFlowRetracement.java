package com.chinaunicom.smartstorage.admin.dto.task.taskflow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskFlowRetracement {
    @NotNull(message = "id不能为空")
    @Schema(name = "id", description = "id", example = "7556684453")
    private String id;

    @NotNull(message = "作业单状态不能为空")
    @Schema(name = "jobStatus",
            description = "作业单状态（logIn已登入、sampled已仟样、inspected已质检、weighed已称重、warehoused已值仓、" +
                    "tareWeighed已称皮、settled已结算、logout已登出。）", example = "logIn")
    private String jobStatus;

    @NotNull(message = "作业类型（如：1入库、2出库）不能为空")
    @Schema(name = "jobType", description = "作业类型（如：1入库、2出库）", example = "1")
    private Integer jobType;
}
