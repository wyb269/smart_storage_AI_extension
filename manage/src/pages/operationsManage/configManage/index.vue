<template>
  <div>
    <common-table :searchList="searchList" tbMt="0px" :searchData="searchData" :columns="columns" :tableData="tableData"
      :pagination="pagination" @search="search" @changePage="changePage" @reset="reset">
      <!-- 搜索侧按钮插槽 -->
      <template #operation>
        <!-- <t-button  variant="base" theme="primary" @click="add">新增</t-button> -->
      </template>
      <!-- 列表操作侧按钮插槽 -->
      <template #btnAry="slotProps">
        <!-- {{slotProps.row.row.index}} -->
        <t-link theme="primary" @click="handleClickDetail(slotProps.row.row, 3)">详情</t-link>
        <t-link theme="success" v-if="slotProps.row.row.needPwd == 2"
          @click="handleClickDetail(slotProps.row.row, 2)">编辑</t-link>

        <t-popconfirm theme="default" v-if="slotProps.row.row.needPwd == 1" :visible="slotProps.row.row.vs"
          @cancel="slotProps.row.row.vs = false; pwd = ''" @confirm="onConfirm(slotProps.row.row)">
          <template #content>
            <p class="title">编辑涉敏数据需先验证登陆密码</p>
            <p class="describe" style="margin-top:12px;position:relative">
              <t-input v-model="pwd" type="password" style="width:220px" clearable placeholder="请输入登录密码"></t-input>
            <div v-if="pwdStu == 2 && !pwd" style="color:#F44336;position: absolute;font-size: 12px;">请输入密码</div>
            </p>
          </template>
          <t-link theme="success" @click="showPop(slotProps.row.row)">编辑</t-link>
        </t-popconfirm>

      </template>
    </common-table>
    <!-- 新增编辑弹窗 -->
    <t-dialog v-model:visible="visible" :header="pagestu == 2 ? '编辑' : '详情'" :footer="pagestu == 2" width="860px"
      class="newDialog newclassNewDialog" :cancelBtn="{ theme: 'default', variant: 'outline' }"
      :confirmBtn="{ theme: 'warning', variant: 'base' }" :onClose="() => { pagestu = 1 }" :confirm-on-enter="true"
      :on-confirm="() => { proxy.$refs.addf.submt() }">
      <t-space direction="vertical" style="width: 100%">
        <addcom ref="addf" :pagestu="pagestu" :phList="phList" :pcList="pcList" @sumt="sumt"></addcom>
      </t-space>
    </t-dialog>
    <!-- 确认删除弹窗 -->
    <t-dialog v-model:visible="confirmVisible" header="确认删除该角色？" @confirm="onConfirmDelete" />
  </div>
</template>
<script setup lang="jsx">
import { listRole, saveRole, deleteRole, updateRole, RoleByDetail, roleRights, roleRightsGet, roleMenu, roleMenuGet } from '@/api/role';
import { listSystemConfigByCondition, updateSystemConfig, getSystemConfigByGroupAndKey } from '@/api/dict';
import CommonTable from '@/components/common-table/index.vue';
import { computed, onMounted, ref, getCurrentInstance, nextTick } from 'vue';
import addcom from './components/add.vue';
import { useRouter } from 'vue-router';
import { MessagePlugin } from 'tdesign-vue-next';

const { proxy } = getCurrentInstance();
const router = useRouter();


const pagination = ref({
  defaultPageSize: 10,
  total: 0,
  defaultCurrent: 1,
});
const searchData = ref({
  // "sycGroup":'',
  "sycKey": '',
  // "sycValue":'',
  // "remark": 12345,
  pageNo: pagination.value.defaultCurrent,
  pageSize: pagination.value.defaultPageSize,
  //  userId: 12345,
  //  regionId: 12345,
  //  orgId: 12345,

});
const searchList = ref([
  {
    label: '键名',
    placeholder: '请输入',
    type: "input",
    value: "sycKey",
  }
]);
const pwd = ref('')
const pwdStu = ref('1')
const columns = ref([
  {
    title: '序号',
    fixed: 'left',
    width: 60,
    //  ellipsis: true,
    align: 'left',
    colKey: 'serial-number',
  },
  { title: '分组', colKey: 'sycGroup' },
  { title: '键名', colKey: 'sycKey' },
  { title: '键值', colKey: 'sycValue' },
  {
    title: '敏感配置', colKey: 'needPwd',
    cell: (h, { row }) => {
      return <p style={{ color: row.needPwd == 1 ? 'red' : 'green' }}     >
        {row.needPwd == 1 ? '是' : '否'}
      </p>
        ;
    },
  },
  {
    title: '缓存', colKey: 'needCache',
    cell: (h, { row }) => {
      return <p>
        {row.needCache == 1 ? '是' : '否'}
      </p>
        ;
    },
  },
  { title: '备注', colKey: 'remark' },
  {
    align: 'left',
    fixed: 'right',
    width: 200,
    colKey: 'op',
    title: '操作',
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
  searchData.value = {};
  init();
};
const init = () => {
  searchData.value.pageNo = pagination.value.defaultCurrent,
    searchData.value.pageSize = pagination.value.defaultPageSize,
    listSystemConfigByCondition(searchData.value).then(res => {
      if (res.status.code === '200') {
        tableData.value = res.result.data
        pagination.value.total = res.result.total;
      } else {
        MessagePlugin.error(res.status.message);
      }
    });
};

const sumt = async (form) => {
  let res = await updateSystemConfig(form)
  if (res.status.code == 200) {
    visible.value = false
    init()
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
const showPop = (data) => {
  tableData.value = tableData.value.map(x => {
    x.vs = false
    return x
  })
  data.vs = true
}
const onConfirm = async (data) => {
  if (!pwd.value) {
    pwdStu.value = 2
    return
  }
  console.log(data)

  let res = await getSystemConfigByGroupAndKey({
    "loginPwd": proxy.$rsAencrypt(pwd.value),
    "sycGroup": data.sycGroup,
    "sycKey": data.sycKey
  })
  if (res.status.code == 200) {
    MessagePlugin.success(res.status.message);
    data.vs = false
    handleClickDetail(res.result, 2)
  } else {
    MessagePlugin.error(res.status.message);
  }
}
const onConfirmDelete = async (data) => {
  // confirmVisible.value = false;
  let res = await deleteRole(data.row.id)
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
  proxy.$refs.addf.rst()
}
const handleClickDetail = async (data, n) => {
  console.log(data)
  tableData.value = tableData.value.map(x => {
    x.vs = false
    return x
  })
  pagestu.value = n
  visible.value = true
  proxy.$refs.addf.initData(data)
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
