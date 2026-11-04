<template>
  <t-dialog :visible="visible" @update:visible="handleVisibleChange" :header='`${type}仓库`' width="860px"
    class="newDialog newclassNewDialog" :cancelBtn="type === '查看' ? null : { theme: 'default', variant: 'outline' }"
    :confirmBtn="type === '查看' ? null : { theme: 'warning', variant: 'base' }" :confirm-on-enter="true"
    :on-confirm="handleSubmit">
    <div class="forNewclass ">
      <t-form :data="formObj" :rules="type === '查看' ? {} : rules" ref="formRef" :labelWidth="110" class="create-form">
        <t-row class="form-row" :gutter="65">
          <t-col :span="6" class="form-col">
            <t-form-item label="仓库编码" name="id" :required-mark="type === '新增' || type === '编辑'" class="required-label">
              <t-input disabled v-model="formObj.id" placeholder="系统自动生成"></t-input>
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="仓库名称" name="warehouseName">
              <t-input :disabled="disabled" v-model="formObj.warehouseName" placeholder="请输入仓库名称"></t-input>
            </t-form-item>
          </t-col>
        </t-row>

        <t-row class="form-row" :gutter="65">
          <t-col :span="6" class="form-col">
            <t-form-item label="仓库类型" name="warehouseType">
              <t-select :disabled="disabled" v-model="formObj.warehouseType" :options="warehouseTypeOptions"
                placeholder="请选择仓库类型"></t-select>
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="库管员" name="keeper">
              <t-select :disabled="disabled" v-model="formObj.keeper" :options="managerOptions" placeholder="请选择库管员"
                @filter="loadUserOptions"></t-select>
            </t-form-item>
          </t-col>
        </t-row>

        <t-row class="form-row" :gutter="65" v-if="type === '编辑' || type === '查看'">
          <t-col :span="6" class="form-col">
            <t-form-item label="粮食年期" name="grainYear" :required-mark="type === '新增' || type === '编辑'">
              <t-input disabled v-model="formObj.grainYear" placeholder="粮食年期"></t-input>
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="粮食等级" name="grainGrade" :required-mark="type === '新增' || type === '编辑'">
              <t-input disabled v-model="formObj.grainGrade" placeholder="粮食等级"></t-input>
            </t-form-item>
          </t-col>
        </t-row>

        <t-row class="form-row" :gutter="65">
          <t-col :span="6" class="form-col">
            <t-form-item label="当前容量(KG)" name="currentWeight">
              <t-input :disabled="type === '编辑' || type === '查看'" v-model="formObj.currentWeight" placeholder="请输入当前容量"
                type="number"></t-input>
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="仓库容量(KG)" name="maxWeight">
              <t-input :disabled="disabled" v-model="formObj.maxWeight" placeholder="请输入仓库容量" type="number"></t-input>
            </t-form-item>
          </t-col>
        </t-row>

        <t-row class="form-row" :gutter="65">
          <t-col :span="6" class="form-col">
            <t-form-item label="启用状态" name="state">
              <t-select :disabled="type === '编辑' || type === '查看'" v-model="formObj.state" :options="statusOptions"
                placeholder="请选择启用状态"></t-select>
            </t-form-item>
          </t-col>
          <t-col :span="6" class="form-col">
            <t-form-item label="粮食品种" name="grainVariety">
              <t-select :disabled="type === '编辑' || type === '查看'" v-model="formObj.grainVariety"
                :options="grainTypeOptions" placeholder="请选择粮食品种"></t-select>
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
  defineComponent,
  defineExpose,
  getCurrentInstance,
  onMounted,
  watch,
  nextTick,
} from "vue";
import { insert, detail, update } from "@/api/warehouse";
import { getDict } from "@/api/sampling.ts";
import { listUser } from "@/api/user";


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

const grainTypeOptions = ref([]);
const managerOptions = ref([]);
const disabled = ref(false);
const formObj = ref({});
const formRef = ref();
const rules = {
  warehouseName: [{ required: true, message: "请输入仓库名称" }],
  warehouseType: [{ required: true, message: "请选择仓库类型" }],
  keeper: [{ required: true, message: "请选择库管员" }],
  //grainYear: [{ required: true, message: "数据自动回显" }],
  //grainGrade: [{ required: true, message: "数据自动回显" }],
  currentWeight: [
    { required: true, message: "请输入当前容量" },
    { pattern: /^\d+$/, message: "请输入非负整数（0及以上）", trigger: "blur" },
  ],
  maxWeight: [
    { required: true, message: "请输入仓库容量" },
    { pattern: /^\d+$/, message: "请输入非负整数（0及以上）", trigger: "blur" },
  ],
  grainVariety: [{ required: true, message: "请选择粮食品种" }],

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

// 仓库类型选项
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

const loadUserOptions = async (searchName = "") => {
  try {
    const res = await listUser({ pageNo: 1, pageSize: 10, name: searchName });
    managerOptions.value = (res?.result?.data || []).map(item => ({ label: item.name, value: item.id }));
  } catch (err) {
    MessagePlugin.error("所属组织加载失败");
    managerOptions.value = [];
  }
};



const handleSubmit = async () => {
  const result = await formRef.value.validate();
  console.log("表单校验结果：", result);
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
      });
    } else {
    }
  },
  { immediate: true }
);


// const getDetail = async () => {
//   await loadUserOptions();
//   let response = await detail(props.id);

//   if (response.status.code != "200") {
//     MessagePlugin.error(response.status.message || "获取详情失败");
//     return;
//   }
//   //
//   await nextTick();
//   formObj.value = response.result || {};
// };

const getDetail = async () => {
  await loadUserOptions();
  const response = await detail(props.id);

  if (response.status.code !== "200") {
    MessagePlugin.error(response.status.message || "获取详情失败");
    return;
  }

  await nextTick();
  const result = response.result || {};
  let keeperValue = result.keeper ?? '';

  let matchOption;
  if (keeperValue) {
    matchOption = managerOptions.value.find(item => item.label === keeperValue);
  }

  if (matchOption) {
    formObj.value = { ...result, keeper: matchOption.value };
  }
};

const init = () => {
  disabled.value = props.type === "查看" ? true : false;
  if (props.type === "查看" || props.type === "编辑") {
    getDetail();
  }
};


onMounted(() => {
  init();
  getgrainTypeOptions();
  loadUserOptions();
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