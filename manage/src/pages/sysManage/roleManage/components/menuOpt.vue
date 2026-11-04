<template>
  <div class="list-common-table" style="margin-top: -20px;">
    <t-radio-group v-model="theme" variant="default-filled" style="margin-bottom:15px;" @change="tabChange">
      <t-radio-button value="1">电脑端</t-radio-button>
      <t-radio-button value="2">移动端</t-radio-button>
    </t-radio-group>
    <t-enhanced-table ref="tableRef" v-model:expandedTreeNodes="expandedTreeNodes" row-key="id"
      :selected-row-keys="selectedRowKeys" drag-sort="row-handler" :data="tableData" :columns="columns"
      :tree="treeConfig" :tree-expand-and-fold-icon="treeExpandIcon" :before-drag-sort="beforeDragSort"
      @expanded-tree-nodes-change="onExpandedTreeNodesChange">
      <template #ckname="slotProps">
        <t-space>
          <div class="tdesign-table-demo__table-operations">
            <t-checkbox v-model="slotProps.row.ckStu" @change="changeCkStu(slotProps.row)">{{ slotProps.row.name
              }}</t-checkbox>
          </div>
        </t-space>
      </template>



      <template #op="slotProps">
        <t-space>
          <div class="tdesign-table-demo__table-operations">
            <t-checkbox-group v-model="slotProps.row.btnList" :disabled="!slotProps.row.ckStu">
              <t-checkbox v-for="(v, k) in slotProps.row.listRights" :key="k" :value="v.id">{{ v.name }}</t-checkbox>
            </t-checkbox-group>
          </div>
        </t-space>
      </template>
    </t-enhanced-table>
  </div>
</template>
<script setup lang="jsx">
import { ref, reactive, computed, onMounted, getCurrentInstance, defineProps } from 'vue';
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


const changeCkStu = (d) => {
  if (!d.ckStu) d.btnList = []
}
const emit = defineEmits(['getmenu']);
const pcList = ref([])
const phList = ref([])
const tabChange = () => {
  if (theme.value == 1) {
    phList.value = JSON.parse(JSON.stringify(tableData.value))
    tableData.value = pcList.value
  }
  if (theme.value == 2) {
    pcList.value = JSON.parse(JSON.stringify(tableData.value))
    tableData.value = phList.value
  }

}

const userStore = useUserStore();
const { proxy } = getCurrentInstance();
const visible = ref(false);

const tableData = ref([]);
const theme = ref('1')
const treeConfig = reactive({
  childrenKey: 'children',
  treeNodeColumnIndex: 0,
  indent: 55,
  // expandTreeNodeOnClick: true,
});

const selectedRowKeys = ref([]);

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
  //   colKey: 'row-select',
  //   type: 'multiple',
  //   // 禁用行选中方式一：使用 disabled 禁用行（示例代码有效，勿删）。disabled 参数：{row: RowData; rowIndex: number })
  //   // 这种方式禁用行选中，当前行会添加行类名 t-table__row--disabled，禁用行文字变灰
  //   // disabled: ({ rowIndex }) => rowIndex === 1 || rowIndex === 3,

  //   // 禁用行选中方式二：使用 checkProps 禁用行（示例代码有效，勿删）
  //   // 这种方式禁用行选中，行文本不会变灰
  // //  checkProps: ({ row }) => ({ disabled: !row.childrenList && row.status !== 0 }),
  //   // 自由调整宽度，如果发现元素看不见，请加大宽度
  //   width: 23,
  // },
  // {
  //   // 列拖拽排序必要参数
  //   colKey: 'drag',
  //   title: '排序',
  //   // eslint-disable-next-line @typescript-eslint/no-unused-vars
  //   cell: (h) => <MoveIcon />,
  //   width: 46,
  // },
  {
    width: 160,
    colKey: 'ckname',
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
    width: 300,
    colKey: 'op',
    title: '权限',
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
const init = (list) => {



  listMenu(searchData.value).then(res => {
    if (res.status.code === '200') {
      let dataPc = []
      let dataPh = []
      res.result.map((x) => {
        if (x.type == 1) dataPc.push(x)
        if (x.type == 2) dataPh.push(x)
      })
      const st = (d) => {
        d.sort((a, b) => a.orderNum - b.orderNum);

        for (let i = 0; i < d.length; i++) {
          let n = []
          n = list.filter(r => r.id == d[i].id)
          console.log(n.length)
          d[i].btnList = []
          if (n.length == 1) {
            d[i].ckStu = true
            n[0].listRights.map(k => d[i].btnList.push(k.id * 1))
          }
          if (d[i].children) st(d[i].children)

        }
      }
      st(dataPc)
      st(dataPh)
      pcList.value = JSON.parse(JSON.stringify(dataPc))
      phList.value = JSON.parse(JSON.stringify(dataPh))
      tableData.value = pcList.value
      console.log(tableData.value)
      setTimeout(() => {
        tableRef.value.expandAll()
      }, 100)
    } else {
      MessagePlugin.error(res.status.message);
    }
  });
};

const sumt = async () => {
  let p = {
    listMenu: []
  }
  const st = (d) => {
    for (let i = 0; i < d.length; i++) {
      if (d[i].ckStu) {
        let ary = []
        d[i].btnList.map(x => {
          ary.push({ id: x })
        })
        p.listMenu.push({ id: d[i].id, listRights: ary })
      }
      if (d[i].children) st(d[i].children)

    }
  }
  st(pcList.value)
  st(phList.value)
  console.log(p)
  emit('getmenu', p)
}

onMounted(() => {


});

defineExpose({
  sumt,
  init
})




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

const expandAll = ref(false);

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




</script>

<style scoped>
.list-common-table {
  background-color: var(--td-bg-color-container);
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

:deep(.t-table__pagination-wrap) {
  display: none;
}

:deep(.t-table__tree-op-icon) {
  vertical-align: 1px
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
