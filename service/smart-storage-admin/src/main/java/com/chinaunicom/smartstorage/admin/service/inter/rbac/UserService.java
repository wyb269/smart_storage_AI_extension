package com.chinaunicom.smartstorage.admin.service.inter.rbac;

import java.util.List;

import com.chinaunicom.smartstorage.admin.dto.rbac.user.UserQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;
import com.chinaunicom.smartstorage.common.result.ActionResult;

/**
 * 用户信息
 *
 * @author liuchink
 * @since 2020/4/19 21:30
 */
public interface UserService {
    /**
     * 添加
     *
     * @param user User
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2020/4/5 14:05
     */
    ActionResult<String, String> saveUser(User user);

    /**
     * 修改
     *
     * @param user User
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2020/4/5 14:05
     */
    ActionResult<String, String> updateUser(User user);

    /**
     *
     * 删除
     *
     * @param id id
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2022/1/27 15:55
     */
    ActionResult<String, String> deleteUser(List<User> listUser);

    /**
     * 获取所有用户信息
     *
     * @param userQueryReqDTO UserQueryReqDTO
     * @return java.util.List<com.liuchink.chinkframe.cloud.entity.rbac.User>
     * @author liuchink 2017/5/17 22:51
     */
    List<User> listUserByConditionOnPage(UserQueryReqDTO userQueryReqDTO);

    /**
     * 根据ID查询
     *
     * @param id id
     * @return com.liuchink.chinkframe.cloud.entity.rbac.User
     * @author liuchink
     * @since 2020/7/7 13:29
     */
    User getUserById(String id);

    User getUserByDetail(String id);

    /**
     * 修改用户状态
     *
     * @param user User
     * @return com.liuchink.chinkframe.cloud.common.bean.ActionResult
     * @author liuchink
     * @since 2017/05/18 11:50
     */

    ActionResult<String, String> updateState(User user);

    /**
     *
     * 重置密码
     *
     * @param user user
     *
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult<java.lang.String,java.lang.String>
     * @author liuchink
     * @since 2021/8/30 11:16
     */
    ActionResult<String, String> restUserPassword(User user);

}
