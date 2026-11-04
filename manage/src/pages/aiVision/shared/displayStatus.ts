/**
 * AI 视觉类功能 · 页面综合展示状态计算
 *
 * 基线依据：AI_HANDOFF.md 第 19.4 节
 *
 * 计算优先级：
 *   1. 数据不足（无设备状态，或状态为 UNKNOWN）→ 未知
 *   2. 设备状态为 OFFLINE → 离线
 *   3. 设备在线且存在有效告警 → 异常
 *   4. 设备在线且无有效告警 → 正常
 *
 * 重要：页面不得仅依赖后端一个含义模糊的字符串判断颜色，
 * 因此本函数是所有状态卡片配色的唯一依据。
 *
 * 各功能共用的语义（设备在线 / 告警）在这里判定；
 * 心跳时间只供诊断，不覆盖接口明确给出的在线状态。
 * 具体文案（异常 vs 遮挡 vs 积水）由调用方自己的标签映射决定。
 */

import { HEARTBEAT_TIMEOUT_MS } from './constants';
import type { AlarmStatus, DeviceStatus, DisplayStatus, DisplayStatusInput } from './types';

export type { DisplayStatusInput } from './types';

/** 告警是否视为「有效告警」 */
function isActiveAlarm(alarmStatus?: AlarmStatus | null): boolean {
  return alarmStatus === 'ACTIVE';
}

/** 设备是否在线 */
function isOnline(deviceStatus?: DeviceStatus | null): boolean {
  return deviceStatus === 'ONLINE';
}

/**
 * 心跳是否超时。
 * 无心跳时间时返回 false ——「无数据」不等于「超时」，
 * 超时与否交由 deviceStatus 判断，此处只处理有明确时间戳的情况。
 */
export function isHeartbeatTimeout(lastHeartbeatAt?: string | null, now: number = Date.now()): boolean {
  if (!lastHeartbeatAt) return false;
  const ts = new Date(lastHeartbeatAt).getTime();
  if (Number.isNaN(ts)) return true;
  return now - ts > HEARTBEAT_TIMEOUT_MS;
}

/** 当前连接状态；告警与布防状态由各页面另行展示。 */
export function connectionStatusLabel(input: Pick<DisplayStatusInput, 'deviceStatus' | 'lastHeartbeatAt'>): string {
  if (!input.deviceStatus || input.deviceStatus === 'UNKNOWN') return '未知';
  return input.deviceStatus === 'ONLINE' ? '在线' : '离线';
}

/**
 * 计算页面综合展示状态。
 *
 * 注意：设备状态为 UNKNOWN 表示「无法判定」，
 * 既不是「明确离线」也不是「在线」，因此落入数据不足一条判为 UNKNOWN，
 * 不得直接显示为离线，避免把「不知道」误报成「已掉线」。
 *
 * @param input 卡片条目的原始状态
 * @param _now  保留旧调用签名；展示状态以接口设备状态为准
 */
export function computeDisplayStatus(input: DisplayStatusInput, _now: number = Date.now()): DisplayStatus {
  const { deviceStatus, alarmStatus } = input;

  // 优先级 0：数据不足。无设备状态，或状态为 UNKNOWN（无法判定）
  if (!deviceStatus || deviceStatus === 'UNKNOWN') return 'UNKNOWN';

  // 优先级 1：接口返回的设备状态是当前状态的唯一来源。
  if (!isOnline(deviceStatus)) {
    return 'OFFLINE';
  }

  // 优先级 2：在线且存在有效告警
  if (isActiveAlarm(alarmStatus)) return 'ABNORMAL';

  // 优先级 3：在线且无有效告警
  return 'NORMAL';
}

/**
 * 展示状态是否需要给出「不自动播放」的提示。
 * 离线与未知一律不自动播放。
 */
export function shouldAutoPlay(displayStatus: DisplayStatus, canPlay: boolean): boolean {
  return displayStatus === 'NORMAL' || displayStatus === 'ABNORMAL' ? canPlay : false;
}

/**
 * 从状态卡片列表中挑选默认选中项。
 *
 * 默认选择第一个在线项；全部离线或未知时选择第一个但不自动播放。
 * 用泛型，因此仓门、摄像头、防区、皮带机、监测点位都能复用。
 */
export function pickDefaultItem<T extends DisplayStatusInput & { sortNo?: number }>(
  items: T[],
  now: number = Date.now(),
): { selected: T | null; autoPlay: boolean } {
  if (!items.length) return { selected: null, autoPlay: false };

  const sorted = [...items].sort((a, b) => (a.sortNo ?? 0) - (b.sortNo ?? 0));

  const online = sorted.find((item) => {
    const status = computeDisplayStatus(item, now);
    return status !== 'OFFLINE' && status !== 'UNKNOWN';
  });

  if (online) {
    const status = computeDisplayStatus(online, now);
    return { selected: online, autoPlay: status === 'NORMAL' || status === 'ABNORMAL' };
  }

  // 全部离线或未知：选择第一个，但不自动播放
  return { selected: sorted[0], autoPlay: false };
}

/**
 * 判断某处理状态的事件是否允许查看详情。
 */
export function canViewEvent(processStatus?: string | null): boolean {
  if (!processStatus) return true;
  return (
    processStatus === 'PENDING' ||
    processStatus === 'PROCESSING' ||
    processStatus === 'VERIFIED_TRUE' ||
    processStatus === 'VERIFIED_FALSE' ||
    processStatus === 'CLOSED'
  );
}
