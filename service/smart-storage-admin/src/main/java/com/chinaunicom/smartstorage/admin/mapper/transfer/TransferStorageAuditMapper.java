package com.chinaunicom.smartstorage.admin.mapper.transfer;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinaunicom.smartstorage.admin.dto.transfer.TransferStorageAuditQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.transfer.TransferStorageAudit;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 倒仓审核信息(TransferStorageAudit)表数据库访问层
 *
 * @author liuchink
 * @since 2026-03-11 10:02:15
 */
public interface TransferStorageAuditMapper extends BaseMapper<TransferStorageAudit> {
    List<TransferStorageAudit>
        listTransferStorageAuditByCondition(TransferStorageAuditQueryReqDTO transferStorageAuditQueryReqDTO);

    TransferStorageAudit getTransferStorageAuditByDetail(@Param("id") String id);
}
