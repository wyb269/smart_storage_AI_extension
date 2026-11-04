package com.chinaunicom.smartstorage.admin.entity.aggregation;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@TableName("base_system_config")
@Data
public class SystemConfig {

    /**
     * 分组
     */
    private String sycGroup;

    /**
     *
     */
    private String sycKey;

    /**
     *
     */
    private String sycValue;
    private Integer needCache;
    private Integer needPwd;
    /**
     * 备注
     */
    private String remark;
}