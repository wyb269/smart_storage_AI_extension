package com.chinaunicom.smartstorage.admin.entity.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_flow")
public class TaskFlow {
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    @TableField( value = "job_no")
    private String jobNo;
    @TableField( value = "job_status")
    private String jobStatus;
    @TableField( value = "job_end_time")
    private LocalDateTime jobEndTime;
    @TableField( value = "job_type")
    private Integer jobType;
    @TableField( value = "smart_card_id")
    private String smartCardId;
    @TableField( value = "customer_id")
    private String customerId;
    @TableField( value = "carrier_name")
    private String carrierName;
    @TableField( value = "id_card")
    private String idCard;
    @TableField( value = "ticket_type")
    private String ticketType;
    @TableField( value = "license_plate")
    private String licensePlate;
    @TableField( value = "contact_phone")
    private String contactPhone;
    @TableField( value = "add_time")
    private LocalDateTime addTime;
    @TableField( value = "add_user")
    private String addUser;
    @TableField( value = "update_time")
    private LocalDateTime updateTime;
    @TableField( value = "update_user")
    private String updateUser;
    @TableField( value = "card_no")
    private String cardNo;
    @TableField(value = "order_id")
    private String orderId;
}