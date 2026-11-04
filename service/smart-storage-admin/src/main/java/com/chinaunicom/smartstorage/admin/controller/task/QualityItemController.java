package com.chinaunicom.smartstorage.admin.controller.task;

import cn.hutool.core.convert.Convert;
import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.dto.task.qualityItem.*;
import com.google.common.collect.Lists;
import com.chinaunicom.smartstorage.admin.component.CodeGeneratorComponent;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.task.qualityItem.*;
import com.chinaunicom.smartstorage.admin.entity.task.QualityItem;
import com.chinaunicom.smartstorage.admin.service.inter.task.QualityItemService;
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
 * @Description  质量检验Controller
 * @author zhangy
 * @Date 2026-01-29
 */
@Tag(name = "质量检验")
@Slf4j
@RestController
@RequestMapping("/qualityItem")
public class QualityItemController extends BaseController {
    @Resource
    private QualityItemService qualityItemService;
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
    @OptLog(title = "质量检验", operatorType = OptLogOperatorType.INSERT)
    @PostMapping(path = "/saveQualityItem", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> saveQualityItem(@RequestBody @Validated QualityItemSaveReqDTO qualityItemSaveReqDTO) {
        ActionResult<String, String> actionResult;
        QualityItem qualityItem = mapperFacade.map(qualityItemSaveReqDTO,QualityItem.class);
        qualityItem.setQualityNo(codeGeneratorComponent.getWorkFlowQualityCheck());
        qualityItem.setAddUser(jwtTokenComponent.getUser().getId());
        qualityItem.setAddTime(LocalDateTime.now());
        String id = Convert.toStr(uidGenerator.getUID());
        qualityItem.setId(id);
        actionResult = qualityItemService.saveQualityItem(qualityItem);
        return actionResult;
    }

    @Operation(summary = "列表查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",content = @Content(schema = @Schema(implementation = QualityItemQueryRespDTO.class))))
    @PostMapping(path = "/listQualityItemByConditionOnPage", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.task.qualityItem.QualityItemQueryRespDTO")
    public Object listQualityItemByConditionOnPage(@RequestBody @Validated QualityItemQueryReqDTO qualityItemQueryReqDTO) {
        qualityItemQueryReqDTO.setUserId(jwtTokenComponent.getUser().getId());
        qualityItemQueryReqDTO.setStartTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        qualityItemQueryReqDTO.setEndTime(qualityItemQueryReqDTO.getStartTime());
        return qualityItemService.listQualityItemByConditionOnPage(qualityItemQueryReqDTO);
    }

    @Operation(summary = "修改")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/updateQualityItem", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "质量检验", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String> updateQualityItem(@RequestBody @Validated QualityItemUpdateDTO qualityItemUpdateDTO) {
        QualityItem qualityItem = mapperFacade.map(qualityItemUpdateDTO, QualityItem.class);
        qualityItem.setUpdateUser(jwtTokenComponent.getUser().getId());
        qualityItem.setUpdateTime(LocalDateTime.now());
        return qualityItemService.updateQualityItem(qualityItem);
    }

    @Operation(summary = "删除")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/deleteQualityItem", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "质量检验", operatorType = OptLogOperatorType.DELETE)
    public ActionResult<String, String> deleteQualityItem(@RequestBody @Validated BatchIdReqDTO batchIdReqDTO) {
        List<QualityItem> listQualityItem = Lists.newArrayList();
        for (String id : batchIdReqDTO.getIds()) {
            QualityItem qualityItem = new QualityItem();
            qualityItem.setId(id);
            listQualityItem.add(qualityItem);
        }
        return qualityItemService.deleteQualityItem(listQualityItem);
    }

    @Operation(summary = "根据ID查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getQualityItemById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<QualityItemRespDTO, String> getQualityItemById(@PathVariable String id) {
        QualityItem qualityItem = qualityItemService.getQualityItemById(id);
        QualityItemRespDTO qualityItemRespDTO = mapperFacade.map(qualityItem, QualityItemRespDTO.class);
        return actionResultService.callBackResult(qualityItemRespDTO);
    }

    @Operation(summary = "根据taskId回显信息")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getQualityItemByTaskId/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<QualityItemRespDTO, String> getQualityItemByTaskId(@PathVariable String taskId) {
        QualityItem qualityItem = qualityItemService.getQualityItemByTaskId(taskId);
        QualityItemRespDTO qualityItemRespDTO = mapperFacade.map(qualityItem, QualityItemRespDTO.class);
        return actionResultService.callBackResult(qualityItemRespDTO);
    }
}
