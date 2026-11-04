package com.chinaunicom.smartstorage.common.bean;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author liuchink
 * @since 2024/06/21 08:55
 */
@Data
public class BatchIdReqDTO {
    @Schema(description = "ids", nullable = true, example = "12345")
    private List<String> ids;
}
