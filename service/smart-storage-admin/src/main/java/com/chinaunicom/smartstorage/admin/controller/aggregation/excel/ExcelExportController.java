package com.chinaunicom.smartstorage.admin.controller.aggregation.excel;

import com.chinaunicom.smartstorage.admin.dto.aggregation.excel.ExportDTO;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.excel.ExcelExportService;
import com.chinaunicom.smartstorage.admin.utils.UniversalExcelUtil;
import com.chinaunicom.smartstorage.common.optlog.OptLog;
import com.chinaunicom.smartstorage.common.optlog.OptLogOperatorType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/excelExport")
@Tag(name = "导出")
public class ExcelExportController {
    @Resource
    private ExcelExportService dataService;

    @PostMapping("/export")
    @Operation(summary = "导出")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "导出", operatorType = OptLogOperatorType.EXPORT)
    public void export(@RequestBody @Validated ExportDTO query, HttpServletResponse response) throws Exception {
        // 1. 自动查数据
        List<?> data = dataService.getDataList(query);
        // 2. 自动导出
        UniversalExcelUtil.export(response, data, query.getBizType());
    }
}
