package com.chinaunicom.smartstorage.applet.service.impl.aggregation;

import java.util.List;

import org.springframework.stereotype.Service;

import com.chinaunicom.smartstorage.applet.dto.aggregation.ditc.DictTypeQueryReqDTO;
import com.chinaunicom.smartstorage.applet.entity.aggregation.dict.DictType;
import com.chinaunicom.smartstorage.applet.mapper.aggregation.DictDataMapper;
import com.chinaunicom.smartstorage.applet.mapper.aggregation.DictTypeMapper;
import com.chinaunicom.smartstorage.applet.service.inter.aggregation.DictionaryService;

import jakarta.annotation.Resource;

/**
 * 字典
 *
 * @author : liuchink
 * @since 2022/6/9 15:09
 */
@Service
public class DictionaryServiceImpl implements DictionaryService {
    @Resource
    private DictTypeMapper dictTypeMapper;
    @Resource
    private DictDataMapper dictDataMapper;

    @Override
    public DictType getDictTypeByTypeWithDictData(String dictType) {
        return dictTypeMapper.getDictTypeByTypeWithDictData(dictType);
    }

    @Override
    public List<DictType> listDictTypeByConditionOnPage(DictTypeQueryReqDTO dictTypeQueryReqDTO) {
        return dictTypeMapper.listDictTypeByConditionOnPage(dictTypeQueryReqDTO);
    }
}
