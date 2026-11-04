package com.chinaunicom.smartstorage.admin.controller.task;

import cn.hutool.core.convert.Convert;
import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.dto.task.sampleItem.*;
import com.google.common.collect.Lists;
import com.chinaunicom.smartstorage.admin.component.CodeGeneratorComponent;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.task.sampleItem.*;
import com.chinaunicom.smartstorage.admin.entity.task.SampleItem;
import com.chinaunicom.smartstorage.admin.service.inter.task.SampleItemService;
import com.chinaunicom.smartstorage.common.bean.BatchIdReqDTO;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.optlog.OptLog;
import com.chinaunicom.smartstorage.common.optlog.OptLogOperatorType;
import com.chinaunicom.smartstorage.common.page.QueryByPage;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
/**
 * @Description  扦样信息Controller
 * @author zhangy
 * @Date 2026-01-26
 */
@Tag(name = "扦样信息")
@Slf4j
@RestController
@RequestMapping("/sampleItem")
public class SampleItemController extends BaseController {
    @Resource
    private SampleItemService sampleItemService;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private CodeGeneratorComponent codeGeneratorComponent;
    @Resource
    private UidGenerator uidGenerator;
    @Operation(summary = "添加")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "扦样信息", operatorType = OptLogOperatorType.INSERT)
    @PostMapping(path = "/saveSampleItem", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> saveSampleItem(@RequestBody @Validated SampleItemSaveReqDTO sampleItemSaveReqDTO) {
        ActionResult<String, String> actionResult;
        SampleItem sampleItem = mapperFacade.map(sampleItemSaveReqDTO,SampleItem.class);
        sampleItem.setSampleNo(codeGeneratorComponent.getWorkFlowSampleCollection());
        sampleItem.setAddUser(jwtTokenComponent.getUser().getId());
        sampleItem.setAddTime(LocalDateTime.now());
        String id = Convert.toStr(uidGenerator.getUID());
        sampleItem.setId(id);
        actionResult = sampleItemService.saveSampleItem(sampleItem);
        return actionResult;
    }
    @Operation(summary = "列表查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",content = @Content(schema = @Schema(implementation = SampleItemQueryRespDTO.class))))
    @PostMapping(path = "/listSampleItemByConditionOnPage", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.task.sampleItem.SampleItemQueryRespDTO")
    public Object listSampleItemByConditionOnPage(@RequestBody @Validated SampleItemQueryReqDTO sampleItemQueryReqDTO) {
        sampleItemQueryReqDTO.setUserId(jwtTokenComponent.getUser().getId());
        sampleItemQueryReqDTO.setStartTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        sampleItemQueryReqDTO.setEndTime(sampleItemQueryReqDTO.getStartTime());
        return sampleItemService.listSampleItemByConditionOnPage(sampleItemQueryReqDTO);
    }

    @Operation(summary = "修改")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/updateSampleItem", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "扦样信息", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String> updateSampleItem(@RequestBody @Validated SampleItemUpdateDTO sampleItemUpdateDTO) {
        SampleItem sampleItem = mapperFacade.map(sampleItemUpdateDTO, SampleItem.class);
        sampleItem.setUpdateUser(jwtTokenComponent.getUser().getId());
        sampleItem.setUpdateTime(LocalDateTime.now());
        return sampleItemService.updateSampleItem(sampleItem);
    }

    @Operation(summary = "删除")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/deleteSampleItem", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "扦样信息", operatorType = OptLogOperatorType.DELETE)
    public ActionResult<String, String> deleteSampleItem(@RequestBody @Validated BatchIdReqDTO batchIdReqDTO) {
        List<SampleItem> listSampleItem = Lists.newArrayList();
        for (String id : batchIdReqDTO.getIds()) {
            SampleItem sampleItem = new SampleItem();
            sampleItem.setId(id);
            listSampleItem.add(sampleItem);
        }
        return sampleItemService.deleteSampleItem(listSampleItem);
    }

    @Operation(summary = "根据ID查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getSampleItemById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<SampleItemRespDTO, String> getSampleItemById(@PathVariable String id) {
        SampleItem sampleItem = sampleItemService.getSampleItemById(id);
        SampleItemRespDTO sampleItemRespDTO = mapperFacade.map(sampleItem, SampleItemRespDTO.class);
        return actionResultService.callBackResult(sampleItemRespDTO);
    }

    @Operation(summary = "根据taskId回显信息")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getSampleItemByTaskId/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<SampleItemRespDTO, String> getSampleItemByTaskId(@PathVariable String taskId) {
        SampleItem sampleItem = sampleItemService.getSampleItemByTaskId(taskId);
        SampleItemRespDTO sampleItemRespDTO = mapperFacade.map(sampleItem, SampleItemRespDTO.class);
        return actionResultService.callBackResult(sampleItemRespDTO);
    }
}
