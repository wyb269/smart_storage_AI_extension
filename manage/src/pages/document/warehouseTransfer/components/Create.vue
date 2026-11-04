<template>
  <t-dialog :visible="visible" @update:visible="handleVisibleChange" width="860px" class="newDialog newclassNewDialog"
    :cancelBtn="type === '查看' ? null : { theme: 'default', variant: 'outline' }"
    :confirmBtn="type === '查看' ? null : { theme: 'warning', variant: 'base' }" :confirm-on-enter="true"
    :on-confirm="handleSubmit">
    <template #header>
      <div class="dialog-header">
        <span>倒仓申请单</span>
        <t-tag v-if="type === '查看' && formObj.status" :style="getStatusTagStyle(formObj.status)" variant="light-outline"
          class="status-tag">
          {{ statusLabelMap[formObj.status] || formObj.status }}
        </t-tag>
      </div>
    </template>

    <div class="forNewclass">
      <t-form :data="formObj" :rules="type === '查看' ? {} : rules" ref="formRef" :labelWidth="120" class="create-form">
        <!-- 第一行：申请单编码 + 申请时间 -->
        <t-row class="form-row" :gutter="32">
          <t-col :span="6" class="form-col">
            <t-form-item label="申请单编码" name="applyCode">
              <t-input disabled v-model="formObj.applyCode" placeholder="系统自动生成" />
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="申请时间" name="applyTime">
              <t-input disabled v-model="formObj.applyTime" placeholder="系统自动生成" />
            </t-form-item>
          </t-col>
        </t-row>

        <!-- 第二行：申请人 + 倒仓类型 -->
        <t-row class="form-row" :gutter="32">
          <t-col :span="6" class="form-col">
            <t-form-item label="申请人" name="applicant">
              <t-input disabled v-model="formObj.applicant" placeholder="系统自动生成" />
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="倒仓类型" name="transferType">
              <t-select :disabled="disabled" v-model="formObj.transferType" :options="transferTypeOptions"
                placeholder="请选择倒仓类型" />
            </t-form-item>
          </t-col>
        </t-row>

        <!-- 第三行：移出仓库 + 粮食品种 -->
        <t-row class="form-row" :gutter="32">
          <t-col :span="6" class="form-col">
            <t-form-item label="移出仓库" name="fromWarehouse">
              <t-select :disabled="disabled" v-model="formObj.fromWarehouse" :options="warehouseOptions"
                placeholder="请选择移出仓库" />
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="粮食品种" name="grainType">
              <t-select :disabled="disabled" v-model="formObj.grainType" :options="grainTypeOptions"
                placeholder="请选择粮食品种" />
            </t-form-item>
          </t-col>
        </t-row>

        <!-- 第四行：移入仓库 + 移入仓可用容量 -->
        <t-row class="form-row" :gutter="32">
          <t-col :span="6" class="form-col">
            <t-form-item label="移入仓库" name="toWarehouse">
              <t-select :disabled="disabled" v-model="formObj.toWarehouse" :options="warehouseOptions"
                placeholder="请选择移入仓库" />
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="移入仓可用容量(KG)" name="availableCapacity">
              <t-input disabled v-model="formObj.availableCapacity" placeholder="自动计算" />
            </t-form-item>
          </t-col>
        </t-row>

        <!-- 第五行：预计倒仓量 + 起止时间 -->
        <t-row class="form-row" :gutter="32">
          <t-col :span="6" class="form-col">
            <t-form-item label="预计倒仓量" name="estimateWeight">
              <t-input :disabled="disabled" v-model="formObj.estimateWeight" placeholder="请输入预计倒仓量" />
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="起止时间" name="transferRange">
              <t-date-range-picker :disabled="disabled" v-model="formObj.transferRange" mode="date" allow-input
                clearable style="width: 100%;">
                <template #suffixIcon>
                  <time-icon />
                </template>
              </t-date-range-picker>
            </t-form-item>
          </t-col>
        </t-row>

        <!-- 第六行：倒仓原因（占一整行） -->
        <t-row class="form-row" :gutter="32">
          <t-col :span="12" class="form-col">
            <t-form-item label="倒仓原因" name="transferReason">
              <t-textarea :disabled="disabled" v-model="formObj.transferReason" :maxlength="200" show-limit-number
                placeholder="请输入倒仓原因" />
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>
    </div>
  </t-dialog>
</template>

<script setup name="WarehouseTransferCreate">
import { MessagePlugin } from "tdesign-vue-next";
import {
  ref,
  onMounted,
  watch,
  nextTick,
} from "vue";
import { TimeIcon } from "tdesign-icons-vue-next";
import { getDict } from "@/api/sampling.ts";
import {
  insertTransferStorageInfo,
  updateTransferStorageInfo,
  detailTransferStorageInfo,
  listWarehouseByConditionOnPage,
} from "@/api/warehouseTransfer.ts";

const emit = defineEmits(["submit", "update:visible", "capacity"]);

const props = defineProps({
  type: {
    type: String,
    default: "",
  },
  rowData: {
    type: Object,
    default: () => ({}),
  },
  visible: {
    type: Boolean,
    default: true,
  },
});

// 倒仓类型选项（与列表页保持一致）
const transferTypeOptions = ref([
  { label: "粮情异常倒仓", value: 1 },
  { label: "质量分储倒仓", value: 2 },
  { label: "仓房维修倒仓", value: 3 },
  { label: "设备升级倒仓", value: 4 },
  { label: "仓房熏蒸倒仓", value: 5 },
  { label: "储备粮轮换倒仓", value: 6 },
  { label: "专项储备倒仓", value: 7 },
  { label: "应急避险倒仓", value: 8 },
  { label: "应急供应倒仓", value: 9 },
  { label: "仓容平衡倒仓", value: 10 },
  { label: "品种集中倒仓", value: 11 },
  { label: "实验性倒仓", value: 12 },
  { label: "跨库调拨倒仓", value: 13 },
]);

// 仓库选项（从字典接口获取）
const warehouseOptions = ref([]);

// 粮食品种选项（从字典接口获取）
const grainTypeOptions = ref([]);

// 单据状态样式映射
const statusColorMap = {
  1: { color: "#186DF5", bgColor: "#F2F3FF" },
  2: { color: "#EF9645", bgColor: "#FDF4EC" },
  3: { color: "#E34D59", bgColor: "#FDECEE" },
  4: { color: "#B0B0B0", bgColor: "#F6F6F8" },
  5: { color: "#2BA471", bgColor: "#E3F9E9" },
  6: { color: "#8E8E8E", bgColor: "#F6F6F8" },
};

const statusLabelMap = {
  1: "待提交",
  2: "审批中",
  3: "已驳回",
  4: "已作废",
  5: "审批完成",
  6: "已撤回",
};

const disabled = ref(false);
const formRef = ref();

// 默认表单数据，避免组件初始化时取到 undefined
const getDefaultForm = () => ({
  applyCode: "",
  applyTime: "",
  applicant: "",
  transferType: "",
  fromWarehouse: "",
  grainType: "",
  toWarehouse: "",
  availableCapacity: "",
  estimateWeight: "",
  transferRange: [],
  transferReason: "",
  status: "",
});

const formObj = ref(getDefaultForm());
const availableCapacity = ref(0);

const rules = {
  transferType: [{ required: true, message: "请选择倒仓类型" }],
  fromWarehouse: [{ required: true, message: "请选择移出仓库" }],
  grainType: [{ required: true, message: "请选择粮食品种" }],
  toWarehouse: [{ required: true, message: "请选择移入仓库" }],
  estimateWeight: [
    { required: true, message: "请输入预计倒仓量" },
    {
      pattern: /^(0|[1-9]\d*)(\.\d{1,2})?$/,
      message: "请输入正确的数值（最多两位小数）",
      trigger: "blur",
    },
  ],
  transferRange: [{ required: true, message: "请选择起止时间", trigger: "submit" }],
  // 说明：日期范围在弹窗初始化时可能触发内部 blur/change，改为仅提交时校验，避免新增弹窗一打开就报错。
};

// 状态标签样式
const getStatusTagStyle = (status) => {
  const defaultStyle = {
    color: "#999",
    backgroundColor: "#F5F5F5",
    borderColor: "#999",
  };
  const statusStyle = statusColorMap[status] || defaultStyle;
  return {
    color: statusStyle.color,
    backgroundColor: statusStyle.bgColor,
    borderColor: statusStyle.color,
    height: "26px",
    borderRadius: "4px",
  };
};

// 提交处理
const handleSubmit = async () => {
  const result = await formRef.value.validate();
  if (result === true) {
    const [startTime, endTime] = formObj.value.transferRange || [];
    const payload = {
      transferType: formObj.value.transferType,
      sourceWarehouseId: formObj.value.fromWarehouse,
      targetWarehouseId: formObj.value.toWarehouse,
      grainVariety: formObj.value.grainType,
      estimatedQuantity: formObj.value.estimateWeight,
      startTime,
      endTime,
      transferReason: formObj.value.transferReason,
    };

    if (Number(formObj.value.estimateWeight) > availableCapacity.value) {
      emit("capacity");
      return;
    }

    if (props.type === "新增") {
      const response = await insertTransferStorageInfo(payload);

      if (response.status.code != "200") {
        MessagePlugin.error(response.status.message || "新增失败");
        return;
      }

      MessagePlugin.success("新增成功");
      emit("submit");
      emit("update:visible", false);
    } else if (props.type === "编辑") {
      const response = await updateTransferStorageInfo({
        id: formObj.value.id,
        ...payload,
      });

      if (response.status.code != "200") {
        MessagePlugin.error(response.status.message || "编辑失败");
        return;
      }

      MessagePlugin.success("编辑成功");
      emit("submit");
      emit("update:visible", false);
    } else {
      emit("update:visible", false);
    }
  }
};

// 弹窗关闭处理
const handleVisibleChange = (value) => {
  if (!value) {
    if (formRef.value) {
      formRef.value.reset();
    }
    formObj.value = getDefaultForm();
  }
  emit("update:visible", value);
};

// 获取粮食品种选项
const getGrainTypeOptions = () => {
  getDict("GRAIN_VARIETY")
    .then((res) => {
      const data = res.result.listDictData || [];
      grainTypeOptions.value = data.map((item) => ({
        label: item.label,
        value: item.description,
      }));
    })
    .catch((error) => {
      console.error("获取粮食品种失败:", error);
    });
};

// 获取仓库选项
const getWarehouseOptions = async () => {
  const response = await listWarehouseByConditionOnPage({
    id: "",
    warehouseName: "",
    warehouseType: 0,
    grainVariety: "",
    state: 0,
  });
  if (response?.status?.code !== "200") {
    MessagePlugin.error(response?.status?.message || "获取仓库列表失败");
    return;
  }
  const list = response.result?.data || response.result || [];
  warehouseOptions.value = list.map((item) => ({
    label: item.warehouseName || item.name || "-",
    value: String(item.id ?? item.warehouseId ?? ""),
    maxWeight: Number(item.maxWeight ?? 0),
    currentWeight: Number(item.currentWeight ?? 0),
  }));
  calcAvailableCapacity(formObj.value.toWarehouse);
};

// 获取详情（编辑/查看时回显）
const getDetail = async () => {
  if (!props.rowData?.id) {
    return;
  }
  const response = await detailTransferStorageInfo(props.rowData.id);
  if (response?.status?.code !== "200") {
    MessagePlugin.error(response?.status?.message || "获取详情失败");
    return;
  }
  const data = response.result || {};
  formObj.value = {
    ...getDefaultForm(),
    id: data.id,
    applyCode: data.id || "",
    applyTime: data.addTime || "",
    applicant: data.addUserName || "",
    transferType: data.transferType || "",
    fromWarehouse: data.sourceWarehouseId || "",
    grainType: data.grainVariety || "",
    toWarehouse: data.targetWarehouseId || "",
    availableCapacity: "",
    estimateWeight: data.estimatedQuantity || "",
    transferRange: [data.startTime || "", data.endTime || ""],
    transferReason: data.transferReason || "",
    status: data.status || "",
  };

  nextTick(() => {
    calcAvailableCapacity(formObj.value.toWarehouse);
    formObj.value = { ...formObj.value };
  });
};

// 监听弹窗显隐
watch(
  () => props.visible,
  (newVal) => {
    if (!newVal) {
      if (formRef.value) {
        formRef.value.reset();
      }
      formObj.value = getDefaultForm();
    } else if (newVal && props.type === "新增") {
      nextTick(() => {
        formObj.value = getDefaultForm();
        // 嵌套 nextTick：等 Vue 完成因数据变化触发的响应式更新和校验后再清除
        nextTick(() => {
          if (formRef.value) {
            formRef.value.clearValidate();
          }
        });
      });
    }
  },
  { immediate: true }
);

// 初始化
const init = () => {
  disabled.value = props.type === "查看";
  if (props.type === "查看" || props.type === "编辑") {
    getDetail();
  }
};

const calcAvailableCapacity = (warehouseId) => {
  const selected = warehouseOptions.value.find((item) => item.value === String(warehouseId));
  const maxWeight = selected?.maxWeight ?? 0;
  const currentWeight = selected?.currentWeight ?? 0;
  const capacity = Math.max(0, maxWeight - currentWeight);
  availableCapacity.value = capacity;
  formObj.value.availableCapacity = capacity ? String(capacity) : "0";
};

watch(
  () => formObj.value.toWarehouse,
  (value) => {
    if (!value) {
      availableCapacity.value = 0;
      formObj.value.availableCapacity = "0";
      return;
    }
    calcAvailableCapacity(value);
  }
);

onMounted(() => {
  getGrainTypeOptions();
  getWarehouseOptions();
  init();
});
</script>

<style lang="less" scoped>
.dialog-header {
  display: flex;
  align-items: center;
  gap: 12px;

  .status-tag {
    margin-left: 4px;
  }
}

::v-deep .create-form {
  width: 100%;
  padding-top: 40px;
  border-top: 1px solid rgba(238, 239, 242, 1);
  border-bottom: 1px solid rgba(238, 239, 242, 1);

  .t-textarea__inner {
    font-size: 13px;
    font-family: PingFang SC;
    min-height: 64px;
  }

  .t-textarea .t-is-disabled {
    background-color: #f9fafc;
    border: 1px solid #e3e7f0;
    color: #8e97a8;
    box-shadow: none !important;
  }

  // 覆盖全局 .newDialog .t-form .t-input { height:36px } 对日期范围选择器的影响
  .t-date-range-picker .t-input {
    height: auto !important;
  }

  .form-row {
    margin-bottom: 24px;
    margin-left: 0 !important;
    margin-right: 0 !important;

    .t-col {
      padding: 0 var(--td-comp-paddingLR-l) !important;

      &:first-child {
        padding-left: 0 !important;
      }

      &:last-child {
        padding-right: 0 !important;
      }
    }
  }
}
</style>
