import { request } from '@/utils/request';

export function listWeigh(data: Object) {
  return request.post({
    url: '/weightItem/listWeightItemByCondition',
    method: 'post',
    data
  });
}


export function listWare(data: Object) {
  return request.post({
    url: '/task/warehouseItem/listWarehouseItemByConditionOnPage',
    method: 'post',
    data
  });
}


// 添加
export function saveWeigh(data: Object) {
  return request.post({
    url: '/weightItem/saveWeightItem',
    method: 'post',
    data
  });
}


export function saveWare(data: Object) {
  return request.post({
    url: '/task/warehouseItem/saveWarehouseItem',
    method: 'post',
    data
  });
}


export function swipeCard(data: Object) {
  return request.post({
    url: '/smartCard/inSwipeCard',
    method: 'post',
    data
  });
}

export function queryTask(data: any) {
  return request.post({
    url: '/aggregation/taskflow/queryTaskFlow/' + data,
    method: 'post',
  });
}


export function detailForForm(data: any) {
  return request.post({
    url: '/weightItem/getWeightItemByDetailForInForm/' + data,
    method: 'post',
  });
}


export function detailWare(data: any) {
  return request.post({
    url: '/task/warehouseItem/getTaskWarehouse/' + data,
    method: 'post',
  });
}

export function listBalance(data: Object) {
  return request.post({
    url: '/task/balanceItem/listBalanceItemOnPage',
    method: 'post',
    data
  });
}

export function saveBalance(data: Object) {
  return request.post({
    url: '/task/balanceItem/saveBalanceItem',
    method: 'post',
    data
  });
}

export function detailBalance(data: any) {
  return request.post({
    url: '/task/balanceItem/getBalanceItemByTaskId/' + data,
    method: 'post',
  });
}

export function getTaskBalance(data: any) {
  return request.post({
    url: '/task/balanceItem/getTaskBalance/' + data,
    method: 'post',
  });
}
