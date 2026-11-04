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
          <t-col :span="12">
            <t-form-item label="类型标识" name="type">
              <t-input v-model="formData.type"  placeholder="请输入" />
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
  name:[{ required: true, message: '请输入用户姓名', type: 'error' }],
  type:[{ required: true, message: '请输入类型', type: 'error' }],
  showState:[{ required: true, message: '请选择', type: 'error' }],
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

const rst=()=>{
    formData.value={}
    formValidator.value.reset()
}

const initData=(form:Object)=>{
  formData.value.id=form.id
  formData.value.name=form.name
  formData.value.remark=form.remark
  formData.value.showState=form.showState+''
  formData.value.type=form.type
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
</style>
