package com.chinaunicom.smartstorage.admin.service.inter.aggregation;


import com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse.WarehouseModalReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse.WarehouseQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse.WarehouseQueryRespDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse.WarehouseRespDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.StsWarehouse;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

public interface WarehouseService {
    ActionResult<String, String> saveWarehouse(StsWarehouse stsWarehouse);

    ActionResult<String, String> updateWarehouse(StsWarehouse stsWarehouse);

    ActionResult<String, String> deleteWarehouse(List<StsWarehouse> warehouseList);

    WarehouseRespDTO getWarehouseById(String id);

    List<WarehouseQueryRespDTO> listWarehouseByCondition(WarehouseQueryReqDTO warehouseQueryReqDTO);

    List<WarehouseRespDTO> selectByGrainVariety(WarehouseModalReqDTO warehouseModalReqDTO);
}
