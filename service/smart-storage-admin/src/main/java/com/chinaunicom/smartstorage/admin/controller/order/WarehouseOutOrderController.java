package com.chinaunicom.smartstorage.admin.controller.order;

import cn.hutool.core.convert.Convert;
import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.order.*;
import com.chinaunicom.smartstorage.admin.dto.order.*;
import com.chinaunicom.smartstorage.admin.entity.order.WarehouseOutOrder;
import com.chinaunicom.smartstorage.admin.service.inter.order.WarehouseOutOrderService;
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

@Tag(name = "出库通知单")
@Slf4j
@RestController
@RequestMapping("/warehouseOutOrder")
public class WarehouseOutOrderController extends BaseController {
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private WarehouseOutOrderService warehouseOutOrderService;
    @Operation(summary = "添加")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "出库通知单", operatorType = OptLogOperatorType.INSERT)
    @PostMapping(path = "/saveWarehouseOutOrder", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> saveWarehouseOutOrder(@RequestBody @Validated WarehouseOutOrderSaveReqDTO warehouseOutOrderSaveReqDTO) {
        ActionResult<String, String> actionResult;
        WarehouseOutOrder warehouseOutOrder = mapperFacade.map(warehouseOutOrderSaveReqDTO, WarehouseOutOrder.class);
        warehouseOutOrder.setAddUser(jwtTokenComponent.getUser().getId());
        warehouseOutOrder.setAddTime(LocalDateTime.now());
        warehouseOutOrder.setId(Convert.toStr(uidGenerator.getUID()));
        actionResult = warehouseOutOrderService.saveWarehouseOutOrder(warehouseOutOrder);
        return actionResult;
    }
    @Operation(summary = "根据ID查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping("/getWarehouseOutOrderById/{id}")
    public ActionResult<WarehouseOutOrderRespDTO, String> getWarehouseOutOrderById(@PathVariable String id) {
        WarehouseOutOrderRespDTO warehouseOutOrderRespDTO = warehouseOutOrderService.getWarehouseOutOrderById(id);
        return actionResultService.callBackResult(warehouseOutOrderRespDTO);
    }

    @Operation(summary = "列表查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",content = @Content(schema = @Schema(implementation = WarehouseOutOrderQueryRespDTO.class))))
    @PostMapping(path = "/listWarehouseOutOrder", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.order.WarehouseOutOrderQueryRespDTO")
    public Object listWarehouseOutOrderByConditionOnPage(@RequestBody @Validated WarehouseOutOrderQueryReqDTO warehouseOutOrderQueryReqDTO) {
        return warehouseOutOrderService.listWarehouseOutOrderByConditionOnPage(warehouseOutOrderQueryReqDTO);
    }

    @Operation(summary = "关闭单据")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping("/close")
    @OptLog(title = "出库通知单", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String> close(@RequestBody @Validated WarehouseInOrderUpdateDTO warehouseOutOrderUpdateDTO) {
        WarehouseOutOrder warehouseOutOrder = mapperFacade.map(warehouseOutOrderUpdateDTO, WarehouseOutOrder.class);
        warehouseOutOrder.setUpdateUser(jwtTokenComponent.getUser().getId());
        warehouseOutOrder.setUpdateTime(LocalDateTime.now());
        return warehouseOutOrderService.updateWarehouseOutOrder(warehouseOutOrder);
    }
}
