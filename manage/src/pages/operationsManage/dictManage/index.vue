<template>
  <div>
    <common-table
      :searchList="searchList"
      :searchData="searchData"
      :columns="columns"
      :tableData="tableData"
      :pagination="pagination"
      @search="search"
      @changePage="changePage"
      @reset="reset"
    >
      <!-- 搜索侧按钮插槽 -->
      <template #operation>
        <t-button
          variant="base"
          theme="warning"
          @click="add"
        >
          <template #icon><t-icon
              name="add"
              style="margin-right: 3px;"
            /></template>
          新增</t-button>
        <t-button
          variant="outline"
          theme="default"
          @click="down"
          class="flt"
        >
          <template #icon><t-icon
              name="download"
              style="margin-right: 3px;"
            /></template>
          下载模板</t-button>
        <t-button
          variant="outline"
          theme="default"
          @click="impf"
          class="flt"
        >
          <template #icon><t-icon
              name="upload"
              style="margin-right: 3px;"
            /></template>
          导入</t-button>
        <t-button
          variant="outline"
          theme="default"
          @click="exp"
          class="flt"
        >
          <template #icon><t-icon
              name="download"
              style="margin-right: 3px;"
            /></template>
          导出</t-button>
        <input
          type="file"
          accept=".xlsx"
          id="fileInput"
          style="display:none;"
        >
      </template>
      <!-- 列表操作侧按钮插槽 -->
      <template #btnAry="slotProps">
        <!-- {{slotProps.row.row.index}} -->
        <t-link
          theme="primary"
          @click="handleClickDetail(slotProps.row.row, 2)"
        >编辑</t-link>
        <t-link
          theme="success"
          @click="handleClickDetail(slotProps.row.row, 3)"
        >数据管理</t-link>

        <!-- <t-popconfirm  theme="danger"  @confirm="onConfirmDelete(slotProps.row)">
        <template #content>
        <p class="title">确认删除该类型？</p>
        <p class="describe">类型一旦删除，对应字典将无法<br />查询，请谨慎操作。</p>
        </template>
<t-link theme="danger"> 删除</t-link>
</t-popconfirm> -->
        <t-link
          theme="danger"
          @click="handleClickDelete(slotProps.row)"
        > 删除</t-link>
      </template>
    </common-table>
    <!-- 新增编辑弹窗 -->
    <t-dialog
      v-model:visible="visible"
      :header="pagestu == 1 ? '创建字典' : '编辑字典'"
      width="860px"
      class="newDialog newclassNewDialog"
      :cancelBtn="{ theme: 'default', variant: 'outline' }"
      :confirmBtn="{ theme: 'warning', variant: 'base' }"
      :confirm-on-enter="true"
      :on-confirm="() => { proxy.$refs.addf.submt() }"
    >
      <t-space
        direction="vertical"
        style="width: 100%"
      >
        <addcom
          ref="addf"
          :roleGroupData="roleGroupData"
          :pagestu="pagestu"
          @sumt="sumt"
        ></addcom>
      </t-space>
    </t-dialog>
    <!-- 确认删除弹窗 -->
    <t-dialog
      v-model:visible="confirmVisible"
      header="删除确认"
      class="newDialog newclassNewDialog"
      :cancelBtn="{ theme: 'default', variant: 'outline' }"
      :confirmBtn="{ theme: 'warning', variant: 'base' }"
      @confirm="onConfirmDelete"
    >
      <t-space
        direction="vertical"
        style="width: 100%"
      >
        <div class="deletCon">
          <div class="delectConHeader"><t-icon
              name="error-circle-filled"
              size="18px"
              color="rgba(255, 41, 75, 1)"
            />
            <span class="a">确认删除该类型</span>
          </div>
          <div class="b">类型一旦删除，对应字典将无法查询，请谨慎操作。</div>
        </div>
      </t-space>
    </t-dialog>

    <t-dialog
      v-model:visible="impVisible"
      header="字典类型导入"
      style="border-radius: 5px;"
      class="newDialog newclassNewDialog"
      :cancelBtn="{ theme: 'default', variant: 'outline' }"
      :confirmBtn="{ theme: 'warning', variant: 'base' }"
      @confirm="imp"
    >
      <t-space
        direction="vertical"
        style="width: 100%;padding-top: 30px;"
      >
        <div style="width: 336px; margin: 0 auto;position: relative;">
          <img
            v-if="file.length == 0"
            style="position: absolute;
    width: 122px;
    left: 107px;
    top: 23px;"
            src="@/assets/upicon.png"
          >
          <t-upload
            v-model="file"
            :auto-upload="true"
            :abridge-name="[10, 8]"
            :beforeUpload="upchange"
            accept=".xlsx"
            draggable
          >
          </t-upload>
          <div style="color: #394b5a;font-size: 13px;margin-top: 10px;text-align: center;">仅允许导入xlsx文件
            <span
              style="color: #247dff;"
              @click="down"
            >下载模板</span>
          </div>
        </div>

      </t-space>
    </t-dialog>
  </div>
</template>
<script setup lang="jsx">
import { listDict, saveDict, deleteDict, updateDict, getSystemConfigByGroupAndKey, typeExportExcel, typeImportExcel } from '@/api/dict';
import CommonTable from '@/components/common-table/index.vue';
import { computed, onMounted, ref, getCurrentInstance } from 'vue';
import { t } from '@/locales';
import addcom from './components/add.vue';
import { useRouter } from 'vue-router';
import { MessagePlugin } from 'tdesign-vue-next';

const { proxy } = getCurrentInstance();
const router = useRouter();
const file = ref([])


const upchange = (e) => {
  // 获取当前时间
  let now = new Date();
  // 格式化日期和时间
  let formattedDate = now.getFullYear() + '-' +
    ('0' + (now.getMonth() + 1)).slice(-2) + '-' +
    ('0' + now.getDate()).slice(-2) + ' ' +
    ('0' + now.getHours()).slice(-2) + ':' +
    ('0' + now.getMinutes()).slice(-2) + ':' +
    ('0' + now.getSeconds()).slice(-2);
  file.value = [{
    name: e.name,
    url: '123.png',
    status: 'success',
    size: e.size,
    uploadTime: formattedDate,
    data: e
  }]
  return false
}

const down = async () => {
  let r = await getSystemConfigByGroupAndKey({
    "sycGroup": 'dictTypeExcelPath',
    "sycKey": 'dictTypeExcelPath',
  })
  if (r.status.code == 200) {
    let url = await proxy.$getPic(r.result.sycValue)
    console.log(url)
    const a = document.createElement('a');
    a.href = url
    a.download = '模板.png'; // 设置下载文件名
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  } else {
    MessagePlugin.error('下载失败!');
  }

}
const impf = () => {
  impVisible.value = true
  file.value = []
}
const impVisible = ref(false)

const imp = () => {
  if (file.value.length == 0) {
    MessagePlugin.error('请上传文件');
    return
  }
  // document.getElementById('fileInput').addEventListener('change', function(event) {
  //   // 处理文件选择
  //   var file = event.target.files[0]; // 获取选中的第一个文件
  //   if (file) {
  console.log('选中的文件:', file.value[0].data.raw); // 输出文件名作为示例
  // 在这里你可以进行其他操作，比如读取文件内容等
  const formData = new FormData();
  formData.append('file', file.value[0].data.raw);
  typeImportExcel(formData).then(r => {
    if (r.status.code == 200) {
      MessagePlugin.success(r.status.message);
      init()
    } else {
      MessagePlugin.error(r.status.message);
    }
  })
  //     }
  //   });
  //   document.getElementById('fileInput').click()
}

const exp = async () => {
  let res = await typeExportExcel({
    name: searchData.value.name,
    "type": '',
    "showState": ''
    // pageNo:1,
    // pageSize:1000,
  })
  let blob = new Blob([res]);
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "字典类型.xlsx"; // 下载文件名
  link.click();
}


const pagination = ref({
  defaultPageSize: 10,
  total: 0,
  defaultCurrent: 1,
});
const searchData = ref({
  name: '',
  // type: '',
  showState: '',
  pageNo: pagination.value.defaultCurrent,
  pageSize: pagination.value.defaultPageSize,
  //  userId: 12345,
  //  regionId: 12345,
  //  orgId: 12345,
});
const opData = ref([{ label: '启用', value: '1' }, { label: '禁用', value: '2' }])
const searchList = ref([
  {
    label: '名称',
    placeholder: '请输入类型名称',
    type: "input",
    value: "name",
  },
  {
    label: '状态',
    placeholder: '请选择',
    type: "select",
    value: "showState",
    data: opData.value
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
  { title: '名称', colKey: 'name' },
  { title: '类型标识', colKey: 'type' },




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
  searchData.value = {};
  init();
};
const init = () => {
  searchData.value.pageNo = pagination.value.defaultCurrent,
    searchData.value.pageSize = pagination.value.defaultPageSize,
    listDict(searchData.value).then(res => {
      if (res.status.code === '200') {
        tableData.value = res.result.data
        pagination.value.total = res.result.total;
      } else {
        MessagePlugin.error(res.status.message);
      }
    });
};

const sumt = async (form) => {
  let res = pagestu.value == '1' ? await saveDict(form) : await updateDict(form)
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
  deletId.value.type = data.row.type
  confirmVisible.value = true;
};
const onConfirmDelete = async () => {
  confirmVisible.value = false;
  let res = await deleteDict(deletId.value.type)
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
  if (n == 2) {
    pagestu.value = n
    let formData = data
    visible.value = true
    proxy.$refs.addf.initData(formData)
  }
  if (n == 3) {
    router.push({
      name: 'dictData', query: {
        type: data.type
      }
    })
  }

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

:deep(.t-upload__dragger) {
  border-radius: 5px;
  border: 1px dashed #acb3b2;
}

:deep(.t-dialog) {
  border-radius: 5px;
}

:deep(.t-upload__trigger) {
  color: #353f47;
  margin-top: 80px;
}

:deep(.t-upload__trigger span) {
  color: #353f47;
}
</style>
