package com.chinaunicom.smartstorage.admin.service.impl.aggregation.excel;

import com.chinaunicom.smartstorage.admin.dto.aggregation.excel.ExportDTO;
import com.chinaunicom.smartstorage.admin.dto.task.taskflow.RegistrationReportReqDTO;
import com.chinaunicom.smartstorage.admin.dto.task.taskflow.SettlementReportReqDTO;
import com.chinaunicom.smartstorage.admin.mapper.task.taskflow.TaskFlowMapper;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.excel.ExcelExportService;
import jakarta.annotation.Resource;
import ma.glasnost.orika.MapperFacade;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.*;

@Service
public class ExcelExportServiceImpl implements ExcelExportService {
    @Resource
    private TaskFlowMapper taskFlowMapper;
    @Resource
    private MapperFacade mapperFacade;
    @Override
    public List<?> getDataList(ExportDTO query) {
        String bizType = query.getBizType();
        Map<String, Object> params = query.getParams();
        if (params == null) {
            params = new HashMap<>();
        }
        // 根据业务类型 → 调用对应的Mapper查询
        return switch (bizType) {
            case "settlementReport" ->{
                SettlementReportReqDTO reqDTO = mapperFacade.map(params, SettlementReportReqDTO.class);
                yield taskFlowMapper.settlementReport(reqDTO);
            }
            case "registrationReport" ->{
                RegistrationReportReqDTO reqDTO = mapperFacade.map(params, RegistrationReportReqDTO.class);
                yield taskFlowMapper.registrationReport(reqDTO);
            }
            default -> Collections.emptyList();
        };
    }
}