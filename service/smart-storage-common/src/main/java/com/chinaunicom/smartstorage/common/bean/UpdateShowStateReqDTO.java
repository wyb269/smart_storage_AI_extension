package com.chinaunicom.smartstorage.common.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author liuchink
 * @since 2024/06/21 08:55
 */
@Data
public class UpdateShowStateReqDTO {

    /**
     * 主键ID
     */
    @Schema(description = "id", nullable = true, example = "12345")
    @NotBlank(message = "ID不能为空")
    private String id;
    /**
     * 启用状态1=启用2=禁用
     */
    @Schema(description = "显示状态1=是；2=否", nullable = true, example = "12345")
    @NotNull(message = "显示状态不能为空")
    @Range(min = 1, max = 2, message = "显示状态超出范围")
    private Integer showState;
}
