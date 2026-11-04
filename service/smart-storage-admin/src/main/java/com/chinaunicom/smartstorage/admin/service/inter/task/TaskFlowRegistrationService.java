package com.chinaunicom.smartstorage.admin.service.inter.task;

import com.chinaunicom.smartstorage.admin.dto.task.registration.TaskFlowRegistrationListReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.registration.TaskFlowRegistrationListRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.registration.out.TaskFlowOutQueryRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.registration.out.TaskFlowOutRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.registration.out.TaskFlowOutSaveDTO;
import com.chinaunicom.smartstorage.admin.entity.task.TaskFlowRegistration;
import com.chinaunicom.smartstorage.admin.entity.task.TaskFlow;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

public interface TaskFlowRegistrationService {
    ActionResult<String, String> saveRegistration(TaskFlow taskFlow, TaskFlowRegistration taskFlowRegistration);

    List<TaskFlowRegistrationListRespDTO> listRegistration(TaskFlowRegistrationListReqDTO taskFlowRegistrationListReqDTO);

    List<TaskFlowRegistrationListRespDTO> listLogout(TaskFlowRegistrationListReqDTO taskFlowRegistrationListReqDTO);

    ActionResult<TaskFlowOutRespDTO, String> queryRegistration(String id);

    ActionResult<String, String> outRegistration(TaskFlowOutSaveDTO taskFlowOutSaveDTO);

    ActionResult<TaskFlowOutQueryRespDTO, String> queryRegistrationOut(String id);
    ActionResult<String, String> saveRegistrationOut(TaskFlow taskFlow, TaskFlowRegistration taskFlowRegistration);
}
