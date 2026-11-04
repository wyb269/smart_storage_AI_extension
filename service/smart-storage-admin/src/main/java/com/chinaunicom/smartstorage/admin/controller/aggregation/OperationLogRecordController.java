package com.chinaunicom.smartstorage.admin.controller.aggregation;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinaunicom.smartstorage.admin.dto.aggregation.optlog.OperationLogRecordQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.optlog.OperationLogRecordRespDTO;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.OperationLogRecordService;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.page.QueryByPage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import ma.glasnost.orika.MapperFacade;

/**
 * 字典
 *
 * @author : liuchink
 * @since 2022/6/9 15:17
 */
@Tag(name = "系统管理-操作日志")
@RestController
@RequestMapping("/aggregation/operationLogRecord")
public class OperationLogRecordController extends BaseController {

    @Resource
    private OperationLogRecordService operationLogRecordService;

    @Resource
    private MapperFacade mapperFacade;

    /**
     *
     * 查询
     *
     * @param operationLogRecordQueryReqDTO operationLogRecordQueryReqDTO
     *
     * @author liuchink
     * @since 2022/6/29 16:58
     */
    @Operation(summary = "根据条件查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = OperationLogRecordRespDTO.class))))
    @PostMapping(path = "/listOperationLogRecordByCondition", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.aggregation.optlog.OperationLogRecordRespDTO")
    public Object listOperationLogRecordByCondition(
        @RequestBody @Validated OperationLogRecordQueryReqDTO operationLogRecordQueryReqDTO) {
        return operationLogRecordService.listOperationLogRecordByCondition(operationLogRecordQueryReqDTO);
    }
}
