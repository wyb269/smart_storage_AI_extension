/**
 * AI 视觉类功能 · 共享类型
 *
 * 这里只放**跨功能语义一致**的状态类型。
 * 各功能自己的业务类型（事件、证据、处理记录等）仍然放在自己的 types 目录，
 * 不要塞进共享层。
 *
 * 判定依据：设备连接状态、告警状态、页面综合展示状态这三者
 * 在所有「摄像头 + 状态卡片 + 历史记录」形态的功能里语义完全相同，
 * 因此下沉到共享层，由 displayStatus.ts 统一计算。
 */

/** 设备连接状态 */
export type DeviceStatus = 'ONLINE' | 'OFFLINE' | 'UNKNOWN';

/** 告警状态 */
export type AlarmStatus = 'NORMAL' | 'ACTIVE' | 'RECOVERED';

/**
 * 页面综合展示状态。
 *
 * 由 computeDisplayStatus 计算得出，是卡片配色的唯一依据。
 * 注意 UNKNOWN 与 OFFLINE 语义不同：「不知道」不等于「已掉线」。
 */
export type DisplayStatus = 'NORMAL' | 'ABNORMAL' | 'OFFLINE' | 'UNKNOWN';

/** computeDisplayStatus 的输入 */
export interface DisplayStatusInput {
  deviceStatus?: DeviceStatus | null;
  alarmStatus?: AlarmStatus | null;
  lastHeartbeatAt?: string | null;
}

/**
 * 带排序与展示名的状态卡片条目。
 *
 * 各功能的状态网格（仓门 / 摄像头 / 防区 / 皮带机 / 监测点位…）
 * 都满足这个形状，因此 pickDefaultItem 可以泛型复用。
 */
export interface StatusCardItem extends DisplayStatusInput {
  id: string;
  name: string;
  sortNo?: number;
}
