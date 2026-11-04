<template>
  <div>
  <common-table :searchList="searchList" tbMt="0px" :searchData="searchData" :columns="columns" :tableData="tableData" :pagination="pagination"  @search="search" @changePage="changePage"  @reset="reset">
  </common-table>
  </div>
</template>
<script setup lang="jsx">
import { listOperationLogRecordByCondition} from '@/api/dict';
import CommonTable from '@/components/common-table/index.vue';
import { computed, onMounted, ref,getCurrentInstance,nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { MessagePlugin } from 'tdesign-vue-next';

const { proxy } = getCurrentInstance();
const router = useRouter();


const pagination = ref({
  defaultPageSize:10,
  total:0,
  defaultCurrent: 1,
});
const searchData = ref({
//  dateRange:[],
  "startTime":'',
  "endTime":'',
  "title": '',
  "businessType":'',
  "operType": '',
  "operName":'',
  "state": '',
  pageNo: pagination.value.defaultCurrent,
  pageSize: pagination.value.defaultPageSize,
 //  userId: 12345,
 //  regionId: 12345,
 //  orgId: 12345,
});
const searchList = ref([
  {
    label:'标题',
    placeholder:'请输入',
    type: "input",
    value: "title",
  },
  // {
  //   label:'时间',
  //   placeholder:'请选择',
  //   type: "dateRange",
  //   value: "dateRange",
  // }
]);

const columns=ref([
  {
    title:'序号',
    fixed: 'left',
    width: 60,
  //  ellipsis: true,
    align: 'left',
    colKey: 'serial-number',
  },
  { title:'标题', colKey: 'title'},
  { title:'业务类型', colKey: 'businessType'},
  { title:'方法名称', colKey: 'method', ellipsis: true,},
  { title:'请求方式', colKey: 'requestMethod'},
  { title:'操作类别', colKey: 'operType'},
  { title:'操作人', colKey: 'operName'},
  { title:'请求URL', colKey: 'operUrl', ellipsis: true,},
  { title:'主机地址', colKey: 'operIp'},
  { title:'请求参数', colKey: 'operParam', ellipsis: true,},
  { title:'返回内容', colKey: 'jsonResult', ellipsis: true,},
  { title:'操作状态', colKey: 'state'},
  { title:'异常信息', colKey: 'errorMsg', ellipsis: true,},
  { title:'操作时间', colKey: 'operTime',ellipsis: true,},
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
  //       {row.organization.name}
  //       </p>
  //     ;
  //   },
  // },
  // { title:'角色组', colKey: 'roleGroup',
  // cell: (h, { row }) => {
  //     // return <span class='rolegp'>
  //     //   {row.listRoleGroup.map(item => (
  //     //     <span key={item.id}>{item.name}</span>
  //     //   ))}
  //     // </span>
  //   let ary=[]
  //   row.listRoleGroup.map(x=>ary.push(x.name))
  //     return <span class='rolegp'>
  //       {ary.join(',')}
  //     </span>
  //   },
  // },
  // {
  //   align: 'left',
  //   fixed: 'right',
  //   width: 200,
  //   colKey: 'op',
  //   title:'操作',
  // },
])

const tableData=ref([])

const search = () => {
  pagination.value.total=0
  pagination.value.defaultCurrent=1
  init();
};
const changePage=(p)=>{
pagination.value.defaultCurrent=p.pagination.current
pagination.value.defaultPageSize=p.pagination.pageSize
init()
}
const reset = () => {
  pagination.value.total=0
  pagination.value.defaultCurrent=1
  searchData.value = {
    // dateRange:[]
  };
  init();
};
const init = () => {
  searchData.value.pageNo=pagination.value.defaultCurrent,
  searchData.value.pageSize=pagination.value.defaultPageSize,
  // searchData.value.startTime=searchData.value?.dateRange?.[0]
  // searchData.value.endTime=searchData.value?.dateRange?.[1]

  listOperationLogRecordByCondition(searchData.value).then(res => {
    if (res.status.code === '200') {
      tableData.value = res.result.data
      pagination.value.total = res.result.total;
    } else {
      MessagePlugin.error(res.status.message);
    }
  });
};

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
