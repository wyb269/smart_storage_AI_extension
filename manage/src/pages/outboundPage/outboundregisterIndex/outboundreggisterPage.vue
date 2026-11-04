<template>
  <div class="register-page-container">
    <div class="top-main-container">
      <div class="fixed-tabs">
        <t-tabs :value="currentTab" class="inner-form-tabs" @change="handleInnerTabChange">
          <t-tab-panel value="register" label="登记"></t-tab-panel>
          <t-tab-panel value="logout" label="登出"></t-tab-panel>
        </t-tabs>
        <div class="register-page-content">
          <div class="left-section">
            <div class="operation-type-title-row">
              <img :src="icon3" class="operation-type-icon" />
              <span class="operation-type-title-text">作业类型</span>
            </div>
            <div class="forNewclass">
              <t-form ref="formRef" :data="registerForm" :rules="rules" class="form-section" labelAlign="top"
                labelWidth="80px">
                <t-row :gutter="[65, 12]">
                  <t-col :span="4">
                    <t-form-item label="承运人" name="carrier" required colon>
                      <t-input v-model="registerForm.carrier" placeholder="请输入承运人" />
                    </t-form-item>
                  </t-col>
                  <t-col :span="4">
                    <t-form-item label="身份证号码" name="idCard" required colon>
                      <t-input v-model="registerForm.idCard" placeholder="请输入身份证号码" maxlength="18" />
                    </t-form-item>
                  </t-col>
                  <t-col :span="4">
                    <t-form-item label="车牌类型" name="licensePlateType" required colon>
                      <t-select v-model="registerForm.licensePlateType" placeholder="请选择车牌类型"
                        :options="licensePlateTypeOptions">
                      </t-select>
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row :gutter="[65, 12]">
                  <t-col :span="4">
                    <t-form-item label="车牌号" name="licensePlate" required colon>
                      <t-input v-model="registerForm.licensePlate" placeholder="请填写车牌号" />
                    </t-form-item>
                  </t-col>
                  <t-col :span="4">
                    <t-form-item label="联系电话" name="contactPhone" required colon>
                      <t-input v-model="registerForm.contactPhone" placeholder="请输入联系电话" />
                    </t-form-item>
                  </t-col>
                  <t-col :span="4">
                    <t-form-item label="客户名称" name="clientName" required colon>
                      <t-select v-model="registerForm.clientName" placeholder="请选择客户" filterable
                        :options="clientNameOptions" @filter="handleCustomerSearch">
                      </t-select>
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row :gutter="[65, 12]">
                  <t-col :span="12">
                    <t-form-item label="备注" name="remark" colon>
                      <t-textarea v-model="registerForm.remark" placeholder="请输入备注，限制长度160" :maxlength="160"
                        show-word-limit />
                    </t-form-item>
                  </t-col>
                </t-row>
              </t-form>
            </div>
            <div class="btn-box">
              <t-button class="left-button" @click="handleConfirmRegister" style="width: 28%;">
                <!-- <template #icon>
                  <img src="@/assets/sampling/icon3.png" class="button-icon">
                </template> -->
                确认登记
              </t-button>
            </div>
          </div>
        </div>
      </div>

      <div class="right-section fixed-card">
        <h3 class="form-subtitle">
          <img :src="icon4" class="operation-type-icon" />
          读卡
        </h3>
        <div class="right-action-section">
          <div class="card-box-wrapper">
            <div class="function-modules">
              <div class="module-item id-card-module">
                <div class="id-card-reader">
                  <div class="id-card-box">
                    <div class="id-card-info-item">
                      <span class="label">姓名:</span>
                      <span class="value">{{ registerForm.carrier || '未读取' }}</span>
                    </div>
                    <div class="id-card-info-item">
                      <span class="label">身份证号:</span>
                      <span class="value">{{ registerForm.idCard || '未读取' }}</span>
                    </div>
                  </div>
                  <t-button theme="primary" class="read-idcard-btn" @click="handleReadIdCard">
                    <img :src="icon5" class="icon5-style" />读取身份证
                  </t-button>
                </div>
              </div>

              <div class="module-item smart-card-module">
                <div class="smart-card-reader">
                  <div class="card-status-box" :class="{ 'success': cardStatus === 'success' }">
                    <div class="status-icon-text-wrapper">
                      <t-icon name="check-circle" class="status-icon" v-if="cardStatus === 'success'" />
                      <span class="status-text">{{ cardStatusText }}</span>
                    </div>
                    <div class="card-info">
                      <div class="info-item">
                        <span class="label">智能卡编号:</span>
                        <span class="value">{{ registerForm.cardNo || '未读取' }}</span>
                      </div>
                      <div class="info-item">
                        <span class="label">智能卡物理编号:</span>
                        <span class="value">{{ registerForm.physicalCardNo || '未读取' }}</span>
                      </div>
                    </div>
                  </div>
                  <t-button theme="primary" variant="base" class="make-card-btn" @click="triggerCardRead">
                    <img :src="icon6" class="icon6-style" />制卡
                  </t-button>
                  <div class="read-card-btn-wrapper">
                    <readCard ref="cardReaderRef" @result="handleRegisterCardHardwareResult" />
                  </div>
                </div>
              </div>

              <div class="module-item photo-upload-module">
                <div class="photo-upload-section">
                  <div class="upload-container">
                    <div class="upload-tip" v-if="!registerForm.operationPhoto">
                      <span>缺少作业照片，请先完成作业照片拍照或图片上传!</span>
                      <img :src="icon7" class="icon7-style" />
                      <div class="upload-buttons">
                        <t-button theme="primary" variant="base" @click="handleTakePhoto">拍照</t-button>
                        <t-button variant="base" @click="handleUploadFile">文件上传</t-button>
                      </div>
                    </div>
                    <div class="photo-preview" v-else>
                      <img :src="previewPhotoUrl" class="preview-img" alt="作业照片" />
                    </div>
                  </div>
                </div>
                <div class="reupload-area" v-if="registerForm.operationPhoto">
                  <t-button variant="base" @click="handleUploadFile" class="reupload-btn">
                    重新上传
                  </t-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="bottom-section fixed-table">
      <div class="bottom-records-table">
        <div class="title-wrapper">
          <img :src="icon11" class="operation-type-icon" />
          <h3 class="table-title">今日登记记录</h3>
        </div>
        <CommonTable justList="1" tbMt="0" :columns="registerTableColumns" :tableData="registerTableData"
          :pagination="tablePagination" :loading="tableLoading" @changePage="handlePageChange"
          class="forNewClassTableAndsearch" />
      </div>
    </div>
  </div>
</template>

<script setup lang="jsx">
import { ref, defineProps, defineEmits, onMounted, getCurrentInstance } from 'vue';
import icon3 from "@/assets/register/icon3.png"
import icon4 from "@/assets/register/icon4.png"
import CommonTable from "@/components/common-table/index.vue"
import icon5 from "@/assets/register/icon5.png"
import icon6 from "@/assets/register/icon6.png"
import icon7 from "@/assets/register/icon7.png"
import icon10 from "@/assets/register/icon10.png"
import readCard from '@/components/readCard/index.vue';
import { swipeCard } from '@/api/outboundregister';
import { MessagePlugin } from 'tdesign-vue-next';
import { getDict } from "@/api/sampling.ts";
import { getList as getCustomerList } from '@/api/customer';
import { getRegisterList } from '@/api/register';
import icon11 from "@/assets/register/icon11.png"
import { saveRegistration } from '@/api/outboundregister';

const triggerCardRead = () => {
  const readCardButtons = document.querySelectorAll('.read-card-btn-wrapper button');
  if (readCardButtons.length > 0) {
    readCardButtons[0].click();
  }

  cardStatus.value = 'loading';
  isLoading.value = true;
};
const formRef = ref([]);
const licensePlateTypeOptions = ref([]);
const cardReaderRef = ref([]);

// 车牌类型字典
const getLicensePlateTypeOptions = () => {
  getDict("LICENCE_PLATE_TYPE")
    .then((res) => {
      const data = res.result.listDictData || [];
      licensePlateTypeOptions.value = data.map((item) => ({
        label: item.label,
        value: item.description,
      }));
    })
    .catch((error) => {
      console.error("加载车牌类型字典失败：", error);
      MessagePlugin.warning("车牌类型字典加载失败，请刷新页面");
    });
};

const clientNameOptions = ref([]);

const loadCustomerOptions = (searchName = "") => {
  return new Promise((resolve, reject) => {
    getCustomerList({
      customerType: 0,
      name: searchName,
      card: "",
      // inStatus: 0,
      outStatus: 1
    }).then((response) => {
      const customerList = response?.result?.data || [];
      clientNameOptions.value = customerList.map(item => ({
        label: item.name,
        value: item.id,
      }));
      resolve();
    }).catch((error) => {
      MessagePlugin.error("客户列表加载失败，请刷新页面");
      clientNameOptions.value = [];
      reject(error);
    });
  });
};

const handleCustomerSearch = (searchValue) => {
  loadCustomerOptions(searchValue);
};

const props = defineProps({
  currentTab: {
    type: String,
    required: true,
    default: 'register'
  }
});

const emit = defineEmits(['tab-change']);

const handleInnerTabChange = (targetTab) => {
  emit('tab-change', targetTab);
};

const innerTab = ref('register');
const cardStatus = ref('init');
const cardStatusText = ref('待读卡');
const idCardInfo = ref({ name: '', gender: '', address: '' });
const isLoading = ref(false);
const previewPhotoUrl = ref('');

const registerForm = ref({
  carrier: "",
  idCard: "",
  licensePlateType: "",
  licensePlate: "",
  contactPhone: "",
  clientName: "",
  smartCardId: "",
  remark: "",
  jobType: 2,
  operationPhoto: "",
  cardNo: "",
  physicalCardNo: ""
});

const rules = ref({
  carrier: [{ required: true, message: "请输入承运人姓名" }],
  idCard: [
    { required: true, message: "请输入承运人身份证号" },
    {
      pattern: /(^\d{18}$)|(^\d{17}(\d|X|x)$)/,
      message: "请输入正确的18位身份证号码"
    }
  ],
  licensePlateType: [{ required: true, message: "请选择车牌类型" }],
  licensePlate: [{ required: true, message: "请输入车牌号" }],
  contactPhone: [{ required: true, message: "请输入联系电话" }],
  clientName: [{ required: true, message: "请选择客户" }],
  cardNo: [{ required: true, message: "请输入智能卡编号" }],
  physicalCardNo: [{ required: true, message: "请读取智能卡物理编号" }],
  operationPhoto: [{ required: true, message: "请上传登记图片" }],
});

const registerTableColumns = [
  { colKey: "serial", title: "序号", width: 60, align: "center" },
  { colKey: "cardNo", title: "智能卡编号", width: 120, align: "center" },
  { colKey: "registerNo", title: "登记单编号", width: 120, align: "center" },
  { colKey: "carrier", title: "承运人", align: "center" },
  { colKey: "licensePlate", title: "车牌号", align: "center" },
  { colKey: "clientName", title: "客户名称", align: "center" },
  { colKey: "jobType", title: "作业类型", align: "center" },
  { colKey: "registerTime", title: "登记时间", align: "center" },
  { colKey: "status", title: "状态", align: "center" },
];

const tableLoading = ref(false);
const tablePagination = ref({
  defaultCurrent: 1,
  defaultPageSize: 10,
  total: 0,
});
const registerTableData = ref([]);
const { proxy } = getCurrentInstance();

const handleReadIdCard = async () => {
  idCardInfo.value = {
    name: '张三',
    gender: '男',
    address: '北京市朝阳区'
  };
  registerForm.value.idCard = '110105199001011234';
  registerForm.value.carrier = "张三"
  MessagePlugin.success('身份证读取成功');
};

const handleRegisterCardHardwareResult = async (r) => {
  console.log('[登记页面 - 硬件读卡返回]：', r);
  cardStatus.value = 'loading';
  cardStatusText.value = '读卡中...';
  isLoading.value = true;

  if (r.result === '1') {
    try {
      const rawHardwareUid = (r.data || '').trim();
      if (!rawHardwareUid) {
        MessagePlugin.warning('硬件未读取到有效物理卡号，请重新刷卡');
        isLoading.value = false;
        cardStatus.value = 'init';
        cardStatusText.value = '待读卡';
        return;
      }

      const requestData = {
        physicalCardNumber: rawHardwareUid,
        processStep: 'logIn'
      };
      console.log('登记页 inSwipeCard 请求参数：', requestData);

      const response = await swipeCard(requestData);
      console.log('[登记页 inSwipeCard 接口返回]：', response);

      const respResult = response?.result;
      if (response?.status?.success && respResult) {
        registerForm.value.physicalCardNo = respResult.physicalCardNumber || '';
        registerForm.value.smartCardId = respResult.id || '';
        registerForm.value.cardNo = respResult.cardNo || '';

        cardStatus.value = 'success';
        cardStatusText.value = '读卡成功';
        MessagePlugin.success(`读卡成功！物理卡号：${registerForm.value.physicalCardNo}`);
      } else {
        const tipMessage = response?.status?.message || '未查询到该卡号的登记信息，请先登记';
        MessagePlugin.warning(tipMessage);
        cardStatus.value = 'init';
        cardStatusText.value = '读卡失败';
        registerForm.value.cardNo = '';
        registerForm.value.physicalCardNo = '';
        registerForm.value.smartCardId = '';
      }
    } catch (error) {
      console.error('登记页读卡处理失败：', error);
      MessagePlugin.error('数据加载失败，请检查网络或接口状态');
      cardStatus.value = 'init';
      cardStatusText.value = '读卡失败';
      registerForm.value.cardNo = '';
      registerForm.value.physicalCardNo = '';
      registerForm.value.smartCardId = '';
    } finally {
      isLoading.value = false;
    }
  } else {
    cardStatus.value = 'init';
    cardStatusText.value = '读卡失败';
    MessagePlugin.error('硬件读卡失败，请重试');
  }
};

const handleTakePhoto = () => {
  MessagePlugin.warning('拍照功能暂不支持，请点击「文件上传」上传照片！');
};

const handleUploadFile = async () => {
  const fileInput = document.createElement('input');
  fileInput.type = 'file';
  fileInput.accept = 'image/jpeg, image/png';
  fileInput.style.display = 'none';
  document.body.appendChild(fileInput);

  fileInput.onchange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    try {
      const uploadRes = await proxy.$uploadPic(file);

      if (uploadRes?.objectKey) {
        registerForm.value.operationPhoto = uploadRes.objectKey;
        previewPhotoUrl.value = await proxy.$getPic(uploadRes.objectKey);

        const successText = registerForm.value.operationPhoto ? '作业照片重新上传成功！' : '作业照片上传成功！';
        MessagePlugin.success(successText);
      } else {
        MessagePlugin.error('照片上传失败，请重试！');
      }
    } catch (error) {
      console.error('照片上传异常：', error);
      MessagePlugin.error('照片上传异常，请检查网络！');
    } finally {
      document.body.removeChild(fileInput);
    }
  };

  fileInput.click();
};

const handleConfirmRegister = async () => {
  try {
    if (!formRef.value) {
      MessagePlugin.error("表单初始化失败，请刷新页面");
      return;
    }
    const validateResult = await formRef.value.validate();
    if (!validateResult) {
      return;
    }

    const requestData = {
      carrierName: registerForm.value.carrier,
      idCard: registerForm.value.idCard,
      ticketType: registerForm.value.licensePlateType,
      licensePlate: registerForm.value.licensePlate,
      contactPhone: registerForm.value.contactPhone,
      customerId: registerForm.value.clientName,
      smartCardId: registerForm.value.smartCardId,
      cardNo: registerForm.value.cardNo,
      remark: registerForm.value.remark,
      jobType: 2,
      pic: registerForm.value.operationPhoto,
    };

    const response = await saveRegistration(requestData);

    if (response?.status?.code === "200" || response?.status?.success) {
      MessagePlugin.success("登记成功！");
      formRef.value.reset();
      resetForm();
      getRegisterTableData();
    } else {
      MessagePlugin.error(response?.status?.message || "登记失败，请重试");
    }
  } catch (error) {
    MessagePlugin.error("登记提交失败：" + (error.message || "网络异常"));
    console.error("登记接口调用失败：", error);
  }
};

const mapRegisterToTable = (item) => {
  if (!item) return {};
  return {
    serial: '',
    cardNo: item.smartCardID || '暂无',
    registerNo: item.registrationNo || '暂无',
    carrier: item.carrierName || '暂无',
    licensePlate: item.licensePlate || '暂无',
    clientName: item.customerName || '暂无',
    jobType: item.jobType === 1 ? '入库' : item.jobType === 2 ? '出库' : '未知',
    registerTime: item.registrationTime
      ? new Date(item.registrationTime).toLocaleString()
      : '暂无',
    status: item.jobStatus || '已登记'
  };
};

const getRegisterTableData = async () => {
  tableLoading.value = true;
  try {
    const reqParams = {
      pageNo: tablePagination.value.defaultCurrent,
      pageSize: tablePagination.value.defaultPageSize,
      pageTotal: false
    };

    const res = await getRegisterList(reqParams);

    const tableList = Array.isArray(res)
      ? res
      : (res?.result?.data && Array.isArray(res.result.data))
        ? res.result.data
        : (res?.result && Array.isArray(res.result))
          ? res.result
          : (res?.data && Array.isArray(res.data))
            ? res.data
            : [];

    const finalData = tableList.map((item, index) => ({
      ...mapRegisterToTable(item),
      serial: index + 1
    }));

    registerTableData.value = finalData;
    tablePagination.value.total = tableList.length;

    if (finalData.length === 0) {
      // MessagePlugin.info('暂无今日登记记录');
    }
  } catch (error) {
    console.error('表格加载失败：', error);
    MessagePlugin.error('登记记录加载失败，请重试');
    registerTableData.value = [];
    tablePagination.value.total = 0;
  } finally {
    tableLoading.value = false;
  }
};

const handlePageChange = (pageInfo) => {
  tablePagination.value.defaultCurrent = pageInfo.current;
  tablePagination.value.defaultPageSize = pageInfo.pageSize;
  getRegisterTableData();
};

const resetForm = () => {
  registerForm.value = {
    carrier: '',
    idCard: '',
    licensePlateType: '',
    licensePlate: '',
    contactPhone: '',
    clientName: '',
    remark: '',
    cardNo: '',
    physicalCardNo: '',
    operationPhoto: '',
    smartCardId: ''
  };
  previewPhotoUrl.value = '';
  cardStatus.value = 'init';
  cardStatusText.value = '待读卡';
  idCardInfo.value = { name: '', gender: '', address: '' };
  isLoading.value = false;
};

onMounted(() => {
  getLicensePlateTypeOptions();
  loadCustomerOptions();
  getRegisterTableData();
});
</script>

<style lang="less" scoped>
.register-page-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 16px;
  //padding: 16px;
  box-sizing: border-box;
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
}

.fixed-card {
  flex: 1;
  min-height: 0;
  border-radius: 4px;
  overflow: hidden;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.fixed-table {
  flex: 1.5;
  width: 100%;
  border-radius: 4px;
  overflow: hidden;
  box-sizing: border-box;
  min-height: 0;
  padding: 15px;
}

.register-page-content {
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

::v-deep .left-section {
  width: 100%;

  .t-textarea__inner {
    font-size: 14px;
    font-family: PingFang SC;
    min-height: 64px;
  }
}

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

.button-icon {
  width: 17px;
  object-fit: contain;
  margin-right: 10px;
}

.right-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background-color: #fff;
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
  }

  .function-modules {
    display: flex;
    flex-direction: column;
    gap: 16px;
    width: 100%;
    flex: 1;
    min-height: 0;
  }

  .module-item {
    flex: 1;
    width: 100%;
    border-radius: 4px;
    padding: 16px;
    box-sizing: border-box;
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 0;
  }

  .id-card-module {
    border: 1px solid #5E7FF5;
    box-shadow: 0px 4px 4px 0px #5D7FF61A;
    background-color: #F2F5FD;
  }

  .smart-card-module {
    background: #EFF7EB;
    border: 1px solid #4AA34C;
    border-radius: 4px;
  }

  .photo-upload-module {
    background: #FBF2E5;
    border: 1px solid #F1933A;
    padding: 15px !important;
    flex-direction: column;
  }
}

.id-card-box {
  background: #5E7FF5;
  border-radius: 4px;
  width: 75%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 15px;
  justify-content: center;
  height: 112px;
}

.id-card-info-item {
  display: flex;
  justify-content: space-between;
  width: 100%;
  gap: 6px;
  padding: 4px 0;

  .label,
  .value {
    color: #fff;
    font-size: 12px;
  }

  .value {
    font-weight: 500;
  }
}

.read-idcard-btn {
  width: 85%;
  padding: 12px 0 !important;
  color: #fff !important;
  background-color: #5E7FF5 !important;
  border: 1px solid #5E7FF5 !important;
  border-radius: 4px !important;
  font-size: 12px !important;
  font-weight: 500 !important;
  cursor: pointer !important;
  opacity: 1 !important;
  pointer-events: auto !important;

  &:hover {
    background-color: #4A6EDB !important;
    border-color: #4A6EDB !important;
    color: #fff !important;
  }

  &:disabled {
    background-color: #5E7FF5 !important;
    border-color: #5E7FF5 !important;
    color: #fff !important;
    cursor: pointer !important;
    opacity: 1 !important;
  }
}

.id-card-reader,
.smart-card-reader {
  gap: 16px !important;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  position: relative;
}

.bottom-section {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #fff;

  .bottom-records-table {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .title-wrapper {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
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

.operation-type-title-row {
  font-size: 16px;
  font-weight: 500;
  font-family: PingFang SC;
  display: flex;
  align-items: center;
  color: #000;
  margin-bottom: 15px;
  gap: 8px;
}

.operation-type-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
  flex-shrink: 0;
}

.forNewclass {
  width: 100%;

  ::v-deep .t-row {
    margin-bottom: 8px !important;
  }

  ::v-deep .t-col {
    margin-bottom: 16px;
  }

  ::v-deep .t-form-item__label {
    padding: 0 !important;
    line-height: 1.5;
    color: #333;
    font-size: 14px;
    font-weight: 500;
  }

  ::v-deep .t-form-item__content {
    line-height: 1.5;
  }
}

.icon5-style {
  width: 20px;
  object-fit: contain;
  flex-shrink: 0;
  margin-right: 16px;
  vertical-align: middle;
}

.smart-card-reader,
.photo-upload-section {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.upload-container {
  border-radius: 4px;
  text-align: center;
  width: 100%;
  overflow: hidden;
}

.upload-tip {
  color: #F1933A;
  font-size: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;

  .icon7-style {
    width: 36px !important;
    height: 36px !important;
    object-fit: contain;
    flex-shrink: 0;
    margin: 4px 0 !important;
  }
}

.photo-preview {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.preview-img {
  width: 100%;
  max-height: 140px;
  object-fit: contain;
  border-radius: 4px;
  border: 1px solid #eee;
  background: #fff;
}

.reupload-area {
  width: 100%;
  display: flex;
  justify-content: center;
  padding: 8px 0;
  border-top: 1px solid #eee;
  background: #FBF2E5;
}

.reupload-btn {
  background: #fff !important;
  border-color: #F1933A !important;
  color: #F1933A !important;
  font-size: 12px !important;
  padding: 4px 12px !important;
  border-radius: 4px !important;
  width: 80%;
}

.reupload-btn:hover {
  border-color: #f3a85c !important;
  color: #f3a85c !important;
}

.upload-buttons {
  gap: 12px !important;
  width: 100%;
  justify-content: center;

  .t-button {
    width: calc((85% / 2) - 6px) !important;
    padding: 8px 0 !important;
    font-size: 12px !important;
    font-weight: 500;
    border-radius: 4px !important;

    &:nth-child(1) {
      background: #F1933A !important;
      border-color: #F1933A !important;
      color: #fff !important;

      &:hover {
        background: #f3a85c !important;
        border-color: #f3a85c !important;
      }
    }

    &:nth-child(2) {
      background: #fff !important;
      border-color: #F1933A !important;
      color: #F1933A !important;

      &:hover {
        border-color: #f3a85c !important;
        color: #f3a85c !important;
      }
    }
  }
}

.card-status-box {
  background: linear-gradient(100.6deg, #55AC98 3.79%, #59E493 95.45%);
  box-shadow: 0px 4px 4px 0px #4AA34C1A;
  border-radius: 4px;
  width: 75%;
  padding: 15px;
  display: flex;
  flex-direction: column;
  align-items: center;

  &.success {
    box-shadow: 0px 4px 8px 0px #4AA34C33;
  }

  .status-icon-text-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    margin: 0;
  }

  .status-icon {
    color: #fff;
    font-size: 24px;
  }

  .status-text {
    font-weight: 600;
    color: #fff;
    font-size: 14px;
  }

  .card-info {
    width: 100%;
    padding: 8px 0;
    display: flex;
    flex-direction: column;
    gap: 6px;
    margin: 0;

    .info-item {
      display: flex;
      justify-content: space-between;
      width: 100%;

      .label,
      .value {
        color: #fff;
        font-size: 12px;
      }

      .value {
        font-weight: 500;
      }
    }
  }
}

.make-card-btn {
  width: 85%;
  padding: 12px 0;
  background: #059669 !important;
  border-color: #059669 !important;
  border-radius: 4px;
  color: #fff !important;
  font-size: 12px;
  font-weight: 500;
  position: relative;
  z-index: 10;
}

.read-card-btn-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  pointer-events: none;
  z-index: 5;
}

.icon6-style {
  width: 20px;
  object-fit: contain;
  flex-shrink: 0;
  margin-right: 8px;
  vertical-align: middle;
}

::v-deep .t-tabs {
  margin-bottom: 15px;

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