package com.chinaunicom.smartstorage.admin.entity.rbac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * @author liuchink
 * @since 2023/7/31 9:15
 */
@Data
@TableName("base_region_info")
public class Region {

    @TableId(type = IdType.INPUT)
    private String id;

    private String parentId;
    @TableField(exist = false)
    private String parentName;
    private String name;
    private String code;
    private Integer levels;

    private String fullName;

    private String fullId;

}
