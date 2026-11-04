<template>
  <div class="forNewclass">
    <t-row :gutter="[25, 25]">
      <t-col :span="9">
        <div class="weighLeft bx">
          <div class="hd">
            <div class="tt"><img src="@/assets/imgs/jjcz-1.png"><span class="wtit">检斤称重(出库)</span></div>
            <div>
              <readCard @result="result" />
            </div>
          </div>
          <div class="weighCon">
          </div>

          <t-form :label-width="70" labelAlign="top">
            <div class="weighCon" style="padding: 0px;padding-top: 2px;">
              <div class="weighInp" style="width: 100%;">
                <t-row :gutter="[65, 12]">
                  <t-col :span="6">
                    <t-form-item label="智能卡编号" class="inp">
                      <t-input v-model="cardForm.cardNo" placeholder="请刷磁卡" disabled />
                    </t-form-item>
                  </t-col>
                  <t-col :span="6">
                    <t-form-item label="承运人" class="inp">
                      <t-input placeholder="请刷磁卡" v-model="cardForm.carrierName" disabled />
                    </t-form-item>
                  </t-col>
                  <t-col :span="6">
                    <t-form-item label="车牌号" class="inp">
                      <t-input placeholder="请刷磁卡" v-model="cardForm.licensePlate" disabled />
                    </t-form-item>
                  </t-col>
                  <t-col :span="6">
                    <t-form-item label="客户名称">
                      <t-input v-model="cardForm.customerName" placeholder="请刷磁卡" disabled />
                    </t-form-item>
                  </t-col>
                </t-row>
              </div>
            </div>
          </t-form>
          <div style="margin-top:13px;">
            <t-form :label-width="70" labelAlign="top" ref="formValidator" :data="formData" :rules="FORM_RULES"
              @submit="onSubmit">
              <t-row :gutter="[65, 12]">
                <t-col :span="4">
                  <t-form-item label="作业编号">
                    <t-input v-model="cardForm.jobNo" placeholder="请刷磁卡" disabled />
                  </t-form-item>
                </t-col>

                <t-col :span="4">
                  <t-form-item label="重量(kg)" name="weight" class="f14">
                    <t-input v-model="formData.weight" placeholder="接地磅默认回显数据" />
                  </t-form-item>
                </t-col>

                <t-col :span="4">
                  <t-form-item label="检斤员">
                    <t-input v-model="cardForm.name" placeholder="请刷磁卡" disabled />
                  </t-form-item>
                </t-col>

                <t-col :span="4">
                  <t-form-item label="仓库名称">
                    <t-input v-model="cardForm.warehouseLabel" placeholder="请刷磁卡" disabled />
                  </t-form-item>
                </t-col>

                <t-col :span="4">
                  <t-form-item label="粮食品种">
                    <t-input v-model="cardForm.varityLabel" placeholder="请刷磁卡" disabled />
                  </t-form-item>
                </t-col>

                <t-col :span="4">
                  <t-form-item label="粮食等级">
                    <t-input v-model="cardForm.gradeLabel" placeholder="请刷磁卡" disabled />
                  </t-form-item>
                </t-col>

                <t-col :span="12">
                  <t-form-item label="备注" class="f14">
                    <t-textarea v-model="formData.remark" :height="124" placeholder="请输入称重备注或保安意见" />
                  </t-form-item>
                </t-col>

                <t-col :span="12" style="margin-top: 38px;padding-bottom: 20px;">
                  <div style="text-align: center;">
                    <t-button variant="base" theme="warning" class="czbt" type="submit">
                      <template #icon>
                        <img style="margin-right: 10px;" src="@/assets/imgs/weighSumt.png">
                      </template>
                      确认称重</t-button>
                  </div>
                </t-col>
              </t-row>
            </t-form>
          </div>
        </div>
      </t-col>
      <t-col :span="3">
        <div class="weighPh bx">
          <div class="phhd">
            <img src="@/assets/imgs/jjcz-2.png">
            <span>视频监控区域</span>
          </div>
          <div class="toptip">
            <div class="text-ellipsis ">
              <t-icon name="error-circle-filled" size="15px" />
              称重图片使用【文件上传】功能进行上传时，需要分别上传车辆前后两个角度的照片!
            </div>
          </div>
          <uploadCom ref="upCom" v-if="freshPic" @deletePic="deletePic" @upPic="upPic" :formData="formData" />
        </div>
      </t-col>
    </t-row>

    <div class="weighTb bx">
      <div class="weighTbhd">
        <img src="@/assets/imgs/jjcz-3.png" style="margin-right: 7px;margin-top: 3px;" />
        今日检斤记录
      </div>

      <div class="hislist">
        <common-table justList="1" tbMt="0" :columns="columns" :tableData="tableData" :pagination="pagination"
          @changePage="changePage">
        </common-table>
      </div>

      <t-dialog v-model:visible="confirmVisible" :header="confirmStu == 2 ? '确认提交' : '删除确认'"
        class="newDialog newclassNewDialog" :cancelBtn="{ theme: 'default', variant: 'outline' }"
        :confirmBtn="{ theme: 'warning', variant: 'base' }" @confirm="onConfirm">
        <t-space direction="vertical" style="width: 100%">
          <div class="deletCon">
            <div class="b"
              style="display: flex;align-items: center; justify-content: center;    margin-bottom: -10px; margin-top: 9px;">
              <t-icon style="margin-right:5px;" name="error-circle-filled" size="18px" color="rgba(255, 41, 75, 1)" />
              {{ confirmStu == 2 ? '表单一旦提交，将影响后续流程，请谨慎操作。' : '照片一旦删除，需重新上传，请谨慎操作。' }}
            </div>
          </div>
        </t-space>
      </t-dialog>

    </div>
  </div>
</template>
<script setup lang="jsx">
import readCard from '@/components/readCard/index.vue';
import uploadCom from './components/uploadCom.vue';
import CommonTable from '@/components/common-table/index.vue';
import { ref, getCurrentInstance, onMounted, nextTick } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';
import { useUserStore } from '@/store';
const userStore = useUserStore();
const { proxy } = getCurrentInstance();
import { listOutWeigh, saveOutWeigh, outSwipeCard, outQueryTask, outDetailForForm } from '@/api/outWeigh';
import { getDict } from "@/api/sampling.ts";

const confirmStu = ref('0')  // 0 前照片   1 后照片   2确认提交
const confirmVisible = ref(false)
const deletePic = (n) => {
  confirmStu.value = n
  confirmVisible.value = true
}

const upPic = (data) => {
  if (data.derct == 0) formData.value.vehicleFrontPic = data.objectKey
  if (data.derct == 1) formData.value.vehicleBackPic = data.objectKey
}
const freshPic = ref(true)
const formValidator = ref(null);
const onConfirm = async () => {
  // 确认提交
  if (confirmStu.value == 2) {
    let res = await saveOutWeigh(formData.value)
    if (res.status.code == 200) {
      MessagePlugin.success(res.status.message);
      pagination.value.defaultCurrent = 1
      init()
    } else {
      MessagePlugin.error(res.status.message);
    }
    formData.value = {
      "taskId": '',
      "weightType": '2',
      "weight": '',
      "remark": '',
      "vehicleFrontPic": '',
      "vehicleBackPic": ''
    }
    cardForm.value = {}
    freshPic.value = false
    formValidator.value.reset()
    nextTick(() => {
      freshPic.value = true
    });
  }
  if (confirmStu.value != 2) {
    if (confirmStu.value == 0) formData.value.vehicleFrontPic = ''
    if (confirmStu.value == 1) formData.value.vehicleBackPic = ''
    MessagePlugin.success('删除成功!');
    proxy.$refs.upCom.confirmDt()
  }
  confirmVisible.value = false
}



const onSubmit = async (ctx) => {
  if (ctx.validateResult === true) {
    if (!formData.value.taskId) {
      MessagePlugin.error('请刷磁卡!');
      return
    }
    if (!formData.value.vehicleBackPic || !formData.value.vehicleFrontPic) {
      MessagePlugin.error('请上传照片!');
      return
    }
    confirmStu.value = 2
    onConfirm()
  }

}

const formData = ref({
  "taskId": '',
  "weightType": '2',
  "weight": '',
  "remark": '',
  "vehicleFrontPic": '',
  "vehicleBackPic": ''
})
const FORM_RULES = ref({
  weight: [{ required: true, message: '请录入重量', type: 'error' }]
})

// 粮食品种选项
const grainVarietyOptions = ref([]);
// 粮食等级选项
const grainGradeOptions = ref([]);
// 粮仓号选项
const grainStorageOptions = ref([]);

// 获取粮食品种
const getGrainVarietyOptions = () => {
  getDict("GRAIN_VARIETY")
    .then((res) => {
      const data = res.result.listDictData || [];
      grainVarietyOptions.value = data.map((item) => ({
        label: item.label,
        value: item.description,
      }));
    })
    .catch((error) => {
      console.error("获取粮食品种失败:", error);
    });
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
    .catch((error) => {
      console.error("获取粮食等级失败:", error);
    });
};

// 获取粮仓号
const getGrainStorageOptions = () => {
  getDict("STORAGE_WAREHOUSE_NUMBER")
    .then((res) => {
      const data = res.result.listDictData || [];
      grainStorageOptions.value = data.map((item) => ({
        label: item.label,
        value: item.description,
      }));
    })
    .catch((error) => {
      console.error("获取粮仓号失败:", error);
    });
};

const cardForm = ref({})
const result = r => {
  console.log(r)
  //  r.result  0失败   1成功
  if (r.result == 1) {
    cardForm.value.card = r.data
    outSwipeCard({
      "physicalCardNumber": cardForm.value.card,
      "processStep": 'weighed'
    }).then(rs => {
      if (rs.status.code == 200) {
        if (rs.result.jobStatus == 'weighed') {
          MessagePlugin.error('当前作业状态禁止出库称重操作');
          return
        }
        if (rs.result.jobStatus == 'warehoused') formData.value.weightType = 1
        outDetailForForm(rs.result.taskId).then(nt => {
          if (nt.status.code == 200) {
            formData.value.taskId = rs.result.taskId
            cardForm.value.cardNo = nt.result.smartCardNo
            cardForm.value.carrierName = nt.result.carrierName
            cardForm.value.licensePlate = nt.result.licensePlate
            cardForm.value.customerName = nt.result.cusName
            cardForm.value.warehouseLabel = nt.result.warehouseName
            // 粮食品种
            grainVarietyOptions.value.map(x => {
              if (x.value == nt.result.grainVariety) cardForm.value.varityLabel = x.label
            })
            // 粮食等级
            grainGradeOptions.value.map(x => {
              if (x.value == nt.result.grainGrade) cardForm.value.gradeLabel = x.label
            })
          } else {
            MessagePlugin.error(nt.status.message);
          }
        })
        outQueryTask(rs.result.taskId).then(dt => {
          if (dt.status.code == 200) {
            cardForm.value.jobNo = dt.result.jobNo
          } else {
            MessagePlugin.error(dt.status.message);
          }
        })
      } else {
        MessagePlugin.error(rs.status.message);
      }
    })


  }
}

const changePage = (p) => {
  pagination.value.defaultCurrent = p.pagination.current
  pagination.value.defaultPageSize = p.pagination.pageSize
  init()
}
const pagination = ref({
  defaultPageSize: 10,
  total: 0,
  defaultCurrent: 1,
});
const tableData = ref([])
const columns = ref([
  {
    title: '序号',
    fixed: 'left',
    width: 60,
    align: 'left',
    colKey: 'serial-number',
  },
  {
    title: '智能卡编号', colKey: 'cardNo',
  },
  {
    title: '作业单编号', colKey: 'jobNo',
  },

  {
    title: '称重类型', colKey: 'weightType',
    cell: (h, { row }) => {
      return <p>
        {row.weightType == '1' ? '毛重' : '皮重'}
      </p>
        ;
    },
  },


  {
    title: '重量', colKey: 'weight',
    cell: (h, { row }) => {
      return <p>
        {row.weight + 'Kg'}
      </p>
        ;
    },
  },
  {
    title: '称重单编号', colKey: 'weightNo',
  },

  { title: '备注', colKey: 'remark' },
  { title: '创建时间', colKey: 'addTime' },
])

const init = () => {
  let now = new Date();
  let time = now.toISOString()
  let p = {
    pageNo: pagination.value.defaultCurrent,
    pageSize: pagination.value.defaultPageSize,
    startTime: time.slice(0, 10) + " 00:00:00",
    endTime: time.slice(0, 10) + " 23:59:59",
  }
  listOutWeigh(p).then(res => {
    if (res.status.code === '200') {
      tableData.value = res.result.data
      pagination.value.total = res.result.total;
    } else {
      MessagePlugin.error(res.status.message);
    }
  });
};

onMounted(() => {
  cardForm.value.name = userStore.userInfo.user.name
  getGrainVarietyOptions();
  getGrainGradeOptions();
  getGrainStorageOptions();
  init();
})
</script>
<style lang="less" scoped>
.bx {
  border-radius: 5px;
}

.weighLeft {
  padding: 15px;
  background: #fff;
  height: 592px;

  .hd {
    display: flex;
    padding-top: 4px;
    justify-content: space-between;
    border-bottom: 1px solid #e6e6e6;
    padding-bottom: 15px;

    .tt {
      font-size: 19px;
      font-weight: 400;
      margin-right: 12px;
      display: flex;
      align-items: center;
    }

    .lc {
      .tg {
        border-radius: 3px;
        padding: 5px 10px 6px;
        background: rgba(253, 244, 236, 1);
        border: none;
        color: rgba(239, 150, 69, 1);

      }
    }
  }

  .weighCon {
    padding: 15px 0px 6px;

    .weighInp {

      .inp {}
    }
  }
}

.czbt {
  width: 300px;
  height: 36px;

  font-family: PingFang SC;
  font-weight: 500;
  font-style: Medium;
  font-size: 14px;
  leading-trim: NONE;
  letter-spacing: 0px;
  background-image: linear-gradient(to right, #f69d0b, #d97807);
  border: none;
  padding-bottom: 2px;
}

.weighPh {
  padding: 15px;
  height: 627pxpx;
  background: #fff;

  .phhd {
    display: flex;
    align-items: center;
    color: rgba(26, 26, 26, 1);
    margin-top: 4px;

    span {
      margin-left: 3px;
      font-family: PingFang SC;
      font-weight: 500;
      font-style: Medium;
      font-size: 16px;
      leading-trim: NONE;
      letter-spacing: 0px;
      margin-top: -1px;

    }
  }

  .phcon {
    display: flex;
    justify-content: space-between;

    .phCld {
      width: 48.5%;
    }
  }
}

.weighTb {
  padding: 15px 15px;
  background: #fff;
  margin-top: 22px;

  .weighTbhd {
    padding: 4px 2px 5px;
    font-size: 14px;

    font-family: PingFang SC;
    font-weight: 500;
    font-style: Medium;
    font-size: 16px;
    leading-trim: NONE;

    letter-spacing: 0px;
    color: rgba(26, 26, 26, 1);
    display: flex;
    align-items: center;

  }

  .hislist {
    padding-top: 15px;
  }
}

.wtit {
  font-family: PingFang SC;
  font-weight: 500;
  font-style: Medium;
  font-size: 16px;
  leading-trim: NONE;
  line-height: 100%;
  letter-spacing: 0px;
  color: rgba(26, 26, 26, 1);
  margin-left: 8px;
}


:deep(.f14 .t-input) {
  font-size: 14px;
}

:deep(.f14 .t-textarea__inner) {
  font-size: 14px;
}

.toptip {
  border: 0.5px solid rgba(220, 220, 220, 1);
  box-shadow: 0px 8px 10px -5px rgba(0, 0, 0, 0.08);
  border-radius: 5px;
  padding: 5px 20px;

  font-family: PingFang SC;
  font-weight: 400;
  font-style: Regular;
  font-size: 12px;
  leading-trim: NONE;
  letter-spacing: 0px;
  text-align: center;
  color: rgba(241, 147, 58, 1);
  line-height: 13px;
  margin-top: 12px;

  height: 41.23px;
  display: flex;
  justify-content: center;
  align-items: center;

  .text-ellipsis {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    /* 限制文本显示为2行 */
    overflow: hidden;
  }



}
</style>
