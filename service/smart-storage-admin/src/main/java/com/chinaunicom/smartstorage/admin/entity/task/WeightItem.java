package com.chinaunicom.smartstorage.admin.entity.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 称重信息表(WeightItem)表实体类
 *
 * @author liuchink
 * @since 2026-01-26 10:45:29
 */
@Data
@TableName("task_flow_weight_item")
public class WeightItem {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    /**
     * 作业单ID
     */
    @TableField(value = "task_id")
    private String taskId;
    @TableField(value = "weight_no")
    private String weightNo;
    /**
     * 称重类型1=毛重；2=皮重
     */
    @TableField(value = "weight_type")
    private Integer weightType;
    /**
     * 重量
     */
    @TableField(value = "weight")
    private BigDecimal weight;
    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;
    /**
     * 车前照片
     */
    @TableField(value = "vehicle_front_pic")
    private String vehicleFrontPic;
    /**
     * 车后照片
     */
    @TableField(value = "vehicle_back_pic")
    private String vehicleBackPic;
    /**
     * 添加人
     */
    @TableField(value = "add_user")
    private String addUser;
    @TableField(exist = false)
    private String addUserName;
    /**
     * 添加时间
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
    /**
     * 智能卡编号
     */
    @TableField(exist = false)
    private String cardNo;
    @TableField(exist = false)
    private String jobNo;
    @TableField(exist = false)
    private String jobType;
    @TableField(exist = false)
    private String jobStatus;
}
