package com.chinaunicom.smartstorage.admin.service.impl.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.dto.order.WarehouseInOrderQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.order.WarehouseInOrderQueryRespDTO;
import com.chinaunicom.smartstorage.admin.dto.order.WarehouseInOrderRespDTO;
import com.chinaunicom.smartstorage.admin.entity.card.SmartCard;
import com.chinaunicom.smartstorage.admin.entity.order.WarehouseInOrder;
import com.chinaunicom.smartstorage.admin.mapper.order.WarehouseInOrderMapper;
import com.chinaunicom.smartstorage.admin.service.inter.order.WarehouseInOrderService;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class WarehouseInOrderServiceImpl implements WarehouseInOrderService {
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private WarehouseInOrderMapper warehouseInOrderMapper;
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveWarehouseInOrder(WarehouseInOrder warehouseInOrder) {
        Long count = warehouseInOrderMapper.selectCount(Wrappers.lambdaQuery(WarehouseInOrder.class)
                .eq(WarehouseInOrder::getCustomerId, warehouseInOrder.getCustomerId())
                .eq(WarehouseInOrder::getState,1));
        if(count!=0){
            return actionResultService.callBackResultFail("一个客户只能有一个正在进行的入库通知单");
        }
        warehouseInOrderMapper.insert(warehouseInOrder);
        return actionResultService.callBackResult("添加成功");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateWarehouseInOrder(WarehouseInOrder warehouseInOrder) {
        warehouseInOrder.setState(3);
        warehouseInOrderMapper.updateById(warehouseInOrder);
        return actionResultService.callBackResult("修改成功");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteWarehouseInOrder(List<WarehouseInOrder> warehouseInOrderList) {
        return null;
    }

    @Override
    public WarehouseInOrderRespDTO getWarehouseInOrderById(String id) {
        return warehouseInOrderMapper.getWarehouseInOrderById(id);
    }

    @Override
    public List<WarehouseInOrderQueryRespDTO> listWarehouseInOrderByConditionOnPage(WarehouseInOrderQueryReqDTO warehouseInOrderQueryReqDTO) {
        return warehouseInOrderMapper.listWarehouseInOrderByConditionOnPage(warehouseInOrderQueryReqDTO);
    }

    @Override
    public WarehouseInOrderRespDTO selectByTaskId(String taskId) {
        return warehouseInOrderMapper.selectByTaskId(taskId);
    }
}
