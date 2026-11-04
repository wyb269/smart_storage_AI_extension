package com.chinaunicom.smartstorage.admin.controller.aggregation;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.chinaunicom.smartstorage.admin.dto.aggregation.sms.CheckSmsVerifyCodeReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.sms.SmsVerifyCodeQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.sms.SmsVerifyCodeRespDTO;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.SmsVerifyCodeService;
import com.chinaunicom.smartstorage.common.page.QueryByPage;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import ma.glasnost.orika.MapperFacade;

/**
 * 验证码
 *
 * @author liuchink
 * @since 2022/1/30 9:53
 */
@Tag(name = "系统管理-短信验证码")
@RestController
@RequestMapping("/aggregation/sms")
public class SmsVerifyCodeController extends BaseController {
    @Resource
    private SmsVerifyCodeService smsVerifyCodeService;

    @Resource
    private MapperFacade mapperFacade;

    /**
     *
     * 验证码验证
     *
     * @param checkSmsVerifyCodeReqDTO checkSmsVerifyCodeReqDTO
     * @author liuchink
     * @since 2022/1/30 9:20
     */
    @Operation(summary = "验证码验证")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/checkSmsVerifyCode", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> checkSmsVerifyCode(

        @RequestBody @Validated CheckSmsVerifyCodeReqDTO checkSmsVerifyCodeReqDTO) {
        return smsVerifyCodeService.checkSmsVerifyCode(checkSmsVerifyCodeReqDTO.getPhone(),
            checkSmsVerifyCodeReqDTO.getCode());
    }

    /**
     *
     * 验证码验证
     *
     * @param phone phone
     * @author liuchink
     * @since 2022/1/30 9:20
     */
    @Operation(summary = "发送验证码")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/createSmsVerifyCode/{phone}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> createSmsVerifyCode(

        @PathVariable String phone) {
        return smsVerifyCodeService.createSmsVerifyCode(phone);
    }

    @Operation(summary = "根据条件查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = SmsVerifyCodeRespDTO.class))))
    @PostMapping(path = "/listSmsVerifyCodeByConditon", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.aggregation.sms.SmsVerifyCodeRespDTO")
    public Object listSmsVerifyCodeByConditon(
        @RequestBody @Validated SmsVerifyCodeQueryReqDTO smsVerifyCodeQueryReqDTO) {
        return smsVerifyCodeService.listSmsVerifyCodeByConditon(smsVerifyCodeQueryReqDTO);
    }
}
