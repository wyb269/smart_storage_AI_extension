package com.chinaunicom.smartstorage.admin.dto.transfer;

import com.chinaunicom.smartstorage.common.bean.PageQueryDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 倒仓信息(TransferStorageInfo)表实体类
 *
 * @author liuchink
 * @since 2026-03-10 16:43:56
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class TransferStorageAuditQueryReqDTO extends PageQueryDTO {
    @Schema(description = "倒仓ID", nullable = true, example = "12345")
    private String transferId;
}
