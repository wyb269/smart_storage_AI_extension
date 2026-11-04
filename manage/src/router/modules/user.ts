import { LogoutIcon } from 'tdesign-icons-vue-next';
import { shallowRef } from 'vue';

import Layout from '@/layouts/index.vue';

export default [
  {
    path: '/userCenter',
    name: 'userCenter',
    component: Layout,
    redirect: '/userCenter/userIndex',
    hidden:true,
    meta: { title: { zh_CN: '个人中心', en_US: 'User Center' }, icon: 'user-circle' },
    children: [
      {
        path: 'userIndex',
        name: 'userIndex',
        component: () => import('@/pages/user/index.vue'),
        meta: { title: { zh_CN: '个人中心', en_US: 'User Center' } },
      },
    ],
  },
  {
    path: '/loginRedirect',
    name: 'loginRedirect',
    redirect: '/login',
    hidden:true,
    meta: { title: { zh_CN: '登录页', en_US: 'Login' }, icon: shallowRef(LogoutIcon) },
    component: () => import('@/layouts/blank.vue'),
    children: [
      {
        path: 'index',
        redirect: '/login',
        component: () => import('@/layouts/blank.vue'),
        meta: { title: { zh_CN: '登录页', en_US: 'Login' } },
      },
    ],
  },
];
