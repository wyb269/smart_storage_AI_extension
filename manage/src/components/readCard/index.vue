<template>
  <div>
    <t-button variant="base" theme="primary" style="background:#0f2645;
    border: 1px solid #0f2645;
    font-family: PingFang SC;
font-weight: 400;
font-style: Medium;
font-size: 13px;
leading-trim: NONE;
line-height: 100%;
letter-spacing: 0px;
padding-bottom: 2px;
width:95px;
--ripple-color:#0f2645;
" @click="readCard">
      <template #icon>
        <!-- <t-icon name="cardmembership" style="margin-right: 3px;" /> -->
        <!-- <t-icon name="creditcard" style="margin-right: 3px;" /> -->
        <img src="@/assets/imgs/readcard.png">
      </template>
      <span style="margin-left: 6px;">读取卡号</span></t-button>
  </div>
</template>
<script setup lang="jsx">
import { OpenReader, M1_ReadSnr, gSetBeep } from './util/Reader750API.js';
import { computed, nextTick, onMounted, onUnmounted, watch, ref, getCurrentInstance, defineEmits } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';

const emit = defineEmits(['result']);

const readCard = () => {
  OpenReader().then(r => {
    console.log(r)
    if (r == 1) {
      ReadCardUID()
    } else {
      MessagePlugin.error('读卡器链接异常!');
      emit('result', { result: '0' })
    }
  })
}

const formatUID = (uidBuffer) => {
  if (!uidBuffer || uidBuffer.length === 0) return "none";
  const uidArray = Array.from(uidBuffer);
  return uidArray.map(byte => byte.toString(16).padStart(2, '0').toUpperCase()).join(" ");
}

const ReadCardUID = async () => {
  console.log("===== 开始执行【读M1卡UID】操作 =====");
  var requet_mode = 0x52;
  console.log("读卡UID参数：请求模式 = 0x" + requet_mode.toString(16).toUpperCase());
  try {
    var ret = await M1_ReadSnr(requet_mode);
    console.log("读M1卡UID原始返回结果：", ret);
    if (ret.status == 0) {
      const uidStr = formatUID(ret.data);
      if (uidStr == 'none') {
        MessagePlugin.error('该卡片无UID数据!');
        emit('result', { result: '0' })
        return
      }
      const uidStrNoSpace = uidStr.replace(/ /g, "");
      MessagePlugin.success('读取磁卡成功!');
      emit('result', { result: '1', data: uidStrNoSpace })
      console.log("✅ 读M1卡UID成功！");
      console.log("   格式化UID（空格分隔）：", uidStr);
      console.log("   紧凑格式UID（无空格）：", uidStrNoSpace);
      gSetBeep(10);
    }
    else {
      MessagePlugin.error('未识别到磁卡!');
      emit('result', { result: '0' })
      // console.error("❌ 读M1卡UID失败！错误码：0x" + ret.status.toString(16).toUpperCase() 
      //       + " → " + explainM1ErrorCode(ret.status));
    }
  } catch (err) {
    console.error("❌ 读M1卡UID异常：", err);
    MessagePlugin.error('未识别到卡片!');
    emit('result', { result: '0' })
  }

}





</script>
<style lang="less" scoped></style>
