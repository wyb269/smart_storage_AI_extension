package com.chinaunicom.smartstorage.admin.controller.aggregation;

import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.aggregation.sysconfig.SystemConfigDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.sysconfig.SystemConfigGroupAndKeyDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.sysconfig.SystemConfigQueryDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.SystemConfig;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.SystemConfigService;
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
import ma.glasnost.orika.metadata.TypeBuilder;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 活动管理
 *
 * @author liuchink
 * @since 2022/6/9 15:17
 */
@Slf4j
@Tag(name = "系统管理-配置管理")
@RestController
@RequestMapping("/aggregation/systemConfig")
public class SystemConfigController extends BaseController {

    @Resource
    private SystemConfigService systemConfigService;
    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private JwtTokenComponent jwtTokenComponent;

    @Operation(summary = "修改")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "配置管理", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/updateSystemConfig", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> updateSystemConfig(@RequestBody @Validated SystemConfigDTO systemConfigDTO) {
        SystemConfig systemConfig = mapperFacade.map(systemConfigDTO, SystemConfig.class);
        systemConfig.setNeedCache(null);
        systemConfig.setNeedPwd(null);
        return systemConfigService.updateSystemConfig(systemConfig);
    }

    @Operation(summary = "根据分组和键值查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getSystemConfigByGroupAndKey", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<SystemConfigDTO, String>
        getSystemConfigByGroupAndKey(@RequestBody @Validated SystemConfigGroupAndKeyDTO systemConfigGroupAndKeyDTO) {
        ActionResult<SystemConfig, String> actionResult =
            systemConfigService.getSystemConfigByGroupAndKey(systemConfigGroupAndKeyDTO);
        ActionResult<SystemConfigDTO, String> actionResultResp = actionResultService.callBackResult();
        mapperFacade.map(actionResult, actionResultResp,
            new TypeBuilder<ActionResult<SystemConfig, String>>() {}.build(),
            new TypeBuilder<ActionResult<SystemConfigDTO, String>>() {}.build());
        return actionResultResp;
    }

    @Operation(summary = "查看详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getSystemConfigByGroupAndKeyByDetail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<SystemConfigDTO, String> getSystemConfigByGroupAndKeyByDetail(
        @RequestBody @Validated SystemConfigGroupAndKeyDTO systemConfigGroupAndKeyDTO) {
        SystemConfig systemConfig = mapperFacade.map(systemConfigGroupAndKeyDTO, SystemConfig.class);
        SystemConfig systemConfigDetail = systemConfigService.getSystemConfigByGroupAndKeyByDetail(systemConfig);
        SystemConfigDTO systemConfigDTO = mapperFacade.map(systemConfigDetail, SystemConfigDTO.class);
        return actionResultService.callBackResult(systemConfigDTO);
    }

    @Operation(summary = "根据条件查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = SystemConfigDTO.class))))
    @PostMapping(path = "/listSystemConfigByCondition", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.aggregation.sysconfig.SystemConfigDTO")
    public Object listSystemConfigByCondition(@RequestBody @Validated SystemConfigQueryDTO systemConfigQueryDTO) {
        return systemConfigService.listSystemConfigByCondition(systemConfigQueryDTO);
    }
}
