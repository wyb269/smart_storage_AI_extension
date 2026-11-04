package com.chinaunicom.smartstorage.admin.service.impl.aggregation;

import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.dto.aggregation.optlog.OperationLogRecordQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.OperationLogRecord;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.OperationLogRecordMapper;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.OperationLogRecordService;
import jakarta.annotation.Resource;
import ma.glasnost.orika.MapperFacade;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author liuchink
 * @since 2024/03/01 14:00
 */
@Service
public class OperationLogRecordServiceImpl implements OperationLogRecordService {
    @Resource
    private OperationLogRecordMapper operationLogRecordMapper;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private UidGenerator uidGenerator;

    @Override
    public List<OperationLogRecord>
        listOperationLogRecordByCondition(OperationLogRecordQueryReqDTO operationLogRecordQueryReqDTO) {
        return operationLogRecordMapper.listOperationLogRecordByCondition(operationLogRecordQueryReqDTO);
    }
}
