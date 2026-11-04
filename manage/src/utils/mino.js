// import * as Minio from 'minio'
 import * as Minio from 'minio'

let bkName='smart-storage-site'
export class minioClient {
  constructor(tempCrd){
    this.accessKey=tempCrd.accessKey
    this.secretKey=tempCrd.secretKey
    this.sessionToken=tempCrd.sessionToken
    this.endPoint='127.0.0.1'
    this.port='48098'
    this.client=new Minio.Client({
      endPoint:this.endPoint, //对象存储服务的URL
      port:this.port, //端口号
      useSSL: false, //true代表使用HTTPS
      accessKey:this.accessKey, //账户id
      secretKey:this.secretKey, //密码
    })
  }
  uploadImg(file){
    return new Promise((resolve,reject)=>{
          this.client.putObject(
      bkName,
      '9871252211.jpg',
      file,
      file.size,
      {'contentType':file.type},(err,etag)=>{
        if(err){
          alert(123)
        }else{
          resolve({etag})
        }
      }
    )

    })

  }
}


