package com.chinaunicom.smartstorage.admin.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author liugang
 * @since 2026/1/22 13:58
 */
@Data
public class SmartCardUpdateReqDTO {
    @Schema(description = "ID", nullable = true, example = "12345")
    @NotBlank(message = "ID不能为空")
    private String id;
    @Schema(description = "物理卡编号", nullable = true, example = "12345")
    @NotBlank(message = "物理卡编号不能为空")
    private String physicalCardNumber;
}
