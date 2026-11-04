package com.chinaunicom.smartstorage.admin.dto.task.registration;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskFlowRegistrationUpdateReqDTO {
    private Long id;
    private String carrierName;
    private String idCard;
    private String ticketType;
    private String licensePlate;
    private String contactPhone;
    private Long customerId;
    private String smartCardNo;
    private String remark;
    private LocalDateTime stayTime;
}
