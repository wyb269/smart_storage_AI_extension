package com.chinaunicom.smartstorage.admin.service.inter.aggregation.customer;

import com.chinaunicom.smartstorage.admin.dto.aggregation.customer.CustomerQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.customer.CustomerQueryRespDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.customer.StsCustomer;
import com.chinaunicom.smartstorage.common.result.ActionResult;

import java.util.List;

public interface CustomerService {
    ActionResult<String, String> saveCustomer(StsCustomer stsCustomer);

    ActionResult<String, String> updateCustomer(StsCustomer stsCustomer);

    ActionResult<String, String> deleteCustomer(List<StsCustomer> customerList);

    StsCustomer getCustomerById(String id);

    List<CustomerQueryRespDTO> listCustomerByCondition(CustomerQueryReqDTO customerQueryReqDTO);
}
