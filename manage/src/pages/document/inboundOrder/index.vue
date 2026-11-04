<template>
  <div class="container">
    <!-- 搜索表单区域 -->
    <div class="top forNewclass">
      <t-form :data="formObj" ref="formRef">
        <t-row style="width: 100%;">
          <t-col :span="4">
            <t-form-item label="客户名称" name="name">
              <t-input v-model="formObj.name" placeholder="请输入客户名称" />
            </t-form-item>
          </t-col>

          <t-col :span="4">
            <t-form-item label="粮食品种" name="grainVariety">
              <t-select v-model="formObj.grainVariety" :options="grainTypeOptions" placeholder="请选择粮食品种" />
            </t-form-item>
          </t-col>

          <t-col :span="4">
            <t-form-item class="last-form-item">
              <t-button theme="primary" variant="base" @click="handleSearch" class="search-btn my-icon-btn">
                <search-icon slot="icon" />查询
              </t-button>
              <t-button theme="primary" variant="outline" @click="handleReset" class="reset-btn my-icon-btn">
                <refresh-icon slot="icon" />重置
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
        <template #statusTag="{ row }">
          <t-tag :style="{
            color: statusThemeMap[row.state].color,
            backgroundColor: statusThemeMap[row.state].bgColor,
            borderColor: statusThemeMap[row.state].color,
            height: '26px',
            borderRadius: '4px'
          }" variant="light-outline">
            {{ row.state }}
          </t-tag>
        </template>

        <template #operation="{ row }">
          <t-button theme="primary" variant="text" @click="handleDetail(row.id)">
            查看
          </t-button>
          <t-button v-if="row.state === '进行中'" theme="warning" variant="text" @click="handleCloseBill(row.id)">
            关闭单据
          </t-button>
        </template>

        <!-- 空状态 -->
        <template #empty="slotProps">
          <t-space>
            <div>
              <img style="width: 120px;" src="@/assets/imgs/empty.png">
              <div
                style="font-family: PingFang SC;font-weight: 300;font-style: Regular;font-size: 13px;line-height: 100%;letter-spacing: 0px;color:rgba(149, 149, 149, 1);text-align: center;">
                暂无数据
              </div>
            </div>
          </t-space>
        </template>
      </t-table>
    </div>

    <!-- 弹窗组件 -->
    <Create v-if="dialogVisible" v-model:visible="dialogVisible" :id="noticeId" :type="type" @submit="handleSubmit" />

    <!-- 关闭单据确认对话框 -->
    <t-dialog v-model:visible="confirmVisible" header="关闭单据确认" class="newDialog newclassNewDialog"
      :cancelBtn="{ theme: 'default', variant: 'outline' }" :confirmBtn="{ theme: 'warning', variant: 'base' }"
      @confirm="onConfirmCloseBill">
      <div class="deletCon">
        <div class="delectConHeader">
          <t-icon name="error-circle-filled" size="18px" color="rgba(255, 41, 75, 1)" style="margin-right: 8px;" />
          <span class="a">确定要关闭该单据吗？</span>
        </div>
        <div class="b">单据一旦关闭，将无法进行入库登记，请谨慎操作。</div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup name="inboundOrder">
import { ref, onMounted, computed } from "vue";
import { MessagePlugin } from "tdesign-vue-next";
import {
  SearchIcon,
  RefreshIcon,
  AddIcon,
} from "tdesign-icons-vue-next";
import Create from "./components/Create.vue";
import { getDict } from "@/api/sampling.ts";
import { getList, deleteApi } from "@/api/inboundorder";

// 表格列定义
const tableColumns = [
  { colKey: "row-select", type: "single", width: 50, title: "选择" },
  { colKey: "serial-number", width: 60, title: "序号" },
  { colKey: "id", title: "通知单编码" },
  { colKey: "grainSourceType", title: "粮源类型" },
  { colKey: "addUser", title: "申请人" },
  { colKey: "name", title: "客户名称" },
  { colKey: "grainVariety", title: "粮食品种" },
  { colKey: "planWeight", title: "预计入库量(KG)" },
  { colKey: "completedWeight", title: "完成量" },
  { colKey: "addTime", title: "申请时间" },
  // { colKey: "executePeriod", title: "执行周期" },
  {
    title: "执行周期",
    colKey: "executePeriod",
    cell: (h, row) => {
      console.log("表格行数据：", row); // 加这行
      if (row.row.startTime && row.row.endTime) {
        const start = row.row.startTime.split(' ')[0];
        const end = row.row.endTime.split(' ')[0];
        return `${start}--${end}`;
      }
      return "-";
    },
  },
  { colKey: "state", title: "单据状态", cell: "statusTag" },
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
const noticeId = ref("");
const confirmVisible = ref(false);
const deleteIds = ref([]);
const grainTypeOptions = ref([]);

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

// 分页配置
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showPageSize: true,
  showPreviousAndNextBtn: true,
});

//状态颜色匹配
const statusThemeMap = {
  "进行中": { color: "#186DF5", bgColor: "#186DF533" },
  "已完成": { color: "#2BA471", bgColor: "#E3F9E9" },
  "已关闭": { color: "#F59E0B", bgColor: " #F59E0B33" },
};

// 计算属性
const billStatusOptions = ref([
  { label: "进行中", value: 1 },
  { label: "已完成", value: 2 },
  { label: "已关闭", value: 3 },
]);

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
    console.log("response.result.data =", response.result.data);
    if (response.status.code !== "200") {
      throw new Error(response.status.message || "获取列表失败");
    }

    //处理单据状态类型显示
    response.result.data.forEach((item) => {
      item.state =
        billStatusOptions.value.find((v) => v.value === item.state)
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
  noticeId.value = id;
  dialogVisible.value = true;
};

// 关闭单据操作
const handleCloseBill = (id) => {
  deleteIds.value = [id];
  console.log("delete", deleteIds.value);

  confirmVisible.value = true;
};

const onConfirmCloseBill = async () => {
  try {
    const response = await deleteApi({ id: deleteIds.value });
    if (response.status.code !== "200") {
      throw new Error(response.status.message || "关闭单据失败");
    }

    MessagePlugin.success("关闭单据成功");

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
  getgrainTypeOptions();
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