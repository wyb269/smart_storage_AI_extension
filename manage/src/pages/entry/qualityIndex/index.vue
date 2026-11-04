<template>
  <div class="container">
    <div class="top">
      <div class="left">
        <div class="title-box">
          <div class="title">
            <img
              src="@/assets/quality/icon1.png"
              alt=""
            >
            质量检验
          </div>
          <readCard @result="result" />
        </div>

        <t-divider style="margin: 15px 0;" />

        <div class="forNewclass">
          <t-form
            :data="formObj"
            :rules="rules"
            ref="formRef"
            class="left-form"
            labelAlign="top"
          >
            <t-row
              class="form-row"
              :gutter="65"
            >
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="智能卡编号"
                  name="cardNo"
                >
                  <t-input
                    disabled
                    v-model="formObj.cardNo"
                    placeholder="请输入智能卡编号"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="承运人"
                  name="carrierName"
                >
                  <t-input
                    disabled
                    v-model="formObj.carrierName"
                    placeholder="请输入承运人"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="车牌号"
                  name="licensePlate"
                >
                  <t-input
                    disabled
                    v-model="formObj.licensePlate"
                    placeholder="请输入车牌号"
                  ></t-input>
                </t-form-item>
              </t-col>
            </t-row>

            <t-row
              class="form-row"
              :gutter="65"
            >
              <t-col
                :span="12"
                class="form-col"
              >
                <t-form-item
                  label="客户名称"
                  name="customerName"
                >
                  <t-input
                    disabled
                    v-model="formObj.customerName"
                    placeholder="请输入客户名称"
                  ></t-input>
                </t-form-item>
              </t-col>
            </t-row>

            <t-row
              class="form-row"
              :gutter="65"
            >
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="作业编号"
                  name="jobNo"
                >
                  <t-input
                    disabled
                    v-model="formObj.jobNo"
                    placeholder="请输入作业编号"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="粮食品种"
                  name="grainVariety"
                >
                  <t-select
                    disabled
                    v-model="formObj.grainVariety"
                    :options="grainVarietyOptions"
                    placeholder="请选择粮食品种"
                  ></t-select>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="粮仓号"
                  name="warehouseName"
                >
                  <t-input
                    readonly
                    v-model="formObj.warehouseName"
                    placeholder="请选择粮仓号"
                    @click="openWarehouseDialog"
                  ></t-input>
                </t-form-item>
              </t-col>
            </t-row>

            <t-row
              class="form-row"
              :gutter="65"
            >
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="粮食等级"
                  name="grainGrade"
                >
                  <t-select
                    v-model="formObj.grainGrade"
                    :options="grainGradeOptions"
                    placeholder="请选择粮食等级"
                    :disabled="grainGradeFixed"
                  ></t-select>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="粮食年期"
                  name="grainYear"
                >
                  <t-select
                    v-model="formObj.grainYear"
                    :options="grainYearOptions"
                    placeholder="请选择粮食年期"
                    :disabled="grainYearFixed"
                  ></t-select>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="粮食单价"
                  name="grainPrice"
                >
                  <t-input
                    v-model="formObj.grainPrice"
                    placeholder="请输入粮食单价"
                    suffix="元/千克"
                  ></t-input>
                </t-form-item>
              </t-col>

            </t-row>

            <t-row
              class="form-row"
              :gutter="65"
            >
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="每千克扣价"
                  name="deductionPrice"
                >
                  <t-input
                    v-model="formObj.deductionPrice"
                    placeholder="请输入每千克扣价"
                    suffix="元"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="每千克扣量"
                  name="deduction"
                >
                  <t-input
                    v-model="formObj.deduction"
                    placeholder="请输入每千克扣量"
                    suffix="千克"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="质检员"
                  name="inspector"
                >
                  <t-input
                    disabled
                    v-model="formObj.inspector"
                    placeholder="请输入质检员"
                  ></t-input>
                </t-form-item>
              </t-col>
            </t-row>

            <t-row
              class="form-row"
              :gutter="65"
            >
              <t-col
                :span="12"
                class="form-col"
              >
                <t-form-item
                  label="备注"
                  name="remarks"
                  class="remarks-item"
                >
                  <t-textarea
                    v-model="formObj.remarks"
                    :maxlength="160"
                    show-limit-number
                    placeholder="请输入备注，限制长度160"
                  ></t-textarea>
                </t-form-item>
              </t-col>
            </t-row>
          </t-form>
        </div>
      </div>
      <div class="right">
        <div class="title-box">
          <div class="title">
            <img
              src="@/assets/quality/icon3.png"
              alt=""
            >
            质检结果选择
          </div>
        </div>
        <div class="result">
          <div
            class="result-item"
            :class="{ 'result-item-selected': selectedResult === 1 }"
            @click="handleSelectResult(1)"
          >
            <img
              src="@/assets/quality/qualified.png"
              alt=""
            >
            合格
          </div>
          <div
            class="result-item"
            :class="{ 'result-item-selected': selectedResult === 2 }"
            @click="handleSelectResult(2)"
          >
            <img
              src="@/assets/quality/unqualified.png"
              alt=""
            >
            不合格
          </div>
          <t-button
            class="result-button"
            :disabled="!selectedResult"
            @click="handleSubmit"
            style="width: 62%;"
          >
            <template #icon>
              <img
                v-if="!selectedResult"
                src="@/assets/quality/icon5.png"
                class="button-icon"
              >
              <img
                v-else
                src="@/assets/quality/icon4.png"
                class="button-icon"
              >
            </template>
            保存
          </t-button>
        </div>
      </div>
    </div>

    <t-dialog
      v-model:visible="warehouseDialogVisible"
      header="仓库信息"
      theme="default"
      :confirmBtn="{ theme: 'warning', variant: 'base' }"
      :cancelBtn="{ theme: 'default', variant: 'outline' }"
      @confirm="confirmWarehouseSelection"
      @cancel="closeWarehouseDialog"
      width="900px"
      class="newDialog newclassNewDialog warehouse-dialog"
    >
      <div class="forNewClassTableAndsearch">
        <t-table
          style="min-height: 500px; border-bottom: 1px solid #eeeff2;"
          :loading="warehouseDialogLoading"
          :data="warehouseDialogData"
          rowKey="id"
          :columns="[
            { colKey: 'row-select', type: 'single', checkProps: { allowUncheck: true }, align: 'center' },
            { title: '仓库名称', colKey: 'warehouseName', minWidth: 140, align: 'center' },
            { title: '仓库类型', colKey: 'warehouseType', minWidth: 120, align: 'center' },
            { title: '粮食品种', colKey: 'grainVarietyLabel', minWidth: 120, align: 'center' },
            { title: '粮食等级', colKey: 'grainGradeLabel', minWidth: 120, align: 'center' },
            { title: '粮食年期', colKey: 'grainYearLabel', minWidth: 120, align: 'center' },
          ]"
          :selected-row-keys="warehouseSelectedKeys"
          :select-on-row-click="true"
          @select-change="onWarehouseSelectChange"
          :pagination="{
            current: warehouseDialogPagination.current,
            pageSize: warehouseDialogPagination.pageSize,
            total: warehouseDialogPagination.total,
            showPageSize: false,
            showPreviousAndNextBtn: true
          }"
          @page-change="(pageInfo) => loadWarehouseDialogData(pageInfo.current)"
        >
        </t-table>
      </div>
    </t-dialog>

    <div class="bottom">
      <div class="title-box">
        <div class="title">
          <img
            src="@/assets/quality/icon2.png"
            alt=""
          >
          今日质检记录
        </div>
      </div>
      <div
        class="forNewClassTableAndsearch"
        style="margin-top: 20px;"
      >
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
  </div>
</template>

<script setup name="quality">
import { computed, onMounted, ref } from "vue";
import { MessagePlugin } from "tdesign-vue-next";
import readCard from "@/components/readCard/index.vue";
import { getDict, swipeCard, getTaskById } from "@/api/sampling.ts";
import { useSettingStore, useUserStore } from "@/store";
import { getList, insert, listWarehouseByModal } from "@/api/quality.ts";
import { getSampleItemByTaskId } from "@/api/sampling.ts";

const user = useUserStore();

const status = ref("待质检");

const formObj = ref({});
const formRef = ref(null);
const selectedResult = ref(null); // 记录选择的结果

const warehouseDialogVisible = ref(false);
const warehouseDialogLoading = ref(false);
const warehouseDialogData = ref([]);
const warehouseDialogPagination = ref({ current: 1, pageSize: 10, total: 0 });
const warehouseSelectedKeys = ref([]);
const selectedWarehouseRow = ref(null);

const grainGradeFixed = ref(false);
const grainYearFixed = ref(false);

const rules = {
  jobNo: [{ required: true, message: "请输入作业编号" }],
  grainVariety: [{ required: true, message: "请选择粮食品种" }],
  grainGrade: [{ required: true, message: "请选择粮食等级" }],
  grainYear: [{ required: true, message: "请选择粮食年期" }],
  warehouseName: [{ required: true, message: "请选择粮仓号" }],
  grainPrice: [
    { required: true, message: "请输入粮食单价" },
    {
      pattern: /^(0|[1-9]\d*)(\.\d{1,2})?$/,
      message: "粮食单价必须为正数且保留两位小数",
    },
  ],
  deductionPrice: [
    { required: true, message: "请输入每千克扣价" },
    {
      pattern: /^(0|[1-9]\d*)(\.\d{1,2})?$/,
      message: "每千克扣价必须为正数且保留两位小数",
    },
  ],
  deduction: [
    { required: true, message: "请输入每千克扣量" },
    {
      pattern: /^(0|[1-9]\d*)(\.\d{1,2})?$/,
      message: "每千克扣量必须为正数且保留两位小数",
    },
  ],
};

// 粮食品种选项
const grainVarietyOptions = ref([]);

// 粮食年期选项
const grainYearOptions = ref([]);

// 粮食等级选项
const grainGradeOptions = ref([]);

// 粮仓号选项
const grainStorageOptions = ref([]);

// 表格列定义
const loading = ref(false);
const tableColumns = [
  { colKey: "serial-number", width: 60, title: "序号" },
  { colKey: "cardNo", title: "智能卡编号", minWidth: 120 },
  { colKey: "qualityNo", title: "质检单编号", minWidth: 120 },
  { colKey: "grainVariety", title: "粮食品种" },
  { colKey: "grainYear", title: "粮食年期" },
  { colKey: "grainGrade", title: "粮食等级" },
  { colKey: "grainPrice", title: "粮食单价" },
  { colKey: "deduction", title: "每千克扣量" },
  { colKey: "deductionPrice", title: "每千克扣价" },
  { colKey: "warehouseName", title: "粮仓号" },
  { colKey: "qualityResult", title: "质检单状态" },
  { colKey: "addTime", title: "时间", minWidth: 140 },
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

const result = async (r) => {
  // 使用 reset 方法重置表单
  formRef.value?.reset();

  formObj.value.inspector = user.userInfo.user.name || "";

  if (r.result == 1) {
    let response = await swipeCard({
      physicalCardNumber: r.data,
      processStep: "inspected",
    });
    if (response.status.code != "200") {
      MessagePlugin.error(response.status.message || "读卡失败");
      return;
    }
    if (
      response.result.jobStatus == "sampled" &&
      response.result.jobType == 1 &&
      response.result.state == 1 &&
      response.result.taskId
    ) {
      let res = await getTaskById(response.result.taskId);
      if (res.status.code != "200") {
        MessagePlugin.error(res.status.message || "获取作业信息失败");
        return;
      }
      formObj.value.cardNo = response.result.cardNo || "";
      formObj.value.carrierName = res.result.carrierName || "";
      formObj.value.licensePlate = res.result.licensePlate || "";
      formObj.value.customerName = res.result.customerName || "";
      formObj.value.jobNo = res.result.jobNo || "";
      formObj.value.taskId = response.result.taskId || "";

      let sampleRes = await getSampleItemByTaskId(response.result.taskId);
      if (sampleRes.status.code != "200") {
        MessagePlugin.error(sampleRes.status.message || "获取质检信息失败");
        return;
      }
      formObj.value.grainVariety = sampleRes.result.grainVariety || "";
    } else {
      MessagePlugin.error("当前作业状态不允许质检");
      return;
    }
  }
};

const getTableData = async () => {
  loading.value = true;
  let data = JSON.parse(JSON.stringify(pagination.value));
  let form = {
    pageNo: data.current,
    pageSize: data.pageSize,
  };
  try {
    const response = await getList(form);

    if (response.status.code != "200") {
      MessagePlugin.error(response.status.message || "获取列表失败");
      return;
    }

    response.result.data.forEach((item) => {
      item.grainVariety =
        grainVarietyOptions.value.find((v) => v.value == item.grainVariety)
          ?.label || "";
      item.grainYear =
        grainYearOptions.value.find((v) => v.value == item.grainYear)?.label ||
        "";
      item.grainGrade =
        grainGradeOptions.value.find((v) => v.value == item.grainGrade)
          ?.label || "";

      if (item.qualityResult == 1) {
        item.qualityResult = "合格";
      } else if (item.qualityResult == 2) {
        item.qualityResult = "不合格";
      } else {
        item.qualityResult = "未知";
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

const loadWarehouseDialogData = async (page = 1) => {
  warehouseDialogLoading.value = true;
  try {
    const params = {
      grainVariety: formObj.value.grainVariety || "",
      pageNo: page,
      pageSize: warehouseDialogPagination.value.pageSize,
    };

    if (!params.grainVariety) {
      MessagePlugin.error("粮食品种不能为空");
      return;
    }

    const response = await listWarehouseByModal(params);
    if (response.status.code !== "200") {
      MessagePlugin.error(response.status.message || "仓库列表获取失败");
      return;
    }
    const data = response.result.data || [];

    warehouseDialogData.value = data.map((item) => ({
      ...item,
      grainGrade: item.grainGrade || null,
      grainYear: item.grainYear || null,
    }));

    // 如果已选仓库存在，则在当前页面保持选中状态
    if (formObj.value.warehouseId) {
      const selected = warehouseDialogData.value.find(
        (item) => item.id === formObj.value.warehouseId
      );
      if (selected) {
        warehouseSelectedKeys.value = [formObj.value.warehouseId];
        selectedWarehouseRow.value = selected;
      }
    }

    warehouseDialogPagination.value.total = response.result.total || 0;
    warehouseDialogPagination.value.current = page;
  } catch (error) {
    MessagePlugin.error("仓库列表获取失败");
  } finally {
    warehouseDialogLoading.value = false;
  }
};

const openWarehouseDialog = async () => {
  warehouseDialogVisible.value = true;
  // 不重置已选仓库，保持上一次选择状态
  if (formObj.value.warehouseId) {
    warehouseSelectedKeys.value = [formObj.value.warehouseId];
  }
  await loadWarehouseDialogData(1);
};

const onWarehouseSelectChange = (keys, rows) => {
  warehouseSelectedKeys.value = keys;
  selectedWarehouseRow.value =
    rows && rows.selectedRowData.length > 0 ? rows.selectedRowData[0] : null;
};

const confirmWarehouseSelection = () => {
  if (!selectedWarehouseRow.value) {
    MessagePlugin.warning("请先选择仓库");
    return;
  }

  const row = selectedWarehouseRow.value;
  formObj.value.warehouseId = row.id || "";
  formObj.value.warehouseName = row.warehouseName || "";

  // 处理粮食等级：仅当仓库提供了有效值且存在于选项列表中时设置
  const gradeValid =
    row.grainGrade &&
    grainGradeOptions.value.some((opt) => opt.value == row.grainGrade);
  console.log("🌷🌷 ~ gradeValid:", gradeValid);
  formObj.value.grainGrade = gradeValid ? row.grainGrade : "";
  console.log("🌷🌷 ~ row.grainGrade:", row.grainGrade);
  grainGradeFixed.value = gradeValid;

  // 处理粮食年期：同上
  const yearValid =
    row.grainYear &&
    grainYearOptions.value.some((opt) => opt.value == row.grainYear);
  console.log("🌷🌷 ~ yearValid:", yearValid);
  formObj.value.grainYear = yearValid ? row.grainYear : "";
  console.log("🌷🌷 ~ row.grainYear:", row.grainYear);
  grainYearFixed.value = yearValid;

  warehouseDialogVisible.value = false;
};

const closeWarehouseDialog = () => {
  warehouseDialogVisible.value = false;
};

const handleSelectResult = (result) => {
  selectedResult.value = selectedResult.value === result ? null : result;
};

const handleSubmit = async () => {
  const result = await formRef.value.validate();
  if (result === true) {
    let data = JSON.parse(JSON.stringify(formObj.value));
    let form = {
      taskId: data.taskId,
      grainYear: data.grainYear,
      grainGrade: data.grainGrade,
      grainPrice: data.grainPrice,
      deduction: data.deduction || "",
      deductionPrice: data.deductionPrice || "",
      warehouseId: data.warehouseId,
      qualityResult: selectedResult.value,
      remarks: data.remarks || "",
    };

    if (form.deductionPrice && form.deductionPrice >= form.grainPrice) {
      MessagePlugin.error("每千克扣价应该小于粮食单价");
      return;
    }

    if (form.deduction && form.deduction >= 1) {
      MessagePlugin.error("每千克扣量应该小于1");
      return;
    }

    let response = await insert(form);

    if (response.status.code != "200") {
      MessagePlugin.error(response.status.message || "质检信息提交失败");
      return;
    }

    MessagePlugin.success("质检信息提交成功");
    getTableData();

    // 使用 reset 方法重置表单
    formRef.value?.reset();
    formObj.value.inspector = user.userInfo.user.name || "";
    selectedResult.value = null;
  }
};

// 获取粮食品种
const getGrainVarietyOptions = () => {
  getDict("GRAIN_VARIETY")
    .then((res) => {
      const data = res.result.listDictData || [];
      grainVarietyOptions.value = data.map((item) => ({
        label: item.label,
        value: item.description,
      }));
    })
    .catch((error) => {
      console.error("获取粮食品种失败:", error);
    });
};

// 获取粮食年期
const getGrainYearOptions = () => {
  getDict("GRAIN_YEAR")
    .then((res) => {
      const data = res.result.listDictData || [];
      grainYearOptions.value = data.map((item) => ({
        label: item.label,
        value: item.description,
      }));
    })
    .catch((error) => {});
};

// 获取粮食等级
const getGrainGradeOptions = () => {
  getDict("GRAIN_GRADE")
    .then((res) => {
      const data = res.result.listDictData || [];
      grainGradeOptions.value = data.map((item) => ({
        label: item.label,
        value: Number(item.description),
      }));
    })
    .catch((error) => {});
};

// 获取粮仓号
const getGrainStorageOptions = () => {
  getDict("STORAGE_WAREHOUSE_NUMBER")
    .then((res) => {
      const data = res.result.listDictData || [];
      grainStorageOptions.value = data.map((item) => ({
        label: item.label,
        value: item.description,
      }));
    })
    .catch((error) => {});
};

const onPageChange = (pageInfo) => {
  pagination.value.current = pageInfo.current;
  pagination.value.pageSize = pageInfo.pageSize;
};

onMounted(() => {
  getGrainVarietyOptions();
  getGrainYearOptions();
  getGrainGradeOptions();
  getGrainStorageOptions();

  getTableData();
  formObj.value.inspector = user.userInfo.user.name || "";
});
</script>

<style lang="less" scoped>
.container {
  width: 100%;
}

.left,
.right,
.bottom {
  padding: 15px;
  border-radius: 4px;
  background-color: #fff;
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

.title-box {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 4px;
}

.title {
  font-size: 16px;
  font-weight: 500;
  font-family: PingFang SC;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #000;

  img {
    height: 24px;
    object-fit: contain;
  }
}

.button-icon {
  width: 16px;
  object-fit: contain;
  margin-right: 10px;
}

.top {
  width: 100%;
  display: flex;
  gap: 25px;
  justify-content: space-between;
  margin-bottom: 25px;
  flex: 1 0 auto;

  .tag {
    padding: 0 14px;
    border-radius: 4px;
    background-color: #edf8f1;
    font-size: 12px;
    color: #5fc486;
  }

  .left {
    width: 80%;

    .left-button {
      margin-bottom: 10px;
    }

    ::v-deep .left-form {
      width: 100%;

      .t-textarea__inner {
        font-size: 14px;
        font-family: PingFang SC;
        min-height: 64px;
      }

      .form-row {
        margin-bottom: 12px;

        .form-col {
          padding: 0 8px;

          &:first-child {
            padding-left: 0;
          }

          &:last-child {
            padding-right: 0;
          }

          ::v-deep .t-form__item {
            margin-bottom: 0;
          }
        }
      }
    }
  }

  .right {
    flex: 1;
    display: flex;
    flex-direction: column;

    .result {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: space-evenly;
      align-items: center;

      .result-item {
        width: 100%;
        height: 200px;
        color: #059669;
        font-family: PingFang SC;
        font-size: 14px;
        border: 1px solid #e3e7ef;
        border-radius: 4px;
        background-color: #f8fafc;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: all 0.3s ease;

        img {
          width: 46px;
          height: 46px;
          object-fit: contain;
          margin-bottom: 15px;
        }
      }

      .result-item:nth-child(2) {
        color: #dc2626;
      }

      .result-item:nth-child(1):hover {
        background-color: #f1fdf5;
        border-color: #059669;
      }

      .result-item:nth-child(2):hover {
        background-color: #fbf2f2;
        border-color: #dc2626;
      }

      .result-item-selected:nth-child(1) {
        background-color: #f1fdf5;
        border: 1px solid #059669;
      }

      .result-item-selected:nth-child(2) {
        background-color: #fbf2f2;
        border: 1px solid #dc2626;
      }

      .result-button {
        background: linear-gradient(90deg, #f59e0b 0%, #d97706 100%) !important;
        color: #fff !important;
        border: none !important;

        &.t-is-disabled {
          background: #f8fafc !important;
          color: #959595 !important;
          border: none !important;
        }

        &:not(.t-is-disabled):not(.t-button--ghost) {
          --ripple-color: var(--td-warning-color-active);
        }
      }
    }
  }
}

.bottom {
  width: 100%;
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;

  .title {
    flex-shrink: 0;
  }

  .quality-table-container {
    flex: 1;
    overflow-y: auto;
    font-size: 14px;
    font-family: PingFang SC;
    margin-top: 15px;
  }
}

::v-deep .warehouse-dialog {
  .t-button {
    width: 68px !important;
    height: 30px !important;
  }

  .t-radio.t-is-checked .t-radio__input {
    border-color: #f59e0b;
  }

  .t-radio__input::after {
    background-color: #f59e0b;
  }

  .t-radio:hover .t-radio__input {
    border-color: #f59e0b;
  }
}
</style>