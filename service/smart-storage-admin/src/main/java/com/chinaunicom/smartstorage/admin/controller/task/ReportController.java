package com.chinaunicom.smartstorage.admin.controller.task;

import com.chinaunicom.smartstorage.admin.dto.task.taskflow.*;
import com.chinaunicom.smartstorage.admin.service.inter.task.SettlementReportService;
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

@RestController
@RequestMapping("/Report")
@Tag(name = "报表查询")
public class ReportController extends BaseController {
    @Resource
    private SettlementReportService service;
    @Operation(summary = "结算报表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = SettlementReportRespDTO.class))))
    @PostMapping(path = "/settlementReport", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.task.taskflow.SettlementReportRespDTO")
    @OptLog(title = "结算报表", operatorType = OptLogOperatorType.FIND)
    public Object settlementReport(@RequestBody @Validated
                                   SettlementReportReqDTO dto) {

        return service.settlementReport(dto);
    }
    @Operation(summary = "结算报表详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/settlementReportQuery/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "结算报表详情", operatorType = OptLogOperatorType.FIND)
    public ActionResult<SettlementReportQueryRespDTO, String> settlementReportQuery(@PathVariable String id) {

        return service.settlementReportQuery(id);
    }
    @Operation(summary = "出入库报表")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
            content = @Content(schema = @Schema(implementation = RegistrationReportRespDTO.class))))
    @PostMapping(path = "/registrationReport", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.task.taskflow.RegistrationReportRespDTO")
    @OptLog(title = "出入库报表", operatorType = OptLogOperatorType.FIND)
    public Object registrationReport(@RequestBody @Validated
                                     RegistrationReportReqDTO dto) {

        return service.registrationReport(dto);
    }
}
