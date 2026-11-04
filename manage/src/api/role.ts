import { request } from '@/utils/request';

// 角色列表
export function listRole(data:Object) {
  return request.post({
    url: '/rbac/role/listRoleByConditionOnPage',
    method: 'post',
    data
  });
}

// 角色添加
export function saveRole(data:Object) {
  return request.post({
    url: '/rbac/role/saveRole',
    method: 'post',
    data
  });
}

// 角色删除
export function deleteRole(data:any) {
  return request.post({
    url: '/rbac/role/deleteRole/'+data,
    method: 'post'
  });
}

// 角色编辑
export function updateRole(data:Object) {
  return request.post({
    url: '/rbac/role/updateRole',
    method: 'post',
    data
  });
}

// 角色详情
export function RoleByDetail(data:any) {
  return request.post({
    url: '/rbac/role/getRoleByDetail/'+data,
    method: 'post',
  });
}


// 角色权限设置
export function roleRights(data:Object) {
  return request.post({
    url: '/rbac/role/updateRoleByIdForRights',
    method: 'post',
    data
  });
}

// 角色权限获取
export function roleRightsGet(data:any) {
  return request.post({
    url: '/rbac/role/listRoleByIdForRights/'+data,
    method: 'post',
  });
}


// 角色菜单设置
export function roleMenu(data:Object) {
  return request.post({
    url: '/rbac/role/updateRoleByIdForMenu',
    method: 'post',
    data
  });
}

// 角色菜单获取
export function roleMenuGet(data:any) {
  return request.post({
    url: '/rbac/role/listRoleByIdForMenu/'+data,
    method: 'post',
  });
}
















