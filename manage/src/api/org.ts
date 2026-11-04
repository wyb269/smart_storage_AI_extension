import { request } from '@/utils/request';

// 机构列表
export function listOrg(data:Object) {
  return request.post({
    url: '/rbac/organization/listOrganizationByConditionOnPage',
    method: 'post',
    data
  });
}

// 机构添加
export function saveOrg(data:Object) {
  return request.post({
    url: '/rbac/organization/saveOrganization',
    method: 'post',
    data
  });
}

// 机构删除
export function deleteOrg(data:any) {
  return request.post({
    url: '/rbac/organization/deleteOrganization/'+data,
    method: 'post'
  });
}

// 机构编辑
export function updateOrg(data:Object) {
  return request.post({
    url: '/rbac/organization/updateOrganization',
    method: 'post',
    data
  });
}

// 机构详情
export function orgByDetail(data:any) {
  return request.post({
    url: '/rbac/organization/getOrganizationByDetail/'+data,
    method: 'post',
  });
}

// 机构级联查询
export function orgCas(id:any,lev:any) {
  return request.post({
    url: `/rbac/organization/listOrganizationChildByIdAndLevels/${id}/${lev}`,
    method: 'post',
  });
}

export function orgFather(id:any,lev:any) {
  return request.post({
    url: `/rbac/organization/listOrganizationParentById/${id}`,
    method: 'post',
  });
}






