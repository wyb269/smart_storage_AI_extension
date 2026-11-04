package com.chinaunicom.smartstorage.admin.service.impl.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.dto.task.warehouseItem.*;
import com.chinaunicom.smartstorage.admin.entity.card.SmartCard;
import com.chinaunicom.smartstorage.admin.entity.task.TaskFlow;
import com.chinaunicom.smartstorage.admin.enumbean.TaskStatusEnum;
import com.chinaunicom.smartstorage.admin.mapper.card.SmartCardMapper;
import com.chinaunicom.smartstorage.admin.mapper.task.taskflow.TaskFlowMapper;
import com.chinaunicom.smartstorage.admin.mapper.task.WarehouseItemMapper;
import com.chinaunicom.smartstorage.admin.service.inter.task.WarehouseItemService;
import com.chinaunicom.smartstorage.admin.entity.task.WarehouseItem;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * @Description  值仓实现类
 * @Author zyp
 * @Date 2026-01-22
 */
@Service
@Slf4j
public class WarehouseItemServiceImpl implements WarehouseItemService {
	@Resource
	private WarehouseItemMapper warehouseItemMapper;
	@Resource
	private TaskFlowMapper taskFlowMapper;
	@Resource
	private SmartCardMapper smartCardMapper;
	@Resource
	private ActionResultService actionResultService;

	@Transactional(rollbackFor = Exception.class)
	@Override
	public ActionResult<String, String> saveWarehouseItem(WarehouseItem warehouseItem) {

		TaskFlow tmpTaskFlow=taskFlowMapper.selectOne(Wrappers.lambdaQuery(TaskFlow.class)
				.eq(TaskFlow::getId, warehouseItem.getTaskId())
				.isNotNull(TaskFlow::getCustomerId));
		if(tmpTaskFlow==null){
			return actionResultService.callBackResultFail("作业单无效");
		}
		Long smartCardCount=smartCardMapper.selectCount(Wrappers.lambdaQuery(SmartCard.class)
				.eq(SmartCard::getId, tmpTaskFlow.getSmartCardId())
				.eq(SmartCard::getState,1));
		if(smartCardCount==0) {
			return actionResultService.callBackResultFail("智能卡无效");
		}
		Long counts=warehouseItemMapper.selectCount(Wrappers.lambdaQuery(WarehouseItem.class)
						.eq(WarehouseItem::getTaskId, warehouseItem.getTaskId())
						.eq(WarehouseItem::getState, 1));
		if(counts!=0){
			return actionResultService.callBackResultFail("不可以重复值仓");
		}
		warehouseItem.setState(1);
		warehouseItemMapper.insert(warehouseItem);

		TaskFlow taskFlow=new TaskFlow();
		taskFlow.setId(warehouseItem.getTaskId());
		taskFlow.setJobStatus(TaskStatusEnum.WAREHOUSED.getCode());
		taskFlowMapper.updateById(taskFlow);

		return actionResultService.callBackResult("添加成功");
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public ActionResult<String, String> updateWarehouseItem(WarehouseItem warehouseItem) {
		warehouseItemMapper.updateById(warehouseItem);
		return actionResultService.callBackResult("修改成功");
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public ActionResult<String, String> deleteWarehouseItem(List<WarehouseItem> listWarehouseItem) {
		warehouseItemMapper.deleteByIds(listWarehouseItem);
		return actionResultService.callBackResult("删除成功");
	}

	@Override
	public WarehouseItem getWarehouseItemById(String id) {
		return warehouseItemMapper.selectById(id);
	}

	@Override
	public List<WarehouseItemQueryRespDTO> listWarehouseItemByConditionOnPage(WarehouseItemQueryReqDTO warehouseItemQueryReqDTO) {
		return warehouseItemMapper.listWarehouseItemByConditionOnPage(warehouseItemQueryReqDTO);
	}

	@Override
	public TaskWarehouseRespDTO getTaskWarehouse(String taskId){
		return warehouseItemMapper.getTaskWarehouse(taskId);
	}

	@Override
	public WarehouseItemRespDTO getWarehouseItemByTaskId(String taskId){
		return warehouseItemMapper.getWarehouseItemByTaskId(taskId);
	}

	@Override
	public WarehouseOutRespDTO getTaskWarehouseOut(String taskId) {
		return warehouseItemMapper.getTaskWarehouseOut(taskId);
	}

}
