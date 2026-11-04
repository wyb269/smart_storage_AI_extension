package com.chinaunicom.smartstorage.admin.entity.aggregation;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * 仓库表(StsWarehouse)实体类
 *
 * @author makejava
 * @since 2026-03-10 16:49:27
 */
@Data
@TableName("sts_warehouse")
public class StsWarehouse implements Serializable {
    private static final long serialVersionUID = 899936941379484654L;
    /**
     * 主键
     */
    @TableField("id")
    private String id;
    /**
     * 仓库名
     */
    @TableField("warehouse_name")
    private String warehouseName;
    /**
     * 仓库类型（1平房仓 2立筒仓 3浅圆仓 4罩棚仓）
     */
    @TableField("warehouse_type")
    private Integer warehouseType;
    /**
     * 库管员
     */
    @TableField("keeper")
    private String keeper;
    /**
     * 当前容量
     */
    @TableField("current_weight")
    private BigDecimal currentWeight;
    /**
     * 仓库容量
     */
    @TableField("max_weight")
    private BigDecimal maxWeight;
    /**
     * 粮食品种
     */
    @TableField("grain_variety")
    private String grainVariety;
    /**
     * 粮食年期
     */
    @TableField("grain_year")
    private String grainYear;
    /**
     * 粮食等级
     */
    @TableField("grain_grade")
    private Integer grainGrade;
    /**
     * 状态（1启用 2禁用）
     */
    @TableField("state")
    private Integer state;
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