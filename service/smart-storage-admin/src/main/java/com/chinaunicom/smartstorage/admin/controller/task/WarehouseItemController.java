package com.chinaunicom.smartstorage.admin.controller.task;

import com.chinaunicom.smartstorage.admin.component.CodeGeneratorComponent;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.task.warehouseItem.*;
import com.chinaunicom.smartstorage.admin.dto.task.warehouseItem.*;
import com.chinaunicom.smartstorage.admin.entity.task.WarehouseItem;
import com.chinaunicom.smartstorage.admin.service.inter.task.WarehouseItemService;
import com.chinaunicom.smartstorage.common.bean.BatchIdReqDTO;
import com.chinaunicom.smartstorage.common.bean.PageQueryDTO;
import com.chinaunicom.smartstorage.common.optlog.OptLog;
import com.chinaunicom.smartstorage.common.optlog.OptLogOperatorType;
import com.chinaunicom.smartstorage.common.page.QueryByPage;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import io.swagger.v3.oas.annotations.Hidden;
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

/**
 * @Description  值仓控制器
 * @author zyp
 * @Date 2026-01-22
 */
@Tag(name = "值仓管理")
@RestController
@RequestMapping("/task/warehouseItem")
@Slf4j
public class WarehouseItemController extends BaseController {

	@Resource
	private WarehouseItemService warehouseItemService;
	@Resource
	private MapperFacade mapperFacade;
	@Resource
	private JwtTokenComponent jwtTokenComponent;
	@Resource
	private CodeGeneratorComponent codeGeneratorComponent;


	@Operation(summary = "添加")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
	@OptLog(title = "值仓管理", operatorType = OptLogOperatorType.INSERT)
	@PostMapping(path = "/saveWarehouseItem", produces = MediaType.APPLICATION_JSON_VALUE)
	public ActionResult<String, String> saveWarehouseItem(@RequestBody @Validated WarehouseItemSaveReqDTO warehouseItemSaveReqDTO) {
		ActionResult<String, String> actionResult;
		WarehouseItem warehouseItem = mapperFacade.map(warehouseItemSaveReqDTO,WarehouseItem.class);
		warehouseItem.setWarehouseNo(codeGeneratorComponent.getWorkFlowDataWarehouse());
		warehouseItem.setAddUser(jwtTokenComponent.getUser().getId());
		warehouseItem.setAddTime(LocalDateTime.now());
		actionResult = warehouseItemService.saveWarehouseItem(warehouseItem);
		return actionResult;
	}

	@Hidden
	@Operation(summary = "修改")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
	@OptLog(title = "值仓管理", operatorType = OptLogOperatorType.UPDATE)
	@PostMapping(path = "/updateWarehouseItem", produces = MediaType.APPLICATION_JSON_VALUE)
	public ActionResult<String, String> updateWarehouseItem( @RequestBody @Validated WarehouseItemUpdateReqDTO warehouseItemUpdateReqDTO) {
		return actionResultService.callBackResult("此功能暂末开放");
//		WarehouseItem warehouseItem = mapperFacade.map(warehouseItemUpdateReqDTO, WarehouseItem.class);
//		warehouseItem.setUpdateUser(jwtTokenComponent.getUser().getId());
//		warehouseItem.setUpdateTime(LocalDateTime.now());
//		return warehouseItemService.updateWarehouseItem(warehouseItem);
	}

	@Hidden
	@Operation(summary = "删除")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
	@OptLog(title ="值仓管理", operatorType = OptLogOperatorType.DELETE)
	@PostMapping(path = "/deleteWarehouseItem", produces = MediaType.APPLICATION_JSON_VALUE)
	public ActionResult<String, String> deleteWarehouseItem(@RequestBody @Validated BatchIdReqDTO batchIdReqDTO) {
		return actionResultService.callBackResult("此功能暂末开放");
//		List<WarehouseItem> listWarehouseItem = Lists.newArrayList();
//		for (String id : batchIdReqDTO.getIds()) {
//			WarehouseItem warehouseItem = new WarehouseItem();
//			warehouseItem.setId(id);
//			listWarehouseItem.add(warehouseItem);
//		}
//		return warehouseItemService.deleteWarehouseItem(listWarehouseItem);
	}

	@Hidden
	@Operation(summary = "根据ID获取信息")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
	@PostMapping(path = "/getWarehouseItemById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ActionResult<WarehouseItemRespDTO, String> getWarehouseItemById(@PathVariable("id") String id) {
		return actionResultService.callBackResult("此功能暂末开放");
//		WarehouseItem warehouseItem = warehouseItemService.getWarehouseItemById(id);
//		WarehouseItemRespDTO warehouseItemRespDTO = mapperFacade.map(warehouseItem, WarehouseItemRespDTO.class);
//		return actionResultService.callBackResult(warehouseItemRespDTO);
	}

	@Hidden
	@Operation(summary = "根据条件查询")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",content = @Content(schema = @Schema(implementation = WarehouseItemQueryRespDTO.class))))
	@PostMapping(path = "/listWarehouseItemByConditionOnPage", produces = MediaType.APPLICATION_JSON_VALUE)
	@QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.task.warehouseItem.WarehouseItemQueryRespDTO")
	public Object listWarehouseItemByConditionOnPage(@RequestBody @Validated WarehouseItemQueryReqDTO warehouseItemQueryReqDTO) {
		warehouseItemQueryReqDTO.setUserId(jwtTokenComponent.getUser().getId());
		warehouseItemQueryReqDTO.setStartTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
		warehouseItemQueryReqDTO.setEndTime(warehouseItemQueryReqDTO.getStartTime());
		return warehouseItemService.listWarehouseItemByConditionOnPage(warehouseItemQueryReqDTO);
	}

	@Operation(summary = "用户当天值仓列表")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",content = @Content(schema = @Schema(implementation = WarehouseItemQueryRespDTO.class))))
	@PostMapping(path = "/listWarehouseItemOnPage", produces = MediaType.APPLICATION_JSON_VALUE)
	@QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.task.warehouseItem.WarehouseItemQueryRespDTO")
	public Object listWarehouseItemOnPage(@RequestBody @Validated PageQueryDTO pageQueryDTO) {
		WarehouseItemQueryReqDTO warehouseItemQueryReqDTO=mapperFacade.map(pageQueryDTO,WarehouseItemQueryReqDTO.class);
		warehouseItemQueryReqDTO.setUserId(jwtTokenComponent.getUser().getId());
		warehouseItemQueryReqDTO.setStartTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
		warehouseItemQueryReqDTO.setEndTime(warehouseItemQueryReqDTO.getStartTime());
		return warehouseItemService.listWarehouseItemByConditionOnPage(warehouseItemQueryReqDTO);
	}

	@Operation(summary = "获取入库值仓回显信息")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
	@PostMapping(path = "/getTaskWarehouse/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ActionResult<TaskWarehouseRespDTO, String> getTaskWarehouse(@PathVariable("taskId") String taskId) {
		TaskWarehouseRespDTO taskWarehouseRespDTO= warehouseItemService.getTaskWarehouse(taskId);
		return actionResultService.callBackResult(taskWarehouseRespDTO);
	}

	@Operation(summary = "根据taskId获取值仓信息")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
	@PostMapping(path = "/getWarehouseItemByTaskId/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ActionResult<WarehouseItemRespDTO, String> getWarehouseItemByTaskId(@PathVariable("taskId") String taskId) {
		WarehouseItemRespDTO warehouseItemRespDTO = warehouseItemService.getWarehouseItemByTaskId(taskId);
		return actionResultService.callBackResult(warehouseItemRespDTO);
	}
	@Operation(summary = "获取出库值仓回显信息")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
	@PostMapping(path = "/getTaskWarehouseOut/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ActionResult<WarehouseOutRespDTO, String> getTaskWarehouseOut(@PathVariable("taskId") String taskId) {
		WarehouseOutRespDTO warehouseOutRespDTO= warehouseItemService.getTaskWarehouseOut(taskId);
		return actionResultService.callBackResult(warehouseOutRespDTO);
	}
}
