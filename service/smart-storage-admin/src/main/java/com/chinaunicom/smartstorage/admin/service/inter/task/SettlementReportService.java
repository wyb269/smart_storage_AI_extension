package com.chinaunicom.smartstorage.admin.service.inter.task;

import com.chinaunicom.smartstorage.admin.dto.task.taskflow.SettlementReportQueryRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.taskflow.SettlementReportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.taskflow.SettlementReportRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.taskflow.RegistrationReportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.taskflow.RegistrationReportRespDTO;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

public interface SettlementReportService {
    List<SettlementReportRespDTO> settlementReport(SettlementReportReqDTO dto);

    ActionResult<SettlementReportQueryRespDTO, String> settlementReportQuery(String id);

    List<RegistrationReportRespDTO> registrationReport(RegistrationReportReqDTO dto);
}
