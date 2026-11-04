package com.chinaunicom.smartstorage.admin.dto.task.weight;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

/**
 * 称重信息表(WeightItem)表实体类
 *
 * @author liuchink
 * @since 2026-01-26 10:45:29
 */
@Data
public class WeightItemSaveReqDTO {
    /**
     * 作业单ID
     */
    @Schema(description = "作业单ID", nullable = true, example = "12345")
    @NotBlank(message = "作业单ID不能为空")
    private String taskId;
    /**
     * 称重类型1=毛重；2=皮重
     */
    @Schema(description = "称重类型1=毛重；2=皮重", nullable = true, example = "12345")
    @NotNull(message = "称重类型不能为空")
    @Range(min = 1, max = 2, message = "称重类型超出范围")
    private Integer weightType;
    /**
     * 重量
     */
    @Schema(description = "重量", nullable = true, example = "12345")
    @NotNull(message = "重量不能为空")
    private BigDecimal weight;
    /**
     * 备注
     */
    @Schema(description = "备注", nullable = true, example = "12345")
    private String remark;
    /**
     * 车前照片
     */
    @Schema(description = "车前照片", nullable = true, example = "12345")
    @NotBlank(message = "车前照片不能为空")
    private String vehicleFrontPic;
    /**
     * 车后照片
     */
    @Schema(description = "车后照片", nullable = true, example = "12345")
    @NotBlank(message = "车后照片不能为空")
    private String vehicleBackPic;

}
