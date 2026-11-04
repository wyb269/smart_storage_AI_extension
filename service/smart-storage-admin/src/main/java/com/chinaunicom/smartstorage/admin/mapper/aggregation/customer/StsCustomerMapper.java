package com.chinaunicom.smartstorage.admin.mapper.aggregation.customer;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chinaunicom.smartstorage.admin.dto.aggregation.customer.CustomerQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.customer.CustomerQueryRespDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.customer.StsCustomer;

import java.util.List;

public interface StsCustomerMapper extends BaseMapper<StsCustomer> {
    List<CustomerQueryRespDTO> listCustomerByCondition(CustomerQueryReqDTO customerQueryReqDTO);
}
