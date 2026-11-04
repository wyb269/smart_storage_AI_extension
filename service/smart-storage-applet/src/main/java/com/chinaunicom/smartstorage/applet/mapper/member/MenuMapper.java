package com.chinaunicom.smartstorage.applet.mapper.member;

import com.github.yulichang.base.MPJBaseMapper;
import com.chinaunicom.smartstorage.applet.entity.member.Menu;

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
    List<Menu> listMenuByType(int type);

}
