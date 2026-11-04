<template>
  <div class="process-record-list">
    <div v-for="record in records" :key="record.recordId" class="process-record-list__item">
      <time class="process-record-list__time">{{
        formatTime ? formatTime(record.processedAt) : record.processedAt
      }}</time>
      <div class="process-record-list__content">
        <slot name="content" :record="record">{{ record.processedBy }}：{{ record.processOpinion || '—' }}</slot>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
interface ProcessRecord {
  recordId: string;
  processedAt: string;
  processedBy: string;
  processOpinion?: string | null;
}

defineProps<{
  records: ProcessRecord[];
  formatTime?: (value: string) => string;
}>();
</script>
<style lang="less" scoped>
.process-record-list {
  display: grid;
  gap: 12px;
}

.process-record-list__item {
  position: relative;
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 2px 0 2px 14px;
  border-left: 2px solid var(--td-component-border);
  overflow-wrap: anywhere;

  &::before {
    position: absolute;
    top: 7px;
    left: -5px;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--td-brand-color);
    content: '';
  }
}

.process-record-list__time {
  color: var(--td-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.process-record-list__content {
  min-width: 0;
  color: var(--td-text-color-primary);
  line-height: 1.5;
}
</style>
