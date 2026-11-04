package com.chinaunicom.smartstorage.admin.controller.aggregation;

import cn.hutool.core.convert.Convert;
import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.dto.aggregation.customer.*;
import com.google.common.collect.Lists;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.aggregation.customer.*;
import com.chinaunicom.smartstorage.admin.entity.aggregation.customer.StsCustomer;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.customer.CustomerService;
import com.chinaunicom.smartstorage.common.bean.BatchIdReqDTO;
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
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "客户管理")
@Slf4j
@RestController
@RequestMapping("/customer")
public class CustomerController extends BaseController {
    @Resource
    private CustomerService customerService;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private UidGenerator uidGenerator;
    @Operation(summary = "添加")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "客户管理", operatorType = OptLogOperatorType.INSERT)
    @PostMapping(path = "/saveCustomer", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> saveCustomer(@RequestBody @Validated CustomerSaveReqDTO customerSaveReqDTO) {
        ActionResult<String, String> actionResult;
        StsCustomer customer = mapperFacade.map(customerSaveReqDTO,StsCustomer.class);
        customer.setAddUser(jwtTokenComponent.getUser().getId());
        customer.setAddTime(LocalDateTime.now());
        String id = Convert.toStr(uidGenerator.getUID());
        customer.setId(id);
        actionResult = customerService.saveCustomer(customer);
        return actionResult;
    }
    @Operation(summary = "修改")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/updateCustomer", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "客户管理", operatorType = OptLogOperatorType.UPDATE)
    public ActionResult<String, String> updateCustomer(@RequestBody @Validated CustomerUpdateDTO customerUpdateDTO) {
        StsCustomer customer = mapperFacade.map(customerUpdateDTO, StsCustomer.class);
        customer.setUpdateUser(jwtTokenComponent.getUser().getId());
        customer.setUpdateTime(LocalDateTime.now());
        return customerService.updateCustomer(customer);
    }
    @Operation(summary = "删除")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/deleteCustomer", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "客户管理", operatorType = OptLogOperatorType.DELETE)
    public ActionResult<String, String> deleteCustomer(@RequestBody @Validated BatchIdReqDTO batchIdReqDTO) {
        List<StsCustomer> customerList = Lists.newArrayList();
        for (String id : batchIdReqDTO.getIds()) {
            StsCustomer customer = new StsCustomer();
            customer.setId(id);
            customerList.add(customer);
        }
        return customerService.deleteCustomer(customerList);
    }
    @Operation(summary = "根据ID查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getCustomerById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<CustomerRespDTO, String> getCustomerById(@PathVariable String id) {
        StsCustomer customer = customerService.getCustomerById(id);
        CustomerRespDTO customerRespDTO = mapperFacade.map(customer, CustomerRespDTO.class);
        return actionResultService.callBackResult(customerRespDTO);
    }
    @Operation(summary = "列表查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",content = @Content(schema = @Schema(implementation = CustomerQueryRespDTO.class))))
    @PostMapping(path = "/listCustomerByConditionOnPage", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.aggregation.customer.CustomerQueryRespDTO")
    public Object listCustomerByConditionOnPage(@RequestBody @Validated CustomerQueryReqDTO customerQueryReqDTO) {
        return customerService.listCustomerByCondition(customerQueryReqDTO);
    }
}
