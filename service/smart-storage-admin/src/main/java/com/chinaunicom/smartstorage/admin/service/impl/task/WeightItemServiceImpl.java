package com.chinaunicom.smartstorage.admin.service.impl.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.component.CodeGeneratorComponent;
import com.chinaunicom.smartstorage.admin.dto.task.weight.WeightItemInFormRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.weight.WeightItemOutFormRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.weight.WeightItemQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.task.TaskFlow;
import com.chinaunicom.smartstorage.admin.entity.task.WeightItem;
import com.chinaunicom.smartstorage.admin.enumbean.TaskStatusEnum;
import com.chinaunicom.smartstorage.admin.mapper.task.WeightItemMapper;
import com.chinaunicom.smartstorage.admin.mapper.task.taskflow.TaskFlowMapper;
import com.chinaunicom.smartstorage.admin.service.inter.task.WeightItemService;
import com.chinaunicom.smartstorage.common.enumbean.OptEnum;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 称重信息表(WeightItem)表服务实现类
 *
 * @author liuchink
 * @since 2026-01-26 10:45:29
 */
@Slf4j
@Service
public class WeightItemServiceImpl implements WeightItemService {
    @Resource
    private WeightItemMapper weightItemMapper;
    @Resource
    private TaskFlowMapper taskFlowMapper;
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private CodeGeneratorComponent codeGeneratorComponent;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveWeightItem(WeightItem weightItem) {
        ActionResult<String, String> actionResult = check(weightItem, OptEnum.ADD);
        if (actionResult.getStatus().isSuccess()) {
            weightItem.setWeightNo(codeGeneratorComponent.getWorkFlowWeightCheck());
            weightItemMapper.insert(weightItem);
            TaskFlow taskFlow = new TaskFlow();
            taskFlow.setId(weightItem.getTaskId());
            if (weightItem.getWeightType() == 1) {
                taskFlow.setJobStatus(TaskStatusEnum.WEIGHED.getCode());
            } else {
                taskFlow.setJobStatus(TaskStatusEnum.TARE_WEIGHED.getCode());
            }
            taskFlowMapper.updateById(taskFlow);
            actionResult = actionResultService.callBackResult();
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateWeightItem(WeightItem weightItem) {
        ActionResult<String, String> actionResult = check(weightItem, OptEnum.UPDATE);
        if (actionResult.getStatus().isSuccess()) {
            weightItemMapper.updateById(weightItem);
            actionResult = actionResultService.callBackResult();
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteWeightItem(List<WeightItem> listWeightItem) {
        weightItemMapper.deleteByIds(listWeightItem);
        return actionResultService.callBackResult();
    }

    @Override
    public List<WeightItem> listWeightItemByCondition(WeightItemQueryReqDTO weightItemQueryReqDTO) {
        return weightItemMapper.listWeightItemByCondition(weightItemQueryReqDTO);
    }

    @Override
    public WeightItem getWeightItemById(String id) {
        return weightItemMapper.selectById(id);
    }

    @Override
    public WeightItem getWeightItemByDetail(String id) {
        return weightItemMapper.getWeightItemByDetail(id);
    }

    @Override
    public WeightItem getWeightItemByTaskId(String taskId, int weightType) {
        return weightItemMapper.getWeightItemByTaskId(taskId, weightType);
    }

    @Override
    public WeightItemInFormRespDTO getWeightItemByDetailForInForm(String taskId) {
        return weightItemMapper.getWeightItemByDetailForInForm(taskId);
    }

    @Override
    public WeightItemOutFormRespDTO getWeightItemByDetailForOutForm(String taskId) {
        return weightItemMapper.getWeightItemByDetailForOutForm(taskId);
    }

    private ActionResult<String, String> check(WeightItem weightItem, OptEnum optEnum) {
        final TaskFlow taskFlow = taskFlowMapper.selectById(weightItem.getTaskId());
        if (taskFlow == null) {
            return actionResultService.callBackResultFail("作业单无效");
        }
        if (optEnum.equals(OptEnum.ADD)) {
            if (weightItem.getWeightType() == 1 && taskFlow.getJobType() == 1
                && !taskFlow.getJobStatus().equals(TaskStatusEnum.INSPECTED.getCode())) {
                return actionResultService.callBackResultFail("当前作业单不能进行入库称毛重");
            }
            if (weightItem.getWeightType() == 2 && taskFlow.getJobType() == 1
                && !taskFlow.getJobStatus().equals(TaskStatusEnum.WAREHOUSED.getCode())) {
                return actionResultService.callBackResultFail("当前作业单不能进行入库称皮重");
            }
            if (weightItem.getWeightType() == 1 && taskFlow.getJobType() == 2
                && !taskFlow.getJobStatus().equals(TaskStatusEnum.WAREHOUSED.getCode())) {
                return actionResultService.callBackResultFail("当前作业单不能进行出库称毛重");
            }
            if (weightItem.getWeightType() == 2 && taskFlow.getJobType() == 2
                && !taskFlow.getJobStatus().equals(TaskStatusEnum.LOGGED_IN.getCode())) {
                return actionResultService.callBackResultFail("当前作业单不能进行出库称皮重");
            }
            final WeightItem weightItemSame = weightItemMapper
                .selectOne(Wrappers.lambdaQuery(WeightItem.class).eq(WeightItem::getTaskId, weightItem.getTaskId())
                    .eq(WeightItem::getWeightType, weightItem.getWeightType()));
            if (weightItemSame != null && !weightItemSame.getId().equals(weightItem.getId())) {
                return actionResultService.callBackResultFail("当前作业单不能重复称重");
            }
        }
        // 入库-皮重
        if (taskFlow.getJobType() == 1 && weightItem.getWeightType() == 2) {
            final WeightItem weightItemMao = weightItemMapper.selectOne(Wrappers.lambdaQuery(WeightItem.class)
                .eq(WeightItem::getTaskId, weightItem.getTaskId()).eq(WeightItem::getWeightType, 1));
            if (weightItemMao != null) {
                if (weightItem.getWeight().compareTo(weightItemMao.getWeight()) > 0) {
                    return actionResultService.callBackResultFail("皮重必须小于毛重");
                }
            } else {
                return actionResultService.callBackResultFail("请先称毛重");
            }
        }
        // 出库-毛重
        if (taskFlow.getJobType() == 2 && weightItem.getWeightType() == 1) {
            final WeightItem weightItemPi = weightItemMapper.selectOne(Wrappers.lambdaQuery(WeightItem.class)
                .eq(WeightItem::getTaskId, weightItem.getTaskId()).eq(WeightItem::getWeightType, 2));
            if (weightItemPi != null) {
                if (weightItem.getWeight().compareTo(weightItemPi.getWeight()) < 0) {
                    return actionResultService.callBackResultFail("皮重必须小于毛重");
                }
            } else {
                return actionResultService.callBackResultFail("请先称皮重");
            }
        }
        return actionResultService.callBackResult();
    }
}
