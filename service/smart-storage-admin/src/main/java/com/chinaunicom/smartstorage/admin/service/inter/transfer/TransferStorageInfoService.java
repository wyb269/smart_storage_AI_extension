package com.chinaunicom.smartstorage.admin.service.inter.transfer;

import com.chinaunicom.smartstorage.admin.dto.transfer.TransferStorageInfoQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.transfer.TransferStorageInfo;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

/**
 * 倒仓信息(TransferStorageInfo)表服务接口
 *
 * @author liuchink
 * @since 2026-03-11 08:38:52
 */
public interface TransferStorageInfoService {
    ActionResult<String, String> saveTransferStorageInfo(TransferStorageInfo transferStorageInfo);

    ActionResult<String, String> updateTransferStorageInfo(TransferStorageInfo transferStorageInfo);

    ActionResult<String, String> deleteTransferStorageInfo(List<TransferStorageInfo> listTransferStorageInfo);

    List<TransferStorageInfo>
        listTransferStorageInfoByCondition(TransferStorageInfoQueryReqDTO transferStorageInfoQueryReqDTO);

    TransferStorageInfo getTransferStorageInfoByDetail(String id);

    ActionResult<String, String> processPending(TransferStorageInfo transferStorageInfo);

    ActionResult<String, String> processCancel(TransferStorageInfo transferStorageInfo);

    ActionResult<String, String> processVoided(TransferStorageInfo transferStorageInfo);

}
