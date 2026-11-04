<template>
  <div>
    <div>
      <div>我是一个首页</div>
      <readCard @result="result" />
    </div>
  </div>
</template>
<script setup lang="jsx">
import readCard from '@/components/readCard/index.vue';
const result=r=>{
  console.log(r)
//  r.result  0失败   1成功
}

</script>
<style lang="less" scoped></style>
