<template>
  <div class="container">
    <!-- 搜索表单区域 -->
    <div class="top forNewclass">
      <t-form :data="formObj" ref="formRef" :colon="false" :class="{
        'form-collapsed': isSearchCollapsed,
        'form-expanded': !isSearchCollapsed
      }">
        <t-row :gutter="[65, 12]">
          <t-col :span="3">
            <t-form-item label="仓库编码" name="id">
              <t-input v-model="formObj.id" placeholder="请输入仓库编码" />
            </t-form-item>
          </t-col>
          <t-col :span="3">
            <t-form-item label="仓库名称" name="warehouseName">
              <t-input v-model="formObj.warehouseName" placeholder="请输入仓库名称" />
            </t-form-item>
          </t-col>
          <t-col :span="3">
            <t-form-item label="仓库类型" name="warehouseType">
              <t-select v-model="formObj.warehouseType" :options="warehouseTypeOptions" placeholder="请选择仓库类型" />
            </t-form-item>
          </t-col>
          <t-col :span="3">
            <!-- 收起状态显示按钮 -->
            <div v-if="isSearchCollapsed" class="last-form-item">
              <t-button theme="primary" variant="base" @click="handleSearch" class="search-btn my-icon-btn">
                <search-icon slot="icon" />查询
              </t-button>
              <t-button theme="primary" variant="outline" @click="handleReset" class="reset-btn my-icon-btn">
                <refresh-icon slot="icon" />重置
              </t-button>
              <t-button theme="primary" variant="outline" @click="handleCollapse" class="collapse-btn my-icon-btn">
                <template #icon>
                  <chevron-down-icon />
                </template> {{ collapseText }}
              </t-button>
            </div>
            <!-- 展开状态显示粮食品种-->
            <t-form-item v-else label="粮食品种" name="grainVariety">
              <t-select v-model="formObj.grainVariety" :options="grainTypeOptions" placeholder="请选择粮食品种" />
            </t-form-item>
          </t-col>
        </t-row>
        <t-row v-show="!isSearchCollapsed" :gutter="[65, 12]">
          <t-col :span="3">
            <t-form-item label="启用状态" name="state">
              <t-select v-model="formObj.state" :options="statusOptions" placeholder="请选择启用状态" />
            </t-form-item>
          </t-col>
          <t-col :span="3">
            <t-form-item label="所属组织" name="org">
              <t-select v-model="formObj.org" :options="orgOptions" placeholder="请选择所属组织" @filter="loadOrgOptions" />
            </t-form-item>
          </t-col>

          <t-col :span="6" style="text-align: right;">
            <t-form-item class="last-form-item">
              <t-button theme="primary" variant="base" @click="handleSearch" class="search-btn my-icon-btn">
                <search-icon slot="icon" />查询
              </t-button>
              <t-button theme="primary" variant="outline" @click="handleReset" class="reset-btn my-icon-btn">
                <refresh-icon slot="icon" />重置
              </t-button>
              <t-button theme="primary" variant="outline" @click="handleCollapse" class="collapse-btn my-icon-btn">
                <template #icon>
                  <chevron-up-icon v-if="!isSearchCollapsed" />
                  <chevron-down-icon v-else />
                </template> {{ collapseText }}
              </t-button>
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>
    </div>

    <!-- 表格区域 -->
    <div class="bottom forNewClassTableAndsearch">
      <div class="btn-box" style="margin: 5px 0 25px 0;">
        <t-button theme="primary" variant="base" @click="handleAdd" class="search-btn my-icon-btn"
          style="width: 110px;">
          <add-icon slot="icon" />新增
        </t-button>
      </div>

      <t-table :loading="loading" :data="tableData" :columns="tableColumns" hover resizable row-key="id"
        :selected-row-keys="selectedRowKeys" @select-change="handleSelectChange" :pagination="pagination"
        @page-change="onPageChange">

        <!-- 状态开关插槽-->
        <template #statusSwitch="{ row }">
          <div>
            <t-switch :value="row.state === '启用'" size="medium" @change="(isOn) => handleStatusChange(row, isOn)"
              :loading="statusLoading === row.id">
              <template #label="{ value }">
                <span style="color: white;font-weight: bold; padding: 0 4px;">
                  {{ value ? '√' : '×' }}
                </span>
              </template>
            </t-switch>
          </div>
        </template>

        <template #operation="{ row }">
          <t-button theme="primary" variant="text" @click="handleDetail(row.id)">查看</t-button>
          <t-button theme="success" variant="text" @click="handleEdit(row.id)">编辑</t-button>
          <t-button theme="danger" variant="text" @click="handleDelete(row.id)">删除</t-button>
        </template>

        <template #empty="slotProps">
          <t-space>
            <div>
              <img style="width: 120px;" src="@/assets/imgs/empty.png">
              <div
                style="font-family: PingFang SC;font-weight: 300;font-style: Regular;font-size: 13px;line-height: 100%;letter-spacing: 0px;color:rgba(149, 149, 149, 1);text-align: center;">
                暂无数据</div>
            </div>
          </t-space>
        </template>
      </t-table>
    </div>

    <!-- 弹窗组件 -->
    <Create v-if="dialogVisible" v-model:visible="dialogVisible" :id="warehouseId" :type="type"
      @submit="handleSubmit" />


    <!-- 删除确认对话框 -->
    <t-dialog v-model:visible="confirmVisible" header="删除确认" class="newDialog newclassNewDialog"
      :cancelBtn="{ theme: 'default', variant: 'outline' }" :confirmBtn="{ theme: 'warning', variant: 'base' }"
      @confirm="onConfirmDelete">
      <div class="deletCon">
        <div class="delectConHeader">
          <t-icon name="error-circle-filled" size="18px" color="rgba(255, 41, 75, 1)" style="margin-right: 8px;" />
          <span class="a">{{ deleteMessage }}</span>
        </div>
        <div class="b">仓库一旦删除，相关数据将无法恢复，请谨慎操作。</div>
      </div>
    </t-dialog>
    <t-dialog v-model:visible="stopVisible" header="禁用确认" class="newDialog newclassNewDialog"
      :cancelBtn="{ theme: 'default', variant: 'outline' }" :confirmBtn="{ theme: 'primary', variant: 'base' }"
      @confirm="onConfirmStop">
      <div class="deletCon">
        <div class="delectConHeader">
          <t-icon name="error-circle-filled" size="18px" color="rgba(255, 41, 75, 1)" style="margin-right: 8px;" />
          <span class="a">{{ stopMessage }}</span>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup name="warehouse">
import { ref, onMounted, computed } from "vue";
import { MessagePlugin } from "tdesign-vue-next";
import {
  SearchIcon,
  RefreshIcon,
  ChevronUpIcon,
  ChevronDownIcon,
  AddIcon,
  CheckIcon,
  CloseIcon,
} from "tdesign-icons-vue-next";
import { getList, deleteApi } from "@/api/warehouse";
import Create from "./Components/Create.vue";
import { getDict } from "@/api/sampling.ts";
import { listOrg } from "@/api/org";
import { update } from "@/api/warehouse";

// 表格列定义
const tableColumns = [
  { colKey: "row-select", type: "single", width: 50, title: "选择" },
  { colKey: "serial-number", width: 60, title: "序号" },
  { colKey: "id", title: "仓库编码" },
  { colKey: "warehouseName", title: "仓库名称" },
  { colKey: "warehouseType", title: "仓库类型" },
  { colKey: "maxWeight", title: "仓库容量(KG)" },
  { colKey: "currentWeight", title: "当前容量(KG)" },
  { colKey: "grainVariety", title: "粮食品种" },
  { colKey: "keeper", title: "库管员" },
  { colKey: "state", title: "启用状态", cell: "statusSwitch" },
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
const warehouseId = ref("");
const confirmVisible = ref(false);
const deleteIds = ref([]);
const isSearchCollapsed = ref(false);
const collapseText = computed(() => { return isSearchCollapsed.value ? "展开" : "收起"; });
const hasEditPermission = ref(true);
const stopVisible = ref(false);
const stopId = ref("");
const stopMessage = computed(() => "确认要禁用这个仓库吗？");
const grainTypeOptions = ref([]);
const orgOptions = ref([]);
const statusLoading = ref("");


// 分页配置
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showPageSize: true,
  showPreviousAndNextBtn: true,
});


// 计算属性
const warehouseTypeOptions = ref([
  { label: "平房仓", value: 1 },
  { label: "立筒仓", value: 2 },
  { label: "浅圆仓", value: 3 },
  { label: "罩棚仓", value: 4 },
]);

const statusOptions = ref([
  { label: "启用", value: 1 },
  { label: "禁用", value: 2 },
]);

//粮食品种类型字典
const getgrainTypeOptions = () => {
  getDict("GRAIN_VARIETY")
    .then((res) => {
      const data = res.result.listDictData || [];
      grainTypeOptions.value = data.map((item) => ({
        label: item.label,
        value: item.description,
      }));
    })
    .catch((error) => {
      console.error("加载粮食品种字典失败：", error);
      MessagePlugin.warning("粮食品种字典加载失败，请刷新页面");
    });
};
//加载组织
const loadOrgOptions = async (searchName = "") => {
  try {
    const res = await listOrg({ pageNo: 1, pageSize: 10, name: searchName });
    orgOptions.value = (res?.result?.data || []).map(item => ({ label: item.name, value: item.id }));
  } catch (err) {
    MessagePlugin.error("所属组织加载失败");
    orgOptions.value = [];
  }
};

const deleteMessage = computed(() =>
  "确认删除该仓库？"
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
    //
    response.result.data.forEach((item) => {
      item.warehouseType = warehouseTypeOptions.value.find(v => v.value === item.warehouseType)?.label || '未知类型';
      item.state = statusOptions.value.find(v => v.value === item.state)?.label || '未知状态';
    });
    tableData.value = response.result.data || [];
    pagination.value.total = response.result.total || 0;
  } catch (error) {
    MessagePlugin.error(error.message);
  } finally {
    loading.value = false;
  }
};

// 搜索和重置和收起
const handleSearch = () => {
  pagination.value.current = 1;
  getTableData();
};

const handleReset = () => {
  formObj.value = {};
  getTableData();
};

const handleCollapse = () => {
  isSearchCollapsed.value = !isSearchCollapsed.value;
};
// 分页处理
const onPageChange = (pageInfo) => {
  pagination.value.current = pageInfo.current;
  pagination.value.pageSize = pageInfo.pageSize;
  getTableData();
};

// 表格操作
const handleAdd = () => {
  type.value = "新增";
  dialogVisible.value = true;
};

const handleDetail = (id) => {
  type.value = "查看";
  warehouseId.value = id;
  dialogVisible.value = true;
};

const handleEdit = (id) => {
  type.value = "编辑";
  warehouseId.value = id;
  dialogVisible.value = true;
};

// 删除操作
const handleDelete = (id) => {
  deleteIds.value = [id];
  confirmVisible.value = true;
};

//删除
const onConfirmDelete = async () => {
  try {
    const response = await deleteApi({ ids: deleteIds.value });
    if (response.status.code !== "200") {
      throw new Error(response.status.message || "删除失败");
    }

    MessagePlugin.success("删除成功");

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

//禁用
const onConfirmStop = async () => {
  statusLoading.value = stopId.value;
  try {
    const targetRow = tableData.value.find(item => item.id === stopId.value);
    if (!targetRow) {
      throw new Error("未找到该仓库数据");
    }

    const newStateNum = 2;
    const newStateStr = "禁用";
    const res = await update({
      id: stopId.value,
      state: newStateNum,
    });
    if (res.status?.code !== "200") {
      throw new Error(res.status.message || "禁用失败");
    }
    targetRow.state = newStateStr;
    MessagePlugin.success("禁用成功");

    // await getTableData();
  } catch (error) {
    MessagePlugin.error(error.message);
  } finally {
    statusLoading.value = "";
    stopId.value = "";
    stopVisible.value = false;
  }
};

const handleStatusChange = async (row, isOn) => {
  if (isOn) {
    const newStateNum = 1;
    const newStateStr = "启用";
    statusLoading.value = row.id;

    try {
      const res = await update({
        id: row.id,
        state: newStateNum,
      });
      if (res.status?.code === "200") {
        row.state = newStateStr;
        MessagePlugin.success("启用成功");
      } else {
        MessagePlugin.error(res.status?.message || "操作失败");
      }
    } catch (err) {
      MessagePlugin.error("状态修改失败");
      console.error(err);
    } finally {
      statusLoading.value = "";
    }
    return;
  }
  stopId.value = row.id;
  stopVisible.value = true;
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
  getgrainTypeOptions();
  loadOrgOptions();
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

.top {
  padding: 20px 15px;
  margin-bottom: 25px;

  ::v-deep .t-form {
    display: grid;
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