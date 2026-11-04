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
        <t-link theme="primary" @click="handleClickDetail(slotProps.row.row, 2)">编辑</t-link>
        <t-link theme="success" @click="handleClickDetail(slotProps.row.row, 3)">权限</t-link>
        <!-- <t-popconfirm  theme="danger"  @confirm="onConfirmDelete(slotProps.row)">
        <template #content>
        <p class="title">确认删除该角色？</p>
        <p class="describe">角色一旦删除，对应用户的菜单将<br />无法显示，请谨慎操作。</p>
      </template>
<t-link theme="danger"> 删除</t-link>
</t-popconfirm> -->
        <t-link theme="danger" @click="handleClickDelete(slotProps.row)"> 删除</t-link>
      </template>
    </common-table>
    <!-- 新增编辑弹窗 -->
    <t-dialog v-model:visible="visible" :header="pagestu == 1 ? '创建角色' : pagestu == 2 ? '编辑角色' : '角色权限'" width="860px"
      class="newDialog newclassNewDialog" :cancelBtn="{ theme: 'default', variant: 'outline' }"
      :confirmBtn="{ theme: 'warning', variant: 'base' }" :top="pagestu == 3 ? '5vh' : '18vh'" :confirm-on-enter="true"
      :on-confirm="() => { proxy.$refs.addf.submt() }">
      <t-space direction="vertical" style="width: 100%">
        <addcom ref="addf" v-if="fresh" :pagestu="pagestu" :phList="phList" :pcList="pcList" @sumt="sumt"></addcom>
      </t-space>
    </t-dialog>
    <!-- 确认删除弹窗 -->
    <t-dialog v-model:visible="confirmVisible" header="删除确认" class="newDialog newclassNewDialog"
      :cancelBtn="{ theme: 'default', variant: 'outline' }" :confirmBtn="{ theme: 'warning', variant: 'base' }"
      @confirm="onConfirmDelete">
      <t-space direction="vertical" style="width: 100%">
        <div class="deletCon">
          <div class="delectConHeader"><t-icon name="error-circle-filled" size="18px" color="rgba(255, 41, 75, 1)" />
            <span class="a">确认删除该角色</span>
          </div>
          <div class="b">角色一旦删除，对应用户的菜单将无法显示，请谨慎操作。</div>
        </div>
      </t-space>
    </t-dialog>
  </div>
</template>
<script setup lang="jsx">
import { listRole, saveRole, deleteRole, updateRole, RoleByDetail, roleRights, roleRightsGet, roleMenu, roleMenuGet } from '@/api/role';
import { listMenu } from '@/api/menu';
import CommonTable from '@/components/common-table/index.vue';
import { computed, onMounted, ref, getCurrentInstance, nextTick } from 'vue';
import { t } from '@/locales';
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
  pageNo: pagination.value.defaultCurrent,
  pageSize: pagination.value.defaultPageSize,
  //  userId: 12345,
  // regionId:'',
  //  orgId: 12345,
});
const searchList = ref([
  {
    label: '角色名称',
    placeholder: '请输入角色名称',
    type: "input",
    value: "name",
  },
  // {
  //   label:'行政区域',
  //   placeholder:'请选择',
  //   type: "rgSelect",
  //   value: "regionId",
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
  { title: '名称', colKey: 'name' },
  // { title:'行政区', colKey: 'region',
  // cell: (h, { row }) => {
  //     return <p>
  //       {row.region.name}
  //       </p>
  //     ;
  //   },
  // },
  // { title:'组织机构', colKey: 'organization',
  // cell: (h, { row }) => {
  //     return <p>
  //       {row?.organization?.name}
  //       </p>
  //     ;
  //   },
  // },
  {
    title: '角色组', colKey: 'roleGroup',
    cell: (h, { row }) => {
      // return <span class='rolegp'>
      //   {row.listRoleGroup.map(item => (
      //     <span key={item.id}>{item.name}</span>
      //   ))}
      // </span>
      let ary = []
      row.listRoleGroup.map(x => ary.push(x.name))
      return <span class='rolegp'>
        {ary.length == 0 ? '-' : ary.join(',')}
      </span>
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
  searchData.value.pageNo = pagination.value.defaultCurrent
  searchData.value.pageSize = pagination.value.defaultPageSize
  listRole(searchData.value).then(res => {
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
  let res = pagestu.value == '1' ? await saveRole(form) : pagestu.value == '2' ? await updateRole(form) : await roleMenu(form)
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
  let res = await deleteRole(deletId.value.id)
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
const phList = ref([])
const pcList = ref([])
const getMenu = () => {
  listMenu(searchData.value).then(res => {
    if (res.status.code === '200') {
      pcList.value = res.result.filter(x => x.type == 1)
      phList.value = res.result.filter(x => x.type == 2)

      const st = (data) => {
        data.sort((a, b) => a.orderNum - b.orderNum);
        for (let i = 0; i < data.length; i++) {
          if (data[i].children) st(data[i].children)
        }
      }
      st(pcList.value)
      st(phList.value)



      // let data=res.result.filter(x=>x.type==1)
      // let datb=res.result.filter(x=>x.type==2)
      // pcList.value=[{
      //     "id": 0,
      //     "name": "全部菜单",
      //     "openUrl": "",
      //     "parentId": -1,
      //     type:1,
      //     children:data
      // }]
      // phList.value=[{
      //     "id": 0,
      //     "name": "全部菜单",
      //     "openUrl": "",
      //     "parentId": -1,
      //     type:1,
      //     children:datb
      // }]
    } else {
      MessagePlugin.error(res.status.message);
    }
  });

}
const fresh = ref(true)
const handleClickDetail = async (data, n) => {
  console.log(data)
  pagestu.value = n
  let formData = data
  formData.pagestu = n
  if (n == 3) {
    fresh.value = false
    roleMenuGet(data.id).then(r => {
      if (r.status.code == 200) formData.listMenu = r.result.listMenu
      fresh.value = true
      nextTick(() => {
        visible.value = true
        proxy.$refs.addf.initData(formData)
      });

    })
  } else {
    visible.value = true
    proxy.$refs.addf.initData(formData)
  }
}
onMounted(() => {
  // getMenu() 
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
