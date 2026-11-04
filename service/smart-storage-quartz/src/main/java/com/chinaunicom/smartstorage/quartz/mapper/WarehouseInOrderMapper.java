package com.chinaunicom.smartstorage.quartz.mapper;



import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinaunicom.smartstorage.quartz.entity.WarehouseInOrder;

import java.util.List;


public interface WarehouseInOrderMapper extends BaseMapper<WarehouseInOrder> {
    List<WarehouseInOrder> listPendingClose();
}
