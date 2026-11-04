package com.chinaunicom.smartstorage.admin.service.inter.task;

import com.chinaunicom.smartstorage.admin.dto.task.warehouseItem.*;
import com.chinaunicom.smartstorage.admin.entity.task.WarehouseItem;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import java.util.List;

/**
 * @Description  值仓Service
 * @author zyp
 * @Date 2026-01-22
 */
public interface WarehouseItemService {

	ActionResult<String, String> saveWarehouseItem(WarehouseItem warehouseItem);

	ActionResult<String, String> updateWarehouseItem(WarehouseItem warehouseItem);

	ActionResult<String, String> deleteWarehouseItem(List<WarehouseItem> listWarehouseItem);

	WarehouseItem getWarehouseItemById(String id);

	List<WarehouseItemQueryRespDTO> listWarehouseItemByConditionOnPage(WarehouseItemQueryReqDTO warehouseItemQueryReqDTO);

	TaskWarehouseRespDTO getTaskWarehouse(String taskId);

	WarehouseItemRespDTO getWarehouseItemByTaskId(String taskId);

	WarehouseOutRespDTO getTaskWarehouseOut(String taskId);

}
