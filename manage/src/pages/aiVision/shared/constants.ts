/**
 * AI 视觉类功能 · 共享常量
 *
 * 只放跨功能语义完全一致的部分：
 *   - 轮询间隔与退避参数（第 21.4 节的统一要求）
 *   - 展示状态的色值与 TDesign 主题
 *
 * **不放中文标签**：同一个 DisplayStatus 在不同功能里文案不同
 * （仓门异动叫「异常」，视频遮挡叫「遮挡」，地面积水叫「积水」），
 * 因此标签由各功能的 constants 自己定义。
 */

import type { DisplayStatus } from './types';

/** 轮询间隔与退避（第 21.4 节 / 第 37 节） */
export const POLLING = {
  /** 设备/点位状态 10 秒 */
  ITEM_INTERVAL: 10_000,
  /** 首页最近事件 15 秒 */
  EVENT_INTERVAL: 15_000,
  /** 连续失败退避基数（毫秒），按次数指数递增 */
  RETRY_BASE_DELAY: 3_000,
  /** 连续失败退避上限（毫秒） */
  RETRY_MAX_DELAY: 30_000,
} as const;

/**
 * 心跳超时阈值：90 分钟。
 *
 * ⚠️ 这是真实故障源：数据库里 device_status 明明是 ONLINE，
 * 但种子数据的 last_heartbeat_at 是几天前，超过本阈值后
 * computeDisplayStatus 一律判为 OFFLINE，页面表现为「全部离线」。
 * 因此任何走这条判定链路的**种子数据 / Mock 数据**，
 * 心跳时间必须每次基于 Date.now() 重新计算，不能在模块初始化时固定。
 */
export const HEARTBEAT_TIMEOUT_MINUTES = 90;

export const HEARTBEAT_TIMEOUT_MS = HEARTBEAT_TIMEOUT_MINUTES * 60 * 1000;

/** 展示状态色值（第 16.3 节的颜色语义，跨功能一致） */
export const DISPLAY_STATUS_COLOR: Record<DisplayStatus, string> = {
  NORMAL: '#00a870',
  ABNORMAL: '#d54941',
  OFFLINE: '#86909c',
  UNKNOWN: '#c9cdd4',
};

/**
 * 展示状态的 TDesign tag 主题。
 * 用 as const 保留字面量类型，避免赋值给组件属性时类型不匹配。
 */
export const DISPLAY_STATUS_THEME = {
  NORMAL: 'success',
  ABNORMAL: 'danger',
  OFFLINE: 'default',
  UNKNOWN: 'warning',
} as const satisfies Record<DisplayStatus, string>;

/**
 * 展示状态对应的卡片图标。
 * 使用 TDesign 官方图标名，不用 emoji 或字符代替。
 */
export const DISPLAY_STATUS_ICON: Record<DisplayStatus, string> = {
  NORMAL: 'video-camera',
  ABNORMAL: 'error-circle',
  OFFLINE: 'poweroff',
  UNKNOWN: 'help-circle',
};

/** 分页默认值（第 18.4 节） */
export const PAGINATION_DEFAULT = {
  PAGE_SIZE: 10,
  /** 历史默认查询最近 7 天（第 18.2、37 节） */
  DEFAULT_DAYS: 7,
} as const;

/** 首页最近事件条数（第 16.5 节） */
export const RECENT_EVENT_LIMIT = 5;
