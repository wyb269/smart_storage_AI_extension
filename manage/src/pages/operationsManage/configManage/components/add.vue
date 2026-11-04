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

        <!-- 合同名称,合同类型 -->
        <t-row class="row-gap" :gutter="[0, 24]" v-if="pagestu==2||pagestu==1||pagestu==3">
          <t-col :span="12">
            <t-form-item label="分组" name="sycGroup">
              <t-input v-model="formData.sycGroup"  :readonly="pagestu==3"   placeholder="请输入分组" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="键名" name="sycKey">
              <t-input v-model="formData.sycKey"  :readonly="pagestu==3"    placeholder="请输入键名" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="敏感" name="needPwd" >  
              <t-select v-model="formData.needPwd" :readonly="pagestu==3"   class="demo-select-base" clearable>
                <t-option value="1" label="是"></t-option>
                <t-option value="2" label="否"></t-option>
              </t-select>

              <!-- <t-radio-group v-model="formData.needPwd"    class="demo-select-base" >
                <t-radio value="1">是</t-radio>
                <t-radio value="2">否 </t-radio>
              </t-radio-group> -->
            </t-form-item>

          </t-col>
          <t-col :span="12">
            <t-form-item label="键值" name="sycValue">
              <t-input v-model="formData.sycValue"   v-if="pagestu==2"   placeholder="请输入键值" />

              <t-input v-model="formData.sycValue"   v-if="pagestu==3" readonly >

                <template #suffixIcon>

                  <t-popconfirm theme="default" v-if="formData.needPwd==1&&formData.sycValue.indexOf('***')>-1"  :visible="vs" @cancel="vs=false;pwd=''" @confirm="onConfirm()">
                    <template #content >
                      <p class="title">请完成登陆密码校验</p>
                      <p class="describe" style="margin-top:12px;position: relative;">
               
                        <t-input v-model="pwd"  type="password" style="width:220px" clearable placeholder="请输入登录密码"></t-input>
                        <div v-if="pwdStu==2&&!pwd" style="color:#F44336;position: absolute;font-size: 12px;">请输入密码</div>
                      </p>
                    </template>
                    <t-icon name="browse-filled"  @click="vs=true" />
                  </t-popconfirm>

                </template>

              </t-input>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="缓存" name="needCache">
              <t-select
                v-model="formData.needCache" 
                :readonly="pagestu==3" 
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
            <t-form-item label="备注">
              <t-textarea v-model="formData.remark"  :readonly="pagestu==3"  :height="124" placeholder="请输入" />
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
import JsEncrypt from 'jsencrypt'
import { getSystemConfigByGroupAndKey} from '@/api/dict';


const { proxy } = getCurrentInstance();
const emit = defineEmits(['sumt']);

const props = defineProps({
  pagestu:{},
  pcList:{
    type:Array,
    default:[]
  },
  phList:{
    type:Array,
    default:[]
  },
});

const FORM_RULES=ref({
  sycGroup:[{ required: true, message: '请输入分组', type: 'error' }],
  sycKey:[{ required: true, message: '请输入键名', type: 'error' }],
  sycValue:[{ required: true, message: '请输入键值', type: 'error' }],
  needCache:[{ required: true, message: '请选择', type: 'error' }],
  needPwd:[{ required: true, message: '请选择', type: 'error' }],
  remark:[{ required: true, message: '请输入', type: 'error' }],
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

const pwd=ref('')
const pwdStu=ref('1')
const vs=ref(false)

const onConfirm=async()=>{
  if(!pwd.value){
    pwdStu.value=2
    return
  }

  let res=await getSystemConfigByGroupAndKey({
      "loginPwd":proxy.$rsAencrypt(pwd.value),
      "sycGroup":formData.value.sycGroup,
      "sycKey": formData.value.sycKey
  })
  if(res.status.code==200){
    MessagePlugin.success(res.status.message);
    formData.value.sycValue=res.result.sycValue
    vs.value=false
  }else {
    MessagePlugin.error(res.status.message);
  }
}

const initData=(form:Object)=>{
  vs.value=false
  pwdStu.value='1'
  pwd.value=''
  form.needCache=form.needCache+''
  form.needPwd=form.needPwd+''
  formData.value=JSON.parse(JSON.stringify(form))
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
  padding-bottom: 50px;
}
</style>
