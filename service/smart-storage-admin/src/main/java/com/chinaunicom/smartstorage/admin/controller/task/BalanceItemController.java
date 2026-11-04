package com.chinaunicom.smartstorage.admin.controller.task;

import com.chinaunicom.smartstorage.admin.component.CodeGeneratorComponent;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.task.balanceItem.*;
import com.chinaunicom.smartstorage.admin.dto.task.balanceItem.*;
import com.chinaunicom.smartstorage.admin.entity.task.BalanceItem;
import com.chinaunicom.smartstorage.admin.service.inter.task.BalanceItemService;
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
 * @Description  单车结算控制器
 * @author zyp
 * @Date 2026-01-22
 */
@Tag(name = "单车结算")
@RestController
@RequestMapping("/task/balanceItem")
@Slf4j
public class BalanceItemController extends BaseController {

	@Resource
	private BalanceItemService balanceItemService;
	@Resource
	private MapperFacade mapperFacade;
	@Resource
	private JwtTokenComponent jwtTokenComponent;
	@Resource
	private CodeGeneratorComponent codeGeneratorComponent;

	@Operation(summary = "入库添加")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
	@OptLog(title = "入库单车结算", operatorType = OptLogOperatorType.INSERT)
	@PostMapping(path = "/saveBalanceItem", produces = MediaType.APPLICATION_JSON_VALUE)
	public ActionResult<String, String> saveBalanceItem(@RequestBody @Validated BalanceItemSaveReqDTO balanceItemSaveReqDTO) {
		ActionResult<String, String> actionResult;
		BalanceItem balanceItem = mapperFacade.map(balanceItemSaveReqDTO,BalanceItem.class);
		balanceItem.setBalanceNo(codeGeneratorComponent.getWorkFlowSettlement());
		balanceItem.setAddUser(jwtTokenComponent.getUser().getId());
		balanceItem.setAddTime(LocalDateTime.now());
		actionResult = balanceItemService.saveBalanceItem(balanceItem);
		return actionResult;
	}

	@Hidden
	@Operation(summary = "修改")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
	@OptLog(title = "单车结算", operatorType = OptLogOperatorType.UPDATE)
	@PostMapping(path = "/updateBalanceItem", produces = MediaType.APPLICATION_JSON_VALUE)
	public ActionResult<String, String> updateBalanceItem( @RequestBody @Validated BalanceItemUpdateReqDTO balanceItemUpdateReqDTO) {
		return actionResultService.callBackResult("此功能暂末开放");
//		BalanceItem balanceItem = mapperFacade.map(balanceItemUpdateReqDTO, BalanceItem.class);
//		balanceItem.setUpdateUser(jwtTokenComponent.getUser().getId());
//		balanceItem.setUpdateTime(LocalDateTime.now());
//		return balanceItemService.updateBalanceItem(balanceItem);
	}

	@Hidden
	@Operation(summary = "删除")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
	@OptLog(title ="单车结算", operatorType = OptLogOperatorType.DELETE)
	@PostMapping(path = "/deleteBalanceItem", produces = MediaType.APPLICATION_JSON_VALUE)
	public ActionResult<String, String> deleteBalanceItem(@RequestBody @Validated BatchIdReqDTO batchIdReqDTO) {
		return actionResultService.callBackResult("此功能暂末开放");
//		List<BalanceItem> listBalanceItem = Lists.newArrayList();
//		for (String id : batchIdReqDTO.getIds()) {
//			BalanceItem balanceItem = new BalanceItem();
//			balanceItem.setId(id);
//			listBalanceItem.add(balanceItem);
//		}
//		return balanceItemService.deleteBalanceItem(listBalanceItem);
	}

	@Hidden
	@Operation(summary = "根据ID获取信息")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
	@PostMapping(path = "/getBalanceItemById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ActionResult<BalanceItemRespDTO, String> getBalanceItemById(@PathVariable("id") String id) {
		return actionResultService.callBackResult("此功能暂末开放");
//		BalanceItem balanceItem = balanceItemService.getBalanceItemById(id);
//		BalanceItemRespDTO balanceItemRespDTO = mapperFacade.map(balanceItem, BalanceItemRespDTO.class);
//		return actionResultService.callBackResult(balanceItemRespDTO);
	}

	@Hidden
	@Operation(summary = "根据条件查询")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",content = @Content(schema = @Schema(implementation = BalanceItemQueryRespDTO.class))))
	@PostMapping(path = "/listBalanceItemByConditionOnPage", produces = MediaType.APPLICATION_JSON_VALUE)
	@QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.task.balanceItem.BalanceItemQueryRespDTO")
	public Object listBalanceItemByConditionOnPage(@RequestBody @Validated BalanceItemQueryReqDTO balanceItemQueryReqDTO) {
		balanceItemQueryReqDTO.setUserId(jwtTokenComponent.getUser().getId());
		balanceItemQueryReqDTO.setStartTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
		balanceItemQueryReqDTO.setEndTime(balanceItemQueryReqDTO.getStartTime());
		return balanceItemService.listBalanceItemByConditionOnPage(balanceItemQueryReqDTO);
	}

	@Operation(summary = "用户当天结算列表")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",content = @Content(schema = @Schema(implementation = BalanceItemQueryRespDTO.class))))
	@PostMapping(path = "/listBalanceItemOnPage", produces = MediaType.APPLICATION_JSON_VALUE)
	@QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.task.balanceItem.BalanceItemQueryRespDTO")
	public Object listBalanceItemOnPage(@RequestBody @Validated PageQueryDTO pageQueryDTO) {
		BalanceItemQueryReqDTO balanceItemQueryReqDTO=mapperFacade.map(pageQueryDTO,BalanceItemQueryReqDTO.class);
		balanceItemQueryReqDTO.setUserId(jwtTokenComponent.getUser().getId());
		balanceItemQueryReqDTO.setStartTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
		balanceItemQueryReqDTO.setEndTime(balanceItemQueryReqDTO.getStartTime());
		return balanceItemService.listBalanceItemByConditionOnPage(balanceItemQueryReqDTO);
	}

	@Operation(summary = "获取结算回显信息")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
	@PostMapping(path = "/getTaskBalance/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ActionResult<TaskBalanceRespDTO, String> getTaskBalance(@PathVariable("taskId") String taskId) {
		TaskBalanceRespDTO taskBalanceRespDTO= balanceItemService.getTaskBalance(taskId);
		return actionResultService.callBackResult(taskBalanceRespDTO);
	}

	@Operation(summary = "根据taskId获取结算信息")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
	@PostMapping(path = "/getBalanceItemByTaskId/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ActionResult<BalanceItemRespDTO, String> getBalanceItemByTaskId(@PathVariable("taskId") String taskId) {
		BalanceItem balanceItem= balanceItemService.getBalanceItemByTaskId(taskId);
		BalanceItemRespDTO balanceItemRespDTO=mapperFacade.map(balanceItem,BalanceItemRespDTO.class);
		return actionResultService.callBackResult(balanceItemRespDTO);
	}

	@Operation(summary = "出库添加")
	@ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
	@OptLog(title = "出库单车结算", operatorType = OptLogOperatorType.INSERT)
	@PostMapping(path = "/saveBalanceItemOut", produces = MediaType.APPLICATION_JSON_VALUE)
	public ActionResult<String, String> saveBalanceItemOut(@RequestBody @Validated BalanceItemSaveReqDTO balanceItemSaveReqDTO) {
		ActionResult<String, String> actionResult;
		BalanceItem balanceItem = mapperFacade.map(balanceItemSaveReqDTO,BalanceItem.class);
		balanceItem.setBalanceNo(codeGeneratorComponent.getWorkFlowSettlement());
		balanceItem.setAddUser(jwtTokenComponent.getUser().getId());
		balanceItem.setAddTime(LocalDateTime.now());
		actionResult = balanceItemService.saveBalanceItemOut(balanceItem);
		return actionResult;
	}
}
