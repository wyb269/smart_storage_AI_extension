<template>
  <div class="list-common-table">
    <t-space direction="vertical" style="width: 100%" class="tdesign-tree-operations">
      <t-input v-model="filterText" @change="onInputChange" style="width: 200px;margin-bottom: 20px;"
        placeholder="请输入节点名称">
        <template #prefix-icon>
          <t-icon name="search" />
        </template>
      </t-input>
    </t-space>

    <t-enhanced-table ref="tableRef" v-model:expandedTreeNodes="expandedTreeNodes" row-key="id" drag-sort="row-handler"
      :data="tableData" :columns="columns" :tree="treeConfig" :tree-expand-and-fold-icon="treeExpandIcon"
      :before-drag-sort="beforeDragSort" @abnormal-drag-sort="onAbnormalDragSort" @drag-sort="onDragSort">
      <template #op="slotProps">
        <t-space>

          <div class="tdesign-table-demo__table-operations">
            <!---:disabled="slotProps.row.levels==2||slotProps.row.levels==1"----->
            <t-link variant="text" :disabled="slotProps.row.levels == 2" hover="color" theme='primary'
              style='padding-left:0' @click="add(slotProps.row, 1)">
              添加下级部门
            </t-link>
            <!-----<t-link variant="text" hover="color" :disabled="slotProps.row.levels==2" theme='primary' @click="add(slotProps.row,2)">
          添加下级库区
        </t-link>---->
            <t-link variant="text" hover="color" theme='success' @click="update(slotProps.row)">
              编辑
            </t-link>
            <!-- <t-popconfirm  theme="danger"  @confirm="onConfirmDelete(slotProps.row)">
          <template #content>
            <p class="title">确认删除该数据？</p>
            <p class="describe">数据一旦删除，对应绑定用户将<br />丢失组织机构，请谨慎操作。</p>
          </template>
  <t-link theme="danger" :disabled="slotProps.row.levels==0"> 删除</t-link>
  </t-popconfirm> -->

            <t-link theme="danger" :disabled="slotProps.row.levels == 0"
              @click="handleClickDelete(slotProps.row)">删除</t-link>


          </div>


        </t-space>
      </template>
    </t-enhanced-table>
    <!-- 新增编辑弹窗 -->
    <t-dialog v-model:visible="visible" top="13vh" :header="pagestu[0] == 1 ? '创建节点' : '编辑节点'" width="860px"
      class="newDialog newclassNewDialog" :cancelBtn="{ theme: 'default', variant: 'outline' }"
      :confirmBtn="{ theme: 'warning', variant: 'base' }" :on-confirm="() => { proxy.$refs.addf.submt() }">
      <t-space direction="vertical" style="width: 100%">
        <addcom ref="addf" :roleGroupData="roleGroupData" :pagestu="pagestu" @sumt="sumt"></addcom>
      </t-space>
    </t-dialog>
    <t-dialog v-model:visible="confirmVisible" header="删除确认" class="newDialog newclassNewDialog"
      :cancelBtn="{ theme: 'default', variant: 'outline' }" :confirmBtn="{ theme: 'warning', variant: 'base' }"
      @confirm="onConfirmDelete">
      <t-space direction="vertical" style="width: 100%">
        <div class="deletCon">
          <div class="delectConHeader"><t-icon name="error-circle-filled" size="18px" color="rgba(255, 41, 75, 1)" />
            <span class="a">确认删除该数据</span>
          </div>
          <div class="b">数据一旦删除，对应绑定用户将丢失组织机构，请谨慎操作。</div>
        </div>
      </t-space>
    </t-dialog>
  </div>
</template>
<script setup lang="jsx">
import { ref, reactive, computed, onMounted, getCurrentInstance } from 'vue';
import { EnhancedTable as TEnhancedTable, MessagePlugin, Loading } from 'tdesign-vue-next';
import {
  ChevronRightIcon,
  ChevronDownIcon,
  MoveIcon,
  AddRectangleIcon,
  MinusRectangleIcon,
} from 'tdesign-icons-vue-next';
import { listOrg, saveOrg, deleteOrg, updateOrg, orgByDetail, orgCas } from '@/api/org';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/store';
import addcom from './components/add.vue';
const userStore = useUserStore();
var bakData

const { proxy } = getCurrentInstance();
const visible = ref(false);
// const tableData = ref(getData());
const tableData = ref([]);
const treeConfig = reactive({
  childrenKey: 'children',
  treeNodeColumnIndex: 0,
  indent: 55,
  // expandTreeNodeOnClick: true,
});
const filterText = ref('')
const onInputChange = () => {
  const fl = (son, part) => {
    // console.log(part,55)
    son = son.filter((y) => {
      if (y.name.indexOf(filterText.value) > -1) {
        return y
      } else {
        y.a = false
        if (y.children) y.a = fl(y.children, y)
        return y.a == true
      }
    })
    console.log(son, 77)
    part.children = son
    if (son.length > 0) return true
  }
  let ary = JSON.parse(JSON.stringify(bakData))
  tableData.value = ary.filter((x) => {
    if (x.name.indexOf(filterText.value) > -1) {
      return x
    } else {
      console.log(x, 66)
      x.a = false
      if (x.children) x.a = fl(x.children, x)
      return x.a == true
    }
  })

  console.log(tableData.value)

}


//创建，编辑   //三个节点
const pagestu = ref(['1', '1'])
const update = (node) => {
  visible.value = true
  pagestu.value[0] = '2'
  pagestu.value[1] = node.levels + 1
  proxy.$refs.addf.initData(node)
}
const add = (node, n) => {
  console.log(node, 11)
  visible.value = true
  pagestu.value[0] = '1'
  pagestu.value[1] = node.levels + 2
  let f = node
  f.sonType = n
  proxy.$refs.addf.rst(f)
}


const confirmVisible = ref(false);
const deletId = ref({})
const handleClickDelete = (node) => {
  deletId.value.id = node.id
  confirmVisible.value = true;
};
const onConfirmDelete = async () => {
  confirmVisible.value = false;
  let res = await deleteOrg(deletId.value.id)
  if (res.status.code == 200) {
    MessagePlugin.success(res.status.message);
    init()
  } else {
    MessagePlugin.error(res.status.message);
  }
};


const treeExpandIcon = computed(() => {
  // 自定义展开图标
  // if (customTreeExpandAndFoldIcon.value) {
  //   return treeExpandAndFoldIconRender;
  // }
  // return lazyLoadingTreeIconRender;
  return treeExpandAndFoldIconRender;
});

const columns = [
  // {
  //   // 列拖拽排序必要参数
  //   colKey: 'drag',
  //   title: '排序',
  //   // eslint-disable-next-line @typescript-eslint/no-unused-vars
  //   cell: (h) => <MoveIcon />,
  //   width: 46,
  // },
  {
    width: 180,
    colKey: 'name',
    title: '名称',
    ellipsis: true,
  },
  {
    colKey: 'id',
    title: '类型',
    ellipsis: true,
    width: 80,
    cell: (h, { row }) => {
      return <span> {row.levels == 0 ? '公司' : row.type == 1 ? '部门' : '库区'}</span>

    },
  },

  {
    align: 'left',
    fixed: 'right',
    width: 200,
    colKey: 'op',
    title: '操作',
  },
];

const searchData = ref({
  pageNo: 0,
  pageSize: 0,
  name: ''
})
const onReset = () => {
  searchData.value = {
    pageNo: 0,
    pageSize: 0,
    name: ''
  }
  init();
};
const init = () => {

  listOrg(searchData.value).then(res => {
    if (res.status.code === '200') {
      let data = res.result.data
      let dataAry = []
      const get = (parentId, p) => {
        let ary = data.filter((x) => {
          if (x.parentId == parentId) {
            x.label = x.name
            x.children = []
            x.children = get(x.id, x.children)
            return x
          }
        })
        p = ary
        return p
      }
      dataAry = get(-1, dataAry)
      console.log(dataAry)
      tableData.value = dataAry
      bakData = JSON.parse(JSON.stringify(tableData.value))
      setTimeout(() => {
        tableRef.value.expandAll()
      }, 100)

    } else {
      MessagePlugin.error(res.status.message);
    }
  });
};

const sumt = async (form) => {
  let res = pagestu.value[0] == '1' ? await saveOrg(form) : await updateOrg(form)
  if (res.status.code == 200) {
    visible.value = false
    onReset()
    MessagePlugin.success(res.status.message);
  } else {
    MessagePlugin.error(res.status.message);
  }
}

onMounted(() => {
  init();

});


const tableRef = ref(null);
const lazyLoadingData = ref(null);
// 非必须，如果不传，表格有内置树形节点展开逻辑
const expandedTreeNodes = ref([]);

const treeExpandAndFoldIconRender = (h, { type, row }) => {
  if (lazyLoadingData.value && lazyLoadingData.value.key === row?.key) {
    return <Loading size="14px" />;
  }
  return type === 'expand' ? <ChevronRightIcon /> : <ChevronDownIcon />;
};

const onAbnormalDragSort = (params) => {
  console.log(params);
  // MessagePlugin.warning(params.reason);
  if (params.code === 1001) {
    MessagePlugin.warning('不同层级的元素，不允许调整顺序');
  }
};


const onDragSort = (params) => {
  console.log('onDragSort:', params);
};

// 应用于需要阻止拖拽排序的场景。如：当子节点存在时，则不允许调整顺序。
// 返回值为 true，允许拖拽排序；返回值 为 false，则阻止拖拽排序
const beforeDragSort = (params) => {
  console.log('beforeDragSort:', params);
  return true;
};


</script>

<style scoped>
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
<style scoped lang="less">
:deep(.tdesign-table-demo__table-operations .t-link) {
  padding: 0 8px;
}

:deep(.t-table th) {
  background: #efefef
}

:deep(.t-table__th-cell-inner) {
  color: #6c6c6c;
}

:deep(.t-table__content) {
  border: 1px solid #eee;
  border-bottom: none;
}

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
