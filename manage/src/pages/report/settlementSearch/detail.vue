<template>
  <div class="container">
    <div class="content">
      <div class="title-back">
        <div class="title">
          <img
            src="@/assets/receipt/icon10.png"
            alt=""
          >
          结算信息
        </div>
        <div class="back-btn">
          <t-button
            @click="router.go(-1)"
            class="reset-btn my-icon-btn"
            variant="outline"
            theme="primary"
          >
            <rollback-icon slot="icon" />
            返回
          </t-button>
        </div>
      </div>

      <div class="forNewclass">
        <t-form
          :data="formObj"
          ref="formRef"
          class="info-form"
          labelAlign="top"
        >
          <!-- 入库详情 -->
          <template v-if="jobType == 1">
            <t-row
              class="form-row"
              :gutter="60"
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
                    readonly
                    v-model="formObj.jobNo"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="作业类型"
                  name="jobType"
                >
                  <t-input
                    readonly
                    v-model="formObj.jobType"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="客户名称"
                  name="customerName"
                >
                  <t-input
                    readonly
                    v-model="formObj.customerName"
                  ></t-input>
                </t-form-item>
              </t-col>
            </t-row>

            <t-row
              class="form-row"
              :gutter="60"
            >
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="毛重(千克)"
                  name="grossWeight"
                >
                  <t-input
                    readonly
                    v-model="formObj.grossWeight"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="皮重"
                  name="tareWeight"
                >
                  <t-input
                    readonly
                    v-model="formObj.tareWeight"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="承运人"
                  name="carrier"
                >
                  <t-input
                    readonly
                    v-model="formObj.carrier"
                  ></t-input>
                </t-form-item>
              </t-col>
            </t-row>

            <t-row
              class="form-row"
              :gutter="60"
            >
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="每千克扣量"
                  name="deductionPerKg"
                >
                  <t-input
                    readonly
                    v-model="formObj.deductionPerKg"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="值仓现场扣重"
                  name="onSiteDeduction"
                >
                  <t-input
                    readonly
                    v-model="formObj.onSiteDeduction"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="净重"
                  name="netWeight"
                >
                  <t-input
                    readonly
                    v-model="formObj.netWeight"
                  ></t-input>
                </t-form-item>
              </t-col>
            </t-row>

            <t-row
              class="form-row"
              :gutter="60"
            >
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="标准单价"
                  name="standardUnitPrice"
                >
                  <t-input
                    readonly
                    v-model="formObj.standardUnitPrice"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="每千克扣价"
                  name="deductionPricePerKg"
                >
                  <t-input
                    readonly
                    v-model="formObj.deductionPricePerKg"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="扣量合计"
                  name="totalDeductionWeightWeight"
                >
                  <t-input
                    readonly
                    v-model="formObj.totalDeductionWeight"
                  ></t-input>
                </t-form-item>
              </t-col>
            </t-row>

            <t-row
              class="form-row"
              :gutter="60"
            >
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="结算重量"
                  name="settleWeight"
                >
                  <t-input
                    readonly
                    v-model="formObj.settleWeight"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="结算金额"
                  name="settleAmount"
                >
                  <t-input
                    readonly
                    v-model="formObj.settleAmount"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="结算单价"
                  name="settleUnitPrice"
                >
                  <t-input
                    readonly
                    v-model="formObj.settleUnitPrice"
                  ></t-input>
                </t-form-item>
              </t-col>
            </t-row>

            <t-row
              class="form-row"
              :gutter="60"
            >
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="账户身份证"
                  name="accountIdCard"
                >
                  <t-input
                    readonly
                    v-model="formObj.accountIdCard"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="账户联系方式"
                  name="accountPhone"
                >
                  <t-input
                    readonly
                    v-model="formObj.accountPhone"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="开户银行"
                  name="bankName"
                >
                  <t-input
                    readonly
                    v-model="formObj.bankName"
                  ></t-input>
                </t-form-item>
              </t-col>
            </t-row>

            <t-row
              class="form-row"
              :gutter="60"
            >

              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="开户行号"
                  name="bankCode"
                >
                  <t-input
                    readonly
                    v-model="formObj.bankCode"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="开户账号"
                  name="bankAccount"
                >
                  <t-input
                    readonly
                    v-model="formObj.bankAccount"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="结算时间"
                  name="settleTime"
                >
                  <t-input
                    readonly
                    v-model="formObj.settleTime"
                  ></t-input>
                </t-form-item>
              </t-col>
            </t-row>
          </template>

          <!-- 出库详情 -->
          <template v-else>
            <t-row
              class="form-row"
              :gutter="60"
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
                    readonly
                    v-model="formObj.jobNo"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="作业类型"
                  name="jobType"
                >
                  <t-input
                    readonly
                    v-model="formObj.jobType"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="客户名称"
                  name="customerName"
                >
                  <t-input
                    readonly
                    v-model="formObj.customerName"
                  ></t-input>
                </t-form-item>
              </t-col>
            </t-row>

            <t-row
              class="form-row"
              :gutter="60"
            >
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="毛重(千克)"
                  name="grossWeight"
                >
                  <t-input
                    readonly
                    v-model="formObj.grossWeight"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="皮重"
                  name="tareWeight"
                >
                  <t-input
                    readonly
                    v-model="formObj.tareWeight"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="承运人"
                  name="carrier"
                >
                  <t-input
                    readonly
                    v-model="formObj.carrier"
                  ></t-input>
                </t-form-item>
              </t-col>
            </t-row>

            <t-row
              class="form-row"
              :gutter="60"
            >
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="标准单价"
                  name="standardUnitPrice"
                >
                  <t-input
                    readonly
                    v-model="formObj.standardUnitPrice"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="结算单价"
                  name="settleUnitPrice"
                >
                  <t-input
                    readonly
                    v-model="formObj.settleUnitPrice"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="净重"
                  name="netWeight"
                >
                  <t-input
                    readonly
                    v-model="formObj.netWeight"
                  ></t-input>
                </t-form-item>
              </t-col>
            </t-row>

            <t-row
              class="form-row"
              :gutter="60"
            >
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="结算重量"
                  name="settleWeight"
                >
                  <t-input
                    readonly
                    v-model="formObj.settleWeight"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="结算金额"
                  name="settleAmount"
                >
                  <t-input
                    readonly
                    v-model="formObj.settleAmount"
                  ></t-input>
                </t-form-item>
              </t-col>
              <t-col
                :span="4"
                class="form-col"
              >
                <t-form-item
                  label="结算时间"
                  name="settleTime"
                >
                  <t-input
                    readonly
                    v-model="formObj.settleTime"
                  ></t-input>
                </t-form-item>
              </t-col>
            </t-row>
          </template>
        </t-form>
      </div>
    </div>
  </div>
</template>

<script setup name="settlementDetail">
import { ref, onMounted } from "vue";
import { MessagePlugin } from "tdesign-vue-next";
import { RollbackIcon } from "tdesign-icons-vue-next";
import { getDetail } from "@/api/settlementSearch.ts";
import { useRoute } from "vue-router";
import router from "@/router";

const route = useRoute();
const formObj = ref({});
const jobType = ref("");

// 获取详情页数据
const getDetailData = async () => {
  try {
    const id = route.query.id;
    const type = route.query.jobType;

    if (!id || !type) {
      MessagePlugin.error("参数错误");
      return;
    }

    jobType.value = type;

    console.log("11111111111111111");

    const response = await getDetail(id);

    console.log("22222222222");

    console.log("🌷🌷 ~ response:", response);
    // if (response.status.code !== "200") {
    //   throw new Error(response.status.message || "获取详情失败");
    // }

    formObj.value = response.result || {};
  } catch (error) {
    MessagePlugin.error(error.message);
  }
};

// 生命周期
onMounted(() => {
  getDetailData();
});
</script>

<style lang="less" scoped>
.container {
  padding: 15px;
  background-color: #fff;
  border-radius: 4px;

  .content {
    .title-back {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 30px;
      padding-bottom: 15px;
      border-bottom: 1px solid #f0f0f0;

      .title {
        display: flex;
        align-items: center;
        font-size: 18px;
        font-weight: bold;
        color: #000;

        img {
          width: 24px;
          height: 24px;
          margin-right: 10px;
        }
      }

      .back-btn {
        display: flex;
        gap: 10px;
      }
    }
  }
}

.forNewclass {
  ::v-deep .t-form {
    width: 100%;

    .form-row {
      margin-bottom: 30px;
      display: flex;
      gap: 16px;

      .form-col {
        flex: 1;
      }
    }

    .t-form__item {
      margin-bottom: 0;
    }

    .t-form__label {
      font-size: 14px;
      margin-bottom: 8px;
    }
  }
}
</style>

<style lang="less">
.reset-btn.t-button--theme-primary.t-button--variant-outline {
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

.my-icon-btn .t-button__text {
  display: flex;
  justify-content: center;
  align-items: center;

  .t-icon {
    margin-right: 6px;
  }
}
</style>
