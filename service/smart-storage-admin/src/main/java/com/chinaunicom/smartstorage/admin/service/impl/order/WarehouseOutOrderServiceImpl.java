package com.chinaunicom.smartstorage.admin.service.impl.order;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.dto.order.WarehouseOutOrderQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.order.WarehouseOutOrderQueryRespDTO;
import com.chinaunicom.smartstorage.admin.dto.order.WarehouseOutOrderRespDTO;
import com.chinaunicom.smartstorage.admin.entity.order.WarehouseInOrder;
import com.chinaunicom.smartstorage.admin.entity.order.WarehouseOutOrder;
import com.chinaunicom.smartstorage.admin.mapper.order.WarehouseOutOrderMapper;
import com.chinaunicom.smartstorage.admin.service.inter.order.WarehouseOutOrderService;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class WarehouseOutOrderServiceImpl implements WarehouseOutOrderService {
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private WarehouseOutOrderMapper warehouseOutOrderMapper;
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveWarehouseOutOrder(WarehouseOutOrder warehouseOutOrder) {
        Long count = warehouseOutOrderMapper.selectCount(Wrappers.lambdaQuery(WarehouseOutOrder.class)
                .eq(WarehouseOutOrder::getCustomerId, warehouseOutOrder.getCustomerId())
                .eq(WarehouseOutOrder::getState,1));
        if(count!=0){
            return actionResultService.callBackResultFail("一个客户只能有一个正在进行的出库通知单");
        }
        warehouseOutOrderMapper.insert(warehouseOutOrder);
        return actionResultService.callBackResult("添加成功");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateWarehouseOutOrder(WarehouseOutOrder warehouseOutOrder) {
        warehouseOutOrder.setState(3);
        warehouseOutOrderMapper.updateById(warehouseOutOrder);
        return actionResultService.callBackResult("修改成功");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteWarehouseOutOrder(List<WarehouseOutOrder> warehouseOutOrderList) {
        return null;
    }

    @Override
    public WarehouseOutOrderRespDTO getWarehouseOutOrderById(String id) {
        return warehouseOutOrderMapper.getWarehouseOutOrderById(id);
    }

    @Override
    public List<WarehouseOutOrderQueryRespDTO> listWarehouseOutOrderByConditionOnPage(WarehouseOutOrderQueryReqDTO warehouseOutOrderQueryReqDTO) {
        return warehouseOutOrderMapper.listWarehouseOutOrderByConditionOnPage(warehouseOutOrderQueryReqDTO);
    }
}
