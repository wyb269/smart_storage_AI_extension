package com.chinaunicom.smartstorage.admin.service.impl.aggregation;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ditc.DictDataQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ditc.DictTypeImportDataDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ditc.DictTypeQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.dict.DictData;
import com.chinaunicom.smartstorage.admin.entity.aggregation.dict.DictType;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.DictDataMapper;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.DictTypeMapper;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.DictionaryService;
import com.chinaunicom.smartstorage.common.enumbean.OptEnum;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.chinaunicom.smartstorage.common.result.QueryPageResult;

import cn.hutool.core.util.StrUtil;
import cn.idev.excel.EasyExcel;
import cn.idev.excel.read.listener.PageReadListener;
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
    @Resource
    private ActionResultService actionResultService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveDictData(DictData dictData) {
        dictDataMapper.insert(dictData);
        return actionResultService.callBackResult();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateDictData(DictData dictData) {
        dictDataMapper.updateById(dictData);
        return actionResultService.callBackResult();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteDictData(DictData dictData) {
        dictDataMapper.deleteById(dictData);
        return actionResultService.callBackResult();
    }

    @Override
    public List<DictData> listDictDataByConditon(DictDataQueryReqDTO dictDataQueryReqDTO) {
        return dictDataMapper.listDictDataByConditon(dictDataQueryReqDTO);
    }

    @Override
    public DictData getDictDataById(Long id) {
        return dictDataMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveDictType(DictType dictType) {
        ActionResult<String, String> actionResult = check(dictType, OptEnum.ADD);
        if (actionResult.getStatus().isSuccess()) {
            dictTypeMapper.insert(dictType);
            actionResult = actionResultService.callBackResult();
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateDictType(DictType dictType) {
        ActionResult<String, String> actionResult = check(dictType, OptEnum.UPDATE);
        if (actionResult.getStatus().isSuccess()) {
            dictTypeMapper.updateById(dictType);
            actionResult = actionResultService.callBackResult();
        }
        return actionResult;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteDictType(DictType dictType) {
        dictTypeMapper.delete(Wrappers.lambdaQuery(DictType.class).eq(DictType::getType, dictType.getType()));
        DictData dictData = new DictData();
        dictData.setType(dictType.getType());
        dictDataMapper.delete(Wrappers.lambdaQuery(DictData.class).eq(DictData::getType, dictData.getType()));
        return actionResultService.callBackResult();
    }

    @Override
    public List<DictType> listDictTypeByConditon(DictTypeQueryReqDTO dictTypeQueryReqDTO) {
        return dictTypeMapper.listDictTypeByConditon(dictTypeQueryReqDTO);
    }

    @Override
    public QueryPageResult<DictType> typeExportExcel(DictTypeQueryReqDTO dictTypeQueryReqDTO) {
        try {
            PageMethod.startPage(dictTypeQueryReqDTO.getPageNo(), dictTypeQueryReqDTO.getPageSize(),
                dictTypeQueryReqDTO.getPageTotal());
            List<DictType> listDictType = dictTypeMapper.listDictTypeByConditon(dictTypeQueryReqDTO);
            PageInfo<DictType> pageInfo = new PageInfo<>(listDictType);
            QueryPageResult<DictType> queryPageResult = new QueryPageResult<>();
            queryPageResult
                .setTotal(dictTypeQueryReqDTO.getPageTotal() ? pageInfo.getTotal() : (long)listDictType.size());
            queryPageResult.getData().addAll(listDictType);
            return queryPageResult;
        } catch (Exception exception) {
            throw new CustomException(exception);
        } finally {
            PageMethod.clearPage();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> typeImportExcel(MultipartFile multipartFile) {
        try(final InputStream inputStream = multipartFile.getInputStream()) {
            EasyExcel.read(inputStream, DictTypeImportDataDTO.class,
                new PageReadListener<DictTypeImportDataDTO>(dataList -> {
                    for (DictTypeImportDataDTO dictTypeImportData : dataList) {
                        final DictType dictType = dictTypeMapper.selectOne(
                            Wrappers.lambdaQuery(DictType.class).eq(DictType::getType, dictTypeImportData.getType()));
                        if (dictType == null) {
                            DictType dictTypeSave = new DictType();
                            dictTypeSave.setName(dictTypeImportData.getName());
                            dictTypeSave.setType(dictTypeImportData.getType());
                            dictTypeSave.setShowState(dictTypeImportData.getShowState());
                            dictTypeSave.setRemark(dictTypeImportData.getRemark());
                            dictTypeMapper.insert(dictTypeSave);
                        }
                    }
                })).sheet().doRead();

        } catch (IOException ioException) {
            throw new CustomException(ioException);
        }
        return actionResultService.callBackResult();
    }

    @Override
    public DictType getDictTypeByTypeWithDictData(String dictType) {
        return dictTypeMapper.getDictTypeByTypeWithDictData(dictType);
    }

    @Override
    public DictType getDictTypeById(Long id) {
        return dictTypeMapper.selectById(id);
    }

    @Override
    public List<DictType> listDictTypeByTypeWithDictDataByList(List<String> listDictType) {
        return dictTypeMapper.listDictTypeByTypeWithDictDataByList(listDictType);
    }

    /**
     * 重复项检查
     *
     * @param dictType User
     * @return com.liuchink.chinkframe.admin.common.result.ActionResult
     * @author liuchink
     * @since 2018/8/12 11:17
     */
    private ActionResult<String, String> check(DictType dictType, OptEnum optEnum) {
        DictType dictTypeByType =
            dictTypeMapper.selectOne(Wrappers.lambdaQuery(DictType.class).eq(DictType::getType, dictType.getType()));
        if (dictTypeByType != null && !StrUtil.equals(dictTypeByType.getType(), dictType.getType())) {
            return actionResultService.callBackResultFail("字典类型不能重复");
        }
        return actionResultService.callBackResult();
    }
}
