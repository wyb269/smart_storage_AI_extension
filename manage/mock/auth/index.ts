/**
 * 登录链路 Mock —— 仅用于本地查看页面效果
 *
 * ⚠️ 生效范围
 * 只在 `VITE_USE_MOCK=true`（`npm run dev:mock`）时被 vite-plugin-mock 装载；
 * 联调与生产构建（`VITE_USE_MOCK=false` 或未设置）不会注册这些 handler，
 * 因此**不构成线上认证绕过**。开关由 `vite.config.ts` 的
 * `viteMockServe({ enable: VITE_USE_MOCK === 'true' })` 控制。
 *
 * 为什么需要它
 * 路由守卫 `src/permission.ts` 在 `userStore.token` 为空时跳 `/login`，
 * 登录后又要依次请求验证码、登录、用户信息与菜单四个接口。
 * Mock 模式下后端不存在，这四个接口全部代理失败，
 * 于是本地无法用浏览器点进仓门异动页面（自动化验收是靠 CDP 注入 token 绕过的，
 * 只适用于脚本，人工浏览没法用）。
 *
 * 菜单必须返回非空内容
 * 仓门异动的监测页与历史页属于**固定路由**，本身不依赖菜单下发；
 * 但 `Login.vue` 登录成功后固定跳 `/pageHome`，而该路由在
 * `src/router/modules/homepage.ts` 里是被注释掉的，只能靠菜单生成。
 * 空菜单会让这一跳落到 404 并清掉登录态。详见下方 DEMO_MENU 注释。
 *
 * 字段结构对齐点
 * - 登录：`userStore.setToken` 读取 `result.accessToken`（`src/store/modules/user.ts:23`）
 * - 用户信息：`Header.vue:43` 读取 `userInfo.user.name`，
 *   而 `UserInfo` 类型只声明了 `{name, roles}`（既有代码的声明与实现不一致），
 *   所以这里必须同时给出 `user.name`，否则头部渲染抛错白屏。
 */
import type { MockMethod } from 'vite-plugin-mock';

/** 模拟后端 ActionResult 结构 */
function ok(result: unknown) {
  return {
    status: {
      success: true,
      code: '200',
      message: '请求成功',
      requestId: 'mock-auth-request-id',
      timestamp: new Date().toISOString(),
    },
    result,
  };
}

/** 验证码图片：内联 SVG，避免依赖真实后端与图片资源 */
const CAPTCHA_SVG = `data:image/svg+xml;utf8,${encodeURIComponent(
  `<svg xmlns="http://www.w3.org/2000/svg" width="88" height="32">` +
    `<rect width="100%" height="100%" fill="#f2f3f5"/>` +
    `<text x="50%" y="55%" dominant-baseline="middle" text-anchor="middle" ` +
    `font-family="monospace" font-size="16" fill="#86909c" letter-spacing="3">DEMO</text>` +
    `</svg>`,
)}`;

/* ============ 菜单 ============ */
/**
 * 菜单不能返回空数组。
 *
 * `Login.vue` 登录成功后固定 `router.push('/pageHome')`，而 `/pageHome`
 * 在 `src/router/modules/homepage.ts:46-64` 是**被注释掉的**，
 * 只能由后端菜单下发。返回空菜单会让该跳转落到 404、守卫随后清掉登录态，
 * 表现为「登录成功但又回到登录页」。
 *
 * 这里下发一条最小菜单，映射到仓库里真实存在的
 * `src/pages/pageHome/pageIndex/index.vue`。字段按
 * `permission.ts:buildAsyncRoutes` 的既有转换规则倒推：
 * `parentId=0` 且 `children=[]` 走「单路由跳页」分支，
 * 于是 path=`/` + openUrl 的第一段（`/pageHome`）、
 * 子路由 component=`/` + openUrl + `/index`（`/pageHome/pageIndex/index`）。
 *
 * `children` 必须显式给空数组：转换代码直接读 `d[i].children.length`，
 * 缺字段会抛错并被包装成 "Can't build routes"，同样回到登录页。
 */
/**
 * 菜单项的最小结构。
 *
 * `children` 必须显式标注类型：`noImplicitAny` 下空数组字面量会被推断成
 * 隐式 `any[]` 而报 TS7018。
 */
interface MockMenuItem {
  id: number;
  parentId: number;
  name: string;
  /** 1 = 参与路由构建（buildAsyncRoutes 会 filter(type == 1)） */
  type: number;
  orderNum: number;
  /** 1 = 显示；2 会被置为 hidden */
  isDisplay: number;
  openUrl: string;
  parentUrl: string;
  icon: string;
  children: MockMenuItem[];
}

const DEMO_MENU: MockMenuItem[] = [
  {
    id: 1,
    parentId: 0,
    name: '首页',
    type: 1,
    orderNum: 1,
    isDisplay: 1,
    openUrl: 'pageHome/pageIndex',
    parentUrl: '',
    icon: '',
    children: [],
  },
];

const P = '/api/sso/admin/default';

export default [
  /* ============ 图形验证码 ============ */
  {
    url: `${P}/captcha/getVerCode`,
    method: 'post',
    response: () =>
      ok({
        key: 'mock-captcha-key',
        verCode: CAPTCHA_SVG,
      }),
  },

  /* ============ 账号密码登录 ============ */
  {
    url: `${P}/loginByPassword`,
    method: 'post',
    response: (opt: any) => {
      const loginId = opt?.body?.loginId;
      // 与真实后端一样要求账号非空，避免把「表单没填」误判成「登录成功但页面空白」
      if (!loginId) {
        return {
          status: {
            success: false,
            code: '4001',
            message: '请输入账号',
            requestId: 'mock-auth-request-id',
            timestamp: new Date().toISOString(),
          },
          result: null,
        };
      }
      return ok({
        accessToken: `mock-dev-token-${loginId}`,
        expiresIn: 2592000,
      });
    },
  },

  /* ============ 当前登录用户信息 ============ */
  {
    url: `${P}/getLoginBaseInfo`,
    method: 'post',
    response: () =>
      ok({
        name: '本地演示账号',
        roles: ['all'],
        // Header.vue 实际读取的是 userInfo.user.name
        user: { name: '本地演示账号' },
      }),
  },

  /* ============ 菜单 ============ */
  {
    url: `${P}/getLoginMenuInfo`,
    method: 'post',
    response: () => ok(DEMO_MENU),
  },
] as MockMethod[];
