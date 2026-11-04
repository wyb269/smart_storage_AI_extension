package com.chinaunicom.smartstorage.admin.dto.transfer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 倒仓信息(TransferStorageInfo)表实体类
 *
 * @author liuchink
 * @since 2026-03-10 16:43:56
 */
@Data
public class TransferStorageAuditReqDTO {
    /**
     * id
     */
    @Schema(description = "transferId", nullable = true, example = "12345")
    @NotBlank(message = "倒仓ID不能为空")
    private String transferId;
    /**
     * 审批状态1=待提交；2=审批中；3=已驳回；4=已作废；5=审批完成
     */
    @Schema(description = "审批状态3=已驳回；5=审批完成", nullable = true, example = "12345")
    @NotNull(message = "审批状态不能为空")
    private Integer status;
    /**
     * 审批意见
     */
    @Schema(description = "审批意见", nullable = true, example = "12345")
    private String approvalOpinion;

    @AssertTrue(message = "审批状态超出数据范围")
    public boolean isStatusValid() {
        return status != null && (status == 3 || status == 5);
    }

}
