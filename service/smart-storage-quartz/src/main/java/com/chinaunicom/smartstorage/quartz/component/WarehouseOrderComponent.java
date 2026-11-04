package com.chinaunicom.smartstorage.quartz.component;

import com.chinaunicom.smartstorage.quartz.entity.WarehouseInOrder;
import com.chinaunicom.smartstorage.quartz.entity.WarehouseOutOrder;
import com.chinaunicom.smartstorage.quartz.mapper.WarehouseInOrderMapper;
import com.chinaunicom.smartstorage.quartz.mapper.WarehouseOutOrderMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Description  入库/出库 通知单自动关闭定时器
 * @author zhangyu
 * @since 2026/3/20
 */
@Slf4j
@Component
public class WarehouseOrderComponent {
    @Resource
    private WarehouseInOrderMapper warehouseInOrderMapper;
    @Resource
    private WarehouseOutOrderMapper warehouseOutOrderMapper;
    @Transactional(rollbackFor = Exception.class)
    @XxlJob("warehouseInOrderCloseJob")
    public void warehouseInOrderAutoClose() {
        log.debug("===== 开始执行【入库通知单自动关闭】定时任务 =====");
        List<WarehouseInOrder> warehouseInOrderList = warehouseInOrderMapper.listPendingClose();
        if (warehouseInOrderList.isEmpty()) {
            log.debug("无待关闭的入库通知单，任务执行完成");
        }else {
            for(WarehouseInOrder warehouseInOrder:warehouseInOrderList){
                warehouseInOrder.setState(3);
                warehouseInOrder.setUpdateTime(LocalDateTime.now());
                warehouseInOrderMapper.updateById(warehouseInOrder);
                log.debug("入库通知单ID：{} 已自动关闭", warehouseInOrder.getId());
            }
        }
        log.debug("=====入库通知单自动关闭任务执行完成=====");
    }
    @Transactional(rollbackFor = Exception.class)
    @XxlJob("warehouseOutOrderCloseJob")
    public void warehouseOutOrderAutoClose() {
        log.debug("===== 开始执行【出库通知单自动关闭】定时任务 =====");
        List<WarehouseOutOrder> warehouseOutOrderList = warehouseOutOrderMapper.listPendingClose();
        if (warehouseOutOrderList.isEmpty()) {
            log.debug("无待关闭的出库通知单，任务执行完成");
        }else {
            for(WarehouseOutOrder warehouseOutOrder:warehouseOutOrderList){
                warehouseOutOrder.setState(3);
                warehouseOutOrder.setUpdateTime(LocalDateTime.now());
                warehouseOutOrderMapper.updateById(warehouseOutOrder);
                log.debug("出库通知单ID：{} 已自动关闭", warehouseOutOrder.getId());
            }
        }
        log.debug("=====出库通知单自动关闭任务执行完成=====");
    }
}
