package com.chinaunicom.smartstorage.admin.entity.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_flow_registration_item")
public class TaskFlowRegistration {
    @TableId(type = IdType.ASSIGN_ID)
    private String  id;
    @TableField( value = "registration_no")
    private String registrationNo;
    @TableField( value = "task_id")
    private String taskId;
    @TableField( value = "remark")
    private String remark;
    @TableField( value = "add_time")
    private LocalDateTime addTime;
    @TableField( value = "pic")
    private String pic;
    @TableField( value = "update_time")
    private LocalDateTime updateTime;
    @TableField( value = "add_user")
    private String addUser;
    @TableField( value = "update_user")
    private String updateUser;
}
