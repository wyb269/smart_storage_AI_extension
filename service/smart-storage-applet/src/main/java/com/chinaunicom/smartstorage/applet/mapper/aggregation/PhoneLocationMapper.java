package com.chinaunicom.smartstorage.applet.mapper.aggregation;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.github.yulichang.base.MPJBaseMapper;
import com.chinaunicom.smartstorage.applet.dto.aggregation.phone.PhoneLocationQueryReqDTO;
import com.chinaunicom.smartstorage.applet.entity.aggregation.PhoneLocation;


/**
 * 号码归属地
 *
 * @author liuchink
 * @since 2022/1/30 9:15
 */

public interface PhoneLocationMapper extends MPJBaseMapper<PhoneLocation> {

    /**
     * 查询
     *
     * @param phonePrefix phonePrefix
     * @return com.liuchink.chinkframe.cloud.aggregation.entity.phoneLocation.PhoneLocation
     * @author liuchink
     * @since 2022/2/16 9:30
     */
    PhoneLocation getPhoneLocation(@Param("phonePrefix") String phonePrefix);

    /**
     *
     * 查询
     *
     * @param phoneLocationQueryReqDTO phoneLocationQueryReqDTO
     *
     * @return com.liuchink.chinkframe.cloud.aggregation.entity.phoneLocation.PhoneLocation
     * @author liuchink
     * @since 2022/4/14 13:06
     */
    List<PhoneLocation> listPhoneLocationByCondition(PhoneLocationQueryReqDTO phoneLocationQueryReqDTO);
}
