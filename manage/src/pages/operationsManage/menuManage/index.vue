<template>
  <div class="list-common-table">
    <!-- <div>
      <t-button @click="appendToRoot">添加根节点</t-button>
      <t-button theme="default" style="margin-left: 16px" @click="resetData">重置/更新数据</t-button>
      <t-button theme="default" style="margin-left: 16px" @click="onRowToggle">任意节点展开/收起</t-button>
      <t-button theme="default" style="margin-left: 16px" @click="onExpandAllToggle">{{
        expandAll ? '收起全部' : '展开全部'
      }}</t-button>
      <t-button theme="default" style="margin-left: 16px" @click="getTreeNode">获取全部树形结构</t-button>
    </div>
    <br />
    <div>
      <t-checkbox v-model="customTreeExpandAndFoldIcon" style="vertical-align: middle">
        自定义折叠/展开图标
      </t-checkbox>
    </div>
    <br /> -->
    <!-- !!! 树形结构 EnhancedTable 才支持，普通 Table 不支持 !!! -->
    <!-- 第一列展开树结点，缩进为 24px，子节点字段 childrenKey 默认为 children -->
    <!-- v-model:displayColumns="displayColumns" used to control displayed columns -->
    <!-- v-model:expandedTreeNodes is not required. you can control expanded tree node by expandedTreeNodes -->

    <!-- <t-tabs v-model="theme" @change="init" scrollPosition="end" :class="theme==1?'a':'b'">
    <t-tab-panel :value="1" label="管理端">
    </t-tab-panel>
    <t-tab-panel :value="2" label="移动端">

    </t-tab-panel>
  </t-tabs> -->

    <t-radio-group v-model="theme" variant="default-filled" style="margin-bottom:15px;" @change="init">
      <t-radio-button value="1">电脑端</t-radio-button>
      <t-radio-button value="2">移动端</t-radio-button>
    </t-radio-group>

    <t-space direction="vertical" style="width: 100%;margin-top: -17px;" class="tdesign-tree-operations">
      <t-input v-model="filterText" @change="onInputChange" style="width: 200px;margin-bottom: 20px;margin-top: 20px;"
        placeholder="请输入节点名称">
        <template #prefix-icon>
          <t-icon name="search" />
        </template>
      </t-input>
    </t-space>

    <t-enhanced-table ref="tableRef" v-model:expandedTreeNodes="expandedTreeNodes" row-key="id" drag-sort="row-handler"
      :data="tableData" :columns="columns" :tree="treeConfig" :tree-expand-and-fold-icon="treeExpandIcon"
      :before-drag-sort="beforeDragSort" @page-change="onPageChange" @abnormal-drag-sort="onAbnormalDragSort"
      @drag-sort="onDragSort" @expanded-tree-nodes-change="onExpandedTreeNodesChange">
      <template #op="slotProps">
        <t-space>

          <div class="tdesign-table-demo__table-operations">

            <t-link variant="text" hover="color" theme='primary' @click="add(slotProps.row,)">
              添加下级菜单
            </t-link>
            <t-link variant="text" :disabled="slotProps.row.levels == 0" hover="color" theme='success'
              @click="update(slotProps.row)">
              编辑
            </t-link>
            <!-- <t-popconfirm  theme="danger"  @confirm="onConfirmDelete(slotProps.row)">
          <template #content>
            <p class="title">确认删除该菜单？</p>
            <p class="describe">菜单一旦删除，对应绑定用户将<br />无法看到页面，请谨慎操作。</p>
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
            <span class="a">确认删除该菜单</span>
          </div>
          <div class="b">菜单一旦删除，对应绑定用户将无法看到页面，请谨慎操作。</div>
        </div>
      </t-space>
    </t-dialog>
    <!-- @tree-expand-change="onTreeExpandChange" -->

    <!-- 第二列展开树结点，缩进为 12px，示例代码有效，勿删 -->
    <!-- indent 定义缩进距离 -->
    <!-- 如果子结点字段不是 'children'，可以使用 childrenKey 定义字段别名，如 `:tree="{ childrenKey: 'list' }"` -->
    <!-- <t-enhanced-table
      ref="tableRef"
      rowKey="key"
      :pagination="defaultPagination"
      :data="data"
      :columns="columns"
      :tree="{ indent: 12, childrenKey: 'list', defaultExpandAll: true }"
      @page-change="onPageChange"
    ></t-enhanced-table> -->
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
import { listMenu, saveMenu, deleteMenu, updateMenu } from '@/api/menu';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/store';
import addcom from './components/add.vue';
const userStore = useUserStore();
var bakData

const { proxy } = getCurrentInstance();
const visible = ref(false);
// const tableData = ref(getData());
const tableData = ref([]);
const theme = ref('1')
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
const pagestu = ref('1')
const update = (node) => {
  visible.value = true
  pagestu.value = '2'
  proxy.$refs.addf.initData(node)
}
const add = (node) => {
  console.log(node)
  visible.value = true
  pagestu.value = '1'
  proxy.$refs.addf.rst(node)
}


const confirmVisible = ref(false);
const deletId = ref({})
const handleClickDelete = (node) => {
  deletId.value.id = node.id
  confirmVisible.value = true;
};
const onConfirmDelete = async () => {
  confirmVisible.value = false;
  let res = await deleteMenu(deletId.value.id)
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
  // {
  //   colKey: 'id',
  //   title: '类型',
  //   ellipsis: true,
  //   width: 80,
  //     cell: (h, { row }) => {
  //     return <span> {row.levels==0?'公司':row.type==1?'部门':'库区'}</span>

  //   },
  // },
  // {
  //   colKey: 'platform',
  //   title: '签署方式',
  //   width: 100,
  // },

  {
    align: 'left',
    fixed: 'right',
    width: 200,
    colKey: 'op',
    title: '操作',
  },
];

const searchData = ref({
  // pageNo:0,
  // pageSize:0,
  // name:''
})
const onReset = () => {
  searchData.value = {
    // pageNo:0,
    // pageSize:0,
    // name:''
  }
  init();
};
const init = () => {

  // 单路由无子菜单 openUrl     sysManage/roleManage
  // 列表与详情并级，详情隐藏 openUrl     dictManage/dictData
  // 其余情况 openUrl  都是对应的文件名且无'/'

  listMenu(searchData.value).then(res => {
    if (res.status.code === '200') {
      let data = [
        {
          "id": 0,
          "name": "全部菜单",
          "openUrl": "",
          "parentId": -1,
          type: theme.value,
          levels: 0,
          children: []
        }
      ]
      res.result.map((x) => {
        if (x.type == theme.value) data[0].children.push(x)
      })
      const st = (d) => {
        d.sort((a, b) => a.orderNum - b.orderNum);
        for (let i = 0; i < d.length; i++) {
          if (d[i].children) st(d[i].children)
        }
      }
      st(data[0].children)
      tableData.value = data
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
  let res = pagestu.value == '1' ? await saveMenu(form) : await updateMenu(form)
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










const TOTAL = 5;

function getObject(i, currentPage) {
  return {
    id: i,
    key: `申请人 ${i}_${currentPage} 号`,
    platform: ['电子签署', '纸质签署', '纸质签署'][i % 3],
    type: ['String', 'Number', 'Array', 'Object'][i % 4],
    default: ['-', '0', '[]', '{}'][i % 4],
    detail: {
      position: `读取 ${i} 个数据的嵌套信息值`,
    },
    needed: i % 4 === 0 ? '是' : '否',
    description: '数据源',
  };
}

function getData(currentPage = 1) {
  const data = [];
  // const pageInfo = `第 ${currentPage} 页`;
  for (let i = 0; i < TOTAL; i++) {
    const obj = getObject(i, currentPage);
    // 第一行不设置子节点
    obj.list = new Array(2).fill(null).map((t, j) => {
      const secondIndex = 100 * j + (i + 1) * 10;
      const secondObj = {
        ...obj,
        id: secondIndex,
        key: `申请人 ${secondIndex}_${currentPage} 号`,
      };
      secondObj.list = new Array(3).fill(null).map((m, n) => {
        const thirdIndex = secondIndex * 1000 + 100 * m + (n + 1) * 10;
        return {
          ...obj,
          id: thirdIndex,
          key: `申请人 ${thirdIndex}_${currentPage} 号`,
          list: true,
        };
      });
      return secondObj;
    });
    // 第一行不设置子节点
    if (i === 0) {
      obj.list = [];
    }
    data.push(obj);
  }
  // 懒加载1
  data.push({
    ...getObject(66666, currentPage),
    /** 如果子节点为懒加载，则初始值设置为 true */
    list: true,
    key: '申请人懒加载节点 66666，点我体验',
  });
  // 懒加载2
  data.push({
    ...getObject(88888, currentPage),
    /** 如果子节点为懒加载，则初始值设置为 true */
    list: true,
    key: '申请人懒加载节点 88888，点我体验 ',
  });
  return data;
}

const tableRef = ref(null);

const lazyLoadingData = ref(null);
const columnController = ref({
  placement: 'bottom-left',
  // 允许控制哪些列显示或隐藏
  fields: ['id', 'platform', 'operate'],
  dialogProps: { preventScrollThrough: true },
});

// 非必须，如果不传，表格有内置树形节点展开逻辑
const expandedTreeNodes = ref([]);



// 重置数据和展开节点
const resetData = () => {
  const newData = getData();
  // 方式一
  data.value = newData;
  expandedTreeNodes.value = [];

  // 方式二
  // tableRef.value.resetData(newData);
};

const onEditClick = (row) => {
  const newData = {
    ...row,
    platform: 'New',
    type: 'Symbol',
    default: 'undefined',
  };
  tableRef.value.setData(row.key, newData);
  MessagePlugin.success('数据已更新');
};

const onDeleteConfirm = (row) => {
  // 移除当前节点及其所有子节点
  tableRef.value.remove(row.key);

  // 仅移除所有子节点
  // tableRef.value.removeChildren(row.key);
  MessagePlugin.success('删除成功');
};

const onLookUp = (row) => {
  const allRowData = tableRef.value.getData(row.key);
  const message = '当前行全部数据，包含节点路径、父节点、子节点、是否展开、是否禁用等';
  MessagePlugin.success(`打开控制台查看${message}`);
  console.log(`${message}：`, allRowData);
};

const appendTo = (row) => {
  const randomKey1 = Math.round(Math.random() * Math.random() * 1000) + 10000;
  tableRef.value.appendTo(row.key, {
    id: randomKey1,
    key: `申请人 ${randomKey1} 号`,
    platform: '电子签署',
    type: 'Number',
  });
  MessagePlugin.success(`已插入子节点申请人 ${randomKey1} 号，请展开查看`);

  // 一次性添加多个子节点。示例代码有效，勿删！!!
  // appendMultipleDataTo(row);
};

function appendMultipleDataTo(row) {
  const randomKey1 = Math.round(Math.random() * Math.random() * 1000) + 10000;
  const randomKey2 = Math.round(Math.random() * Math.random() * 1000) + 10000;
  const randomKey3 = Math.round(Math.random() * Math.random() * 1000) + 10000;
  const appendList = [
    {
      id: randomKey1,
      key: `申请人 ${randomKey1} 号`,
      platform: '电子签署',
      type: 'Number',
    },
    {
      id: randomKey2,
      key: `申请人 ${randomKey2} 号`,
      platform: '纸质签署',
      type: 'Number',
    },
    {
      id: randomKey3,
      key: `申请人 ${randomKey3} 号`,
      platform: '纸质签署',
      type: 'Number',
      list: true,
    },
  ];
  tableRef.value.appendTo(row?.key, appendList);
  MessagePlugin.success(`已插入子节点申请人 ${randomKey1} 和 ${randomKey2} 号，请展开查看`);
}

// 当前节点之前，新增兄弟节前
const insertBefore = (row) => {
  const randomKey = Math.round(Math.random() * Math.random() * 1000) + 10000;
  tableRef.value.insertBefore(row.key, {
    id: randomKey,
    key: `申请人 ${randomKey} 号`,
    platform: '纸质签署',
    type: 'Number',
  });
  MessagePlugin.success(`已插入子节点申请人 ${randomKey} 号，请展开查看`);
};

// 当前节点之后，新增兄弟节前
const insertAfter = (row) => {
  const randomKey = Math.round(Math.random() * Math.random() * 1000) + 10000;
  tableRef.value.insertAfter(row.key, {
    id: randomKey,
    key: `申请人 ${randomKey} 号`,
    platform: '纸质签署',
    type: 'Number',
  });
  MessagePlugin.success(`已插入子节点申请人 ${randomKey} 号，请展开查看`);
};


const expandAll = ref(false);
const pagination = reactive({
  current: 1,
  pageSize: TOTAL,
  total: TOTAL,
});

// const defaultPagination = {
//   defaultCurrent: 1,
//   defaultPageSize: TOTAL,
//   total: TOTAL,
// };

const onPageChange = (pageInfo) => {
  pagination.current = pageInfo.current;
  pagination.pageSize = pageInfo.pageSize;
  data.value = getData(pageInfo.current);
};

const onRowToggle = () => {
  const rowIds = ['申请人 1_1 号', '申请人 2_1 号', '申请人 3_1 号', '申请人 4_1 号'];
  rowIds.forEach((id) => {
    // getData 参数为行唯一标识，lodash.get(row, rowKey)
    const rowData = tableRef.value.getData(id);
    tableRef.value.toggleExpandData(rowData);
    // 或者
    // tableRef.value.toggleExpandData({ rowIndex: rowData.rowIndex, row: rowData.row });
  });
};

const customTreeExpandAndFoldIcon = ref(false);

const treeExpandAndFoldIconRender = (h, { type, row }) => {
  if (lazyLoadingData.value && lazyLoadingData.value.key === row?.key) {
    return <Loading size="14px" />;
  }
  return type === 'expand' ? <ChevronRightIcon /> : <ChevronDownIcon />;
};

// 懒加载图标渲染
const lazyLoadingTreeIconRender = (h, params) => {
  const { type, row } = params;
  if (lazyLoadingData.value && lazyLoadingData.value.key === row?.key) {
    return <Loading size="14px" />;
  }
  return type === 'expand' ? <AddRectangleIcon /> : <MinusRectangleIcon />;
};

// 默认展开全部。示例代码有效，勿删
// onMounted(() => {
//   tableRef.value.expandAll();
// });

const getTreeNode = () => {
  // 查看树形结构平铺数据
  // tableRef.value.dataSource
  const treeData = tableRef.value.getTreeNode();
  console.log(treeData);
  MessagePlugin.success('树形结构获取成功，请打开控制台查看');
};

const onExpandAllToggle = () => {
  expandAll.value = !expandAll.value;
  expandAll.value ? tableRef.value.expandAll() : tableRef.value.foldAll();
};

const appendToRoot = () => {
  const key = Math.round(Math.random() * 10010);
  const newData = {
    id: key,
    key: `申请人 ${key}_${1} 号`,
    platform: key % 2 === 0 ? '共有' : '私有',
    type: ['String', 'Number', 'Array', 'Object'][key % 4],
    default: ['-', '0', '[]', '{}'][key % 4],
    detail: {
      position: `读取 ${key} 个数据的嵌套信息值`,
    },
    needed: key % 4 === 0 ? '是' : '否',
    description: '数据源',
  };
  // data.value.push(newData);
  tableRef.value.appendTo('', newData);

  // 同时添加多个元素，示例代码有效勿删
  // appendMultipleDataTo();
};

const onAbnormalDragSort = (params) => {
  console.log(params);
  // MessagePlugin.warning(params.reason);
  if (params.code === 1001) {
    MessagePlugin.warning('不同层级的元素，不允许调整顺序');
  }
};

const onExpandedTreeNodesChange = (expandedTreeNodes, context) => {
  console.log(expandedTreeNodes, context);
  // 全选不需要处理；仅处理懒加载
  if (!context.rowState) return;
  onTreeExpandChange(context);
};

const onTreeExpandChange = (context) => {
  console.log(context.rowState.expanded ? '展开' : '收起', context);
  /**
   * 如果是懒加载，请确认自己完成了以下几个步骤
   * 1. 提前设置 children 值为 true；
   * 2. 在 onTreeExpandChange 事件中处理异步数据；
   * 3. 自定义展开图标渲染 lazyLoadingTreeIconRender
   */
  if (context.row.list === true) {
    lazyLoadingData.value = context.row;
    const timer = setTimeout(() => {
      appendMultipleDataTo(context.row);
      lazyLoadingData.value = null;
      clearTimeout(timer);
    }, 200);
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

:deep(.t-tabs__nav-item-wrapper) {
  padding: 0px;
  padding-bottom: 10px;
  margin-bottom: 2px;
  background: #fff !important;
}

:deep(.t-tabs__nav-item-wrapper:hover) {
  background: #fff;
}

:deep(.t-tabs__nav-item) {
  margin-left: 12px;
}

:deep(.a .t-tabs__bar) {
  margin-left: 12px;
}

:deep(.b .t-tabs__bar) {
  margin-left: 24px;
}

:deep(.t-radio-group.t-radio-group--filled .t-radio-button.t-is-checked) {
  background: rgba(245, 158, 11, 1);
  color: #fff;
  border-radius: 3px;
}

:deep(.t-radio-group.t-radio-group--filled) {
  background: rgba(250, 250, 250, 1);
}
</style>
