package com.chinaunicom.smartstorage.admin.mapper.aggregation;

import com.github.yulichang.base.MPJBaseMapper;
import com.chinaunicom.smartstorage.admin.dto.aggregation.optlog.OperationLogRecordQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.OperationLogRecord;

import java.util.List;

/**
 * @author liuchink
 * @since 2024/03/01 14:02
 */

public interface OperationLogRecordMapper extends MPJBaseMapper<OperationLogRecord> {
    List<OperationLogRecord>
        listOperationLogRecordByCondition(OperationLogRecordQueryReqDTO operationLogRecordQueryReqDTO);
}
