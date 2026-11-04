import { request } from '@/utils/request';

// 入库通知单 - 列表
export function getList(data: Object) {
  return request.post({
    url: '/warehouseInOrder/listWarehouseInOrder',
    method: 'post',
    data
  });
}

// 入库通知单 - 新增
export function insert(data: Object) {
  return request.post({
    url: '/warehouseInOrder/saveWarehouseInOrder',
    method: 'post',
    data
  });
}

// 入库通知单 - 详情
export function detail(id: any) {
  return request.post({
    url: `/warehouseInOrder/getWarehouseInOrderById/${id}`,
    method: 'post',
  });
}

// 入库通知单 - 关闭单据
export function deleteApi(data: any) {
  return request.post({
    url: '/warehouseInOrder/close',
    method: 'post',
    data
  });
}