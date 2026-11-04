<template>
  <div class="smart-card-page">
    <div class="top-container">
      <div class="top-header">
        <h2 class="title"> <img :src="smartCardIcon" alt="智能卡图标" class="title-icon" />智能卡管理</h2>
        <readCard @result="handleCardHardwareResult" class="read-card-btn-wrapper" />
      </div>
      <div class="forNewclass">
        <div class="top-operation">
          <t-form-item label="物理卡编号" class="card-number-form-item">
            <t-input v-model="physicalCardNumber" class="input-card" disabled placeholder="读卡后自动填充" />
          </t-form-item>
          <t-button theme="primary" @click="handleAddCard" class="add-btn">+ 添加</t-button>
        </div>
      </div>
    </div>

    <div class="forNewClassTableAndsearch">
      <t-table :loading="loading" :data="tableData" :columns="tableColumns" row-key="id" :pagination="pagination"
        @page-change="onPageChange" class="table-container">
        <template #statusTag="{ row }">
          <t-tag :style="getCardStateTagStyle(row.state)" variant="light-outline">
            {{ row.state === 1 ? '已发卡' : '未发卡' }}
          </t-tag>
        </template>
        <template #processStatusTag="{ row }">
          <template v-if="row.processStatus === '-'">
            {{ row.processStatus }}
          </template>
          <t-tag v-else :style="getStatusTagStyle(row.processStatus)" variant="light-outline">
            {{ row.processStatus }}
          </t-tag>
        </template>
        <template #operation="{ row }">
          <t-button theme="danger" variant="text" @click="handleClickDelete(row)">
            删除
          </t-button>
        </template>
      </t-table>
    </div>

    <t-dialog v-model:visible="confirmVisible" header="删除确认" class="newDialog"
      :cancelBtn="{ theme: 'default', variant: 'outline' }" :confirmBtn="{ theme: 'success', variant: 'base' }"
      @confirm="doDelete">
      <t-space direction="vertical" style="width: 100%">
        <div class="deletCon">
          <div class="delectConHeader">
            <t-icon name="error-circle-filled" size="18px" color="rgba(255, 41, 75, 1)" />
            <span class="a">确认删除该智能卡</span>
          </div>
          <div class="b">智能卡一旦删除，相关数据将无法恢复，请谨慎操作。</div>
        </div>
      </t-space>
    </t-dialog>
  </div>
</template>

<script setup lang="jsx">
import { onMounted, ref, computed } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';
import readCard from '@/components/readCard/index.vue';
import { saveSmartCard, listSmartCardByPhysicalCard, deleteSmartCard, swipeCard } from '@/api/smartcard';
import smartCardIcon from "@/assets/smartcard/icon1.png"

//清空卡片响应式数据
const clearCardData = () => {
  physicalCardNumber.value = '';
  queryCardData.value = {};
};

//解析卡片
const parseCardData = (res) => {
  if (!res) return {};
  return (res.id || (Array.isArray(res) && res.length > 0))
    ? (Array.isArray(res) ? res[0] : res)
    : {};
};

const mapJobType = (jobType) => {
  switch (jobType) {
    case 1:
      return '入库';
    case 2:
      return '出库';
    default:
      return '-';
  }
};

// 流程状态文字映射
const statusMap = {
  logIn: "待扦样",
  sampled: "待质检",
  inspected: "待称重",
  weighed: "待值仓",
  warehoused: "待称皮",
  tareWeighed: "待结算",
  settled: "待销卡",
  logout: "已结束",
  pendCard: "待销卡"
};

// 流程状态颜色映射
const statusColorMap = {
  待扦样: { color: "#186DF5", bgColor: "#F2F3FF" },
  待质检: { color: "#5FC486", bgColor: "#EDF8F1" },
  待称重: { color: "#EF9645", bgColor: "#FDF4EC" },
  待值仓: { color: "#EA5C38", bgColor: "#FCEBE7" },
  待称皮: { color: "#35B5A4", bgColor: "#E6F5F3" },
  待结算: { color: "#8969E6", bgColor: "#F8F6FD" },
  待销卡: { color: "#ecc100", bgColor: "#FFF9DD" },
  已结束: { color: "#CDCDCD", bgColor: "#F6F6F8" },
  // '-': { color: "#999", bgColor: "#F5F5F5" }
};

//发卡状态颜色映射
const cardStateColorMap = {
  1: { color: "#2BA471", bgColor: "#EDF8F1" }, // 已发卡
  2: { color: "#186DF5", bgColor: "#F2F3FF" }  // 未发卡
};


const getCardStateTagStyle = (state) => {
  const defaultStyle = {
    color: "#999",
    backgroundColor: "#F5F5F5",
    borderColor: "#999",
  };
  const stateStyle = cardStateColorMap[state] || defaultStyle;
  return {
    color: stateStyle.color,
    backgroundColor: stateStyle.bgColor,
    borderColor: stateStyle.color,
  };
};

const getStatusTagStyle = (processStatus) => {
  const defaultStyle = {
    color: "#999",
    backgroundColor: "#F5F5F5",
    borderColor: "#999",
  };
  const statusStyle = statusColorMap[processStatus] || defaultStyle;
  return {
    color: statusStyle.color,
    backgroundColor: statusStyle.bgColor,
    borderColor: statusStyle.color,
  };
};

const mapCardToTable = (item) => {
  if (!item || Object.keys(item).length === 0) return {};

  let processStatus = "-";
  if (item.taskFlowJobStatus) {
    if (item.taskFlowJobStatus === "pendCard") {
      item.taskFlowJobStatus = "settled";
    }
    processStatus = statusMap[item.taskFlowJobStatus] || "-";
  }

  return {
    ...item,
    serial: '',
    physicalCardNumber: item.physicalCardNumber,
    addUser: item.taskFlowAddUserName || '未知用户',
    addTime: item.taskFlowAddTime || '-',
    state: item.state || 2,
    jobNo: item.taskFlowJobNo || '-',
    jobType: mapJobType(item.taskFlowJobType),
    processStatus: processStatus
  };
};

const physicalCardNumber = ref('');
const queryCardData = ref({});
const tableData = ref([]);
const loading = ref(false);
const confirmVisible = ref(false);
const deleteId = ref('');

//分页配置
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showPageSize: true,
  showPreviousAndNextBtn: true,
});

//表格列
const tableColumns = [
  {
    colKey: "serial",
    title: "序号",
    width: 60,
    fixed: "left",
  },
  {
    colKey: "cardNo",
    title: "智能卡编号",
    fixed: "left",
  },
  {
    colKey: "physicalCardNumber",
    title: "物理卡编号",
  },
  {
    colKey: "state",
    title: "状态",
    cell: "statusTag"
  },
  {
    colKey: "addUser",
    title: "发卡人",
  },
  {
    colKey: "addTime",
    title: "发卡时间",
  },
  {
    colKey: "jobNo",
    title: "作业编号",
  },
  {
    colKey: "jobType",
    title: "作业类型",
  },
  {
    colKey: "processStatus",
    title: "流程状态",
    cell: "processStatusTag"
  },
  {
    colKey: "operation",
    title: "操作",
    width: 80,
    align: "center",
    cell: "operation",
    fixed: "right",
  },
];


//获取表格数据
const getTableData = async (queryCardNo = '') => {
  loading.value = true;
  try {
    const res = await listSmartCardByPhysicalCard(queryCardNo);
    const validCardList = Array.isArray(res)
      ? res
      : (res?.result?.data && Array.isArray(res.result.data))
        ? res.result.data
        : (res?.result && Array.isArray(res.result))
          ? res.result
          : (res?.data && Array.isArray(res.data))
            ? res.data
            : [];

    const tableDataWithSerial = validCardList.map((item, index) => ({
      ...mapCardToTable(item),
      serial: index + 1
    }));

    tableData.value = tableDataWithSerial;
    pagination.value.total = validCardList.length;
  } catch (error) {
    console.error(error);
    MessagePlugin.error('表格加载失败，请联系管理员');
    tableData.value = [];
    pagination.value.total = 0;
  } finally {
    loading.value = false;
  }
};

const logCardReadResult = (r) => {
  console.log('[读卡接口返回完整r对象]：', r);
  console.log('[提取到的物理卡编号]：', r.data || '未获取到卡号');
  console.log('[读卡状态]：', r.result === '1' ? '读卡成功' : '读卡失败');
};

const handleCardHardwareResult = async (r) => {
  logCardReadResult(r);
  if (r.result === '1') {
    physicalCardNumber.value = (r.data || '').trim();
    try {
      const res = await listSmartCardByPhysicalCard(physicalCardNumber.value);
      const parsedCardData = parseCardData(res);
      queryCardData.value = parsedCardData;
    } catch (error) {
      console.error(error);
      clearCardData();
    }
  } else {
    clearCardData();
  }
};

//添加智能卡
const handleAddCard = async () => {
  if (!physicalCardNumber.value) {
    MessagePlugin.warning('请先读取有效的物理卡编号');
    return;
  }

  const isTableDuplicate = tableData.value.some(
    item => item.physicalCardNumber === physicalCardNumber.value
  );
  if (isTableDuplicate) {
    MessagePlugin.warning('该物理卡编号已在表格中存在，请勿重复添加');
    return;
  }

  try {
    const addRes = await saveSmartCard(physicalCardNumber.value);
    const responseStatus = addRes.status || addRes;
    if (responseStatus?.success === true && (responseStatus?.code === '200' || responseStatus?.code === 200)) {
      MessagePlugin.success('添加成功，表格已同步最新数据');
      await getTableData(physicalCardNumber.value);
      clearCardData();
    } else {
      const errorMsg = responseStatus?.message || '后端处理添加请求失败（未返回成功状态）';
      throw new Error(errorMsg);
    }
  } catch (error) {
    console.error(error);
    if (error.message.includes('不能重复')) {
      MessagePlugin.error('该物理卡已存在于系统中，请勿重复添加');
      await getTableData(physicalCardNumber.value);
    } else {
      MessagePlugin.error(`添加失败：${error.message || '请联系管理员排查接口问题'}`);
    }
    clearCardData();
  }
};

const onPageChange = (pageInfo) => {
  pagination.value.current = pageInfo.current;
  pagination.value.pageSize = pageInfo.pageSize;
};

const handleClickDelete = (row) => {
  if (!row?.id) {
    MessagePlugin.warning('无效的卡片数据，无法删除');
    return;
  }

  if (row.state === 1) {
    MessagePlugin.error('该卡片已发卡，无法删除');
    return;
  }

  deleteId.value = row.id;
  confirmVisible.value = true;
};

//删除智能卡
const doDelete = async () => {
  if (!deleteId.value) {
    MessagePlugin.warning('未选择要删除的智能卡');
    confirmVisible.value = false;
    return;
  }

  try {
    const deleteRes = await deleteSmartCard(deleteId.value);
    const responseStatus = deleteRes?.status || deleteRes;
    if (!responseStatus || !responseStatus?.success) {
      const errorMsg = responseStatus?.message || '删除请求处理失败（后端未返回成功状态）';
      throw new Error(errorMsg);
    }

    await getTableData();
    MessagePlugin.success('删除成功');
  } catch (error) {
    console.error(error);
    MessagePlugin.error('删除失败，请重试');
  } finally {
    confirmVisible.value = false;
    deleteId.value = '';
  }
};

onMounted(() => {
  getTableData();
});
</script>

<style lang="less" scoped>
.smart-card-page {
  width: 100%;
}

.top-container {
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;
  padding: 15px;
  background-color: #FFFFFF;
  border-radius: 4px;
}

.top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-bottom: 16px;
  border-bottom: 1px solid #E6E6E6;
}

.title-icon {
  width: 20px;
  height: 20px;
  margin-right: 8px;
  vertical-align: middle;
}

.title {
  font-size: 16px;
  font-weight: 500;
  font-family: PingFang SC;
  display: flex;
  align-items: center;
  color: #000;
}

.read-card-btn-wrapper {

  :deep(.t-button) {
    background-color: #0A1629;
    border-color: #0A1629;
    color: #fff;

    &:hover {
      background-color: #1a2a44;
      border-color: #1a2a44;
    }
  }
}

.top-operation {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding-top: 16px;
}

.label-text {
  color: #333;
  white-space: nowrap;
  font-size: 14px;
}

.input-card {
  width: 280px;
  flex-shrink: 0;
}

.add-btn {
  margin-left: auto;
  background-color: #F59E0B !important;
  border-color: #F59E0B !important;
  color: #fff !important;

  &:hover {
    background-color: #d97706 !important;
    border-color: #d97706 !important;
  }
}

.forNewClassTableAndsearch {
  padding: 15px;
  background-color: #FFFFFF;
  border-radius: 4px;
}

.table-container {
  border-radius: 4px;
  width: 100%;
}

// 分页样式优化
::v-deep .t-pagination {
  .t-pagination__total {
    margin-right: 20px;
    flex: none;
  }

  .t-pagination__jump {
    background-color: transparent;
    border: none;
  }
}

// 删除弹窗样式
/deep/ .deletCon {
  width: 100%;
  padding: 10px 0;
}

/deep/ .delectConHeader {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

/deep/ .b {
  margin-top: 8px;
  font-size: 12px;
  color: var(--td-text-color-secondary);
  line-height: 1.5;
}
</style>

<style lang="less">
.smart-card-page {
  .t-table__th-cell-inner {
    color: #000000 !important;
  }

  .t-table__td-cell-inner {
    color: #000000 !important;
  }

  .t-table .t-tag {
    height: 26px !important;
    border-radius: 4px !important;
  }
}
</style>