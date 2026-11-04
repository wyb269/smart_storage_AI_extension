<template>
  <t-form ref="formValidator" class="base-form" :data="formData" :rules="FORM_RULES" :label-width="90" @reset="onReset"
    @submit="onSubmit">
    <div class="form-basic-container">
      <div class="form-basic-item">
        <!-- 表单内容 -->

        <!-- 合同名称,合同类型 -->
        <t-row class="row-gap" :gutter="[0, 24]">
          <t-col :span="12">
            <t-form-item label="名称" name="name">
              <t-input v-model="formData.name" :disabled="pagestu == 3" placeholder="请输入名称" />
            </t-form-item>
          </t-col>
          <!-- <t-col :span="12">
            <t-form-item label="行政区域" name="regionId">
              <regionSelect ref="rgn" :disabled="pagestu==3?true:false" @getRgdata="getRgdata" ></regionSelect>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="组织机构" name="orgId">
              <orgSelect ref="org"  :disabled="pagestu==3?true:false" @getdata="getdata"></orgSelect>
            </t-form-item>
          </t-col> -->
          <t-col :span="12">
            <t-form-item label="添加角色" name="listRoleIds">
              <t-select :disabled="pagestu == 3" v-model="listRole" :options="RoleOptionList" placeholder="请添加角色"
                multiple filterable @change="opChange" />
            </t-form-item>
          </t-col>
          <t-col :span="12" name="remark">
            <t-form-item label="备注">
              <t-textarea v-model="formData.remark" :disabled="pagestu == 3" :height="124" placeholder="请输入备注" />
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

const props = defineProps({
  RoleOptionList: {
    type: Array
  },
  pagestu: {}
});

const emit = defineEmits(['sumt']);
const getdata = (e: any) => {
  formData.value.orgId = e
}
const getRgdata = (e: any) => {
  formData.value.regionId = e
}

const rst = () => {
  delete formData.value.id
  listRole.value = []
  formData.value.remark = ''
  formValidator.value.reset()
  formData.value.regionId = 0
  formData.value.orgId = 0
  // proxy.$refs.org.rst()
  // proxy.$refs.rgn.rst()
}

const initData = (form: Object) => {
  formData.value.id = form.id
  formData.value.name = form.name
  formData.value.remark = form.remark
  // formData.value.regionId = form.regionId
  // formData.value.orgId = form.orgId
  formData.value.regionId = 0
  formData.value.orgId = 0
  listRole.value = form.roleIds
  formData.value.listRoleIds = form.roleIds
  // proxy.$refs.org.initData(form.organization)
  // proxy.$refs.rgn.initData(form.region)
}

const opChange = () => {
  if (listRole.value.length == 0) {
    formData.value.listRoleIds = ''
    return
  }
  formData.value.listRoleIds = listRole.value
  console.log(formData.value)
}
const listRole = ref()
const FORM_RULES = ref({
  name: [{ required: true, message: '请输入用户姓名', type: 'error' }],
  regionId: [{ required: true, message: '请选择行政区域', type: 'error' }],
  orgId: [{ required: true, message: '请选择组织机构', type: 'error' }],
  listRoleIds: [{ required: true, message: '请选择角色', type: 'error' }],
})

const formValidator = ref(null);
const submt = () => {
  formValidator.value.submit()
}
const onSubmit = (ctx: SubmitContext) => {
  if (ctx.validateResult === true) {
    emit('sumt', formData.value)
  }
};
defineOptions({
  name: 'FormBase',
});

const formData = ref({
  "orgId": '0',
  "regionId": '0',
  "name": '',
  "remark": '',
  "listRoleIds": '',
});

const onReset = () => {

};

const beforeUpload = (file: UploadFile) => {
  if (!/\.pdf$/.test(file.name)) {
    MessagePlugin.warning('请上传pdf文件');
    return false;
  }
  if (file.size > 60 * 1024 * 1024) {
    MessagePlugin.warning('上传文件不能大于60M');
    return false;
  }
  return true;
};
const handleFail = (options: UploadFailContext) => {
  MessagePlugin.error(`文件 ${options.file.name} 上传失败`);
};
// 用于格式化接口响应值，error 会被用于上传失败的提示文字；url 表示文件/图片地址
const formatResponse = (res: any) => {
  return { ...res, error: '上传失败，请重试', url: res.url };
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
  padding-left: 0px;
  padding-right: 0px;
}
</style>
