package com.chinaunicom.smartstorage.admin.dto.rbac.menu;

import com.chinaunicom.smartstorage.admin.dto.rbac.rights.RightsRespDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 菜单表
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@Data
public class MenuRespDTO {

    /**
     * id
     */
    @Schema(description = "id", nullable = true, example = "12345")
    private Integer id;

    /**
     * 父id
     */
    @Schema(description = "父id", nullable = true, example = "12345")
    private Integer parentId;

    /**
     * 名称
     */
    @Schema(description = "名称", nullable = true, example = "12345")
    private String name;

    /**
     * 打开的url
     */
    @Schema(description = "url", nullable = true, example = "12345")
    private String openUrl;

    /**
     * 图标
     */
    @Schema(description = "图标", nullable = true, example = "12345")
    private String icon;
    /**
     * 是否为外链1=是、2=否
     */
    @Schema(description = "是否为外链1=是、2=否", nullable = true, example = "12345")
    private Integer isFrame;
    /**
     * 显示顺序
     */
    @Schema(description = "显示顺序", nullable = true, example = "12345")
    private Integer orderNum;
    /**
     * 是否显示1=是、2=否
     */
    @Schema(description = "是否显示1=是、2=否", nullable = true, example = "12345")
    private Integer isDisplay;
    @Schema(description = "菜单类型，1=管理后台，2=移动端", nullable = true, example = "12345")
    private Integer type;
    @Schema(description = "权限信息", nullable = true, example = "12345")
    private List<RightsRespDTO> listRights;
    @Schema(description = "子节点", nullable = true, example = "12345")
    private List<MenuRespDTO> children;
}
