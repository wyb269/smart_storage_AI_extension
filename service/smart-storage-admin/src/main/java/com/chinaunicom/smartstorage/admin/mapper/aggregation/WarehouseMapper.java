package com.chinaunicom.smartstorage.admin.mapper.aggregation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse.WarehouseModalReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse.WarehouseQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse.WarehouseQueryRespDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse.WarehouseRespDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.StsWarehouse;

import java.util.List;


/**
 * 仓库表(StsWarehouse)表数据库访问层
 *
 * @author makejava
 * @since 2026-03-10 16:49:26
 */
public interface WarehouseMapper extends BaseMapper<StsWarehouse> {


    WarehouseRespDTO getWarehouseById(String id);

    List<WarehouseQueryRespDTO> listWarehouseByCondition(WarehouseQueryReqDTO warehouseQueryReqDTO);

    List<WarehouseRespDTO> selectByGrainVariety(WarehouseModalReqDTO warehouseModalReqDTO);


}

