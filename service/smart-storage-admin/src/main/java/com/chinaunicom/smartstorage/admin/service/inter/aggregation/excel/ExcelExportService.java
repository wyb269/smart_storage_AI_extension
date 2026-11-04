package com.chinaunicom.smartstorage.admin.service.inter.aggregation.excel;

import com.chinaunicom.smartstorage.admin.dto.aggregation.excel.ExportDTO;

import java.util.List;

public interface ExcelExportService {
    List<?> getDataList(ExportDTO query);
}
