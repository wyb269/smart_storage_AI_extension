<template>
  <div class="forNewclass">
    <t-row :gutter="[25, 25]">
      <t-col :span="9">
        <div class="weighLeft bx">
          <div class="hd">
            <div class="tt"><img src="@/assets/imgs/zcgl-1.png"><span class="wtit">值仓管理</span></div>
            <!-- <span class="lc">
              <t-tag theme="warning" variant="outline" class="tg">当前流程 : 待称重</t-tag>
            </span> -->
            <div>
              <readCard @result="result" />
            </div>
          </div>
          <div class="weighCon">
          </div>


          <t-form :label-width="70" labelAlign="top">
            <div class="weighCon" style="width: 100%;padding: 0px;">

              <div class="weighInp">

                <t-row :gutter="[65, 4]">
                  <t-col :span="4">
                    <t-form-item label="智能卡编号" class="inp">
                      <t-input v-model="cardForm.cardNo" placeholder="请刷磁卡" disabled />
                    </t-form-item>
                  </t-col>
                  <t-col :span="4">
                    <t-form-item label="承运人" class="inp">
                      <t-input placeholder="请刷磁卡" v-model="cardForm.carrierName" disabled />
                    </t-form-item>
                  </t-col>
                  <t-col :span="4">
                    <t-form-item label="车牌号" class="inp">
                      <t-input placeholder="请刷磁卡" v-model="cardForm.licensePlate" disabled />
                    </t-form-item>
                  </t-col>

                  <t-col :span="12">
                    <t-form-item label="客户名称" class="inp">
                      <t-input v-model="cardForm.customerName" placeholder="请刷磁卡" disabled />
                    </t-form-item>
                  </t-col>


                </t-row>

              </div>

            </div>
          </t-form>
          <div style="margin-top:5px;">
            <t-form :label-width="70" labelAlign="top" ref="formValidator" :data="formData" :rules="FORM_RULES"
              @submit="onSubmit">
              <t-row :gutter="[65, 4]">
                <t-col :span="4">
                  <t-form-item label="作业编号">
                    <t-input v-model="cardForm.jobNo" placeholder="请刷磁卡" disabled />
                  </t-form-item>
                </t-col>
                <t-col :span="4">
                  <t-form-item label="粮仓号">
                    <t-input v-model="cardForm.warehouseLabel" placeholder="请刷磁卡" disabled />
                  </t-form-item>
                </t-col>
                <t-col :span="4">
                  <t-form-item label="满车称重">
                    <t-input v-model="cardForm.grossWeight" suffix="千克" placeholder="请刷磁卡" disabled />
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

                <t-col :span="4">
                  <t-form-item label="现场扣重" name="deductWeight" class="f14">
                    <t-input v-model="formData.deductWeight" suffix="千克" placeholder="请输入" />
                  </t-form-item>
                </t-col>


                <t-col :span="4">
                  <t-form-item label="库管员">
                    <t-input v-model="cardForm.name" placeholder="请刷磁卡" disabled />
                  </t-form-item>
                </t-col>





                <t-col :span="12">
                  <t-form-item label="备注" class="f14" style="margin-top: 7px;">
                    <t-textarea v-model="formData.remark" :height="124" placeholder="请输入" />
                  </t-form-item>
                </t-col>

                <t-col :span="12" style="margin-top: 15px;padding-bottom: 20px;">
                  <div style="text-align: center;">
                    <t-button variant="base" theme="warning" class="czbt" type="submit">
                      <template #icon>
                        <!-- <t-icon name="undertake-delivery-filled" style="margin-right: 3px;" /> -->
                        <img style="margin-right: 10px;" src="@/assets/imgs/wareSave.png">
                      </template>
                      保存</t-button>
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


          <div style="text-align: center;padding-top: 138px;">
            <img style="width: 220px;" src="@/assets/imgs/empty.png">
            <div style="font-family: PingFang SC;
font-weight: 300;
font-style: Regular;
font-size: 13px;
leading-trim: NONE;
line-height: 100%;
letter-spacing: 0px;
color:rgba(149, 149, 149, 1);
text-align: center;
">暂无数据</div>
          </div>


        </div>
      </t-col>
    </t-row>

    <div class="weighTb bx">
      <div class="weighTbhd">
        <img src="@/assets/imgs/jjcz-3.png" style="margin-right: 7px;margin-top: 3px;" />
        今日值仓记录
      </div>

      <div class="hislist">
        <common-table justList="1" tbMt="0" :columns="columns" :tableData="tableData" :pagination="pagination"
          @changePage="changePage">

          <!-- 列表操作侧按钮插槽 -->
          <!-- <template #btnAry="slotProps">

      <t-link theme="success" @click="handleClickDetail(slotProps.row.row)">编辑</t-link>

    </template> -->
        </common-table>
      </div>

      <t-dialog v-model:visible="confirmVisible" header="确认提交" class="newDialog newclassNewDialog"
        :cancelBtn="{ theme: 'default', variant: 'outline' }" :confirmBtn="{ theme: 'warning', variant: 'base' }"
        @confirm="onConfirm">
        <t-space direction="vertical" style="width: 100%">
          <div class="deletCon">
            <!-- <div class="delectConHeader"> <span class="a">确认删除该角色</span></div> -->
            <div class="b"
              style="display: flex;align-items: center; justify-content: center;    margin-bottom: -10px; margin-top: 9px;">
              <t-icon style="margin-right:5px;" name="error-circle-filled" size="18px" color="rgba(255, 41, 75, 1)" />
              表单一旦提交，将影响后续流程，请谨慎操作
            </div>
          </div>
        </t-space>
      </t-dialog>

    </div>
  </div>
</template>
<script setup lang="jsx">
import readCard from '@/components/readCard/index.vue';
import CommonTable from '@/components/common-table/index.vue';
import { ref, defineComponent, getCurrentInstance, onMounted, nextTick } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/store';
const userStore = useUserStore();
const { proxy } = getCurrentInstance();
import { listWare, saveWare, swipeCard, queryTask, detailWare } from '@/api/weigh';
import { getDict } from "@/api/sampling.ts";
import { listWarehouseByModal } from "@/api/quality.ts";

const confirmStu = ref('2')  //  2确认提交
const confirmVisible = ref(false)
const formValidator = ref(null);
const onConfirm = async () => {
  // 确认提交
  let res = await saveWare(formData.value)
  if (res.status.code == 200) {
    MessagePlugin.success(res.status.message);
    pagination.value.defaultCurrent = 1
    init()
  } else {
    MessagePlugin.error(res.status.message);
  }
  formData.value = {
    "taskId": '',
    "deductWeight": '',
    "remark": ''
  }
  cardForm.value = {}
  formValidator.value.reset()
  confirmVisible.value = false
}



const onSubmit = async (ctx) => {
  if (ctx.validateResult === true) {
    if (!formData.value.taskId) {
      MessagePlugin.error('请刷磁卡!');
      return
    }
    confirmStu.value = 2
    // confirmVisible.value = true
    onConfirm()
  }

}

const formData = ref({
  "taskId": '',
  "deductWeight": '',
  "remark": ''
})
const FORM_RULES = ref({
  deductWeight: [{ required: true, message: '请录入重量', type: 'error' }]
})

const cardForm = ref({})
const result = r => {

  console.log(r)
  //  r.result  0失败   1成功
  if (r.result == 1) {
    cardForm.value.card = r.data
    //  cardForm.value.card = '04A3654A240189'
    //  cardForm.value.card = '04B39947240189'
    swipeCard({
      "physicalCardNumber": cardForm.value.card,
      "processStep": 'warehoused'
    }).then(rs => {
      if (rs.status.code == 200) {
        detailWare(rs.result.taskId).then(nt => {
          if (nt.status.code == 200) {
            formData.value.taskId = rs.result.taskId

            cardForm.value.grossWeight = nt.result.grossWeight
            //粮仓号
            if(nt.result.grainVariety){
              getGrainStorageOptions(nt.result.grainVariety,nt.result.warehouseId,cardForm.value)
            }
            //粮食品种
            grainVarietyOptions.value.map(x => {
              if (x.value == nt.result.grainVariety) cardForm.value.varityLabel = x.label
            })
            //粮食等级
            grainGradeOptions.value.map(x => {
              if (x.value == nt.result.grainGrade) cardForm.value.gradeLabel = x.label
            })

          } else {
            MessagePlugin.error(nt.status.message);
          }
        })
        queryTask(rs.result.taskId).then(dt => {
          if (dt.status.code == 200) {
            cardForm.value.cardNo = dt.result.smartCardNo
            cardForm.value.jobNo = dt.result.jobNo
            cardForm.value.carrierName = dt.result.carrierName
            cardForm.value.licensePlate = dt.result.licensePlate
            cardForm.value.customerName = dt.result.customerName
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
    //  ellipsis: true,
    align: 'left',
    colKey: 'serial-number',
  },
  {
    title: '智能卡编号', colKey: 'cardNo',
    width: '140px'
  },

  {
    title: '车牌号', colKey: 'licensePlate',
  },


  {
    title: '粮仓号', colKey: 'warehouseLabel',
  },


  {
    title: '满车称重', colKey: 'grossWeight',
    cell: (h, { row }) => {
      return <p>
        {row.grossWeight}kg
      </p>
        ;
    },
  },
  {
    title: '粮食品种', colKey: 'varityLabel',
  },
  {
    title: '粮食等级', colKey: 'gradeLabel',
  },
  {
    title: '现场扣重', colKey: 'deductWeight',
    cell: (h, { row }) => {
      return <p>
        {row.deductWeight}kg
      </p>
        ;
    },
  },
  {
    title: '值仓单编号', colKey: 'warehouseNo',
    width: '140px'
  },

  {
    title: '值仓单状态', colKey: 'cardNo',
    cell: (h, { row }) => {
      return <p>
        {row.state == '1' ? '正常' : '作废'}
      </p>
        ;
    },
  },
  {
    title: '作业类型', colKey: 'jobType',
    cell: (h, { row }) => {
      return <p>
        {row.jobType == '1' ? '入库' : '出库'}
      </p>
        ;
    },
  },
  {
    title: '时间', colKey: 'addTime',
    // ellipsis: true,
    width: '180px'
  },
])

const init = () => {
  let p = {
    pageNo: pagination.value.defaultCurrent,
    pageSize: pagination.value.defaultPageSize,

  }
  listWare(p).then(res => {
    if (res.status.code === '200') {
      tableData.value = res.result.data.map(data => {

        //粮仓号
        getGrainStorageOptions(data.grainVariety,data.warehouseId,data)
        //粮食品种
        grainVarietyOptions.value.map(x => {
          if (x.value == data.grainVariety) data.varityLabel = x.label
        })
        //粮食等级
        grainGradeOptions.value.map(x => {
          if (x.value == data.grainGrade) data.gradeLabel = x.label
        })
        return data
      })
      pagination.value.total = res.result.total;
    } else {
      MessagePlugin.error(res.status.message);
    }
  });
};



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
const getGrainStorageOptions =async (grainVariety,id,obj) => {
    if(!grainVariety){
      obj.warehouseLabel=''
      return
    }
    let response = await listWarehouseByModal({
      grainVariety:grainVariety,
      pageNo:1,
      pageSize:0,
    });
    if (response.status.code !== "200") {
      MessagePlugin.error(response.status.message || "仓库列表获取失败");
      return;
    }
    let data = response.result.data || [];
    data.map(x=>{
      if(x.id==id)obj.warehouseLabel=x.warehouseName
    })
};
onMounted(() => {
  cardForm.value.name = userStore.userInfo.user.name
  getGrainVarietyOptions();
  getGrainGradeOptions();
  init();
})
</script>
<style lang="less" scoped>
.bx {
  // box-shadow: 0 1px 4px rgba(38, 47, 62, 0.2);
  border-radius: 5px;
}

.weighLeft {
  padding: 15px;
  background: #fff;
  height: 588px;

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

      img {
        margin-top: 1px;
      }
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
    padding: 0px 0px 8px;


    .weighInp {
      width: 100%;


      // .inp {
      //   // width: 33.33%;
      //   padding-right: 15px;
      // }
    }
  }
}

:deep(.weighCon .t-form__label) {
  // padding-right: 15px;
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
  height: 588px;
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
</style>
