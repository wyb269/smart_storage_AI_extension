package com.chinaunicom.smartstorage.applet.controller.aggregation;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinaunicom.smartstorage.applet.dto.aggregation.ditc.DictTypeRespDTO;
import com.chinaunicom.smartstorage.applet.entity.aggregation.dict.DictType;
import com.chinaunicom.smartstorage.applet.service.inter.aggregation.DictionaryService;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import ma.glasnost.orika.MapperFacade;

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

    @Resource
    private DictionaryService dictionaryService;

    @Resource
    private MapperFacade mapperFacade;

    /**
     *
     * 根据类型查询
     *
     * @param type type
     *
     * @author liuchink
     * @since 2022/6/29 16:58
     */
    @Operation(summary = "根据类型查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getDictTypeByTypeWithDictData/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<DictTypeRespDTO, String> getDictTypeByTypeWithDictData(@PathVariable String type) {
        DictType dictType = dictionaryService.getDictTypeByTypeWithDictData(type);
        DictTypeRespDTO dictTypeRespDTO = mapperFacade.map(dictType, DictTypeRespDTO.class);
        return actionResultService.callBackResult(dictTypeRespDTO);
    }

}
