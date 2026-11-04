package com.chinaunicom.smartstorage.applet.entity.member;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;

/**
 * 菜单表
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@Data
@TableName("base_menu_info")
public class Menu {

    /**
     * id
     */
    private Integer id;

    /**
     * 父id
     */

    private Integer parentId;

    /**
     * 名称
     */
    private String name;

    /**
     * 打开的url
     */

    private String openUrl;

    /**
     * 图标
     */
    private String icon;
    /**
     * 是否为外链1=是、2=否
     */

    private Integer isFrame;
    /**
     * 显示顺序
     */

    private Integer orderNum;
    /**
     * 是否显示1=是、2=否
     */

    private Integer isDisplay;
    private Integer type;
    @TableField(exist = false)
    private List<Menu> children;
}
