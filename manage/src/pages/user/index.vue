<template>
  <div>
  <t-row :gutter="[24, 24]">
    <t-col :flex="3">
      <div class="user-left-greeting">
        <div>
          Hi，{{user.userInfo.user.name}}
          <span class="regular"> 欢迎访问种芯粮安智控管理平台</span>
        </div>
        <t-dropdown :options="options" @click="clickHandler">
            <t-button theme="default" shape="square" variant="text">
              <t-icon name="lock-time" />
            </t-button>
        </t-dropdown>
    
        <!-- <img src="@/assets/assets-tencent-logo.png" class="logo" /> -->
      </div>

      <t-card class="user-info-list" title="个人信息" :bordered="false">
        <template #actions>
          <t-button theme="default" shape="square" variant="text">
            <t-icon name="ellipsis" />
          </t-button>
        </template>
        <t-descriptions :column="4" item-layout="vertical">
          <t-descriptions-item v-for="(item, index) in INFO_LIST" :key="index" :label="item.label">
            {{ item.content }}
          </t-descriptions-item>
        </t-descriptions>
      </t-card>
    </t-col>

    <t-col :flex="1">
      <t-card class="user-intro" :bordered="false">
        <div style="display: flex; align-items: center;">
          <t-avatar size="75px">
          {{user.userInfo.user.loginId.slice(0,1)}}
          <!-- {{user.userInfo.user.name.length<3?user.userInfo.user.name.slice(0,2):user.userInfo.user.name.slice(1,3)}} -->
        </t-avatar>
        <div style="margin-left: 15px;">
        <div class="name" style="color: #121212;font-size: 14px;font-weight:600;margin-top: 0px;"> {{user.userInfo.user.loginId}}</div>
        <div class="position" style="color: #333;font-size: 13px;display: flex;margin-top: 7px;"><span style="margin-right:20px;color: #666;">账号ID:</span>{{user.userInfo.user.loginId}}   
          <!-- {{user.userInfo.user.userType==1?'超级管理员':'其他'}} -->
        </div>

        <div class="position" style="color: #333;font-size: 13px;display: flex;margin-top:2px;">
          <span style="margin-right:20px;color: #666;">手机号:</span>
<span style="color: #44c56d;font-size: 14px;display: flex;align-items: center;">
  <t-icon name="check-circle-filled"  />
<span style="font-size: 13px;margin-left:8px;">已绑定</span>
</span>
          
          
          <!-- {{user.userInfo.user.loginId}}    -->
          <!-- {{user.userInfo.user.userType==1?'超级管理员':'其他'}} -->
        </div>

        </div>


        </div>

      </t-card>

      <t-card :title="t('pages.user.teamMember')" class="user-team" :bordered="false" >
        <template #actions>
          <t-button theme="default" shape="square" variant="text">
            <t-icon name="ellipsis" />
          </t-button>
        </template>
        <t-list :split="false">
          <t-list-item v-for="(item, index) in TEAM_MEMBERS" :key="index">
            <t-list-item-meta :image="item.avatar" :title="item.title" :description="item.description" />
          </t-list-item>
        </t-list>
      </t-card>

      <!-- <t-card :title="t('pages.user.serviceProduction')" class="product-container" :bordered="false">
        <template #actions>
          <t-button theme="default" shape="square" variant="text">
            <t-icon name="ellipsis" />
          </t-button>
        </template>
        <t-row class="content" :getters="16">
          <t-col v-for="(item, index) in PRODUCT_LIST" :key="index" :span="3">
            <component :is="getIcon(item)"></component>
          </t-col>
        </t-row>
      </t-card> -->
    </t-col>
  </t-row>
    <!-- 新增编辑弹窗 -->
  <t-dialog
      v-model:visible="visible"
      header="修改密码"
      width="550px"
      :confirm-on-enter="true"
      :on-confirm=" ()=>{formValidator.submit()}"
    >
      <t-space direction="vertical" style="width: 100%">


      <t-form
      ref="formValidator"
      class="base-form"
      :data="formData"
      :rules="FORM_RULES"
      label-align="top"
      :label-width="100"
      @reset="onReset"
      @submit="onSubmit"
      >
      <div class="form-basic-container">
        <div class="form-basic-item" style="height: 300px;padding: 5px;">

          <t-row class="row-gap" :gutter="[0, 24]">
            <t-col :span="12">
              <t-form-item label="新密码" name="loginPassword">
                <t-input v-model="formData.loginPassword"  type="password" clearable placeholder="请输入新密码">
                <template #prefix-icon>
                  <lock-on-icon />
                </template>
                </t-input>
              </t-form-item>
            </t-col>
            <t-col :span="12">
              <t-form-item label="确认新密码" name="loginPasswordSecond">
                <t-input v-model="formData.loginPasswordSecond"  type="password" clearable placeholder="请确认新密码">
                <template #prefix-icon>
                  <lock-on-icon />
                </template>
                </t-input>
              </t-form-item>
            </t-col>
            <t-col :span="12">
              <t-form-item label="老密码" name="oldPassword">
                <t-input v-model="formData.oldPassword"  type="password" clearable placeholder="请输入旧密码">
                <template #prefix-icon>
                  <lock-on-icon />
                </template>
                </t-input>
              </t-form-item>
            </t-col>

          </t-row>

        </div>
      </div>


    </t-form>
     
  </t-space>
  </t-dialog>
  <t-dialog
        v-model:visible="confirmVisible"
        header="确认重置密码？"
        @confirm="onConfirmDelete"
      />
  </div>
</template>
<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, watch,ref,getCurrentInstance } from 'vue';
import { changePwd } from '@/api/login';
import { MessagePlugin } from 'tdesign-vue-next';
import { useRoute, useRouter } from 'vue-router';

import { t } from '@/locales';
import { useSettingStore,useUserStore } from '@/store';
import { PRODUCT_LIST, TEAM_MEMBERS } from './constants';
const { proxy } = getCurrentInstance();
const router = useRouter();
const user = useUserStore();
const options=[ 
  // { content: '重置密码', value: 1 }, 
  { content: '修改密码', value: 2 },]
const clickHandler=(e)=>{
  console.log(e)
  if(e.value==1)confirmVisible.value=true
  if(e.value==2){
    formValidator.value.reset()
    visible.value=true
  }
}
const formValidator = ref(null);
const rePassword = (val) =>
  new Promise((resolve) => {
    const timer = setTimeout(() => {
      resolve(formData.value.loginPassword === val);
      clearTimeout(timer);
    });
  });
const FORM_RULES=ref({
  loginPassword: [{ required: true, message: '请输入新密码', type: 'error' },{
    pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,15}$/,
    message: '密码必须包含大小字母、数字、特殊字符，最少8个字符，最多15个字符'
  }],
  loginPasswordSecond: [{ required: true, message: '请确认新密码', type: 'error' },{ validator: rePassword, message: '两次密码不一致' },],
  oldPassword: [{ required: true, message: '请输入老密码', type: 'error' }],
})

defineOptions({
  name: 'UserIndex',
});

const store = useSettingStore();
const confirmVisible=ref(false)
const visible=ref(false)
const formData=ref({
  "oldPassword":'',
  "loginPassword":'',
  "loginPasswordSecond":'',
})
const onConfirmDelete = () => {
  // 真实业务请发起请求
  // data.value.splice(deleteIdx.value, 1);
  // pagination.value.total = data.value.length;
  // confirmVisible.value = false;
  // MessagePlugin.success('删除成功');
  // resetIdx();
};


const INFO_LIST=ref([
  {label:'手机号',content:user.userInfo.user.loginId},
  {label:'用户类型',content:user.userInfo.user.userType==1?'超级管理员':'其他'},
  {label:'在职状态',content:user.userInfo.user.employedStatus==1?'在职':'不在职'},
  {label:'行政区',content:user.userInfo.region.name},
  {label:'角色组',content:user.userInfo.roleGroup.name},
  {label:'组织机构',content:user.userInfo.organization.name},
])

const onSubmit = (ctx: SubmitContext) => {
  if (ctx.validateResult === true) {
    let p ={
      "oldPassword":proxy.$rsAencrypt(formData.value.oldPassword),
      "loginPassword":proxy.$rsAencrypt(formData.value.loginPassword),
      "loginPasswordSecond":proxy.$rsAencrypt(formData.value.loginPasswordSecond),
    }
    changePwd(p).then(res=>{
      if (res.status.code == '200') {
        MessagePlugin.success(res.status.message);
        visible.value=false
        // MessagePlugin.success('修改成功,请重新登录!');
        // router.push({
        //   path: '/login',
        //   query: { redirect: encodeURIComponent(router.currentRoute.value.fullPath) },
        // });
      }else {
        MessagePlugin.error(res.status.message);
      }
    })
    
  }
};

onMounted(() => {
  console.log(user.userInfo,99)
});

onUnmounted(() => {

});

</script>
<style lang="less" scoped>
@import './index.less';

.t-descriptions {
  margin-top: 24px;
}
</style>
