package com.chinaunicom.smartstorage.admin.service.inter.order;

import com.chinaunicom.smartstorage.admin.dto.order.WarehouseInOrderQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.order.WarehouseInOrderQueryRespDTO;
import com.chinaunicom.smartstorage.admin.dto.order.WarehouseInOrderRespDTO;
import com.chinaunicom.smartstorage.admin.entity.order.WarehouseInOrder;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

public interface WarehouseInOrderService {
    ActionResult<String, String> saveWarehouseInOrder(WarehouseInOrder warehouseInOrder);

    ActionResult<String, String> updateWarehouseInOrder(WarehouseInOrder warehouseInOrder);

    ActionResult<String, String> deleteWarehouseInOrder(List<WarehouseInOrder> warehouseInOrderList);

    WarehouseInOrderRespDTO getWarehouseInOrderById(String id);

    List<WarehouseInOrderQueryRespDTO> listWarehouseInOrderByConditionOnPage(WarehouseInOrderQueryReqDTO warehouseInOrderQueryReqDTO);

    WarehouseInOrderRespDTO selectByTaskId(String taskId);
}
