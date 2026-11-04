package com.chinaunicom.smartstorage.admin.service.impl.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.dto.task.sampleItem.SampleItemQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.sampleItem.SampleItemQueryRespDTO;
import com.chinaunicom.smartstorage.admin.entity.order.WarehouseInOrder;
import com.chinaunicom.smartstorage.admin.entity.task.SampleItem;
import com.chinaunicom.smartstorage.admin.entity.card.SmartCard;
import com.chinaunicom.smartstorage.admin.entity.task.TaskFlow;
import com.chinaunicom.smartstorage.admin.enumbean.TaskStatusEnum;
import com.chinaunicom.smartstorage.admin.mapper.order.WarehouseInOrderMapper;
import com.chinaunicom.smartstorage.admin.mapper.rbac.UserMapper;
import com.chinaunicom.smartstorage.admin.mapper.task.SampleItemMapper;
import com.chinaunicom.smartstorage.admin.mapper.card.SmartCardMapper;
import com.chinaunicom.smartstorage.admin.mapper.task.taskflow.TaskFlowMapper;
import com.chinaunicom.smartstorage.admin.service.inter.task.SampleItemService;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * @Description 扦样信息
 * @author zhangy
 * @Date 2026-01-26
 */
@Slf4j
@Service
public class SampleItemServiceImpl implements SampleItemService {
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private SampleItemMapper sampleItemMapper;
    @Resource
    private TaskFlowMapper taskFlowMapper;
    @Resource
    private SmartCardMapper smartCardMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private WarehouseInOrderMapper warehouseInOrderMapper;
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveSampleItem(SampleItem sampleItem) {
        //校验智能卡是否处于已发卡状态
        TaskFlow taskFlow = taskFlowMapper.selectById(sampleItem.getTaskId());
        if(taskFlow==null){
            return actionResultService.callBackResultFail("作业单无效");
        }
        Long smartCardCount=smartCardMapper.selectCount(Wrappers.lambdaQuery(SmartCard.class)
                .eq(SmartCard::getId, taskFlow.getSmartCardId())
                .eq(SmartCard::getState,1));
        if(smartCardCount==0) {
            return actionResultService.callBackResultFail("智能卡处于未发卡状态");
        }
        //校验是否进行了重复扦样
        List<SampleItem> sampleItemList=sampleItemMapper.selectByTaskId(sampleItem.getTaskId());
        if(sampleItemList != null && sampleItemList.size() > 0){
            return actionResultService.callBackResultFail("不能重复扦样");
        }
        //粮食品种从入库通知单取值
        WarehouseInOrder warehouseInOrder = warehouseInOrderMapper.selectById(taskFlow.getOrderId());
        sampleItem.setGrainVariety(warehouseInOrder.getGrainVariety());
        //插入扦样表
        sampleItemMapper.insert(sampleItem);
        //更新作业单流程为已扦样
        TaskFlow insertTaskFlow=new TaskFlow();
        insertTaskFlow.setId(sampleItem.getTaskId());
        insertTaskFlow.setJobStatus(TaskStatusEnum.SAMPLED.getCode());
        taskFlowMapper.updateById(insertTaskFlow);
        return actionResultService.callBackResult("添加成功");
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateSampleItem(SampleItem sampleItem) {
        sampleItemMapper.updateById(sampleItem);
        return actionResultService.callBackResult("修改成功");
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteSampleItem(List<SampleItem> listSampleItem) {
        sampleItemMapper.deleteByIds(listSampleItem);
        return actionResultService.callBackResult("删除成功");
    }

    @Override
    public SampleItem getSampleItemById(String id) {
        SampleItem sampleItem = sampleItemMapper.selectById(id);
        String addUser = userMapper.getUserByDetail(sampleItem.getAddUser()).getName();
        sampleItem.setAddUser(addUser);
        return sampleItem;
    }

    @Override
    public List<SampleItemQueryRespDTO> listSampleItemByConditionOnPage(SampleItemQueryReqDTO sampleItemQueryReqDTO) {
        return sampleItemMapper.listSampleItemByCondition(sampleItemQueryReqDTO);
    }

    @Override
    public SampleItem getSampleItemByTaskId(String taskId) {
        SampleItem sampleItem = sampleItemMapper.getSampleItemByTaskId(taskId);
        String addUser = userMapper.getUserByDetail(sampleItem.getAddUser()).getName();
        sampleItem.setAddUser(addUser);
        return sampleItem;
    }


}
