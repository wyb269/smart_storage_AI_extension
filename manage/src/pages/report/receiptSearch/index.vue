<template>
  <div class="container">
    <!-- 搜索表单 -->
    <SearchForm
      v-model="formObj"
      :fields="searchFields"
      @search="handleSearch"
      @reset="handleReset"
    />

    <!-- 表格区域（保持不变） -->
    <div class="bottom forNewClassTableAndsearch">
      <div
        class="btn-box"
        style="margin: 5px 0 25px 0"
      >
        <t-button
          theme="primary"
          variant="base"
          @click="handleExport"
          class="search-btn my-icon-btn"
          style="width: 110px"
        >
          <file-export-icon slot="icon" />导出表格
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
        class="search-table-container"
      >
        <template #jobTypeTag="{ row }">
          <t-tag
            :style="{
              color: jobTypeColorMap[row.jobType]?.color,
              backgroundColor: jobTypeColorMap[row.jobType]?.bgColor,
              borderColor: jobTypeColorMap[row.jobType]?.color,
            }"
            variant="light-outline"
          >
            {{ row.jobType }}
          </t-tag>
        </template>
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
            @click="handleDetail(row.id)"
          >查看</t-button>
        </template>
        <template #empty>
          <t-space>
            <div>
              <img
                style="width: 120px"
                src="@/assets/imgs/empty.png"
                alt="empty"
              />
              <div style="font-family: PingFang SC;font-weight: 300;font-size: 13px;color:rgba(149, 149, 149, 1);text-align: center;">暂无数据</div>
            </div>
          </t-space>
        </template>
      </t-table>
    </div>
  </div>
</template>

<script setup name="receiptSearch">
import { ref, onMounted } from "vue";
import { MessagePlugin } from "tdesign-vue-next";
import { FileExportIcon } from "tdesign-icons-vue-next";
import SearchForm from "@/components/SearchForm/index.vue";
import { granaryList, getList } from "@/api/receiptSearch.ts";
import { excelExport } from "@/api/settlementSearch.ts";
import { getDict } from "@/api/sampling.ts";
import router from "@/router";

// 选项数据（保持不变）
const jobTypeOptions = ref([
  { label: "入库", value: 1 },
  { label: "出库", value: 2 },
]);
const sszzOptions = ref([]);
const granaryOptions = ref([]);
const jobStatusOptions = ref([
  { value: "logIn", label: "待扦样" },
  { value: "sampled", label: "待质检" },
  { value: "inspected", label: "待称重" },
  { value: "weighed", label: "待值仓" },
  { value: "warehoused", label: "待称皮" },
  { value: "tareWeighed", label: "待结算" },
  { value: "pendCard", label: "待销卡" },
  { value: "logout", label: "已结束" },
]);
const grainVarietyOptions = ref([]);

// 搜索字段配置
const searchFields = ref([
  // 基础字段
  {
    name: "jobNo",
    label: "作业编号",
    type: "input",
    placeholder: "请输入作业编号",
  },
  {
    name: "jobType",
    label: "作业类型",
    type: "select",
    options: jobTypeOptions.value,
  },
  {
    name: "customerName",
    label: "客户名称",
    type: "input",
    placeholder: "请输入客户名称",
  },
  {
    name: "carrier",
    label: "承运人",
    type: "input",
    placeholder: "请输入承运人",
  },
  // 可展开字段
  {
    name: "granary",
    label: "粮仓名称",
    type: "select",
    options: granaryOptions.value,
    expandable: true,
  },
  {
    name: "jobStatus",
    label: "流程状态",
    type: "select",
    options: jobStatusOptions.value,
    expandable: true,
  },
  {
    name: "grainVariety",
    label: "粮食品种",
    type: "select",
    options: grainVarietyOptions.value,
    expandable: true,
  },
]);

// 表格列定义
const tableColumns = [
  { colKey: "serial-number", width: 60, title: "序号" },
  { colKey: "jobNo", title: "作业编号" },
  { colKey: "jobType", title: "作业类型", cell: "jobTypeTag" },
  { colKey: "customerName", title: "客户名称" },
  { colKey: "carrier", title: "承运人" },
  { colKey: "carNo", title: "车牌号" },
  { colKey: "grainVariety", title: "粮食品种" },
  { colKey: "warehouseName", title: "仓库名称" },
  { colKey: "jobStatus", title: "流程状态", cell: "processStatusTag" },
  { colKey: "grossWeight", title: "毛重(千克)" },
  { colKey: "tareWeight", title: "皮重(千克)" },
  { colKey: "netWeight", title: "净重(千克)" },
  { colKey: "zkz", title: "总扣重(千克)" },
  { colKey: "settleWeight", title: "结算重量(千克)" },
  { colKey: "endTime", title: "结束时间" },
  {
    colKey: "operation",
    title: "操作",
    width: 60,
    cell: "operation",
    align: "center",
  },
];

// 作业类型颜色配置
const jobTypeColorMap = {
  入库: { color: "#0052D9", bgColor: "#F2F3FF" },
  出库: { color: "#2BA471", bgColor: "#E3F9E9" },
};

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

// 响应式数据
const formObj = ref({});
const loading = ref(false);
const tableData = ref([]);

// 分页配置
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showPageSize: true,
  showPreviousAndNextBtn: true,
});

// 数据获取
const getTableData = async () => {
  loading.value = true;
  const params = {
    pageNo: pagination.value.current,
    pageSize: pagination.value.pageSize,
    ...formObj.value,
  };

  try {
    const response = await getList(params);
    if (response.status.code !== "200") {
      throw new Error(response.status.message || "获取列表失败");
    }

    response.result.data.forEach((item) => {
      if (item.jobStatus) {
        if (item.jobStatus === "settled") {
          item.jobStatus = "pendCard";
        }
        if (item.jobStatus === "待出门销卡") {
          item.jobStatus = "待销卡";
        }
        item.jobStatusEN = item.jobStatus;
        item.jobStatus = statusMap[item.jobStatus] || item.jobStatus;
      } else {
        item.jobStatus = "未知";
      }

      item.grainVariety =
        grainVarietyOptions.value.find((dic) => dic.value === item.grainVariety)
          ?.label || item.grainVariety;
    });

    tableData.value = response.result.data || [];
    pagination.value.total = response.result.total || 0;
  } catch (error) {
    MessagePlugin.error(error.message);
  } finally {
    loading.value = false;
  }
};

// 导出
const handleExport = async () => {
  const form = JSON.parse(JSON.stringify(formObj.value));
  const requestData = {
    bizType: "registrationReport",
    params: {
      ...form,
    },
  };

  try {
    const res = await excelExport(requestData);

    if (!res || !res.data) {
      throw new Error("导出失败，请重试");
    }

    const blob = new Blob([res.data], {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "出入库报表.xlsx";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);

    MessagePlugin.success("导出成功");
  } catch (error) {
    MessagePlugin.error((error && error.message) || "导出失败，请重试");
  }
};

const handleDetail = (id) => {
  router.push({
    path: "/taskDetail",
    query: { taskId: id },
  });
};

// 搜索和重置
const handleSearch = () => {
  pagination.value.current = 1;
  getTableData();
};

const handleReset = () => {
  formObj.value = {};
  getTableData();
};

// 分页处理
const onPageChange = (pageInfo) => {
  pagination.value.current = pageInfo.current;
  pagination.value.pageSize = pageInfo.pageSize;
  getTableData();
};

// 粮仓名称
const getGranaryOptions = async () => {
  let response = await granaryList({});
  if (response.status.code === "200") {
    response.result.data.forEach((item) => {
      granaryOptions.value.push({
        value: item.id,
        label: item.warehouseName,
      });
    });

    // 更新搜索字段配置中的粮仓名称选项
    const granaryField = searchFields.value.find((f) => f.name === "granary");
    if (granaryField) {
      granaryField.options = granaryOptions.value;
    }
  }
};

//  粮食品种
const getGrainVarietyOptions = () => {
  getDict("GRAIN_VARIETY")
    .then((res) => {
      const data = res.result.listDictData || [];
      grainVarietyOptions.value = data.map((item) => ({
        label: item.label,
        value: item.description,
      }));

      // 更新搜索字段配置中的粮食品种选项
      const grainVarietyField = searchFields.value.find(
        (f) => f.name === "grainVariety"
      );
      if (grainVarietyField) {
        grainVarietyField.options = grainVarietyOptions.value;
      }
    })
    .catch((error) => {});
};

// 生命周期
onMounted(() => {
  getGranaryOptions();
  getGrainVarietyOptions();
  getTableData();
});
</script>

<style lang="less" scoped>
.top,
.bottom {
  padding: 15px;
  background-color: #fff;
  border-radius: 4px;
}

.bottom {
  .t-button--variant-text {
    padding-left: 0;
    padding-right: 0;
  }

  :deep(.t-pagination) {
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

  .search-table-container {
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

<style lang="less">
// 全局按钮样式（保持不变）
.my-icon-btn .t-button__text {
  display: flex;
  justify-content: center;
  align-items: center;
  .t-icon {
    margin-right: 6px;
  }
}

.search-btn.t-button--theme-primary.t-button--variant-base {
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

.reset-btn.t-button--theme-primary.t-button--variant-outline,
.expand-btn.t-button--theme-primary.t-button--variant-outline,
.collapse-btn.t-button--theme-primary.t-button--variant-outline {
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