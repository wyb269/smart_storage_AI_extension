<template>
  <t-form ref="formValidator" class="base-form" :data="formData" :rules="FORM_RULES" :label-width="90" @reset="onReset"
    @submit="onSubmit">
    <div class="form-basic-container">
      <div class="form-basic-item">
        <!-- 菜单管理 -->
        <menuOpt ref="menuOpt" @getmenu="getmenu" v-if="pagestu == 3" />
        <!-- <t-row class="row-gap" :gutter="[0, 24]" >
          <t-col :span="12"> -->
        <!-- <t-space direction="vertical" style="width: 100%" class="tdesign-tree-operations"> -->
        <!-- <t-space>-->
        <!-- <t-radio-group v-model="theme" variant="default-filled" @change="init">
                <t-radio-button value="1">电脑端</t-radio-button>
                <t-radio-button value="2">移动端</t-radio-button>
              </t-radio-group> -->
        <!-- <t-input-adornment prepend="筛选:">
                <t-input v-model="filterText" @change="onInputChange" />
              </t-input-adornment> -->
        <!-- </t-space>  -->

        <!-- <t-tree
              ref="tree"
              :data="items"
              hover
              :label="getLabel"
              expand-all
              activable
              checkStrictly
              checkable
              @change="onChange"
              :expand-on-click-node="false"
              :filter="filterByText"
              line
            >
              <template #operations="{ node }">
                <t-space :size="10">
                </t-space>
              </template>
</t-tree> -->
        <!-- </t-space> -->

        <!-- </t-col>
        </t-row> -->

        <!-- 合同名称,合同类型 -->
        <t-row class="row-gap" :gutter="[0, 24]" v-if="pagestu == 2 || pagestu == 1">
          <t-col :span="12">
            <t-form-item label="名称" name="name">
              <t-input v-model="formData.name" placeholder="请输入角色名称" />
            </t-form-item>
          </t-col>
          <!-- <t-col :span="12">
            <t-form-item label="行政区域" name="regionId">
              <regionSelect ref="rgn"  @getRgdata="getRgdata" ></regionSelect>
            </t-form-item>
          </t-col>
          <t-col :span="12">
            <t-form-item label="组织机构" name="orgId">
              <orgSelect ref="org"  @getdata="getdata" ></orgSelect>
            </t-form-item>
          </t-col> -->
          <t-col :span="12">
            <t-form-item label="备注">
              <t-textarea v-model="formData.remark" :height="124" placeholder="请输入" />
            </t-form-item>
          </t-col>

        </t-row>
      </div>
    </div>

  </t-form>
</template>
<script setup lang="ts">
import type { SubmitContext, UploadFailContext, UploadFile } from 'tdesign-vue-next';
import { MessagePlugin } from 'tdesign-vue-next';
import { ref, defineComponent, defineExpose, getCurrentInstance, reactive } from 'vue';
import regionSelect from '@/components/regionSelect.vue';
import orgSelect from '@/components/orgSelect.vue';
import menuOpt from './menuOpt.vue';
import { listMenu } from '@/api/menu';


const { proxy } = getCurrentInstance();
const emit = defineEmits(['sumt']);

const props = defineProps({
  pagestu: {},
  pcList: {
    type: Array,
    default: []
  },
  phList: {
    type: Array,
    default: []
  },
});

const FORM_RULES = ref({
  name: [{ required: true, message: '请输入用户姓名', type: 'error' }],
  regionId: [{ required: true, message: '请选择行政区域', type: 'error' }],
  orgId: [{ required: true, message: '请选择组织机构', type: 'error' }],
})
const tree = ref();
const pcMenu = ref([])
const phMenu = ref([])
const getall = (f) => {
  console.log(f)
  if (theme.value == 1) pcMenu.value = []
  if (theme.value == 2) phMenu.value = []
  let a = tree.value.getItems();
  if (f) for (let i = 0; i < a.length; i++) {
    if (theme.value == 1) pcMenu.value.push(a[i].data.id)
    if (theme.value == 2) phMenu.value.push(a[i].data.id)
  }
  onInputChange()
}
const onChange = (vals, state) => {
  // console.log(vals,state.node.checked)
  if (state.node.data.id == 0) {
    getall(state.node.checked)
    return
  }
  if (theme.value == 1) {
    if (state.node.checked && pcMenu.value.indexOf(state.node.data.id) == -1) pcMenu.value.push(state.node.data.id)
    if (!state.node.checked) pcMenu.value = pcMenu.value.filter(x => x != state.node.data.id * 1)

  }
  if (theme.value == 2) {
    if (state.node.checked && phMenu.value.indexOf(state.node.data.id) == -1) phMenu.value.push(state.node.data.id)
    if (!state.node.checked) phMenu.value = phMenu.value.filter(x => x != state.node.data.id)
  }
  console.log(pcMenu.value, phMenu.value)
};
const theme = ref('1')
const items = ref([])
const init = () => {
  if (theme.value == 1) items.value = props.pcList
  if (theme.value == 2) items.value = props.phList

  console.log(pcMenu.value)
  console.log(phMenu.value)
  onInputChange()
}

const filterByText = ref<TreeProps['filter']>(null);
const onInputChange = () => {
  filterByText.value = (node) => {
    let ary = theme.value == 1 ? pcMenu.value : phMenu.value
    const id = node?.data?.id || '';
    if (ary.indexOf(id) >= 0 && ary.length > 0) tree.value.setItem(node?.value, { checked: true })
    //  if(ary.length==0)tree.value.setItem(node?.value,{checked:false})
    //   if(ary.indexOf(id) == -1)tree.value.setItem(node?.value,{checked:false})
    return node
  };
};


const getLabel = (h, node) => {
  const { data } = node;
  return data.name;
};
const formValidator = ref(null);
const submt = () => {
  formValidator.value.submit()
}
const getmenu = (f: any) => {
  f.id = formData.value.id
  emit('sumt', f)
}
const onSubmit = (ctx: SubmitContext) => {
  if (ctx.validateResult === true) {
    if (props.pagestu == 3) {
      proxy.$refs.menuOpt.sumt()
      return
      // 老版用树结构处理数据的代码
      //  let a=pcMenu.value.filter(x=>x.id!=0)
      //  let b=phMenu.value.filter(x=>x.id!=0)
      //  console.log(a,b)
      //  if(a.length==0&&b.length==0){
      //   MessagePlugin.error('请勿提交空数据!');
      //   return
      //  }
      //   formData.value.menuId=[...a,...b].join(',')
    }
    emit('sumt', formData.value)
  }
};
defineOptions({
  name: 'FormBase',
});

const getdata = (e: any) => {
  formData.value.orgId = e
}
const getRgdata = (e: any) => {
  formData.value.regionId = e
}
const rst = () => {
  formData.value = {}
  formValidator.value.reset()
  // proxy.$refs.org.rst()
  // proxy.$refs.rgn.rst()
  formData.value.regionId = 0
  formData.value.orgId = 0
}

const initData = (form: Object) => {
  formData.value.id = form.id
  if (form.pagestu == 2) {
    formData.value.name = form.name
    formData.value.remark = form.remark
    // formData.value.regionId = form.regionId
    // formData.value.orgId = form.orgId
    formData.value.regionId = 0
    formData.value.orgId = 0
    proxy.$refs.org.initData(form.organization)
    proxy.$refs.rgn.initData(form.region)
  }
  if (form.pagestu == 3) {
    theme.value = '1'
    items.value = props.pcList
    pcMenu.value = []
    phMenu.value = []
    // form.listMenu.map((x:any)=>{
    //   if(x.type==1)pcMenu.value.push(x.id)
    //   if(x.type==2)phMenu.value.push(x.id)
    // })
    onInputChange()
    proxy.$refs.menuOpt.init(form.listMenu);
  }
}

const formData = ref({});

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
  padding-bottom: 40px;
  padding-top: 40px;
  padding-left: 0;
  padding-right: 0;
}
</style>
