package com.chinaunicom.smartstorage.admin.controller.rbac;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.rbac.role.RoleGroupQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.role.RoleGroupRespDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.role.RoleGroupSaveReqDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.role.RoleGroupUpdateReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.role.RoleGroup;
import com.chinaunicom.smartstorage.admin.service.inter.rbac.RoleGroupService;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.optlog.OptLog;
import com.chinaunicom.smartstorage.common.optlog.OptLogOperatorType;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.QueryPageResult;

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
 * 信息
 *
 * @author liuchink
 * @since 2020/4/8 21:22
 */
@Tag(name = "系统管理-角色分组管理")
@RestController
@RequestMapping("/rbac/roleGroup")
@Slf4j
public class RoleGroupController extends BaseController {
    @Resource
    private RoleGroupService roleGroupService;

    @Resource
    private UidGenerator uidGenerator;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private MapperFacade mapperFacade;

    /**
     * 添加
     *
     * @param roleGroupSaveReqDTO roleSaveReqDTO
     * @author liuchink
     * @since 2020/4/8 21:24
     */
    @Operation(summary = "添加")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "角色分组管理", operatorType = OptLogOperatorType.INSERT)
    @PostMapping(path = "/saveRoleGroup", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> saveRoleGroup(@RequestBody @Validated RoleGroupSaveReqDTO roleGroupSaveReqDTO) {
        RoleGroup roleGroup = mapperFacade.map(roleGroupSaveReqDTO, RoleGroup.class);
        roleGroup.setId(Convert.toStr(uidGenerator.getUID()));
        roleGroup.setAddUser(jwtTokenComponent.getUser().getId());
        roleGroup.setAddTime(LocalDateTime.now());
        return roleGroupService.saveRoleGroup(roleGroup);

    }

    /**
     * 修改
     *
     * @param roleGroupUpdateReqDTO RoleUpdateReqDTO
     * @author liuchink
     * @since 2020/4/8 21:24
     */
    @Operation(summary = "修改")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "角色分组管理", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/updateRoleGroup", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> updateRoleGroup(
        @RequestBody @Validated RoleGroupUpdateReqDTO roleGroupUpdateReqDTO) {
        RoleGroup roleGroup = mapperFacade.map(roleGroupUpdateReqDTO, RoleGroup.class);
        roleGroup.setUpdateTime(LocalDateTime.now());
        roleGroup.setUpdateUser(jwtTokenComponent.getUser().getId());
        return roleGroupService.updateRoleGroup(roleGroup);

    }

    /**
     * 删除
     *
     * @param id id
     * @author liuchink
     * @since 2020/4/8 21:24
     */
    @Operation(summary = "删除")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "角色分组管理", operatorType = OptLogOperatorType.DELETE)
    @PostMapping(path = "/deleteRoleGroup/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> deleteRoleGroup(
        @PathVariable String id) {
        RoleGroup role = new RoleGroup();
        role.setId(id);
        return roleGroupService.deleteRoleGroup(role);

    }

    /**
     * 根据查询条件查询
     *
     * @param roleGroupQueryReqDTO RoleQueryReqDTO
     * @author liuchink
     * @since 2018/8/21 16:31
     */
    @Operation(summary = "根据条件查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = RoleGroupRespDTO.class))))
    @PostMapping(path = "/listRoleGroupByConditionOnPage", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<QueryPageResult<RoleGroupRespDTO>, String> listRoleGroupByConditionOnPage(

        @RequestBody @Validated RoleGroupQueryReqDTO roleGroupQueryReqDTO) {
        roleGroupQueryReqDTO.setPageNo(roleGroupQueryReqDTO.getPageNo() > 1
            ? (roleGroupQueryReqDTO.getPageNo() - 1) * roleGroupQueryReqDTO.getPageSize() : 0);
        final QueryPageResult<RoleGroup> roleGroupQueryPageResult =
            roleGroupService.listRoleGroupByConditionOnPage(roleGroupQueryReqDTO);
        List<RoleGroupRespDTO> listRoleGroupRespDTO =
            mapperFacade.mapAsList(roleGroupQueryPageResult.getData(), RoleGroupRespDTO.class);
        QueryPageResult<RoleGroupRespDTO> queryPageResult = new QueryPageResult<>();
        queryPageResult.setData(listRoleGroupRespDTO);
        queryPageResult.setTotal(roleGroupQueryPageResult.getTotal());
        return actionResultService.callBackResult(queryPageResult);
    }

    /**
     * 根据ID查询
     *
     * @param id id
     * @author liuchink
     * @since 2020/9/16 10:01
     */
    @Operation(summary = "根据ID查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getRoleGroupById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<RoleGroupRespDTO, String>
        getRoleGroupById( @PathVariable String id) {
        RoleGroup roleGroupById = roleGroupService.getRoleGroupById(id);
        RoleGroupRespDTO roleGroupRespDTO = mapperFacade.map(roleGroupById, RoleGroupRespDTO.class);
        return actionResultService.callBackResult(roleGroupRespDTO);
    }

    @Operation(summary = "查看详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getRoleGroupByDetail/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<RoleGroupRespDTO, String> getRoleGroupByDetail(
         @PathVariable String id) {
        RoleGroup roleGroupById = roleGroupService.getRoleGroupByDetail(id);
        RoleGroupRespDTO roleGroupRespDTO = mapperFacade.map(roleGroupById, RoleGroupRespDTO.class);
        return actionResultService.callBackResult(roleGroupRespDTO);
    }
}
