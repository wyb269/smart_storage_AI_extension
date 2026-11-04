package com.chinaunicom.smartstorage.applet.controller.aggregation;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chinaunicom.smartstorage.applet.dto.aggregation.phone.PhoneLocationRespDTO;
import com.chinaunicom.smartstorage.applet.entity.aggregation.PhoneLocation;
import com.chinaunicom.smartstorage.applet.service.inter.aggregation.PhoneLocationService;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;

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
    public ActionResult<PhoneLocationRespDTO, String>
        getPhoneLocation(@PathVariable String phonePrefix) {
        ActionResult<PhoneLocation, String> actionResult = phoneLocationService.getPhoneLocation(phonePrefix);
        PhoneLocationRespDTO phoneLocationRespDTO =
            mapperFacade.map(actionResult.getResult(), PhoneLocationRespDTO.class);
        ActionResult<PhoneLocationRespDTO, String> actionResultResp = new ActionResult<>();
        actionResultResp.setStatus(actionResult.getStatus());
        actionResultResp.setResult(phoneLocationRespDTO);
        return actionResultResp;
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

}
