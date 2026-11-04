package com.chinaunicom.smartstorage.admin.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author liugang
 * @since 2026/1/22 13:56
 */
@Data
public class SmartCardSaveReqDTO {
    @Schema(description = "物理卡编号", nullable = true, example = "12345")
    @NotBlank(message = "物理卡编号不能为空")
    private String physicalCardNumber;
}
