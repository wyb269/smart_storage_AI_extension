import path from 'node:path';

import vue from '@vitejs/plugin-vue';
import vueJsx from '@vitejs/plugin-vue-jsx';
import type { ConfigEnv, UserConfig } from 'vite';
import { loadEnv } from 'vite';
import { viteMockServe } from 'vite-plugin-mock';
import svgLoader from 'vite-svg-loader';

const CWD = process.cwd();

// https://vitejs.dev/config/
export default ({ mode }: ConfigEnv): UserConfig => {
  const { VITE_BASE_URL, VITE_API_URL_PREFIX, VITE_API_URL, VITE_USE_MOCK } = loadEnv(mode, CWD);
  return {
    base: VITE_BASE_URL,
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
        // ... 你的其他别名
        //   'minio': path.resolve(__dirname, './node_modules/minio-js/dist/minio-js.js'),
      },
    },

    css: {
      preprocessorOptions: {
        less: {
          modifyVars: {
            hack: `true; @import (reference) "${path.resolve('src/style/variables.less')}";`,
          },
          math: 'strict',
          javascriptEnabled: true,
        },
      },
    },

    plugins: [
      vue(),
      vueJsx(),
      viteMockServe({
        mockPath: 'mock',
        /**
         * Mock 开关由环境变量控制，便于前后端联调时随时切换。
         *
         * - 不设置 VITE_USE_MOCK 或设为 'false'：走真实后端接口（联调/生产）
         * - 设为 'true'：使用本地 Mock 数据（前端独立开发、后端未就绪时）
         *
         * 切换后需重启 dev server 才生效。
         */
        enable: VITE_USE_MOCK === 'true',
      }),
      svgLoader(),
    ],

    server: {
      port: 3002,
      host: '0.0.0.0',
      allowedHosts: true,
      proxy: {
        [VITE_API_URL_PREFIX]: {
          target: VITE_API_URL,
          secure: false,
        },
      },
    },
  };
};
