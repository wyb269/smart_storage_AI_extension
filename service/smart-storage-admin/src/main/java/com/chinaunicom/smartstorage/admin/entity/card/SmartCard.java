package com.chinaunicom.smartstorage.admin.entity.card;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * (SmartCard)表实体类
 *
 * @author liuchink
 * @since 2026-01-22 13:35:41
 */
@Data
@TableName("sts_smart_card")
public class SmartCard {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    @TableField(value = "card_no")
    private String cardNo;
    @TableField(value = "physical_card_number")
    private String physicalCardNumber;
    @TableField(value = "state")
    private Integer state;
    @TableField(value = "add_user")
    private String addUser;
    @TableField(value = "add_time")
    private LocalDateTime addTime;
    @TableField(exist = false)
    private String taskFlowAddUserName;
    @TableField(exist = false)
    private LocalDateTime taskFlowAddTime;
    @TableField(exist = false)
    private String taskFlowJobNo;
    @TableField(exist = false)
    private Integer taskFlowJobType;
    @TableField(exist = false)
    private String taskFlowJobStatus;
}
