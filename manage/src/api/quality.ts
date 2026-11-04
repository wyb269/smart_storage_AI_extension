import { request } from '@/utils/request';

// 质量检验 - 列表
export function getList(data: Object) {
  return request.post({
    url: '/qualityItem/listQualityItemByConditionOnPage',
    method: 'post',
    data
  });
}


// 质量检验 - 添加
export function insert(data: Object) {
  return request.post({
    url: '/qualityItem/saveQualityItem',
    method: 'post',
    data
  });
}

// 仓库弹窗列表（按粮食品种过滤）
export function listWarehouseByModal(data: Object) {
  return request.post({
    url: '/warehouse/listWarehouseByModal',
    method: 'post',
    data
  });
}