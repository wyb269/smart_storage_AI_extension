package com.chinaunicom.smartstorage.applet.entity.aggregation;

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

    /**
     * 备注
     */
    private String remark;
}