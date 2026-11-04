package com.chinaunicom.smartstorage.admin.dto.task.sampleItem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
/**
 * @Description 扦样信息
 * @author zhangy
 * @Date 2026-01-26
 */
@Data
public class SampleItemSaveReqDTO {
    @NotBlank(message = "作业单ID不能为空")
    @Schema(name ="taskId", description = "作业单ID", required = true)
    private String taskId;

//    @NotBlank(message = "粮食品种不能为空")
//    @Schema(name ="grainVariety", description = "粮食品种", required = true)
//    private String grainVariety;

    @NotNull(message = "扦样方式不能为空")
    @Schema(name ="sampleType", description = "扦样方式(1 人工)", required = true)
    private Integer sampleType;

    @Schema(name ="remarks", description = "备注", required = false)
    private String remarks;
}
