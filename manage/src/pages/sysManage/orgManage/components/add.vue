<template>
  <t-form
    ref="formValidator"
    class="base-form"
    :data="formData"
    :rules="FORM_RULES"
    :label-width="100"
    @reset="onReset"
    @submit="onSubmit"
  >
    <div class="form-basic-container">
      <div class="form-basic-item">
        <!-- 表单内容 -->

        <!-- 合同名称,合同类型 -->
        <t-row class="row-gap" :gutter="[32, 24]" v-if="pagestu[1]==1">
          <t-col :span="12">
            <t-form-item label="名称" name="name">
              <t-input v-model="formData.name"  placeholder="请输入节点名称" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="统一编码">
              <t-input v-model="formData.unifyEncoding"  placeholder="请输入统一编码" />
            </t-form-item>
          </t-col>

          <t-col :span="12">
            <t-form-item label="企业性质" >
              <t-select
                v-model="formData.companyNature"
              
                class="demo-select-base"
                placeholder="请选择企业性质"
                clearable
              >
                <t-option  value="1" label="国有企业"> </t-option>
                <t-option  value="2" label="民营企业"> </t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="工商登记注册号" >
              <t-input v-model="formData.businessRegistrationNumber"  placeholder="请输入工商登记注册号" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="法人姓名" >
              <t-input v-model="formData.legalPersonName"  placeholder="请输入法人姓名" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="法人联系电话" name="legalRepresentativePhoneNumber">
              <t-input v-model="formData.legalRepresentativePhoneNumber"  placeholder="请输入法人联系电话" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="行政区域">
               <regionSelect ref="rgn"  @getRgdata="getRgdata" ></regionSelect>
            </t-form-item>
          </t-col>

          <t-col :span="12">
            <t-form-item label="详细地址">
              <t-input v-model="formData.detailedAddress"  placeholder="请输入详细地址" />
            </t-form-item>
          </t-col>

          <t-col :span="12">
            <t-form-item label="描述说明">
              <t-textarea v-model="formData.description"  :height="124" placeholder="请输入描述说明" />
            </t-form-item>
          </t-col>
        </t-row>



        <t-row class="row-gap" :gutter="[0, 24]" v-else>
          <t-col :span="12">
            <t-form-item label="名称" name="name">
              <t-input v-model="formData.name"  placeholder="请输入节点名称" />
            </t-form-item>
          </t-col>
          <t-col :span="12" name="type">
            <t-form-item label="类型" >
              <t-select
                v-model="formData.type"
                disabled
              
                class="demo-select-base"
                placeholder="请选择类型"
                clearable
              >
                <t-option  :value="1" label="部门"> </t-option>
                <t-option  :value="2" label="库区"> </t-option>
              </t-select>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="联系人" >
              <t-input v-model="formData.contactPerson"  placeholder="请输入联系人姓名" />
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="联系人电话" name="contactPhone">
              <t-input v-model="formData.contactPhone"  placeholder="请输入联系人电话" />
            </t-form-item>
          </t-col>
          <template v-if="formData.type==2">
            <t-col :span="6">
            <t-form-item label="统一编码">
              <t-input v-model="formData.unifyEncoding" :style="{ width: '322px' }" placeholder="请输入统一编码" />
            </t-form-item>
            </t-col>

           <t-col :span="6">
            <t-form-item label="仓库性质" >
            <t-select
                v-model="formData.warehouseNature"
                :style="{ width: '322px' }"
                class="demo-select-base"
                placeholder="请选择"
                clearable
              >
                <t-option  :value="1" label="普通仓"> </t-option>
                <t-option  :value="2" label="临时仓"> </t-option>
              </t-select>
            </t-form-item>
          </t-col>

          <t-col :span="6">
            <t-form-item label="库点设计储量">
              <t-input v-model="formData.designStorage" :style="{ width: '322px' }" placeholder="请输入库点设计储量" />
            </t-form-item>
            </t-col>

          <t-col :span="6">
            <t-form-item label="库点设计面积">
              <t-input v-model="formData.designArea" type="number" :style="{ width: '322px' }" placeholder="请输入库点设计面积" >
                          <template #suffix>
            <div>平方米</div>
          </template>
              </t-input>
            </t-form-item>
            </t-col>

           <t-col :span="6">
            <t-form-item label="库区编码">
              <t-input v-model="formData.libraryAreaCode" :style="{ width: '322px' }" placeholder="请输入库区编码" />
            </t-form-item>
            </t-col>


          <t-col :span="6">
            <t-form-item label="行政区域">
               <regionSelect ref="rgnt"  @getRgdata="getRgdata" ></regionSelect>
            </t-form-item>
          </t-col>
          <t-col :span="6">
            <t-form-item label="详细地址">
              <t-input v-model="formData.detailedAddress" :style="{ width: '322px' }" placeholder="请输入详细地址" />
            </t-form-item>
          </t-col>

            <t-col :span="6">
            <t-form-item label="描述说明">
              <t-textarea v-model="formData.description"  :height="124" placeholder="请输入描述说明" />
            </t-form-item>
          </t-col>

          </template>



















        </t-row>



      </div>
    </div>

  </t-form>
</template>
<script setup lang="ts">
import type { SubmitContext, UploadFailContext, UploadFile } from 'tdesign-vue-next';
import { MessagePlugin } from 'tdesign-vue-next';
import { ref,defineComponent,defineExpose,getCurrentInstance,nextTick} from 'vue';
import regionSelect from '@/components/regionSelect.vue';
import orgSelect from '@/components/orgSelect.vue';
import { template } from 'lodash';


const { proxy } = getCurrentInstance();
const emit = defineEmits(['sumt']);

const props = defineProps({
  pagestu:{},
  roleGroupData:{
    type:Array,
    default:[]
  },
});

const FORM_RULES=ref({
  name:[{ required: true, message: '请输入节点名称', type: 'error' }],
  legalRepresentativePhoneNumber:[{ required:false, message: '请输入手机号', type: 'error' }
  // ,{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
  //       message: "请输入正确的手机号码" }
    ],
  // contactPhone:[{ required:false, message: '请输入手机号', type: 'error' },{
  //       pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
  //       message: "请输入正确的手机号码"}
  //     ],
})

const formValidator = ref(null);
const submt=()=>{
  formValidator.value.submit()
}
const onSubmit = (ctx: SubmitContext) => {
  if (ctx.validateResult === true) {
    // formData.value.loginId=formData.value.phone
    // if(props.pagestu=='2')delete formData.value.loginPwd
    for (const key in formData.value) {
      if(formData.value[key]==null){
        formData.value[key]=''
      }
    }
    emit('sumt',formData.value)
  }
};
defineOptions({
  name: 'FormBase',
});

const getRgdata=(e:any)=>{
  formData.value.city=e
}
const rst=(data)=>{
    formData.value={}
    formData.value.parentId=data.id
    console.log(props.pagestu)
    //第三级
    // if(props.pagestu[1]==3)formData.value.type=2
    // //是新增
    // if(props.pagestu[0]==1)formData.value.type=data.sonType
    formData.value.type=1
    formValidator.value.reset()

    nextTick(() => {
      if(formData.value.type==2)proxy.$refs.rgnt.rst()
    });
}

const initData=(form:Object)=>{
//  delete formData.value.parentId
  formData.value={}
  formData.value.id=form.id
  formData.value.type=form.type
  formData.value.name=form.name
 // 如果是节点1 总公司
  if(props.pagestu[1]==1){
    formData.value.detailedAddress=form.detailedAddress
    formData.value.city=form.city
    formData.value.unifyEncoding=form.unifyEncoding
    formData.value.description=form.description

    formData.value.companyNature=form.companyNature
    formData.value.businessRegistrationNumber=form.businessRegistrationNumber
    formData.value.legalPersonName=form.legalPersonName
    formData.value.legalRepresentativePhoneNumber=form.legalRepresentativePhoneNumber
  }else{
    formData.value.contactPerson=form.contactPerson
    formData.value.contactPhone=form.contactPhone
    // formData.value.libraryAreaCode=form.libraryAreaCode
    // formData.value.designArea=form.designArea
    // formData.value.designStorage=form.designStorage
    // formData.value.warehouseNature=form.warehouseNature?form.warehouseNature*1:''
  }
  nextTick(() => {
      if(form.city){
        if(formData.value.type==2)proxy.$refs.rgnt.initData({id:form.city})
        if(formData.value.type==1&&form.levels==0)proxy.$refs.rgn.initData({id:form.city})
      }
      if(!form.city){
        if(formData.value.type==2)proxy.$refs.rgnt.rst()
        if(formData.value.type==1&&form.levels==0)proxy.$refs.rgn.rst()
      }
    })
  }

const formData = ref({
//   "name": '', 
//   "loginId":'',
//   "phone":'',
//   "loginPwd":'',
//   "regionId": '',
  
//   "orgId":'',
//   "roleGroupId": '',

// //  "headUrl":'',
//   "userType":'1',
//   "showState":'1',
//   "employedStatus":'1'
 });

const onReset = () => {

};
defineExpose({
submt,
rst,
initData
})
</script>
<style lang="less" scoped>
@import './index.less';
.form-basic-container {
  width: 100%;
  overflow:hidden;
  padding-top: 30px;
  padding-bottom: 30px;
}
</style>
