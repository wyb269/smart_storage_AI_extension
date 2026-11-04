<template>
  <div class="container">
    <div class="top">
      <div class="left">
        <div class="title-box">
          <div class="title">
            <img
              src="@/assets/sampling/icon1.png"
              alt=""
            >
            扦样信息
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
                  label="扦样员"
                  name="sampler"
                >
                  <t-input
                    disabled
                    v-model="formObj.sampler"
                    placeholder="请输入扦样员"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="扦样方式"
                  name="sampleType"
                >
                  <t-select
                    disabled
                    v-model="formObj.sampleType"
                    :options="sampleTypeOptions"
                    placeholder="请选择扦样方式"
                  ></t-select>
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
                    v-model="formObj.remarks"
                    :maxlength="160"
                    show-limit-number
                    placeholder="请输入备注，限制长度160"
                  ></t-textarea>
                </t-form-item>
              </t-col>
            </t-row>
          </t-form>

          <div class="btn-box">
            <t-button
              class="left-button"
              @click="handleSubmit"
              style="width: 28%;"
            >
              <template #icon>
                <img
                  src="@/assets/sampling/icon3.png"
                  class="button-icon"
                >
              </template>
              确认扦样
            </t-button>
          </div>

        </div>

      </div>
      <div class="right">
        <div class="title-box">
          <div class="title">
            <img
              src="@/assets/sampling/icon4.png"
              alt=""
            >
            扦样实时画面
          </div>
        </div>
        <div class="empty-content">
          <t-empty>
            <template #image>
              <img
                src="@/assets/sampling/empty.png"
                style="width: 120px; object-fit: contain;"
              >
            </template>
          </t-empty>
        </div>
      </div>
    </div>
    <div class="bottom">
      <div class="title-box">
        <div class="title">
          <img
            src="@/assets/sampling/icon5.png"
            alt=""
          >
          今日扦样记录
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
                <div style="font-family: PingFang SC;font-weight: 300;font-style: Regular;font-size: 13px;line-height: 100%;letter-spacing: 0px;color:rgba(149, 149, 149, 1);text-align: center;">暂无数据</div>
              </div>
            </t-space>
          </template>
        </t-table>
      </div>

    </div>
  </div>
</template>

<script setup name="sampling">
import { computed, onMounted, ref } from "vue";
import { MessagePlugin } from "tdesign-vue-next";
import readCard from "@/components/readCard/index.vue";
import {
  getList,
  insert,
  getDict,
  swipeCard,
  getTaskById,
  getWarehouseInOrderByTaskId,
} from "@/api/sampling.ts";
import { useSettingStore, useUserStore } from "@/store";

const user = useUserStore();

const status = ref("待扦样");

const formObj = ref({});
const formRef = ref(null);

const rules = {
  jobNo: [{ required: true, message: "请输入作业编号" }],
  sampleType: [{ required: true, message: "请选择扦样方式" }],
  grainVariety: [{ required: true, message: "请选择粮食品种" }],
};

// 扦样方式选项
const sampleTypeOptions = [{ label: "人工", value: 1 }];

// 粮食品种选项
const grainVarietyOptions = ref([]);

// 表格列定义
const loading = ref(false);
const tableColumns = [
  { colKey: "serial-number", width: 60, title: "序号" },
  { colKey: "cardNo", title: "智能卡编号" },
  { colKey: "id", title: "扦样单编号" },
  { colKey: "grainVariety", title: "粮食品种" },
  { colKey: "carrierName", title: "承运人" },
  { colKey: "licensePlate", title: "车牌号" },
  { colKey: "addTime", title: "扦样时间" },
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

  // 然后设置初始值
  formObj.value = {
    sampler: user.userInfo.user.name || "",
    sampleType: 1,
  };

  if (r.result == 1) {
    let response = await swipeCard({
      physicalCardNumber: r.data,
      processStep: "sampled",
    });
    if (response.status.code != "200") {
      MessagePlugin.error(response.status.message || "读卡失败");
      return;
    }
    if (
      response.result.jobStatus == "logIn" &&
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

      if (response.result.taskId) {
        let res2 = await getWarehouseInOrderByTaskId(response.result.taskId);
        if (res2.status.code != "200") {
          MessagePlugin.error(res2.status.message || "获取粮食品种失败");
          return;
        }
        formObj.value.grainVariety = res2.result.grainVariety;
        console.log(formObj.value.grainVariety);
      }
    } else {
      MessagePlugin.error("当前作业状态不允许扦样");
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
        grainVarietyOptions.value.find((v) => v.value === item.grainVariety)
          ?.label || "";
    });
    tableData.value = response.result.data || [];
    pagination.value.total = response.result.total || 0;
  } catch (error) {
  } finally {
    loading.value = false;
  }
};

const handleSubmit = async () => {
  const result = await formRef.value.validate();
  if (result === true) {
    let data = JSON.parse(JSON.stringify(formObj.value));
    let form = {
      taskId: data.taskId,
      grainVariety: data.grainVariety,
      sampleType: data.sampleType,
      remarks: data.remarks || "",
    };

    let response = await insert(form);

    if (response.status.code != "200") {
      MessagePlugin.error(response.status.message || "扦样信息提交失败");
      return;
    }

    MessagePlugin.success("扦样信息提交成功");
    getTableData();

    // 使用 reset 方法重置表单
    formRef.value?.reset();

    // 设置默认值
    formObj.value = {
      sampler: user.userInfo.user.name || "",
      sampleType: 1,
    };
  }
};

const getGrainVarietyOptions = () => {
  getDict("GRAIN_VARIETY")
    .then((res) => {
      const data = res.result.listDictData || [];
      grainVarietyOptions.value = data.map((item) => ({
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
  formObj.value.sampleType = 1; // 默认人工扦样
  formObj.value.sampler = user.userInfo.user.name || "";
  getGrainVarietyOptions();
  getTableData();
});
</script>


<style lang="less" scoped>
.page-container {
  width: 100%;
}

.empty-content {
  flex: 1;
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;

  ::v-deep .t-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
  }

  ::v-deep .t-empty__title {
    font-family: PingFang SC !important;
    font-size: 14px;
  }
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
  width: 17px;
  object-fit: contain;
  margin-right: 10px;
}

.top {
  width: 100%;
  display: flex;
  justify-content: space-between;
  gap: 25px;
  margin-bottom: 30px;
  flex: 1 0 auto;

  .tag {
    padding: 0 14px;
    border-radius: 4px;
    background-color: #edf1f7;
    font-size: 12px;
    color: #186df5;
  }

  .left {
    width: 65%;

    .btn-box {
      width: 100%;
      display: flex;
      justify-content: center;
      margin-top: 26px;
    }

    .left-button {
      margin-bottom: 5px;
      background: linear-gradient(90deg, #f59e0b 0%, #d97706 100%) !important;
      color: #fff !important;
      border: none !important;
      font-size: 14px;
      font-family: PingFang SC;
      height: 36px;

      &.t-is-disabled {
        background: #f8fafc !important;
        color: #959595 !important;
        border: none !important;
      }

      &:not(.t-is-disabled):not(.t-button--ghost) {
        --ripple-color: var(--td-warning-color-active);
      }
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
      }
    }
  }

  .right {
    flex: 1;
    display: flex;
    flex-direction: column;

    .empty-content {
      flex: 1;
    }
  }
}

.bottom {
  width: 100%;
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
}
</style>