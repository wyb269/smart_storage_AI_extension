<template>
  <div class="list-common-table">
    <t-space direction="vertical" style="width: 100%" class="tdesign-tree-operations">
        <t-input v-model="filterText" @change="onInputChange" placeholder="请输入节点名称">
          <template #prefix-icon>
            <t-icon name="search" />
          </template>
        </t-input>



    <!-- <t-enhanced-table
      ref="tableRef"
      v-model:expandedTreeNodes="expandedTreeNodes"
      row-key="key"
      drag-sort="row-handler"
      :data="data"
      :columns="columns"
      :tree="treeConfig"
      :tree-expand-and-fold-icon="treeExpandIcon"
      :pagination="pagination"
      :before-drag-sort="beforeDragSort"
      :column-controller="columnController"
      lazy-load
      @page-change="onPageChange"
      @abnormal-drag-sort="onAbnormalDragSort"
      @drag-sort="onDragSort"
      @expanded-tree-nodes-change="onExpandedTreeNodesChange"
    ></t-enhanced-table> -->






 

      <!-- checkable -->
             <!-- @expand="onExpand"
      @change="onChange"
      @active="onActive" -->
    <t-tree
      ref="tree"
      :data="items"
      hover
      expand-all
      activable
    
      :expand-on-click-node="false"
      :expand-parent="expandParent"
      :filter="filterByText"
      line

    >
      <template #operations="{ node }">
        <t-space :size="10">
          <t-button size="small" variant="base" :disabled="node.data.levels==2" @click="add(node)">添加子节点</t-button>

          <t-button size="small" variant="outline" @click="update(node)">编辑节点</t-button>
  
          <t-button size="small" variant="base" :disabled="node.data.levels==0" theme="danger" @click="handleClickDelete(node)">删除</t-button>
        </t-space>
      </template>
    </t-tree>
  </t-space>



    <!-- 新增编辑弹窗 -->
  <t-dialog
      v-model:visible="visible"
      top="13vh"
      :header="pagestu[0]==1?'创建节点':'编辑节点'"
      width="45%"
      :on-confirm=" ()=>{proxy.$refs.addf.submt()}"
    >
      <t-space direction="vertical" style="width: 100%">
        <addcom ref="addf" :roleGroupData="roleGroupData" :pagestu="pagestu"  @sumt="sumt" ></addcom>
      </t-space>
  </t-dialog>
  <!-- 确认删除弹窗 -->
  <t-dialog
        v-model:visible="confirmVisible"
        header="确认删除该节点？"
        @confirm="onConfirmDelete"
      />


  </div>
</template>

<script lang="ts" setup>
import { listOrg,saveOrg,deleteOrg,updateOrg,orgByDetail,orgCas} from '@/api/org';
import { computed, onMounted, ref,getCurrentInstance } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/store';
import addcom from './components/add.vue';
const userStore = useUserStore();
import {
  TreeInstanceFunctions,
  TreeProps,
  ButtonProps,
  InputProps,
  TreeNodeModel,
  TreeOptionData,
  TreeNodeValue,
  TypeTreeNodeModel,
} from 'tdesign-vue-next';

const { proxy } = getCurrentInstance();
const visible = ref(false);
const tree = ref<TreeInstanceFunctions>();
const items = ref([]);
          //创建，编辑   //三个节点
const pagestu=ref(['1','1'])
const update=(node:any)=>{
  visible.value=true
  pagestu.value[0]='2'
  pagestu.value[1]=node.data.levels+1
  proxy.$refs.addf.initData(node.data)
}
const add=(node)=>{
      visible.value=true
      pagestu.value[0]='1'
      pagestu.value[1]=node.data.levels+2
      proxy.$refs.addf.rst(node.data)
}


const confirmVisible = ref(false);
const deletId=ref({})
const handleClickDelete = (node) => {
  deletId.value.id=node.data.id
  confirmVisible.value = true;
};
const onConfirmDelete = async() => {
  confirmVisible.value = false;
  let res= await deleteOrg(deletId.value.id)
  if(res.status.code==200){
    MessagePlugin.success(res.status.message);
    init()
  }else {
    MessagePlugin.error(res.status.message);
  }
};


const searchData=ref({
  pageNo:0,
  pageSize:0,
  name:''
})
const onReset = () => {
  searchData.value = {
    pageNo:0,
    pageSize:0,
    name:''
  }
  init();
};
const init = () => {

  listOrg(searchData.value).then(res => {
    if (res.status.code === '200') {
      let data=res.result.data
      let dataAry:any=[]
      const get=(parentId:any,p:any)=>{
        let ary=data.filter((x:any)=>{
          if(x.parentId==parentId){
            x.label=x.name
            x.children=[]
            x.children=get(x.id,x.children)
            return x
          }
        })
        p=ary
        return p
      }
      dataAry=get(-1,dataAry)
      console.log(dataAry)
      items.value=dataAry
    } else {
      MessagePlugin.error(res.status.message);
    }
  });
};

const sumt=async(form:Object)=>{
  let res=pagestu.value[0]=='1'? await saveOrg(form):await updateOrg(form)
  if(res.status.code==200){
    visible.value=false
    onReset()
    MessagePlugin.success(res.status.message);
  }else{
    MessagePlugin.error(res.status.message);
  } 
}
const filterText = ref('');
const filterByText = ref<TreeProps['filter']>(null);

const onInputChange: InputProps['onChange'] = (state) => {
  console.info('on input:', state);
  if (filterText.value) {
    filterByText.value = (node) => {
      const label = node?.data?.label || '';
      const rs = (label as string).indexOf(filterText.value) >= 0;
      return rs;
    };
  } else {
    filterByText.value = null;
  }
};


onMounted(() => {
  init();
});



</script>

<style lang="less" scoped>
.list-common-table {
  background-color: var(--td-bg-color-container);
  padding: var(--td-comp-paddingTB-xxl) var(--td-comp-paddingLR-xxl);
  border-radius: var(--td-radius-medium);

  .table-container {
    margin-top: var(--td-comp-margin-xxl);
  }
}

.form-item-content {
  width: 100%;
}

.operation-container {
  display: flex;
  justify-content: flex-end;
  align-items: center;

  .expand {
    .t-button__text {
      display: flex;
      align-items: center;
    }
  }
}

.payment-col {
  display: flex;

  .trend-container {
    display: flex;
    align-items: center;
    margin-left: var(--td-comp-margin-s);
  }
}


.tdesign-tree-operations .t-is-active .t-tree__label,
.tdesign-tree-operations .t-is-active .t-checkbox__label {
  background-color: rgba(255, 0, 0, 0.3);
}
.tdesign-tree-operations .tips p {
  line-height: 24px;
  text-indent: 1em;
}
</style>
