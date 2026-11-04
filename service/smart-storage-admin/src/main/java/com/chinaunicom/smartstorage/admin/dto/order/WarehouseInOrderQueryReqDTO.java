package com.chinaunicom.smartstorage.admin.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class WarehouseInOrderQueryReqDTO {
    @Schema(name ="name", description = "客户名称", required = false)
    private String name;

    @Schema(name ="grainVariety", description = "粮食品种", required = false)
    private String grainVariety;
}
