package com.chinaunicom.smartstorage.admin.service.inter.order;

import com.chinaunicom.smartstorage.admin.dto.order.WarehouseOutOrderQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.order.WarehouseOutOrderQueryRespDTO;
import com.chinaunicom.smartstorage.admin.dto.order.WarehouseOutOrderRespDTO;
import com.chinaunicom.smartstorage.admin.dto.order.*;
import com.chinaunicom.smartstorage.admin.entity.order.WarehouseOutOrder;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

public interface WarehouseOutOrderService {
    ActionResult<String, String> saveWarehouseOutOrder(WarehouseOutOrder warehouseOutOrder);

    ActionResult<String, String> updateWarehouseOutOrder(WarehouseOutOrder warehouseOutOrder);

    ActionResult<String, String> deleteWarehouseOutOrder(List<WarehouseOutOrder> warehouseOutOrderList);

    WarehouseOutOrderRespDTO getWarehouseOutOrderById(String id);

    List<WarehouseOutOrderQueryRespDTO> listWarehouseOutOrderByConditionOnPage(WarehouseOutOrderQueryReqDTO warehouseOutOrderQueryReqDTO);
}
