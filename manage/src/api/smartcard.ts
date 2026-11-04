import { request } from "@/utils/request";

//智能卡列表
export function listSmartCardByPhysicalCard(physicalCardNumber: string) {
  const requestData = {
    smartCardQueryReqDTO: {
      physicalCardNumber,
    }
  };

  return request.post({
    url: '/smartCard/listSmartCardByCondition',
    data: requestData,
    method: 'post',
  });
}

//添加智能卡
export function saveSmartCard(physicalCardNumber: string) {
  return request.post({
    url: '/smartCard/saveSmartCard',
    data: { physicalCardNumber },
    method: 'post',
  });
}

//删除智能卡
export function deleteSmartCard(ids: string | string[]) {
  return request.post({
    url: '/smartCard/deleteSmartCard',
    data: {
      ids: Array.isArray(ids) ? ids : [ids]
    },
    method: 'post',
  });
}


//读取智能卡
export function swipeCard(params: any) {
  return request.post({
    url: '/smartCard/inSwipeCard',
    data: params,
    method: 'post',
  });
}