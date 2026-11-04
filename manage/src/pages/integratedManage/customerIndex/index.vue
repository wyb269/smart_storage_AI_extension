<template>
  <div class="container">
    <!-- 搜索表单区域 -->
    <div class="top forNewclass">
      <t-form
        :data="formObj"
        ref="formRef"
      >
        <t-row style="width: 100%;">
          <t-col :span="3">
            <t-form-item
              label="客户名称"
              name="name"
            >
              <t-input
                v-model="formObj.name"
                placeholder="请输入客户名称"
              />
            </t-form-item>
          </t-col>
          <t-col :span="3">
            <t-form-item
              label="身份证号"
              name="card"
            >
              <t-input
                v-model="formObj.card"
                placeholder="请输入身份证号"
              />
            </t-form-item>
          </t-col>
          <t-col :span="3">
            <t-form-item
              label="客户类型"
              name="customerType"
            >
              <t-select
                v-model="formObj.customerType"
                :options="customerTypeOptions"
                placeholder="请选择客户类型"
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
                <search-icon slot="icon" />查询
              </t-button>
              <t-button
                theme="primary"
                variant="outline"
                @click="handleReset"
                class="reset-btn my-icon-btn"
              >
                <refresh-icon slot="icon" />重置
              </t-button>
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>
    </div>

    <!-- 表格区域 -->
    <div class="bottom forNewClassTableAndsearch">
      <div
        class="btn-box"
        style="margin: 5px 0 25px 0;"
      >
        <t-button
          theme="primary"
          variant="base"
          @click="handleAdd"
          class="search-btn my-icon-btn"
          style="width: 110px;"
        >
          <add-icon slot="icon" />新增
        </t-button>
        <t-button
          :disabled="!selectedRowKeys.length"
          theme="danger"
          variant="base"
          @click="handleBatchDelete"
          class="my-icon-btn"
          style="width: 110px;"
        >
          <delete-icon slot="icon" />批量删除
        </t-button>
      </div>

      <t-table
        :loading="loading"
        :data="tableData"
        :columns="tableColumns"
        hover
        resizable
        row-key="id"
        :selected-row-keys="selectedRowKeys"
        @select-change="handleSelectChange"
        :pagination="pagination"
        @page-change="onPageChange"
      >
        <template #customerTypeTag="{ row }">
          <t-tag
            :style="{
              color: statusColorMap[row.customerType].color,
              backgroundColor: statusColorMap[row.customerType].bgColor,
              borderColor: statusColorMap[row.customerType].color,
              height: '26px',
              borderRadius: '4px'
            }"
            variant="light-outline"
          >
            {{ row.customerType }}
          </t-tag>
        </template>

        <template #operation="{ row }">
          <t-button
            theme="primary"
            variant="text"
            @click="handleDetail(row.id)"
          >查看</t-button>
          <t-button
            theme="success"
            variant="text"
            @click="handleEdit(row.id)"
          >编辑</t-button>
          <t-button
            theme="danger"
            variant="text"
            @click="handleDelete(row.id)"
          >删除</t-button>
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

    <!-- 弹窗组件 -->
    <Create
      v-if="dialogVisible"
      v-model:visible="dialogVisible"
      :id="customerId"
      :type="type"
      @submit="handleSubmit"
    />

    <!-- 删除确认对话框 -->
    <t-dialog
      v-model:visible="confirmVisible"
      header="删除确认"
      class="newDialog newclassNewDialog"
      :cancelBtn="{ theme: 'default', variant: 'outline' }"
      :confirmBtn="{ theme: 'warning', variant: 'base' }"
      @confirm="onConfirmDelete"
    >
      <div class="deletCon">
        <div class="delectConHeader">
          <t-icon
            name="error-circle-filled"
            size="18px"
            color="rgba(255, 41, 75, 1)"
            style="margin-right: 8px;"
          />
          <span class="a">{{ deleteMessage }}</span>
        </div>
        <div class="b">客户一旦删除，相关数据将无法恢复，请谨慎操作。</div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup name="customer">
import { ref, onMounted, computed } from "vue";
import { MessagePlugin } from "tdesign-vue-next";
import {
  SearchIcon,
  RefreshIcon,
  AddIcon,
  DeleteIcon,
} from "tdesign-icons-vue-next";
import { getList, deleteApi } from "@/api/customer.ts";
import Create from "./components/Create.vue";

// 表格列定义
const tableColumns = [
  { colKey: "row-select", type: "multiple", width: 50 },
  { colKey: "serial-number", width: 60, title: "序号" },
  { colKey: "name", title: "客户名称" },
  { colKey: "customerType", title: "客户类型", cell: "customerTypeTag" },
  {
    colKey: "address",
    title: "联系人居住地址",
    ellipsis: {
      theme: "light",
      placement: "top",
    },
  },
  { colKey: "card", title: "身份证" },
  { colKey: "phone", title: "联系电话" },
  {
    colKey: "operation",
    title: "操作",
    width: 200,
    cell: "operation",
    align: "center",
  },
];

// 响应式数据
const formObj = ref({});
const loading = ref(false);
const tableData = ref([]);
const selectedRowKeys = ref([]);
const dialogVisible = ref(false);
const type = ref("");
const customerId = ref("");
const confirmVisible = ref(false);
const deleteIds = ref([]);

// 分页配置
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showPageSize: true,
  showPreviousAndNextBtn: true,
});

const statusColorMap = {
  个人: { color: "#0052D9", bgColor: "#F2F3FF" },
  企业: { color: "#2BA471", bgColor: "#E3F9E9" },
};

// 计算属性
const customerTypeOptions = ref([
  { label: "个人", value: 1 },
  { label: "企业", value: 2 },
]);

const deleteMessage = computed(() =>
  deleteIds.value.length > 1
    ? `确认删除选中的 ${deleteIds.value.length} 个客户？`
    : "确认删除该客户？"
);

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

    // 处理客户类型显示
    response.result.data.forEach((item) => {
      item.customerType =
        customerTypeOptions.value.find((v) => v.value === item.customerType)
          ?.label || "";
    });

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
  formObj.value = {};
  getTableData();
};

// 分页处理
const onPageChange = (pageInfo) => {
  pagination.value.current = pageInfo.current;
  pagination.value.pageSize = pageInfo.pageSize;
};

// 表格操作
const handleAdd = () => {
  type.value = "新增";
  dialogVisible.value = true;
};

const handleDetail = (id) => {
  type.value = "查看";
  customerId.value = id;
  dialogVisible.value = true;
};

const handleEdit = (id) => {
  type.value = "编辑";
  customerId.value = id;
  dialogVisible.value = true;
};

// 删除操作
const handleDelete = (id) => {
  deleteIds.value = [id];
  confirmVisible.value = true;
};

const handleBatchDelete = () => {
  if (!selectedRowKeys.value.length) {
    MessagePlugin.warning("请至少选择一条数据进行删除");
    return;
  }
  deleteIds.value = [...selectedRowKeys.value];
  confirmVisible.value = true;
};

const onConfirmDelete = async () => {
  try {
    const response = await deleteApi({ ids: deleteIds.value });
    if (response.status.code !== "200") {
      throw new Error(response.status.message || "删除失败");
    }

    MessagePlugin.success(
      deleteIds.value.length > 1 ? "批量删除成功" : "删除成功"
    );

    // 如果是批量删除，清空选中项
    if (deleteIds.value.length > 1) {
      selectedRowKeys.value = [];
    }

    // 刷新表格数据
    await getTableData();

    // 如果删除后当前页没有数据且不是第一页，则跳转到上一页
    if (!tableData.value.length && pagination.value.current > 1) {
      pagination.value.current -= 1;
      await getTableData();
    }
  } catch (error) {
    MessagePlugin.error(error.message);
  } finally {
    deleteIds.value = [];
    confirmVisible.value = false;
  }
};

// 表单提交处理
const handleSubmit = () => {
  getTableData();
  selectedRowKeys.value = [];
  pagination.value.current = 1;
};

// 选中行变化
const handleSelectChange = (value) => {
  selectedRowKeys.value = value;
};

// 生命周期
onMounted(() => {
  getTableData();
});
</script><style lang="less" scoped>
.top,
.bottom {
  padding: 15px;
  background-color: #fff;
  border-radius: 4px;
}

.top {
  padding: 15px;
  margin-bottom: 25px;

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