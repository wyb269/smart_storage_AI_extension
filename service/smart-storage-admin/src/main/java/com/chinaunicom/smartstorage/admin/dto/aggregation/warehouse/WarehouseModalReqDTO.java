package com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WarehouseModalReqDTO {
    @NotBlank(message = "粮食品种不能为空")
    @Schema(name ="grainVariety", description = "粮食品种", required = true)
    private String grainVariety;
}
