package com.chinaunicom.smartstorage.admin.service.impl.task;

import cn.hutool.core.convert.Convert;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.chinaunicom.smartstorage.admin.component.CodeGeneratorComponent;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.task.registration.TaskFlowRegistrationListReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.registration.TaskFlowRegistrationListRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.registration.out.TaskFlowOutQueryRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.registration.out.TaskFlowOutRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.registration.out.TaskFlowOutSaveDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.customer.StsCustomer;
import com.chinaunicom.smartstorage.admin.entity.card.SmartCard;
import com.chinaunicom.smartstorage.admin.entity.order.WarehouseInOrder;
import com.chinaunicom.smartstorage.admin.entity.order.WarehouseOutOrder;
import com.chinaunicom.smartstorage.admin.entity.task.TaskFlowRegistration;
import com.chinaunicom.smartstorage.admin.entity.task.TaskFlowRegistrationOut;
import com.chinaunicom.smartstorage.admin.entity.task.TaskFlow;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;
import com.chinaunicom.smartstorage.admin.enumbean.TaskStatusEnum;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.customer.StsCustomerMapper;
import com.chinaunicom.smartstorage.admin.mapper.card.SmartCardMapper;
import com.chinaunicom.smartstorage.admin.mapper.order.WarehouseInOrderMapper;
import com.chinaunicom.smartstorage.admin.mapper.order.WarehouseOutOrderMapper;
import com.chinaunicom.smartstorage.admin.mapper.task.registration.TaskFlowRegistrationMapper;
import com.chinaunicom.smartstorage.admin.mapper.task.taskflow.TaskFlowMapper;
import com.chinaunicom.smartstorage.admin.mapper.task.registration.TaskFlowOutMapper;
import com.chinaunicom.smartstorage.admin.mapper.rbac.UserMapper;
import com.chinaunicom.smartstorage.admin.service.inter.task.TaskFlowRegistrationService;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class TaskFlowRegistrationServiceImpl implements TaskFlowRegistrationService {
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private CodeGeneratorComponent codeGeneratorComponent;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private TaskFlowMapper taskFlowMapper;
    @Resource
    private TaskFlowRegistrationMapper taskFlowRegistrationMapper;
    @Resource
    private TaskFlowOutMapper taskFlowOutMapper;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private SmartCardMapper smartCardMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private StsCustomerMapper stsCustomerMapper;
    @Resource
    private WarehouseInOrderMapper warehouseInOrderMapper;
    @Resource
    private WarehouseOutOrderMapper warehouseOutOrderMapper;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActionResult<String, String> saveRegistration(TaskFlow taskFlow, TaskFlowRegistration taskFlowRegistration) {
        //验证智能卡状态
        if(smartCardMapper.selectOne(new LambdaQueryWrapper<SmartCard>()
                .eq(SmartCard::getId, taskFlow.getSmartCardId())).getState().equals(1)){
           return actionResultService.callBackResultFail("请勿重复发卡");
        }
        //验证通知单
        WarehouseInOrder warehouseInOrder = warehouseInOrderMapper.selectOne(new LambdaQueryWrapper<WarehouseInOrder>()
                .eq(WarehouseInOrder::getCustomerId, taskFlow.getCustomerId()).eq(WarehouseInOrder::getState,1));
        if(warehouseInOrder == null){
            return actionResultService.callBackResultFail("无正在进行的通知单");
        }

        //生成登记id
        String id = Convert.toStr(uidGenerator.getUID());
        //生成工作单id
        String taskid = Convert.toStr(uidGenerator.getUID());
        //添加工作单数据
        taskFlow.setId(taskid);
        taskFlow.setJobType(1);
        taskFlow.setOrderId(warehouseInOrder.getId());
        taskFlow.setJobStatus(TaskStatusEnum.LOGGED_IN.getCode());
        taskFlow.setJobNo(codeGeneratorComponent.getWorkFlow());
        taskFlow.setAddUser(jwtTokenComponent.getUser().getId());
        taskFlow.setAddTime(LocalDateTime.now());
        //添加登记数据
        taskFlowRegistration.setId(id);
        taskFlowRegistration.setTaskId(taskid);
        taskFlowRegistration.setRegistrationNo(codeGeneratorComponent.getWorkFlowLogin());
        taskFlowRegistration.setAddUser(jwtTokenComponent.getUser().getId());
        taskFlowRegistration.setAddTime(LocalDateTime.now());
        //绑定智能卡信息
        smartCardMapper.update(null, new LambdaUpdateWrapper<SmartCard>()
                .eq(SmartCard::getId, taskFlow.getSmartCardId())
                .set(SmartCard::getState,1));
        //保存数据
        taskFlowMapper.insert(taskFlow);
        taskFlowRegistrationMapper.insert(taskFlowRegistration);
        //返回结果
        return actionResultService.callBackResult("保存成功");
    }

    @Override
    public List<TaskFlowRegistrationListRespDTO> listRegistration(TaskFlowRegistrationListReqDTO taskFlowRegistrationListReqDTO) {
        return taskFlowRegistrationMapper.listRegistration(taskFlowRegistrationListReqDTO);
    }
    @Override
    public List<TaskFlowRegistrationListRespDTO> listLogout(TaskFlowRegistrationListReqDTO taskFlowRegistrationListReqDTO) {
        return taskFlowRegistrationMapper.listLogout(taskFlowRegistrationListReqDTO);
    }

    @Override
    public ActionResult<TaskFlowOutRespDTO, String> queryRegistration(String id) {
        //查询工作单信息
        TaskFlow taskFlow = taskFlowMapper.selectById(id);
        //查询登记信息
        TaskFlowRegistration taskFlowRegistration = taskFlowRegistrationMapper.selectOne(
                new LambdaQueryWrapper<TaskFlowRegistration>()
                .eq(TaskFlowRegistration::getTaskId, taskFlow.getId()));
        //数据整合
        TaskFlowOutRespDTO map = mapperFacade.map(taskFlow, TaskFlowOutRespDTO.class);
        map.setPic(taskFlowRegistration.getPic());
        map.setRegistrationNo(taskFlowRegistration.getRegistrationNo());
        StsCustomer stsCustomer = stsCustomerMapper.selectOne(new LambdaQueryWrapper<StsCustomer>()
                .eq(StsCustomer::getId, taskFlow.getCustomerId()));
        if (stsCustomer != null) {
            map.setCustomerName(stsCustomer.getName());
        }

        map.setRegistrationUser(userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, taskFlowRegistration.getAddUser())).getName());
        map.setRegistrationTime(taskFlowRegistration.getAddTime());
        map.setRemark(taskFlowRegistration.getRemark());
        //计算停留时间
//        if (taskFlow.getJobType().equals(1)) {
            if (taskFlowOutMapper.selectOne(new LambdaQueryWrapper<TaskFlowRegistrationOut>()
                    .eq(TaskFlowRegistrationOut::getTaskId, taskFlow.getId()))==null) {

                LocalDateTime addTime = taskFlow.getAddTime();
                LocalDateTime currentTime = LocalDateTime.now();
                long totalMinutes = Duration.between(addTime, currentTime).toMinutes();
                map.setStayTime(totalMinutes < 0 ? 0 : (int) totalMinutes);
            }else {
                map.setStayTime(taskFlowOutMapper.selectOne(new LambdaQueryWrapper<TaskFlowRegistrationOut>()
                        .eq(TaskFlowRegistrationOut::getTaskId, taskFlow.getId())).getStayTime());
            }
//        }
        return actionResultService.callBackResult(map);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActionResult<String, String> outRegistration(TaskFlowOutSaveDTO taskFlowOutSaveDTO) {
        //验证智能卡
        SmartCard smartCard = smartCardMapper.selectOne(new LambdaQueryWrapper<SmartCard>()
                .eq(SmartCard::getId, taskFlowOutSaveDTO.getSmartCardId()));
        if(smartCard.getState().equals(2)){
            return actionResultService.callBackResultFail("智能卡未绑定");
        }
        //验证流程状态
        TaskFlow taskFlow = taskFlowMapper.selectOne(new LambdaQueryWrapper<TaskFlow>()
                .eq(TaskFlow::getId, taskFlowOutSaveDTO.getTaskId()));
        if (!taskFlow.getJobStatus().equals(TaskStatusEnum.SETTLED.getCode())
                && !taskFlow.getJobStatus().equals(TaskStatusEnum.PEND_CARD.getCode())){
            return actionResultService.callBackResultFail("该工作单未到此流程");
        }
        //验证是否重复登出
        if(!taskFlowOutMapper.selectList(new LambdaQueryWrapper<TaskFlowRegistrationOut>()
                .eq(TaskFlowRegistrationOut::getTaskId, taskFlowOutSaveDTO.getTaskId())).isEmpty()) {
            return actionResultService.callBackResultFail("同一工作单不能重复登出");
        }
        //记录登出信息
        TaskFlowRegistrationOut taskFlowRegistrationOut = new TaskFlowRegistrationOut();
        taskFlowRegistrationOut.setId(Convert.toStr(uidGenerator.getUID()));
        taskFlowRegistrationOut.setTaskId(taskFlowOutSaveDTO.getTaskId());
        taskFlowRegistrationOut.setStayTime(taskFlowOutSaveDTO.getStayTime());
        taskFlowRegistrationOut.setAddTime(LocalDateTime.now());
        taskFlowRegistrationOut.setAddUser(jwtTokenComponent.getUser().getId());
        taskFlowRegistrationOut.setOutNo(codeGeneratorComponent.getout());
//        taskFlowRegistrationOut.setCardNo(smartCard.getCardNo());
        taskFlowOutMapper.insert(taskFlowRegistrationOut);
        //更新工作单状态
        taskFlowMapper.update(null, new LambdaUpdateWrapper<TaskFlow>()
                .eq(TaskFlow::getId, taskFlowOutSaveDTO.getTaskId())
                .set(TaskFlow::getJobStatus, TaskStatusEnum.LOGOUT.getCode())
                .set(TaskFlow::getJobEndTime,taskFlowRegistrationOut.getAddTime())
                .set(TaskFlow::getUpdateTime,LocalDateTime.now())
                .set(TaskFlow::getSmartCardId,"")
                .set(TaskFlow::getUpdateUser,jwtTokenComponent.getUser().getId()));
        //智能卡解绑
        smartCardMapper.update(null, new LambdaUpdateWrapper<SmartCard>()
                .eq(SmartCard::getId, taskFlowOutSaveDTO.getSmartCardId())
                .set(SmartCard::getState,2));

        return actionResultService.callBackResult("登出成功");
    }

    @Override
    public ActionResult<TaskFlowOutQueryRespDTO, String> queryRegistrationOut(String id) {
        //查询工作单信息
        TaskFlow taskFlow = taskFlowMapper.selectById(id);
        //判断是否登出
        if (taskFlow.getJobStatus().equals(TaskStatusEnum.LOGOUT.getCode())||taskFlow.getJobStatus().equals(TaskStatusEnum.SETTLED.getCode())) {
            //查询登记信息
            TaskFlowRegistration taskFlowRegistration = taskFlowRegistrationMapper.selectOne(
                    new LambdaQueryWrapper<TaskFlowRegistration>()
                            .eq(TaskFlowRegistration::getTaskId, taskFlow.getId()));
            //查询登出信息
            TaskFlowRegistrationOut taskFlowRegistrationOut = taskFlowOutMapper.selectOne(new LambdaQueryWrapper<TaskFlowRegistrationOut>()
                    .eq(TaskFlowRegistrationOut::getTaskId, taskFlow.getId()));
            //数据整合
            TaskFlowOutQueryRespDTO map = mapperFacade.map(taskFlow, TaskFlowOutQueryRespDTO.class);
            map.setPic(taskFlowRegistration.getPic());
            map.setOutNo(taskFlowRegistrationOut.getOutNo());
            map.setRegistrationUser(userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getId, taskFlowRegistration.getAddUser())).getName());
            map.setStayTime(taskFlowRegistrationOut.getStayTime());
            map.setAddTime(taskFlowRegistrationOut.getAddTime());
            return actionResultService.callBackResult(map);
        }else {
            TaskFlowOutQueryRespDTO map = new TaskFlowOutQueryRespDTO();
            return actionResultService.callBackResult(map);
        }
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActionResult<String, String> saveRegistrationOut(TaskFlow taskFlow, TaskFlowRegistration taskFlowRegistration) {
        //验证智能卡状态
        if(smartCardMapper.selectOne(new LambdaQueryWrapper<SmartCard>()
                .eq(SmartCard::getId, taskFlow.getSmartCardId())).getState().equals(1)){
            return actionResultService.callBackResultFail("请勿重复发卡");
        }
        //验证通知单
        WarehouseOutOrder warehouseOutOrder = warehouseOutOrderMapper.selectOne(new LambdaQueryWrapper<WarehouseOutOrder>()
                .eq(WarehouseOutOrder::getCustomerId, taskFlow.getCustomerId()).eq(WarehouseOutOrder::getState,1));
        if(warehouseOutOrder == null){
            return actionResultService.callBackResultFail("无正在进行的通知单");
        }

        //生成登记id
        String id = Convert.toStr(uidGenerator.getUID());
        //生成工作单id
        String taskid = Convert.toStr(uidGenerator.getUID());
        //添加工作单数据
        taskFlow.setId(taskid);
        taskFlow.setJobType(2);
        taskFlow.setOrderId(warehouseOutOrder.getId());
        taskFlow.setJobStatus(TaskStatusEnum.LOGGED_IN.getCode());
        taskFlow.setJobNo(codeGeneratorComponent.getWorkFlow());
        taskFlow.setAddUser(jwtTokenComponent.getUser().getId());
        taskFlow.setAddTime(LocalDateTime.now());
        //添加登记数据
        taskFlowRegistration.setId(id);
        taskFlowRegistration.setTaskId(taskid);
        taskFlowRegistration.setRegistrationNo(codeGeneratorComponent.getWorkFlowLogin());
        taskFlowRegistration.setAddUser(jwtTokenComponent.getUser().getId());
        taskFlowRegistration.setAddTime(LocalDateTime.now());
        //绑定智能卡信息
        smartCardMapper.update(null, new LambdaUpdateWrapper<SmartCard>()
                .eq(SmartCard::getId, taskFlow.getSmartCardId())
                .set(SmartCard::getState,1));
        //保存数据
        taskFlowMapper.insert(taskFlow);
        taskFlowRegistrationMapper.insert(taskFlowRegistration);
        //返回结果
        return actionResultService.callBackResult("保存成功");
    }
}
