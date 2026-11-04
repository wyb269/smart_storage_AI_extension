<template>
  <t-form ref="formValidator" class="base-form" :data="formData" :rules="FORM_RULES" :label-width="90" @reset="onReset"
    @submit="onSubmit">
    <div class="form-basic-container">
      <div class="form-basic-item">
        <!-- 表单内容 -->

        <!-- 合同名称,合同类型 -->
        <t-row class="row-gap" :gutter="[0, 24]">
          <t-col :span="12">
            <t-form-item label="姓名" name="name">
              <t-input v-model="formData.name" placeholder="请输入用户姓名" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="账号" name="loginId">
              <t-input v-model="formData.loginId" placeholder="请输入账号" />
            </t-form-item>
          </t-col>
          <t-col :span="12" v-if="pagestu != 2">
            <t-form-item label="密码" name="loginPwd">
              <t-input v-model="formData.loginPwd" type="password" placeholder="请输入密码" />
            </t-form-item>
          </t-col>

          <t-col :span="12">
            <!-- name="phone" -->
            <t-form-item label="手机号">
              <t-input v-model="formData.phone" placeholder="请输入手机号" />
            </t-form-item>
          </t-col>

          <!-- <t-col :span="12">
            <t-form-item label="行政区域" name="regionId">
              <regionSelect ref="rgn"  @getRgdata="getRgdata" ></regionSelect>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="组织机构" name="orgId">
              <orgSelect ref="org"  @getdata="getdata" ></orgSelect>
            </t-form-item>
          </t-col> -->
          <t-col :span="12">
            <t-form-item label="角色组" name="roleGroupId">
              <t-select v-model="formData.roleGroupId" class="demo-select-base" placeholder="请选择角色组" clearable
                filterable>
                <t-option v-for="(item, index) in roleGroupData" :key="index" :value="item.id" :label="item.name">
                </t-option>
              </t-select>

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
import { ref, defineComponent, defineExpose, getCurrentInstance } from 'vue';
import regionSelect from '@/components/regionSelect.vue';
import orgSelect from '@/components/orgSelect.vue';


const { proxy } = getCurrentInstance();
const emit = defineEmits(['sumt']);

const props = defineProps({
  pagestu: {},
  roleGroupData: {
    type: Array,
    default: []
  },
});

const FORM_RULES = ref({
  loginPwd: [{ required: true, message: '请输入密码', type: 'error' }, {
    pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,15}$/,
    message: '密码必须包含大小字母、数字、特殊字符，最少8个字符，最多15个字符'
  }],
  name: [{ required: true, message: '请输入用户姓名', type: 'error' }],
  loginId: [{ required: true, message: '请输入账号', type: 'error' }],
  phone: [{ required: true, message: '请输入手机号', type: 'error' }, {
    pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
    message: "请输入正确的手机号码"
  }],
  regionId: [{ required: true, message: '请选择行政区域', type: 'error' }],
  orgId: [{ required: true, message: '请选择组织机构', type: 'error' }],
  roleGroupId: [{ required: true, message: '请选择角色组', type: 'error' }],
})

const formValidator = ref(null);
const submt = () => {
  formValidator.value.submit()
}
const onSubmit = (ctx: SubmitContext) => {
  if (ctx.validateResult === true) {
    let forma = JSON.parse(JSON.stringify(formData.value))
    if (forma.phone.indexOf('****') > -1) delete forma.phone
    if (props.pagestu == '1') forma.loginPwd = proxy.$rsAencrypt(forma.loginPwd)
    emit('sumt', forma)
  }
};
defineOptions({
  name: 'FormBase',
});

const getdata = (e: any) => {
  formData.value.orgId = e
}
const getRgdata = (e: any) => {
  formData.value.regionId = e
}
const rst = () => {
  formData.value = {
    "name": '',
    "loginId": '',
    "phone": '',
    "loginPwd": '',
    "regionId": '0',

    "orgId": '0',
    "roleGroupId": '',
    "userType": '1',
    "showState": '1',
    "employedStatus": '1'
  }
  formValidator.value.reset()
  // proxy.$refs.org.rst()
  // proxy.$refs.rgn.rst()
}

const initData = (form: Object) => {
  delete formData.value.loginPwd
  formData.value.id = form.id
  formData.value.name = form.name
  formData.value.remark = form.remark
  // formData.value.regionId = form.regionId
  // formData.value.orgId = form.orgId
  formData.value.regionId = 0
  formData.value.orgId = 0
  formData.value.roleGroupId = form.roleGroupId

  formData.value.showState = form.showState
  formData.value.loginId = form.loginId
  formData.value.phone = form.phone
  formData.value.userType = form.userType
  formData.value.employedStatus = form.employedStatus

  // proxy.$refs.org.initData(form.organization)
  // proxy.$refs.rgn.initData(form.region)
}

const formData = ref({
  "name": '',
  "loginId": '',
  "phone": '',
  "loginPwd": '',
  "regionId": '0',

  "orgId": '0',
  "roleGroupId": '',

  //  "headUrl":'',
  "userType": '1',
  "showState": '1',
  "employedStatus": '1'
});

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
