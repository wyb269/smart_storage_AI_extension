import { request } from '@/utils/request';

// 仓库管理 - 列表
export function getList(data: Object) {
  return request.post({
    url: '/warehouse/listWarehouseByConditionOnPage',
    method: 'post',
    data
  });
}

// 仓库管理 - 新增
export function insert(data: Object) {
  return request.post({
    url: '/warehouse/saveWarehouse',
    method: 'post',
    data
  });
}

// 仓库管理 - 详情
export function detail(id: any) {
  return request.post({
    url: `/warehouse/getWarehouseById/${id}`,
    method: 'post',
  });
}

// 仓库管理 - 编辑
export function update(data: any) {
  return request.post({
    url: '/warehouse/updateWarehouse',
    method: 'post',
    data
  });
}

// 仓库管理 - 删除
export function deleteApi(data: any) {
  return request.post({
    url: '/warehouse/deleteWarehouse',
    method: 'post',
    data
  });
}