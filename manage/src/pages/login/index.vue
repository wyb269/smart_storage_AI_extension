<template>
  <div class="login-wrapper">
    <!-- <login-header /> -->

    <div class="login-container">
      <div class="title-container">
        <!-- <h1 class="title margin-no">{{ t('pages.login.loginTitle') }}</h1> -->
        <div class="title">种芯粮安智控管理平台</div>
      </div>
      <div class="sub-title">
        <p class="tip" style="color:rgba(151, 162, 182, 1);margin-top: 3px;font-family: PingFang SC;
font-weight: 400;
font-style: Regular;
font-size: 14px;
leading-trim: NONE;
line-height: 100%;
letter-spacing: 0px;
">请用账号登录</p>
      </div>

      <login v-if="type === 'login'" />
      <register v-else @register-success="switchType('login')" />
      <tdesign-setting />
    </div>

    <!-- <footer class="copyright">Copyright @ 2021-2025 All Rights Reserved</footer> -->
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue';

import TdesignSetting from '@/layouts/setting.vue';
import { t } from '@/locales';

import LoginHeader from './components/Header.vue';
import Login from './components/Login.vue';
import Register from './components/Register.vue';

defineOptions({
  name: 'LoginIndex',
});
const type = ref('login');
const switchType = (val: string) => {
  type.value = val;
};
</script>
<style lang="less" scoped>
@import './index.less';
</style>
