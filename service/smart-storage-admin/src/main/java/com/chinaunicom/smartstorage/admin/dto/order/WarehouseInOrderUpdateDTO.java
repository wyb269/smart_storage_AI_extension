package com.chinaunicom.smartstorage.admin.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WarehouseInOrderUpdateDTO {
    @NotBlank(message = "id不能为空")
    @Schema(name ="id", description = "id", required = true)
    private String id;
}
