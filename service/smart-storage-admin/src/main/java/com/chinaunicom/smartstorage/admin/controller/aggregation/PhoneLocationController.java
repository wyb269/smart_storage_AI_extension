package com.chinaunicom.smartstorage.admin.controller.aggregation;

import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.write.metadata.WriteSheet;
import com.chinaunicom.smartstorage.admin.dto.aggregation.phone.PhoneLocationExportData;
import com.chinaunicom.smartstorage.admin.dto.aggregation.phone.PhoneLocationQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.phone.PhoneLocationRespDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.PhoneLocation;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.PhoneLocationService;
import com.chinaunicom.smartstorage.common.constant.SystemConstant;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.QueryPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 号码归属地
 *
 * @author liuchink
 * @since 2022/1/30 9:53
 */
@Tag(name = "系统管理-手机归属地")
@RestController
@RequestMapping("/aggregation/phoneLocation")
@Slf4j
public class PhoneLocationController extends BaseController {
    private static final int EXCEL_PAGE_SIZE = 50000;
    @Resource
    private PhoneLocationService phoneLocationService;
    @Resource
    private MapperFacade mapperFacade;

    /**
     * 查询
     *
     * @param phonePrefix phonePrefix
     * @author liuchink
     * @since 2022/2/16 9:30
     */
    @Operation(summary = "根据号码查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getPhoneLocation/{phonePrefix}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<PhoneLocationRespDTO, String> getPhoneLocation(

        @PathVariable String phonePrefix) {
        PhoneLocation phoneLocation = phoneLocationService.getPhoneLocation(phonePrefix);
        PhoneLocationRespDTO phoneLocationRespDTO = mapperFacade.map(phoneLocation, PhoneLocationRespDTO.class);
        return actionResultService.callBackResult(phoneLocationRespDTO);
    }

    /**
     * 查询
     *
     * @param phoneLocationQueryReqDTO phoneLocationQueryReqDTO
     * @author liuchink
     * @since 2022/2/16 9:30
     */
    // @PostMapping(path = "/listPhoneLocationByCondition", produces = MediaType.APPLICATION_JSON_VALUE)
    // public ActionResult<QueryPageResult<PhoneLocationRespDTO>, String>
    // listPhoneLocationByCondition(@RequestBody @Validated PhoneLocationQueryReqDTO phoneLocationQueryReqDTO) {
    // QueryPageResult<PhoneLocation> queryPageResult =
    // phoneLocationService.listPhoneLocationByCondition(phoneLocationQueryReqDTO);
    // List<PhoneLocationRespDTO> listPhoneLocationRespDTO =
    // mapperFacade.mapAsList(queryPageResult.getData(), PhoneLocationRespDTO.class);
    // QueryPageResult<PhoneLocationRespDTO> queryPageResultResp = new QueryPageResult<>();
    // queryPageResultResp.setTotal(queryPageResult.getTotal());
    // queryPageResultResp.getData().addAll(listPhoneLocationRespDTO);
    // return actionResultService.callBackResult( queryPageResultResp);
    // }

    /**
     * 查询
     *
     * @author liuchink
     * @since 2022/2/16 9:30
     */
    @Operation(summary = "导出")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/exportExcel", produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportExcel(@RequestBody @Validated PhoneLocationQueryReqDTO phoneLocationQueryReqDTO) {
        httpServletResponse.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        httpServletResponse.setCharacterEncoding("utf-8");
        try (ExcelWriter excelWriter =
            FastExcel.write(httpServletResponse.getOutputStream(), PhoneLocationExportData.class).build()) {
            String fileName = URLEncoder.encode("手机号码归属地导出", StandardCharsets.UTF_8).replace("\\+", "%20");
            httpServletResponse.setHeader("Content-Disposition",
                "attachment; filename=\"" + fileName + ".xlsx\"; filename*=" + fileName + ".xlsx");
            WriteSheet writeSheet = FastExcel.writerSheet("数据导出").build();
            phoneLocationQueryReqDTO.setPageSize(EXCEL_PAGE_SIZE);
            phoneLocationQueryReqDTO.setPageNo(1);
            phoneLocationQueryReqDTO.setPageTotal(true);
            QueryPageResult<PhoneLocation> queryPageResult =
                phoneLocationService.listPhoneLocationByCondition(phoneLocationQueryReqDTO);
            List<PhoneLocationExportData> listPhoneLocationExportData =
                mapperFacade.mapAsList(queryPageResult.getData(), PhoneLocationExportData.class);
            excelWriter.write(listPhoneLocationExportData, writeSheet);
            long pageCount = queryPageResult.getTotal() / EXCEL_PAGE_SIZE;
            if (queryPageResult.getTotal() % EXCEL_PAGE_SIZE > 0) {
                pageCount++;
            }
            for (int i = 1; i < pageCount; i++) {
                phoneLocationQueryReqDTO.setPageNo(i + 1);
                phoneLocationQueryReqDTO.setPageTotal(false);
                queryPageResult = phoneLocationService.listPhoneLocationByCondition(phoneLocationQueryReqDTO);
                listPhoneLocationExportData =
                    mapperFacade.mapAsList(queryPageResult.getData(), PhoneLocationExportData.class);
                excelWriter.write(listPhoneLocationExportData, writeSheet);
            }
        } catch (IOException e) {
            log.error(SystemConstant.LOG_ERROR_TITLE, e.getMessage(), e);
        }
    }
}
