package com.chinaunicom.smartstorage.applet.entity.aggregation;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 *
 * 号码归属地
 *
 * @author liuchink
 * @since 2022/2/16 9:28
 */
@Data
@TableName("base_phone_location")
public class PhoneLocation  {

    /**
     * 号码前缀
     */
    @TableId(type = IdType.INPUT)
    private String perfix;

    /**
     * 运营商
     */
    private String isp;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 区号
     */

    private String cityCode;

    /**
     * 邮编
     */

    private String postCode;

}
