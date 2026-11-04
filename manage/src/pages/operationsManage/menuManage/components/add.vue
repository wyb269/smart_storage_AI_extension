<template>
  <t-form
    ref="formValidator"
    class="base-form"
    :data="formData"
    :rules="FORM_RULES"
    :label-width="90"
    @reset="onReset"
    @submit="onSubmit"
  >
    <div class="form-basic-container">
      <div class="form-basic-item">
        <!-- 表单内容 -->

        <!-- 合同名称,合同类型 -->
        <t-row class="row-gap" :gutter="[0, 24]">
          <t-col :span="12">
            <t-form-item label="名称" name="name">
              <t-input v-model="formData.name"  placeholder="请输入" />
            </t-form-item>
          </t-col>
          <t-col :span="12" >
            <t-form-item label="openUrl" name="openUrl" >
              <t-input v-model="formData.openUrl"  placeholder="请输入" />
            </t-form-item>
          </t-col>

          <t-col :span="12" >
            <t-form-item label="隐藏" name="isDisplay" >
              <t-select
                v-model="formData.isDisplay"
              
                class="demo-select-base"
                placeholder="请选择"
                clearable
              >
                <t-option  :value="1" label="显示"> </t-option>
                <t-option  :value="2" label="隐藏"> </t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="12" >
            <t-form-item label="排序" name="orderNum">
              <t-input-number v-model="formData.orderNum"  placeholder="请输入" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="图标">
              <t-input v-model="formData.icon"  placeholder="请输入" />
            </t-form-item>
          </t-col>
        </t-row>

      </div>
    </div>

  </t-form>
</template>
<script setup lang="ts">
import type { SubmitContext, UploadFailContext, UploadFile } from 'tdesign-vue-next';
import { MessagePlugin } from 'tdesign-vue-next';
import { ref,defineComponent,defineExpose,getCurrentInstance } from 'vue';
import { template } from 'lodash';


const { proxy } = getCurrentInstance();
const emit = defineEmits(['sumt']);

const props = defineProps({
  pagestu:{},
});
const formData = ref({});
const FORM_RULES=ref({
  name:[{ required: true, message: '请输入', type: 'error' }],
  openUrl:[{ required: true, message: '请输入', type: 'error' }],
  orderNum:[{ required: true, message: '请输入', type: 'error' }],
  isDisplay:[{ required: true, message: '请输入', type: 'error' }],
})

const formValidator = ref(null);
const submt=()=>{
  formValidator.value.submit()
}
const onSubmit = (ctx: SubmitContext) => {
  if (ctx.validateResult === true) {
    emit('sumt',formData.value)
  }
};
defineOptions({
  name: 'FormBase',
});
const rst=(data)=>{
    formData.value={}
    formData.value.isFrame='2'
    formData.value.parentId=data.id
    formData.value.type=data.type
    console.log(props.pagestu,data)
    let ary=[]
    data.children.map((x:any)=>{
      x.id=x.id*1;
      ary.push(x.id)
    })
    if(ary.length==0){
      if(data.type==1)formData.value.id=data.id+'01'
      if(data.type==2)formData.value.id='99'+data.id+'01'
    }else{
      const maxValue = Math.max(...ary);
      formData.value.id=maxValue+1
    }
    formValidator.value.reset()
}

const initData=(form:Object)=>{
  formData.value.parentId=form.parentId
  formData.value.id=form.id
  formData.value.name=form.name
  formData.value.openUrl=form.openUrl
  formData.value.isFrame=form.isFrame
  formData.value.type=form.type
  formData.value.isDisplay=form.isDisplay
  formData.value.orderNum=form.orderNum
  formData.value.icon=form.icon
}



const onReset = () => {

};
defineExpose({
submt,
rst,
initData
})
</script>
<style lang="less" scoped>
@import './index.less';
.form-basic-container {
  padding-top: 30px;
  padding-bottom: 30px;
}

:deep(.t-input-number .t-button) {
  height:36px
}

:deep(.t-input-number .t-input) {
  border-radius: 0;
}
</style>
