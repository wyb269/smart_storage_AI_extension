package com.chinaunicom.smartstorage.admin.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WarehouseInOrderQueryRespDTO {
    /**
     * 主键
     */
    @Schema(name ="id", description = "id")
    private String id;

    @Schema(name ="name", description = "客户名称")
    private String name;

    /**
     * 粮食品种
     */
    @Schema(name ="grainVariety", description = "粮食品种")
    private String grainVariety;
    /**
     * 粮源类型
     */
    @Schema(name ="grainSourceType", description = "粮源类型")
    private String grainSourceType;
    /**
     * 预计入库量
     */
    @Schema(name ="planWeight", description = "预计入库量")
    private BigDecimal planWeight;
    /**
     * 完成量
     */
    @Schema(name ="completedWeight", description = "完成量")
    private BigDecimal completedWeight;
    /**
     * 单据状态 1进行中 2已完成 3已关闭
     */
    @Schema(name ="state", description = "单据状态 1进行中 2已完成 3已关闭")
    private Integer state;
    /**
     * 执行开始日期
     */
    @Schema(name ="startTime", description = "执行开始日期")
    private LocalDateTime startTime;
    /**
     * 执行结束日期
     */
    @Schema(name ="endTime", description = "执行结束日期")
    private LocalDateTime endTime;

    /**
     * 创建人
     */
    @Schema(name ="addUser", description = "申请人")
    private String addUser;
    /**
     * 创建时间
     */
    @Schema(name ="addTime", description = "申请时间")
    private LocalDateTime addTime;
}
