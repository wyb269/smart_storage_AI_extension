package com.chinaunicom.smartstorage.admin.service.inter.task;

import com.chinaunicom.smartstorage.admin.dto.task.balanceItem.BalanceItemQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.balanceItem.BalanceItemQueryRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.balanceItem.TaskBalanceRespDTO;
import com.chinaunicom.smartstorage.admin.entity.task.BalanceItem;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import java.util.List;

/**
 * @Description  单车结算Service
 * @author zyp
 * @Date 2026-01-22
 */
public interface BalanceItemService {

	ActionResult<String, String> saveBalanceItem(BalanceItem balanceItem);

	ActionResult<String, String> updateBalanceItem(BalanceItem balanceItem);

	ActionResult<String, String> deleteBalanceItem(List<BalanceItem> listBalanceItem);

	BalanceItem getBalanceItemById(String id);

	List<BalanceItemQueryRespDTO> listBalanceItemByConditionOnPage(BalanceItemQueryReqDTO balanceItemQueryReqDTO);

	TaskBalanceRespDTO getTaskBalance(String taskId);

	BalanceItem getBalanceItemByTaskId(String taskId);

	ActionResult<String, String> saveBalanceItemOut(BalanceItem balanceItem);
}
