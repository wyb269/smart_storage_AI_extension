package com.chinaunicom.smartstorage.admin.service.impl.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.dto.task.balanceItem.TaskBalanceRespDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.StsWarehouse;
import com.chinaunicom.smartstorage.admin.entity.aggregation.SystemConfig;
import com.chinaunicom.smartstorage.admin.entity.aggregation.customer.StsCustomer;
import com.chinaunicom.smartstorage.admin.entity.card.SmartCard;
import com.chinaunicom.smartstorage.admin.entity.order.WarehouseInOrder;
import com.chinaunicom.smartstorage.admin.entity.order.WarehouseOutOrder;
import com.chinaunicom.smartstorage.admin.entity.task.QualityItem;
import com.chinaunicom.smartstorage.admin.entity.task.TaskFlow;
import com.chinaunicom.smartstorage.admin.enumbean.TaskStatusEnum;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.SystemConfigMapper;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.WarehouseMapper;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.customer.StsCustomerMapper;
import com.chinaunicom.smartstorage.admin.mapper.card.SmartCardMapper;
import com.chinaunicom.smartstorage.admin.mapper.order.WarehouseInOrderMapper;
import com.chinaunicom.smartstorage.admin.mapper.order.WarehouseOutOrderMapper;
import com.chinaunicom.smartstorage.admin.mapper.task.QualityItemMapper;
import com.chinaunicom.smartstorage.admin.mapper.task.taskflow.TaskFlowMapper;
import com.chinaunicom.smartstorage.admin.mapper.task.BalanceItemMapper;
import com.chinaunicom.smartstorage.admin.service.inter.task.BalanceItemService;
import com.chinaunicom.smartstorage.admin.dto.task.balanceItem.BalanceItemQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.balanceItem.BalanceItemQueryRespDTO;
import com.chinaunicom.smartstorage.admin.entity.task.BalanceItem;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Description  单车结算实现类
 * @Author zyp
 * @Date 2026-01-22
 */
@Service
@Slf4j
public class BalanceItemServiceImpl implements BalanceItemService {
	@Resource
	private BalanceItemMapper balanceItemMapper;
	@Resource
	private TaskFlowMapper taskFlowMapper;
	@Resource
	private SmartCardMapper smartCardMapper;
	@Resource
	private StsCustomerMapper customerMapper;
	@Resource
	private ActionResultService actionResultService;
	@Resource
	private WarehouseMapper warehouseMapper;
	@Resource
	private WarehouseInOrderMapper warehouseInOrderMapper;
	@Resource
	private SystemConfigMapper systemConfigMapper;
	@Resource
	private QualityItemMapper qualityItemMapper;
	@Resource
	private WarehouseOutOrderMapper warehouseOutOrderMapper;
	@Transactional(rollbackFor = Exception.class)
	@Override
	public ActionResult<String, String> saveBalanceItem(BalanceItem balanceItem) {
		TaskFlow tmpTaskFlow=taskFlowMapper.selectOne(Wrappers.lambdaQuery(TaskFlow.class)
				.eq(TaskFlow::getId, balanceItem.getTaskId())
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
		//获取客户信息
		StsCustomer  customer=customerMapper.selectById(tmpTaskFlow.getCustomerId());
		if(customer==null){
			return actionResultService.callBackResultFail("客户信息无效");
		}
		Long counts=balanceItemMapper.selectCount(Wrappers.lambdaQuery(BalanceItem.class)
				.eq(BalanceItem::getTaskId, balanceItem.getTaskId()));
		if(counts!=0){
			return actionResultService.callBackResultFail("不可以重复结算");
		}
		//校验入库通知单状态
		WarehouseInOrder warehouseInOrder = warehouseInOrderMapper.selectById(tmpTaskFlow.getOrderId());
		if(warehouseInOrder == null){
			return actionResultService.callBackResultFail("入库通知单无效");
		}
		if(warehouseInOrder.getState()!=1){
			return actionResultService.callBackResultFail("入库通知单已完成或已关闭，无法结算");
		}
		//结算计算
		TaskBalanceRespDTO taskBalanceRespDTO=balanceItemMapper.getTaskBalance(balanceItem.getTaskId());
		if(taskBalanceRespDTO==null){
			return actionResultService.callBackResultFail("入库流程异常，无法结算");
		}
		balanceItem.setGrossWeight(taskBalanceRespDTO.getGrossWeight());
		balanceItem.setTareWeight(taskBalanceRespDTO.getTareWeight());
		balanceItem.setNetWeight(taskBalanceRespDTO.getNetWeight());
		balanceItem.setPreDeductWeight(taskBalanceRespDTO.getDeduction());
		balanceItem.setSceneDeductWeight(taskBalanceRespDTO.getDeductWeight());
		//每千克扣量x净重+值仓现场扣量
		balanceItem.setDeductWeightTotal(balanceItem.getPreDeductWeight().multiply(balanceItem.getNetWeight()).add(balanceItem.getSceneDeductWeight()));
		balanceItem.setPreDeductPrice(taskBalanceRespDTO.getDeductionPrice());

		//标准单价-每千克扣价=结算单价
		balanceItem.setBalancePrice(balanceItem.getPrice().subtract(balanceItem.getPreDeductPrice()));
		//净重-扣量合计(每千克扣量x净重+值仓现场扣量)=结算重量
		balanceItem.setBalanceWeight(balanceItem.getNetWeight().subtract(balanceItem.getDeductWeightTotal()));
		//结算单价x结算重量=结算金额
		balanceItem.setBalanceAmount(balanceItem.getBalancePrice().multiply(balanceItem.getBalanceWeight()));

		//客户信息
		balanceItem.setIdCard(customer.getCard());
		balanceItem.setLinkPhone(customer.getPhone());
		balanceItem.setBankAccount(customer.getBankAccount());
		balanceItem.setBankCode(customer.getBankCode());
		balanceItem.setBankName(customer.getBankName());
		balanceItemMapper.insert(balanceItem);

		TaskFlow taskFlow=new TaskFlow();
		taskFlow.setId(balanceItem.getTaskId());
		taskFlow.setJobStatus(TaskStatusEnum.SETTLED.getCode());
		taskFlowMapper.updateById(taskFlow);

		//结算重量累加到仓库的当前容量
		QualityItem qualityItem = qualityItemMapper.selectOne(Wrappers.lambdaQuery(QualityItem.class).eq(QualityItem::getTaskId,balanceItem.getTaskId()));
		StsWarehouse warehouse = warehouseMapper.selectById(qualityItem.getWarehouseId());
		BigDecimal current = warehouse.getCurrentWeight().add(balanceItem.getBalanceWeight());
		warehouse.setCurrentWeight(current);
		warehouseMapper.updateById(warehouse);
		//结算重量累加到入库通知单的完成量
		BigDecimal completed = warehouseInOrder.getCompletedWeight().add(balanceItem.getBalanceWeight());
		warehouseInOrder.setCompletedWeight(completed);
		warehouseInOrderMapper.updateById(warehouseInOrder);
		//通知单的累计完成量 >= 通知单的预计入库量-差容量 通知单状态变更为“已完成”
		SystemConfig systemConfig = systemConfigMapper.selectOne(
				Wrappers.lambdaQuery(SystemConfig.class).eq(SystemConfig::getSycGroup, "inboundDiffCapacity")
						.eq(SystemConfig::getSycKey, "inboundDiffCapacity"));
		BigDecimal diff = new BigDecimal(systemConfig.getSycValue());
		BigDecimal threshold = warehouseInOrder.getPlanWeight().subtract(diff);
		if(warehouseInOrder.getCompletedWeight().compareTo(threshold) >= 0){
			warehouseInOrder.setState(2);
			warehouseInOrderMapper.updateById(warehouseInOrder);
		}

		return actionResultService.callBackResult("添加成功");
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public ActionResult<String, String> updateBalanceItem(BalanceItem balanceItem) {
		balanceItemMapper.updateById(balanceItem);
		return actionResultService.callBackResult("修改成功");
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public ActionResult<String, String> deleteBalanceItem(List<BalanceItem> listBalanceItem) {
		balanceItemMapper.deleteByIds(listBalanceItem);
		return actionResultService.callBackResult("删除成功");
	}

	@Override
	public BalanceItem getBalanceItemById(String id) {
		return balanceItemMapper.selectById(id);
	}

	@Override
	public List<BalanceItemQueryRespDTO> listBalanceItemByConditionOnPage(BalanceItemQueryReqDTO balanceItemQueryReqDTO) {
		return balanceItemMapper.listBalanceItemByConditionOnPage(balanceItemQueryReqDTO);
	}

	@Override
	public TaskBalanceRespDTO getTaskBalance(String taskId){
		TaskBalanceRespDTO taskBalanceRespDTO=balanceItemMapper.getTaskBalance(taskId);
		//每千克扣量x净重+值仓现场扣量=扣量合计
		taskBalanceRespDTO.setDeductWeightTotal(taskBalanceRespDTO.getDeduction().multiply(taskBalanceRespDTO.getNetWeight()).add(taskBalanceRespDTO.getDeductWeight()));
		return taskBalanceRespDTO;
	}

	@Override
	public BalanceItem getBalanceItemByTaskId(String taskId){
		BalanceItem balanceItem= balanceItemMapper.selectOne(Wrappers.lambdaQuery(BalanceItem.class)
				.eq(BalanceItem::getTaskId,taskId));
		return balanceItem;
	}

	@Override
	public ActionResult<String, String> saveBalanceItemOut(BalanceItem balanceItem) {
		TaskFlow tmpTaskFlow=taskFlowMapper.selectOne(Wrappers.lambdaQuery(TaskFlow.class)
				.eq(TaskFlow::getId, balanceItem.getTaskId())
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
		//获取客户信息
		StsCustomer  customer=customerMapper.selectById(tmpTaskFlow.getCustomerId());
		if(customer==null){
			return actionResultService.callBackResultFail("客户信息无效");
		}
		Long counts=balanceItemMapper.selectCount(Wrappers.lambdaQuery(BalanceItem.class)
				.eq(BalanceItem::getTaskId, balanceItem.getTaskId()));
		if(counts!=0){
			return actionResultService.callBackResultFail("不可以重复结算");
		}
		//校验出库通知单状态
		WarehouseOutOrder warehouseOutOrder = warehouseOutOrderMapper.selectById(tmpTaskFlow.getOrderId());
		if(warehouseOutOrder == null){
			return actionResultService.callBackResultFail("出库通知单无效");
		}
		if(warehouseOutOrder.getState()!=1){
			return actionResultService.callBackResultFail("出库通知单已完成或已关闭，无法结算");
		}
		//结算计算
		TaskBalanceRespDTO taskBalanceRespDTO=balanceItemMapper.getTaskBalance(balanceItem.getTaskId());
		if(taskBalanceRespDTO==null){
			return actionResultService.callBackResultFail("入库流程异常，无法结算");
		}
		balanceItem.setGrossWeight(taskBalanceRespDTO.getGrossWeight());
		balanceItem.setTareWeight(taskBalanceRespDTO.getTareWeight());
		balanceItem.setNetWeight(taskBalanceRespDTO.getNetWeight());
		balanceItem.setPreDeductWeight(taskBalanceRespDTO.getDeduction());
		balanceItem.setSceneDeductWeight(taskBalanceRespDTO.getDeductWeight());
		//每千克扣量x净重+值仓现场扣量
		balanceItem.setDeductWeightTotal(balanceItem.getPreDeductWeight().multiply(balanceItem.getNetWeight()).add(balanceItem.getSceneDeductWeight()));
		balanceItem.setPreDeductPrice(taskBalanceRespDTO.getDeductionPrice());

		//标准单价-每千克扣价=结算单价
		balanceItem.setBalancePrice(balanceItem.getPrice().subtract(balanceItem.getPreDeductPrice()));
		//净重-扣量合计(每千克扣量x净重+值仓现场扣量)=结算重量
		balanceItem.setBalanceWeight(balanceItem.getNetWeight().subtract(balanceItem.getDeductWeightTotal()));
		//结算单价x结算重量=结算金额
		balanceItem.setBalanceAmount(balanceItem.getBalancePrice().multiply(balanceItem.getBalanceWeight()));

		//客户信息
		balanceItem.setIdCard(customer.getCard());
		balanceItem.setLinkPhone(customer.getPhone());
		balanceItem.setBankAccount(customer.getBankAccount());
		balanceItem.setBankCode(customer.getBankCode());
		balanceItem.setBankName(customer.getBankName());
		balanceItemMapper.insert(balanceItem);

		TaskFlow taskFlow=new TaskFlow();
		taskFlow.setId(balanceItem.getTaskId());
		taskFlow.setJobStatus(TaskStatusEnum.SETTLED.getCode());
		taskFlowMapper.updateById(taskFlow);

		//仓库的当前容量减去结算重量
		StsWarehouse warehouse = warehouseMapper.selectById(warehouseOutOrder.getWarehouseId());
		BigDecimal current = warehouse.getCurrentWeight().subtract(balanceItem.getBalanceWeight());
		warehouse.setCurrentWeight(current);
		warehouseMapper.updateById(warehouse);
		//结算重量累加到出库通知单的完成量
		BigDecimal completed = warehouseOutOrder.getCompletedWeight().add(balanceItem.getBalanceWeight());
		warehouseOutOrder.setCompletedWeight(completed);
		warehouseOutOrderMapper.updateById(warehouseOutOrder);
		//通知单的累计完成量 >= 通知单的预计出库量-差容量 通知单状态变更为“已完成”
		SystemConfig systemConfig = systemConfigMapper.selectOne(
				Wrappers.lambdaQuery(SystemConfig.class).eq(SystemConfig::getSycGroup, "outboundDiffCapacity")
						.eq(SystemConfig::getSycKey, "outboundDiffCapacity"));
		BigDecimal diff = new BigDecimal(systemConfig.getSycValue());
		BigDecimal threshold = warehouseOutOrder.getPlanWeight().subtract(diff);
		if(warehouseOutOrder.getCompletedWeight().compareTo(threshold) >= 0){
			warehouseOutOrder.setState(2);
			warehouseOutOrderMapper.updateById(warehouseOutOrder);
		}

		return actionResultService.callBackResult("添加成功");
	}
}
