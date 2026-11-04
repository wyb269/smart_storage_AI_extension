package com.chinaunicom.smartstorage.admin.mapper.task.taskflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinaunicom.smartstorage.admin.dto.task.taskflow.*;
import com.chinaunicom.smartstorage.admin.entity.task.TaskFlow;
import com.chinaunicom.smartstorage.admin.dto.task.taskflow.TaskFlowStatisticsOutRespDTO;

import java.util.List;

public interface TaskFlowMapper extends BaseMapper<TaskFlow> {
    List<TaskFlowRespDTO> listTaskFlow(TaskFlowQueryReqDTO dto);

    TaskFlowStatisticsRespDTO statistics();

    List<TaskFlowRespDTO> listTaskFlowOut(TaskFlowQueryReqDTO dto);

    TaskFlowStatisticsOutRespDTO statisticsOut();

    List<SettlementReportRespDTO> settlementReport(SettlementReportReqDTO dto);

    SettlementReportQueryRespDTO settlementReportQuery(String id);

    List<RegistrationReportRespDTO> registrationReport(RegistrationReportReqDTO dto);
}
