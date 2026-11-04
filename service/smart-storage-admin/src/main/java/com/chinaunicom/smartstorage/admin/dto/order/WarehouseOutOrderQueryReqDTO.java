package com.chinaunicom.smartstorage.admin.dto.order;

import lombok.Data;


import io.swagger.v3.oas.annotations.media.Schema;
@Data
public class WarehouseOutOrderQueryReqDTO {
    @Schema(name ="name", description = "客户名称", required = false)
    private String name;

    @Schema(name ="grainVariety", description = "粮食品种", required = false)
    private String grainVariety;

    @Schema(name ="warehouseId", description = "仓库id", required = false)
    private String warehouseId;
}
