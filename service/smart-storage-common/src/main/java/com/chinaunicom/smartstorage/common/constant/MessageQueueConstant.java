package com.chinaunicom.smartstorage.common.constant;

import com.google.common.collect.ImmutableSet;

/**
 * @author liuchink
 * @since 2023/12/26 10:48
 */
public class MessageQueueConstant {
    /**
     * 队列名称
     */
    public static final String OPERATION_LOG_RECORD_QUEUE_NAME = "operation.log.record";
    public static final String SMS_VERIFY_RECORD_QUEUE_NAME = "sms.verify";
    public static final ImmutableSet<String> MESSAGE_QUEUE_NAME_SET =
        ImmutableSet.of(OPERATION_LOG_RECORD_QUEUE_NAME, SMS_VERIFY_RECORD_QUEUE_NAME);

    private MessageQueueConstant() {

    }
}
