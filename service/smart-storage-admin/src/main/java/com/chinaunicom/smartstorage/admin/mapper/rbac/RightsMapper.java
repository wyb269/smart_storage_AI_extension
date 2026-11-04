package com.chinaunicom.smartstorage.admin.mapper.rbac;

import com.github.yulichang.base.MPJBaseMapper;
import com.chinaunicom.smartstorage.admin.entity.rbac.Rights;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * 权限信息
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */

public interface RightsMapper extends MPJBaseMapper<Rights> {

    /**
     * 获取所有权限
     *
     * @return java.util.List<com.liuchink.chinkframe.cloud.entity.rbac.Rights>
     * @author liuchink
     * @since 2017/5/17 22:47
     */
    List<Rights> listRights();

    /**
     *
     * 查询
     *
     * @param groupId groupId
     *
     * @return java.util.List<com.chinaunicom.smartstorage.admin.mapper.entity.rbac.rights.Rights>
     * @author liuchink
     * @since 2023/8/31 13:16
     */
    List<Rights> listRightsByRoleGroupId(@Param("groupId") String groupId);
}
