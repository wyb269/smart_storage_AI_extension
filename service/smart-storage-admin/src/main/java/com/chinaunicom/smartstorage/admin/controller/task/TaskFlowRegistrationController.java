package com.chinaunicom.smartstorage.admin.controller.task;

import com.chinaunicom.smartstorage.admin.dto.task.registration.TaskFlowRegistrationListReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.registration.TaskFlowRegistrationListRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.registration.TaskFlowRegistrationSaveReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.registration.out.TaskFlowOutQueryRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.registration.out.TaskFlowOutRespDTO;
import com.chinaunicom.smartstorage.admin.dto.task.registration.out.TaskFlowOutSaveDTO;
import com.chinaunicom.smartstorage.admin.entity.task.TaskFlowRegistration;
import com.chinaunicom.smartstorage.admin.entity.task.TaskFlow;
import com.chinaunicom.smartstorage.admin.service.inter.task.TaskFlowRegistrationService;
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
import ma.glasnost.orika.MapperFacade;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "登记信息")
@RestController
@RequestMapping("/aggregation/registration")
public class TaskFlowRegistrationController extends BaseController {
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private TaskFlowRegistrationService taskFlowRegistrationService;

    @Operation(summary = "登记")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/saveRegistration", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "登记信息-登记", operatorType = OptLogOperatorType.INSERT)
    public ActionResult<String, String> saveRegistration(@RequestBody @Validated
                                                             TaskFlowRegistrationSaveReqDTO taskFlowRegistrationSaveReqDTO) {
        TaskFlow taskFlow = mapperFacade.map(taskFlowRegistrationSaveReqDTO,TaskFlow.class);
        TaskFlowRegistration taskFlowRegistration = mapperFacade.map(taskFlowRegistrationSaveReqDTO,TaskFlowRegistration.class);
        return taskFlowRegistrationService.saveRegistration(taskFlow,taskFlowRegistration);
    }
    @Operation(summary = "登记列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = TaskFlowRegistrationListRespDTO.class))))
    @PostMapping(path = "/listRegistration", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.task.registration.TaskFlowRegistrationListRespDTO")
    @OptLog(title = "登记 - 列表", operatorType = OptLogOperatorType.FIND)
    public Object listRegistration(@RequestBody @Validated
                                   TaskFlowRegistrationListReqDTO taskFlowRegistrationListReqDTO) {

        return taskFlowRegistrationService.listRegistration(taskFlowRegistrationListReqDTO);
    }
    @Operation(summary = "登出列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = TaskFlowRegistrationListRespDTO.class))))
    @PostMapping(path = "/listLogout", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.task.registration.TaskFlowRegistrationListRespDTO")
    @OptLog(title = "登出 - 列表", operatorType = OptLogOperatorType.FIND)
    public Object listLogout(@RequestBody @Validated
                                   TaskFlowRegistrationListReqDTO taskFlowRegistrationListReqDTO) {

        return taskFlowRegistrationService.listLogout(taskFlowRegistrationListReqDTO);
    }
    @Operation(summary = "登出")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/outRegistration", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "登记信息-登出", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String> outRegistration(@RequestBody @Validated
                                                        TaskFlowOutSaveDTO taskFlowOutSaveDTO) {

        return taskFlowRegistrationService.outRegistration(taskFlowOutSaveDTO);
    }
    @Operation(summary = "登记信息")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/queryRegistration/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "登记信息-登记信息", operatorType = OptLogOperatorType.FIND)
    public ActionResult<TaskFlowOutRespDTO, String> queryRegistration(@PathVariable String id) {

        return taskFlowRegistrationService.queryRegistration(id);
    }
    @Operation(summary = "登出信息")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/queryRegistrationOut/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "登记信息-登出信息", operatorType = OptLogOperatorType.FIND)
    public ActionResult<TaskFlowOutQueryRespDTO, String> queryRegistrationOut(@PathVariable String id) {

        return taskFlowRegistrationService.queryRegistrationOut(id);
    }
    @Operation(summary = "出库登记")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/saveRegistrationOut", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "登记信息-出库登记", operatorType = OptLogOperatorType.INSERT)
    public ActionResult<String, String> saveRegistrationOut(@RequestBody @Validated
                                                         TaskFlowRegistrationSaveReqDTO taskFlowRegistrationSaveReqDTO) {
        TaskFlow taskFlow = mapperFacade.map(taskFlowRegistrationSaveReqDTO,TaskFlow.class);
        TaskFlowRegistration taskFlowRegistration = mapperFacade.map(taskFlowRegistrationSaveReqDTO,TaskFlowRegistration.class);
        return taskFlowRegistrationService.saveRegistrationOut(taskFlow,taskFlowRegistration);
    }
}
