package com.chinaunicom.smartstorage.quartz.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinaunicom.smartstorage.quartz.entity.WarehouseOutOrder;

import java.util.List;


public interface WarehouseOutOrderMapper extends BaseMapper<WarehouseOutOrder> {
    List<WarehouseOutOrder> listPendingClose();

}
