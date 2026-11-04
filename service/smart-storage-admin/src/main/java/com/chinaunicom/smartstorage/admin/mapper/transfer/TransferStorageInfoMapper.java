package com.chinaunicom.smartstorage.admin.mapper.transfer;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinaunicom.smartstorage.admin.dto.transfer.TransferStorageInfoQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.transfer.TransferStorageInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 倒仓信息(TransferStorageInfo)表数据库访问层
 *
 * @author liuchink
 * @since 2026-03-11 08:38:52
 */
public interface TransferStorageInfoMapper extends BaseMapper<TransferStorageInfo> {
    List<TransferStorageInfo>
        listTransferStorageInfoByCondition(TransferStorageInfoQueryReqDTO transferStorageInfoQueryReqDTO);

    TransferStorageInfo getTransferStorageInfoByDetail(@Param("id") String id);
}
