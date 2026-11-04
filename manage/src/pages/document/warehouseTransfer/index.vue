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
      <div class="btn-box">
        <t-button theme="primary" variant="base" class="search-btn my-icon-btn" @click="handleAdd">
          <template #icon> <add-icon /> </template>新增
        </t-button>
      </div>

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
            <t-button theme="primary" variant="text" @click="handleEdit(row)"> 编辑 </t-button>
            <t-dropdown trigger="click" :min-column-width="120">
              <t-button theme="primary" variant="text"> 更多 </t-button>
              <template #dropdown>
                <t-dropdown-menu>
                  <t-dropdown-item @click="handleFlow(row)"> 查看审批流 </t-dropdown-item>
                  <t-dropdown-item @click="handleSubmit(row)"> 提交 </t-dropdown-item>
                  <t-dropdown-item @click="handleWithdraw(row)"> 撤回 </t-dropdown-item>
                  <t-dropdown-item @click="handleDelete(row)"> 删除 </t-dropdown-item>
                  <t-dropdown-item @click="handleVoid(row)"> 作废 </t-dropdown-item>
                </t-dropdown-menu>
              </template>
            </t-dropdown>
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

    <!-- 弹窗组件 -->
    <create
      v-if="dialogVisible"
      v-model:visible="dialogVisible"
      :type="dialogType"
      :row-data="currentRow"
      @submit="handleDialogSubmit"
      @capacity="capacityVisible = true"
    />

    <!-- 审批流弹窗 -->
    <approval-flow v-if="flowVisible" v-model:visible="flowVisible" :row-data="currentRow" />

    <!-- 撤回提示弹窗 -->
    <t-dialog
      v-model:visible="withdrawVisible"
      header="确认提示"
      class="newDialog newclassNewDialog"
      :cancel-btn="null"
      :confirm-btn="{ theme: 'warning', variant: 'base' }"
      @confirm="withdrawVisible = false"
    >
      <div class="deletCon">
        <div class="delectConHeader">
          <t-icon name="error-circle-filled" size="18px" color="#EF9645" style="margin-right: 8px" />
          <span class="a">下一环节存在审批流，不允许撤回！</span>
        </div>
      </div>
    </t-dialog>

    <!-- 倒仓量超出容量提示弹窗 -->
    <t-dialog
      v-model:visible="capacityVisible"
      header="确认提示"
      class="newDialog newclassNewDialog"
      :cancel-btn="null"
      :confirm-btn="{ theme: 'warning', variant: 'base' }"
      @confirm="capacityVisible = false"
    >
      <div class="deletCon">
        <div class="delectConHeader">
          <t-icon name="error-circle-filled" size="18px" color="rgba(255, 41, 75, 1)" style="margin-right: 8px" />
          <span class="a">倒仓量超出仓库可容量，不允许倒仓！</span>
        </div>
      </div>
    </t-dialog>
  </div>
</template>
<script setup name="warehouseTransfer">
import { AddIcon } from 'tdesign-icons-vue-next';
import { MessagePlugin } from 'tdesign-vue-next';
import { computed, onMounted, ref } from 'vue';

import { getDict } from '@/api/sampling';
import {
  deleteTransferStorageInfo,
  listTransferStorageAuditFlowByCondition,
  listTransferStorageInfoByCondition,
  listWarehouseByConditionOnPage,
  processCancel,
  processPending,
  processVoided,
} from '@/api/warehouseTransfer';
import SearchForm from '@/components/SearchForm/index.vue';

import ApprovalFlow from './components/ApprovalFlow.vue';
import Create from './components/Create.vue';

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
  { colKey: 'status', title: '单据状态', width: 110, cell: 'statusTag' },
  { colKey: 'operation', title: '操作', width: 200, cell: 'operation' },
];

const loading = ref(false);

// 弹窗相关状态
const dialogVisible = ref(false);
const dialogType = ref('');
const currentRow = ref({});

// 审批流弹窗
const flowVisible = ref(false);

// 撤回提示弹窗
const withdrawVisible = ref(false);

// 容量超出提示弹窗
const capacityVisible = ref(false);

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
  const response = await listTransferStorageInfoByCondition(params);
  if (response?.status?.code !== '200') {
    MessagePlugin.error(response?.status?.message || '获取倒仓管理列表失败');
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

// 操作按钮
const handleAdd = () => {
  dialogType.value = '新增';
  currentRow.value = {};
  dialogVisible.value = true;
};

const handleEdit = (row) => {
  dialogType.value = '编辑';
  currentRow.value = { ...row };
  dialogVisible.value = true;
};

const handleDetail = (row) => {
  dialogType.value = '查看';
  currentRow.value = { ...row };
  dialogVisible.value = true;
};

// 弹窗提交后刷新列表
const handleDialogSubmit = () => {
  pagination.value.current = 1;
  getTableData();
};

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

const handleSubmit = async (row) => {
  const response = await processPending(row.id);
  if (response?.status?.code !== '200') {
    MessagePlugin.error(response?.status?.message || '提交失败');
    return;
  }
  MessagePlugin.success('提交成功');
  getTableData();
};

const handleWithdraw = async (row) => {
  const response = await processCancel(row.id);
  if (response?.status?.code !== '200') {
    MessagePlugin.error(response?.status?.message || '撤回失败');
    return;
  }
  MessagePlugin.success('撤回成功');
  getTableData();
};

const handleDelete = async (row) => {
  const response = await deleteTransferStorageInfo({ ids: [row.id] });
  if (response?.status?.code !== '200') {
    MessagePlugin.error(response?.status?.message || '删除失败');
    return;
  }
  MessagePlugin.success('删除成功');
  getTableData();
};

const handleVoid = async (row) => {
  const response = await processVoided(row.id);
  if (response?.status?.code !== '200') {
    MessagePlugin.error(response?.status?.message || '作废失败');
    return;
  }
  MessagePlugin.success('作废成功');
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
    maxWeight: Number(item.maxWeight ?? 0),
    currentWeight: Number(item.currentWeight ?? 0),
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

.btn-box {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 5px 0 25px 0;
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

// 提示弹窗样式
::v-deep .deletCon {
  width: 100%;
  padding: 10px 0;
}

::v-deep .delectConHeader {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
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
