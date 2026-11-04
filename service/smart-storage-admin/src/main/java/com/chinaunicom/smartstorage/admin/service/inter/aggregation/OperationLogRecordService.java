package com.chinaunicom.smartstorage.admin.service.inter.aggregation;

import java.util.List;

import com.chinaunicom.smartstorage.admin.dto.aggregation.optlog.OperationLogRecordQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.OperationLogRecord;


/**
 * @author liuchink
 * @since 2024/08/19 15:22
 */
public interface OperationLogRecordService {
    List<OperationLogRecord>
        listOperationLogRecordByCondition(OperationLogRecordQueryReqDTO operationLogRecordQueryReqDTO);
}
