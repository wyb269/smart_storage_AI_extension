package com.chinaunicom.smartstorage.admin.mapper.task;

import com.chinaunicom.smartstorage.admin.dto.task.warehouseItem.*;
import com.chinaunicom.smartstorage.admin.entity.task.WarehouseItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;

/**
 * @Description  值仓Mapper
 * @author zyp
 * @Date 2026-01-22
 */
public interface WarehouseItemMapper extends BaseMapper<WarehouseItem> {

	List<WarehouseItemQueryRespDTO> listWarehouseItemByConditionOnPage(WarehouseItemQueryReqDTO warehouseItemQueryReqDTO);

	TaskWarehouseRespDTO getTaskWarehouse(String taskId);

	WarehouseItemRespDTO getWarehouseItemByTaskId(String taskId);

	WarehouseOutRespDTO getTaskWarehouseOut(String taskId);


}
