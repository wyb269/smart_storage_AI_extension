package com.chinaunicom.smartstorage.admin.controller.aggregation;

import cn.hutool.core.convert.Convert;
import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse.*;
import com.google.common.collect.Lists;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse.*;
import com.chinaunicom.smartstorage.admin.entity.aggregation.StsWarehouse;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.WarehouseService;
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

@Tag(name = "仓库管理")
@Slf4j
@RestController
@RequestMapping("/warehouse")
public class WarehouseController extends BaseController {
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private WarehouseService warehouseService;
    @Operation(summary = "添加")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "仓库管理", operatorType = OptLogOperatorType.INSERT)
    @PostMapping(path = "/saveWarehouse", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> saveWarehouse(@RequestBody @Validated WarehouseSaveReqDTO warehouseSaveReqDTO) {
        ActionResult<String, String> actionResult;
        StsWarehouse warehouse = mapperFacade.map(warehouseSaveReqDTO, StsWarehouse.class);
        warehouse.setAddUser(jwtTokenComponent.getUser().getId());
        warehouse.setAddTime(LocalDateTime.now());
        warehouse.setId(Convert.toStr(uidGenerator.getUID()));
        actionResult = warehouseService.saveWarehouse(warehouse);
        return actionResult;
    }
    @Operation(summary = "修改")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/updateWarehouse", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "仓库管理", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String> updateWarehouse(@RequestBody @Validated WarehouseUpdateDTO warehouseUpdateDTO) {
        StsWarehouse warehouse = mapperFacade.map(warehouseUpdateDTO, StsWarehouse.class);
        warehouse.setUpdateUser(jwtTokenComponent.getUser().getId());
        warehouse.setUpdateTime(LocalDateTime.now());
        return warehouseService.updateWarehouse(warehouse);
    }
    @Operation(summary = "删除")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/deleteWarehouse", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "仓库管理", operatorType = OptLogOperatorType.DELETE)
    public ActionResult<String, String> deleteWarehouse(@RequestBody @Validated BatchIdReqDTO batchIdReqDTO) {
        List<StsWarehouse> warehouseList = Lists.newArrayList();
        for (String id : batchIdReqDTO.getIds()) {
            StsWarehouse warehouse = new StsWarehouse();
            warehouse.setId(id);
            warehouseList.add(warehouse);
        }
        return warehouseService.deleteWarehouse(warehouseList);
    }
    @Operation(summary = "根据ID查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getWarehouseById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<WarehouseRespDTO, String> getWarehouseById(@PathVariable String id) {
        WarehouseRespDTO warehouseRespDTO = warehouseService.getWarehouseById(id);
        return actionResultService.callBackResult(warehouseRespDTO);
    }

    @Operation(summary = "列表查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",content = @Content(schema = @Schema(implementation = WarehouseQueryRespDTO.class))))
    @PostMapping(path = "/listWarehouseByConditionOnPage", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse.WarehouseQueryRespDTO")
    public Object listWarehouseByConditionOnPage(@RequestBody @Validated WarehouseQueryReqDTO warehouseQueryReqDTO) {
        return warehouseService.listWarehouseByCondition(warehouseQueryReqDTO);
    }

    @Operation(summary = "质检 - 选择仓库弹窗")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",content = @Content(schema = @Schema(implementation = WarehouseRespDTO.class))))
    @PostMapping(path = "/listWarehouseByModal", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.aggregation.warehouse.WarehouseRespDTO")
    public Object listWarehouseByModal(@RequestBody @Validated WarehouseModalReqDTO warehouseModalReqDTO) {
        return warehouseService.selectByGrainVariety(warehouseModalReqDTO);
    }
}
