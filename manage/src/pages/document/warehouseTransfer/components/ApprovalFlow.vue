<template>
  <t-dialog :visible="visible" width="600px" class="newDialog newclassNewDialog" :cancel-btn="null" :confirm-btn="null"
    @update:visible="handleVisibleChange">
    <template #header>
      <div class="dialog-header">
        <span>查看审批流</span>
      </div>
    </template>

    <div class="approval-flow-content">
      <div class="timeline-wrapper">
        <div v-for="(item, index) in flowList" :key="index" class="timeline-item">
          <!-- 节点圆 + 连线 -->
          <div class="timeline-node-col">
            <div class="timeline-dot" :class="{ 'timeline-dot--end': item.isEnd }">
              {{ item.isEnd ? '✓' : index + 1 }}
            </div>
            <div v-if="index < flowList.length - 1" class="timeline-line"></div>
          </div>

          <!-- 内容区域 -->
          <div class="timeline-content-col">
            <!-- 审批人姓名 -->
            <div class="timeline-title">{{ item.name }}</div>

            <!-- 审批时间 + 审批状态 -->
            <div v-if="!item.isEnd" class="timeline-meta">
              <span class="timeline-time">{{ item.time }}</span>
              <t-tag :style="getStatusTagStyle(item.status)" variant="light-outline" size="small"
                class="timeline-status-tag">
                {{ item.status }}
              </t-tag>
            </div>

            <!-- 审批意见 -->
            <div v-if="item.opinion" class="timeline-opinion">审批意见：{{ item.opinion }}</div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <t-button theme="default" variant="outline" @click="handleClose"> 关闭 </t-button>
      </div>
    </template>
  </t-dialog>
</template>
<script setup name="ApprovalFlow">
import { computed, ref } from 'vue';

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  rowData: {
    type: Object,
    default: () => ({}),
  },
});

const emit = defineEmits(['update:visible']);

// 审批状态样式映射
const statusColorMap = {
  待提交: { color: '#186DF5', bgColor: '#F2F3FF' },
  审批中: { color: '#EF9645', bgColor: '#FDF4EC' },
  已驳回: { color: '#E34D59', bgColor: '#FDECEE' },
  已作废: { color: '#B0B0B0', bgColor: '#F6F6F8' },
  审批完成: { color: '#2BA471', bgColor: '#E3F9E9' },
  已撤回: { color: '#8E8E8E', bgColor: '#F6F6F8' },
};

const getStatusTagStyle = (status) => {
  const defaultStyle = {
    color: '#999',
    backgroundColor: '#F5F5F5',
    borderColor: '#999',
  };
  const statusStyle = statusColorMap[status] || defaultStyle;
  return {
    color: statusStyle.color,
    backgroundColor: statusStyle.bgColor,
    borderColor: statusStyle.color,
    height: '22px',
    borderRadius: '4px',
  };
};

const statusTextMap = {
  1: '待提交',
  2: '审批中',
  3: '已驳回',
  4: '已作废',
  5: '审批完成',
  6: '已撤回',
};

const flowList = computed(() => {
  const list = props.rowData?.auditFlow || [];
  if (!list.length) {
    return [
      {
        name: '暂无审批记录',
        time: '',
        status: '',
        opinion: '',
        isEnd: true,
      },
    ];
  }

  const reversedList = [...list].reverse();
  const mapped = reversedList.map((item) => ({
    name: item.auditUserName || item.addUserName || '-',
    time: item.auditTime || item.addTime || '-',
    status: statusTextMap[item.status] || '-',
    opinion: item.approvalOpinion || '',
    isEnd: false,
  }));

  const rawStatus = props.rowData?.status ?? reversedList[reversedList.length - 1]?.status;
  const endStatus = Number(rawStatus);
  const shouldShowEnd = endStatus === 4 || endStatus === 5;

  if (!shouldShowEnd) {
    return mapped;
  }

  return [
    ...mapped,
    {
      name: '流程结束',
      time: '',
      status: '',
      opinion: '',
      isEnd: true,
    },
  ];
});

const handleVisibleChange = (value) => {
  emit('update:visible', value);
};

const handleClose = () => {
  emit('update:visible', false);
};
</script>
<style lang="less" scoped>
.dialog-header {
  display: flex;
  align-items: center;
}

.approval-flow-content {
  padding: 20px 0 10px;
  border-top: 1px solid rgba(238, 239, 242, 1);
  border-bottom: 1px solid rgba(238, 239, 242, 1);
  max-height: 60vh;
  overflow-y: auto;
}

.timeline-wrapper {
  padding: 0 20px;
}

.timeline-item {
  display: flex;
  position: relative;
  min-height: 80px;

  &:last-child {
    min-height: auto;

    .timeline-content-col {
      padding-bottom: 0;
    }
  }
}

.timeline-node-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 16px;
  flex-shrink: 0;
}

.timeline-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: #186df5;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
  z-index: 1;

  &--end {
    background-color: #2ba471;
    font-size: 14px;
  }
}

.timeline-line {
  width: 2px;
  flex: 1;
  background-color: #e3e7f0;
  margin: 4px 0;
}

.timeline-content-col {
  flex: 1;
  padding-bottom: 20px;
  padding-top: 3px;
}

.timeline-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  line-height: 22px;
  font-family: PingFang SC;
}

.timeline-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 6px;
}

.timeline-time {
  font-size: 13px;
  color: #8e97a8;
  font-family: PingFang SC;
}

.timeline-status-tag {
  flex-shrink: 0;
}

.timeline-opinion {
  margin-top: 6px;
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  font-family: PingFang SC;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
