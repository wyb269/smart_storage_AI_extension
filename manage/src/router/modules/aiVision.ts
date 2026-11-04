import Layout from '@/layouts/index.vue';

/**
 * AI 视觉识别路由模块。
 *
 * <p>每个子功能一个一级菜单项，页面形态统一为
 * 「左侧状态卡片网格 + 右侧实时监控 + 底部分页历史记录」。</p>
 *
 * <p>路由 path 与后端接口路径保持语义一致，便于排查问题时对照。</p>
 *
 * <p><b>图标一律用字符串名</b>，不能传组件对象：菜单数据会经过 store 的
 * JSON 持久化往返，组件对象反序列化后变成普通对象，
 * MenuContent 的 &lt;component :is&gt; 会报
 * "Component is missing template or render function" 并渲染空白。</p>
 */
export default [
  {
    path: '/aiVision/doorAnomaly',
    component: Layout,
    redirect: '/aiVision/doorAnomaly/monitor',
    meta: {
      title: {
        zh_CN: '仓门异动监测',
        en_US: 'Door Anomaly Monitor',
      },
      icon: 'video-camera-2',
    },
    children: [
      {
        path: 'monitor',
        component: () => import('@/pages/aiVision/doorAnomaly/index.vue'),
        name: 'DoorAnomalyMonitor',
        meta: {
          title: {
            zh_CN: '仓门异动监测',
            en_US: 'Door Anomaly Monitor',
          },
        },
      },
      {
        path: 'history',
        component: () => import('@/pages/aiVision/doorAnomaly/history.vue'),
        name: 'DoorAnomalyHistory',
        meta: {
          title: {
            zh_CN: '仓门异动历史记录',
            en_US: 'Door Anomaly History',
          },
        },
      },
    ],
  },
  {
    path: '/aiVision/videoOcclusion',
    component: Layout,
    redirect: '/aiVision/videoOcclusion/monitor',
    meta: {
      title: {
        zh_CN: '视频遮挡检测',
        en_US: 'Video Occlusion Monitor',
      },
      icon: 'rotate',
    },
    children: [
      {
        path: 'monitor',
        component: () => import('@/pages/aiVision/videoOcclusion/index.vue'),
        name: 'VideoOcclusionMonitor',
        meta: {
          title: {
            zh_CN: '视频遮挡检测',
            en_US: 'Video Occlusion Monitor',
          },
        },
      },
      {
        path: 'history',
        component: () => import('@/pages/aiVision/videoOcclusion/history.vue'),
        name: 'VideoOcclusionHistory',
        meta: {
          title: {
            zh_CN: '视频遮挡历史记录',
            en_US: 'Video Occlusion History',
          },
        },
      },
    ],
  },
  {
    path: '/aiVision/personBehavior',
    component: Layout,
    redirect: '/aiVision/personBehavior/monitor',
    meta: {
      title: {
        zh_CN: '人员行为',
        en_US: 'Person Behavior',
      },
      icon: 'usergroup',
    },
    children: [
      {
        path: 'monitor',
        component: () => import('@/pages/aiVision/personBehavior/index.vue'),
        name: 'PersonBehaviorMonitor',
        meta: {
          title: {
            zh_CN: '人员行为监测',
            en_US: 'Person Behavior Monitor',
          },
        },
      },
      {
        path: 'history',
        component: () => import('@/pages/aiVision/personBehavior/history.vue'),
        name: 'PersonBehaviorHistory',
        meta: {
          title: {
            zh_CN: '人员违规历史记录',
            en_US: 'Person Behavior History',
          },
        },
      },
    ],
  },
  {
    path: '/aiVision/groundWater',
    component: Layout,
    redirect: '/aiVision/groundWater/monitor',
    meta: {
      title: {
        zh_CN: '地面积水',
        en_US: 'Ground Water',
      },
      icon: 'earth',
    },
    children: [
      {
        path: 'monitor',
        component: () => import('@/pages/aiVision/groundWater/index.vue'),
        name: 'GroundWaterMonitor',
        meta: {
          title: {
            zh_CN: '地面积水监测',
            en_US: 'Ground Water Monitor',
          },
        },
      },
      {
        path: 'history',
        component: () => import('@/pages/aiVision/groundWater/history.vue'),
        name: 'GroundWaterHistory',
        meta: {
          title: {
            zh_CN: '积水历史记录',
            en_US: 'Ground Water History',
          },
        },
      },
    ],
  },
  {
    path: '/aiVision/beltMachine',
    component: Layout,
    redirect: '/aiVision/beltMachine/monitor',
    meta: {
      title: {
        zh_CN: '皮带机监测',
        en_US: 'Belt Machine Monitor',
      },
      icon: 'tools',
    },
    children: [
      {
        path: 'monitor',
        component: () => import('@/pages/aiVision/beltMachine/index.vue'),
        name: 'BeltMachineMonitor',
        meta: {
          title: {
            zh_CN: '皮带机下站人监测',
            en_US: 'Belt Machine Monitor',
          },
        },
      },
      {
        path: 'history',
        component: () => import('@/pages/aiVision/beltMachine/history.vue'),
        name: 'BeltMachineHistory',
        meta: {
          title: {
            zh_CN: '皮带机闲人历史记录',
            en_US: 'Belt Machine History',
          },
        },
      },
    ],
  },
  {
    path: '/aiVision/perimeterIntrusion',
    component: Layout,
    redirect: '/aiVision/perimeterIntrusion/monitor',
    meta: {
      title: {
        zh_CN: '周界入侵',
        en_US: 'Perimeter Intrusion',
      },
      icon: 'map-information',
    },
    children: [
      {
        path: 'monitor',
        component: () => import('@/pages/aiVision/perimeterIntrusion/index.vue'),
        name: 'PerimeterIntrusionMonitor',
        meta: {
          title: {
            zh_CN: '周界入侵识别',
            en_US: 'Perimeter Intrusion Detection',
          },
        },
      },
      {
        path: 'history',
        component: () => import('@/pages/aiVision/perimeterIntrusion/history.vue'),
        name: 'PerimeterIntrusionHistory',
        meta: {
          title: {
            zh_CN: '周界入侵事件记录',
            en_US: 'Perimeter Intrusion History',
          },
        },
      },
    ],
  },
  {
    path: '/aiVision/pestDetection',
    component: Layout,
    redirect: '/aiVision/pestDetection/monitor',
    meta: {
      title: {
        zh_CN: '虫害识别',
        en_US: 'Pest Detection',
      },
      icon: 'bug',
    },
    children: [
      {
        path: 'monitor',
        component: () => import('@/pages/aiVision/pestDetection/index.vue'),
        name: 'PestDetectionMonitor',
        meta: {
          title: {
            zh_CN: '粮面虫害智能识别',
            en_US: 'Pest Detection',
          },
        },
      },
      {
        path: 'history',
        component: () => import('@/pages/aiVision/pestDetection/history.vue'),
        name: 'PestDetectionHistory',
        meta: {
          title: {
            zh_CN: '虫害检测记录',
            en_US: 'Pest Detection History',
          },
        },
      },
    ],
  },
  {
    path: '/aiVision/vehicleRecognition',
    component: Layout,
    redirect: '/aiVision/vehicleRecognition/monitor',
    meta: {
      title: {
        zh_CN: '车辆识别',
        en_US: 'Vehicle Recognition',
      },
      icon: 'vehicle',
    },
    children: [
      {
        path: 'monitor',
        component: () => import('@/pages/aiVision/vehicleRecognition/index.vue'),
        name: 'VehicleRecognitionMonitor',
        meta: {
          title: {
            zh_CN: '车辆智能识别',
            en_US: 'Vehicle Recognition',
          },
        },
      },
      {
        path: 'history',
        component: () => import('@/pages/aiVision/vehicleRecognition/history.vue'),
        name: 'VehicleRecognitionHistory',
        meta: {
          title: {
            zh_CN: '车辆出入事件记录',
            en_US: 'Vehicle Recognition History',
          },
        },
      },
    ],
  },
];
