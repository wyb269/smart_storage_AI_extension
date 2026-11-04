<template>
  <t-dialog :visible="visible" @update:visible="handleVisibleChange" :header="`${type}出库通知单`" width="860px"
    class="newDialog newclassNewDialog" :cancelBtn="type === '查看' ? null : { theme: 'default', variant: 'outline' }"
    :confirmBtn="type === '查看' ? null : { theme: 'primary', variant: 'base' }" :confirm-on-enter="true"
    :on-confirm="handleSubmit">
    <div class="forNewclass">
      <t-form :data="formObj" :rules="type === '查看' ? {} : rules" ref="formRef" :labelWidth="110" class="create-form">
        <t-row class="form-row" :gutter="65">
          <t-col :span="6" class="form-col">
            <t-form-item label="通知单编码" name="id" :required-mark="type === '新增'">
              <t-input disabled v-model="formObj.id" placeholder="系统自动生成"></t-input>
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="申请人" name="addUser">
              <t-input disabled v-model="formObj.addUser" placeholder="获取账号信息"></t-input>
            </t-form-item>
          </t-col>
        </t-row>

        <t-row class="form-row" :gutter="65">
          <t-col :span="6" class="form-col">
            <t-form-item label="执行开始日期" name="startTime">
              <t-date-picker :disabled="disabled" v-model="formObj.startTime" placeholder="开始日期" format="YYYY-MM-DD"
                value-type="YYYY-MM-DD" style="width: 100%"> <template #suffix-icon>
                  <t-icon name="time" size="16" />
                </template></t-date-picker>
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="执行结束日期" name="endTime">
              <t-date-picker :disabled="disabled" v-model="formObj.endTime" placeholder="结束日期" format="YYYY-MM-DD"
                value-type="YYYY-MM-DD" style="width: 100%"> <template #suffix-icon>
                  <t-icon name="time" size="16" /></template></t-date-picker>
            </t-form-item>
          </t-col>
        </t-row>

        <t-row class="form-row" :gutter="65">
          <t-col :span="6" class="form-col">
            <t-form-item label="收粮客户" name="customerId">
              <t-select :disabled="disabled" v-model="formObj.customerId" :options="customerOptions"
                placeholder="请选择收粮客户" @filter="loadCustomerOptions"></t-select>
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="联系电话" name="phone">
              <t-input disabled v-model="formObj.phone" placeholder="根据客户信息带出"></t-input>
            </t-form-item>
          </t-col>
        </t-row>

        <t-row class="form-row" :gutter="65">
          <t-col :span="6" class="form-col">
            <t-form-item label="出库仓库" name="warehouseId">
              <t-select :disabled="disabled" v-model="formObj.warehouseId" :options="warehouseOptions"
                placeholder="请选择出库仓库" @filter="loadWarehouseOptions"></t-select>
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="库管员" name="keeper">
              <t-input disabled v-model="formObj.keeper" placeholder="根据仓库信息带出"></t-input>
            </t-form-item>
          </t-col>
        </t-row>

        <t-row class="form-row" :gutter="65">
          <t-col :span="6" class="form-col">
            <t-form-item label="粮食品种" name="grainVariety">
              <t-input disabled v-model="formObj.grainVariety" placeholder="根据仓库信息带出"></t-input>
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="粮食等级" name="grainGrade">
              <t-input disabled v-model="formObj.grainGrade" placeholder="根据仓库信息带出"></t-input>
            </t-form-item>
          </t-col>
        </t-row>

        <t-row class="form-row" :gutter="65">
          <t-col :span="6" class="form-col">
            <t-form-item label="粮食年期" name="grainYear">
              <t-input disabled v-model="formObj.grainYear" placeholder="根据仓库信息带出"></t-input>
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="粮源类型" name="grainSourceType">
              <t-select :disabled="disabled" v-model="formObj.grainSourceType" :options="grainSourceTypeOptions"
                placeholder="请选择粮源类型"></t-select>
            </t-form-item>
          </t-col>
        </t-row>

        <t-row class="form-row" :gutter="65">
          <t-col :span="6" class="form-col">
            <t-form-item label="预计出库量(KG)" name="planWeight">
              <t-input :disabled="disabled" v-model="formObj.planWeight" placeholder="请输入预计入库量" type="number"></t-input>
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="完成量(KG)" name="completedWeight" :required-mark="type === '新增'">
              <t-input disabled v-model="formObj.completedWeight" placeholder="自动回显"></t-input>
            </t-form-item>
          </t-col>
        </t-row>
        <t-row class="form-row" :gutter="65" v-if="type === '查看'">
          <t-col :span="6" class="form-col">
            <t-form-item label="申请时间" name="addTime">
              <t-input disabled v-model="formObj.addTime" placeholder="系统自动生成"></t-input>
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="单据状态" name="state">
              <t-input disabled v-model="formattedBillStatus" placeholder="自动回显"></t-input>
            </t-form-item>
          </t-col>
        </t-row>
        <t-row class="form-row" :gutter="65">
          <t-col :span="12" class="form-col">
            <t-form-item label="备注" name="remarks">
              <t-textarea :disabled="disabled" v-model="formObj.remarks" :maxlength="160" show-limit-number
                placeholder="请输入备注，限制长度160"></t-textarea>
            </t-form-item>
          </t-col>
        </t-row>
      </t-form>
    </div>
  </t-dialog>
</template>

<script setup name="Create">
import { MessagePlugin } from "tdesign-vue-next";
import {
  ref,
  getCurrentInstance,
  onMounted,
  watch,
  nextTick,
  computed
} from "vue";
import { getInfo } from "@/api/login";
import { getList } from "@/api/customer";
import { getDict } from "@/api/sampling";
import { insert, detail, getWarehouseList, getWarehouseById } from "@/api/outboundorder";
const { proxy } = getCurrentInstance();
const emit = defineEmits(["submit", "update:visible"]); // 添加 update:visible 事件

const props = defineProps({
  type: {
    type: String,
    default: "",
  },
  id: {
    type: String,
    default: "",
  },
  visible: {
    type: Boolean,
    default: true,
  },
});
const customerOptions = ref([]);
const customerList = ref([]);
const warehouseOptions = ref([]);
const warehouseList = ref([]);
const grainTypeOptions = ref([]);
const grainSourceTypeOptions = ref([]);
const disabled = ref(false);
const formObj = ref({});
const formRef = ref();
const rules = {
  //noticeCode: [{ required: true, message: "系统自动生成" }],
  addUser: [{ required: true, message: "获取账号信息" }],
  startTime: [{ required: true, message: "请选择执行开始日期", trigger: "change" }],
  endTime: [
    { required: true, message: "请选择执行结束日期", trigger: "change" },
    {
      validator: (rule, value) => {
        if (!formObj.value.startTime || !value) return true;
        const start = new Date(formObj.value.startTime).getTime();
        const end = new Date(formObj.value.endTime).getTime();
        if (start >= end) {
          MessagePlugin.error("执行结束日期必须晚于开始日期");
          throw new Error("执行结束日期必须晚于开始日期");
        }
        return true;
      },
      trigger: "change"
    }
  ],
  customerId: [{ required: true, message: "请选择客户名称" }],
  phone: [{ required: true, message: "根据客户信息带出" }],
  warehouseId: [{ required: true, message: "根据客户信息带出" }],
  keeper: [{ required: true, message: "根据客户信息带出" }],
  grainVariety: [{ required: true, message: "请选择粮食品种" }],
  grainSourceType: [{ required: true, message: "请选择粮源类型" }],
  planWeight: [
    { required: true, message: "请输入预计入库量" },
    { pattern: /^\d+$/, message: "请输入非负整数（0及以上）", trigger: "blur" },
  ],
  //completedWeight: [{ required: true, message: "数据自动回显" }],
};

//计算属性
const billStatusOptions = ref([
  { label: "进行中", value: 1 },
  { label: "已完成", value: 2 },
  { label: "已关闭", value: 3 },
]);

//数字转化为文字
const formattedBillStatus = computed(() => {
  if (!formObj.value.state) return '';
  const matchItem = billStatusOptions.value.find(item => item.value === formObj.value.state);
  return matchItem ? matchItem.label : formObj.value.state;
});

//获取客户名称
const loadCustomerOptions = async (searchName = "") => {
  try {
    const res = await getList({ customerType: 0, name: searchName, card: "" });
    customerList.value = res?.result?.data || [];
    customerOptions.value = (res?.result?.data || []).map(item => ({ label: item.name, value: item.id }));
  } catch (err) {
    MessagePlugin.error("客户加载失败");
    customerOptions.value = [];
  }
};

//获取仓库类型
const loadWarehouseOptions = async (searchName = "") => {
  try {
    const res = await getWarehouseList({ name: searchName });
    warehouseList.value = res?.result?.data || [];
    warehouseOptions.value = (res?.result?.data || []).map(item => ({ label: item.warehouseName, value: item.id }));
  } catch (err) {
    MessagePlugin.error("仓库加载失败");
    warehouseOptions.value = [];
  }
};

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
//粮源类型数据字典
const getgrainSourceTypeOptions = () => {
  getDict("GRAIN_SOURCE_TYPE")
    .then((res) => {
      const data = res.result.listDictData || [];
      grainSourceTypeOptions.value = data.map((item) => ({
        label: item.label,
        value: item.description,
      }));
    })
    .catch((error) => {
      console.error("加载粮源类型字典失败：", error);
      MessagePlugin.warning("粮源类型字典加载失败，请刷新页面");
    });
};

const handleSubmit = async () => {
  if (props.type === "查看") {
    emit("update:visible", false);
    return;
  }
  const result = await formRef.value.validate();
  if (result === true) {
    let data = JSON.parse(JSON.stringify(formObj.value));
    if (props.type === "新增") {
      let response = await insert(data);

      if (response.status.code != "200") {
        MessagePlugin.error(response.status.message || "新增失败");
        return;
      }

      MessagePlugin.success("新增成功");
      emit("submit");
      emit("update:visible", false);
    } else {
      emit("update:visible", false);
    }
  }
};

const handleVisibleChange = (value) => {
  if (!value) {
    if (formRef.value) {
      formRef.value.reset();
    }
    formObj.value = {};
  }
  emit("update:visible", value);
};

watch(
  () => props.visible,
  (newVal) => {
    if (!newVal) {
      // 重置表单
      if (formRef.value) {
        formRef.value.reset();
      }
      formObj.value = {};
    } else if (newVal && props.type === "新增") {
      nextTick(() => {
        //startTimer();
      });
    } else {
      //stopTimer();
    }
  },
  { immediate: true }
);

// 自动回显电话
watch(
  () => formObj.value.customerId,
  (customerId) => {
    if (!customerId) {
      formObj.value.phone = "";
      return;
    }
    const selectedCustomer = customerList.value.find(item => item.id === customerId);
    formObj.value.phone = selectedCustomer?.phone || "";
  },
  { immediate: true }
);

//自动回显仓库相关数据
watch(
  () => formObj.value.warehouseId,
  async (warehouseId) => {
    if (!warehouseId) {
      formObj.value.keeper = "";
      formObj.value.grainVariety = "";
      formObj.value.grainGrade = "";
      formObj.value.grainYear = "";
      return;
    }
    try {
      const res = await getWarehouseById(warehouseId);
      if (res.status.code === "200") {
        const warehouseInfo = res.result;
        formObj.value.keeper = warehouseInfo.keeper || "";
        formObj.value.grainVariety = warehouseInfo.grainVarietyLabel || "";
        formObj.value.grainGrade = warehouseInfo.grainGradeLabel || "";
        formObj.value.grainYear = warehouseInfo.grainYearLabel || "";
      }
    } catch (err) {
      MessagePlugin.error("获取仓库详情失败");
    }
  },
  { immediate: true }
);
const getDetail = async () => {
  let response = await detail(props.id);
  console.log("后端返回详情数据：", response.result);
  if (response.status.code != "200") {
    MessagePlugin.error(response.status.message || "获取详情失败");
    return;
  }

  formObj.value = response.result || {};
  formObj.value.customerId = customerList.value.find(item => item.name === formObj.value.name)?.id || "";
  formObj.value.warehouseId = warehouseList.value.find(item => item.warehouseName === formObj.value.warehouseName)?.id || "";

};

const init = async () => {
  disabled.value = props.type === "查看";
  await loadCustomerOptions();
  await loadWarehouseOptions();
  await new Promise((resolve) => {
    getgrainTypeOptions();
    getgrainSourceTypeOptions();
    resolve();
  });

  if (props.type === "新增") {
    try {
      const res = await getInfo();
      if (res.status?.success && res.status.code === "200") {
        formObj.value.addUser = res.result.user.name;
      } else {
        MessagePlugin.warning("获取当前用户信息失败");
      }
    } catch (err) {
      console.error("获取用户信息异常：", err);
    }
  }
  if (props.type === "查看") {
    await getDetail();
  }
};

onMounted(() => {
  init();
});
</script>


<style lang="less" scoped>
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