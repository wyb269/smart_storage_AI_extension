package com.chinaunicom.smartstorage.admin.service.inter.task;

import com.chinaunicom.smartstorage.admin.dto.task.taskflow.*;
import com.chinaunicom.smartstorage.admin.dto.task.taskflow.TaskFlowStatisticsOutRespDTO;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

public interface TaskFlowService {

    List<TaskFlowRespDTO> listTaskFlow(TaskFlowQueryReqDTO dto);

    ActionResult<TaskFlowQueryRespDTO, String> queryTaskFlow(String id);

    ActionResult<TaskFlowStatisticsRespDTO, String> statistics();

    ActionResult<TaskFlowStatisticsOutRespDTO, String> statisticsOut();

    ActionResult<String, String> terminated(String id);

    ActionResult<String, String> retracement(TaskFlowRetracement taskFlowRetracement);

    List<TaskFlowRespDTO> listTaskFlowOut(TaskFlowQueryReqDTO dto);

    ActionResult<TaskFlowQueryRespDTO, String> queryTaskFlowOut(String id);
}
