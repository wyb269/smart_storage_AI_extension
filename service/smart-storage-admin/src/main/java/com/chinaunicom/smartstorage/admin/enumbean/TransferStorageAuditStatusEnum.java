package com.chinaunicom.smartstorage.admin.enumbean;

import lombok.Getter;

/**
 * @author liuchink
 */

@Getter
public enum TransferStorageAuditStatusEnum {
    PENDING(1, "待提交"), IN_REVIEW(2, "审批中"), REJECTED(3, "已驳回"), VOIDED(4, "已作废"), COMPLETED(5, "审批完成"),
    CANCEL(6, "已撤回");

    private final int code;
    private final String description;

    TransferStorageAuditStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static TransferStorageAuditStatusEnum fromCode(int code) {
        for (TransferStorageAuditStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未匹配的状态编码: " + code);
    }
}
