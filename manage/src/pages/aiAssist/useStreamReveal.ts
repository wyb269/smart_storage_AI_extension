import { ref, watch, nextTick } from 'vue';

/**
 * 模拟大模型流式回显：右侧内容分块逐段输出 + 打字动画 + 自动滚动到底部
 * @param keys 内容区块 key 列表（按输出顺序，首项约定为 'ask' 参数追问块）
 * @param getBox 滚动容器 getter
 * @param isAskMode 返回当前结果是否为"AI 追问缺失参数"模式（true 时只展示追问块）
 */
export function useStreamReveal(
  keys: string[],
  getBox: () => HTMLElement | undefined,
  isAskMode?: () => boolean,
) {
  const revealedCount = ref(0);
  const typing = ref(false);
  const streaming = ref(false);
  const generating = ref(false);
  /** AI 追问文案的逐字流式输出内容 */
  const askText = ref('');
  let askFull = '';
  let token = 0;

  function rk(key: string) {
    if (key === 'ask') {
      // 追问模式不跑分块 reveal，跟随已输出的文字显隐
      return !!isAskMode?.() && askText.value.length > 0;
    }
    return revealedCount.value > keys.indexOf(key) && !isAskMode?.();
  }

  function sleep(ms: number) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  /**
   * AI 追问文案逐字打字机输出（模拟大模型流式回复）
   * <p>先“思考”1.2~2.8s（三点动画），再以 28~68ms/字 的节奏输出</p>
   */
  async function typeAsk(message: string) {
    token += 1;
    const my = token;
    askFull = message;
    askText.value = '';
    streaming.value = true;
    generating.value = true;
    // 模拟 AI 思考：打字点先动一会儿再开始输出
    await sleep(1200 + Math.random() * 1600);
    if (my !== token) return;
    typing.value = false;
    for (let i = 1; i <= message.length; i++) {
      if (my !== token) return;
      askText.value = message.slice(0, i);
      await sleep(28 + Math.random() * 40);
    }
    if (my !== token) return;
    streaming.value = false;
    generating.value = false;
  }

  async function reveal() {
    token += 1;
    const my = token;
    streaming.value = true;
    revealedCount.value = 0;
    for (let i = 0; i < keys.length; i++) {
      typing.value = true;
      await sleep(250 + Math.random() * 350);
      if (my !== token) return;
      typing.value = false;
      revealedCount.value = i + 1;
      if (i < keys.length - 1) await sleep(100);
      if (my !== token) return;
    }
    streaming.value = false;
    generating.value = false;
  }

  /** 停止输出，立即展示全部内容 */
  function finishAll() {
    token += 1;
    typing.value = false;
    streaming.value = false;
    generating.value = false;
    revealedCount.value = keys.length;
    askText.value = askFull;
  }

  /** 请求失败/无结果时收尾 */
  function abort() {
    token += 1;
    typing.value = false;
    streaming.value = false;
    generating.value = false;
  }

  watch([revealedCount, typing, askText], async () => {
    await nextTick();
    const box = getBox();
    if (box) box.scrollTo({ top: box.scrollHeight, behavior: 'smooth' });
  });

  return { typing, streaming, generating, askText, rk, reveal, typeAsk, finishAll, abort };
}
