package com.chinaunicom.smartstorage.admin.mapper.aggregation;

import com.github.yulichang.base.MPJBaseMapper;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ditc.DictDataQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.dict.DictData;

import java.util.List;

/**
 * @author : liuchink
 * @since 2022/6/29 16:56
 */

public interface DictDataMapper extends MPJBaseMapper<DictData> {
    List<DictData> listDictDataByConditon(DictDataQueryReqDTO dictDataQueryReqDTO);
}
