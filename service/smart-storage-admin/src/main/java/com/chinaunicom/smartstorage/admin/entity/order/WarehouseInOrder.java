package com.chinaunicom.smartstorage.admin.entity.order;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * 入库通知单(WarehouseInOrder)实体类
 *
 * @author makejava
 * @since 2026-03-12 15:27:31
 */
@Data
@TableName("warehouse_in_order")
public class WarehouseInOrder implements Serializable {
    private static final long serialVersionUID = -83881191629669208L;
    /**
     * 主键
     */
    @TableField("id")
    private String id;
    /**
     * 客户id
     */
    @TableField("customer_id")
    private String customerId;
    /**
     * 粮食品种
     */
    @TableField("grain_variety")
    private String grainVariety;
    /**
     * 粮源类型
     */
    @TableField("grain_source_type")
    private Integer grainSourceType;
    /**
     * 预计入库量
     */
    @TableField("plan_weight")
    private BigDecimal planWeight;
    /**
     * 完成量
     */
    @TableField("completed_weight")
    private BigDecimal completedWeight;
    /**
     * 单据状态 1进行中 2已完成 3已关闭
     */
    @TableField("state")
    private Integer state;
    /**
     * 执行开始日期
     */
    @TableField("start_time")
    private LocalDateTime startTime;
    /**
     * 执行结束日期
     */
    @TableField("end_time")
    private LocalDateTime endTime;
    /**
     * 备注
     */
    @TableField("remarks")
    private String remarks;
    /**
     * 创建人
     */
    @TableField("add_user")
    private String addUser;
    /**
     * 创建时间
     */
    @TableField("add_time")
    private LocalDateTime addTime;
    /**
     * 修改人
     */
    @TableField("update_user")
    private String updateUser;
    /**
     * 修改时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;



}

