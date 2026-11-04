import { DashboardIcon } from 'tdesign-icons-vue-next';
import { shallowRef } from 'vue';

import Layout from '@/layouts/index.vue';

export default [
  // 作业单详情
  {
    path: '/taskDetail',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/pages/taskDetail/index.vue'),
        name: 'taskDetail',
        meta: {
          title: {
            zh_CN: '作业单详情',
          },
        },
      },
    ],
  },

  // 结算单详情
  {
    path: '/settlement/detail',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/pages/report/settlementSearch/detail.vue'),
        name: 'SettlementDetail',
        meta: {
          title: {
            zh_CN: '结算单详情',
          },
        },
      },
    ],
  },

  //   {
  //  path: '/pageHome',
  //     component: Layout,
  //  //    component: () => import('@/pages/sysManage/userManage/index.vue'),
  //     redirect: '/pageHome/pageIndex',
  //     name: 'pageHome',
  //     meta: {
  //       title: {
  //         zh_CN: '首页',

  //       },
  //       icon:'desktop',
  //     },
  //     children: [

  //        {
  //         path: 'pageIndex',
  //         name: 'pageIndex',
  //         hidden:true,
  //         component: () => import('@/pages/pageHome/pageIndex/index.vue'),
  //         meta: {
  //           title: {
  //             zh_CN: '首页'
  //           },
  //         },
  //       },

  //     ]
  //   },
  //     {
  //     path: '/sysManageNew',
  //     component: Layout,
  //  //    component: () => import('@/pages/sysManage/userManage/index.vue'),
  //  //   redirect: '/sysManage/base',
  //     name: 'sysManageNew',
  //     meta: {
  //       title: {
  //         zh_CN: '系统管理',

  //       },
  //       icon: 'layers',
  //     },
  //     children: [
  //       {
  //         path: 'configManage',
  //         name: 'configManage',
  //         component: () => import('@/pages/sysManage/configManage/index.vue'),
  //         meta: {
  //           title: {
  //             zh_CN: '配置管理'
  //           },
  //         },
  //       },

  //             {
  //         path: 'oprtManagenew',
  //         name: 'oprtManagenew',
  //         component: () => import('@/pages/sysManage/oprtManage/index.vue'),
  //         meta: {
  //           title: {
  //             zh_CN: '操作日志'
  //           },
  //         },
  //       },

  //       {
  //         path: 'userManage',
  //         name: 'userManage',
  //         component: () => import('@/pages/sysManage/userManage/index.vue'),
  //         meta: {
  //           title: {
  //             zh_CN: '用户管理',
  //               en_US: 'Overview',

  //           },
  //         },
  //         children:[
  //       {
  //         path: 'userManageDeatil',
  //         name: 'userManageDetail',
  //         hidden: true,
  //         component: () => import('@/pages/sysManage/userManage/detail.vue'),
  //         meta: {
  //           title: {
  //             zh_CN: '用户详情'
  //           },
  //         },
  //       },
  //         ]
  //      },

  //       {
  //         path: 'userManage/userManageDetail',
  //         name: 'userManageDetail',
  //         hidden: true,
  //         component: () => import('@/pages/sysManage/userManage/detail.vue'),
  //         meta: {
  //           title: {
  //             zh_CN: '用户详情'
  //           },
  //         },
  //       },

  //       {
  //         path: 'orgManage',
  //         name: 'orgManage',
  //         component: () => import('@/pages/sysManage/orgManage/index.vue'),
  //         meta: {
  //           title: {
  //             zh_CN: '组织机构管理',
  //               en_US: 'Overview',
  //           },
  //         },
  //       },
  //       {
  //         path: 'roleGpManage',
  //         name: 'roleGpManage',
  //         component: () => import('@/pages/sysManage/roleGpManage/index.vue'),
  //         meta: {
  //           title: {
  //             zh_CN: '角色分组管理'
  //           },
  //         },
  //       },
  //       {
  //         path: 'roleManage',
  //         name: 'roleManage',
  //         component: () => import('@/pages/sysManage/roleManage/index.vue'),
  //         meta: {
  //           title: {
  //             zh_CN: '角色管理'
  //           },
  //         },
  //       },
  //       {
  //         path: 'menuManage',
  //         name: 'menuManage',
  //         component: () => import('@/pages/sysManage/menuManage/index.vue'),
  //         meta: {
  //           title: {
  //             zh_CN: '菜单管理'
  //           },
  //         },
  //       },
  //       {
  //         path: 'dictManage',
  //         name: 'dictManage',
  //         component: () => import('@/pages/sysManage/dictManage/index.vue'),
  //         meta: {
  //           title: {
  //             zh_CN: '字典管理'
  //           },
  //         },
  //       },
  //       {
  //         path: 'dictManage/dictData',
  //         name: 'dictData',
  //         hidden:true,
  //         component: () => import('@/pages/sysManage/dictManage/dictData/index.vue'),
  //         meta: {
  //           title: {
  //             zh_CN: '字典数据管理'
  //           },
  //         },
  //       },
  //     ],
  //   },



  //   {
  //     path: '/dashboard',
  //     component: Layout,
  //     redirect: '/dashboard/base',

  //     name: 'dashboard',
  //     meta: {
  //       title: {
  //         zh_CN: '仪表盘',
  //         en_US: 'Dashboard',
  //       },
  //       icon: shallowRef(DashboardIcon),
  //       orderNo: 0,
  //     },
  //     children: [
  //             {
  //         path: 'userManageDeatil',
  //         name: 'userManageDetail',
  //         // hidden: true,
  //         component: () => import('@/pages/sysManage/userManage/detail.vue'),
  //         meta: {
  //           title: {
  //             zh_CN: '用户详情'
  //           },
  //         },
  //       },
  //       {
  //         path: 'base',
  //         name: 'DashboardBase',
  //         component: () => import('@/pages/dashboard/base/index.vue'),
  //         meta: {
  //           title: {
  //             zh_CN: '概览仪表盘',
  //             en_US: 'Overview',
  //           },
  //         },
  //       },
  //       {
  //         path: 'dashboard',
  //         name: 'Dashboard',
  //         meta: {
  //           title: {
  //             zh_CN: '报表',
  //             en_US: 'Dashboard',
  //           },
  //         },
  //         children: [
  //           {
  //             path: 'detail',
  //             name: 'Detail',
  //             component: () => import('@/pages/dashboard/detail/index.vue'),
  //             meta: {
  //               title: {
  //                 zh_CN: '统计报表',
  //                 en_US: 'Dashboard Detail',
  //               },
  //             },
  //           },
  //         ],
  //       },
  //     ],
  //   },
];
