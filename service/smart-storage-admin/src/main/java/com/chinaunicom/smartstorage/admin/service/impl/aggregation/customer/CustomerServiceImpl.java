package com.chinaunicom.smartstorage.admin.service.impl.aggregation.customer;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinaunicom.smartstorage.admin.dto.aggregation.customer.CustomerQueryReqDTO;
import com.chinaunicom.smartstorage.admin.dto.aggregation.customer.CustomerQueryRespDTO;
import com.chinaunicom.smartstorage.admin.entity.aggregation.customer.StsCustomer;
import com.chinaunicom.smartstorage.admin.entity.task.TaskFlow;
import com.chinaunicom.smartstorage.admin.mapper.aggregation.customer.StsCustomerMapper;
import com.chinaunicom.smartstorage.admin.mapper.task.taskflow.TaskFlowMapper;
import com.chinaunicom.smartstorage.admin.service.inter.aggregation.customer.CustomerService;
import com.chinaunicom.smartstorage.common.result.ActionResult;
import com.chinaunicom.smartstorage.common.result.ActionResultService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Slf4j
@Service
public class CustomerServiceImpl implements CustomerService {
    @Resource
    private ActionResultService actionResultService;
    @Resource
    private StsCustomerMapper customerMapper;
    @Resource
    private TaskFlowMapper taskFlowMapper;
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> saveCustomer(StsCustomer stsCustomer) {
        StsCustomer customerByName = customerMapper.selectOne(Wrappers.lambdaQuery(StsCustomer.class).eq(StsCustomer::getName,stsCustomer.getName()));
        if(customerByName!=null && !StrUtil.equals(customerByName.getId(),stsCustomer.getId())){
            return actionResultService.callBackResultFail("客户名重复");
        }
        StsCustomer customerByCard = customerMapper.selectOne(Wrappers.lambdaQuery(StsCustomer.class).eq(StsCustomer::getCard,stsCustomer.getCard()));
        if(customerByCard!=null && !StrUtil.equals(customerByCard.getId(),stsCustomer.getId())){
            return actionResultService.callBackResultFail("身份证号重复");
        }
        customerMapper.insert(stsCustomer);
        return actionResultService.callBackResult("添加成功");
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> updateCustomer(StsCustomer stsCustomer) {
        StsCustomer customerByName = customerMapper.selectOne(Wrappers.lambdaQuery(StsCustomer.class).eq(StsCustomer::getName,stsCustomer.getName()));
        if(customerByName!=null && !StrUtil.equals(customerByName.getId(),stsCustomer.getId())){
            return actionResultService.callBackResultFail("客户名重复");
        }
        StsCustomer customerByCard = customerMapper.selectOne(Wrappers.lambdaQuery(StsCustomer.class).eq(StsCustomer::getCard,stsCustomer.getCard()));
        if(customerByCard!=null && !StrUtil.equals(customerByCard.getId(),stsCustomer.getId())){
            return actionResultService.callBackResultFail("身份证号重复");
        }
        customerMapper.updateById(stsCustomer);
        return actionResultService.callBackResult("修改成功");
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ActionResult<String, String> deleteCustomer(List<StsCustomer> customerList) {
        for(StsCustomer customer: customerList){
            Long taskFlowCount = taskFlowMapper.selectCount(Wrappers.lambdaQuery(TaskFlow.class).eq(TaskFlow::getCustomerId,customer.getId()));
            if(taskFlowCount != 0 ){
                return actionResultService.callBackResultFail("客户已绑定作业单，无法删除");
            }
        }
        customerMapper.deleteByIds(customerList);
        return actionResultService.callBackResult("删除成功");
    }

    @Override
    public StsCustomer getCustomerById(String id) {
        return customerMapper.selectById(id);
    }

    @Override
    public List<CustomerQueryRespDTO> listCustomerByCondition(CustomerQueryReqDTO customerQueryReqDTO) {
        return customerMapper.listCustomerByCondition(customerQueryReqDTO);
    }


}
