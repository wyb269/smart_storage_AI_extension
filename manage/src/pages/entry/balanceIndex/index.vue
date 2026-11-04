<template>
  <div class="forNewclass">
    <t-row :gutter="[25, 25]">
      <t-col :span="8">
        <div class="weighLeft bx">
          <div class="hd">
            <div class="tt"><img src="@/assets/imgs/dcjs-1.png"><span class="wtit">单车结算</span></div>
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
            <div class="weighCon" style="padding: 0px;padding-top: 2px;">

              <div class="weighInp" style="width: 100%;">

                <t-row :gutter="[65, 9]">
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
                  <t-form-item label="每千克扣价">
                    <t-input v-model="cardForm.deductionPrice" suffix="元" placeholder="请刷磁卡" disabled />
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

                <t-col :span="4">
                  <t-form-item label="每千克扣量">
                    <t-input v-model="cardForm.deduction" suffix="千克" placeholder="请刷磁卡" disabled />
                  </t-form-item>
                </t-col>

                <t-col :span="4">
                  <t-form-item label="值仓现场扣重">
                    <t-input v-model="cardForm.deductWeight" suffix="千克" placeholder="请刷磁卡" disabled />
                  </t-form-item>
                </t-col>
                <t-col :span="4">
                  <t-form-item label="扣量合计">
                    <t-input v-model="cardForm.deductWeightTotal" suffix="千克" placeholder="请刷磁卡" disabled />
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
              </t-row>
            </t-form>
          </div>
        </div>
      </t-col>
      <t-col :span="4">
        <div class="weighPh bx">
          <div class="phhd">
            <img src="@/assets/imgs/dcjs-2.png">
            <span>支付信息</span>
          </div>

          <div style="    margin-top: 15px;">
            <t-form :label-width="70" labelAlign="top">
              <t-row :gutter="[35, 12]">
                <t-col :span="12">

                  <t-form-item label="支付方式">
                    <t-input v-model="cardForm.payM" placeholder="请刷磁卡" disabled />
                  </t-form-item>

                </t-col>

                <t-col :span="12">

                  <t-form-item label="账户身份证">
                    <t-input v-model="cardForm.idCard" placeholder="请刷磁卡" disabled />
                  </t-form-item>

                </t-col>


                <t-col :span="12">

                  <t-form-item label="账户联系方式">
                    <t-input v-model="cardForm.linkPhone" placeholder="请刷磁卡" disabled />
                  </t-form-item>

                </t-col>


                <t-col :span="12">

                  <t-form-item label="开户银行">
                    <t-input v-model="cardForm.bankName" placeholder="请刷磁卡" disabled />
                  </t-form-item>

                </t-col>

                <t-col :span="12">

                  <t-form-item label="开户行号">
                    <t-input v-model="cardForm.bankCode" placeholder="请刷磁卡" disabled />
                  </t-form-item>

                </t-col>

                <t-col :span="12">

                  <t-form-item label="开户账号">
                    <t-input v-model="cardForm.bankAccount" placeholder="请刷磁卡" disabled />
                  </t-form-item>

                </t-col>



                <t-col :span="12">
                  <div style='display:flex;justify-content:space-around;min-width: 210px;width: 60%;margin: 7px auto;'>

                    <t-button variant="base" theme="warning" class="czbt" @click='go'>
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
import { ref, defineComponent, getCurrentInstance, onMounted, nextTick, watch } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/store';
const userStore = useUserStore();
const { proxy } = getCurrentInstance();
import { listBalance, saveBalance, swipeCard, queryTask, detailBalance, getTaskBalance } from '@/api/weigh';

const confirmStu = ref('2')  // 0 前照片   1 后照片   2确认提交
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
  let res = await saveBalance(formData.value)
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
    // confirmVisible.value = true
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
    // pattern: /^\d+(\.\d+)?$/,
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
    //  cardForm.value.card = '04A3654A240189'
    //    cardForm.value.card = '04B39947240189'
    //   cardForm.value.card = '6B5640A6'
    swipeCard({
      "physicalCardNumber": cardForm.value.card,
      "processStep": 'settled'
    }).then(rs => {
      if (rs.status.code == 200) {
        queryTask(rs.result.taskId).then(dt => {
          if (dt.status.code == 200) {
            cardForm.value.cardNo = rs.result.cardNo
            cardForm.value.jobNo = dt.result.jobNo
            cardForm.value.carrierName = dt.result.carrierName
            cardForm.value.licensePlate = dt.result.licensePlate
            cardForm.value.customerName = dt.result.customerName
          } else {
            MessagePlugin.error(dt.status.message);
          }
        })

        getTaskBalance(rs.result.taskId).then(mt => {
          if (mt.status.code == 200) {
            formData.value.taskId = rs.result.taskId
            if (formData.value.price) formData.value.price = formData.value.price * 1
            cardForm.value = { ...cardForm.value, ...mt.result, ...{ payM: '银行卡支付' } }
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
    //  ellipsis: true,
    align: 'left',
    colKey: 'serial-number',
  },
  {
    title: '智能卡编号', colKey: 'cardNo'
  },

  {
    title: '作业单编号', colKey: 'jobNo'
  },
  {
    title: '承运人', colKey: 'carrierName',
  },

  {
    title: '结算单编号', colKey: 'balanceNo',
  },

  {
    title: '结算价格', colKey: 'balancePrice',
    cell: (h, { row }) => {
      return <p>
        {row.balancePrice}元/千克
      </p>
        ;
    },
  },

  {
    title: '结算重量', colKey: 'balanceWeight',
    cell: (h, { row }) => {
      return <p>
        {row.balanceWeight}千克
      </p>
        ;
    },
  },

  {
    title: '结算金额', colKey: 'balanceAmount',
    cell: (h, { row }) => {
      return <p>
        {row.balanceAmount}元
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
  { title: '创建时间', colKey: 'addTime' },
])

const init = () => {
  let p = {
    pageNo: pagination.value.defaultCurrent,
    pageSize: pagination.value.defaultPageSize,
  }
  listBalance(p).then(res => {
    if (res.status.code === '200') {
      tableData.value = res.result.data
      pagination.value.total = res.result.total;
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

  // 主逻辑
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
      // MessagePlugin.error('请刷磁卡!');
      // formData.value.price=''
    } else {
      buyForm.value.balancePrice = formData.value.price * 1 - cardForm.value.deductionPrice * 1
      buyForm.value.balanceWeight = cardForm.value.netWeight * 1 - cardForm.value.deductWeightTotal * 1
      buyForm.value.balanceAmount = (buyForm.value.balanceWeight * 1) * (buyForm.value.balancePrice * 1)
      buyForm.value.hanzi = numberToChineseCurrency(buyForm.value.balanceAmount)
      // {{buyForm.balancePrice||0}}{{buyForm.balanceWeight||0}}{{buyForm.balanceAmount||0}}
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
  // box-shadow: 0 1px 4px rgba(38, 47, 62, 0.2);
  border-radius: 5px;
}

.weighLeft {
  padding: 15px;
  background: #fff;
  height: 587px;

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
    // display: flex;

    .weighInp {
      // width: calc(100% - 110px);
      // display: flex;

      .inp {
        // width: 33.33%;
        // padding-right: 15px;
      }
    }
  }
}

.czbt {
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

.weighPh {
  padding: 15px;
  height: 587px;
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
