/* eslint-disable simple-import-sort/imports */
import { createApp } from 'vue';
import TDesign from 'tdesign-vue-next';

import App from './App.vue';
import router from './router';
import { store } from './store';
import i18n from './locales';

import 'tdesign-vue-next/es/style/index.css';
import '@/style/index.less';

import '@/style/cloud.less';
import './permission';
import { getPic, uploadPic } from '@/api/login';
import JsEncrypt from 'jsencrypt'

let publicKey = 'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtJ3ETuVcKW6cNy8oTjHbAqTWrVyrjGRBLqX3RxB1WKvovN9J1RK1qyiw6LDLRKeb6SPePwtgqOcBMnOefh7qQFZLgJ9shtb0fWgkdU0KaILH1spYzGXO+ypKyEhqrRjWK0Jqj9lZVAVNo6+uH1vSsJfGuzH89q98XD2LemKZtwrgFZfVfKWwsQFnAgdP/me7e/9qAQ9L/t/PZ/5+CAqYteH8uXs4xS+BIPbW/OBYLpBtuZu6vsnP1AUgP8Ez4VWPRmyWM7iOJbfuYl9rp4NJYdii7SGh+p01mgZykz7lscQQrmbw8s4TzDbnMWXc0kD/25IZj9kBVqh5J5o5q3TV1QIDAQAB'

//加密方法
const RSAencrypt=(pas:any)=>{
    let jse = new JsEncrypt();
    jse.setPublicKey(publicKey);
    return jse.encrypt(pas);
}

const app = createApp(App);
app.config.globalProperties.$getPic = getPic;
app.config.globalProperties.$uploadPic = uploadPic;
app.config.globalProperties.$rsAencrypt = RSAencrypt;

app.use(TDesign);
app.use(store);
app.use(router);
app.use(i18n);

app.mount('#app');
