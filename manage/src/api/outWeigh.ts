import { request } from '@/utils/request';

// 出库 - 检斤称重列表
export function listOutWeigh(data: Object) {
  return request.post({
    url: '/weightItem/listWeightItemByCondition',
    method: 'post',
    data
  });
}

// 出库 - 检斤称重保存
export function saveOutWeigh(data: Object) {
  return request.post({
    url: '/weightItem/saveWeightItem',
    method: 'post',
    data
  });
}

// 出库 - 刷卡
export function outSwipeCard(data: Object) {
  return request.post({
    url: '/smartCard/outSwipeCard',
    method: 'post',
    data
  });
}

// 出库 - 查询任务流
export function outQueryTask(data: any) {
  return request.post({
    url: '/aggregation/taskflow/queryTaskFlow/' + data,
    method: 'post',
  });
}

// 出库 - 检斤称重详情
export function outDetailForForm(data: any) {
  return request.post({
    url: '/weightItem/getWeightItemByDetailForOutForm/' + data,
    method: 'post',
  });
}

// 出库 - 值仓管理列表（用户当天值仓列表）
export function listOutWare(data: Object) {
  return request.post({
    url: '/task/warehouseItem/listWarehouseItemOnPage',
    method: 'post',
    data
  });
}

// 出库 - 值仓管理保存（添加）
export function saveOutWare(data: Object) {
  return request.post({
    url: '/task/warehouseItem/saveWarehouseItem',
    method: 'post',
    data
  });
}

// 出库 - 值仓管理详情（出库值仓回显）
export function outDetailWare(data: any) {
  return request.post({
    url: '/task/warehouseItem/getTaskWarehouseOut/' + data,
    method: 'post',
  });
}

// 出库 - 结算列表（用户当天结算列表）
export function listOutBalance(data: Object) {
  return request.post({
    url: '/task/balanceItem/listBalanceItemOnPage',
    method: 'post',
    data
  });
}

// 出库 - 结算保存（出库添加）
export function saveOutBalance(data: Object) {
  return request.post({
    url: '/task/balanceItem/saveBalanceItemOut',
    method: 'post',
    data
  });
}

// 出库 - 结算回显
export function getOutTaskBalance(data: any) {
  return request.post({
    url: '/task/balanceItem/getTaskBalance/' + data,
    method: 'post',
  });
}
