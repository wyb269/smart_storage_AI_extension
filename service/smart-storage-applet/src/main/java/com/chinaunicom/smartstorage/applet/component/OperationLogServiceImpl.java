package com.chinaunicom.smartstorage.applet.component;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.applet.mapper.aggregation.OperationLogRecordMapper;
import com.chinaunicom.smartstorage.applet.entity.aggregation.OperationLogRecord;
import com.chinaunicom.smartstorage.common.optlog.OptLog;
import com.chinaunicom.smartstorage.common.optlog.OperationLog;
import com.chinaunicom.smartstorage.common.optlog.OperationLogService;

import cn.hutool.core.convert.Convert;
import jakarta.annotation.Resource;
import ma.glasnost.orika.MapperFacade;

/**
 * @author liuchink
 * @since 2024/03/01 14:00
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {
    // @Resource
    // private AmqpTemplate amqpTemplate;
    @Resource
    private OperationLogRecordMapper operationLogRecordMapper;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private UidGenerator uidGenerator;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveOperationLog(OptLog optLog, OperationLog operationLog) {
        switch (optLog.storageType()) {
            // case RABBITMQ:
            // amqpTemplate.convertAndSend(MessageQueueConstant.OPERATION_LOG_RECORD_QUEUE_NAME,
            // ChinkFastJson2Utils.toJsonString(operationLog));
            // break;
            default:
                OperationLogRecord operationLogRecord = mapperFacade.map(operationLog, OperationLogRecord.class);
                operationLogRecord.setId(Convert.toStr(uidGenerator.getUID()));
                operationLogRecordMapper.insert(operationLogRecord);
        }
    }
}
