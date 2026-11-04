import { request } from '@/utils/request';
import axios from 'axios';

// 获取验证码
export function getCodeImg() {
  return request.post({
    url: '/sso/admin/default/captcha/getVerCode',
    method: 'post',
  });
}
// 登录方法
export function login(data:Object) {
  return request.post({
    url: '/sso/admin/default/loginByPassword',
    method: 'post',
    data
  });
}

// 获取用户信息
export function getInfo() {
  return request.post({
    url: '/sso/admin/default/getLoginBaseInfo',
    method: 'post',
  });
}

// 获取用户菜单
export function getMenu() {
  return request.post({
    url: '/sso/admin/default/getLoginMenuInfo',
    method: 'post',
  });
}




// 修改用户密码
export function changePwd(data:any) {
  return request.post({
    url: '/sso/admin/default/updateUserLoginPassword',
    method: 'post',
    data
  });
}

export function lgchangePwd(data:any) {
  return request.post({
    url: '/sso/admin/default/updateUserLoginPasswordByLoginId',
    method: 'post',
    data
  });
}





// 根据名字查询行政区
export function listRegionByName(data:any) {
  return request.post({
    url: '/rbac/region/listRegionByName',
    method: 'post',
    data
  });
}

export function listRegionById(data:any) {
  return request.post({
    url: '/rbac/region/getRegionById/'+data,
    method: 'post',
  });
}

export function listRegionChildByIdAndLevels(id:any,levels:any) {
  return request.post({
    url: `/rbac/region/listRegionChildByIdAndLevels/${id}/${levels}`,
    method: 'post',
  });
}

export function rgFather(id:any) {
  return request.post({
    url: `/rbac/region/listRegionParentById/${id}`,
    method: 'post',
  });
}

export function minoCrd() {
  return request.post({
    url: `/aggregation/minio/temporaryCredential/getCredentials`,
    method: 'post',
  });
}

export function upGetUrl(domain:any,fileName:any) {
  return request.get({
    url: `/file/s3/presign-upload?domain=${domain}&fileName=${fileName}`,
    method: 'get',
  });
}

export function uploadPic(file:any,model:string = "default",) {
      return new Promise((resolve,reject)=>{
        upGetUrl(model,file.name).then(res=>{
          axios.put(res.result.url,file, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
          }).then(r => {
            resolve({objectKey:res.result.objectKey})
          })
          .catch(error => {
            console.error('Error uploading file:', error);
            resolve({objectKey:''})
          });
        }).catch(error => {
            resolve({objectKey:''})
        });
    })
}


export function upGetPic(objectKey:any) {
  return request.get({
    url: `/file/s3/presign-download?objectKey=${objectKey}`,
    method: 'get',
  });
}

export function getPic(obj:any){
  let data=upGetPic(obj).then(res=> {
  if(res.status.code=='200'){
      return data=res.status.message      
    }else{
      return data=''
    }
  }).catch(function (error) {
    return data=''
  })
  return data

}





