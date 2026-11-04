package com.chinaunicom.smartstorage.admin.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinaunicom.smartstorage.admin.dto.order.WarehouseOutOrderQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.order.WarehouseOutOrderQueryRespDTO;
import com.chinaunicom.smartstorage.admin.dto.order.WarehouseOutOrderRespDTO;
import com.chinaunicom.smartstorage.admin.entity.order.WarehouseOutOrder;

import java.util.List;


public interface WarehouseOutOrderMapper extends BaseMapper<WarehouseOutOrder> {
    WarehouseOutOrderRespDTO getWarehouseOutOrderById(String id);

    List<WarehouseOutOrderQueryRespDTO> listWarehouseOutOrderByConditionOnPage(WarehouseOutOrderQueryReqDTO warehouseOutOrderQueryReqDTO);


}
