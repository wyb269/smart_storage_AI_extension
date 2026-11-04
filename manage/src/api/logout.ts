import { request } from "@/utils/request";
//登记列表
export function getRegistrationList(params: any) {
  return request.post({
    url: '/registration/listRegistration',
    data: params,
    method: 'post',
  })
}

//登记信息
export function queryRegistrationById(id: string, params: any = {}) {
  return request.post({
    url: `/aggregation/registration/queryRegistration/${id}`,
    data: params,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    method: 'post',
  })
}

//登出
export function outRegistration(params: any) {
  return request.post({
    url: '/aggregation/registration/outRegistration',
    data: params,
    headers: {
      'Content-Type': 'application/json'
    }
  });
}

//登出列表
export function queryTodayLogoutRecords(params: any) {
  return request.post({
    url: '/aggregation/registration/listLogout',
    data: params,
    headers: {
      'Content-Type': 'application/json'
    }
  });
}