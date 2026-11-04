<template>
<div>
  <input ref="input" type="file" @change="handleFiles" />
  <img src="http://127.0.0.1:48097/smart-storage-site/1234.png">
   <div @click="down()">下载图片</div>
</div>
</template>
<script setup>
import { ref ,onMounted,getCurrentInstance} from 'vue';
import { createMinioClient, fileToStream } from 'tz-minio-upload_beta'
import { minoCrd } from '@/api/login';
const minioClient =ref()

onMounted(()=>{
  minoCrd().then(r=>{
    console.log(r.result)
    crd.value=r.result

    minioClient.value=createMinioClient({

        endPoint: '127.0.0.1', //对象存储服务的URL
        port: 48097, //端口号
        useSSL: false, //true代表使用HTTPS
        accessKey:crd.value.accessKey, //账户id
        secretKey:crd.value.secretKey, //密码
        bucketName: ' smart-storage-site',
        sessionToken:crd.value.sessionToken
			});


    // minioClient=new Minio.Client({
    //     endPoint: '127.0.0.1', //对象存储服务的URL
    //     port: 48098, //端口号
    //     useSSL: false, //true代表使用HTTPS
    //     accessKey:crd.value.accessKey, //账户id
    //     secretKey:crd.value.secretKey, //密码
    //   });


  })

//  console.log( minioClient.value)
})

const crd=ref()
 
function handleFiles(event) {
	try {
		var file = event.target.files[0];



    fileToStream(file, (data) => {
                let buf = data._readableState.buffer.head.data
                minioClient.value.putObject('smart-storage-site','1234.png', buf, (err, data) => {
                        if (err) {
                            resolve(null)
                            return
                        }
                        console.log(data)
                        // resolve(data)
                    }
                )
            })


    
//  m.value.putObject('smart-storage-site','1234',
//       f,
//     //   f.size,
//     //  {
//     //   'contentType':f.type,
//     //   'Authorization':"Bearer "+crd.value.sessionToken
//     //   }, 
//       callback


// 		)



    return
		let reader = new FileReader();
		reader.readAsArrayBuffer(f);
		reader.onload = function (e) {
			let res = e.target.result; //ArrayBuffer
			//初始化
			// initMinio({

      //   endPoint: '127.0.0.1', //对象存储服务的URL
      //   port: 48097, //端口号
      //   useSSL: false, //true代表使用HTTPS
      //   accessKey:crd.value.accessKey, //账户id
      //   secretKey:crd.value.secretKey, //密码
      //   bucketName:'smart-storage-site',
      //   sessionToken:crd.value.sessionToken
			// });
			//上传,callback是回调函数
			// putObject('smart-storage-site', res, f.name,{
      // 'contentType':f.type,
      // 'Authorization':"Bearer "+crd.value.sessionToken
      // }, callback);
console.log( f.name)

 m.value.putObject('smart-storage-site',
          
      '1234',
      res,
    //   f.size,
    //  {
    //   'contentType':f.type,
    //   'Authorization':"Bearer "+crd.value.sessionToken
    //   }, 
      callback);


		};
	} catch (error) {
		alert('cc')
	}
}
 
function callback(err, data) {
	if (err) {
    console.log(err)
	  alert('ee')
	} else {
    console.log(err)
    console.log(data)
		alert('ss')
	}
}


const down=()=>{
  		// 	initMinio({

      //   endPoint: '127.0.0.1', //对象存储服务的URL
      //   port: 48097, //端口号
      //   useSSL: false, //true代表使用HTTPS
      //   accessKey:crd.value.accessKey, //账户id
      //   secretKey:crd.value.secretKey, //密码
      //   bucketName:'smart-storage-site',
      //   sessionToken:crd.value.sessionToken
			// });



       minioClient.value.getObject('smart-storage-site','1234.png', (err,  dataStream) => {
        console.log(err)
        console.log(dataStream.url)

        console.log(dataStream)
                let size = 0;
                let file = [];
        
                dataStream.on('data', chunk=>file.push(chunk))


                //  {
                //   size += chunk.length
                //   console.log(chunk);
                //   let aaa = Array.from(chunk)
                //   file = file.concat(aaa);
                // })
                dataStream.on('end', function () {


                            const blob = new Blob(file,{type:'image/*'})
          const blobUrl = URL.createObjectURL(blob)

          console.log(blobUrl)
          const a = document.createElement('a')
          a.setAttribute('href', blobUrl)
        a.setAttribute('download', '1234.png')
          a.click()
                 
                })
                dataStream.on('error', function (err) {
                  resolve(false)
                })


            // if (err || result.statusCode != 200) {

            //     resolve(null)
            //     return
            // }
            // resolve(result.url)
        })
      // getobject('smart-storage-site','1234', (err, data) => {

      //   if (err) {

      //      alert(111)

      //   } else {
      //      alert(222)
      //   }
      //   })
}

let etag="7d8b41b525ed759489f9bc82d0e3a3c7"
</script>

<style scoped>
 
</style>