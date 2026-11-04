package com.chinaunicom.smartstorage.admin.controller.rbac;

import cn.hutool.core.convert.Convert;
import com.baidu.fsg.uid.UidGenerator;
import com.chinaunicom.smartstorage.admin.component.JwtTokenComponent;
import com.chinaunicom.smartstorage.admin.component.PwdComponent;
import com.chinaunicom.smartstorage.admin.dto.rbac.user.*;
import com.chinaunicom.smartstorage.admin.dto.rbac.user.*;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;
import com.chinaunicom.smartstorage.admin.service.inter.rbac.UserService;
import com.chinaunicom.smartstorage.common.bean.BatchIdReqDTO;
import com.chinaunicom.smartstorage.common.bean.UpdateShowStateReqDTO;
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
import org.apache.commons.compress.utils.Lists;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息
 *
 * @author liuchink
 * @since 2020/4/8 21:22
 */
@Tag(name = "系统管理-用户管理")
@RestController
@RequestMapping("/rbac/user")
@Slf4j
public class UserController extends BaseController {
    @Resource
    private UserService userService;

    @Resource
    private UidGenerator uidGenerator;

    @Resource
    private MapperFacade mapperFacade;
    @Resource
    private JwtTokenComponent jwtTokenComponent;
    @Resource
    private PwdComponent pwdComponent;

    /**
     * 添加
     *
     * @param userSaveReqDTO UserSaveDTO
     * @author liuchink
     * @since 2020/4/8 21:24
     */
    // @PreAuthorize("hasAnyAuthority('101')")
    @Operation(summary = "添加")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "用户管理", operatorType = OptLogOperatorType.INSERT)
    @PostMapping(path = "/saveUser", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> saveUser(@RequestBody @Validated UserSaveReqDTO userSaveReqDTO) {
        ActionResult<String, String> actionResult =
            actionResultService.callBackResultFail("密码必须包含大小字母、数字、特殊字符，最少8个字符，最多15个字符");
        userSaveReqDTO.setLoginPwd(pwdComponent.getRsaDecrypt(userSaveReqDTO.getLoginPwd()));
        String regex = "(?=.*[0-9])(?=.*[A-Z])(?=.*[a-z])(?=.*[^a-zA-Z0-9]).{8,15}";
        if (userSaveReqDTO.getLoginPwd().matches(regex)) {
            String id = Convert.toStr(uidGenerator.getUID());
            User user = mapperFacade.map(userSaveReqDTO, User.class);
            user.setId(id);
            user.setAddUser(jwtTokenComponent.getUser().getId());
            user.setLoginPwd(pwdComponent.getEncryptMd5(userSaveReqDTO.getLoginPwd()));
            user.setAddTime(LocalDateTime.now());
            user.setLastUpdatePwd(LocalDateTime.now());
            user.setMustUpdatePwd(2);
            actionResult = userService.saveUser(user);
        }
        return actionResult;
    }

    /**
     * 修改
     *
     * @param userUpdateReqDTO UserUpdateReqDTO
     * @author liuchink
     * @since 2020/4/8 21:24
     */
    @Operation(summary = "修改")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "用户管理", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/updateUser", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> updateUser(@RequestBody @Validated UserUpdateReqDTO userUpdateReqDTO) {
        User user = mapperFacade.map(userUpdateReqDTO, User.class);
        user.setUpdateUser(jwtTokenComponent.getUser().getId());
        user.setUpdateTime(LocalDateTime.now());
        return userService.updateUser(user);
    }

    /**
     * 删除
     *
     * @author liuchink
     * @since 2020/4/8 21:24
     */
    @Operation(summary = "删除")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "用户管理", operatorType = OptLogOperatorType.DELETE)
    @PostMapping(path = "/deleteUser", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> deleteUser(@RequestBody @Validated BatchIdReqDTO batchIdReqDTO) {
        List<User> listUser = Lists.newArrayList();
        for (String id : batchIdReqDTO.getIds()) {
            User user = new User();
            user.setId(id);
            listUser.add(user);
        }
        return userService.deleteUser(listUser);
    }

    /**
     * 根据条件查询
     *
     * @param userQueryReqDTO UserQueryDTO
     * @author liuchink
     * @since 2018/8/12 12:53
     */

    @Operation(summary = "根据条件查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功",
        content = @Content(schema = @Schema(implementation = UserRespDTO.class))))
    @PostMapping(path = "/listUserByConditionOnPage", produces = MediaType.APPLICATION_JSON_VALUE)
    @QueryByPage(returnClass = "com.chinaunicom.smartstorage.admin.dto.rbac.user.UserRespDTO")
    public Object listUserByConditionOnPage(@RequestBody @Validated UserQueryReqDTO userQueryReqDTO) {
        return userService.listUserByConditionOnPage(userQueryReqDTO);
    }

    /**
     * 根据ID查询
     *
     * @param id id
     * @author liuchink
     * @since 2020/7/7 13:31
     */
    @Operation(summary = "根据ID查询")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getUserById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<UserRespDTO, String> getUserById(@PathVariable String id) {
        User userById = userService.getUserById(id);
        UserRespDTO userRespDTO = mapperFacade.map(userById, UserRespDTO.class);
        return actionResultService.callBackResult(userRespDTO);
    }

    /**
     * 详情
     *
     * @param id id
     * @author liuchink
     * @since 2020/7/7 13:31
     */
    @Operation(summary = "详情")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @PostMapping(path = "/getUserByDetail/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<UserRespDTO, String> getUserByDetail(@PathVariable String id) {
        User userById = userService.getUserByDetail(id);
        UserRespDTO userRespDTO = mapperFacade.map(userById, UserRespDTO.class);
        return actionResultService.callBackResult(userRespDTO);
    }

    /**
     * 修改用户状态
     *
     * @param updateShowStateReqDTO updateShowStateReqDTO
     * @author liuchink
     * @since 2018/8/15 13:39
     */
    @Operation(summary = "修改用户状态")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "用户管理", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/update/state", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String>
        updateState(@RequestBody @Validated UpdateShowStateReqDTO updateShowStateReqDTO) {
        User user = mapperFacade.map(updateShowStateReqDTO, User.class);
        return userService.updateState(user);
    }

    /**
     * 重置用户密码
     *
     * @param id 用户ID
     * @author liuchink
     * @since 2018/8/15 13:41
     */
    @Operation(summary = "重置用户密码")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "用户管理", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/rest/password/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String> restLoginPassword(@PathVariable String id) {
        User user = new User();
        user.setId(id);
        return userService.restUserPassword(user);
    }

    /**
     * 修改在职状态
     *
     * @param userUpdateEmployedStatusReqDTO userUpdateEmployedStatusReqDTO
     * @author liuchink
     * @since 2018/8/15 13:39
     */
    @Operation(summary = "修改在职状态")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "请求成功"))
    @OptLog(title = "用户管理", operatorType = OptLogOperatorType.UPDATE)
    @PostMapping(path = "/update/employedStatus", produces = MediaType.APPLICATION_JSON_VALUE)
    public ActionResult<String, String>
        employedStatus(@RequestBody @Validated UserUpdateEmployedStatusReqDTO userUpdateEmployedStatusReqDTO) {
        User user = mapperFacade.map(userUpdateEmployedStatusReqDTO, User.class);
        return userService.updateState(user);
    }
}
