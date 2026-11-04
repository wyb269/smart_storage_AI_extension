import { request } from '@/utils/request';

// 角色组列表
export function listRoleGp(data:Object) {
  return request.post({
    url: '/rbac/roleGroup/listRoleGroupByConditionOnPage',
    method: 'post',
    data
  });
}

// 角色组添加
export function saveRoleGp(data:Object) {
  return request.post({
    url: '/rbac/roleGroup/saveRoleGroup',
    method: 'post',
    data
  });
}

// 角色组删除
export function deleteRoleGp(data:any) {
  return request.post({
    url: '/rbac/roleGroup/deleteRoleGroup/'+data,
    method: 'post'
  });
}

// 角色组编辑
export function updateRoleGp(data:Object) {
  return request.post({
    url: '/rbac/roleGroup/updateRoleGroup',
    method: 'post',
    data
  });
}

// 角色组详情
export function RoleGpByDetail(data:any) {
  return request.post({
    url: '/rbac/roleGroup/getRoleGroupByDetail/'+data,
    method: 'post',
  });
}

export function RoleList(data:any) {
  return request.post({
    url: '/rbac/role/listRoleByConditionOnPage',
    method: 'post',
    data
  });
}














