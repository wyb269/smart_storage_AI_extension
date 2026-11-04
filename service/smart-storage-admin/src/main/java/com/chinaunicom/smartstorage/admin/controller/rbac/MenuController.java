package com.chinaunicom.smartstorage.admin.controller.rbac;

import com.chinaunicom.smartstorage.admin.dto.rbac.menu.MenuRespDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.menu.MenuSaveReqDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.menu.MenuUpdateReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.menu.Menu;
import com.chinaunicom.smartstorage.admin.service.inter.rbac.MenuService;
import com.chinaunicom.smartstorage.common.controller.BaseController;
import com.chinaunicom.smartstorage.common.jwt.JwtConstant;
import com.chinaunicom.smartstorage.common.optlog.OptLog;
import com.chinaunicom.smartstorage.common.optlog.OptLogOperatorType;
import com.chinaunicom.smartstorage.common.result.ActionResult;
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

import java.util.List;

/**
 * 菜单信息
 *
 * @author liuchink
 * @since 2020/4/8 21:22
 */
@Tag(name = "系统管理-菜单管理")
@RestController
@RequestMapping("/rbac/menu")
@Slf4j
public class MenuController extends BaseController {
    @Resource
    private MenuService menuService;

    @Resource
    private MapperFacade mapperFacade;

    /**
     * 添加
     *
     * @param menuSaveReqDTO roleSaveReqDTO
     * @author liuchink
     * @since 2020/4/8 21:24
     */
    @Operation(summary = "添加")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "菜单管理", operatorType = OptLogOperatorType.INSERT)
    @PostMapping(path = "/saveMenu", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> saveMenu(@RequestBody @Validated MenuSaveReqDTO menuSaveReqDTO) {
        Menu menu = mapperFacade.map(menuSaveReqDTO, Menu.class);
        return menuService.saveMenu(menu);

    }

    /**
     * 修改
     *
     * @param menuUpdateReqDTO RoleUpdateReqDTO
     * @author liuchink
     * @since 2020/4/8 21:24
     */
    @Operation(summary = "修改")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "菜单管理", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/updateMenu", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> updateMenu(@RequestBody @Validated MenuUpdateReqDTO menuUpdateReqDTO) {
        Menu menu = mapperFacade.map(menuUpdateReqDTO, Menu.class);
        return menuService.updateMenu(menu);

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
    @OptLog(title = "菜单管理", operatorType = OptLogOperatorType.DELETE)
    @PostMapping(path = "/deleteMenu/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> deleteMenu(@PathVariable int id) {
        return menuService.deleteMenu(id);

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
    @PostMapping(path = "/getMenuById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<MenuRespDTO, String> getMenuById(@PathVariable int id) {
        Menu menu = menuService.getMenuById(id);
        MenuRespDTO menuRespDTO = mapperFacade.map(menu, MenuRespDTO.class);
        return actionResultService.callBackResult(menuRespDTO);
    }

    /**
     * 获取所有菜单
     *
     * @author liuchink
     * @since 2018/8/15 9:04
     */
    @Operation(summary = "获取所有菜单")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/listMenu", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<List<MenuRespDTO>, String>
        listMenu(@RequestHeader(JwtConstant.REQUEST_HEAD_TOKEN) String token) {
        List<Menu> listMenu = menuService.listMenu();
        List<MenuRespDTO> listMenuRespDTO = mapperFacade.mapAsList(listMenu, MenuRespDTO.class);
        return actionResultService.callBackResult(listMenuRespDTO);
    }
}
