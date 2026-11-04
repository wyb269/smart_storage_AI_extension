package com.chinaunicom.smartstorage.admin.controller.rbac;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.rbac.organization.OrganizationQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.organization.OrganizationRespDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.organization.OrganizationSaveReqDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.organization.OrganizationUpdateReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.Organization;
import com.chinaunicom.smartstorage.admin.service.inter.rbac.OrganizationService;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.optlog.OptLog;
import com.chinaunicom.smartstorage.common.optlog.OptLogOperatorType;
import com.chinaunicom.smartstorage.common.page.QueryByPage;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import cn.hutool.core.convert.Convert;
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
@Tag(name = "系统管理-组织机构管理")
@RestController
@RequestMapping("/rbac/organization")
@Slf4j
public class OrganizationController extends BaseController {
    @Resource
    private OrganizationService organizationService;

    @Resource
    private UidGenerator uidGenerator;

    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private JwtTokenComponent jwtTokenComponent;

    /**
     * 添加
     *
     * @param organizationSaveReqDTO organizationReqDTO
     * @author liuchink
     * @since 2018/8/21 16:30
     */
    @Operation(summary = "添加")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    // @PreAuthorize("hasAnyAuthority('301')")
    @PostMapping(path = "/saveOrganization", produces = MediaType.APPLICATION_JSON_VALUE)
    @OptLog(title = "机构管理", operatorType = OptLogOperatorType.INSERT)
    public ActionResult<String, String> saveOrganization(
        @RequestBody @Validated OrganizationSaveReqDTO organizationSaveReqDTO) {
        Organization organization = mapperFacade.map(organizationSaveReqDTO, Organization.class);
        organization.setId(Convert.toStr(uidGenerator.getUID()));
        organization.setAddUser(jwtTokenComponent.getUser().getId());
        organization.setAddTime(LocalDateTime.now());
        return organizationService.saveOrganization(organization);
    }

    /**
     * 修改
     *
     * @param organizationUpdateReqDTO OrganizationUpdateReqDTO
     * @author liuchink
     * @since 2018/8/21 16:30
     */
    @Operation(summary = "修改")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "机构管理", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/updateOrganization", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> updateOrganization(
        @RequestBody @Validated OrganizationUpdateReqDTO organizationUpdateReqDTO) {
        Organization organization = mapperFacade.map(organizationUpdateReqDTO, Organization.class);
        organization.setUpdateUser(jwtTokenComponent.getUser().getId());
        organization.setUpdateTime(LocalDateTime.now());
        return organizationService.updateOrganization(organization);
    }

    /**
     * 删除
     *
     * @param id id
     * @author liuchink
     * @since 2018/8/21 16:31
     */
    @Operation(summary = "删除")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "机构管理", operatorType = OptLogOperatorType.DELETE)
    @PostMapping(path = "/deleteOrganization/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> deleteOrganization(@PathVariable String id) {
        return organizationService.deleteOrganization(id);
    }

    /**
     * 根据组织机构ID查询机构信息
     *
     * @param id id
     * @author liuchink
     * @since 2018/8/21 16:17
     */
    @Operation(summary = "根据ID查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    // @RequestLimit(key = "getOrganizationById", permitsCount = 3, timeout = 5, timeunit = TimeUnit.SECONDS)
    @PostMapping(path = "/getOrganizationById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<OrganizationRespDTO, String> getOrganizationById(
         @PathVariable String id) {
        Organization organizationById = organizationService.getOrganizationById(id);
        OrganizationRespDTO organizationRespDTO = mapperFacade.map(organizationById, OrganizationRespDTO.class);
        return actionResultService.callBackResult(organizationRespDTO);
    }

    @Operation(summary = "详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getOrganizationByDetail/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<OrganizationRespDTO, String> getOrganizationByDetail(
         @PathVariable String id) {
        Organization organizationById = organizationService.getOrganizationByDetail(id);
        OrganizationRespDTO organizationRespDTO = mapperFacade.map(organizationById, OrganizationRespDTO.class);
        return actionResultService.callBackResult(organizationRespDTO);
    }

    /**
     * 根据查询条件查询
     *
     * @param organizationQueryReqDTO OrganizationQueryReqDTO
     * @author liuchink
     * @since 2018/8/21 16:31
     */
    @Operation(summary = "根据条件查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = OrganizationRespDTO.class))))
    @PostMapping(path = "/listOrganizationByConditionOnPage", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.rbac.organization.OrganizationRespDTO")
    // @QueryByPage()
    public Object listOrganizationByConditionOnPage(
        @RequestBody @Validated OrganizationQueryReqDTO organizationQueryReqDTO) {
        return organizationService.listOrganizationByConditionOnPage(organizationQueryReqDTO);
    }

    /**
     *
     * 查询
     *
     * @param id id
     *
     * @author liuchink
     * @since 2022/1/27 9:42
     */
    @Operation(summary = "根据ID查询子机构")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/listOrganizationChildById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<List<OrganizationRespDTO>, String> listOrganizationChildById(
         @PathVariable String id) {
        List<Organization> listOrganization = organizationService.listOrganizationChildById(id);
        List<OrganizationRespDTO> listOrganizationRespDTO =
            mapperFacade.mapAsList(listOrganization, OrganizationRespDTO.class);
        return actionResultService.callBackResult(listOrganizationRespDTO);
    }

    /**
     *
     * 查询
     *
     * @param id id
     *
     * @author liuchink
     * @since 2022/1/27 9:42
     */
    @Operation(summary = "根据ID查询父机构")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/listOrganizationParentById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<List<OrganizationRespDTO>, String> listOrganizationParentById(
         @PathVariable String id) {
        List<Organization> listOrganization = organizationService.listOrganizationParentById(id);
        List<OrganizationRespDTO> listOrganizationRespDTO =
            mapperFacade.mapAsList(listOrganization, OrganizationRespDTO.class);
        return actionResultService.callBackResult(listOrganizationRespDTO);
    }

    /**
     * 获取所有子节点信息
     *
     * @param id id
     * @author liuchink
     * @since 2018/8/21 16:17
     */
    @Operation(summary = "组织机构级联查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/listOrganizationChildByIdAndLevels/{id}/{levels}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<List<OrganizationRespDTO>, String> listOrganizationChildByIdAndLevels(
         @PathVariable String id,
        @PathVariable Integer levels) {
        List<Organization> listOrganization = organizationService.listOrganizationInfoChildByIdAndLevels(id, levels);
        List<OrganizationRespDTO> listOrganizationRespDTO =
            mapperFacade.mapAsList(listOrganization, OrganizationRespDTO.class);
        return actionResultService.callBackResult(listOrganizationRespDTO);
    }
}
