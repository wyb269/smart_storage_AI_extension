<template>
  <t-form ref="form" class="item-container forNewclass" :class="[`login-${type}`]" :data="formData" :rules="FORM_RULES"
    label-width="0" @submit="onSubmit">
    <template v-if="type === 'password'">
      <t-form-item name="account" style="margin-bottom: 32px;">
        <t-input v-model="formData.account" size="large" placeholder="请输入账号">
          <template #prefix-icon>
            <!-- <t-icon name="user" /> -->
            <img src="@/assets/imgs/lg-user.png" />
          </template>
        </t-input>
      </t-form-item>

      <t-form-item name="password" style="margin-bottom: 32px;">
        <t-input v-model="formData.password" size="large" :type="showPsw ? 'text' : 'password'" clearable
          placeholder="请输入密码">
          <template #prefix-icon>
            <!-- <t-icon name="lock-on" /> -->
            <img src="@/assets/imgs/lg-psw.png" />
          </template>
          <template #suffix-icon>
            <t-icon :name="showPsw ? 'browse' : 'browse-off'" @click="showPsw = !showPsw" />
          </template>
        </t-input>
      </t-form-item>
      <t-form-item name="verifyCode">
        <t-input v-model="formData.verifyCode" size="large" placeholder="请输入验证码">
          <template #prefix-icon>
            <!-- <t-icon name="verify" /> -->
            <img src="@/assets/imgs/lg-verfy.png" />
          </template>
          <template #suffix>
            <div><img @click="getImg" :src="codeUrl"
                style="cursor: pointer;height: 100%; width: 80px; margin-top: 4px;"></div>
          </template>
        </t-input>
      </t-form-item>

      <div class="check-container remember-pwd">
        <t-checkbox v-model="jz"><span style="color:rgba(30, 41, 59, 1);">记住账号</span></t-checkbox>
        <!-- <span class="tip">忘记账号</span> -->
      </div>
    </template>

    <!-- 扫码登录 -->
    <template v-else-if="type === 'qrcode'">
      <div class="tip-container">
        <span class="tip">{{ t('pages.login.wechatLogin') }}</span>
        <span class="refresh">{{ t('pages.login.refresh') }} <t-icon name="refresh" /> </span>
      </div>
      <t-qrcode value="tdesign" :size="160" level="H" />
    </template>

    <!-- 手机号登录 -->
    <template v-else>
      <t-form-item name="phone">
        <t-input v-model="formData.phone" size="large" :placeholder="t('pages.login.input.phone')">
          <template #prefix-icon>
            <t-icon name="mobile" />
          </template>
        </t-input>
      </t-form-item>

      <t-form-item class="verification-code" name="verifyCode">
        <t-input v-model="formData.verifyCode" size="large" :placeholder="t('pages.login.input.verification')" />
        <t-button size="large" variant="outline" :disabled="countDown > 0" @click="sendCode">
          {{ countDown === 0 ? t('pages.login.sendVerification') : `${countDown}秒后可重发` }}
        </t-button>
      </t-form-item>
    </template>

    <t-form-item v-if="type !== 'qrcode'" class="btn-container">
      <t-button block size="large" type="submit" class="dlbtn" theme="warning"> {{ t('pages.login.signIn') }}
      </t-button>
    </t-form-item>

    <!-- <div class="switch-container">
      <span v-if="type !== 'password'" class="tip" @click="switchType('password')">{{
        t('pages.login.accountLogin')
      }}</span>
      <span v-if="type !== 'qrcode'" class="tip" @click="switchType('qrcode')">{{ t('pages.login.wechatLogin') }}</span>
      <span v-if="type !== 'phone'" class="tip" @click="switchType('phone')">{{ t('pages.login.phoneLogin') }}</span>
    </div> -->

    <t-dialog v-model:visible="visible" header="修改密码" width="550px" :confirm-on-enter="true"
      :on-confirm="() => { formValidator.submit() }">
      <t-space direction="vertical" style="width: 100%">


        <t-form ref="formValidator" class="base-form" :data="formDataCpw" :rules="FORM_RULESCpw" label-align="top"
          :label-width="100" @reset="onReset" @submit="onSubmitCpw">
          <div class="form-basic-container">
            <div class="form-basic-item" style="height: 300px;padding: 5px;">

              <t-row class="row-gap" :gutter="[0, 24]">
                <t-col :span="12">
                  <t-form-item label="新密码" name="loginPassword">
                    <t-input v-model="formDataCpw.loginPassword" type="password" clearable placeholder="请输入新密码">
                      <template #prefix-icon>
                        <lock-on-icon />
                      </template>
                    </t-input>
                  </t-form-item>
                </t-col>
                <t-col :span="12">
                  <t-form-item label="确认新密码" name="loginPasswordSecond">
                    <t-input v-model="formDataCpw.loginPasswordSecond" type="password" clearable placeholder="请确认新密码">
                      <template #prefix-icon>
                        <lock-on-icon />
                      </template>
                    </t-input>
                  </t-form-item>
                </t-col>
                <t-col :span="12">
                  <t-form-item label="老密码" name="oldPassword">
                    <t-input v-model="formDataCpw.oldPassword" type="password" clearable placeholder="请输入旧密码">
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


  </t-form>
</template>
<script setup lang="ts">
import axios from 'axios';

import type { FormInstanceFunctions, FormRule, SubmitContext } from 'tdesign-vue-next';
import { MessagePlugin } from 'tdesign-vue-next';
import { ref, onMounted, getCurrentInstance } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { useCounter } from '@/hooks';
import { t } from '@/locales';
import { useUserStore } from '@/store';

const userStore = useUserStore();
const { proxy } = getCurrentInstance();
import { getCodeImg, login, getInfo, lgchangePwd } from '@/api/login';

const INITIAL_DATA = {
  phone: '',
  account: '',
  password: '',
  verifyCode: '',
  checked: false,
  key: ''
};

const jz = ref(false)

const FORM_RULES: Record<string, FormRule[]> = {
  phone: [{ required: true, message: '请输入账号', type: 'error' }],
  account: [{ required: true, message: '请输入账号', type: 'error' }],
  password: [{ required: true, message: '请输入密码', type: 'error' }],
  verifyCode: [{ required: true, message: '请输入验证码', type: 'error' }],
};

const type = ref('password');

const form = ref<FormInstanceFunctions>();
const formData = ref({ ...INITIAL_DATA });
const showPsw = ref(false);

const [countDown, handleCounter] = useCounter();

const switchType = (val: string) => {
  type.value = val;
};

const router = useRouter();
const route = useRoute();

/**
 * 发送验证码
 */
const sendCode = () => {
  form.value.validate({ fields: ['phone'] }).then((e) => {
    if (e === true) {
      handleCounter();
    }
  });
};
const onSubmit = async (ctx: SubmitContext) => {
  if (ctx.validateResult === true) {
    login({
      loginId: formData.value.account,
      loginPwd: proxy.$rsAencrypt(formData.value.password),
      verCode: formData.value.verifyCode,
      key: formData.value.key,
    }).then(res => {
      //  if(res?.status?.message.indexOf('成功')>-1){
      if (res?.status?.message.indexOf('密码已过期') > -1) {
        MessagePlugin.error(res.status.message);
        visible.value = true
        return
      }
      if (res.status.code == '200') {
        res.result.time = res.status.timestamp
        userStore.setToken(res.result);
        MessagePlugin.success(res.status.message);
        // setToken(res.result.accessToken);
        getUserInfo();
      } else {
        MessagePlugin.error(res.status.message);
        getImg();
      }
    });
  }
};
const getUserInfo = async () => {
  await getInfo().then(res => {
    userStore.getUserInfo(res.result);
    // const redirect = route.query.redirect as string;
    // const redirectUrl = redirect ? decodeURIComponent(redirect) : '/dashboard';
    if (jz.value) { }
    router.push('/pageHome');
    // localStorage.setItem('USER_INFO', JSON.stringify(res.result));
    // // localStorage.setItem('ROLE_ID', res.result.roleId);
    // usePermissionStore()
    //   .generateRoutes()
    //   .then(accessRoutes => {
    //     router.addRoute(accessRoutes); // 动态添加可访问路由表
    //     if(env=='TYYproduction'){
    //       router.push('/index');
    //     }else{
    //       router.push('/navigation');
    //     }
    //   });
  });
};
const codeUrl = ref("");
const getImg = async () => {
  let res = await getCodeImg()
  if (res.status.code == 200) {
    formData.value.key = res.result.key
    codeUrl.value = res.result.verCode
  }
}



const formValidator = ref(null);
const rePassword = (val) =>
  new Promise((resolve) => {
    const timer = setTimeout(() => {
      resolve(formDataCpw.value.loginPassword === val);
      clearTimeout(timer);
    });
  });
const FORM_RULESCpw = ref({
  loginPassword: [{ required: true, message: '请输入新密码', type: 'error' }, {
    pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,15}$/,
    message: '密码必须包含大小字母、数字、特殊字符，最少8个字符，最多15个字符'
  }],
  loginPasswordSecond: [{ required: true, message: '请确认新密码', type: 'error' }, { validator: rePassword, message: '两次密码不一致' },],
  oldPassword: [{ required: true, message: '请输入老密码', type: 'error' }],
})

const visible = ref(false)
const formDataCpw = ref({
  "oldPassword": '',
  "loginPassword": '',
  "loginPasswordSecond": '',
})


const onSubmitCpw = (ctx: SubmitContext) => {
  if (ctx.validateResult === true) {
    let p = {
      loginId: formData.value.account,
      "oldPassword": proxy.$rsAencrypt(formDataCpw.value.oldPassword),
      "loginPassword": proxy.$rsAencrypt(formDataCpw.value.loginPassword),
      "loginPasswordSecond": proxy.$rsAencrypt(formDataCpw.value.loginPasswordSecond),
    }
    lgchangePwd(p).then(res => {
      if (res.status.code == '200') {
        MessagePlugin.success('修改成功,请重新登录!');
        visible.value = false
        formDataCpw.value = {
          "oldPassword": '',
          "loginPassword": '',
          "loginPasswordSecond": '',
        }
        formData.value = {
          phone: '',
          account: '',
          password: '',
          verifyCode: '',
          checked: false,
          key: ''
        };
        form.value.reset()
        getImg()
        formValidator.value.reset()
      } else {
        MessagePlugin.error(res.status.message);
      }
    })

  }
};



onMounted(() => {
  getImg()
  // axios({
  //     method: 'post',
  //     url: '/farmland-quality-backend-admin/sso/captcha/getValidateCode/base64',
  // }).then((res) => {
  // })
});
</script>
<style lang="less" scoped>
@import '../index.less';
</style>
