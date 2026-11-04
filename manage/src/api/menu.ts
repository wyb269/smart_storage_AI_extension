import { request } from '@/utils/request';

// 列表
export function listMenu(data:Object) {
  return request.post({
    url: '/rbac/menu/listMenu',
    method: 'post',
    data
  });
}

// 添加
export function saveMenu(data:Object) {
  return request.post({
    url: '/rbac/menu/saveMenu',
    method: 'post',
    data
  });
}

// 删除
export function deleteMenu(data:any) {
  return request.post({
    url: '/rbac/menu/deleteMenu/'+data,
    method: 'post'
  });
}

// 编辑
export function updateMenu(data:Object) {
  return request.post({
    url: '/rbac/menu/updateMenu',
    method: 'post',
    data
  });
}







