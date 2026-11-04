<template>
  <div>
    <common-table :searchList="searchList" :searchData="searchData" :columns="columns" :tableData="tableData"
      :pagination="pagination" @search="search" @changePage="changePage" @reset="reset">
      <!-- 搜索侧按钮插槽 -->
      <template #operation>
        <t-button variant="base" theme="warning" @click="add">
          <template #icon><t-icon name="add" style="margin-right: 3px;" /></template>
          新增
        </t-button>
      </template>
      <!-- 列表操作侧按钮插槽 -->
      <template #btnAry="slotProps">
        <!-- {{slotProps.row.row.index}} -->
        <t-link theme="primary" @click="handleClickDetail(slotProps.row.row)">编辑</t-link>
        <!-- <t-link theme="primary" @click="handleClickDelete(slotProps.row,2)"> 重置密码</t-link>

      <t-popconfirm  theme="danger" content="确认删除该用户?" @confirm="onConfirmDelete(slotProps.row,1)">
         <t-link theme="danger"> 删除</t-link>
      </t-popconfirm> -->

        <!-- <t-link theme="danger" @click="handleClickDelete(slotProps.row,1)"> 删除</t-link> -->
      </template>
    </common-table>
    <!-- 新增编辑弹窗 -->
    <t-dialog v-model:visible="visible" :header="pagestu == 1 ? '创建用户' : '编辑用户'" width="860px"
      class="newDialog newclassNewDialog" :cancelBtn="{ theme: 'default', variant: 'outline' }"
      :confirmBtn="{ theme: 'warning', variant: 'base' }" :confirm-on-enter="true"
      :on-confirm="() => { proxy.$refs.addf.submt() }">
      <t-space direction="vertical" style="width: 100%">
        <addcom ref="addf" :roleGroupData="roleGroupData" :pagestu="pagestu" @sumt="sumt"></addcom>
      </t-space>
    </t-dialog>
    <!-- 确认删除弹窗 -->
    <t-dialog v-model:visible="confirmVisible" :header="deletId.n == 1 ? '确认删除该用户？' : '确认重置该用户密码？'"
      @confirm="onConfirmDelete" />
  </div>
</template>
<script setup lang="jsx">
import { listUser, saveUser, deleteUser, updateUser, userByDetail, restPwd, userState, employedStatus } from '@/api/user';
import { listRoleGp } from '@/api/roleGp';
import CommonTable from '@/components/common-table/index.vue';
import { computed, onMounted, ref, getCurrentInstance } from 'vue';
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
  name: "",
  // regionId:'',
  orgId: '',
  pageNo: pagination.value.defaultCurrent,
  pageSize: pagination.value.defaultPageSize,
  //  userId: 12345,
  // accountNumber: "",
  // phoneNumber:'',
});
const searchList = ref([
  {
    label: '姓名',
    placeholder: '请输入用户姓名',
    type: "input",
    value: "name",
  },
  // {
  //   label:'行政区域',
  //   placeholder:'请选择',
  //   type: "rgSelect",
  //   value: "regionId",
  // },
  // {
  //   label:'组织机构',
  //   placeholder:'请选择',
  //   type: "orgSelect",
  //   value: "orgId",
  // },
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
  { title: '姓名', colKey: 'name' },
  { title: '账号', colKey: 'loginId' },
  {
    title: '手机号', colKey: 'phone',
    cell: (h, { row }) => {
      return <p>
        {row.phone ? row.phone : '-'}
      </p>
        ;
    },
  },
  // {
  //   title: '行政区', colKey: 'region',
  //   cell: (h, { row }) => {
  //     return <p>
  //       {row.region.name}
  //     </p>
  //       ;
  //   },
  // },
  // {
  //   title: '组织机构', colKey: 'organization',
  //   cell: (h, { row }) => {
  //     return <p>
  //       {row?.organization?.name}
  //     </p>
  //       ;
  //   },
  // },
  {
    title: '群组', colKey: 'roleGroup',
    cell: (h, { row }) => {
      return <p>
        {row.roleGroup ? row.roleGroup.name : '-'}
      </p>
        ;
    },
  },
  {
    title: '在职状态', colKey: 'employedStatus',
    cell: (h, { row }) => {
      //  loading={row.employedStatusload}
      return (<t-switch onChange={() => change(row, 1)} v-model={row.employedStatusflag} size="medium" label={[renderActiveContent, renderInactiveContent]} >
      </t-switch>
      )
    },
  },
  {
    title: '账号状态', colKey: 'showState',
    cell: (h, { row }) => {
      return (<t-switch onChange={() => change(row, 2)} v-model={row.showStateflag} size="medium" label={[renderActiveContent, renderInactiveContent]} >
      </t-switch>
      )
    },
  },
  {
    align: 'left',
    fixed: 'right',
    width: 200,
    colKey: 'op',
    title: '操作',
  },
])
const renderActiveContent = () => {
  return <t-icon name="check" />;
};
const renderInactiveContent = () => {
  return <t-icon name="close" />;
};
const change = async (e, n) => {
  console.log(e.employedStatusflag, n)
  let p = {
    id: e.id
  }
  if (n == 1) p.employedStatus = e.employedStatusflag ? '1' : '2'
  if (n == 2) p.showState = e.showStateflag ? '1' : '2'
  let res = n === 1 ? await employedStatus(p) : await userState(p)
  if (res.status.code == 200) {
    MessagePlugin.success(res.status.message);
  } else {
    MessagePlugin.error(res.status.message);
    init()
  }
}

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
    listUser(searchData.value).then(res => {
      if (res.status.code === '200') {
        tableData.value = res.result.data.map(x => {
          x.employedStatusflag = x.employedStatus == 1 ? true : false
          x.showStateflag = x.showState == 1 ? true : false
          return x
        });
        pagination.value.total = res.result.total;
      } else {
        MessagePlugin.error(res.status.message);
      }
    });
};

const sumt = async (form) => {
  let res = pagestu.value == '1' ? await saveUser(form) : await updateUser(form)
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
const handleClickDelete = (data, n) => {
  deletId.value.id = data.row.id
  deletId.value.n = n
  confirmVisible.value = true;
};
const onConfirmDelete = async (data, n) => {
  deletId.value.id = data.row.id
  deletId.value.n = n
  confirmVisible.value = false;
  let res = deletId.value.n == 1 ? await deleteUser({ "ids": deletId.value.id }) : await restPwd(deletId.value.id)
  if (res.status.code == 200) {
    MessagePlugin.success(res.status.message);
    if (deletId.value.n == 1) init()
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
const handleClickDetail = async (data) => {
  let n = 2
  console.log(data)
  pagestu.value = n
  let formData = data
  visible.value = true
  proxy.$refs.addf.initData(formData)
}
const roleGroupData = ref([])
onMounted(() => {
  init();
  listRoleGp().then(r => {
    if (r.status.code == 200) {
      roleGroupData.value = r.result.data
      console.log(roleGroupData.value)
    }
  })
});


</script>
