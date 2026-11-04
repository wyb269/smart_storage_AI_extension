package com.chinaunicom.smartstorage.admin.controller.task;

import cn.hutool.core.convert.Convert;
import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.dto.task.weight.*;
import com.google.common.collect.Lists;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.task.weight.*;
import com.chinaunicom.smartstorage.admin.entity.task.WeightItem;
import com.chinaunicom.smartstorage.admin.service.inter.task.WeightItemService;
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
import java.util.List;

/**
 * 称重信息表(WeightItem)表控制层
 *
 * @author liuchink
 * @since 2026-01-26 10:45:29
 */
@Tag(name = "检斤称重")
@Slf4j
@RestController
@RequestMapping("/weightItem")
public class WeightItemController extends BaseController {
    /**
     * 服务对象
     */
    @Resource
    private WeightItemService weightItemService;
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private JwtTokenComponent jwtTokenComponent;

    /**
     * 新增数据
     *
     * @param weightItemSaveReqDTO weightItemSaveReqDTO
     * @return 新增结果
     * @author liuchink
     */
    @Operation(summary = "添加")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/saveWeightItem", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "检斤称重", operatorType = OptLogOperatorType.INSERT)
    public ActionResult<String, String>
        saveWeightItem(@RequestBody @Validated WeightItemSaveReqDTO weightItemSaveReqDTO) {
        WeightItem weightItem = mapperFacade.map(weightItemSaveReqDTO, WeightItem.class);
        weightItem.setId(Convert.toStr(uidGenerator.getUID()));
        weightItem.setAddTime(LocalDateTime.now());
        weightItem.setAddUser(jwtTokenComponent.getUser().getId());
        return weightItemService.saveWeightItem(weightItem);
    }

    /**
     * 修改数据
     *
     * @param weightItemUpdateReqDTO weightItemUpdateReqDTO
     * @return 修改结果
     * @author liuchink
     */
    @Operation(summary = "修改")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/updateWeightItem", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "检斤称重", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String>
        updateWeightItem(@RequestBody @Validated WeightItemUpdateReqDTO weightItemUpdateReqDTO) {
        WeightItem weightItem = mapperFacade.map(weightItemUpdateReqDTO, WeightItem.class);
        weightItem.setUpdateUser(jwtTokenComponent.getUser().getId());
        weightItem.setUpdateTime(LocalDateTime.now());
        return weightItemService.updateWeightItem(weightItem);
    }

    /**
     * 删除数据
     *
     * @param batchIdReqDTO batchIdReqDTO
     * @return 删除结果
     * @author liuchink
     */
    @Operation(summary = "删除")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/deleteWeightItem", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "检斤称重", operatorType = OptLogOperatorType.DELETE)
    public ActionResult<String, String> deleteWeightItem(@RequestBody @Validated BatchIdReqDTO batchIdReqDTO) {
        List<WeightItem> listWeightItem = Lists.newArrayList();
        for (String id : batchIdReqDTO.getIds()) {
            WeightItem weightItem = new WeightItem();
            weightItem.setId(id);
            listWeightItem.add(weightItem);
        }
        return weightItemService.deleteWeightItem(listWeightItem);
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id id
     * @return 单条数据
     * @author liuchink
     */
    @Operation(summary = "根据ID查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getWeightItemById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<WeightItemRespDTO, String> getWeightItemById(@PathVariable String id) {
        WeightItem weightItemById = weightItemService.getWeightItemById(id);
        WeightItemRespDTO weightItemRespDTO = mapperFacade.map(weightItemById, WeightItemRespDTO.class);
        return actionResultService.callBackResult(weightItemRespDTO);
    }

    @Operation(summary = "根据ID查询详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getWeightItemByDetail/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<WeightItemRespDTO, String> getWeightItemByDetail(@PathVariable String id) {
        WeightItem weightItemById = weightItemService.getWeightItemByDetail(id);
        WeightItemRespDTO weightItemRespDTO = mapperFacade.map(weightItemById, WeightItemRespDTO.class);
        return actionResultService.callBackResult(weightItemRespDTO);
    }

    @Operation(summary = "根据任务ID查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getWeightItemByTaskId/{taskId}/{weightType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<WeightItemRespDTO, String> getWeightItemByTaskId(@PathVariable String taskId,
        @PathVariable int weightType) {
        WeightItem weightItemById = weightItemService.getWeightItemByTaskId(taskId, weightType);
        WeightItemRespDTO weightItemRespDTO = mapperFacade.map(weightItemById, WeightItemRespDTO.class);
        return actionResultService.callBackResult(weightItemRespDTO);
    }

    @Operation(summary = "查询入库表单提交详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getWeightItemByDetailForInForm/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<WeightItemInFormRespDTO, String> getWeightItemByDetailForInForm(@PathVariable String taskId) {
        WeightItemInFormRespDTO weightItemByDetailForInForm = weightItemService.getWeightItemByDetailForInForm(taskId);
        return actionResultService.callBackResult(weightItemByDetailForInForm);
    }

    @Operation(summary = "查询出库表单提交详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getWeightItemByDetailForOutForm/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<WeightItemOutFormRespDTO, String> getWeightItemByDetailForOutForm(@PathVariable String taskId) {
        WeightItemOutFormRespDTO weightItemByDetailForOutForm =
            weightItemService.getWeightItemByDetailForOutForm(taskId);
        return actionResultService.callBackResult(weightItemByDetailForOutForm);
    }

    /**
     * 根据查询条件查询
     *
     * @param weightItemQueryReqDTO weightItemQueryReqDTO
     * @author liuchink
     */
    @Operation(summary = "根据条件查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = WeightItemRespDTO.class))))
    @PostMapping(path = "/listWeightItemByCondition", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.task.weight.WeightItemRespDTO")
    public Object listWeightItemByCondition(@RequestBody @Validated WeightItemQueryReqDTO weightItemQueryReqDTO) {
        return weightItemService.listWeightItemByCondition(weightItemQueryReqDTO);
    }
}
