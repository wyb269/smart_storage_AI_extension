import { request } from '@/utils/request';

// 用户列表
export function listUser(data:Object) {
  return request.post({
    url: '/rbac/user/listUserByConditionOnPage',
    method: 'post',
    data
  });
}

// 用户添加
export function saveUser(data:Object) {
  return request.post({
    url: '/rbac/user/saveUser',
    method: 'post',
    data
  });
}

// 用户删除
export function deleteUser(data:Object) {
  return request.post({
    url: '/rbac/user/deleteUser',
    method: 'post',
    data
  });
}

// 用户编辑
export function updateUser(data:Object) {
  return request.post({
    url: '/rbac/user/updateUser',
    method: 'post',
    data
  });
}

// 用户详情
export function userByDetail(data:any) {
  return request.post({
    url: '/rbac/user/getUserByDetail/'+data,
    method: 'post',
  });
}

// 用户重置密码
export function restPwd(data:any) {
  return request.post({
    url: '/rbac/user/rest/password/'+data,
    method: 'post',
  });
}

// 用户状态修改
export function userState(data:Object) {
  return request.post({
    url: '/rbac/user/update/state',
    method: 'post',
    data
  });
}

// 用户状态修改
export function employedStatus(data:Object) {
  return request.post({
    url: '/rbac/user/update/employedStatus',
    method: 'post',
    data
  });
}







