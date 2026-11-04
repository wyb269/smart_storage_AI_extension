package com.chinaunicom.smartstorage.admin.utils;

import com.chinaunicom.smartstorage.admin.config.ExcelExportConfig;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class UniversalExcelUtil {
    public static <T> void export(HttpServletResponse response,
                                  List<T> dataList,
                                  String bizType) throws Exception {

        ExcelExportConfig.ExportConfig exportConfig = ExcelExportConfig.config.get(bizType);
        Map<String, String> headers = exportConfig.headers;

        // =======================
        // 文件名：别名 + 当前年月日
        // =======================
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = exportConfig.aliasName + "_" + today;

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("数据");

        // 表头
        int cellIndex = 0;
        Row headRow = sheet.createRow(0);
        for (String name : headers.keySet()) {
            headRow.createCell(cellIndex++).setCellValue(name);
        }

        // 数据
        List<String> fieldList = headers.values().stream().toList();
        for (int i = 0; i < dataList.size(); i++) {
            Row dataRow = sheet.createRow(i + 1);
            T data = dataList.get(i);

            for (int j = 0; j < fieldList.size(); j++) {
                String fieldName = fieldList.get(j);
                Object value = getFieldValue(data, fieldName);
                dataRow.createCell(j).setCellValue(value == null ? "" : value.toString());
            }
        }

        // 下载
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName + ".xlsx", "UTF-8"));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        try (OutputStream os = response.getOutputStream()) {
            workbook.write(os);
        }
    }

    private static Object getFieldValue(Object obj, String field) throws Exception {
        java.lang.reflect.Field f = obj.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return f.get(obj);
    }
}
