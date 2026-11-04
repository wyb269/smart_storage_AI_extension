package com.chinaunicom.smartstorage.admin.controller.aggregation;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.write.metadata.WriteSheet;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ditc.*;
import com.google.common.collect.Maps;
import com.chinaunicom.smartstorage.admin.dto.aggregation.ditc.*;
import com.chinaunicom.smartstorage.admin.entity.aggregation.dict.DictData;
import com.chinaunicom.smartstorage.admin.entity.aggregation.dict.DictType;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.DictTypeMapper;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.DictionaryService;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.optlog.OptLog;
import com.chinaunicom.smartstorage.common.optlog.OptLogOperatorType;
import com.chinaunicom.smartstorage.common.page.QueryByPage;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.QueryPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import ma.glasnost.orika.MapperFacade;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 字典
 *
 * @author : liuchink
 * @since 2022/6/9 15:17
 */
@Tag(name = "系统管理-数据字典")
@RestController
@RequestMapping("/aggregation/dictionary")
public class DictionaryController extends BaseController {
    private static final int EXCEL_PAGE_SIZE = 5000;
    @Resource
    private DictionaryService dictionaryService;
    @Resource
    private DictTypeMapper dictTypeMapper;
    @Resource
    private MapperFacade mapperFacade;

    @Operation(summary = "类型-添加")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/saveDictType", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "数据字典-数据", operatorType = OptLogOperatorType.INSERT)
    public ActionResult<String, String> saveDictType(@RequestBody @Validated DictTypeSaveReqDTO dictTypeSaveReqDTO) {
        DictType dictType = mapperFacade.map(dictTypeSaveReqDTO, DictType.class);
        return dictionaryService.saveDictType(dictType);
    }

    @Operation(summary = "类型-修改")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/updateDictType", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "数据字典-类型", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String>
        updateDictType(@RequestBody @Validated DictTypeUpdateReqDTO dictTypeUpdateReqDTO) {
        DictType dictType = mapperFacade.map(dictTypeUpdateReqDTO, DictType.class);
        return dictionaryService.updateDictType(dictType);
    }

    @Operation(summary = "类型-删除")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "数据字典-类型", operatorType = OptLogOperatorType.DELETE)
    @PostMapping(path = "/deleteDictType/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> deleteDictType(@PathVariable String type) {
        DictType dictType = new DictType();
        dictType.setType(type);
        return dictionaryService.deleteDictType(dictType);
    }

    @Operation(summary = "类型 - 根据条件查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = DictTypeRespDTO.class))))
    @PostMapping(path = "/listDictTypeByConditon", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.aggregation.ditc.DictTypeRespDTO")
    public Object listDictTypeByConditon(@RequestBody @Validated DictTypeQueryReqDTO dictTypeQueryReqDTO) {
        return dictionaryService.listDictTypeByConditon(dictTypeQueryReqDTO);
    }

    @Operation(summary = "类型-根据ID查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getDictTypeById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<DictTypeRespDTO, String> getDictTypeById(@PathVariable Long id) {
        final DictType dictTypeById = dictionaryService.getDictTypeById(id);
        DictTypeRespDTO dictTypeRespDTO = mapperFacade.map(dictTypeById, DictTypeRespDTO.class);
        return actionResultService.callBackResult(dictTypeRespDTO);
    }

    /**
     *
     * 根据类型查询
     *
     * @param type type
     *
     * @author liuchink
     * @since 2022/6/29 16:58
     */
    @Operation(summary = "类型-根据类型查询-单参数")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getDictTypeByTypeWithDictData/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<DictTypeRespDTO, String> getDictTypeByTypeWithDictData(@PathVariable String type) {
        DictType dictType = dictionaryService.getDictTypeByTypeWithDictData(type);
        DictTypeRespDTO dictTypeRespDTO = mapperFacade.map(dictType, DictTypeRespDTO.class);
        return actionResultService.callBackResult(dictTypeRespDTO);
    }

    @Operation(summary = "类型-根据类型查询-多参数")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/listDictTypeByTypeWithDictDataByList", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<Map<String, List<DictDataRespDTO>>, String>
        listDictTypeByTypeWithDictDataByList(@RequestBody List<String> listDictType) {
        Map<String, List<DictDataRespDTO>> map = Maps.newHashMap();
        final List<DictType> dictTypes = dictionaryService.listDictTypeByTypeWithDictDataByList(listDictType);
        List<DictTypeRespDTO> listDictTypeRespDTO = mapperFacade.mapAsList(dictTypes, DictTypeRespDTO.class);
        for (DictTypeRespDTO dictTypeRespDTO : listDictTypeRespDTO) {
            map.put(dictTypeRespDTO.getType(), dictTypeRespDTO.getListDictData());
        }
        return actionResultService.callBackResult(map);
    }

    @Operation(summary = "数据-添加")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/saveDictData", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "数据字典-数据", operatorType = OptLogOperatorType.INSERT)
    public ActionResult<String, String> saveDictData(@RequestBody @Validated DictDataSaveReqDTO dictDataSaveReqDTO) {
        DictData dictData = mapperFacade.map(dictDataSaveReqDTO, DictData.class);
        return dictionaryService.saveDictData(dictData);
    }

    @Operation(summary = "数据-修改")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/updateDictData", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "数据字典-数据", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String>
        updateDictData(@RequestBody @Validated DictDataUpdateReqDTO dictDataUpdateReqDTO) {
        DictData dictData = mapperFacade.map(dictDataUpdateReqDTO, DictData.class);
        return dictionaryService.updateDictData(dictData);
    }

    @Operation(summary = "数据-删除")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "数据字典-数据", operatorType = OptLogOperatorType.DELETE)
    @PostMapping(path = "/deleteDictData/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> deleteDictData(@PathVariable Long id) {
        DictData dictData = new DictData();
        dictData.setId(id);
        return dictionaryService.deleteDictData(dictData);
    }

    @Operation(summary = "数据-根据ID查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getDictDataById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<DictDataRespDTO, String> getDictDataById(@PathVariable Long id) {
        final DictData dictDataById = dictionaryService.getDictDataById(id);
        DictDataRespDTO dictDataRespDTO = mapperFacade.map(dictDataById, DictDataRespDTO.class);
        return actionResultService.callBackResult(dictDataRespDTO);
    }

    @Operation(summary = "数据 - 根据条件查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = DictDataRespDTO.class))))
    @PostMapping(path = "/listDictDataByConditon", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.aggregation.ditc.DictDataRespDTO")
    public Object listDictDataByConditon(@RequestBody @Validated DictDataQueryReqDTO dictDataQueryReqDTO) {
        return dictionaryService.listDictDataByConditon(dictDataQueryReqDTO);
    }

    @Operation(summary = "类型-字典类型导出")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/typeExportExcel", produces = MediaType.APPLICATION_JSON_VALUE)
    public void typeExportExcel(@RequestBody @Validated DictTypeQueryReqDTO dictTypeQueryReqDTO) {
        httpServletResponse.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        httpServletResponse.setCharacterEncoding("utf-8");
        try (ExcelWriter excelWriter =
            FastExcel.write(httpServletResponse.getOutputStream(), DictTypeExportDataDTO.class).build()) {
            String fileName = URLEncoder.encode("字典类型导出", StandardCharsets.UTF_8).replace("\\+", "%20");
            httpServletResponse.setHeader("Content-Disposition",
                "attachment; filename=\"" + fileName + ".xlsx\"; filename*=" + fileName + ".xlsx");
            WriteSheet writeSheet = FastExcel.writerSheet("数据导出").build();
            dictTypeQueryReqDTO.setPageSize(EXCEL_PAGE_SIZE);
            dictTypeQueryReqDTO.setPageNo(1);
            dictTypeQueryReqDTO.setPageTotal(true);
            QueryPageResult<DictType> dictTypeQueryPageResult = dictionaryService.typeExportExcel(dictTypeQueryReqDTO);
            List<DictTypeExportDataDTO> listDictTypeExportData =
                mapperFacade.mapAsList(dictTypeQueryPageResult.getData(), DictTypeExportDataDTO.class);
            excelWriter.write(listDictTypeExportData, writeSheet);
            long pageCount = dictTypeQueryPageResult.getTotal() / EXCEL_PAGE_SIZE;
            if (dictTypeQueryPageResult.getTotal() % EXCEL_PAGE_SIZE > 0) {
                pageCount++;
            }
            for (int i = 1; i < pageCount; i++) {
                dictTypeQueryReqDTO.setPageNo(i + 1);
                dictTypeQueryReqDTO.setPageTotal(false);
                dictTypeQueryPageResult = dictionaryService.typeExportExcel(dictTypeQueryReqDTO);
                listDictTypeExportData =
                    mapperFacade.mapAsList(dictTypeQueryPageResult.getData(), DictTypeExportDataDTO.class);
                excelWriter.write(listDictTypeExportData, writeSheet);
            }

        } catch (IOException ioException) {
            throw new CustomException(ioException);
        }
    }

    @Operation(summary = "类型-字典类型导入")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/typeImportExcel", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> typeImportExcel(@RequestPart("file") MultipartFile multipartFile) {
        return dictionaryService.typeImportExcel(multipartFile);
    }
}
