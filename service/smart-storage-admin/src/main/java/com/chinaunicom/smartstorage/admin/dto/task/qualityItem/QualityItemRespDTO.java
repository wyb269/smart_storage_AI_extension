package com.chinaunicom.smartstorage.admin.dto.task.qualityItem;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @Description  质量检验-id查询详情接口 返回实体类
 * @author zhangy
 * @Date 2026-01-29
 */
@Data
public class QualityItemRespDTO {
    /**
     * 主键
     */
    @Schema(name ="id", description = "唯一标识", nullable = true)
    private String id;
    /**
     * 质检单编号
     */
    @Schema(name ="qualityNo", description = "质检单编号", nullable = true)
    private String qualityNo;
    /**
     * 作业单ID
     */
    @Schema(name ="taskId", description = "作业单ID", nullable = true)
    private String taskId;
    /**
     * 粮食年期(数据字典GRAIN_YEAR)
     */
    @Schema(name ="grainYear", description = "粮食年期", nullable = true)
    private String grainYear;
    /**
     * 粮食等级（数据字典GRAIN_GRADE）
     */
    @Schema(name ="grainGrade", description = "粮食等级", nullable = true)
    private Integer grainGrade;
    /**
     * 粮食单价
     */
    @Schema(name ="grainPrice", description = "粮食单价", nullable = true)
    private BigDecimal grainPrice;
    /**
     * 每千克扣量
     */
    @Schema(name ="deduction", description = "每千克扣量", nullable = true)
    private BigDecimal deduction;
    /**
     * 每千克扣价
     */
    @Schema(name ="deductionPrice", description = "每千克扣价", nullable = true)
    private BigDecimal deductionPrice;
    /**
     * 仓库ID(数据字典STORAGE_WAREHOUSE_NUMBER)
     */
    @Schema(name ="warehouseId", description = "粮仓号", nullable = true)
    private String warehouseId;
    /**
     * 质检结果 1合格,2不合格
     */
    @Schema(name ="qualityResult", description = "质检结果 1合格,2不合格", nullable = true)
    private Integer qualityResult;
    /**
     * 备注
     */
    @Schema(name ="remarks", description = "备注", nullable = true)
    private String remarks;

    /**
     * 创建时间
     */
    @Schema(name ="addTime", description = "创建时间", nullable = true)
    private LocalDateTime addTime;
    /**
     * 创建人
     */
    @Schema(name ="addUser", description = "创建人", nullable = true)
    private String addUser;
    /**
     * 修改人
     */
    @Schema(name ="updateUser", description = "修改人", nullable = true)
    private String updateUser;
    /**
     * 修改时间
     */
    @Schema(name ="updateTime", description = "修改时间", nullable = true)
    private LocalDateTime updateTime;
}
