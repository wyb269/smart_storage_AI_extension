<template>
  <span v-if="showLastReport" class="last-report" :class="{ 'last-report--alarm': isAlarm }" :title="tooltip">
    <t-icon :name="isAlarm ? 'error-circle' : 'check-circle'" />
    上次{{ isAlarm ? alarmLabel : normalLabel }}
  </span>
</template>
<script setup lang="ts">
import { computed } from 'vue';

import { isHeartbeatTimeout } from '../displayStatus';

const props = withDefaults(
  defineProps<{
    deviceStatus?: string | null;
    alarmStatus?: string | null;
    lastHeartbeatAt?: string | null;
    normalLabel?: string;
    alarmLabel?: string;
  }>(),
  { normalLabel: '正常', alarmLabel: '告警' },
);

// 只在原始状态为在线、但心跳过期时展示末次状态；它不是当前实时状态。
const showLastReport = computed(
  () => props.deviceStatus === 'ONLINE' && !!props.lastHeartbeatAt && isHeartbeatTimeout(props.lastHeartbeatAt),
);
const isAlarm = computed(() => props.alarmStatus === 'ACTIVE');
const tooltip = computed(() => `末次心跳：${props.lastHeartbeatAt ?? '未知'}；当前连接状态：离线`);
</script>
<style lang="less" scoped>
.last-report {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  align-self: flex-start;
  max-width: 100%;
  padding: 1px 4px;
  border-radius: 3px;
  background: #e8f8f0;
  color: #087f57;
  font-size: 11px;
  line-height: 15px;
  white-space: nowrap;
}

.last-report--alarm {
  background: #fdeeee;
  color: #b43b35;
}
</style>
