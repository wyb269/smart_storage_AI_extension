import { request } from '@/utils/request';

// 客户管理 - 列表
export function getList(data: Object) {
  return request.post({
    url: '/customer/listCustomerByConditionOnPage',
    method: 'post',
    data
  });
}

// 客户管理 - 新增
export function insert(data: Object) {
  return request.post({
    url: '/customer/saveCustomer',
    method: 'post',
    data
  });
}

// 客户管理 - 详情
export function detail(id) {
  return request.post({
    url: `/customer/getCustomerById/${id}`,
    method: 'post',
  });
}

// 客户管理 - 编辑
export function update(data) {
  return request.post({
    url: '/customer/updateCustomer',
    method: 'post',
    data
  });
}

// 客户管理 - 删除
export function deleteApi(data) {
  return request.post({
    url: '/customer/deleteCustomer',
    method: 'post',
    data
  });
}