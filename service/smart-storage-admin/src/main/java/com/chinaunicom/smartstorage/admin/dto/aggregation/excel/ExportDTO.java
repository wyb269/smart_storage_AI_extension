package com.chinaunicom.smartstorage.admin.dto.aggregation.excel;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;
@Data
public class ExportDTO {
    // 业务类型
    @Schema(name = "bizType", description = "业务类型", example = "")
    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    // 业务数据
    @Schema(name = "params", description = "业务数据", example = "")
    private Map<String, Object> params;
}
