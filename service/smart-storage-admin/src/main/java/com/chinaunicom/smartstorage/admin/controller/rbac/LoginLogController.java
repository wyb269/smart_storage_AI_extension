package com.chinaunicom.smartstorage.admin.controller.rbac;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.dto.rbac.login.LoginLogQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.login.LoginLogRespDTO;
import com.chinaunicom.smartstorage.admin.service.inter.rbac.LoginLogService;
import com.chinaunicom.smartstorage.common.page.QueryByPage;
import com.chinaunicom.smartstorage.common.controller.BaseController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;

/**
 * @author liuchink
 * @since 2020/4/5 15:19
 */
@Tag(name = "系统管理-用户登录日志")
@RestController
@RequestMapping("/rbac/loginLog")
@Slf4j
public class LoginLogController extends BaseController {
    @Resource
    private LoginLogService loginLogService;

    @Resource
    private UidGenerator uidGenerator;

    @Resource
    private MapperFacade mapperFacade;

    /**
     * 根据查询条件查询
     *
     * @param loginLogQueryReqDTO loginLogQueryReqDTO
     * @author liuchink
     * @since 2018/8/21 16:31
     */
    @Operation(summary = "根据条件查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = LoginLogRespDTO.class))))
    @PostMapping(path = "/listLoginLogByConditionOnPage", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.rbac.login.LoginLogRespDTO")
    public Object listLoginLogByConditionOnPage(
        @RequestBody @Validated LoginLogQueryReqDTO loginLogQueryReqDTO) {
        return loginLogService.listLoginLogByConditionOnPage(loginLogQueryReqDTO);
    }

}
