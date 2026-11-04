package com.chinaunicom.smartstorage.admin.controller.task;

import com.chinaunicom.smartstorage.admin.dto.task.taskflow.*;
import com.chinaunicom.smartstorage.admin.dto.task.taskflow.TaskFlowStatisticsOutRespDTO;
import com.chinaunicom.smartstorage.admin.service.inter.task.TaskFlowService;
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
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "作业单管理")
@RestController
@RequestMapping("/aggregation/taskflow")
public class TaskFlowController extends BaseController {
    @Resource
    private TaskFlowService taskFlowService;
    @Operation(summary = "列表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = TaskFlowRespDTO.class))))
    @PostMapping(path = "/listTaskFlow", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.task.taskflow.TaskFlowRespDTO")
    @OptLog(title = "作业单管理 - 列表", operatorType = OptLogOperatorType.FIND)
    public Object listTaskFlow(@RequestBody @Validated
                               TaskFlowQueryReqDTO dto) {

        return taskFlowService.listTaskFlow(dto);
    }
    @Operation(summary = "列表 - 出库")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = TaskFlowRespDTO.class))))
    @PostMapping(path = "/listTaskFlowOut", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.task.taskflow.TaskFlowRespDTO")
    @OptLog(title = "作业单管理 - 列表 - 出库", operatorType = OptLogOperatorType.FIND)
    public Object listTaskFlowOut(@RequestBody @Validated
                               TaskFlowQueryReqDTO dto) {

        return taskFlowService.listTaskFlowOut(dto);
    }
    @Operation(summary = "查看")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/queryTaskFlow/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "作业单管理-查看", operatorType = OptLogOperatorType.FIND)
    public ActionResult<TaskFlowQueryRespDTO, String> queryTaskFlow(@PathVariable String id) {

        return taskFlowService.queryTaskFlow(id);
    }
    @Operation(summary = "查看-出库")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/queryTaskFlowOut/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "作业单管理-查看-出库", operatorType = OptLogOperatorType.FIND)
    public ActionResult<TaskFlowQueryRespDTO, String> queryTaskFlowOut(@PathVariable String id) {

        return taskFlowService.queryTaskFlowOut(id);
    }
    @Operation(summary = "流程数据")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "作业单管理-流程数据", operatorType = OptLogOperatorType.FIND)
    public ActionResult<TaskFlowStatisticsRespDTO, String> statistics() {

        return taskFlowService.statistics();
    }
    @Operation(summary = "流程数据-出库")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/statisticsOut", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "作业单管理-流程数据-出库", operatorType = OptLogOperatorType.FIND)
    public ActionResult<TaskFlowStatisticsOutRespDTO, String> statisticsOut() {

        return taskFlowService.statisticsOut();
    }
    @Operation(summary = "流程回退")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/retracement", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "作业单管理-流程回退", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String> retracement(@RequestBody @Validated TaskFlowRetracement taskFlowRetracement) {

        return taskFlowService.retracement(taskFlowRetracement);
    }
    @Operation(summary = "流程终止")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/terminated/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "作业单管理-流程终止", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String> terminated(@PathVariable String id) {

        return taskFlowService.terminated(id);
    }
}
