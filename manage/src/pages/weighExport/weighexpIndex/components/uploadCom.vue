<template>
  <div class="upwrap">
    <div class="uppic">
      <slot name="pic"></slot>
    </div>
    <div class="upControl">
      <t-button  variant="base" theme="primary" class="upbt">
                    <template #icon><t-icon name="camera" style="margin-right: 3px;" /></template>  
                    拍照</t-button>

      <t-button  variant="base" theme="success" class="upbt">
                    <template #icon><t-icon name="upload" style="margin-right: 3px;" /></template>  
                    上传</t-button>
    </div>
    <div style="width: 100%;">
      <t-button  variant="outline" theme="danger" style="width: 100%;">
        <template #icon><t-icon name="delete" style="margin-right: 3px;" /></template>  
        删除</t-button>
    </div>
  </div>
</template>
<script setup lang="ts">
import type { SubmitContext, UploadFailContext, UploadFile } from 'tdesign-vue-next';
import { MessagePlugin } from 'tdesign-vue-next';
import { ref,defineComponent,getCurrentInstance } from 'vue';


const { proxy } = getCurrentInstance();
const emit = defineEmits(['sumt']);

const props = defineProps({
  pagestu:{},
});

defineExpose({
// submt,

})
</script>
<style lang="less" scoped>
.upwrap {
  padding:15px 0px;
  .uppic {
    background: #f5f5f5;
    border: 1px sloid #dedede;
    border-radius: 5px;
    height: 255px;
  }
  .upControl {
    padding: 15px 0px 10px;
    display: flex;
    justify-content: space-between;
    .upbt {
      width: 47%;
    }
  }
}

</style>
