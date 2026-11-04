import { request } from '@/utils/request';

//出库读取智能卡
export function swipeCard(params: any) {
  return request.post({
    url: '/smartCard/outSwipeCard',
    data: params,
    method: 'post',
  });
}

//出库登记
export function saveRegistration(params: any) {
  return request.post({
    url: '/aggregation/registration/saveRegistrationOut',
    data: params,
    headers: {
      'Content-Type': 'application/json'
    },
  });
}