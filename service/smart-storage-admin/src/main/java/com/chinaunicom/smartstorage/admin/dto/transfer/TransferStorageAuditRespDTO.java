package com.chinaunicom.smartstorage.admin.dto.transfer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 倒仓审核信息(TransferStorageAudit)表实体类
 *
 * @author liuchink
 * @since 2026-03-11 10:03:52
 */
@Data
public class TransferStorageAuditRespDTO {
    /**
     * id
     */
    @Schema(description = "id", nullable = true, example = "12345")
    private String id;
    @Schema(description = "移出仓库ID", nullable = true, example = "12345")
    private String transferId;
    /**
     * 审批状态1=待提交；2=审批中；3=已驳回；4=已作废；5=审批完成；6=已撤回
     */
    @Schema(description = "审批状态1=待提交；2=审批中；3=已驳回；4=已作废；5=审批完成；6=已撤回", nullable = true, example = "12345")
    private Integer status;

    /**
     * 审批意见
     */
    @Schema(description = "审批意见", nullable = true, example = "12345")
    private String approvalOpinion;
    /**
     * 审核人
     */
    @Schema(description = "审核人", nullable = true, example = "12345")
    private String auditUser;
    @Schema(description = "审核人", nullable = true, example = "12345")
    private String auditUserName;
    /**
     * 审核时间
     */
    @Schema(description = "审核时间", nullable = true, example = "12345")
    private LocalDateTime auditTime;
    /**
     * 添加人
     */
    @Schema(description = "添加人", nullable = true, example = "12345")
    private String addUser;
    @Schema(description = "添加人", nullable = true, example = "12345")
    private String addUserName;
    /**
     * 添加时间
     */
    @Schema(description = "添加时间", nullable = true, example = "12345")
    private LocalDateTime addTime;
}
