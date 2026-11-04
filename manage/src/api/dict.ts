import { request } from '@/utils/request';

// 类型列表
export function listDict(data:Object) {
  return request.post({
    url: '/aggregation/dictionary/listDictTypeByConditon',
    method: 'post',
    data
  });
}

// 类型添加
export function saveDict(data:Object) {
  return request.post({
    url: '/aggregation/dictionary/saveDictType',
    method: 'post',
    data
  });
}

// 类型删除
export function deleteDict(data:any) {
  return request.post({
    url: '/aggregation/dictionary/deleteDictType/'+data,
    method: 'post',
  });
}

// 类型编辑
export function updateDict(data:Object) {
  return request.post({
    url: '/aggregation/dictionary/updateDictType',
    method: 'post',
    data
  });
}



// 数据列表
export function listDictD(data:Object) {
  return request.post({
    url: '/aggregation/dictionary/listDictDataByConditon',
    method: 'post',
    data
  });
}

// 数据添加
export function saveDictD(data:Object) {
  return request.post({
    url: '/aggregation/dictionary/saveDictData',
    method: 'post',
    data
  });
}

// 数据删除
export function deleteDictD(data:any) {
  return request.post({
    url: '/aggregation/dictionary/deleteDictData/'+data,
    method: 'post',
  });
}

// 数据编辑
export function updateDictD(data:Object) {
  return request.post({
    url: '/aggregation/dictionary/updateDictData',
    method: 'post',
    data
  });
}


// 配置根据字段查询
export function getSystemConfigByGroupAndKey(data:Object) {
  return request.post({
    url: '/aggregation/systemConfig/getSystemConfigByGroupAndKey',
    method: 'post',
    data
  });
}


// 配置列表查询
export function listSystemConfigByCondition(data:Object) {
  return request.post({
    url: '/aggregation/systemConfig/listSystemConfigByCondition',
    method: 'post',
    data
  });
}

// 配置修改
export function updateSystemConfig(data:Object) {
  return request.post({
    url: '/aggregation/systemConfig/updateSystemConfig',
    method: 'post',
    data
  });
}

// 操作日志
export function listOperationLogRecordByCondition(data:Object) {
  return request.post({
    url: '/aggregation/operationLogRecord/listOperationLogRecordByCondition',
    method: 'post',
    data
  });
}


export function typeExportExcel(data:any) {
  return request.post({
    url: '/aggregation/dictionary/typeExportExcel',
    method: 'post',
    'responseType':'blob',
    data
  });
}



export function typeImportExcel(data:any) {
  return request.post({
    url: '/aggregation/dictionary/typeImportExcel',
    method: 'post',
    upload:1,
    data
  });
}













