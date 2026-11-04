package com.chinaunicom.smartstorage.admin.service.impl.aggregation;

import cn.hutool.core.text.CharSequenceUtil;
import com.github.pagehelper.PageInfo;
import com.github.pagehelper.page.PageMethod;
import com.chinaunicom.smartstorage.admin.dto.aggregation.phone.PhoneLocationQueryReqDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.PhoneLocation;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.PhoneLocationMapper;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.PhoneLocationService;
import com.chinaunicom.smartstorage.common.exception.bean.CustomException;
import com.chinaunicom.smartstorage.common.result.QueryPageResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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

    /**
     * 查询
     *
     * @param phonePrefix phonePrefix
     * @return com.liuchink.chinkframe.cloud.aggregation.entity.phoneLocation.PhoneLocation
     * @author liuchink
     * @since 2022/2/16 9:30
     */
    @Override
    public PhoneLocation getPhoneLocation(String phonePrefix) {
        PhoneLocation phoneLocation = null;
        if (phonePrefix.length() > 7) {
            phoneLocation = phoneLocationMapper.getPhoneLocation(CharSequenceUtil.subPre(phonePrefix, 7));
        }
        return phoneLocation;
    }

    @Override
    public QueryPageResult<PhoneLocation>
        listPhoneLocationByCondition(PhoneLocationQueryReqDTO phoneLocationQueryReqDTO) {
        try {
            PageMethod.startPage(phoneLocationQueryReqDTO.getPageNo(), phoneLocationQueryReqDTO.getPageSize(),
                phoneLocationQueryReqDTO.getPageTotal());
            List<PhoneLocation> listPhoneLocation =
                phoneLocationMapper.listPhoneLocationByCondition(phoneLocationQueryReqDTO);
            PageInfo<PhoneLocation> pageInfo = new PageInfo<>(listPhoneLocation);
            QueryPageResult<PhoneLocation> queryPageResult = new QueryPageResult<>();
            queryPageResult.setTotal(
                phoneLocationQueryReqDTO.getPageTotal() ? pageInfo.getTotal() : (long)listPhoneLocation.size());
            queryPageResult.getData().addAll(listPhoneLocation);
            return queryPageResult;
        } catch (Exception exception) {
            throw new CustomException(exception);
        } finally {
            PageMethod.clearPage();
        }
    }
}
