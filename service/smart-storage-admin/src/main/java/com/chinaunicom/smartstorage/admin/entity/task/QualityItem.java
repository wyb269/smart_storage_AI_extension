package com.chinaunicom.smartstorage.admin.entity.task;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.io.Serializable;

/**
 * 质检表(TaskFlowQualityItem)实体类
 *
 * @author zhangy
 * @since 2026-01-27 11:13:56
 */
@Data
@TableName(value ="task_flow_quality_item")
public class QualityItem  {
    /**
     * 主键
     */
    @TableField(value = "id")
    private String id;
    /**
     * 质检单编号
     */
    @TableField(value = "quality_no")
    private String qualityNo;
    /**
     * 作业单ID
     */
    @TableField(value = "task_id")
    private String taskId;
    /**
     * 粮食年期(数据字典GRAIN_YEAR)
     */
    @TableField(value = "grain_year")
    private String grainYear;
    /**
     * 粮食等级（数据字典GRAIN_GRADE）
     */
    @TableField(value = "grain_grade")
    private Integer grainGrade;
    /**
     * 粮食单价
     */
    @TableField(value = "grain_price")
    private BigDecimal grainPrice;
    /**
     * 每千克扣量
     */
    @TableField(value = "deduction")
    private BigDecimal deduction;
    /**
     * 每千克扣价
     */
    @TableField(value = "deduction_price")
    private BigDecimal deductionPrice;
    /**
     * 仓库ID(数据字典STORAGE_WAREHOUSE_NUMBER)
     */
    @TableField(value = "warehouse_id")
    private String warehouseId;
    /**
     * 质检结果 1合格,2不合格
     */
    @TableField(value = "quality_result")
    private Integer qualityResult;
    /**
     * 备注
     */
    @TableField(value = "remarks")
    private String remarks;
    /**
     * 创建人（质检员）
     */
    @TableField(value = "add_user")
    private String addUser;
    /**
     * 创建时间
     */
    @TableField(value = "add_time")
    private LocalDateTime addTime;
    /**
     * 修改人
     */
    @TableField(value = "update_user")
    private String updateUser;
    /**
     * 修改时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;




}

