<template>
  <div class="logout-page-container">
    <div class="top-main-container">
      <div class="fixed-tabs">
        <div class="tab-header-row">
          <t-tabs v-model="innerTab" @change="handleInnerTabChange" class="inner-form-tabs" tab-style="card">
            <t-tab-panel value="register" label="登记"></t-tab-panel>
            <t-tab-panel value="logout" label="登出"></t-tab-panel>
          </t-tabs>
          <div class="read-card-wrapper">
            <readCard @result="handleLogoutCardHardwareResult" />
          </div>
        </div>
        <div class="logout-page-content">
          <div class="left-section">
            <div class="forNewclass">
              <t-form ref="formRef" :data="logoutForm" labelAlign="top" labelWidth="70px">
                <t-row :gutter="[65, 12]">
                  <t-col :span="4">
                    <t-form-item label="智能卡编号" name="cardNo">
                      <t-input v-model="logoutForm.cardNo" disabled placeholder="智能卡编号" style="width: 100%;" />
                    </t-form-item>
                  </t-col>
                  <t-col :span="4">
                    <t-form-item label="承运人" name="carrier">
                      <t-input v-model="logoutForm.carrier" disabled placeholder="承运人" style="width: 100%;" />
                    </t-form-item>
                  </t-col>
                  <t-col :span="4">
                    <t-form-item label="车牌号" name="licensePlate">
                      <t-input v-model="logoutForm.licensePlate" disabled placeholder="车牌号" style="width: 100%;" />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row :gutter="[65, 12]">
                  <t-col :span="12">
                    <t-form-item label="客户名称" name="clientName">
                      <t-input v-model="logoutForm.clientName" disabled placeholder="客户名称" style="width: 100%;" />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row :gutter="[65, 12]">
                  <t-col :span="4">
                    <t-form-item name="jobNo">
                      <template #label>
                        <span class="required">*</span>作业编号
                      </template>
                      <t-input v-model="logoutForm.jobNo" readonly style="width: 100%;" />
                    </t-form-item>
                  </t-col>
                  <t-col :span="4">
                    <t-form-item name="jobType">
                      <template #label>
                        <span class="required">*</span>作业类型
                      </template>
                      <t-input v-model="logoutForm.jobType" readonly style="width: 100%;" />
                    </t-form-item>
                  </t-col>
                  <t-col :span="4">
                    <t-form-item name="jobStatus">
                      <template #label>
                        <span class="required">*</span>作业流程状态
                      </template>
                      <t-input v-model="logoutForm.jobStatus" readonly style="width: 100%;" />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row :gutter="[65, 12]">
                  <t-col :span="4">
                    <t-form-item name="registerTime">
                      <template #label>
                        <span class="required">*</span>登记时间
                      </template>
                      <t-input v-model="logoutForm.registerTime" readonly style="width: 100%;" />
                    </t-form-item>
                  </t-col>
                  <t-col :span="4">
                    <t-form-item name="stayTime">
                      <template #label>
                        <span class="required">*</span>停留时长
                      </template>
                      <t-input v-model="logoutForm.stayTime" readonly placeholder="请输入" suffix="分钟"
                        style="width: 100%;" />
                    </t-form-item>
                  </t-col>
                  <t-col :span="4"></t-col>
                </t-row>

                <t-row :gutter="[65, 12]">
                  <t-col :span="4">
                    <t-form-item name="vehiclePhoto">
                      <template #label>
                        <span class="required">*</span>登记车辆照片:
                      </template>
                      <div class="photo-container" @click="previewLogoutPhoto">
                        <div v-if="!logoutForm.vehiclePhoto" class="photo-empty-tip">
                          <t-icon name="camera" class="camera-icon" />
                        </div>
                        <img v-else :src="logoutForm.vehiclePhoto" alt="登出车辆照片" class="vehicle-photo"
                          @error="() => logoutForm.vehiclePhoto = ''" />
                      </div>
                    </t-form-item>
                  </t-col>
                  <t-col :span="8"></t-col>
                </t-row>
              </t-form>
            </div>
          </div>
        </div>
      </div>
      <div class="right-section fixed-card">
        <h3 class="form-subtitle"><img :src="icon4" class="operation-type-icon" />销卡</h3>
        <div class="right-action-section">
          <div class="card-box-wrapper">
            <div class="card-status-box" :class="{ 'success': logoutCardStatus === 'success' }">
              <div class="status-icon-text-wrapper">
                <t-icon name="check-circle" class="status-icon" v-if="logoutCardStatus === 'success'" />
                <span class="status-text">{{ logoutStatusText }}</span>
              </div>
              <div class="card-info">
                <div class="info-item">
                  <span class="label">智能卡编号:</span>
                  <span class="value">{{ logoutForm.cardNo }}</span>
                </div>
                <div class="info-item">
                  <span class="label">智能卡物理编号:</span>
                  <span class="value">{{ logoutForm.physicalCardNo }}</span>
                </div>
              </div>
            </div>
            <t-button theme="primary" variant="base" class="logout-btn" @click="handleLogoutCard"
              :disabled="!logoutForm.cardNo">销卡出库</t-button>
          </div>
        </div>
      </div>
    </div>
    <div class="bottom-section fixed-table">
      <div class="bottom-records-table">
        <h3 class="table-title"><img :src="icon11" class="operation-type-icon" />今日登出记录</h3>
        <CommonTable justList="1" tbMt="0" :columns="logoutTableColumns" :tableData="logoutTableData"
          :pagination="logoutPagination" :loading="logoutLoading" @changePage="handleLogoutPageChange"
          class="forNewClassTableAndsearch" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, defineProps, defineEmits, getCurrentInstance, watch } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';
import readCard from '@/components/readCard/index.vue';
import { queryRegistrationById, queryTodayLogoutRecords, outRegistration } from '@/api/logout';
import { swipeCard } from '@/api/smartcard';
import CommonTable from "@/components/common-table/index.vue"
import icon4 from "@/assets/register/icon4.png"
import icon11 from "@/assets/register/icon11.png"

const { proxy } = getCurrentInstance();

// 图片地址转换方法
const getPreviewPhotoUrl = async (picKey) => {
  if (!picKey) return '';
  if (picKey.startsWith('http')) {
    return picKey;
  }
  try {
    return await proxy.$getPic(picKey);
  } catch (error) {
    console.error('图片地址转换失败', error);
    return '';
  }
};

const props = defineProps({
  currentTab: { type: String, required: true, default: 'logout' }
});

const innerTab = ref(props.currentTab);
const emit = defineEmits(['tab-change']);
const handleInnerTabChange = (targetTab) => {
  innerTab.value = targetTab;
  emit('tab-change', targetTab);
};
const logoutLoading = ref(false);
const logoutCardStatus = ref('init');
const logoutStatusText = ref('待销卡');
const logoutForm = ref({
  cardNo: '',
  physicalCardNo: '',
  carrier: '',
  licensePlate: '',
  clientName: '',
  jobNo: '',
  jobType: '',
  jobStatus: '',
  registerTime: '',
  stayTime: '',
  vehiclePhoto: '',
  idCard: ''
});
const logoutPagination = ref({
  defaultCurrent: 1,
  defaultPageSize: 10,
  total: 0,
  showPageSize: true,
  showPreviousAndNextBtn: true,
});

// 表格列配置
const logoutTableColumns = [
  { colKey: "serial", title: "序号", width: 60, align: "center" },
  { colKey: "cardNo", title: "智能卡编号", width: 120, align: "center" },
  { colKey: "logoutNo", title: "登出单编号", width: 120, align: "center" },
  { colKey: "carrier", title: "承运人", align: "center" },
  { colKey: "licensePlate", title: "车牌号", align: "center" },
  { colKey: "clientName", title: "客户名称", align: "center" },
  { colKey: "jobType", title: "作业类型", align: "center" },
  { colKey: "factoryOutTime", title: "出厂时间", align: "center" },
  { colKey: "status", title: "状态", align: "center" },
];
const logoutTableData = ref([]);

// 硬件读卡回调
const handleLogoutCardHardwareResult = async (r) => {
  console.log('[登出页面 - 硬件读卡返回]', r);
  let physicalCardNo = '';

  if (r.result === '1') {
    try {
      logoutLoading.value = true;
      physicalCardNo = (r.data || '').trim();

      if (!physicalCardNo) {
        MessagePlugin.warning('硬件未读取到有效物理卡号，请重新刷卡');
        logoutLoading.value = false;
        return;
      }

      const swipeCardParams = {
        physicalCardNumber: physicalCardNo,
        processStep: "logout"
      };
      console.log('swipeCard请求参数', JSON.stringify(swipeCardParams));

      const response = await swipeCard(swipeCardParams);
      console.log('[swipeCard 接口返回完整数据]', response);

      const isRequestSuccess = response?.status?.success === true;
      const respData = response?.result;

      if (isRequestSuccess && respData) {
        if (respData.jobType !== 1) {
          MessagePlugin.warning('当前页面仅支持入库作业登出！');
          resetLogoutForm();
          return;
        }
        const baseFormData = {
          cardNo: respData.cardNo || '',
          physicalCardNo: respData.physicalCardNumber || physicalCardNo,
          jobNo: respData.jobNo || '',
          jobType: respData.jobType === 1 ? '入库' : respData.jobType === 2 ? '出库' : '未知',
          jobStatus: respData.jobStatus || '待出门销卡',
          taskId: respData.taskId || '',
          stayTime: respData.stayTime || '60',
          vehiclePhoto: '',
          carrier: '',
          clientName: '',
          licensePlate: '',
          registerTime: '',
          idCard: respData.smartCardId || '',
          contactPhone: ''
        };
        Object.assign(logoutForm.value, baseFormData);

        if (respData.taskId) {
          try {
            const validTaskId = String(respData.taskId).trim();
            console.log('最终传入详情接口的 taskId', validTaskId);

            const detailResponse = await queryRegistrationById(validTaskId);
            console.log('[queryRegistrationById 详情接口返回]', detailResponse);

            const isDetailSuccess = detailResponse?.status?.success === true;
            const detailData = detailResponse?.result;

            if (isDetailSuccess && detailData) {
              const detailFormData = {
                carrier: detailData.carrierName || '暂无数据',
                licensePlate: detailData.licensePlate || '暂无数据',
                clientName: detailData.customerName || '暂无数据',
                jobStatus: mapJobStatusToText(detailData.jobStatus) || logoutForm.value.jobStatus,
                stayTime: detailData.stayTime || logoutForm.value.stayTime,
                registerTime: detailData.registrationTime
                  ? (new Date(detailData.registrationTime).toString() !== 'Invalid Date'
                    ? new Date(detailData.registrationTime).toLocaleString()
                    : '暂无有效登记时间')
                  : '暂无登记时间',
                idCard: detailData.smartCardId || respData.smartCardId || '',
                contactPhone: detailData.contactPhone || '暂无数据',
                registrationUser: detailData.registrationUser || '暂无数据',
                vehiclePhoto: await getPreviewPhotoUrl(detailData.pic) || '暂无照片',
              };

              Object.assign(logoutForm.value, detailFormData);
            } else {
              console.warn('详情接口返回无有效数据，接口状态', detailResponse?.status);
              MessagePlugin.warning('详情接口无有效数据，核心读卡信息已保留');
            }
          } catch (detailError) {
            console.error('获取登出详情接口失败', detailError);
            MessagePlugin.warning('补全附加数据失败，核心读卡信息已保留');
          }
        } else {
          MessagePlugin.success('读卡成功，核心信息已回显（无有效作业ID，无法补全详情）');
        }

      } else {
        const tipMessage = response?.status?.message || '未查询到该卡号的登记信息，请先登记';
        MessagePlugin.warning(tipMessage);
        resetLogoutForm();
      }
    } catch (error) {
      console.error('读卡数据处理/接口请求失败', error);
      MessagePlugin.error('数据加载失败，请检查网络或接口状态');
      resetLogoutForm();
    } finally {
      logoutLoading.value = false;
    }
  } else {
    MessagePlugin.error('硬件读卡失败，请重试');
  }
};

// 作业状态映射
const mapJobStatusToText = (status) => {
  const statusMap = {
    'logIn': '已登入',
    'sampled': '已扦样',
    'inspected': '已质检',
    'weighed': '已称重',
    'warehoused': '已入仓',
    'tareWeighed': '已称皮',
    'settled': '已结算',
    'logout': '已登出',
    'pendCard': '待销卡'
  };
  return statusMap[status] || status || '未知状态';
};

// 表格数据映射
const mapLogoutToTable = (item) => {
  if (!item || Object.keys(item).length === 0) return {};
  return {
    id: item.id || '',
    // cardNo: item.smartCardId || '',
    cardNo: item.smartCardID || '',
    logoutNo: item.registrationNo || '',
    carrier: item.carrierName || '暂无',
    licensePlate: item.licensePlate || '暂无',
    clientName: item.customerName || '暂无',
    jobType: item.jobType === 1 ? '入库' : item.jobType === 2 ? '出库' : '未知',
    factoryOutTime: item.registrationTime ? new Date(item.registrationTime).toLocaleString() : '暂无时间',
    status: '已登出'
  };
};

// 获取登出记录表格数据
const getLogoutTableData = async () => {
  logoutLoading.value = true;
  try {
    const requestData = {
      pageNo: logoutPagination.value.defaultCurrent,
      pageSize: logoutPagination.value.defaultPageSize,
      pageTotal: false
    };
    console.log('对接接口的请求参数', requestData);

    const res = await queryTodayLogoutRecords(requestData);
    console.log('接口返回原始数据', res);

    const validLogoutList = Array.isArray(res)
      ? res
      : (res?.result?.data && Array.isArray(res.result.data))
        ? res.result.data
        : (res?.result && Array.isArray(res.result))
          ? res.result
          : (res?.data && Array.isArray(res.data))
            ? res.data
            : [];

    const tableDataWithSerial = validLogoutList.map((item, index) => ({
      ...mapLogoutToTable(item),
      serial: index + 1
    }));

    logoutTableData.value = tableDataWithSerial;
    logoutPagination.value.total = validLogoutList.length;

    if (logoutTableData.value.length === 0) {
      //MessagePlugin.info('暂无今日登出记录');
    }
  } catch (error) {
    console.error('加载登出记录失败', error);
    MessagePlugin.error('表格加载失败，请联系管理员');
    logoutTableData.value = [];
    logoutPagination.value.total = 0;
  } finally {
    logoutLoading.value = false;
  }
};

// 表格分页切换
const handleLogoutPageChange = (pageInfo) => {
  logoutPagination.value.defaultCurrent = pageInfo.current;
  logoutPagination.value.defaultPageSize = pageInfo.pageSize;
  getLogoutTableData();
};

// 重置表单
const resetLogoutForm = () => {
  logoutForm.value = {
    cardNo: '',
    physicalCardNo: '',
    carrier: '',
    licensePlate: '',
    clientName: '',
    jobNo: '',
    jobType: '',
    jobStatus: '',
    registerTime: '',
    stayTime: '',
    vehiclePhoto: '',
    idCard: ''
  };
  logoutCardStatus.value = 'init';
  logoutStatusText.value = '待销卡';
};

// 照片预览
const previewLogoutPhoto = () => {
  if (logoutForm.value.vehiclePhoto) {
    MessagePlugin.info('登出照片预览功能待实现');
  } else {
    MessagePlugin.warning('暂无有效车辆照片可供预览');
  }
};

// 销卡操作
const handleLogoutCard = async () => {
  try {
    logoutLoading.value = true;

    const { cardNo, taskId, stayTime, idCard } = logoutForm.value;

    console.log('准备提交的智能卡ID（smartCardId）', idCard);

    if (!cardNo) {
      MessagePlugin.warning('请先读卡获取智能卡信息');
      return;
    }
    if (!taskId) {
      MessagePlugin.warning('缺少工作单ID，无法完成销卡');
      return;
    }
    if (!stayTime || isNaN(Number(stayTime))) {
      MessagePlugin.warning('停留时间必须为有效数字，无法完成销卡');
      return;
    }
    if (!idCard) {
      MessagePlugin.warning('智能卡ID不能为空，无法完成销卡');
      return;
    }

    const logoutParams = {
      taskId: taskId,
      stayTime: Number(stayTime),
      smartCardId: idCard
    };
    console.log('销卡接口请求参数', logoutParams);

    const logoutRes = await outRegistration(logoutParams);
    console.log('销卡接口返回结果', logoutRes);

    const responseStatus = logoutRes?.status || {};
    if (responseStatus?.success === true) {
      MessagePlugin.success('销卡成功，表格已同步最新数据');
      await getLogoutTableData();
      logoutCardStatus.value = 'success';
      logoutStatusText.value = '销卡完成';
      setTimeout(() => {
        resetLogoutForm();
      }, 3000);
    } else {
      const errorMsg = responseStatus?.message || '销卡失败，后端未返回成功状态';
      throw new Error(errorMsg);
    }

  } catch (error) {
    console.error('销卡操作失败', error);
    MessagePlugin.error(`销卡失败${error.message || '请检查网络或联系管理员'}`);
  } finally {
    logoutLoading.value = false;
  }
};

// 初始化加载表格数据
onMounted(() => {
  getLogoutTableData();
});
</script>

<style lang="less" scoped>
.operation-type-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
  flex-shrink: 0;
}

.logout-page-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 16px;
  // padding: 16px;
  box-sizing: border-box;
  overflow: hidden;
}

.top-main-container {
  flex: 2.5;
  display: flex;
  gap: 25px;
  box-sizing: border-box;
  min-height: 0;
}

.fixed-tabs {
  flex: 3;
  min-height: 0;
  border-radius: 4px;
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  padding: 15px;
  position: relative;
}

.fixed-card {
  flex: 1;
  min-height: 0;
  border-radius: 4px;
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.fixed-table {
  flex: 1.5;
  width: 100%;
  border-radius: 4px;
  overflow: hidden;
  box-sizing: border-box;
  min-height: 0;
  background-color: #fff;
}

.tab-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0;
  border-bottom: none;
  flex-shrink: 0;
  width: 100%;
  margin-bottom: 15px;
}

.read-card-wrapper {
  position: relative;
  z-index: 9999 !important;
}

.read-card-wrapper :deep(.t-button) {
  position: relative;
  z-index: 10000 !important;
  pointer-events: auto !important;
  cursor: pointer !important;
}

.inner-form-tabs {
  width: 100%;
}

.logout-page-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  flex: 1;
  overflow-y: auto;
}

.left-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: auto;
  box-sizing: border-box;
  overflow-y: auto;
  overflow-x: hidden;
  padding-bottom: 0;
}

.right-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 15px;

  .form-subtitle {
    font-size: 16px;
    font-weight: 500;
    font-family: PingFang SC;
    display: flex;
    align-items: center;
    color: #000;
    gap: 8px;
    margin-bottom: 15px;
  }

  .right-action-section {
    flex: 1;
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
    min-height: 0;
    align-items: center;
    justify-content: center;
  }

  .card-box-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 16px;
    width: 100%;
    flex: 1;
    min-height: 0;
    border: 1px dashed #4AA34C;
    background: #EFF7EB;
    border-radius: 4px;
    padding: 24px;
  }

  .card-status-box {
    width: 100%;
    max-width: 306px;
    background: linear-gradient(100.6deg, #55AC98 3.79%, #59E493 95.45%);
    border-radius: 4px;
    padding: 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 15px;
    flex-shrink: 0;

    &.success {
      box-shadow: 0px 4px 8px #4AA34C33;
    }
  }

  .status-icon-text-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .status-icon {
    color: #fff;
    font-size: 28px;
  }

  .status-text {
    color: #fff;
    font-size: 16px;
    font-weight: 500;
  }

  .card-info {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 10px;

    .info-item {
      display: flex;
      justify-content: space-between;

      .label,
      .value {
        color: #fff;
        font-size: 14px;
      }
    }
  }

  .logout-btn {
    width: 100%;
    max-width: 306px;
    padding: 12px 0;
    background: #4AA34C !important;
    border-color: #4AA34C !important;
    color: #fff !important;
    font-weight: 500;
    border-radius: 4px;

    &:hover {
      background: #5ab45c !important;
      border-color: #5ab45c !important;
    }
  }
}

.forNewclass {
  width: 100%;

  ::v-deep .t-row {
    margin-bottom: 8px !important;
  }

  ::v-deep .t-col {
    margin-bottom: 16px;
  }

  // ::v-deep .t-form-item__label {
  //   padding: 0 !important;
  //   line-height: 1.5;
  //   color: #333;
  //   font-size: 14px;
  //   font-weight: 500;
  // }

  // ::v-deep .t-form-item__content {
  //   line-height: 1.5;
  // }

  // ::v-deep .t-input {
  //   &.t-is-disabled {
  //     background-color: #f5f7fa !important;
  //     border: 1px solid #dcdfe6 !important;
  //     cursor: not-allowed;

  //     .t-input__inner {
  //       background-color: #f5f7fa !important;
  //       border: none !important;
  //       color: #8e97a8 !important;
  //       cursor: not-allowed;
  //     }
  //   }

  //   &.t-is-readonly {
  //     background-color: #fff !important;
  //     border-color: #dcdcdc !important;

  //     .t-input__inner {
  //       background-color: #fff !important;
  //       color: #333 !important;
  //     }
  //   }
  // }
}

.bottom-section {
  height: 100%;
  display: flex;
  flex-direction: column;

  .bottom-records-table {
    padding: 15px;
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .table-title {
    font-size: 16px;
    font-weight: 500;
    font-family: PingFang SC;
    display: flex;
    align-items: center;
    color: #000;
    gap: 8px;
  }
}

.required {
  color: #f5222d;
  margin-right: 4px;
}

.photo-container {
  width: 120px;
  height: 120px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;

  .camera-icon {
    color: #999;
    font-size: 24px;
  }

  .vehicle-photo {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 4px;
  }
}

::v-deep .t-tabs {

  .t-tabs__nav-wrap {
    height: 38px;
  }

  .t-tabs__nav-item {
    font-size: 15px;
    font-family: PingFang SC;

    &.t-is-active {
      color: #f59e0b;
      text-shadow: 0 0 0.3px #f59e0b;
    }
  }

  .t-tabs__bar {
    background-color: #f59e0b;
  }
}
</style>