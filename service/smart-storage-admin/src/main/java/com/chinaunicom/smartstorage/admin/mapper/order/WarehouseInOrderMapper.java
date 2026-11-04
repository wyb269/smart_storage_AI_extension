package com.chinaunicom.smartstorage.admin.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinaunicom.smartstorage.admin.dto.order.WarehouseInOrderQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.order.WarehouseInOrderQueryRespDTO;
import com.chinaunicom.smartstorage.admin.dto.order.WarehouseInOrderRespDTO;
import com.chinaunicom.smartstorage.admin.entity.order.WarehouseInOrder;

import java.util.List;

public interface WarehouseInOrderMapper extends BaseMapper<WarehouseInOrder> {
    WarehouseInOrderRespDTO getWarehouseInOrderById(String id);

    List<WarehouseInOrderQueryRespDTO> listWarehouseInOrderByConditionOnPage(WarehouseInOrderQueryReqDTO warehouseInOrderQueryReqDTO);

    WarehouseInOrderRespDTO selectByTaskId(String taskId);
}
