<template>
  <t-dialog :visible="visible" @update:visible="handleVisibleChange" width="600px" class="newDialog newclassNewDialog"
    :cancelBtn="{ theme: 'default', variant: 'outline' }" :confirmBtn="{ theme: 'warning', variant: 'base' }"
    :on-confirm="handleConfirm">
    <template #header>
      <div class="dialog-header">
        <span>审批</span>
      </div>
    </template>

    <div class="forNewclass">
      <t-form :data="formObj" :rules="rules" ref="formRef" :labelWidth="100" class="approval-form">
        <t-form-item label="审批意见" name="opinion">
          <t-textarea v-model="formObj.opinion" :maxlength="200" show-limit-number placeholder="请输入审批意见" />
        </t-form-item>
        <t-form-item label="审批结果" name="result">
          <t-radio-group v-model="formObj.result">
            <t-radio value="通过">通过</t-radio>
            <t-radio value="驳回">驳回</t-radio>
          </t-radio-group>
        </t-form-item>
      </t-form>
    </div>
  </t-dialog>
</template>

<script setup name="ApprovalDialog">
import { ref, watch, nextTick } from "vue";
import { MessagePlugin } from "tdesign-vue-next";
import { auditTransferStorage } from "@/api/warehouseTransfer";

const emit = defineEmits(["submit", "update:visible"]);

const props = defineProps({
  visible: {
    type: Boolean,
    default: false,
  },
  rowData: {
    type: Object,
    default: () => ({}),
  },
});

const formRef = ref();

const formObj = ref({
  opinion: "",
  result: "通过",
});

const rules = {
  result: [{ required: true, message: "请选择审批结果" }],
};

const getDefaultForm = () => ({
  opinion: "",
  result: "通过",
});

const handleConfirm = async () => {
  const result = await formRef.value.validate();
  if (result === true) {
    const status = formObj.value.result === "通过" ? 5 : 3;
    const response = await auditTransferStorage({
      transferId: props.rowData?.id,
      status,
      approvalOpinion: formObj.value.opinion,
      statusValid: true,
    });
    if (response?.status?.code !== "200") {
      MessagePlugin.error(response?.status?.message || "审批提交失败");
      return;
    }
    MessagePlugin.success("审批提交成功");
    emit("submit", { ...formObj.value, id: props.rowData?.id });
    emit("update:visible", false);
  }
};

const handleVisibleChange = (value) => {
  if (!value) {
    formObj.value = getDefaultForm();
    if (formRef.value) {
      formRef.value.clearValidate();
    }
  }
  emit("update:visible", value);
};

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      nextTick(() => {
        formObj.value = getDefaultForm();
        nextTick(() => {
          if (formRef.value) {
            formRef.value.clearValidate();
          }
        });
      });
    }
  }
);
</script>

<style lang="less" scoped>
.dialog-header {
  display: flex;
  align-items: center;
}

::v-deep .approval-form {
  width: 100%;
  padding-top: 20px;
  border-top: 1px solid rgba(238, 239, 242, 1);
  border-bottom: 1px solid rgba(238, 239, 242, 1);

  .t-textarea__inner {
    font-size: 13px;
    font-family: PingFang SC;
    min-height: 80px;
  }

  .t-form__item {
    margin-bottom: 24px;
  }
}
</style>
