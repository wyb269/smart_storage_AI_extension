package com.chinaunicom.smartstorage.admin.entity.rbac;


import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 权限表
 *
 * @author liuchink
 * @since 2020/4/5 13:29
 */
@Data
@TableName("base_rights_info")
public class Rights {

    /**
     * id
     */
    private Integer id;

    /**
     * 名称
     */
    private String name;
}
