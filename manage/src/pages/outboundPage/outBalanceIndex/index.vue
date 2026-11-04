<template>
  <div class="forNewclass">
    <t-row :gutter="[25, 25]">
      <t-col :span="12">
        <div class="weighLeft bx">
          <div class="hd">
            <div class="tt"><img src="@/assets/imgs/dcjs-1.png"><span class="wtit">单车结算(出库)</span></div>
            <div>
              <readCard @result="result" />
            </div>
          </div>
          <div class="weighCon">
          </div>

          <t-form :label-width="70" labelAlign="top">
            <div class="weighCon" style="padding: 0px;padding-top: 2px;">
              <div class="weighInp" style="width: 100%;">
                <t-row :gutter="[65, 9]">
                  <t-col :span="3">
                    <t-form-item label="智能卡编号" class="inp">
                      <t-input v-model="cardForm.cardNo" placeholder="请刷磁卡" disabled />
                    </t-form-item>
                  </t-col>
                  <t-col :span="3">
                    <t-form-item label="承运人" class="inp">
                      <t-input placeholder="请刷磁卡" v-model="cardForm.carrierName" disabled />
                    </t-form-item>
                  </t-col>
                  <t-col :span="3">
                    <t-form-item label="车牌号" class="inp">
                      <t-input placeholder="请刷磁卡" v-model="cardForm.licensePlate" disabled />
                    </t-form-item>
                  </t-col>
                  <t-col :span="3">
                    <t-form-item label="客户名称">
                      <t-input v-model="cardForm.customerName" placeholder="请刷磁卡" disabled />
                    </t-form-item>
                  </t-col>
                </t-row>
              </div>
            </div>
          </t-form>
          <div style="margin-top:9px;">
            <t-form :label-width="70" labelAlign="top" ref="formValidator" :data="formData" :rules="FORM_RULES"
              @submit="onSubmit">
              <t-row :gutter="[65, 9]">
                <t-col :span="4">
                  <t-form-item label="作业编号">
                    <t-input v-model="cardForm.jobNo" placeholder="请刷磁卡" disabled />
                  </t-form-item>
                </t-col>

                <t-col :span="4">
                  <t-form-item label="标准单价" name="price" class="f14">
                    <t-input v-model="formData.price" suffix="(元/千克)" placeholder="请输入" />
                  </t-form-item>
                </t-col>

                <t-col :span="4">
                  <t-form-item label="毛重">
                    <t-input v-model="cardForm.grossWeight" suffix="千克" placeholder="请刷磁卡" disabled />
                  </t-form-item>
                </t-col>

                <t-col :span="4">
                  <t-form-item label="皮重">
                    <t-input v-model="cardForm.tareWeight" suffix="千克" placeholder="请刷磁卡" disabled />
                  </t-form-item>
                </t-col>

                <t-col :span="4">
                  <t-form-item label="净重">
                    <t-input v-model="cardForm.netWeight" suffix="千克" placeholder="请刷磁卡" disabled />
                  </t-form-item>
                </t-col>


                <t-col :span="12" style="">
                  <div style="display: flex;align-items: center;margin-bottom: 10px;padding-top: 3px;">
                    <img src="@/assets/imgs/dcjs-3.png" />
                    <span style="font-family: PingFang SC;
font-weight: 500;
font-style: Medium;
font-size: 16px;
leading-trim: NONE;
line-height: 100%;
letter-spacing: 0px;
color: rgba(30, 41, 59, 1);
margin-left: 5px;
">结算信息</span>

                  </div>
                  <t-row :gutter="[35, 12]">
                    <t-col :span="4">
                      <div class="jsxxA">
                        <div>
                          <div class="aaa">结算单价</div>
                          <div class="bbb">{{ buyForm.balancePrice || 0 }}元/千克</div>
                        </div>
                        <div style='margin-top:8px'>
                          <img src="@/assets/imgs/dcjs-4.png" />
                        </div>
                      </div>
                    </t-col>

                    <t-col :span="4">
                      <div class="jsxxB">
                        <div>
                          <div class="aaa">结算重量</div>
                          <div class="bbb">{{ buyForm.balanceWeight || 0 }}千克</div>
                        </div>
                        <div style='margin-top:5px'>
                          <img src="@/assets/imgs/dcjs-5.png" />
                        </div>
                      </div>
                    </t-col>

                    <t-col :span="4">
                      <div class="jsxxC">
                        <div style="width: calc(100% - 64px);">
                          <div class="aaa">结算金额</div>
                          <div class="bbb">{{ buyForm.balanceAmount || 0 }}元</div>
                          <div class="ccc">{{ buyForm.hanzi || '零' }}元</div>
                        </div>
                        <div style='margin-top:8px'>
                          <img src="@/assets/imgs/dcjs-6.png" />
                        </div>
                      </div>
                    </t-col>
                  </t-row>
                </t-col>

                <t-col :span="12">
                  <div style='display:flex;justify-content:space-around;min-width: 210px;width: 30%;margin: 18px auto 0;'>

                    <t-button variant="base" theme="warning" class="czbt2" @click='go'>
                      <template #icon>
                        <img style="margin-right: 10px;" src="@/assets/imgs/dcjs-7.png">
                      </template>
                      确认结算</t-button>

                    <t-button variant="outline" theme="warning" class="jsdy" @click='jsbdy'>
                      <template #icon>
                        <img style="margin-right: 10px;" src="@/assets/imgs/dcjs-8.png">
                      </template>
                      结算并打印</t-button>

                  </div>
                </t-col>
              </t-row>
            </t-form>
          </div>
        </div>
      </t-col>
    </t-row>

    <div class="weighTb bx">
      <div class="weighTbhd">
        <img src="@/assets/imgs/jjcz-3.png" style="margin-right: 7px;margin-top: 3px;" />
        今日结算记录
      </div>

      <div class="hislist">
        <common-table justList="1" tbMt="0" :columns="columns" :tableData="tableData" :pagination="pagination"
          @changePage="changePage">
        </common-table>
      </div>

      <t-dialog v-model:visible="confirmVisible" header="确认提交" class="newDialog newclassNewDialog"
        :cancelBtn="{ theme: 'default', variant: 'outline' }" :confirmBtn="{ theme: 'warning', variant: 'base' }"
        @confirm="onConfirm">
        <t-space direction="vertical" style="width: 100%">
          <div class="deletCon">
            <div class="b"
              style="display: flex;align-items: center; justify-content: center;    margin-bottom: -10px; margin-top: 9px;">
              <t-icon style="margin-right:5px;" name="error-circle-filled" size="18px" color="rgba(255, 41, 75, 1)" />
              表单一旦提交，将影响后续流程，请谨慎操作.
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
import { ref, getCurrentInstance, onMounted, watch } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';
import { useUserStore } from '@/store';
const userStore = useUserStore();
const { proxy } = getCurrentInstance();
import { listOutBalance, saveOutBalance, outSwipeCard, outQueryTask, getOutTaskBalance } from '@/api/outWeigh';

const confirmStu = ref('2')  //  2确认提交
const confirmVisible = ref(false)

const go = () => {
  if (!formData.value.taskId) {
    MessagePlugin.error('请刷磁卡!');
    return
  }
  formValidator.value.submit()
}


const jsbdy = () => {
  MessagePlugin.error('该功能暂未开放!');
}

const formValidator = ref(null);
const onConfirm = async () => {
  // 确认提交
  formData.value.price = formData.value.price * 1
  let res = await saveOutBalance(formData.value)
  if (res.status.code == 200) {
    MessagePlugin.success(res.status.message);
    pagination.value.defaultCurrent = 1
    init()
  } else {
    MessagePlugin.error(res.status.message);
  }
  formData.value = {
    "taskId": '',
    "price": '',
  }
  cardForm.value = {}
  buyForm.value = {}
  formValidator.value.reset()
  confirmVisible.value = false
}



const onSubmit = async (ctx) => {
  if (ctx.validateResult === true) {
    console.log(formData.value)
    confirmStu.value = 2
    onConfirm()
  }

}

const formData = ref({
  "taskId": '',
  "price": '',
})
const FORM_RULES = ref({
  price: [{ required: true, message: '请录入单价', type: 'error' }
    , {
    pattern: /^(0\.[1-9]\d*|[1-9]\d*(\.\d+)?)$/,
    message: '请正确输入数字'
  }
  ]
})

const cardForm = ref({})
const buyForm = ref({})
const result = r => {
  console.log(r)
  //  r.result  0失败   1成功
  if (r.result == 1) {
    cardForm.value.card = r.data
    outSwipeCard({
      "physicalCardNumber": cardForm.value.card,
      "processStep": 'settled'
    }).then(rs => {
      if (rs.status.code == 200) {
        outQueryTask(rs.result.taskId).then(dt => {
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

        getOutTaskBalance(rs.result.taskId).then(mt => {
          if (mt.status.code == 200) {
            formData.value.taskId = rs.result.taskId
            if (formData.value.price) formData.value.price = formData.value.price * 1
            cardForm.value = { ...cardForm.value, ...mt.result }
          } else {
            MessagePlugin.error(mt.status.message);
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
    title: '智能卡编号', colKey: 'cardNo'
  },

  {
    title: '车牌号', colKey: 'licensePlate'
  },
  {
    title: '承运人', colKey: 'carrierName',
  },

  {
    title: '作业编号', colKey: 'jobNo',
  },

  {
    title: '出/入库', colKey: 'jobType',
    cell: (h, { row }) => {
      return <p>
        {row.jobType == '1' ? '入库' : '出库'}
      </p>
        ;
    },
  },

  {
    title: '结算重量', colKey: 'balanceWeight',
    cell: (h, { row }) => {
      return <p>
        {row.balanceWeight ? row.balanceWeight + '千克' : '-'}
      </p>
        ;
    },
  },

  {
    title: '结算单价', colKey: 'balancePrice',
    cell: (h, { row }) => {
      return <p>
        {row.balancePrice ? row.balancePrice + '元/千克' : '-'}
      </p>
        ;
    },
  },

  {
    title: '结算金额', colKey: 'balanceAmount',
    cell: (h, { row }) => {
      return <p>
        {row.balanceAmount ? row.balanceAmount + '元' : '-'}
      </p>
        ;
    },
  },

  {
    title: '作业状态', colKey: 'jobStatus',
  },
  { title: '时间', colKey: 'addTime' },
])

const init = () => {
  let p = {
    pageNo: pagination.value.defaultCurrent,
    pageSize: pagination.value.defaultPageSize,
    pageTotal: true,
  }
  listOutBalance(p).then(res => {
    if (res.status.code == 200) {
      const list = res.result?.data || res.result?.records || (Array.isArray(res.result) ? res.result : [])
      tableData.value = list
      pagination.value.total = res.result?.total || list.length;
    } else {
      MessagePlugin.error(res.status.message);
    }
  });
};


const numberToChineseCurrency = (num) => {
  const chineseNums = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖'];
  const chineseUnits = ['', '拾', '佰', '仟'];
  const bigUnits = ['', '万', '亿'];
  const decimalUnits = ['角', '分'];

  try {
    if (isNaN(num)) throw new Error('请输入有效数字');
    if (num < 0) throw new Error('金额不能为负数');

    num = parseFloat(num).toFixed(2);
    let str = num.toString();
    let [integerPart, decimalPart] = str.split('.');
    let result = '';

    // 处理整数部分
    if (integerPart !== '0') {
      let zeroCount = 0;
      const len = integerPart.length;
      const groupCount = Math.ceil(len / 4);

      for (let i = 0; i < groupCount; i++) {
        const start = Math.max(0, len - (i + 1) * 4);
        const end = len - i * 4;
        const group = integerPart.slice(start, end);
        let groupChinese = '';
        let zeroFlag = false;

        for (let j = 0; j < group.length; j++) {
          const digit = parseInt(group[j]);
          const position = group.length - j - 1;

          if (digit === 0) {
            zeroFlag = true;
          } else {
            if (zeroFlag) {
              groupChinese += chineseNums[0];
              zeroFlag = false;
            }
            groupChinese += chineseNums[digit] + chineseUnits[position];
          }
        }

        if (groupChinese) {
          result = groupChinese + bigUnits[i] + result;
        }
      }

      result += '';
    }

    // 处理小数部分
    if (decimalPart) {
      if (decimalPart[0] !== '0') {
        result += chineseNums[parseInt(decimalPart[0])] + decimalUnits[0];
      }
      if (decimalPart[1] !== '0') {
        result += chineseNums[parseInt(decimalPart[1])] + decimalUnits[1];
      }
    } else {
      result += '整';
    }

    // 处理零元的情况
    if (result === '元整') {
      result = '零元整';
    }

    return result;
  } catch (error) {
    return error.message;
  }
}

watch(
  () => formData.value.price,
  () => {
    if (!formData.value.taskId) {
      // 未刷卡时不计算
    } else {
      buyForm.value.balancePrice = formData.value.price * 1
      buyForm.value.balanceWeight = cardForm.value.netWeight * 1
      buyForm.value.balanceAmount = (buyForm.value.balanceWeight * 1) * (buyForm.value.balancePrice * 1)
      buyForm.value.hanzi = numberToChineseCurrency(buyForm.value.balanceAmount)
    }
  },
);

onMounted(() => {
  cardForm.value.name = userStore.userInfo.user.name
  init();
})
</script>
<style lang="less" scoped>
.jsxxA {
  width: 100%;
  height: 82px;
  background: linear-gradient(104.55deg, #D0E7FF 7.49%, #E5F7FD 95.16%);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0px 25px;

  .aaa {
    font-family: PingFang SC;
    font-weight: 400;
    font-style: Regular;
    font-size: 14px;
    leading-trim: NONE;
    line-height: 100%;
    letter-spacing: 0px;
    vertical-align: middle;
    color: rgba(30, 41, 59, 1);
    margin-bottom: 18px;

  }

  .bbb {
    font-family: PingFang SC;
    font-weight: 600;
    font-style: Semibold;
    font-size: 16px;
    leading-trim: NONE;
    line-height: 100%;
    letter-spacing: 0px;
    vertical-align: middle;
    color: rgba(38, 85, 227, 1);

  }
}

.jsxxB {
  width: 100%;
  height: 82px;
  background: linear-gradient(104.55deg, #D3F7EF 7.49%, #E9F4F5 95.16%);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0px 25px;

  .aaa {
    font-family: PingFang SC;
    font-weight: 400;
    font-style: Regular;
    font-size: 14px;
    leading-trim: NONE;
    line-height: 100%;
    letter-spacing: 0px;
    vertical-align: middle;
    color: rgba(30, 41, 59, 1);
    margin-bottom: 18px;

  }

  .bbb {
    font-family: PingFang SC;
    font-weight: 600;
    font-style: Semibold;
    font-size: 16px;
    leading-trim: NONE;
    line-height: 100%;
    letter-spacing: 0px;
    vertical-align: middle;
    color: rgba(0, 142, 106, 1);


  }
}

.jsxxC {
  width: 100%;
  height: 82px;
  background: linear-gradient(104.55deg, #E2E9FF 7.49%, #EDF0FA 95.16%);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0px 25px;

  .aaa {
    font-family: PingFang SC;
    font-weight: 400;
    font-style: Regular;
    font-size: 14px;
    leading-trim: NONE;
    line-height: 100%;
    letter-spacing: 0px;
    vertical-align: middle;
    color: rgba(30, 41, 59, 1);

  }

  .bbb {
    font-family: PingFang SC;
    font-weight: 600;
    font-style: Semibold;
    font-size: 16px;
    leading-trim: NONE;
    line-height: 100%;
    letter-spacing: 0px;
    vertical-align: middle;
    color: rgba(38, 85, 227, 1);
    margin-top: 6px;
    margin-bottom: 6px;

  }

  .ccc {
    font-family: PingFang SC;
    font-weight: 400;
    font-style: Regular;
    font-size: 10px;
    leading-trim: NONE;
    line-height: 100%;
    letter-spacing: 0px;
    vertical-align: middle;

    color: rgba(149, 149, 149, 1);


    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    width: 100%;
  }
}




.bx {
  border-radius: 5px;
}

.weighLeft {
  padding: 15px;
  background: #fff;

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
    padding: 0px 0px 10px;

    .weighInp {

      .inp {}
    }
  }
}

.czbt2 {
  width: 112px;
  height: 30px;

  font-family: PingFang SC;
  font-weight: 500;
  font-style: Medium;
  font-size: 14px;
  leading-trim: NONE;
  letter-spacing: 0px;
  background: rgba(245, 158, 11, 1);
  border: none;
  padding-bottom: 2px;
}

.jsdy {
  width: 112px;
  height: 30px;
  border: 1px solid rgba(245, 158, 11, 1);
  color: rgba(245, 158, 11, 1);
  font-family: PingFang SC;
  font-weight: 500;
  font-style: Medium;
  font-size: 14px;
  leading-trim: NONE;
  line-height: 100%;
  letter-spacing: 0px;


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
