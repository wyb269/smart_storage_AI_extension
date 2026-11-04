package com.chinaunicom.smartstorage.applet.service.inter.aggregation;

import java.util.List;

import com.chinaunicom.smartstorage.applet.dto.aggregation.ditc.DictTypeQueryReqDTO;
import com.chinaunicom.smartstorage.applet.entity.aggregation.dict.DictType;

/**
 * 字典
 *
 * @author : liuchink
 * @since 2022/6/9 15:06
 */
public interface DictionaryService {

    DictType getDictTypeByTypeWithDictData(String dictType);

    List<DictType> listDictTypeByConditionOnPage(DictTypeQueryReqDTO dictTypeQueryReqDTO);
}
