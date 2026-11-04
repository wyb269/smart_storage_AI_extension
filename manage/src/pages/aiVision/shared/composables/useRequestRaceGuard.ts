/**
 * 仓门异动功能 - 请求竞态守卫
 *
 * 依据 AI_HANDOFF.md 第 30 节：
 *   「快速切换仓门时，旧请求结果不得覆盖新选择。」
 *
 * 使用方式：
 *   const guard = useRequestRaceGuard();
 *   const token = guard.begin();          // 发起请求前调用
 *   const data = await fetch();
 *   if (!guard.isCurrent(token)) return;   // 已过期则丢弃结果
 */

import { onBeforeUnmount, ref } from 'vue';

export function useRequestRaceGuard() {
  /** 当前有效的令牌；每次 begin 自增 */
  const currentToken = ref(0);
  /** 组件是否已卸载 */
  const disposed = ref(false);

  /**
   * 开始一次受保护的请求。
   * @returns 本次请求的令牌
   */
  function begin(): number {
    currentToken.value += 1;
    return currentToken.value;
  }

  /**
   * 判断令牌是否仍为最新。
   * @param token begin 返回的令牌
   */
  function isCurrent(token: number): boolean {
    return !disposed.value && token === currentToken.value;
  }

  /**
   * 主动使当前所有在途请求失效。
   * 用于切换仓门、离开页面等场景。
   */
  function invalidate(): void {
    currentToken.value += 1;
  }

  onBeforeUnmount(() => {
    disposed.value = true;
    invalidate();
  });

  return {
    begin,
    isCurrent,
    invalidate,
  };
}
