import { request } from '@/utils/request';

// 入库作业单 - 列表
export function getList(data: Object) {
  return request.post({
    url: '/aggregation/taskflow/listTaskFlow',
    method: 'post',
    data
  });
}

// 入库作业单 - 详情
export function taskDetail(id: String) {
  return request.post({
    url: `/aggregation/taskflow/queryTaskFlow/${id}`,
    method: 'post',
  });
}

// 入库作业单 - 统计
export function taskStatistics() {
  return request.post({
    url: '/aggregation/taskflow/statistics',
    method: 'post',
  });
}

// 入库作业单 - 流程回退
export function taskRetrace(data) {
  return request.post({
    url: '/aggregation/taskflow/retracement',
    method: 'post',
    data
  });
}

// 入库作业单 - 流程终止
export function taskTerminated(id) {
  return request.post({
    url: `/aggregation/taskflow/terminated/${id}`,
    method: 'post',
  });
}


// tab1 - 登记信息
export function queryRegistration(taskId) {
  return request.post({
    url: `/aggregation/registration/queryRegistration/${taskId}`,
    method: 'post',
  });
}

// tab2 - 扦样信息
export function querySampleItem(taskId) {
  return request.post({
    url: `/sampleItem/getSampleItemByTaskId/${taskId}`,
    method: 'post',
  });
}


// tab3 - 质检信息
export function queryQualityItem(taskId) {
  return request.post({
    url: `/qualityItem/getQualityItemByTaskId/${taskId}`,
    method: 'post',
  });
}

// tab4 - 称重信息
// tab5 - 称皮信息
export function queryWeightItem(taskId, weightType) {
  return request.post({
    url: `/weightItem/getWeightItemByTaskId/${taskId}/${weightType}`,
    method: 'post',
  });
}

// tab5 - 值仓信息
export function queryWarehouseItem(taskId) {
  return request.post({
    url: `/task/warehouseItem/getWarehouseItemByTaskId/${taskId}`,
    method: 'post',
  });
}

// tab7 - 结算信息
export function queryBalance(taskId) {
  return request.post({
    url: `/task/balanceItem/getBalanceItemByTaskId/${taskId}`,
    method: 'post',
  });
}

// tab1 - 登出信息
export function queryRegistrationOut(taskId) {
  return request.post({
    url: `/aggregation/registration/queryRegistrationOut/${taskId}`,
    method: 'post',
  });
}