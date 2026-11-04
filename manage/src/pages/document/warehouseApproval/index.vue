<template>
  <div class="container">
    <!-- 搜索表单区域 -->
    <search-form
      v-model="formObj"
      :fields="searchFields"
      :show-expand="false"
      @search="handleSearch"
      @reset="handleReset"
    />

    <!-- 表格区域 -->
    <div class="bottom forNewClassTableAndsearch">
      <t-table
        :loading="loading"
        :data="tableData"
        :columns="tableColumns"
        hover
        resizable
        row-key="id"
        :pagination="pagination"
        @page-change="onPageChange"
      >
        <template #statusTag="{ row }">
          <t-tag :style="getStatusTagStyle(row.status)" variant="light-outline">
            {{ row.statusLabel || row.status }}
          </t-tag>
        </template>
        <template #operation="{ row }">
          <div class="table-operations">
            <t-button theme="primary" variant="text" @click="handleDetail(row)"> 查看 </t-button>
            <t-button theme="primary" variant="text" @click="handleApproval(row)"> 审批 </t-button>
            <t-button theme="primary" variant="text" @click="handleFlow(row)"> 查看审批流 </t-button>
          </div>
        </template>
        <template #empty>
          <t-space>
            <div>
              <img style="width: 120px" src="@/assets/imgs/empty.png" />
              <div class="empty-text">暂无数据</div>
            </div>
          </t-space>
        </template>
      </t-table>
    </div>

    <!-- 查看弹窗（复用 warehouseTransfer 的 Create 组件，type="查看"） -->
    <create v-if="detailVisible" v-model:visible="detailVisible" type="查看" :row-data="currentRow" />

    <!-- 审批弹窗 -->
    <approval-dialog
      v-if="approvalVisible"
      v-model:visible="approvalVisible"
      :row-data="currentRow"
      @submit="handleApprovalSubmit"
    />

    <!-- 审批流弹窗（复用 warehouseTransfer 的 ApprovalFlow 组件） -->
    <approval-flow v-if="flowVisible" v-model:visible="flowVisible" :row-data="currentRow" />
  </div>
</template>
<script setup name="warehouseApproval">
import { MessagePlugin } from 'tdesign-vue-next';
import { computed, onMounted, ref } from 'vue';

import { getDict } from '@/api/sampling';
import {
  listTransferStorageAuditByCondition,
  listTransferStorageAuditFlowByCondition,
  listWarehouseByConditionOnPage,
} from '@/api/warehouseTransfer';
import SearchForm from '@/components/SearchForm/index.vue';

import ApprovalFlow from '../warehouseTransfer/components/ApprovalFlow.vue';
import Create from '../warehouseTransfer/components/Create.vue';
import ApprovalDialog from './components/ApprovalDialog.vue';

// 搜索表单数据
const formObj = ref({
  applicant: '',
  transferType: '',
  fromWarehouse: '',
  toWarehouse: '',
  transferRange: [],
  status: '',
});

const transferTypeOptions = ref([
  { label: '粮情异常倒仓', value: 1 },
  { label: '质量分储倒仓', value: 2 },
  { label: '仓房维修倒仓', value: 3 },
  { label: '设备升级倒仓', value: 4 },
  { label: '仓房熏蒸倒仓', value: 5 },
  { label: '储备粮轮换倒仓', value: 6 },
  { label: '专项储备倒仓', value: 7 },
  { label: '应急避险倒仓', value: 8 },
  { label: '应急供应倒仓', value: 9 },
  { label: '仓容平衡倒仓', value: 10 },
  { label: '品种集中倒仓', value: 11 },
  { label: '实验性倒仓', value: 12 },
  { label: '跨库调拨倒仓', value: 13 },
]);

const warehouseOptions = ref([]);
const grainVarietyOptions = ref([]);

const statusOptions = ref([
  { label: '待提交', value: 1 },
  { label: '审批中', value: 2 },
  { label: '已驳回', value: 3 },
  { label: '已作废', value: 4 },
  { label: '审批完成', value: 5 },
  { label: '已撤回', value: 6 },
]);

const searchFields = computed(() => [
  {
    name: 'applicant',
    label: '申请人',
    type: 'input',
    placeholder: '请输入申请人',
    defaultValue: '',
  },
  {
    name: 'transferType',
    label: '倒仓类型',
    type: 'select',
    options: transferTypeOptions.value,
    placeholder: '请选择倒仓类型',
    defaultValue: '',
  },
  {
    name: 'fromWarehouse',
    label: '移出仓库',
    type: 'select',
    options: warehouseOptions.value,
    placeholder: '请选择移出仓库',
    defaultValue: '',
  },
  {
    name: 'toWarehouse',
    label: '移入仓库',
    type: 'select',
    options: warehouseOptions.value,
    placeholder: '请选择移入仓库',
    defaultValue: '',
  },
  {
    name: 'transferRange',
    label: '起止时间',
    type: 'date-range',
    defaultValue: [],
  },
  {
    name: 'status',
    label: '单据状态',
    type: 'select',
    options: statusOptions.value,
    placeholder: '请选择单据状态',
    defaultValue: '',
  },
]);

const statusColorMap = {
  1: { color: '#186DF5', bgColor: '#F2F3FF' },
  2: { color: '#EF9645', bgColor: '#FDF4EC' },
  3: { color: '#E34D59', bgColor: '#FDECEE' },
  4: { color: '#B0B0B0', bgColor: '#F6F6F8' },
  5: { color: '#2BA471', bgColor: '#E3F9E9' },
  6: { color: '#8E8E8E', bgColor: '#F6F6F8' },
};

// 表格列定义
const tableColumns = [
  { colKey: 'serial-number', title: '序号', width: 60 },
  { colKey: 'applicantName', title: '申请人', width: 100 },
  { colKey: 'addTime', title: '申请时间', width: 170 },
  { colKey: 'transferTypeLabel', title: '倒仓类型', width: 120 },
  { colKey: 'sourceWarehouseName', title: '移出仓库', width: 120 },
  { colKey: 'targetWarehouseName', title: '移入仓库', width: 120 },
  { colKey: 'grainVarietyLabel', title: '粮食品种', width: 110 },
  { colKey: 'estimatedQuantity', title: '预计倒仓量(KG)', width: 150 },
  { colKey: 'transferRangeText', title: '起止时间', width: 180 },
  { colKey: 'status', title: '单据状态', width: 100, cell: 'statusTag' },
  { colKey: 'operation', title: '操作', width: 240, cell: 'operation' },
];

const loading = ref(false);

// 弹窗相关状态
const detailVisible = ref(false);
const approvalVisible = ref(false);
const flowVisible = ref(false);
const currentRow = ref({});

// 分页配置
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showPageSize: true,
  showPreviousAndNextBtn: true,
});

const tableData = ref([]);

// 单据状态样式
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
    height: '26px',
    borderRadius: '4px',
  };
};

const statusLabelMap = {
  1: '待提交',
  2: '审批中',
  3: '已驳回',
  4: '已作废',
  5: '审批完成',
  6: '已撤回',
};

const getTransferTypeLabel = (value) => {
  const match = transferTypeOptions.value.find((item) => item.value === value);
  return match ? match.label : '-';
};

const buildTransferRangeText = (startTime, endTime) => {
  if (!startTime || !endTime) return '-';
  return `${startTime} - ${endTime}`;
};

const getGrainVarietyLabel = (value) => {
  const match = grainVarietyOptions.value.find((item) => item.value === value);
  return match ? match.label : value || '-';
};

const formatListData = (list, startIndex) => {
  return list.map((item, index) => ({
    ...item,
    transferTypeLabel: getTransferTypeLabel(item.transferType),
    grainVarietyLabel: getGrainVarietyLabel(item.grainVariety),
    applicantName: item.addUserName || item.applicant || '-',
    statusLabel: statusLabelMap[item.status] || '-',
    transferRangeText: buildTransferRangeText(item.startTime, item.endTime),
    'serial-number': startIndex + index + 1,
  }));
};

const buildQueryParams = () => {
  const { applicant, transferType, fromWarehouse, toWarehouse, transferRange, status } = formObj.value;
  return {
    pageNo: pagination.value.current,
    pageSize: pagination.value.pageSize,
    pageTotal: true,
    applicant: applicant || undefined,
    transferType: transferType || undefined,
    sourceWarehouseId: fromWarehouse || undefined,
    targetWarehouseId: toWarehouse || undefined,
    status: status || undefined,
    startTime: transferRange?.[0] || undefined,
    endTime: transferRange?.[1] || undefined,
  };
};

const getTableData = async () => {
  loading.value = true;
  const params = buildQueryParams();
  const response = await listTransferStorageAuditByCondition(params);
  if (response?.status?.code !== '200') {
    MessagePlugin.error(response?.status?.message || '获取倒仓审批列表失败');
    loading.value = false;
    return;
  }

  const list = response.result?.data || [];
  pagination.value.total = response.result?.total || 0;
  const startIndex = (pagination.value.current - 1) * pagination.value.pageSize;
  tableData.value = formatListData(list, startIndex);
  loading.value = false;
};

// 查询
const handleSearch = () => {
  pagination.value.current = 1;
  getTableData();
};

// 重置
const handleReset = () => {
  pagination.value.current = 1;
  getTableData();
};

// 分页变化
const onPageChange = (pageInfo) => {
  pagination.value.current = pageInfo.current;
  pagination.value.pageSize = pageInfo.pageSize;
  getTableData();
};

// 查看
const handleDetail = (row) => {
  currentRow.value = { ...row };
  detailVisible.value = true;
};

// 审批
const handleApproval = (row) => {
  currentRow.value = { ...row };
  approvalVisible.value = true;
};

// 查看审批流
const handleFlow = async (row) => {
  const response = await listTransferStorageAuditFlowByCondition({
    pageNo: 1,
    pageSize: 50,
    pageTotal: true,
    transferId: row.id,
  });
  if (response?.status?.code !== '200') {
    MessagePlugin.error(response?.status?.message || '获取审批流失败');
    return;
  }
  currentRow.value = {
    ...row,
    auditFlow: response.result?.data || [],
  };
  flowVisible.value = true;
};

// 审批提交后刷新列表
const handleApprovalSubmit = () => {
  getTableData();
};

const getGrainVarietyOptions = () => {
  getDict('GRAIN_VARIETY')
    .then((res) => {
      const data = res.result.listDictData || [];
      grainVarietyOptions.value = data.map((item) => ({
        label: item.label,
        value: item.description,
      }));
    })
    .catch((error) => {
      console.error('获取粮食品种失败:', error);
    });
};

const getWarehouseOptions = async () => {
  const response = await listWarehouseByConditionOnPage({
    id: '',
    warehouseName: '',
    warehouseType: 0,
    grainVariety: '',
    state: 0,
  });
  if (response?.status?.code !== '200') {
    MessagePlugin.error(response?.status?.message || '获取仓库列表失败');
    return;
  }
  const list = response.result?.data || response.result || [];
  warehouseOptions.value = list.map((item) => ({
    label: item.warehouseName || item.name || '-',
    value: String(item.id ?? item.warehouseId ?? ''),
  }));
};

onMounted(() => {
  getGrainVarietyOptions();
  getWarehouseOptions();
  getTableData();
});
</script>
<style lang="less" scoped>
.container {
  width: 100%;
}

.top,
.bottom {
  padding: 15px;
  background-color: #fff;
  border-radius: 4px;
}

.bottom {
  :deep(.t-button--variant-text) {
    padding-left: 0;
    padding-right: 0;
  }
}

.table-operations {
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
}

.table-operations :deep(.t-button--variant-text) {
  padding-left: 0;
  padding-right: 0;
}

.table-operations :deep(.t-button--variant-text) + :deep(.t-button--variant-text) {
  margin-left: 12px;
}

.empty-text {
  font-family: PingFang SC;
  font-weight: 300;
  font-size: 13px;
  line-height: 100%;
  color: rgba(149, 149, 149, 1);
  text-align: center;
  margin-top: 4px;
}

::v-deep .t-pagination {
  font-family: PingFang SC;

  .t-pagination__total {
    margin-right: 20px;
    flex: none;
  }

  .t-pagination__jump {
    background-color: transparent;
    border: none;
  }
}
</style>
<style lang="less">
.my-icon-btn .t-button__text {
  display: flex;
  justify-content: center;
  align-items: center;

  .t-icon {
    margin-right: 6px;
  }
}

/* 新增搜索按钮样式 */
.search-btn.t-button.t-button--theme-primary.t-button--variant-base {
  background-color: #f59e0b;
  border-color: #f59e0b;

  &:hover {
    background-color: #e5940a;
    border-color: #e5940a;
  }

  &:not(.t-is-disabled):not(.t-button--ghost) {
    --ripple-color: var(--td-warning-color-active);
  }
}

/* 新增重置按钮样式 */
.reset-btn.t-button.t-button--theme-primary.t-button--variant-outline {
  color: #f59e0b;
  border-color: #f59e0b;

  &:hover {
    color: #e5940a;
    border-color: #e5940a;
    background-color: rgba(245, 158, 11, 0.1);
  }

  &:not(.t-is-disabled):not(.t-button--ghost) {
    --ripple-color: rgba(245, 158, 11, 0.1);
  }
}
</style>
