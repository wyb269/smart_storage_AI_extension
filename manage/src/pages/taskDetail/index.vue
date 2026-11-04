<template>
  <div class="container">
    <div class="content">
      <div class="title-back">
        <div class="title">
          <img
            src="@/assets/receipt/icon10.png"
            alt=""
          >
          作业信息
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
          :data="taskForm"
          :rules="taskRules"
          ref="taskFormRef"
          class="info-form"
          labelAlign="top"
        >
          <t-row
            class="form-row"
            :gutter="80"
          >
            <t-col
              :span="3"
              class="form-col"
            >
              <t-form-item
                label="作业编号"
                name="jobNo"
              >
                <t-input
                  :readonly="taskFormReadonly"
                  v-model="taskForm.jobNo"
                />
              </t-form-item>
            </t-col>
            <t-col
              :span="3"
              class="form-col"
            >
              <t-form-item
                label="流程状态"
                name="jobStatus"
              >
                <t-select
                  :readonly="taskFormReadonly"
                  v-model="taskForm.jobStatus"
                  :options="currentJobStatusOptions"
                />
              </t-form-item>
            </t-col>

            <t-col
              :span="3"
              class="form-col"
            >
              <t-form-item
                label="作业开始时间"
                name="addTime"
              >
                <t-date-picker
                  :readonly="taskFormReadonly"
                  v-model="taskForm.addTime"
                  type="datetime"
                  enableTimePicker
                  style="width: 100%;"
                />
              </t-form-item>
            </t-col>

            <t-col
              :span="3"
              class="form-col"
            >
              <t-form-item
                label="作业完成时间"
                name="jobEndTime"
              >
                <t-date-picker
                  :readonly="taskFormReadonly"
                  v-model="taskForm.jobEndTime"
                  type="datetime"
                  enableTimePicker
                  style="width: 100%;"
                />
              </t-form-item>
            </t-col>
          </t-row>

          <t-row
            class="form-row"
            :gutter="80"
          >
            <t-col
              :span="3"
              class="form-col"
            >
              <t-form-item
                label="作业类型"
                name="jobType"
              >
                <t-select
                  :readonly="taskFormReadonly"
                  v-model="taskForm.jobType"
                  :options="jobTypeOptions"
                />
              </t-form-item>
            </t-col>
          </t-row>
        </t-form>
      </div>

      <t-tabs
        v-model:value="activeTab"
        type="line"
        placement="top"
        style="margin-top: 35px; margin-bottom: 20px;"
        @change="tabChange"
      >
        <t-tab-panel
          v-for="item in currentTabList"
          :key="item.value"
          :value="item.value"
          :label="item.label"
        />
      </t-tabs>

      <!-- Tab 内容区域 + Loading 包裹 -->
      <t-loading
        :loading="tabLoading"
        size="medium"
      >
        <div class="box-container">
          <div
            v-if="activeTab === 'tab-1'"
            class="register-box"
          >
            <div
              v-if="Object.keys(registerForm).length === 0"
              class="empty-box"
            >
              <t-empty>
                <template #image>
                  <img
                    src="@/assets/sampling/empty.png"
                    style="width: 120px; object-fit: contain;"
                  >
                </template>
              </t-empty>
            </div>
            <div
              v-else
              class="forNewclass"
            >
              <t-form
                :data="registerForm"
                labelAlign="top"
                class="info-form"
              >
                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="登记单编号"
                      name="registrationNo"
                    >
                      <t-input
                        v-model="registerForm.registrationNo"
                        :readonly="registerReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="承运人"
                      name="carrierName"
                    >
                      <t-input
                        v-model="registerForm.carrierName"
                        :readonly="registerReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="身份证号码"
                      name="idCard"
                    >
                      <t-input
                        v-model="registerForm.idCard"
                        :readonly="registerReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="登记类型"
                      name="jobType"
                    >
                      <t-select
                        v-model="registerForm.jobType"
                        :readonly="registerReadonly"
                        :options="jobTypeOptions"
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="客户名称"
                      name="customerName"
                    >
                      <t-input
                        v-model="registerForm.customerName"
                        :readonly="registerReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="车牌类型"
                      name="ticketType"
                    >
                      <t-select
                        v-model="registerForm.ticketType"
                        :readonly="registerReadonly"
                        :options="ticketTypeOptions"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="联系电话"
                      name="contactPhone"
                    >
                      <t-input
                        v-model="registerForm.contactPhone"
                        :readonly="registerReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="车牌号"
                      name="licensePlate"
                    >
                      <t-input
                        v-model="registerForm.licensePlate"
                        :readonly="registerReadonly"
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="登记员"
                      name="registrationUser"
                    >
                      <t-input
                        v-model="registerForm.registrationUser"
                        :readonly="registerReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="登记时间"
                      name="registrationTime"
                    >
                      <t-date-picker
                        v-model="registerForm.registrationTime"
                        type="datetime"
                        enableTimePicker
                        :readonly="registerReadonly"
                        style="width:100%;"
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="12"
                    class="form-col"
                  >
                    <t-form-item
                      label="备注"
                      name="remark"
                      class="remark-item"
                    >
                      <t-textarea
                        v-model="registerForm.remark"
                        :readonly="registerReadonly"
                        :maxlength="160"
                        show-limit-number
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="登记照片"
                      name="pic"
                      class="img-item"
                    >
                      <div class="img-box">
                        <t-image-viewer
                          :images="[registerForm.pic]"
                          :z-index="10000"
                        ></t-image-viewer>
                      </div>
                    </t-form-item>
                  </t-col>
                </t-row>
              </t-form>
            </div>
          </div>

          <div
            v-if="showSamplingTab && activeTab === 'tab-2'"
            class="sampling-box"
          >
            <div
              v-if="Object.keys(samplingForm).length === 0"
              class="empty-box"
            >
              <t-empty>
                <template #image>
                  <img
                    src="@/assets/sampling/empty.png"
                    style="width: 120px; object-fit: contain;"
                  >
                </template>
              </t-empty>
            </div>
            <div
              v-else
              class="forNewclass"
            >
              <t-form
                :data="samplingForm"
                labelAlign="top"
                class="info-form"
              >
                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="扦样单编号"
                      name="sampleNo"
                    >
                      <t-input
                        v-model="samplingForm.sampleNo"
                        :readonly="samplingReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="扦样员"
                      name="addUser"
                    >
                      <t-input
                        v-model="samplingForm.addUser"
                        :readonly="samplingReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="扦样时间"
                      name="addTime"
                    >
                      <t-date-picker
                        v-model="samplingForm.addTime"
                        type="datetime"
                        enableTimePicker
                        :readonly="samplingReadonly"
                        style="width:100%;"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="粮食品种"
                      name="grainVariety"
                    >
                      <t-select
                        v-model="samplingForm.grainVariety"
                        :options="grainVarietyOptions"
                        :readonly="samplingReadonly"
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="扦样方式"
                      name="sampleType"
                    >
                      <t-select
                        v-model="samplingForm.sampleType"
                        :options="sampleTypeOptions"
                        :readonly="samplingReadonly"
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="12"
                    class="form-col"
                  >
                    <t-form-item
                      label="备注"
                      name="remarks"
                      class="remark-item"
                    >
                      <t-textarea
                        v-model="samplingForm.remarks"
                        :readonly="samplingReadonly"
                        :maxlength="160"
                        show-limit-number
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="扦样视频"
                      name="video"
                      class="video-item"
                    >
                      <div class="video-empty">
                        <close-circle-icon
                          size="20"
                          style="margin-bottom: 6px;"
                        />暂无视频
                      </div>
                    </t-form-item>
                  </t-col>
                </t-row>
              </t-form>
            </div>
          </div>

          <div
            v-if="showQualityTab && activeTab === 'tab-3'"
            class="quality-box"
          >
            <div
              v-if="Object.keys(qualityForm).length === 0"
              class="empty-box"
            >
              <t-empty>
                <template #image>
                  <img
                    src="@/assets/sampling/empty.png"
                    style="width: 120px; object-fit: contain;"
                  >
                </template>
              </t-empty>
            </div>
            <div
              v-else
              class="forNewclass"
            >
              <t-form
                :data="qualityForm"
                labelAlign="top"
                class="info-form"
              >
                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="质检单编号"
                      name="qualityNo"
                    >
                      <t-input
                        v-model="qualityForm.qualityNo"
                        :readonly="qualityReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="质检员"
                      name="addUser"
                    >
                      <t-input
                        v-model="qualityForm.addUser"
                        :readonly="qualityReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="质检时间"
                      name="addTime"
                    >
                      <t-date-picker
                        v-model="qualityForm.addTime"
                        type="datetime"
                        enableTimePicker
                        :readonly="qualityReadonly"
                        style="width:100%;"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="粮食年期"
                      name="grainYear"
                    >
                      <t-select
                        v-model="qualityForm.grainYear"
                        :readonly="qualityReadonly"
                        :options="grainYearOptions"
                        placeholder="请选择粮食年期"
                      ></t-select>
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="粮食等级"
                      name="grainGrade"
                    >
                      <t-select
                        v-model="qualityForm.grainGrade"
                        :readonly="qualityReadonly"
                        :options="grainGradeOptions"
                        placeholder="请选择粮食等级"
                      ></t-select>
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="粮仓号"
                      name="warehouseNo"
                    >
                      <!-- <t-select
                        v-model="qualityForm.warehouseId"
                        :readonly="qualityReadonly"
                        :options="grainStorageOptions"
                        placeholder="请选择粮仓号"
                      ></t-select> -->
                      <t-input
                        v-model="qualityForm.warehouseId"
                        :readonly="qualityReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="粮食单价"
                      name="grainPrice"
                    >
                      <t-input
                        v-model="qualityForm.grainPrice"
                        :readonly="qualityReadonly"
                        suffix="元/千克"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="每千克扣价"
                      name="deductionPrice"
                    >
                      <t-input
                        v-model="qualityForm.deductionPrice"
                        :readonly="qualityReadonly"
                        suffix="元"
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="每千克扣量"
                      name="deduction"
                    >
                      <t-input
                        v-model="qualityForm.deduction"
                        :readonly="qualityReadonly"
                        suffix="千克"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="质量结果"
                      name="qualityResult"
                    >
                      <t-input
                        v-model="qualityForm.qualityResult"
                        :readonly="qualityReadonly"
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="12"
                    class="form-col"
                  >
                    <t-form-item
                      label="备注"
                      name="remarks"
                      class="remark-item"
                    >
                      <t-textarea
                        v-model="qualityForm.remarks"
                        :readonly="qualityReadonly"
                        :maxlength="160"
                        show-limit-number
                      />
                    </t-form-item>
                  </t-col>
                </t-row>
              </t-form>
            </div>
          </div>

          <div
            v-if="showWeighTab && activeTab === 'tab-4'"
            class="weigh-box"
          >
            <div
              v-if="Object.keys(currentWeighForm).length === 0"
              class="empty-box"
            >
              <t-empty>
                <template #image>
                  <img
                    src="@/assets/sampling/empty.png"
                    style="width: 120px; object-fit: contain;"
                  >
                </template>
              </t-empty>
            </div>
            <div
              v-else
              class="forNewclass"
            >
              <t-form
                :data="currentWeighForm"
                labelAlign="top"
                class="info-form"
              >
                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      :label="receiptType === 'inbound' ? '称重单编号' : '称皮单编号'"
                      name="weightNo"
                    >
                      <t-input
                        v-model="currentWeighForm.weightNo"
                        :readonly="currentWeighReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      :label="receiptType === 'inbound' ? '称重员' : '称皮员'"
                      name="addUserName"
                    >
                      <t-input
                        v-model="currentWeighForm.addUserName"
                        :readonly="currentWeighReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      :label="receiptType === 'inbound' ? '称重时间' : '称皮时间'"
                      name="addTime"
                    >
                      <t-date-picker
                        v-model="currentWeighForm.addTime"
                        type="datetime"
                        enableTimePicker
                        :readonly="currentWeighReadonly"
                        style="width:100%;"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="重量"
                      name="weight"
                    >
                      <t-input
                        v-model="currentWeighForm.weight"
                        :readonly="currentWeighReadonly"
                        suffix="千克"
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="12"
                    class="form-col"
                  >
                    <t-form-item
                      label="备注"
                      name="remark"
                    >
                      <t-textarea
                        v-model="currentWeighForm.remark"
                        :readonly="currentWeighReadonly"
                        :maxlength="160"
                        show-limit-number
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <div style="display: flex;">
                  <t-form-item
                    label="车头照片"
                    name="vehicleFrontPic"
                    class="img-item"
                    style="margin-right: 15px;"
                  >
                    <div class="img-box">
                      <t-image-viewer
                        :images="[currentWeighForm.vehicleFrontPic]"
                        :z-index="10000"
                      ></t-image-viewer>
                    </div>
                  </t-form-item>
                  <t-form-item
                    label="车尾照片"
                    name="vehicleBackPic"
                    class="img-item"
                  >
                    <div class="img-box">
                      <t-image-viewer
                        :images="[currentWeighForm.vehicleBackPic]"
                        :z-index="10000"
                      ></t-image-viewer>
                    </div>
                  </t-form-item>
                </div>
              </t-form>
            </div>
          </div>

          <div
            v-if="showWarehouseTab && activeTab === 'tab-5'"
            class="tare-box"
          >
            <div
              v-if="Object.keys(warehousForm).length === 0"
              class="empty-box"
            >
              <t-empty>
                <template #image>
                  <img
                    src="@/assets/sampling/empty.png"
                    style="width: 120px; object-fit: contain;"
                  >
                </template>
              </t-empty>
            </div>
            <div
              v-else
              class="forNewclass"
            >
              <t-form
                :data="warehousForm"
                labelAlign="top"
                class="info-form"
              >
                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="值仓单编号"
                      name="warehouseNo"
                    >
                      <t-input
                        v-model="warehousForm.warehouseNo"
                        :readonly="warehousReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="值仓员"
                      name="addUserName"
                    >
                      <t-input
                        v-model="warehousForm.addUserName"
                        :readonly="warehousReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="值仓时间"
                      name="addTime"
                    >
                      <t-date-picker
                        v-model="warehousForm.addTime"
                        type="datetime"
                        enableTimePicker
                        :readonly="warehousReadonly"
                        style="width:100%;"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="现场扣重"
                      name="deductWeight"
                    >
                      <t-input
                        v-model="warehousForm.deductWeight"
                        :readonly="warehousReadonly"
                        suffix="千克"
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="12"
                    class="form-col"
                  >
                    <t-form-item
                      label="备注"
                      name="remark"
                    >
                      <t-textarea
                        v-model="warehousForm.remark"
                        :readonly="warehousReadonly"
                        :maxlength="160"
                        show-limit-number
                      />
                    </t-form-item>
                  </t-col>
                </t-row>
              </t-form>
            </div>
          </div>

          <div
            v-if="showTareTab && activeTab === 'tab-6'"
            class="tare-box"
          >
            <div
              v-if="Object.keys(currentTareForm).length === 0"
              class="empty-box"
            >
              <t-empty>
                <template #image>
                  <img
                    src="@/assets/sampling/empty.png"
                    style="width: 120px; object-fit: contain;"
                  >
                </template>
              </t-empty>
            </div>
            <div
              v-else
              class="forNewclass"
            >
              <t-form
                :data="currentTareForm"
                labelAlign="top"
                class="info-form"
              >
                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      :label="receiptType === 'inbound' ? '称皮单编号' : '称重单编号'"
                      name="weightNo"
                    >
                      <t-input
                        v-model="currentTareForm.weightNo"
                        :readonly="currentTareReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      :label="receiptType === 'inbound' ? '称皮员' : '称重员'"
                      name="addUserName"
                    >
                      <t-input
                        v-model="currentTareForm.addUserName"
                        :readonly="currentTareReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      :label="receiptType === 'inbound' ? '称皮时间' : '称重时间'"
                      name="addTime"
                    >
                      <t-date-picker
                        v-model="currentTareForm.addTime"
                        type="datetime"
                        enableTimePicker
                        :readonly="currentTareReadonly"
                        style="width:100%;"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="重量"
                      name="weight"
                    >
                      <t-input
                        v-model="currentTareForm.weight"
                        :readonly="currentTareReadonly"
                        suffix="千克"
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="12"
                    class="form-col"
                  >
                    <t-form-item
                      label="备注"
                      name="remark"
                    >
                      <t-textarea
                        v-model="currentTareForm.remark"
                        :readonly="currentTareReadonly"
                        :maxlength="160"
                        show-limit-number
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <div style="display: flex;">
                  <t-form-item
                    label="车头照片"
                    name="vehicleFrontPic"
                    class="img-item"
                    style="margin-right: 15px;"
                  >
                    <div class="img-box">
                      <t-image-viewer
                        :images="[currentTareForm.vehicleFrontPic]"
                        :z-index="10000"
                      ></t-image-viewer>
                    </div>
                  </t-form-item>
                  <t-form-item
                    label="车尾照片"
                    name="vehicleBackPic"
                    class="img-item"
                  >
                    <div class="img-box">
                      <t-image-viewer
                        :images="[currentTareForm.vehicleBackPic]"
                        :z-index="10000"
                      ></t-image-viewer>
                    </div>
                  </t-form-item>
                </div>
              </t-form>
            </div>
          </div>

          <div
            v-if="showSettleTab && activeTab === 'tab-7'"
            class="settle-box"
          >
            <div
              v-if="Object.keys(settleForm).length === 0"
              class="empty-box"
            >
              <t-empty>
                <template #image>
                  <img
                    src="@/assets/sampling/empty.png"
                    style="width: 120px; object-fit: contain;"
                  >
                </template>
              </t-empty>
            </div>
            <div
              v-else
              class="forNewclass"
            >
              <t-form
                :data="settleForm"
                labelAlign="top"
                class="info-form"
              >
                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="结算单编号"
                      name="balanceNo"
                    >
                      <t-input
                        v-model="settleForm.balanceNo"
                        :readonly="settleReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="毛重"
                      name="grossWeight"
                    >
                      <t-input
                        v-model="settleForm.grossWeight"
                        :readonly="settleReadonly"
                        suffix="千克"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="皮重"
                      name="tareWeight"
                    >
                      <t-input
                        v-model="settleForm.tareWeight"
                        :readonly="settleReadonly"
                        suffix="千克"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="净重"
                      name="netWeight"
                    >
                      <t-input
                        v-model="settleForm.netWeight"
                        :readonly="settleReadonly"
                        suffix="千克"
                      />
                    </t-form-item>
                  </t-col>

                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="标准单价"
                      name="price"
                    >
                      <t-input
                        v-model="settleForm.price"
                        :readonly="settleReadonly"
                        suffix="元"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="扣量合计"
                      name="deductWeightTotal"
                    >
                      <t-input
                        v-model="settleForm.deductWeightTotal"
                        :readonly="settleReadonly"
                        suffix="千克"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="结算单价"
                      name="balancePrice"
                    >
                      <t-input
                        v-model="settleForm.balancePrice"
                        :readonly="settleReadonly"
                        suffix="元"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="结算重量"
                      name="balanceWeight"
                    >
                      <t-input
                        v-model="settleForm.balanceWeight"
                        :readonly="settleReadonly"
                        suffix="千克"
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >

                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="结算金额"
                      name="balanceAmount"
                    >
                      <t-input
                        v-model="settleForm.balanceAmount"
                        :readonly="settleReadonly"
                        suffix="元"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="账户身份证"
                      name="idCard"
                    >
                      <t-input
                        v-model="settleForm.idCard"
                        :readonly="settleReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="联系方式"
                      name="linkPhone"
                    >
                      <t-input
                        v-model="settleForm.linkPhone"
                        :readonly="settleReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="开户银行"
                      name="bankName"
                    >
                      <t-input
                        v-model="settleForm.bankName"
                        :readonly="settleReadonly"
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="开户行号"
                      name="bankCode"
                    >
                      <t-input
                        v-model="settleForm.bankCode"
                        :readonly="settleReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="开户账号"
                      name="bankAccount"
                    >
                      <t-input
                        v-model="settleForm.bankAccount"
                        :readonly="settleReadonly"
                      />
                    </t-form-item>
                  </t-col>
                </t-row>
              </t-form>
            </div>
          </div>

          <div
            v-if="showLogoutTab && activeTab === 'tab-8'"
            class="logout-box"
          >
            <div
              v-if="Object.keys(logoutForm).length === 0"
              class="empty-box"
            >
              <t-empty>
                <template #image>
                  <img
                    src="@/assets/sampling/empty.png"
                    style="width: 120px; object-fit: contain;"
                  >
                </template>
              </t-empty>
            </div>
            <div
              v-else
              class="forNewclass"
            >
              <t-form
                :data="logoutForm"
                labelAlign="top"
                class="info-form"
              >
                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="登出单编号"
                      name="outNo"
                    >
                      <t-input
                        v-model="logoutForm.outNo"
                        :readonly="logoutReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="登出类型"
                      name="jobType"
                    >
                      <t-select
                        v-model="logoutForm.jobType"
                        :readonly="logoutReadonly"
                        :options="jobTypeOptions"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="登出时间"
                      name="addTime"
                    >
                      <t-date-picker
                        v-model="logoutForm.addTime"
                        type="datetime"
                        enableTimePicker
                        :readonly="logoutReadonly"
                        style="width:100%;"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="停留时长"
                      name="stayTime"
                    >
                      <t-input
                        v-model="logoutForm.stayTime"
                        :readonly="logoutReadonly"
                        suffix="分钟"
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-row
                  class="form-row"
                  :gutter="80"
                >
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="车牌号"
                      name="licensePlate"
                    >
                      <t-input
                        v-model="logoutForm.licensePlate"
                        :readonly="logoutReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="承运人"
                      name="carrierName"
                    >
                      <t-input
                        v-model="logoutForm.carrierName"
                        :readonly="logoutReadonly"
                      />
                    </t-form-item>
                  </t-col>
                  <t-col
                    :span="3"
                    class="form-col"
                  >
                    <t-form-item
                      label="登记员"
                      name="registrationUser"
                    >
                      <t-input
                        v-model="logoutForm.registrationUser"
                        :readonly="logoutReadonly"
                      />
                    </t-form-item>
                  </t-col>
                </t-row>

                <t-form-item
                  label="登记车辆照片"
                  name="pic"
                  class="img-item"
                >
                  <div class="img-box">
                    <t-image-viewer
                      :images="[logoutForm.pic]"
                      :z-index="10000"
                    ></t-image-viewer>
                  </div>
                </t-form-item>

              </t-form>
            </div>
          </div>
        </div>
      </t-loading>

    </div>

  </div>
</template>

<script setup name="commonDetail">
import { computed, onMounted, ref, getCurrentInstance } from "vue";
import { MessagePlugin } from "tdesign-vue-next";
import { RollbackIcon, CloseCircleIcon } from "tdesign-icons-vue-next";
import {
  taskDetail,
  queryRegistration,
  querySampleItem,
  queryQualityItem,
  queryWeightItem,
  queryWarehouseItem,
  queryBalance,
  queryRegistrationOut,
} from "@/api/receiptInbound.ts";
import { getDict } from "@/api/sampling.ts";
import { granaryList } from "@/api/receiptSearch.ts";
import { useRouter } from "vue-router";
const router = useRouter();

const { proxy } = getCurrentInstance();

const taskId = ref(null);
const receiptType = ref("inbound"); // 'inbound' for 入库, 'outbound' for 出库

const taskForm = ref({});
const taskFormRef = ref(null);
const taskRules = ref({});
const taskFormReadonly = ref(true);

// Tab 加载状态
const tabLoading = ref(false);

// 入库作业状态选项
const inboundJobStatusOptions = [
  { label: "待扦样", value: "logIn" },
  { label: "待质检", value: "sampled" },
  { label: "待称重", value: "inspected" },
  { label: "待值仓", value: "weighed" },
  { label: "待称皮", value: "warehoused" },
  { label: "待结算", value: "tareWeighed" },
  { label: "待销卡", value: "pendCard" },
  { label: "已结束", value: "logout" },
];

// 出库作业状态选项
const outboundJobStatusOptions = [
  { label: "待称皮", value: "logIn" },
  { label: "待值仓", value: "weighed" },
  { label: "待称重", value: "warehoused" },
  { label: "待结算", value: "tareWeighed" },
  { label: "待销卡", value: "pendCard" },
  { label: "已结束", value: "logout" },
];

// 获取当前作业状态选项（根据作业类型）
const currentJobStatusOptions = computed(() => {
  return receiptType.value === "inbound"
    ? inboundJobStatusOptions
    : outboundJobStatusOptions;
});

// 入库标签页列表
const inboundTabList = [
  { label: "登记信息", value: "tab-1" },
  { label: "扦样信息", value: "tab-2" },
  { label: "质检信息", value: "tab-3" },
  { label: "称重信息", value: "tab-4" },
  { label: "值仓信息", value: "tab-5" },
  { label: "称皮信息", value: "tab-6" },
  { label: "结算信息", value: "tab-7" },
  { label: "登出信息", value: "tab-8" },
];

// 出库标签页列表
const outboundTabList = [
  { label: "登记信息", value: "tab-1" },
  { label: "称皮信息", value: "tab-4" }, // 出库先称皮(weightType=2)
  { label: "值仓信息", value: "tab-5" },
  { label: "称重信息", value: "tab-6" }, // 出库后称重(weightType=1)
  { label: "结算信息", value: "tab-7" },
  { label: "登出信息", value: "tab-8" },
];

// 获取当前标签页列表（根据作业类型）
const currentTabList = computed(() => {
  return receiptType.value === "inbound" ? inboundTabList : outboundTabList;
});

// 是否显示各个标签页（根据作业类型）
const showSamplingTab = computed(() => receiptType.value === "inbound");
const showQualityTab = computed(() => receiptType.value === "inbound");
const showWeighTab = computed(() => true); // 入库显示称重(4)，出库显示称皮(4)
const showWarehouseTab = computed(() => true); // 入库显示值仓(5)，出库显示值仓(5)
const showTareTab = computed(() => true); // 入库显示称皮(6)，出库显示称重(6)
const showSettleTab = computed(() => true);
const showLogoutTab = computed(() => true);

// 当前活动 tab
const activeTab = ref("tab-1");

// 每个表单的数据与只读开关（默认只读 true）
const registerForm = ref({});
const registerReadonly = ref(true);

const samplingForm = ref({});
const samplingReadonly = ref(true);

const qualityForm = ref({});
const qualityReadonly = ref(true);

const weighForm = ref({});
const weighReadonly = ref(true);

const warehousForm = ref({});
const warehousReadonly = ref(true);

const tareForm = ref({});
const tareReadonly = ref(true);

const settleForm = ref({});
const settleReadonly = ref(true);

const logoutForm = ref({});
const logoutReadonly = ref(true);

// 下拉选项示例
const sampleTypeOptions = ref([{ label: "人工", value: 1 }]);
const grainVarietyOptions = ref([]);
const jobTypeOptions = ref([
  { label: "入库", value: 1 },
  { label: "出库", value: 2 },
]);
const grainYearOptions = ref([]);
const grainGradeOptions = ref([]);
const grainStorageOptions = ref([]);
const ticketTypeOptions = ref([]);

// Computed properties for dynamic form binding
const currentWeighForm = computed(() => {
  return receiptType.value === "inbound" ? weighForm.value : tareForm.value;
});

const currentTareForm = computed(() => {
  return receiptType.value === "inbound" ? tareForm.value : weighForm.value;
});

const currentWeighReadonly = computed(() => {
  return receiptType.value === "inbound"
    ? weighReadonly.value
    : tareReadonly.value;
});

const currentTareReadonly = computed(() => {
  return receiptType.value === "inbound"
    ? tareReadonly.value
    : weighReadonly.value;
});

// 标签切换（异步+Loading控制）
const tabChange = async (value) => {
  tabLoading.value = true;
  try {
    switch (value) {
      case "tab-1":
        await getRegisterForm();
        break;
      case "tab-2":
        if (showSamplingTab.value) {
          await getSamplingForm();
        }
        break;
      case "tab-3":
        if (showQualityTab.value) {
          await getQualityForm();
        }
        break;
      case "tab-4":
        await getWeightOrTareData(4);
        break;
      case "tab-5":
        await getWarehousForm();
        break;
      case "tab-6":
        await getWeightOrTareData(6);
        break;
      case "tab-7":
        await getSettleForm();
        break;
      case "tab-8":
        await getLogoutForm();
        break;
      default:
        break;
    }
  } finally {
    tabLoading.value = false;
  }
};

// 统一获取称重/称皮数据
const getWeightOrTareData = async (tabKey) => {
  let weightType = 1;
  if (receiptType.value === "inbound") {
    weightType = tabKey === 4 ? 1 : 2;
  } else {
    weightType = tabKey === 4 ? 2 : 1;
  }

  const response = await queryWeightItem(taskId.value, weightType);
  if (response?.status?.code != "200") return;

  const result = response.result;
  if (!result) {
    if (weightType === 1) {
      weighForm.value = {};
    } else {
      tareForm.value = {};
    }
    return;
  }

  result.vehicleFrontPic = await proxy?.$getPic(result.vehicleFrontPic);
  result.vehicleBackPic = await proxy?.$getPic(result.vehicleBackPic);

  if (weightType === 1) {
    weighForm.value = result;
  } else {
    tareForm.value = result;
  }
};

const getTaskForm = async () => {
  const response = await taskDetail(taskId.value);

  if (response?.status?.code != "200") return;

  const result = response.result;
  if (!response.result) {
    taskForm.value = {};
    return;
  }
  result.ticketType = Number(response.result.ticketType) || "";
  result.jobType = Number(response.result.jobType) || "";
  if (result.jobStatus === "settled") {
    result.jobStatus = "pendCard";
  }

  taskForm.value = result;

  // 根据作业类型设置当前类型
  if (result.jobType === 1) {
    receiptType.value = "inbound";
  } else if (result.jobType === 2) {
    receiptType.value = "outbound";
  }
};

// tab1 - 登记信息
const getRegisterForm = async () => {
  const response = await queryRegistration(taskId.value);

  if (response?.status?.code != "200") return;

  const result = response.result;
  if (!result) {
    registerForm.value = {};
    return;
  }

  result.pic = await proxy.$getPic(result.pic);

  registerForm.value = result;
};

// tab2 - 扦样信息
const getSamplingForm = async () => {
  const response = await querySampleItem(taskId.value);

  if (response?.status?.code != "200") return;

  const result = response.result;
  if (!result) {
    samplingForm.value = {};
    return;
  }
  samplingForm.value = result;
};

// tab3 - 质检信息
const getQualityForm = async () => {
  const response = await queryQualityItem(taskId.value);

  if (response?.status?.code != "200") return;
  const result = response.result;
  if (!result) {
    qualityForm.value = {};
  }

  result.grainGrade = String(result.grainGrade);
  result.warehouseId = String(result.warehouseId);
  if (result.qualityResult == 1) {
    result.qualityResult = "合格";
  } else if (result.qualityResult == 2) {
    result.qualityResult = "不合格";
  } else {
    result.qualityResult = "未知";
  }
  qualityForm.value = result;
};

// tab5 - 值仓信息
const getWarehousForm = async () => {
  const response = await queryWarehouseItem(taskId.value);

  if (response?.status?.code != "200") return;

  const result = response.result;
  if (!result) {
    warehousForm.value = {};
    return;
  }
  warehousForm.value = result;
};

// tab7 - 结算信息
const getSettleForm = async () => {
  const response = await queryBalance(taskId.value);

  if (response?.status?.code != "200") return;

  const result = response.result;
  if (!result || !result.balanceNo) {
    settleForm.value = {};
    return;
  }
  settleForm.value = result;
  settleForm.value.payMethod = "银行卡支付";
};

// tab8 - 登出信息
const getLogoutForm = async () => {
  const response = await queryRegistrationOut(taskId.value);

  if (response?.status?.code != "200") return;

  const result = response.result;
  if (!result || !result.outNo) {
    logoutForm.value = {};
    return;
  }

  result.pic = await proxy.$getPic(result.pic);

  logoutForm.value = result;
};

// 车牌类型
const getTicketTypeOptions = () => {
  getDict("LICENCE_PLATE_TYPE")
    .then((res) => {
      const data = res.result.listDictData || [];
      ticketTypeOptions.value = data.map((item) => ({
        label: item.label,
        value: item.description,
      }));
    })
    .catch((error) => {});
};

// 粮食品种
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
        value: item.description,
      }));
    })
    .catch((error) => {});
};

// 获取粮仓号
const getGranaryOptions = async () => {
  let response = await granaryList({});
  if (response.status.code === "200") {
    response.result.data.forEach((item) => {
      grainStorageOptions.value.push({
        value: item.id,
        label: item.warehouseName,
      });
    });
  }
};

onMounted(() => {
  taskId.value = router.currentRoute.value.query.taskId;
  receiptType.value = router.currentRoute.value.query.type || "inbound";

  getTicketTypeOptions();
  getGrainVarietyOptions();
  getGrainYearOptions();
  getGrainGradeOptions();
  getGranaryOptions();

  getTaskForm();
  getRegisterForm();
});
</script>

<style lang="less">
.my-icon-btn .t-button__text {
  display: flex;
  justify-content: center;
  align-items: center;

  .t-icon {
    margin-right: 6px;
  }
}

/* 新增重置按钮样式 */
.reset-btn.t-button.t-button--theme-primary.t-button--variant-outline {
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
</style>

<style lang="less" scoped>
.container {
  width: 100%;
}

.title-back {
  margin-bottom: 15px;
}

.title {
  font-size: 16px;
  font-weight: 500;
  font-family: PingFang SC;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #000;
  padding-top: 4px;

  img {
    height: 24px;
    object-fit: contain;
  }
}

.content {
  width: 100%;
  border-radius: 4px;
  flex: 0 0 auto;
  display: flex;
  flex-direction: column;
  padding: 15px;
  background-color: #fff;

  .title-back {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title {
      flex-shrink: 0;
    }
  }

  ::v-deep .info-form {
    width: 100%;

    .t-textarea__inner {
      font-size: 14px;
      font-family: PingFang SC;
      min-height: 64px;
    }

    .form-row {
      margin-bottom: 15px;

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

    .img-item {
      .img-box {
        width: 110px;
        height: 110px;
        border: 1px solid #ddd;
        border-radius: 4px;

        img {
          width: 100%;
          height: 100%;
          object-fit: contain;
        }
      }
    }

    .video-item {
      .video-empty {
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        color: #333;
        font-size: 14px;
        font-family: PingFang SC;
        background-color: #f8fafc;
        width: 220px;
        height: 165px;
        border: 1px solid #ddd;
        border-radius: 4px;
      }
    }
  }

  .empty-box {
    width: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 300px;

    :deep(.t-empty) {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
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
}
</style>