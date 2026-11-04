import { defineStore } from 'pinia';

import { usePermissionStore } from '@/store';
import type { UserInfo } from '@/types/interface';

const InitUserInfo: UserInfo = {
  name: 'tent', // 用户名，用于展示在页面右上角头像处
  roles: ['all'], // 前端权限模型使用 如果使用请配置modules/permission-fe.ts使用
};

export const useUserStore = defineStore('user', {
  state: () => ({
    tokenInfo:{},
    token: 'main_token', // 默认token不走权限
    userInfo: { ...InitUserInfo },
  }),
  getters: {
    roles: (state) => {
      return state.userInfo?.roles;
    },
  },
  actions: {
    setToken(tokenInfo:any) {
      this.token ="Bearer "+ tokenInfo.accessToken 
      this.tokenInfo =tokenInfo
    },
    getUserInfo(userinfo:any) {
      this.userInfo =userinfo;
    },
    async logout() {
      this.tokenInfo ={}
      this.token = '';
      this.userInfo = { ...InitUserInfo };
    },
  },
  persist: {
    afterRestore: () => {
      const permissionStore = usePermissionStore();
      permissionStore.initRoutes();
    },
    key: 'user',
    paths: ['token','tokenInfo','userInfo'],
  },
});
