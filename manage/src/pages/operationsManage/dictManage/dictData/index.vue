<template>
  <div>
    <common-table :searchList="searchList" :searchData="searchData" :columns="columns" :tableData="tableData"
      :pagination="pagination" @search="search" @changePage="changePage" @reset="reset">
      <!-- 搜索侧按钮插槽 -->
      <template #operation>
        <t-button variant="base" theme="warning" @click="add">
          <template #icon><t-icon name="add" style="margin-right: 3px;" /></template>
          新增</t-button>
        <t-button variant="outline" theme="default" class="flt" @click="router.go(-1)">
          <template #icon><t-icon name="arrow-left" style="margin-right: 3px;" /></template>
          返回</t-button>
      </template>
      <!-- 列表操作侧按钮插槽 -->
      <template #btnAry="slotProps">
        <!-- {{slotProps.row.row.index}} -->
        <t-link theme="primary" @click="handleClickDetail(slotProps.row.row)">编辑</t-link>
        <!-- <t-popconfirm  theme="danger"  @confirm="onConfirmDelete(slotProps.row)">
        <template #content>
        <p class="title">确认删除该数据？</p>
        <p class="describe">数据一旦删除，对应数据将无法<br />查询，请谨慎操作。</p>
        </template>
<t-link theme="danger"> 删除</t-link>
</t-popconfirm> -->
        <t-link theme="danger" @click="handleClickDelete(slotProps.row)"> 删除</t-link>
      </template>
    </common-table>
    <!-- 新增编辑弹窗 -->
    <t-dialog v-model:visible="visible" :header="pagestu == 1 ? '创建数据' : '编辑数据'" top="8vh" width="860px"
      class="newDialog newclassNewDialog" :cancelBtn="{ theme: 'default', variant: 'outline' }"
      :confirmBtn="{ theme: 'warning', variant: 'base' }" :confirm-on-enter="true"
      :on-confirm="() => { proxy.$refs.addf.submt() }">
      <t-space direction="vertical" style="width: 100%">
        <addcom ref="addf" :pagestu="pagestu" @sumt="sumt"></addcom>
      </t-space>
    </t-dialog>
    <!-- 确认删除弹窗 -->
    <t-dialog v-model:visible="confirmVisible" header="删除确认" class="newDialog newclassNewDialog"
      :cancelBtn="{ theme: 'default', variant: 'outline' }" :confirmBtn="{ theme: 'warning', variant: 'base' }"
      @confirm="onConfirmDelete">
      <t-space direction="vertical" style="width: 100%">
        <div class="deletCon">
          <div class="delectConHeader"><t-icon name="error-circle-filled" size="18px" color="rgba(255, 41, 75, 1)" />
            <span class="a">确认删除该数据</span>
          </div>
          <div class="b">数据一旦删除，对应数据将无法查询，请谨慎操作。</div>
        </div>
      </t-space>
    </t-dialog>
  </div>
</template>
<script setup lang="jsx">
import { listDictD, saveDictD, deleteDictD, updateDictD } from '@/api/dict';
import CommonTable from '@/components/common-table/index.vue';
import { computed, onMounted, ref, getCurrentInstance } from 'vue';
import { t } from '@/locales';
import addcom from './components/add.vue';
import { useRouter, useRoute } from 'vue-router';
import { MessagePlugin } from 'tdesign-vue-next';

const { proxy } = getCurrentInstance();
const router = useRouter();
const route = useRoute();


const pagination = ref({
  defaultPageSize: 10,
  total: 0,
  defaultCurrent: 1,
});
const searchData = ref({
  label: '',
  type: route.query.type,
  // showState:'',
  pageNo: pagination.value.defaultCurrent,
  pageSize: pagination.value.defaultPageSize,
  //  userId: 12345,
  //  regionId: 12345,
  //  orgId: 12345,
});
const searchList = ref([
  {
    label: '名称',
    placeholder: '请输入数据名称',
    type: "input",
    value: "name",
  }
]);

const columns = ref([
  {
    title: '序号',
    fixed: 'left',
    width: 60,
    //  ellipsis: true,
    align: 'left',
    colKey: 'serial-number',
  },
  { title: '名称', colKey: 'label' },
  { title: '键值', colKey: 'description' },
  { title: '类型', colKey: 'type' },
  {
    title: '状态', colKey: 'showState',
    cell: (h, { row }) => {
      return <span>{row.showState == 1 ? '启用' : '禁用'}</span>
    },
  },
  {
    align: 'left',
    fixed: 'right',
    width: 200,
    colKey: 'op',
    title: t('components.commonTable.operation'),
  },
])


const tableData = ref([])

const search = () => {
  pagination.value.total = 0
  pagination.value.defaultCurrent = 1
  init();
};
const changePage = (p) => {
  pagination.value.defaultCurrent = p.pagination.current
  pagination.value.defaultPageSize = p.pagination.pageSize
  init()
}
const reset = () => {
  pagination.value.total = 0
  pagination.value.defaultCurrent = 1
  searchData.value = {
    type: route.query.type,
  };
  init();
};
const init = () => {
  searchData.value.pageNo = pagination.value.defaultCurrent,
    searchData.value.pageSize = pagination.value.defaultPageSize,
    listDictD(searchData.value).then(res => {
      if (res.status.code === '200') {
        tableData.value = res.result.data
        pagination.value.total = res.result.total;
      } else {
        MessagePlugin.error(res.status.message);
      }
    });
};

const sumt = async (form) => {
  let res = pagestu.value == '1' ? await saveDictD(form) : await updateDictD(form)
  if (res.status.code == 200) {
    visible.value = false
    if (pagestu.value = '1') reset()
    if (pagestu.value = '2') init()
    MessagePlugin.success(res.status.message);
  } else {
    MessagePlugin.error(res.status.message);
  }
}

const confirmVisible = ref(false);
const deletId = ref({})
const handleClickDelete = (data) => {
  deletId.value.id = data.row.id
  confirmVisible.value = true;
};
const onConfirmDelete = async () => {
  confirmVisible.value = false;
  let res = await deleteDictD(deletId.value.id)
  if (res.status.code == 200) {
    MessagePlugin.success(res.status.message);
    init()
  } else {
    MessagePlugin.error(res.status.message);
  }
};
const visible = ref(false);
const pagestu = ref('1')
const add = () => {
  pagestu.value = '1'
  visible.value = true
  proxy.$refs.addf.rst(route.query.type)
}
const handleClickDetail = async (data) => {
  pagestu.value = 2
  let formData = data
  visible.value = true
  proxy.$refs.addf.initData(formData)
}
onMounted(() => {
  init();
});


</script>

<style lang="less" scoped>
.title {
  font-weight: 500;
  font-size: 14px;
}

.describe {
  margin-top: 8px;
  font-size: 12px;
  color: var(--td-text-color-secondary);
}
</style>
