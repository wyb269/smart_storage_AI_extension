package com.chinaunicom.smartstorage.admin.dto.transfer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 倒仓信息(TransferStorageInfo)表实体类
 *
 * @author liuchink
 * @since 2026-03-10 16:43:56
 */
@Data
public class TransferStorageInfoUpdateReqDTO {
    /**
     * id
     */
    @Schema(description = "id", nullable = true, example = "12345")
    @NotBlank(message = "id不能为空")
    private String id;
    /**
     * 类型
     */
    @Schema(
            description = "倒仓类型1 - 粮情异常倒仓；2 - 质量分储倒仓；3 - 仓房维修倒仓；4 - 设备升级倒仓；5 - 仓房熏蒸倒仓；6 - 储备粮轮换倒仓；7 - 专项储备倒仓；8 - 应急避险倒仓；9 - 应急供应倒仓；10 - 仓容平衡倒仓；11 - 品种集中倒仓；12 - 实验性倒仓；13 - 跨库调拨倒仓",
            nullable = true, example = "12345")
    @NotNull(message = "类型不能为空")
    @Range(min = 1, max = 13, message = "类型数据超出范围")
    private Integer transferType;
    /**
     * 移出仓库
     */
    @Schema(description = "移出仓库ID", nullable = true, example = "12345")
    @NotBlank(message = "移出仓库不能为空")
    private String sourceWarehouseId;
    /**
     * 移入仓库
     */
    @Schema(description = "移入仓库ID", nullable = true, example = "12345")
    @NotBlank(message = "移入仓库不能为空")
    private String targetWarehouseId;
    /**
     * 粮食品种
     */
    @Schema(description = "粮食品种", nullable = true, example = "12345")
    @NotBlank(message = "粮食品种不能为空")
    private String grainVariety;
    /**
     * 预计倒仓量(KG)
     */
    @Schema(description = "预计倒仓量(KG)", nullable = true, example = "12345")
    @NotNull(message = "预计倒仓量不能为空")
    private BigDecimal estimatedQuantity;
    /**
     * 开始时间
     */
    @Schema(description = "开始时间", nullable = true, example = "12345")
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    @Schema(description = "结束时间", nullable = true, example = "12345")
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;
    /**
     * 倒仓原因
     */
    @Schema(description = "倒仓原因", nullable = true, example = "12345")
    private String transferReason;

}
