package com.chinaunicom.smartstorage.admin.entity.transfer;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 倒仓审核信息(TransferStorageAudit)表实体类
 *
 * @author liuchink
 * @since 2026-03-11 10:03:52
 */
@Data
@TableName("sts_transfer_storage_audit")
public class TransferStorageAudit {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    /**
     * 倒仓ID
     */
    @TableField(value = "transfer_id")
    private String transferId;
    /**
     * 审批状态1=待提交；2=审批中；3=已驳回；4=已作废；5=审批完成；6=已撤回
     */
    @TableField(value = "status")
    private Integer status;
    /**
     * 审批意见
     */
    @TableField(value = "approval_opinion")
    private String approvalOpinion;
    /**
     * 审核人
     */
    @TableField(value = "audit_user")
    private String auditUser;
    @TableField(exist = false)
    private String auditUserName;
    /**
     * 审核时间
     */
    @TableField(value = "audit_time")
    private LocalDateTime auditTime;
    /**
     * 添加时间
     */
    @TableField(value = "add_time")
    private LocalDateTime addTime;
    /**
     * 添加人
     */
    @TableField(value = "add_user")
    private String addUser;
    @TableField(exist = false)
    private String addUserName;
}
