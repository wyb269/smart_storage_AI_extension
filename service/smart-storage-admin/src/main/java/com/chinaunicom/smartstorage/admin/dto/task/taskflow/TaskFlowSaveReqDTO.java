package com.chinaunicom.smartstorage.admin.dto.task.taskflow;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskFlowSaveReqDTO {
    private String jobNo;
    private String processStatus;
    private LocalDateTime jobStartTime;
    private LocalDateTime jobEndTime;
    private String jobStatus;
    private String jobType;
}
