package com.chinaunicom.smartstorage.admin.dto.task.sampleItem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/**
 * @Description 扦样信息
 * @author zhangy
 * @Date 2026-01-26
 */
@Data
public class SampleItemUpdateDTO {
    /**
     * 主键
     */
    @NotBlank(message = "ID不能为空")
    @Schema(name ="id", description = "唯一标识", required = true)
    private String id;
    /**
     * 粮食品种(数据字典GRAIN_VARIETY)
     */
    @Schema(name ="grainVariety", description = "粮食品种", required = false)
    private String grainVariety;
    /**
     * 扦样方式(1 人工)
     */
    @Schema(name ="sampleType", description = "扦样方式(1 人工)", required = false)
    private Integer sampleType;
    /**
     * 备注
     */
    @Schema(name ="remarks", description = "备注", required = false)
    private String remarks;
}
