package com.chinaunicom.smartstorage.admin.controller.rbac;

import java.time.LocalDateTime;
import java.util.List;

import com.chinaunicom.smartstorage.admin.dto.rbac.role.*;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.dto.rbac.role.*;
import com.chinaunicom.smartstorage.admin.entity.rbac.role.Role;
import com.chinaunicom.smartstorage.admin.service.inter.rbac.RoleService;
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
@Tag(name = "系统管理-角色管理")
@RestController
@RequestMapping("/rbac/role")
@Slf4j
public class RoleController extends BaseController {
    @Resource
    private RoleService roleService;

    @Resource
    private UidGenerator uidGenerator;

    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private JwtTokenComponent jwtTokenComponent;

    /**
     * 添加
     *
     * @param roleSaveReqDTO roleSaveReqDTO
     * @author liuchink
     * @since 2020/4/8 21:24
     */
    @Operation(summary = "添加")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "角色管理", operatorType = OptLogOperatorType.INSERT)
    @PostMapping(path = "/saveRole", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> saveRole(@RequestBody @Validated RoleSaveReqDTO roleSaveReqDTO) {
        Role role = mapperFacade.map(roleSaveReqDTO, Role.class);
        role.setId(Convert.toStr(uidGenerator.getUID()));
        role.setAddUser(jwtTokenComponent.getUser().getId());
        role.setAddTime(LocalDateTime.now());
        return roleService.saveRole(role);

    }

    /**
     * 修改
     *
     * @param roleUpdateReqDTO RoleUpdateReqDTO
     * @author liuchink
     * @since 2020/4/8 21:24
     */
    @Operation(summary = "修改")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "角色管理", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/updateRole", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> updateRole(@RequestBody @Validated RoleUpdateReqDTO roleUpdateReqDTO) {
        Role role = mapperFacade.map(roleUpdateReqDTO, Role.class);
        role.setUpdateTime(LocalDateTime.now());
        role.setUpdateUser(jwtTokenComponent.getUser().getId());
        return roleService.updateRole(role);

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
    @OptLog(title = "角色管理", operatorType = OptLogOperatorType.DELETE)
    @PostMapping(path = "/deleteRole/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> deleteRole(@PathVariable String id) {
        Role role = new Role();
        role.setId(id);
        return roleService.deleteRole(role);

    }

    /**
     * 根据查询条件查询
     *
     * @param roleQueryReqDTO RoleQueryReqDTO
     * @author liuchink
     * @since 2018/8/21 16:31
     */
    @Operation(summary = "根据条件查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = RoleRespDTO.class))))
    @PostMapping(path = "/listRoleByConditionOnPage", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<QueryPageResult<RoleRespDTO>, String> listRoleByConditionOnPage(

        @RequestBody @Validated RoleQueryReqDTO roleQueryReqDTO) {
        roleQueryReqDTO.setPageNo(
            roleQueryReqDTO.getPageNo() > 1 ? (roleQueryReqDTO.getPageNo() - 1) * roleQueryReqDTO.getPageSize() : 0);
        final QueryPageResult<Role> userQueryPageResult = roleService.listRoleByConditionOnPage(roleQueryReqDTO);
        List<RoleRespDTO> listRoleRespDTO = mapperFacade.mapAsList(userQueryPageResult.getData(), RoleRespDTO.class);
        QueryPageResult<RoleRespDTO> queryPageResult = new QueryPageResult<>();
        queryPageResult.setData(listRoleRespDTO);
        queryPageResult.setTotal(userQueryPageResult.getTotal());
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
    @PostMapping(path = "/getRoleById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<RoleRespDTO, String> getRoleById(@PathVariable String id) {
        Role roleById = roleService.getRoleById(id);
        RoleRespDTO roleRespDTO = mapperFacade.map(roleById, RoleRespDTO.class);
        return actionResultService.callBackResult(roleRespDTO);
    }

    @Operation(summary = "详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getRoleByDetail/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<RoleRespDTO, String> getRoleByDetail(@PathVariable String id) {
        Role roleById = roleService.getRoleByDetail(id);
        RoleRespDTO roleRespDTO = mapperFacade.map(roleById, RoleRespDTO.class);
        return actionResultService.callBackResult(roleRespDTO);
    }

    /**
     * 查询角色下菜单
     *
     * @param id id
     * @author liuchink
     * @since 2018/8/12 12:22
     */
    @Operation(summary = "查询角色下菜单")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/listRoleByIdForMenu/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<RoleRespDTO, String> listRoleByIdForMenu(@PathVariable String id) {
        Role role = roleService.listRoleByIdForMenu(id);
        RoleRespDTO roleRespDTO = mapperFacade.map(role, RoleRespDTO.class);
        return actionResultService.callBackResult(roleRespDTO);

    }

    /**
     * 查询角色下权限
     *
     * @param id id
     * @author liuchink
     * @since 2018/8/12 12:22
     */
    // @Operation(summary = "查询角色下权限")
    // @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    // @PostMapping(path = "/listRoleByIdForRights/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    // public ActionResult<RoleRespDTO, String> listRoleByIdForRights(@PathVariable String id) {
    // Role role = roleService.listRoleByIdForRights(id);
    // RoleRespDTO roleRespDTO = mapperFacade.map(role, RoleRespDTO.class);
    // return actionResultService.callBackResult(roleRespDTO);
    //
    // }

    /**
     * 给添加菜单
     *
     * @param roleMenuReqDTO roleMenuReqDTO
     * @author liuchink
     * @since 2017/12/18 12:01
     */
    @Operation(summary = "给角色添加菜单")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "角色管理", operatorType = OptLogOperatorType.INSERT)
    @PostMapping(path = "/updateRoleByIdForMenu", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> updateRoleByIdForMenu(@RequestBody @Validated RoleMenuReqDTO roleMenuReqDTO) {
        Role role = mapperFacade.map(roleMenuReqDTO, Role.class);
        return roleService.updateRoleByIdForMenu(role);

    }

    /**
     * 给添加权限
     *
     * @param roleRightsReqDTO roleRightsReqDTO
     * @author liuchink
     * @since 2018/8/15 10:39
     */
    // @Operation(summary = "给角色添加权限")
    // @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    // @OptLog(title = "角色管理", operatorType = OptLogOperatorType.INSERT)
    // @PostMapping(path = "/updateRoleByIdForRights", produces = MediaType.APPLICATION_JSON_VALUE)
    // public ActionResult<String, String> updateRoleByIdForRights(@RequestBody RoleRightsReqDTO roleRightsReqDTO) {
    // Role role = mapperFacade.map(roleRightsReqDTO, Role.class);
    // return roleService.updateRoleByIdForRights(role);
    //
    // }
}
