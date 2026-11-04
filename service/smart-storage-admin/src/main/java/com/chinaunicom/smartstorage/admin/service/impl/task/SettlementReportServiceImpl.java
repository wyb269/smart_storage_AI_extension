package com.chinaunicom.smartstorage.admin.service.impl.task;

import com.chinaunicom.smartstorage.admin.dto.task.taskflow.SettlementReportQueryRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.taskflow.SettlementReportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.taskflow.SettlementReportRespDTO;
import com.chinaunicom.smartstorage.admin.mapper.task.taskflow.TaskFlowMapper;
import com.chinaunicom.smartstorage.admin.service.inter.task.SettlementReportService;
import com.chinaunicom.smartstorage.admin.dto.task.taskflow.RegistrationReportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.taskflow.RegistrationReportRespDTO;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@Slf4j
public class SettlementReportServiceImpl implements SettlementReportService {
    @Resource
    private TaskFlowMapper taskFlowMapper;
    @Resource
    private ActionResultService actionResultService;

    @Override
    public List<SettlementReportRespDTO> settlementReport(SettlementReportReqDTO dto) {
        return taskFlowMapper.settlementReport(dto);
    }

    @Override
    public ActionResult<SettlementReportQueryRespDTO, String> settlementReportQuery(String id) {
        SettlementReportQueryRespDTO settlementReportQueryRespDTO = taskFlowMapper.settlementReportQuery(id);
        return actionResultService.callBackResult(settlementReportQueryRespDTO);
    }

    @Override
    public List<RegistrationReportRespDTO> registrationReport(RegistrationReportReqDTO dto) {
        return taskFlowMapper.registrationReport(dto);
    }
}
