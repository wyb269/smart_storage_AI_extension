package com.chinaunicom.smartstorage.admin.mapper.rbac;


import com.github.yulichang.base.MPJBaseMapper;
import com.chinaunicom.smartstorage.admin.entity.rbac.menu.Menu;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单信息
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */

public interface MenuMapper extends MPJBaseMapper<Menu> {

    /**
     * 查询所有的菜单
     *
     * @return List<Menu>
     * @author liuchink
     * @since 2017/5/17 22:43
     */
    List<Menu> listMenu();

    /**
     *
     * 查询
     *
     * @param groupId groupId
     *
     * @return java.util.List<com.chinaunicom.smartstorage.admin.mapper.entity.rbac.menu.Menu>
     * @author liuchink
     * @since 2023/8/31 13:10
     */
    List<Menu> listMenuByGroupId(@Param("groupId") String groupId);

    List<Integer> listMenuByIdWithChild(@Param("id") int id);
}
