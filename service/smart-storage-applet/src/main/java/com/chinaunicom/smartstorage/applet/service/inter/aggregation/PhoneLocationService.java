package com.chinaunicom.smartstorage.applet.service.inter.aggregation;


import com.chinaunicom.smartstorage.applet.dto.aggregation.phone.PhoneLocationQueryReqDTO;
import com.chinaunicom.smartstorage.applet.entity.aggregation.PhoneLocation;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.QueryPageResult;

/**
 * 归属地
 *
 * @author liuchink
 * @since 2022/1/30 9:15
 */
public interface PhoneLocationService {

    /**
     * 查询
     *
     * @param phonePrefix phonePrefix
     * @return com.liuchink.chinkframe.cloud.common.result.ActionResult
     * @author liuchink
     * @since 2022/2/16 9:30
     */
    ActionResult<PhoneLocation, String> getPhoneLocation(String phonePrefix);

    /**
     *
     * 查询
     *
     * @param phoneLocationQueryReqDTO phoneLocationQueryReqDTO
     * @return com.liuchink.chinkframe.cloud.aggregation.entity.phoneLocation.PhoneLocation
     * @author liuchink
     * @since 2022/4/14 13:06
     */
    QueryPageResult<PhoneLocation> listPhoneLocationByCondition(PhoneLocationQueryReqDTO phoneLocationQueryReqDTO);
}
