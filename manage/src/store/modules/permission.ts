import { defineStore } from 'pinia';
import type { RouteRecordRaw } from 'vue-router';

import type { RouteItem } from '@/api/model/permissionModel';
import { getMenuList } from '@/api/permission';
import router, { fixedRouterList, homepageRouterList } from '@/router';
import { store } from '@/store';
import { transformObjectToRoute } from '@/utils/route';

import { getMenu } from '@/api/login';

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    whiteListRouters: ['/login'],
    routers: [],
    removeRoutes: [],
    asyncRoutes: [],
  }),
  actions: {
    async initRoutes() {
      const accessedRouters = this.asyncRoutes;

      // 在菜单展示全部路由
      this.routers = [...homepageRouterList, ...accessedRouters, ...fixedRouterList];
      // 在菜单只展示动态路由和首页
      // this.routers = [...homepageRouterList, ...accessedRouters];
      // 在菜单只展示动态路由
      // this.routers = [...accessedRouters];
    },
    async buildAsyncRoutes() {
      try {
        // 发起菜单权限请求 获取菜单列表

        let r = await getMenu()
        let data = r.result.filter((x: any) => x.type == 1)
        data.sort((a, b) => a.orderNum - b.orderNum);
        const lookUp = (d: any) => {
          for (let i = 0; i < d.length; i++) {
            d[i].meta = { title: { zh_CN: '' }, icon: '' }
            d[i].meta.title.zh_CN = d[i].name
            if (d[i].parentId == 0) {
              d[i].meta.icon = d[i].icon
              d[i].path = '/' + d[i].openUrl
              d[i].component = 'LAYOUT'
              d[i].parentUrl = ''
            }
            if (d[i].parentId != 0) {
              d[i].path = d[i].openUrl
              d[i].component = d[i].parentUrl + '/' + d[i].path + '/index'
            }

            let a = d[i].openUrl.split('/')
            d[i].name = a[a.length - 1]
            if (d[i].isDisplay == 2) d[i].hidden = true

            // console.log(d[i])
            if (d[i].children.length == 0) {
              //处理单路由跳页
              if (d[i].parentId == 0) {
                d[i].children = [{
                  children: [],
                  hidden: true,
                  path: a[a.length - 1],
                  name: a[a.length - 1],
                  meta: { title: { zh_CN: d[i].meta.title.zh_CN }, icon: '' },
                  component: '/' + d[i].openUrl + '/index'
                }]
                d[i].redirect = '/' + d[i].openUrl
                d[i].path = '/' + a[0]
                d[i].name = a[0]
              }
            } else {
              d[i].children.sort((a, b) => a.orderNum - b.orderNum);
              d[i].children = d[i].children.map((x: any) => {
                x.parentUrl = d[i].parentUrl + '/' + d[i].openUrl
                return x
              })
              lookUp(d[i].children)
            }
          }
        }

        lookUp(data)
        const asyncRoutes: Array<RouteItem> = data
        this.asyncRoutes = transformObjectToRoute(asyncRoutes);
        await this.initRoutes();
        return this.asyncRoutes;
      } catch (error) {
        throw new Error("Can't build routes", error);
      }
    },
    async restoreRoutes() {
      // 不需要在此额外调用initRoutes更新侧边导肮内容，在登录后asyncRoutes为空会调用
      this.asyncRoutes.forEach((item: RouteRecordRaw) => {
        if (item.name) {
          router.removeRoute(item.name);
        }
      });
      this.asyncRoutes = [];
    },
  },
});

export function getPermissionStore() {
  return usePermissionStore(store);
}
