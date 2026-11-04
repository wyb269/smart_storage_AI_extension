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
            <t-form-item label="名称" name="label">
              <t-input v-model="formData.label"  placeholder="请输入" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="键值" name="description">
              <t-input v-model="formData.description"  placeholder="请输入" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="cssClass">
              <t-input v-model="formData.cssClass"  placeholder="请输入" />
            </t-form-item>
          </t-col>
         <t-col :span="12">
            <t-form-item label="listClass">
              <t-input v-model="formData.listClass"  placeholder="请输入" />
            </t-form-item>
          </t-col>

          


          <t-col :span="12">
            <t-form-item label="排序" name="sort">
              <t-input-number v-model="formData.sort"  placeholder="请输入" />
            </t-form-item>
          </t-col>

          <t-col :span="12">
            <t-form-item label="是否默认" name="isDefault">
              <t-select
                v-model="formData.isDefault"
               
                class="demo-select-base"
                placeholder="请选择"
                clearable
                filte
                >
                <t-option value="1" label="是"></t-option>
                <t-option value="2" label="否"></t-option>
              </t-select>

            </t-form-item>
          </t-col>

          <t-col :span="12">
            <t-form-item label="状态" name="showState">
              <t-select
                v-model="formData.showState"
           
                class="demo-select-base"
                placeholder="请选择"
                clearable
                filte
                >
                <t-option value="1" label="启用"></t-option>
                <t-option value="2" label="禁用"></t-option>
              </t-select>

            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="备注">
              <t-textarea v-model="formData.remark" :height="124" placeholder="请输入" />
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
import regionSelect from '@/components/regionSelect.vue';
import orgSelect from '@/components/orgSelect.vue';
import router from '@/router';


const { proxy } = getCurrentInstance();
const emit = defineEmits(['sumt']);

const props = defineProps({
  pagestu:{},
  roleGroupData:{
    type:Array,
    default:[]
  },
});

const FORM_RULES=ref({
  label:[{ required: true, message: '请输入', type: 'error' }],
  sort:[{ required: true, message: '请输入', type: 'error' }],
  description:[{ required: true, message: '请输入类型', type: 'error' }],
  showState:[{ required: true, message: '请选择', type: 'error' }],
  isDefault:[{ required: true, message: '请选择', type: 'error' }],
  remark:[{ required: true, message: '请输入类型', type: 'error' }],
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

const rst=(type)=>{
    formData.value={}
    formData.value.type=type
    formValidator.value.reset()
}

const initData=(form:Object)=>{
  formData.value.id=form.id
  formData.value.label=form.label
  formData.value.remark=form.remark
  formData.value.showState=form.showState+''
  formData.value.type=form.type
  formData.value.sort=form.sort
  formData.value.cssClass=form.cssClass
  formData.value.listClass=form.listClass
  formData.value.description=form.description
  formData.value.isDefault=form.isDefault+''
}

const formData = ref({});

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
