import { request } from "@/utils/request";

export function saveRegistration(params: any) {
  return request.post({
    url: '/aggregation/registration/saveRegistration',
    data: params,
    headers: {
      'Content-Type': 'application/json'
    },
  });
}

export function getRegisterList(params: any) {
  return request.post({
    url: '/aggregation/registration/listRegistration',
    data: params,
    headers: {
      'Content-Type': 'application/json'
    }
  });
}