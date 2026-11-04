package com.chinaunicom.smartstorage.admin.service.inter.transfer;

import com.chinaunicom.smartstorage.admin.dto.transfer.TransferStorageAuditQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.transfer.TransferStorageAuditReqDTO;
import com.chinaunicom.smartstorage.admin.dto.transfer.TransferStorageInfoQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.transfer.TransferStorageAudit;
import com.chinaunicom.smartstorage.admin.entity.transfer.TransferStorageInfo;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

/**
 * 倒仓审核信息(TransferStorageAudit)表服务接口
 *
 * @author liuchink
 * @since 2026-03-11 10:02:15
 */
public interface TransferStorageAuditService {
    List<TransferStorageInfo>
        listTransferStorageInfoByCondition(TransferStorageInfoQueryReqDTO transferStorageInfoQueryReqDTO);

    List<TransferStorageAudit>
        listTransferStorageAuditByCondition(TransferStorageAuditQueryReqDTO transferStorageAuditQueryReqDTO);

    ActionResult<String, String> audit(TransferStorageAuditReqDTO transferStorageAuditReqDTO);
}
