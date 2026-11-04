import { request } from '@/utils/request';

// 搜索栏
// 仓库名称
export function granaryList(data: Object) {
  return request.post({
    url: '/warehouse/listWarehouseByConditionOnPage',
    method: 'post',
    data
  });
}

// 出入库报表查询 - 列表
export function getList(data: Object) {
  return request.post({
    url: '/Report/registrationReport',
    method: 'post',
    data
  });
}