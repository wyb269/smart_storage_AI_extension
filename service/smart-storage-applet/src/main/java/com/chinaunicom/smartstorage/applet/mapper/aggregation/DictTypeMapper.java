package com.chinaunicom.smartstorage.applet.mapper.aggregation;

import com.github.yulichang.base.MPJBaseMapper;
import com.chinaunicom.smartstorage.applet.dto.aggregation.ditc.DictTypeQueryReqDTO;
import com.chinaunicom.smartstorage.applet.entity.aggregation.dict.DictType;

import java.util.List;

/**
 * @author : liuchink
 * @since 2022/6/29 16:56
 */

public interface DictTypeMapper extends MPJBaseMapper<DictType> {

    /**
     *
     * 根据类型查询
     *
     * @param type type
     *
     * @return com.liuchink.chinkframe.cloud.aggregation.entity.dictionary.DictType
     * @author liuchink
     * @since 2022/6/29 16:58
     */
    DictType getDictTypeByTypeWithDictData(String type);

    List<DictType> listDictTypeByConditionOnPage(DictTypeQueryReqDTO dictTypeQueryReqDTO);
}
