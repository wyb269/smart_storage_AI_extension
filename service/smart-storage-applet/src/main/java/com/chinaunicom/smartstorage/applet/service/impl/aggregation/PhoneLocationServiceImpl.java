package com.chinaunicom.smartstorage.applet.service.impl.aggregation;

import java.util.List;

import org.springframework.stereotype.Service;

import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.chinaunicom.smartstorage.applet.mapper.aggregation.PhoneLocationMapper;
import com.chinaunicom.smartstorage.applet.dto.aggregation.phone.PhoneLocationQueryReqDTO;
import com.chinaunicom.smartstorage.applet.entity.aggregation.PhoneLocation;
import com.chinaunicom.smartstorage.applet.service.inter.aggregation.PhoneLocationService;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import com.chinaunicom.smartstorage.common.result.QueryPageResult;

import cn.hutool.core.text.CharSequenceUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 归属地
 *
 * @author liuchink
 * @since 2022/1/30 9:15
 */
@Service
@Slf4j
public class PhoneLocationServiceImpl implements PhoneLocationService {
    @Resource
    private PhoneLocationMapper phoneLocationMapper;
    @Resource
    private ActionResultService actionResultService;

    /**
     * 查询
     *
     * @param phonePrefix phonePrefix
     * @return com.liuchink.chinkframe.cloud.aggregation.entity.phoneLocation.PhoneLocation
     * @author liuchink
     * @since 2022/2/16 9:30
     */
    @Override
    public ActionResult<PhoneLocation, String> getPhoneLocation(String phonePrefix) {
        if (phonePrefix.length() > 7) {
            phonePrefix = CharSequenceUtil.subPre(phonePrefix, 7);
        } else {
            return actionResultService.callBackResultFail("手机号码至少大于7位");
        }
        PhoneLocation phoneLocation = phoneLocationMapper.getPhoneLocation(phonePrefix);
        return actionResultService.callBackResult(phoneLocation);
    }

    @Override
    public QueryPageResult<PhoneLocation>
        listPhoneLocationByCondition(PhoneLocationQueryReqDTO phoneLocationQueryReqDTO) {
        try {
            PageMethod.startPage(phoneLocationQueryReqDTO.getPageNo(), phoneLocationQueryReqDTO.getPageSize(),
                phoneLocationQueryReqDTO.getCount());
            List<PhoneLocation> listPhoneLocation =
                phoneLocationMapper.listPhoneLocationByCondition(phoneLocationQueryReqDTO);
            PageInfo<PhoneLocation> pageInfo = new PageInfo<>(listPhoneLocation);
            QueryPageResult<PhoneLocation> queryPageResult = new QueryPageResult<>();
            queryPageResult.setTotal(phoneLocationQueryReqDTO.getCount() ? pageInfo.getTotal()
                : (long)listPhoneLocation.size());
            queryPageResult.getData().addAll(listPhoneLocation);
            return queryPageResult;
        } catch (Exception exception) {
            throw new CustomException(exception);
        } finally {
            PageMethod.clearPage();
        }
    }
}
