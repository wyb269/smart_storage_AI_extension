package com.chinaunicom.smartstorage.admin.mapper.task.registration;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinaunicom.smartstorage.admin.dto.task.registration.TaskFlowRegistrationListReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.registration.TaskFlowRegistrationListRespDTO;
import com.chinaunicom.smartstorage.admin.entity.task.TaskFlowRegistration;

import java.util.List;

public interface TaskFlowRegistrationMapper extends BaseMapper<TaskFlowRegistration> {
    List<TaskFlowRegistrationListRespDTO> listRegistration(TaskFlowRegistrationListReqDTO reqDTO);
    List<TaskFlowRegistrationListRespDTO> listLogout(TaskFlowRegistrationListReqDTO reqDTO);
}
