package com.chinaunicom.smartstorage.admin.entity.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@TableName("task_flow_out_item")
public class TaskFlowRegistrationOut {
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    @TableField( value = "task_id")
    private String taskId;
    @TableField( value = "stay_time")
    private Integer stayTime;
    @TableField( value = "add_time")
    private LocalDateTime addTime;
    @TableField( value = "update_time")
    private LocalDateTime updateTime;
    @TableField( value = "add_user")
    private String addUser;
    @TableField( value = "update_user")
    private String updateUser;
    @TableField( value = "out_no")
    private String outNo;
//    @TableField( value = "card_no")
//    private String cardNo;
}
