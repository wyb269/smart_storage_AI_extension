package com.chinaunicom.smartstorage.admin.service.inter.aggregation;

import com.chinaunicom.smartstorage.admin.dto.aggregation.ditc.DictDataQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ditc.DictTypeQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.dict.DictData;
import com.chinaunicom.smartstorage.admin.entity.aggregation.dict.DictType;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.QueryPageResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 字典
 *
 * @author : liuchink
 * @since 2022/6/9 15:06
 */
public interface DictionaryService {

    ActionResult<String, String> saveDictData(DictData dictData);

    ActionResult<String, String> updateDictData(DictData dictData);

    ActionResult<String, String> deleteDictData(DictData dictData);

    List<DictData> listDictDataByConditon(DictDataQueryReqDTO dictDataQueryReqDTO);

    DictData getDictDataById(Long id);

    ActionResult<String, String> saveDictType(DictType dictType);

    ActionResult<String, String> updateDictType(DictType dictType);

    ActionResult<String, String> deleteDictType(DictType dictType);

    List<DictType> listDictTypeByConditon(DictTypeQueryReqDTO dictTypeQueryReqDTO);

    QueryPageResult<DictType> typeExportExcel(DictTypeQueryReqDTO dictTypeQueryReqDTO);

    ActionResult<String, String> typeImportExcel(MultipartFile multipartFile);

    DictType getDictTypeByTypeWithDictData(String dictType);

    DictType getDictTypeById(Long id);

    List<DictType> listDictTypeByTypeWithDictDataByList(List<String> listDictType);
}
