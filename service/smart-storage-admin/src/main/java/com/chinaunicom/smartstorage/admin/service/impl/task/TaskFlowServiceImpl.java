package com.chinaunicom.smartstorage.admin.service.impl.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.chinaunicom.smartstorage.admin.dto.task.taskflow.*;
import com.chinaunicom.smartstorage.admin.entity.aggregation.customer.StsCustomer;
import com.chinaunicom.smartstorage.admin.entity.aggregation.dict.DictData;
import com.chinaunicom.smartstorage.admin.entity.task.*;
import com.chinaunicom.smartstorage.admin.enumbean.TaskStatusEnum;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.DictDataMapper;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.customer.StsCustomerMapper;
import com.chinaunicom.smartstorage.admin.mapper.task.*;
import com.chinaunicom.smartstorage.admin.mapper.task.taskflow.TaskFlowMapper;
import com.chinaunicom.smartstorage.admin.service.inter.task.TaskFlowService;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class TaskFlowServiceImpl implements TaskFlowService {
    @Resource
    private SampleItemMapper sampleItemMapper;
    @Resource
    private TaskFlowMapper taskFlowMapper;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private DictDataMapper dictDataMapper;
    @Resource
    private StsCustomerMapper stsCustomerMapper;
    @Resource
    private QualityItemMapper qualityItemMapper;
    @Resource
    private BalanceItemMapper balanceItemMapper;
    @Resource
    private WeightItemMapper weightItemMapper;
    @Resource
    private WarehouseItemMapper warehouseItemMapper;


    @Override
    public List<TaskFlowRespDTO> listTaskFlow(TaskFlowQueryReqDTO dto) {
        return taskFlowMapper.listTaskFlow(dto);
    }
    @Override
    public List<TaskFlowRespDTO> listTaskFlowOut(TaskFlowQueryReqDTO dto) {
        return taskFlowMapper.listTaskFlowOut(dto);
    }

    @Override
    public ActionResult<TaskFlowQueryRespDTO, String> queryTaskFlowOut(String id) {
        TaskFlow taskFlow = taskFlowMapper.selectById(id);
        TaskFlowQueryRespDTO map = mapperFacade.map(taskFlow, TaskFlowQueryRespDTO.class);
        // 查询车牌类型
        map.setTicketType(dictDataMapper.selectOne(new LambdaQueryWrapper<DictData>()
                .eq(DictData::getType, "LICENCE_PLATE_TYPE")
                .eq(DictData::getDescription, taskFlow.getTicketType())).getLabel());
        //查询客户名称
        StsCustomer stsCustomer = stsCustomerMapper.selectOne(new LambdaQueryWrapper<StsCustomer>()
                .eq(StsCustomer::getId, taskFlow.getCustomerId()));
        if (stsCustomer != null) {
            map.setCustomerName(stsCustomer.getName());
        }
        map.setSmartCardNo(taskFlow.getCardNo());
        return actionResultService.callBackResult(map);
    }

    @Override
    public ActionResult<TaskFlowQueryRespDTO, String> queryTaskFlow(String id) {
        TaskFlow taskFlow = taskFlowMapper.selectById(id);
        TaskFlowQueryRespDTO map = mapperFacade.map(taskFlow, TaskFlowQueryRespDTO.class);
        // 查询车牌类型
        map.setTicketType(dictDataMapper.selectOne(new LambdaQueryWrapper<DictData>()
                .eq(DictData::getType, "LICENCE_PLATE_TYPE")
                .eq(DictData::getDescription, taskFlow.getTicketType())).getLabel());
        //查询客户名称
        StsCustomer stsCustomer = stsCustomerMapper.selectOne(new LambdaQueryWrapper<StsCustomer>()
                .eq(StsCustomer::getId, taskFlow.getCustomerId()));
        if (stsCustomer != null) {
            map.setCustomerName(stsCustomer.getName());
        }
        map.setSmartCardNo(taskFlow.getCardNo());
        return actionResultService.callBackResult(map);
    }

    @Override
    public ActionResult<TaskFlowStatisticsRespDTO, String> statistics() {
        return actionResultService.callBackResult(taskFlowMapper.statistics());
    }
    @Override
    public ActionResult<TaskFlowStatisticsOutRespDTO, String> statisticsOut() {
        return actionResultService.callBackResult(taskFlowMapper.statisticsOut());
    }

    @Override
    public ActionResult<String, String> terminated(String id) {
         LambdaUpdateWrapper<TaskFlow> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(TaskFlow::getJobStatus, TaskStatusEnum.PEND_CARD.getCode())
                .eq(TaskFlow::getId,id);
        int update = taskFlowMapper.update(wrapper);
        if (update==1) {
            return actionResultService.callBackResult("终止成功");
        }else {
            return actionResultService.callBackResultFail("终止失败");
        }
    }

    @Override
    public ActionResult<String, String> retracement(TaskFlowRetracement taskFlowRetracement) {
        String currentStatus = taskFlowRetracement.getJobStatus();
        String prevStatus = goBack(currentStatus,taskFlowRetracement.getJobType());

        // 不能回退
        if (prevStatus.equals(currentStatus)) {
            return actionResultService.callBackResultFail("当前流程无法回退");
        }

        // 回退时：清空 当前状态 的数据
        switch (TaskStatusEnum.fromCode(currentStatus)) {
            case SAMPLED -> clearSampledData(taskFlowRetracement.getId());
            case INSPECTED -> clearInspectedData(taskFlowRetracement.getId());
            case WEIGHED -> clearWeighedData(taskFlowRetracement.getId());
            case TARE_WEIGHED -> clearTareWeighedData(taskFlowRetracement.getId());
            case WAREHOUSED -> clearWarehousedData(taskFlowRetracement.getId());
            case SETTLED -> clearSettledData(taskFlowRetracement.getId());
        }
        TaskFlowRetracement taskFlowRetracement1 = new TaskFlowRetracement();
        // 最后更新状态为上一步
        taskFlowRetracement1.setJobStatus(prevStatus);
        taskFlowRetracement1.setId(taskFlowRetracement.getId());
        //更新数据库
        LambdaUpdateWrapper<TaskFlow> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(TaskFlow::getJobStatus, taskFlowRetracement1.getJobStatus())
                .eq(TaskFlow::getId,taskFlowRetracement1.getId());
        taskFlowMapper.update(wrapper);
        return actionResultService.callBackResult("回退成功");
    }
    //入库回退流程
    private static final List<String> FLOW = Arrays.asList(
            TaskStatusEnum.LOGGED_IN.getCode(),
            TaskStatusEnum.SAMPLED.getCode(),
            TaskStatusEnum.INSPECTED.getCode(),
            TaskStatusEnum.WEIGHED.getCode(),
            TaskStatusEnum.WAREHOUSED.getCode(),
            TaskStatusEnum.TARE_WEIGHED.getCode()
            //TaskStatusEnum.SETTLED.getCode()
    );
    // 回退方法
    public static String goBack(String currentState,Integer job) {
        if (job.equals(1)) {
            int index = FLOW.indexOf(currentState);

            if (index == -1) {
                throw new IllegalArgumentException("非法状态");
            }

            // 初始状态 或 已登出 → 不回退
            if (index == 0 || currentState.equals(TaskStatusEnum.LOGOUT.getCode())
                    || currentState.equals(TaskStatusEnum.PEND_CARD.getCode())) {
                return currentState;
            }

            return FLOW.get(index - 1);
        }else {
            int index = FLOWOUT.indexOf(currentState);

            if (index == -1) {
                throw new IllegalArgumentException("非法状态");
            }

            // 初始状态 或 已登出 → 不回退
            if (index == 0 || currentState.equals(TaskStatusEnum.LOGOUT.getCode())
                    ||currentState.equals(TaskStatusEnum.PEND_CARD.getCode())) {
                return currentState;
            }

            return FLOWOUT.get(index - 1);
        }
    }
    private static final List<String> FLOWOUT = Arrays.asList(
            TaskStatusEnum.LOGGED_IN.getCode(),
            TaskStatusEnum.TARE_WEIGHED.getCode(),
            TaskStatusEnum.WAREHOUSED.getCode(),
            TaskStatusEnum.WEIGHED.getCode()
            //TaskStatusEnum.SETTLED.getCode()
    );
    /**
     * 清空仟样数据
     */
    private void clearSampledData(String id) {
        LambdaQueryWrapper<SampleItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SampleItem::getTaskId,id);
        sampleItemMapper.delete(wrapper);
    }
    /**
     *  清空质检
     */
    private void clearInspectedData(String id) {
        LambdaQueryWrapper<QualityItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QualityItem::getTaskId,id);
        qualityItemMapper.delete(wrapper);
    }
    /**
     * 清空称重
     */
    private void clearWeighedData(String id) {
        LambdaQueryWrapper<WeightItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeightItem::getTaskId,id).eq(WeightItem::getWeightType,1);
        weightItemMapper.delete(wrapper);
    }

    /**
     * 清空称皮
     */
    private void clearTareWeighedData(String id) {
        LambdaQueryWrapper<WeightItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WeightItem::getTaskId,id).eq(WeightItem::getWeightType,2);
        weightItemMapper.delete(wrapper);
    }

    /**
     *  清空植仓
     */
    private void clearWarehousedData(String id) {
        LambdaQueryWrapper<WarehouseItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WarehouseItem::getTaskId,id);
        warehouseItemMapper.delete(wrapper);
    }

    /**
     * 清空结算
     */
    private void clearSettledData(String id) {
        LambdaQueryWrapper<BalanceItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BalanceItem::getTaskId,id);
        balanceItemMapper.delete(wrapper);}

}
