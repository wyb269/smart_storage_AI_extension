package com.chinaunicom.smartstorage.admin.dto.rbac.menu;

import org.hibernate.validator.constraints.Range;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 菜单表
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@Data
public class MenuSaveReqDTO {

    /**
     * id
     */
    @NotNull(message = "id不能为空")
    @Schema(description = "id", nullable = true, example = "12345")
    private Integer id;

    /**
     * 父id
     */
    @NotNull(message = "父id不能为空")
    @Schema(description = "父id", nullable = true, example = "12345")
    private Integer parentId;

    /**
     * 名称
     */
    @NotBlank(message = "名称不能为空")
    @Schema(description = "名称", nullable = true, example = "12345")
    private String name;

    /**
     * 打开的url
     */
    @NotBlank(message = "名称不能为空")
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
    @NotNull(message = "是否为外链不能为空")
    @Range(min = 1, max = 2, message = "是否为外链超出范围")
    @Schema(description = "是否为外链1=是、2=否", nullable = true, example = "12345")
    private Integer isFrame;
    /**
     * 显示顺序
     */
    @NotNull(message = "显示顺序不能为空")
    @Schema(description = "显示顺序", nullable = true, example = "12345")
    private Integer orderNum;
    /**
     * 是否显示1=是、2=否
     */
    @NotNull(message = "是否显示不能为空")
    @Range(min = 1, max = 2, message = "是否显示超出范围")
    @Schema(description = "是否显示1=是、2=否", nullable = true, example = "12345")
    private Integer isDisplay;
    @Schema(description = "菜单类型，1=管理后台，2=移动端", nullable = true, example = "12345")
    @NotNull(message = "菜单类型不能为空")
    @Range(min = 1, max = 2, message = "菜单类型数据不合法")
    private Integer type;
}
