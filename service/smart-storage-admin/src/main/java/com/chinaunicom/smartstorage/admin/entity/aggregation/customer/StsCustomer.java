package com.chinaunicom.smartstorage.admin.entity.aggregation.customer;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客户信息表实体类
 */
@Data
@TableName("sts_customer")
public class StsCustomer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @TableField("customer_type")
    private Integer customerType;

    @TableField("name")
    private String name;

    @TableField("phone")
    private String phone;

    @TableField("card")
    private String card;

    @TableField("address")
    private String address;

    @TableField("bank_name")
    private String bankName;

    @TableField("bank_account")
    private String bankAccount;

    @TableField("remarks")
    private String remarks;

    @TableField("add_user")
    private String addUser;

    @TableField("add_time")
    private LocalDateTime addTime;

    @TableField("update_user")
    private String updateUser;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("bank_code")
    private String bankCode;
}