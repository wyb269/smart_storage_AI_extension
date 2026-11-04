import { request } from '@/utils/request';

// 出库通知单 - 列表
export function getList(data: Object) {
  return request.post({
    url: '/warehouseOutOrder/listWarehouseOutOrder',
    method: 'post',
    data
  });
}

// 出库通知单 - 新增
export function insert(data: Object) {
  return request.post({
    url: '/warehouseOutOrder/saveWarehouseOutOrder',
    method: 'post',
    data
  });
}

// 出库通知单 - 详情
export function detail(id: any) {
  return request.post({
    url: `/warehouseOutOrder/getWarehouseOutOrderById/${id}`,
    method: 'post',
  });
}

// 出库通知单 - 关闭单据
export function deleteApi(data: any) {
  return request.post({
    url: '/warehouseOutOrder/close',
    method: 'post',
    data
  });
}

// 仓库管理 - 详情
export function getWarehouseById(id: any) {
  return request.post({
    url: `/warehouse/getWarehouseById/${id}`,
    method: 'post',
  });
}

// 仓库管理 - 列表
export function getWarehouseList(data: Object) {
  return request.post({
    url: '/warehouse/listWarehouseByConditionOnPage',
    method: 'post',
    data
  });
}