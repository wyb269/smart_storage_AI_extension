<template>
  <t-dialog
    :visible="visible"
    @update:visible="handleVisibleChange"
    :header='`${type}客户`'
    width="860px"
    class="newDialog newclassNewDialog"
    :cancelBtn="type === '查看' ? null : { theme: 'default', variant: 'outline' }"
    :confirmBtn="type === '查看' ? null : { theme: 'warning', variant: 'base' }"
    :confirm-on-enter="true"
    :on-confirm="handleSubmit"
  >
    <div class="forNewclass ">
      <t-form
        :data="formObj"
        :rules="type === '查看' ? {} : rules"
        ref="formRef"
        :labelWidth="110"
        class="create-form"
      >
        <t-row
          class="form-row"
          :gutter="65"
        >
          <t-col
            :span="6"
            class="form-col"
          >
            <t-form-item
              label="客户类型"
              name="customerType"
            >
              <t-select
                :disabled="disabled"
                v-model="formObj.customerType"
                :options="customerTypeOptions"
                placeholder="请选择客户类型"
              ></t-select>
            </t-form-item>
          </t-col>
          <t-col
            :span="6"
            class="form-col"
          >
            <t-form-item
              label="客户名称"
              name="name"
            >
              <t-input
                :disabled="disabled"
                v-model="formObj.name"
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
            :span="6"
            class="form-col"
          >
            <t-form-item
              label="身份证号"
              name="card"
            >
              <t-input
                :disabled="disabled"
                v-model="formObj.card"
                placeholder="请输入身份证号"
              ></t-input>
            </t-form-item>
          </t-col>
          <t-col
            :span="6"
            class="form-col"
          >
            <t-form-item
              label="联系电话"
              name="phone"
            >
              <t-input
                :disabled="disabled"
                v-model="formObj.phone"
                placeholder="请输入联系电话"
              ></t-input>
            </t-form-item>
          </t-col>
        </t-row>

        <t-row
          class="form-row"
          :gutter="65"
        >
          <t-col
            :span="6"
            class="form-col"
          >
            <t-form-item
              label="联系人居住地址"
              name="address"
            >
              <t-input
                :disabled="disabled"
                v-model="formObj.address"
                placeholder="请输入联系人居住地址"
              ></t-input>
            </t-form-item>
          </t-col>
          <t-col
            :span="6"
            class="form-col"
          >
            <t-form-item
              label="开户银行"
              name="bankName"
            >
              <t-input
                :disabled="disabled"
                v-model="formObj.bankName"
                placeholder="请输入开户银行"
              ></t-input>
            </t-form-item>
          </t-col>
        </t-row>

        <t-row
          class="form-row"
          :gutter="65"
        >
          <t-col
            :span="6"
            class="form-col"
          >
            <t-form-item
              label="开户行号"
              name="bankCode"
            >
              <t-input
                :disabled="disabled"
                v-model="formObj.bankCode"
                placeholder="请输入开户行号"
              ></t-input>
            </t-form-item>
          </t-col>
          <t-col
            :span="6"
            class="form-col"
          >
            <t-form-item
              label="银行卡号"
              name="bankAccount"
            >
              <t-input
                :disabled="disabled"
                v-model="formObj.bankAccount"
                placeholder="请输入银行卡号"
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
            >
              <t-textarea
                :disabled="disabled"
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
  </t-dialog>
</template>

<script setup name="Create">
import { MessagePlugin } from "tdesign-vue-next";
import {
  ref,
  defineComponent,
  defineExpose,
  getCurrentInstance,
  onMounted,
  watch,
  nextTick,
} from "vue";
import dayjs from "dayjs";
import { insert, update, detail } from "@/api/customer.ts";

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

const disabled = ref(false);
const formObj = ref({});
const formRef = ref();
const rules = {
  customerType: [{ required: true, message: "请选择客户类型" }],
  name: [{ required: true, message: "请输入客户名称" }],
  card: [
    { required: true, message: "请选择身份证号" },
    {
      pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/,
      message: "请输入正确的身份证号",
      trigger: "blur",
    },
  ],
  phone: [
    { required: true, message: "请选择联系电话" },
    {
      validator: (value) => {
        // 手机号正则：1开头的11位数字
        const mobileRegex = /^1[3-9]\d{9}$/;
        // 座机号正则：区号3-4位，号码7-8位，中间可有横线
        const landlineRegex = /^(0\d{2,3}-?)?\d{7,8}$/;

        return mobileRegex.test(value) || landlineRegex.test(value);
      },
      message: "请输入正确的手机号或座机号",
      trigger: "blur",
    },
  ],
  address: [{ required: true, message: "请输入联系人居住地址" }],
  bankName: [{ required: true, message: "请输入开户银行" }],
  bankAccount: [
    { required: true, message: "请输入银行卡号" },
    {
      pattern: /^\d{16,19}$/,
      message: "请输入16-19位银行卡号",
      trigger: "blur",
    },
  ],
  bankCode: [{ required: true, message: "请输入开户行号" }],
};

// 客户类型选项
const customerTypeOptions = [
  { label: "个人", value: 1 },
  { label: "企业", value: 2 },
];

let timer = null; // 定时器引用

const handleSubmit = async () => {
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
    } else if (props.type === "编辑") {
      let response = await update(data);

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

const handleVisibleChange = (value) => {
  if (!value) {
    if (formRef.value) {
      formRef.value.reset();
    }
    formObj.value = {};
  }
  emit("update:visible", value);
};

const updateCurrentTime = () => {
  formObj.value.addTime = dayjs().format("YYYY-MM-DD HH:mm:ss");
};

const startTimer = () => {
  if (timer) {
    clearInterval(timer);
  }

  updateCurrentTime();

  timer = setInterval(() => {
    updateCurrentTime();
  }, 1000);
};

const stopTimer = () => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
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
        startTimer();
      });
    } else {
      stopTimer();
    }
  },
  { immediate: true }
);

const getDetail = async () => {
  let response = await detail(props.id);

  if (response.status.code != "200") {
    MessagePlugin.error(response.status.message || "获取详情失败");
    return;
  }

  formObj.value = response.result || {};
};

const init = () => {
  disabled.value = props.type === "查看" ? true : false;
  if (props.type === "查看" || props.type === "编辑") {
    getDetail();
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