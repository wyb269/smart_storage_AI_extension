package com.chinaunicom.smartstorage.admin.entity.task;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * 扦样表(TaskFlowSampleItem)实体类
 *
 * @author zhangy
 * @since 2026-01-26 09:48:25
 */
@Data
@TableName(value ="task_flow_sample_item")
public class SampleItem implements Serializable {
    private static final long serialVersionUID = 898099722883700080L;
    /**
     * 主键
     */
    @TableField(value = "id")
    private String id;
    /**
     * 扦样单编号
     */
    @TableField(value = "sample_no")
    private String sampleNo;
    /**
     * 作业单ID
     */
    @TableField(value = "task_id")
    private String taskId;
    /**
     * 粮食品种(数据字典GRAIN_VARIETY)
     */
    @TableField(value = "grain_variety")
    private String grainVariety;
    /**
     * 扦样方式(1 人工)
     */
    @TableField(value = "sample_type")
    private Integer sampleType;
    /**
     * 备注
     */
    @TableField(value = "remarks")
    private String remarks;
    /**
     * 创建人（扦样员）
     */
    @TableField(value = "add_user")
    private String addUser;
    /**
     * 创建时间（扦样时间）
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

