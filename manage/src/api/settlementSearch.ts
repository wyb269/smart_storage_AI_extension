import { request } from '@/utils/request';
import { getDict } from '@/api/sampling.ts';

// 结算查询列表
export function getList(data: Object) {
  return request.post({
    url: '/Report/settlementReport',
    method: 'post',
    data
  });
}

// 获取结算详情
export function getDetail(id: string) {
  return request.post({
    url: `/Report/settlementReportQuery/${id}`,
    method: 'post',
  });
}

// 获取粮食品种
export function getGrainVariety() {
  return getDict('GRAIN_VARIETY');
}


// 导出
export function excelExport(data) {
  return request.post({
    url: '/excelExport/export',
    method: 'post',
    data,
    responseType: 'blob',
  }, {
    isReturnNativeResponse: true,
    isTransformResponse: false,
  });
}
