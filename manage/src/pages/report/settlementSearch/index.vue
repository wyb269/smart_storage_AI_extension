<template>
  <div class="container">
    <!-- 搜索表单 -->
    <SearchForm
      v-model="formObj"
      :fields="searchFields"
      :show-expand="false"
      @search="handleSearch"
      @reset="handleReset"
    />

    <!-- 表格区域 -->
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
      >
        <template #jobTypeTag="{ row }">
          <t-tag
            :style="{
              color: jobTypeColorMap[row.jobType]?.color,
              backgroundColor: jobTypeColorMap[row.jobType]?.bgColor,
              borderColor: jobTypeColorMap[row.jobType]?.color,
              height: '26px',
              borderRadius: '4px'
            }"
            variant="light-outline"
          >
            {{ row.jobType }}
          </t-tag>
        </template>

        <template #operation="{ row }">
          <t-button
            theme="primary"
            variant="text"
            @click="handleDetail(row.id, row.jobType)"
          >查看</t-button>
        </template>

        <template #empty="slotProps">
          <t-space>
            <div>
              <img
                style="width: 120px;"
                src="@/assets/imgs/empty.png"
              >
              <div style="font-family: PingFang SC;font-weight: 300;font-style: Regular;font-size: 13px;line-height: 100%;letter-spacing: 0px;color:rgba(149, 149, 149, 1);text-align: center;">暂无数据</div>
            </div>
          </t-space>
        </template>
      </t-table>
    </div>
  </div>
</template>

<script setup name="settlementSearch">
import { ref, onMounted } from "vue";
import { MessagePlugin } from "tdesign-vue-next";
import { FileExportIcon } from "tdesign-icons-vue-next";
import SearchForm from "@/components/SearchForm/index.vue";
import {
  getList,
  getGrainVariety,
  excelExport,
} from "@/api/settlementSearch.ts";
import router from "@/router";

// 搜索选项
const jobTypeOptions = ref([
  { label: "入库", value: 1 },
  { label: "出库", value: 2 },
]);
const grainVarietyOptions = ref([]);

// 搜索字段配置
const searchFields = ref([
  {
    name: "customerName",
    label: "客户名称",
    type: "input",
    placeholder: "请输入客户名称",
  },
  {
    name: "grainVariety",
    label: "粮食品种",
    type: "select",
    options: grainVarietyOptions.value,
  },
  {
    name: "settlementTimeRange",
    label: "结算时间",
    type: "date-range",
  },
  {
    name: "jobType",
    label: "作业类型",
    type: "select",
    options: jobTypeOptions.value,
  },
]);

// 表格列定义
const tableColumns = [
  { colKey: "serial-number", width: 60, title: "序号" },
  { colKey: "jobNo", title: "作业编号" },
  { colKey: "customerName", title: "客户名称" },
  { colKey: "grainVariety", title: "粮食品种" },
  { colKey: "grainLevel", title: "粮食等级" },
  { colKey: "grossWeight", title: "毛重(千克)" },
  { colKey: "tareWeight", title: "皮重(千克)" },
  { colKey: "netWeight", title: "净重(千克)" },
  { colKey: "jobType", title: "作业类型", cell: "jobTypeTag" },
  { colKey: "settleWeight", title: "结算重量(千克)" },
  { colKey: "settleUnitPrice", title: "结算单价(元)" },
  { colKey: "settleAmount", title: "结算金额(元)" },
  { colKey: "settleTime", title: "结算时间" },
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

// 响应式数据
const formObj = ref({
  settlementTimeRange: [], // t-date-range-picker 期望数组
});
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

// 获取粮食品种
const getGrainVarietyOptions = async () => {
  try {
    const response = await getGrainVariety();
    if (response.status.code === "200") {
      const data = response.result.listDictData || [];
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
    }
  } catch (error) {
    MessagePlugin.error("获取粮食品种失败");
  }
};

const formatParams = () => {
  const params = {
    pageNo: pagination.value.current,
    pageSize: pagination.value.pageSize,
  };

  // 处理表单参数
  if (formObj.value.customerName) {
    params.customerName = formObj.value.customerName;
  }
  if (formObj.value.grainVariety) {
    params.grainVariety = formObj.value.grainVariety;
  }
  if (formObj.value.jobType) {
    params.jobType = formObj.value.jobType;
  }

  // 处理日期范围 - 支持数组格式
  if (formObj.value.settlementTimeRange) {
    const dateRange = formObj.value.settlementTimeRange;
    if (Array.isArray(dateRange) && dateRange.length === 2) {
      params.settleTimeStart = `${dateRange[0]} 00:00:00`;
      params.settleTimeEnd = `${dateRange[1]} 23:59:59`;
    }
  }

  return params;
};

// 获取表格数据
const getTableData = async () => {
  loading.value = true;

  const params = formatParams();

  try {
    const response = await getList(params);
    if (response.status.code !== "200") {
      throw new Error(response.status.message || "获取列表失败");
    }
    tableData.value = response.result.data || [];
    pagination.value.total = response.result.total || 0;
  } catch (error) {
    MessagePlugin.error(error.message);
  } finally {
    loading.value = false;
  }
};

// 搜索和重置
const handleSearch = () => {
  pagination.value.current = 1;
  getTableData();
};

const handleReset = () => {
  // SearchForm会自动重置formObj，这里只需要重新加载数据
  getTableData();
};

// 分页处理
const onPageChange = (pageInfo) => {
  pagination.value.current = pageInfo.current;
  pagination.value.pageSize = pageInfo.pageSize;
  getTableData();
};

// 导出
const handleExport = async () => {
  const form = formatParams();
  const requestData = {
    bizType: "settlementReport",
    params: {
      ...form,
    },
  };

  delete requestData.pageNo;
  delete requestData.pageSize;

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
    link.download = "结算报表.xlsx";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);

    MessagePlugin.success("导出成功");
  } catch (error) {
    MessagePlugin.error((error && error.message) || "导出失败，请重试");
  }
};

// 查看详情
const handleDetail = (id, jobType) => {
  router.push({
    path: "/settlement/detail",
    query: {
      id: id,
      jobType: jobType === "入库" ? 1 : 2,
    },
  });
};

// 生命周期
onMounted(() => {
  getGrainVarietyOptions();
  getTableData();
});
</script>

<style lang="less" scoped>
.bottom {
  padding: 15px;
  background-color: #fff;
  border-radius: 4px;
  margin-bottom: 25px;

  .t-button--variant-text {
    padding-left: 0;
    padding-right: 0;
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
</style>