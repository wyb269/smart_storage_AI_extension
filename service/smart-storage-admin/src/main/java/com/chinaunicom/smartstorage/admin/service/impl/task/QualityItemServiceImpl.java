package com.chinaunicom.smartstorage.admin.service.impl.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.dto.task.qualityItem.QualityItemQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.qualityItem.QualityItemQueryRespDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.StsWarehouse;
import com.chinaunicom.smartstorage.admin.entity.task.TaskFlow;
import com.chinaunicom.smartstorage.admin.entity.card.SmartCard;
import com.chinaunicom.smartstorage.admin.entity.task.QualityItem;
import com.chinaunicom.smartstorage.admin.enumbean.TaskStatusEnum;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.WarehouseMapper;
import com.chinaunicom.smartstorage.admin.mapper.card.SmartCardMapper;
import com.chinaunicom.smartstorage.admin.mapper.rbac.UserMapper;
import com.chinaunicom.smartstorage.admin.mapper.task.taskflow.TaskFlowMapper;
import com.chinaunicom.smartstorage.admin.mapper.task.QualityItemMapper;
import com.chinaunicom.smartstorage.admin.service.inter.task.QualityItemService;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
/**
 * @Description  质量检验
 * @author zhangy
 * @Date 2026-01-29
 */
@Slf4j
@Service
public class QualityItemServiceImpl implements QualityItemService {
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private QualityItemMapper qualityItemMapper;
    @Resource
    private TaskFlowMapper taskFlowMapper;
    @Resource
    private SmartCardMapper smartCardMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private WarehouseMapper warehouseMapper;
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveQualityItem(QualityItem qualityItem) {
        if(qualityItem.getDeduction().compareTo(BigDecimal.ONE)>=0){
            return actionResultService.callBackResultFail("每千克扣量应该小于1千克");
        }
        if(qualityItem.getDeductionPrice().compareTo(qualityItem.getGrainPrice())>=0){
            return actionResultService.callBackResultFail("每千克扣价应该小于粮食单价");
        }
        //校验智能卡是否处于已发卡状态
        TaskFlow taskFlow = taskFlowMapper.selectById(qualityItem.getTaskId());
        if(taskFlow==null){
            return actionResultService.callBackResultFail("作业单无效");
        }
        Long smartCardCount=smartCardMapper.selectCount(Wrappers.lambdaQuery(SmartCard.class)
                .eq(SmartCard::getId, taskFlow.getSmartCardId())
                .eq(SmartCard::getState,1));
        if(smartCardCount==0) {
            return actionResultService.callBackResultFail("智能卡处于未发卡状态");
        }
        //校验是否进行了重复质检
        List<QualityItem> qualityItemList=qualityItemMapper.selectByTaskId(qualityItem.getTaskId());
        if(qualityItemList != null && qualityItemList.size() > 0){
            return actionResultService.callBackResultFail("不能重复质检");
        }
        qualityItemMapper.insert(qualityItem);
        //合格为已质检，不合格为待销卡
        TaskFlow insertTaskFlow=new TaskFlow();
        insertTaskFlow.setId(qualityItem.getTaskId());
        if(qualityItem.getQualityResult()==1){
            insertTaskFlow.setJobStatus(TaskStatusEnum.INSPECTED.getCode());
        }
        if(qualityItem.getQualityResult()==2){
            insertTaskFlow.setJobStatus(TaskStatusEnum.PEND_CARD.getCode());
        }
        taskFlowMapper.updateById(insertTaskFlow);
        //如果仓库没有粮食年期和粮食等级 更新
        StsWarehouse warehouse = warehouseMapper.selectById(qualityItem.getWarehouseId());
        if(warehouse.getGrainGrade()==null || warehouse.getGrainYear()==null){
            warehouse.setGrainGrade(qualityItem.getGrainGrade());
            warehouse.setGrainYear(qualityItem.getGrainYear());
            warehouseMapper.updateById(warehouse);
        }
        return actionResultService.callBackResult("添加成功");
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateQualityItem(QualityItem qualityItem) {
        qualityItemMapper.updateById(qualityItem);
        return actionResultService.callBackResult("修改成功");
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteQualityItem(List<QualityItem> qualityItemList) {
        qualityItemMapper.deleteByIds(qualityItemList);
        return actionResultService.callBackResult("删除成功");
    }

    @Override
    public QualityItem getQualityItemById(String id) {
        QualityItem qualityItem = qualityItemMapper.selectById(id);
        String addUser = userMapper.getUserByDetail(qualityItem.getAddUser()).getName();
        qualityItem.setAddUser(addUser);
        return qualityItem;
    }

    @Override
    public List<QualityItemQueryRespDTO> listQualityItemByConditionOnPage(QualityItemQueryReqDTO qualityItemQueryReqDTO) {
        return qualityItemMapper.listQualityItemByCondition(qualityItemQueryReqDTO);
    }

    @Override
    public QualityItem getQualityItemByTaskId(String taskId) {
        QualityItem qualityItem = qualityItemMapper.getQualityItemByTaskId(taskId);
        return qualityItem;
    }
}
