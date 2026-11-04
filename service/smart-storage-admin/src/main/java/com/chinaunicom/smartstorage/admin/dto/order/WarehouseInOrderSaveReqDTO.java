package com.chinaunicom.smartstorage.admin.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WarehouseInOrderSaveReqDTO {
    /**
     * 客户id
     */
    @NotBlank(message = "客户id不能为空")
    @Schema(name ="customerId", description = "客户id", required = true)
    private String customerId;
    /**
     * 粮食品种
     */
    @NotBlank(message = "粮食品种不能为空")
    @Schema(name ="grainVariety", description = "粮食品种", required = true)
    private String grainVariety;
    /**
     * 粮源类型
     */
    @NotNull(message = "粮源类型不能为空")
    @Schema(name ="grainSourceType", description = "粮源类型（数据字典）", required = true)
    private Integer grainSourceType;
    /**
     * 预计入库量
     */
    @NotNull(message = "预计入库量不能为空")
    @Schema(name ="planWeight", description = "预计入库量", required = true)
    private BigDecimal planWeight;


    /**
     * 执行开始日期
     */
    @NotNull(message = "执行开始日期不能为空")
    @Schema(name ="startTime", description = "执行开始日期", required = true)
    private LocalDateTime startTime;
    /**
     * 执行结束日期
     */
    @NotNull(message = "执行结束日期不能为空")
    @Schema(name ="endTime", description = "执行结束日期", required = true)
    private LocalDateTime endTime;
    /**
     * 备注
     */
    @Schema(name ="remarks", description = "备注", required = false)
    private String remarks;
}
