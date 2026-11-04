package com.chinaunicom.smartstorage.admin.controller.order;

import cn.hutool.core.convert.Convert;
import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.order.*;
import com.chinaunicom.smartstorage.admin.dto.order.*;
import com.chinaunicom.smartstorage.admin.entity.order.WarehouseInOrder;
import com.chinaunicom.smartstorage.admin.service.inter.order.WarehouseInOrderService;
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

@Tag(name = "入库通知单")
@Slf4j
@RestController
@RequestMapping("/warehouseInOrder")
public class WarehouseInOrderController extends BaseController {
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private WarehouseInOrderService warehouseInOrderService;
    @Operation(summary = "添加")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "入库通知单", operatorType = OptLogOperatorType.INSERT)
    @PostMapping(path = "/saveWarehouseInOrder", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> saveWarehouseInOrder(@RequestBody @Validated WarehouseInOrderSaveReqDTO warehouseInOrderSaveReqDTO) {
        ActionResult<String, String> actionResult;
        WarehouseInOrder warehouseInOrder = mapperFacade.map(warehouseInOrderSaveReqDTO, WarehouseInOrder.class);
        warehouseInOrder.setAddUser(jwtTokenComponent.getUser().getId());
        warehouseInOrder.setAddTime(LocalDateTime.now());
        warehouseInOrder.setId(Convert.toStr(uidGenerator.getUID()));
        actionResult = warehouseInOrderService.saveWarehouseInOrder(warehouseInOrder);
        return actionResult;
    }
    @Operation(summary = "根据ID查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping("/getWarehouseInOrderById/{id}")
    public ActionResult<WarehouseInOrderRespDTO, String> getWarehouseInOrderById(@PathVariable String id) {
        WarehouseInOrderRespDTO warehouseInOrderRespDTO = warehouseInOrderService.getWarehouseInOrderById(id);
        return actionResultService.callBackResult(warehouseInOrderRespDTO);
    }

    @Operation(summary = "列表查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",content = @Content(schema = @Schema(implementation = WarehouseInOrderQueryRespDTO.class))))
    @PostMapping(path = "/listWarehouseInOrder", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.order.WarehouseInOrderQueryRespDTO")
    public Object listWarehouseInOrderByConditionOnPage(@RequestBody @Validated WarehouseInOrderQueryReqDTO warehouseInOrderQueryReqDTO) {
        return warehouseInOrderService.listWarehouseInOrderByConditionOnPage(warehouseInOrderQueryReqDTO);
    }

    @Operation(summary = "关闭单据")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping("/close")
    @OptLog(title = "入库通知单", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String> close(@RequestBody @Validated WarehouseInOrderUpdateDTO warehouseInOrderUpdateDTO) {
        WarehouseInOrder warehouseInOrder = mapperFacade.map(warehouseInOrderUpdateDTO, WarehouseInOrder.class);
        warehouseInOrder.setUpdateUser(jwtTokenComponent.getUser().getId());
        warehouseInOrder.setUpdateTime(LocalDateTime.now());
        return warehouseInOrderService.updateWarehouseInOrder(warehouseInOrder);
    }

    @Operation(summary = "根据taskId查询粮食品种")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping("/getWarehouseInOrderByTaskId/{taskId}")
    public ActionResult<WarehouseInOrderRespDTO, String> getWarehouseInOrderByTaskId(@PathVariable String taskId) {
        WarehouseInOrderRespDTO warehouseInOrderRespDTO = warehouseInOrderService.selectByTaskId(taskId);
        return actionResultService.callBackResult(warehouseInOrderRespDTO);
    }
}
