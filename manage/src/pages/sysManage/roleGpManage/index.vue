<template>
  <div>
    <div class="forNewClassTableAndsearch">
      <div class="list-card-operation tdop">
        <t-button variant="base" theme="warning" @click="add">
          <template #icon><t-icon name="add" style="margin-right: 3px;" /></template>
          新增
        </t-button>
        <div class="search-input">
          <t-input v-model="searchData.name" @change="search" placeholder="请输入角色组名称">
            <template #suffix-icon>
              <t-icon name="search" />
            </template>
          </t-input>
        </div>
      </div>

      <template v-if="pagination.total > 0 && !dataLoading">
        <div class="list-card-items">
          <t-row :gutter="[16, 16]">
            <t-col v-for="product in tableData" :key="product.index" :lg="4" :xs="6" :xl="3">
              <product-card class="list-card-item" :product="product" @delete-item="handleClickDelete"
                @handleClickDetail="handleClickDetail" />
            </t-col>
          </t-row>
        </div>
        <div class="list-card-pagination list-common-table">
          <!-- @page-size-change="onPageSizeChange" -->
          <!-- @current-change="changePage" -->
          <t-pagination v-model="pagination.defaultCurrent" v-model:page-size="pagination.defaultPageSize"
            :total="pagination.total" :page-size-options="[8, 16, 24]" @change="changePage" />
        </div>
      </template>
      <div v-if="tableData.length == 0" style="margin-top: 130px;">
        <div style="color: #999;text-align: center;font-size: 25px;">
          <t-icon name="system-search" />
        </div>
        <div style="color: #999;text-align: center;margin-top:5px;">

          暂无数据</div>
      </div>

      <div v-else-if="dataLoading" class="list-card-loading">
        <t-loading size="large" text="加载数据中..." />
      </div>
    </div>
    <!-- 新增编辑弹窗 -->
    <t-dialog v-model:visible="visible" :header="pagestu == 1 ? '创建角色组' : pagestu == 2 ? '编辑角色组' : '详情'" width="860px"
      class="newDialog newclassNewDialog" :cancelBtn="{ theme: 'default', variant: 'outline' }"
      :confirmBtn="pagestu == 3 ? null : { theme: 'warning', variant: 'base' }" :confirm-on-enter="true"
      :on-cancel="onCancel" :on-esc-keydown="onEscKeydown" :on-close-btn-click="onCloseBtnClick"
      :on-overlay-click="onOverlayClick" :on-close="close" :on-confirm="() => { proxy.$refs.addf.submt() }">
      <t-space direction="vertical" style="width: 100%">
        <addcom ref="addf" :RoleOptionList="RoleOptionList" :pagestu="pagestu" @sumt="sumt"></addcom>
      </t-space>
    </t-dialog>
    <!-- 确认删除弹窗 -->
    <t-dialog v-model:visible="confirmVisible" header="删除确认" class="newDialog newclassNewDialog"
      :cancelBtn="{ theme: 'default', variant: 'outline' }" :confirmBtn="{ theme: 'warning', variant: 'base' }"
      @confirm="onConfirmDelete">
      <t-space direction="vertical" style="width: 100%">
        <div class="deletCon">
          <div class="delectConHeader"><t-icon name="error-circle-filled" size="18px" color="rgba(255, 41, 75, 1)" />
            <span class="a">确认删除该角色组</span>
          </div>
          <div class="b">角色组一旦删除，对应用户的菜单将无法显示，请谨慎操作。</div>
        </div>
      </t-space>
    </t-dialog>
  </div>
</template>
<script setup lang="jsx">
import { listRoleGp, saveRoleGp, deleteRoleGp, updateRoleGp, RoleGpByDetail, RoleList } from '@/api/roleGp';
import CommonTable from '@/components/common-table/index.vue';
import { computed, onMounted, ref, getCurrentInstance } from 'vue';
import { t } from '@/locales';
import addcom from './components/add.vue';
import { useRouter } from 'vue-router';
import { MessagePlugin } from 'tdesign-vue-next';
import ProductCard from './components/product-card.vue';

const { proxy } = getCurrentInstance();
const router = useRouter();

const dataLoading = ref(false);
const pagination = ref({
  defaultPageSize: 8,
  total: 0,
  defaultCurrent: 1,
});
const searchData = ref({
  // "pageTotal": 12345,
  // "userId": 12345,
  // "regionId": 12345,
  // "orgId": 12345,
  name: "",
  pageNo: pagination.value.defaultCurrent,
  pageSize: pagination.value.defaultPageSize,
});
const searchList = ref([
  {
    label: '分组名称',
    placeholder: '请输入分组名称',
    type: "input",
    value: "name",
  },
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
  {
    title: '行政区', colKey: 'region',
    cell: (h, { row }) => {
      //  v-show={row.contractType==2}
      return <p>
        {row.region.name}
      </p>
        ;
    },
  },
  {
    title: '组织机构', colKey: 'organization',
    cell: (h, { row }) => {
      //  v-show={row.contractType==2}
      return <p>
        {row.organization.name}
      </p>
        ;
    },
  },
  {
    title: '创建时间', colKey: 'addTime',
  },
  {
    align: 'left',
    fixed: 'right',
    width: 200,
    colKey: 'op',
    title: t('components.commonTable.operation'),
  },
])

const sumt = async (form) => {
  let res = pagestu.value == '1' ? await saveRoleGp(form) : await updateRoleGp(form)
  if (res.status.code == 200) {
    visible.value = false
    if (pagestu.value = '1') reset()
    if (pagestu.value = '2') init()
    MessagePlugin.success(res.status.message);
  } else {
    MessagePlugin.error(res.status.message);
  }
}

const tableData = ref([])
const search = () => {
  pagination.value.total = 0
  pagination.value.defaultCurrent = 1
  init();
};
const changePage = (p) => {
  pagination.value.defaultCurrent = p.current
  pagination.value.defaultPageSize = p.pageSize
  init()
}
const reset = () => {
  pagination.value.total = 0
  pagination.value.defaultCurrent = 1
  searchData.value = {};
  init();
};
const init = () => {
  dataLoading.value = true
  searchData.value.pageNo = pagination.value.defaultCurrent
  searchData.value.pageSize = pagination.value.defaultPageSize
  listRoleGp(searchData.value).then(res => {
    dataLoading.value = false
    if (res.status.code === '200') {
      tableData.value = res.result.data.map(x => {
        x.math = Math.floor(Math.random() * 10)
        return x
      })
      pagination.value.total = res.result.total;
    } else {
      MessagePlugin.error(res.status.message);
    }
  });
};
const confirmVisible = ref(false);
const deletId = ref({})
const handleClickDelete = (data) => {
  deletId.value.id = data.id
  confirmVisible.value = true;
};
const onConfirmDelete = async (data) => {
  confirmVisible.value = false;
  let res = await deleteRoleGp(deletId.value.id)
  if (res.status.code == 200) {
    MessagePlugin.success(res.status.message);
    init()
  } else {
    MessagePlugin.error(res.status.message);
  }
};

const onCancel = () => {

};

const visible = ref(false);
const pagestu = ref('1')
const add = () => {
  pagestu.value = '1'
  visible.value = true
  proxy.$refs.addf.rst()
}
const handleClickDetail = async (data) => {
  //  router.push('/sysManage/userManageDeatil');
  console.log(data)
  pagestu.value = 2
  let formData = data
  formData.roleIds = []
  data.listRole.map(x => {
    formData.roleIds.push(x.id)
  })
  visible.value = true
  proxy.$refs.addf.initData(formData)
}
const RoleOptionList = ref([{ label: '全选', checkAll: true }])
const getRoleList = async () => {
  let res = await RoleList({
    "pageNo": '1',
    "pageSize": '1000',
    // "pageTotal": 12345,
    // "userId": 12345,
    // "regionId": 12345,
    // "orgId": 12345,
    // "name":''
  })
  if (res.status.code == 200) {
    RoleOptionList.value = [{ label: '全选', checkAll: true }]
    res.result.data.map(x => {
      RoleOptionList.value.push({ label: x.name, value: x.id })
    })
  }
}
onMounted(() => {
  getRoleList()
  init();
});


</script>



<style lang="less" scoped>
.list-card {
  height: 100%;

  &-operation {
    display: flex;
    justify-content: space-between;
    margin-bottom: var(--td-comp-margin-xxl);

    .search-input {
      width: 360px;
    }
  }

  &-item {
    padding: var(--td-comp-paddingTB-xl) var(--td-comp-paddingTB-xl);

    :deep(.t-card__header) {
      padding: 0;
    }

    :deep(.t-card__body) {
      padding: 0;
      margin-top: var(--td-comp-margin-xxl);
      margin-bottom: var(--td-comp-margin-xxl);
    }

    :deep(.t-card__footer) {
      padding: 0;
    }
  }

  &-pagination {
    padding: var(--td-comp-paddingTB-xl) var(--td-comp-paddingTB-xl);
  }

  &-loading {
    height: 100%;
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
