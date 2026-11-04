package com.chinaunicom.smartstorage.admin.mapper.rbac;


import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.github.yulichang.base.MPJBaseMapper;
import com.chinaunicom.smartstorage.admin.dto.rbac.user.UserQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.rbac.user.User;


/**
 * 用户信息
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */

public interface UserMapper extends MPJBaseMapper<User> {

    /**
     * 登录查询
     *
     * @param loginId loginId
     * @return com.chinaunicom.smartstorage.admin.entity.rbac.User
     * @author liuchink
     * @since 2020/5/22 14:46
     */
    User login(@Param("loginId") String loginId);

    /**
     * 获取所有用户信息
     *
     * @param userQueryReqDTO UserQueryReqDTO
     * @return java.util.List<com.liuchink.chinkframe.cloud.entity.rbac.User>
     * @author liuchink 2017/5/17 22:51
     */
    List<User> listUserByConditionOnPage(UserQueryReqDTO userQueryReqDTO);

    /**
     * 根据机构ID获取所有用户信息
     *
     * @param orgId orgId
     * @return java.util.List<com.liuchink.chinkframe.cloud.entity.User>
     * @author liuchink
     * @since 2017/07/10 14:40
     */

    List<String> listUserByOrgId(@Param("orgId") String orgId);

    /**
     *
     * 查询
     *
     * @param loginId loginId
     * @return java.util.List
     * @author liuchink
     * @since 2022/4/15 10:56
     */
    User getUserByLoginId(@Param("loginId") String loginId);

    /**
     *
     * 修改密码
     *
     * @param user user
     *
     * @author liuchink
     * @since 2022/4/15 13:14
     */
    void updateUserLoginPassword(User user);

    /**
     * 登录查询
     *
     * @param loginId loginId
     * @return com.liuchink.chinkframe.cloud.rbac.entity.User
     * @author liuchink
     * @since 2020/5/22 14:46
     */
    User loginInfoByLoginId(@Param("loginId") String loginId);

    User loginInfoByPhone(@Param("phone") String phone);

    /**
     *
     * 基础信息
     *
     * @param id id
     *
     * @return com.chinaunicom.smartstorage.admin.mapper.entity.rbac.user.User
     * @author liuchink
     * @since 2023/8/31 11:18
     */
    User getLoginBaseInfo(@Param("id") String id);

    User getUserByDetail(@Param("id") String id);
}
