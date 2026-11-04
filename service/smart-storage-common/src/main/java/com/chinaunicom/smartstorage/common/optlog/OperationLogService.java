package com.chinaunicom.smartstorage.common.optlog;

import org.springframework.stereotype.Component;

/**
 * @author liuchink
 * @since 2024/03/01 13:57
 */
@Component
public interface OperationLogService {
    void saveOperationLog(OptLog optLog, OperationLog operationLog);
}
