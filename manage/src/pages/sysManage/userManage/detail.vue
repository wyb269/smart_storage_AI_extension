<template>
<div>
  <input ref="input" type="file" @change="handleFiles" />
  <img src="http://127.0.0.1:48097/smart-storage-site/1234.png">
  <img :src="purl">
   <div @click="down()">下载图片</div>
</div>
</template>
<script setup>
import { ref ,onMounted,getCurrentInstance} from 'vue';
import { createMinioClient, fileToStream } from 'tz-minio-upload_beta'
const { proxy } = getCurrentInstance();
const purl=ref('')
onMounted(()=>{
})

let internalInstance = getCurrentInstance();
let uploadPic = internalInstance.appContext.config.globalProperties.$uploadPic;

function handleFiles(event) {
	try {
		var file = event.target.files[0];
    proxy.$uploadPic(file).then(r=>{
      if(r.objectKey){
        console.log('上传成功')
      }else{
        console.log('上传失败')
      }
    })
	} catch (error) {
		alert('cc')
	}
}
</script>

<style scoped>
 
</style>