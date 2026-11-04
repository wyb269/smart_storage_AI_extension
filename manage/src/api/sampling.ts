import { request } from '@/utils/request';

// 扦样信息 - 列表
export function getList(data: Object) {
  return request.post({
    url: '/sampleItem/listSampleItemByConditionOnPage',
    method: 'post',
    data
  });
}


// 扦样信息 - 新增
export function insert(data: Object) {
  return request.post({
    url: '/sampleItem/saveSampleItem',
    method: 'post',
    data
  });
}

// 扦样信息 - 根据taskId查询
export function getSampleItemByTaskId(taskId: String) {
  return request.post({
    url: `/sampleItem/getSampleItemByTaskId/${taskId}`,
    method: 'post',
  });
}

// 粮食品种 - 根据taskId查询入库作业单
export function getWarehouseInOrderByTaskId(taskId: String) {
  return request.post({
    url: `/warehouseInOrder/getWarehouseInOrderByTaskId/${taskId}`,
    method: 'post',
  });
}

// 字典 - 根据类型查询
export function getDict(type: String) {
  return request.post({
    url: `/aggregation/dictionary/getDictTypeByTypeWithDictData/${type}`,
    method: 'post',
  });
}

// 读卡
export function swipeCard(data: Object) {
  return request.post({
    url: '/smartCard/inSwipeCard',
    method: 'post',
    data
  });
}


// 读卡
export function getTaskById(id: String) {
  return request.post({
    url: `/aggregation/taskflow/queryTaskFlow/${id}`,
    method: 'post',
  });
}

