<template>
  <div class="container">
    <div class="top">
      <div class="title">
        <img
          src="@/assets/receipt/icon9.png"
          alt=""
        >
        作业情况
      </div>

      <t-divider style="margin: 15px 0;" />

      <div class="status-list">
        <div
          class="status-item"
          v-for="(item, index) in statusList"
          @click="statusClick(item, index)"
          @mouseenter="index !== 0 && (hoverIndex = index)"
          @mouseleave="hoverIndex = null"
          :class="{ 'is-hover': hoverIndex === index, 'is-selected': selectedIndex === index, 'is-disabled': index === 0 }"
        >
          <t-badge :count="item.count">
            <div class="status-icon">
              <img
                :src="(hoverIndex === index || selectedIndex === index) && index !== 0 ? item.iconStatic : item.icon"
                alt=""
              >
            </div>
          </t-badge>
          <div class="status-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <div class="middle forNewclass">
      <t-form
        :data="formObj"
        ref="formRef"
      >
        <t-row style="width: 100%;">
          <t-col :span="2">
            <t-form-item
              label="客户名称"
              name="customerName"
            >
              <t-input
                v-model="formObj.customerName"
                placeholder="请输入客户名称"
              ></t-input>
            </t-form-item>
          </t-col>
          <t-col :span="2">
            <t-form-item
              label="作业编号"
              name="jobNo"
            >
              <t-input
                v-model="formObj.jobNo"
                placeholder="请输入作业编号"
              ></t-input>
            </t-form-item>
          </t-col>
          <t-col :span="2">
            <t-form-item
              label="承运人"
              name="carrierName"
            >
              <t-input
                v-model="formObj.carrierName"
                placeholder="请输入承运人"
              ></t-input>
            </t-form-item>
          </t-col>
          <t-col :span="3">
            <t-form-item
              label="作业日期"
              name="workTime"
            >
              <t-date-range-picker
                v-model="formObj.workTime"
                mode="date"
                clearable
                allow-input
                @change="workTimeChange"
              />
            </t-form-item>
          </t-col>
          <t-col :span="3">
            <t-form-item class="last-form-item">
              <t-button
                theme="primary"
                variant="base"
                @click="handleSearch"
                class="search-btn my-icon-btn"
              >
                <search-icon slot="icon" />
                查询
              </t-button>
              <t-button
                theme="primary"
                variant="outline"
                @click="handleReset"
                class="reset-btn my-icon-btn"
              >
                <refresh-icon slot="icon" />
                重置
              </t-button>
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>
    </div>

    <div class="bottom">
      <div class="forNewClassTableAndsearch">
        <t-table
          :loading="loading"
          :data="tableData"
          :columns="tableColumns"
          hover
          resizable
          row-key="id"
          :pagination="pagination"
          @page-change="onPageChange"
          class="inbound-table-container"
        >
          <template #processStatusTag="{ row }">
            <t-tag
              :style="{
                color: statusColorMap[row.jobStatus]?.color,
                backgroundColor: statusColorMap[row.jobStatus]?.bgColor,
                borderColor: statusColorMap[row.jobStatus]?.color,
              }"
              variant="light-outline"
            >
              {{ row.jobStatus }}
            </t-tag>
          </template>
          <template #operation="{ row }">
            <t-button
              theme="primary"
              variant="text"
              @click="handleDetail(row)"
              style="padding: 0;"
            >
              查看
            </t-button>
            <t-button
              theme="primary"
              variant="text"
              @click="handleOthers('回退', row)"
              style="padding: 0;"
              :disabled="row.jobStatus === '已结束' || row.jobStatus === '待销卡'"
            >
              流程回退
            </t-button>
            <t-button
              theme="primary"
              variant="text"
              @click="handleOthers('终止', row)"
              style="padding: 0;"
              :disabled="row.jobStatus === '已结束' || row.jobStatus === '待销卡'"
            >
              流程终止
            </t-button>
          </template>
          <template #empty="slotProps">
            <t-space>
              <div>
                <img
                  style="width: 120px;"
                  src="@/assets/imgs/empty.png"
                >
                <div style="font-family: PingFang SC;font-weight: 300;font-style: Regular;font-size: 13px;line-height: 100%;letter-spacing: 0px;color:rgba(149, 149, 149, 1);text-align: center;">
                  暂无数据</div>
              </div>
            </t-space>
          </template>
        </t-table>
      </div>
    </div>

    <!-- 确认对话框 -->
    <t-dialog
      v-model:visible="confirmVisible"
      :header="`${ confirmType }确认`"
      class="newDialog newclassNewDialog"
      :cancelBtn="{ theme: 'default', variant: 'outline' }"
      :confirmBtn="{ theme: 'warning', variant: 'base' }"
      @confirm="onConfirm"
    >
      <div class="deletCon">
        <div class="delectConHeader">
          <t-icon
            name="error-circle-filled"
            size="18px"
            color="rgba(255, 41, 75, 1)"
            style="margin-right: 8px;"
          />
          <span class="a">确认{{ confirmType }}该流程？</span>
        </div>
        <div class="b">流程一旦{{ confirmType }}，将无法恢复，请谨慎操作。</div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup name="quality">
import { computed, onMounted, ref } from "vue";
import { MessagePlugin } from "tdesign-vue-next";
import { SearchIcon, RefreshIcon } from "tdesign-icons-vue-next";
import {
  getList,
  taskStatistics,
  taskRetrace,
  taskTerminated,
} from "@/api/receiptInbound.ts";
import { useRouter } from "vue-router";
const router = useRouter();

const statusList = ref([
  {
    label: "入库登记",
    value: "register_pending",
    icon: new URL("@/assets/receipt/icon1.png", import.meta.url).href,
    count: 0,
  },
  {
    label: "待扦样",
    value: "logIn",
    iconStatic: new URL("@/assets/receipt/icon2.png", import.meta.url).href,
    icon: new URL("@/assets/receipt/icon2-static.png", import.meta.url).href,
    count: 0,
  },
  {
    label: "待质检",
    value: "sampled",
    iconStatic: new URL("@/assets/receipt/icon3.png", import.meta.url).href,
    icon: new URL("@/assets/receipt/icon3-static.png", import.meta.url).href,
    count: 0,
  },
  {
    label: "待称重",
    value: "inspected",
    iconStatic: new URL("@/assets/receipt/icon4.png", import.meta.url).href,
    icon: new URL("@/assets/receipt/icon4-static.png", import.meta.url).href,
    count: 0,
  },
  {
    label: "待值仓",
    value: "weighed",
    iconStatic: new URL("@/assets/receipt/icon5.png", import.meta.url).href,
    icon: new URL("@/assets/receipt/icon5-static.png", import.meta.url).href,
    count: 0,
  },
  {
    label: "待称皮",
    value: "warehoused",
    iconStatic: new URL("@/assets/receipt/icon6.png", import.meta.url).href,
    icon: new URL("@/assets/receipt/icon6-static.png", import.meta.url).href,
    count: 0,
  },
  {
    label: "待结算",
    value: "tareWeighed",
    iconStatic: new URL("@/assets/receipt/icon7.png", import.meta.url).href,
    icon: new URL("@/assets/receipt/icon7-static.png", import.meta.url).href,
    count: 0,
  },
  {
    label: "出门退卡",
    value: "pendCard",
    iconStatic: new URL("@/assets/receipt/icon8.png", import.meta.url).href,
    icon: new URL("@/assets/receipt/icon8-static.png", import.meta.url).href,
    count: 0,
  },
]);

const formObj = ref({
  workTime: [],
});

// login已登入、sampled已仟样、inspected已质检、weighed已称重、warehoused已值仓、tareWeighed已称皮、settled已结算、logout已登出、pendCard待销卡
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
};

const statusMap = {
  logIn: "待扦样",
  sampled: "待质检",
  inspected: "待称重",
  weighed: "待值仓",
  warehoused: "待称皮",
  tareWeighed: "待结算",
  pendCard: "待销卡",
  logout: "已结束",
};

// 表格列定义
const loading = ref(false);
const tableColumns = [
  {
    colKey: "serial-number",
    width: 60,
    title: "序号",
    fixed: "left",
  },
  {
    colKey: "jobNo",
    title: "作业编号",
    fixed: "left",
    width: 140,
  },
  { colKey: "jobType", title: "作业类型" },
  { colKey: "customerName", title: "客户名称" },
  { colKey: "carrierName", title: "承运人" },
  { colKey: "licensePlate", title: "车牌号" },
  { colKey: "jobStartTime", title: "登记时间", width: 140 },
  { colKey: "jobEndTime", title: "登出时间", width: 140 },
  { colKey: "warehouse", title: "仓库名称" },
  {
    colKey: "jobStatus",
    title: "流程状态",
    cell: "processStatusTag",
  },
  { colKey: "grossWeight", title: "毛重(千克)" },
  { colKey: "tareWeigh", title: "皮重(千克)" },
  { colKey: "netWeigh", title: "净重(千克)" },
  {
    colKey: "operation",
    title: "操作",
    width: 170,
    cell: "operation",
    align: "center",
    fixed: "right",
  },
];

// 表格数据
const tableData = ref([]);

// 分页配置
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showPageSize: true,
  // showJumper: true,
  showPreviousAndNextBtn: true,
});

const hoverIndex = ref(null);
const selectedIndex = ref(null);

const taskInfo = ref({});

const confirmVisible = ref(false);
const confirmType = ref("");

const statusClick = (item, index) => {
  // 第一个节点不能被点击
  if (index === 0) return;

  selectedIndex.value = index;
  if (item) {
    formObj.value.jobStatus = item.value;
    getTableData();
  }
};

const workTimeChange = async (val) => {
  if (val && val.length !== 0) {
    formObj.value.startTime = val[0];
    formObj.value.endTime = val[1];
  } else {
    formObj.value.startTime = "";
    formObj.value.endTime = "";
  }
};

const getTableData = async () => {
  loading.value = true;
  let data = JSON.parse(JSON.stringify(pagination.value));
  let params = JSON.parse(JSON.stringify(formObj.value));
  let form = {
    pageNo: data.current,
    pageSize: data.pageSize,
    customerName: params.customerName || "",
    jobNo: params.jobNo || "",
    carrierName: params.carrierName || "",
    startTime: params.startTime || "",
    endTime: params.endTime || "",
  };

  if (params.jobStatus) {
    form.jobStatus = params.jobStatus;
  }

  try {
    const response = await getList(form);

    if (response.status.code != "200") {
      MessagePlugin.error(response.status.message || "获取列表失败");
      return;
    }

    response.result.data.forEach((item) => {
      if (item.jobType == 1) {
        item.jobType = "入库";
      } else if (item.jobType == 2) {
        item.jobType = "出库";
      } else {
        item.jobType = "未知";
      }

      if (item.jobStatus) {
        if (item.jobStatus === "settled") {
          item.jobStatus = "pendCard";
        }
        item.jobStatusEN = item.jobStatus;
        item.jobStatus = statusMap[item.jobStatus] || item.jobStatus;
      } else {
        item.jobStatus = "未知";
      }
    });
    tableData.value = response.result.data || [];
    pagination.value.total = response.result.total || 0;
  } catch (error) {
    MessagePlugin.error("获取列表失败");
  } finally {
    loading.value = false;
  }
};

const getTaskStatistics = async () => {
  const response = await taskStatistics();

  if (response.status.code != "200") {
    MessagePlugin.error(response.status.message || "获取统计数据失败");
    return;
  }

  const stats = response.result || {};
  // if (stats.pendCard != 0) {
  //   stats.settled = stats.pendCard;
  // }
  statusList.value.forEach((item) => {
    item.count = stats[item.value] || 0;
  });
};

const handleSearch = () => {
  pagination.value.current = 1;
  getTableData();
};

const handleReset = () => {
  formObj.value = {
    workTime: [],
  };
  hoverIndex.value = null;
  selectedIndex.value = null;
  getTableData();
};

const handleDetail = (row) => {
  if (!row.id) {
    MessagePlugin.error("缺少作业ID参数");
    return;
  }
  router.push({
    path: "/taskDetail",
    query: { taskId: row.id },
  });
};

const handleOthers = async (type, row) => {
  if (!row.id) {
    MessagePlugin.error("缺少作业ID参数");
    return;
  }

  if (!row.jobStatusEN) {
    MessagePlugin.error("作业流程错误");
    return;
  }

  taskInfo.value = row;
  confirmType.value = type;
  confirmVisible.value = true;
};

const onConfirm = async () => {
  let response = null;

  if (confirmType.value === "回退") {
    response = await taskRetrace({
      id: taskInfo.value.id,
      jobStatus: taskInfo.value.jobStatusEN,
      jobType: 1,
    });
  } else {
    response = await taskTerminated(taskInfo.value.id);
  }

  if (response.status.code != "200") {
    MessagePlugin.error(response.status.message || `${confirmType.value}失败`);
  } else {
    MessagePlugin.success(
      response.status.message || `${confirmType.value}成功`
    );
    getTaskStatistics();
    getTableData();
  }

  confirmVisible.value = false;
};

const onPageChange = (pageInfo) => {
  pagination.value.current = pageInfo.current;
  pagination.value.pageSize = pageInfo.pageSize;
};

onMounted(() => {
  getTaskStatistics();
  getTableData();
});
</script>


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


<style lang="less" scoped>
.container {
  width: 100%;
}

.top,
.middle,
.bottom {
  padding: 15px;
  background-color: #fff;
  border-radius: 4px;
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

.title {
  font-size: 16px;
  font-weight: 500;
  font-family: PingFang SC;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #000;
  padding-top: 4px;

  img {
    height: 24px;
    object-fit: contain;
  }
}

.top {
  width: 100%;

  .status-list {
    display: flex;
    justify-content: space-between;
    margin-top: 25px;

    .status-item {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      position: relative;
      z-index: 1;
      width: 100%;
      cursor: pointer;

      .status-icon {
        width: 48px;
        height: 48px;
        background-color: #fff;
        border: 2px solid #f59e0b;
        border-radius: 50%;
        display: flex;
        justify-content: center;
        align-items: center;
        margin-bottom: 10px;
        transition: background-color 0.3s ease;

        img {
          height: 24px;
          object-fit: contain;
        }
      }

      .status-label {
        font-size: 14px;
        color: #333;
      }
    }

    .status-item.is-disabled {
      cursor: not-allowed;
    }

    .status-item:first-child .status-icon {
      background-color: #f8fafc;
      border: 2px solid #e3e7ef;
    }

    .status-item.is-hover .status-icon {
      background-color: #f59e0b;
    }

    .status-item.is-selected .status-icon {
      background-color: #f59e0b;
    }

    /* 生成节点间的横线（最后一个节点不加） */
    .status-item:not(:last-child)::after {
      content: "";
      position: absolute;
      top: 23px;
      /* 调整到图标中心高度 */
      left: 50%;
      width: 100%;
      height: 2px;
      background-color: #f59e0b;
      z-index: -1;
    }
  }
}

.middle {
  margin: 25px 0;
  padding: 20px 15px;

  ::v-deep .t-form {
    display: flex;
    align-items: center;
    gap: 16px;

    .t-row {
      align-items: center;
    }

    .t-form__item {
      flex: 1;
      margin-bottom: 0;
    }

    .last-form-item {
      display: flex;
      justify-content: flex-end;
      align-items: center;
    }
  }
}

.bottom {
  .inbound-table-container {
    .t-tag {
      height: 26px;
      border-radius: 4px;
    }

    ::v-deep .t-table {
      width: 100%;
    }
  }
}
</style>