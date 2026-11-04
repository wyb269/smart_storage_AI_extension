package com.chinaunicom.smartstorage.admin.service.impl.aggregation;

import com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse.WarehouseModalReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse.WarehouseQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse.WarehouseQueryRespDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse.WarehouseRespDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.StsWarehouse;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.WarehouseMapper;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.WarehouseService;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class WarehouseServiceImpl implements WarehouseService {
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private WarehouseMapper warehouseMapper;
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveWarehouse(StsWarehouse stsWarehouse) {
        warehouseMapper.insert(stsWarehouse);
        return actionResultService.callBackResult("添加成功");
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateWarehouse(StsWarehouse stsWarehouse) {
        warehouseMapper.updateById(stsWarehouse);
        return actionResultService.callBackResult("修改成功");
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteWarehouse(List<StsWarehouse> warehouseList) {
        for(StsWarehouse warehouse: warehouseList){
            StsWarehouse stsWarehouse = warehouseMapper.selectById(warehouse.getId());
            if(stsWarehouse!=null){
                if( stsWarehouse.getState() != 2 ){
                    return actionResultService.callBackResultFail("仓库不是禁用状态，无法删除");
                }
            }
        }
        warehouseMapper.deleteByIds(warehouseList);
        return actionResultService.callBackResult("删除成功");
    }

    @Override
    public WarehouseRespDTO getWarehouseById(String id) {
        return warehouseMapper.getWarehouseById(id);
    }

    @Override
    public List<WarehouseQueryRespDTO> listWarehouseByCondition(WarehouseQueryReqDTO warehouseQueryReqDTO) {
        return warehouseMapper.listWarehouseByCondition(warehouseQueryReqDTO);
    }

    @Override
    public List<WarehouseRespDTO> selectByGrainVariety(WarehouseModalReqDTO warehouseModalReqDTO) {
        return warehouseMapper.selectByGrainVariety(warehouseModalReqDTO);
    }
}
