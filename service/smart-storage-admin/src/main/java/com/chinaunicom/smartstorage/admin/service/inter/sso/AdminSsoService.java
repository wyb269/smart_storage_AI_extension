package com.chinaunicom.smartstorage.admin.service.inter.sso;

import com.chinaunicom.smartstorage.admin.dto.rbac.login.LoginBaseInfoRespDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.login.LoginMenuRespDTO;
import com.chinaunicom.smartstorage.admin.dto.rbac.login.LoginRespDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.PasswordLoginRepDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.PhoneLoginRepDTO;
import com.chinaunicom.smartstorage.admin.dto.sso.UpdateUserPasswordReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author liuchink
 * @since 2023/7/13 9:22
 */
public interface AdminSsoService {
    /**
     * 登录
     *
     * @param passwordLoginRepDTO passwordLoginRepDTO
     * @author liuchink
     * @since 2022/1/6 8:57
     */
    ActionResult<LoginRespDTO, String> loginByPassword(@RequestBody @Validated PasswordLoginRepDTO passwordLoginRepDTO);

    /**
     * 登录
     *
     * @param phoneLoginRepDTO phoneLoginRepDTO
     * @return result.com.chinaunicom.smartstorage.common.ActionResult<java.lang.String, java.lang.String>
     * @author liuchink
     * @since 2023/7/31 14:22
     */
    ActionResult<LoginRespDTO, String> loginByPhone(@RequestBody @Validated PhoneLoginRepDTO phoneLoginRepDTO);

    /**
     *
     * 获取用户基本信息
     *
     *
     * @return result.com.chinaunicom.smartstorage.common.ActionResult
     * @author liuchink
     * @since 2023/8/31 11:17
     */
    ActionResult<LoginBaseInfoRespDTO, String> getLoginBaseInfo();

    /**
     *
     * 查询菜单
     *
     *
     * @return result.com.chinaunicom.smartstorage.common.ActionResult
     * @author liuchink
     * @since 2023/8/31 13:15
     */
    ActionResult<List<LoginMenuRespDTO>, String> getLoginMenuInfo();


    /**
     *
     * 修改密码
     *
     * @param updateUserPasswordReqDTO updateUserPasswordReqDTO
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2022/4/15 13:14
     */
    ActionResult<String, String> updateUserLoginPassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO);

    ActionResult<String, String> updateUserLoginPasswordByLoginId(UpdateUserPasswordReqDTO updateUserPasswordReqDTO);

    /**
     * 修改
     *
     * @param user User
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2020/4/5 14:05
     */
    ActionResult<String, String> updateUserBase(String token, User user);

}
