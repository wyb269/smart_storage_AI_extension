/**
 * 周界入侵识别功能 · 页面展示状态计算
 *
 * 为什么要有这一层（本功能相对模板的唯一结构性差异）：
 *
 * 共享层 `shared/displayStatus.ts` 的 `computeDisplayStatus` 只吃三样输入
 * —— deviceStatus / alarmStatus / lastHeartbeatAt，产出四态
 * NORMAL / ABNORMAL / OFFLINE / UNKNOWN。周界原型要的是第四种语义：
 * **人工撤防**（defense_status = DISARMED）时卡片必须显示「未布防」。
 * 这一维既不是设备态也不是告警态，共享层的判定链路里没有任何位置可以表达它，
 * 而共享层是多个功能共用的基座、禁止为单个功能开洞。
 *
 * 所以这里做**本地包装**而不是改共享文件：
 *   1. 先判布防 —— DISARMED 直接返回本功能独有的 'DISARMED' 态；
 *   2. 其余情况原样回落到共享的 computeDisplayStatus，
 *      离线/告警/未知的优先级与全项目保持一致。
 *
 * 优先级口径（与需求描述逐条对应）：
 *   defense_status = DISARMED                      → 未布防
 *   device_status ≠ ONLINE 或心跳超过 90 分钟      → 离线
 *   alarm_status = ACTIVE                          → 入侵报警
 *   device_status = UNKNOWN                        → 未知
 *   其余                                            → 正常布防
 *
 * ⚠️ 「未布防」排在「离线」之前：撤防是人做出来的决定，
 *    值班人员最需要知道的正是「这一段现在没人管」。
 *    若把离线排在前面，一段既撤防又掉线的墙只会显示「离线」，
 *    撤防这条风险信息就被设备故障盖掉了。
 */

import { computeDisplayStatus, isHeartbeatTimeout } from '../../shared/displayStatus';
import type { AlarmStatus, DeviceStatus } from '../../shared/types';
import type { PerimeterDisplayStatus } from '../types';

/** 参与状态计算所需的最小字段，避免与完整卡片类型耦合 */
export interface PerimeterDisplayInput {
  deviceStatus?: DeviceStatus | string | null;
  alarmStatus?: AlarmStatus | string | null;
  lastHeartbeatAt?: string | null;
  defenseStatus?: string | null;
}

/**
 * 计算防区段的页面展示状态。
 *
 * @param input 卡片条目的原始状态
 * @param now   当前时间戳，便于测试注入
 */
export function computePerimeterDisplayStatus(
  input: PerimeterDisplayInput,
  now: number = Date.now(),
): PerimeterDisplayStatus {
  // 优先级 0：人工撤防。共享层没有这一态，必须在回落之前先判掉。
  if (input.defenseStatus === 'DISARMED') return 'DISARMED';

  // 其余三态（正常/报警/离线/未知）语义与全项目一致，直接复用共享计算。
  return computeDisplayStatus(
    {
      deviceStatus: input.deviceStatus as DeviceStatus | null | undefined,
      alarmStatus: input.alarmStatus as AlarmStatus | null | undefined,
      lastHeartbeatAt: input.lastHeartbeatAt,
    },
    now,
  );
}

/** 该展示状态下右侧是否允许出画面 */
export function isPlayableStatus(status: PerimeterDisplayStatus): boolean {
  return status === 'NORMAL' || status === 'ABNORMAL';
}

/** 心跳是否超时（透传共享实现，供卡片副标题等场景单独使用） */
export { isHeartbeatTimeout };

/**
 * 挑选默认选中的防区段。
 *
 * 共享层的 pickDefaultItem 只跳过离线与未知，会把「未布防」当成正常项选中，
 * 于是首屏右侧出现「已撤防、不提供实时画面」的空窗，因此本功能自带一份：
 *   1. 优先第一个「可出画面」（正常布防 / 入侵报警）的段；
 *   2. 全不可播时退到第一个非离线、非未知的段（即未布防段，仍可看元信息）；
 *   3. 再退到排序后的第一段，但不自动播放。
 */
export function pickDefaultPoint<T extends PerimeterDisplayInput & { sortNo?: number }>(
  items: T[],
  now: number = Date.now(),
): { selected: T | null; autoPlay: boolean } {
  if (!items.length) return { selected: null, autoPlay: false };

  const sorted = [...items].sort((a, b) => (a.sortNo ?? 0) - (b.sortNo ?? 0));
  const statusOf = (item: T) => computePerimeterDisplayStatus(item, now);

  const playable = sorted.find((item) => isPlayableStatus(statusOf(item)));
  if (playable) {
    return { selected: playable, autoPlay: true };
  }

  const visible = sorted.find((item) => {
    const status = statusOf(item);
    return status !== 'OFFLINE' && status !== 'UNKNOWN';
  });
  if (visible) {
    return { selected: visible, autoPlay: false };
  }

  return { selected: sorted[0], autoPlay: false };
}
