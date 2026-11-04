# SearchForm 通用搜索组件

一个灵活、可配置的搜索表单组件，支持展开/收起功能，适用于各种搜索场景。

## 特性

- ✅ 支持多种字段类型（input、select、date、date-range、textarea）
- ✅ 展开/收起功能
- ✅ v-model 双向绑定
- ✅ 通过配置快速创建表单
- ✅ 支持自定义插槽扩展
- ✅ 响应式布局（一行 4 列）

## 基础使用

```vue
<template>
  <SearchForm
    v-model="searchForm"
    :fields="fields"
    @search="handleSearch"
    @reset="handleReset"
  />
</template>

<script setup>
import { ref } from 'vue';
import SearchForm from '@/components/SearchForm/index.vue';

// 搜索表单数据
const searchForm = ref({
  jobNo: '',
  jobType: '',
  customerName: '',
  carrier: '',
});

// 字段配置
const fields = ref([
  {
    name: 'jobNo',
    label: '作业编号',
    type: 'input',
    placeholder: '请输入作业编号',
  },
  {
    name: 'jobType',
    label: '作业类型',
    type: 'select',
    placeholder: '请选择作业类型',
    options: [
      { label: '入库', value: 1 },
      { label: '出库', value: 2 },
    ],
  },
  {
    name: 'customerName',
    label: '客户名称',
    type: 'input',
    placeholder: '请输入客户名称',
  },
  {
    name: 'carrier',
    label: '承运人',
    type: 'input',
    placeholder: '请输入承运人',
  },
  // 展开时显示的字段
  {
    name: 'granary',
    label: '粮仓名称',
    type: 'select',
    expandable: true,
    options: [],
  },
  {
    name: 'jobStatus',
    label: '流程状态',
    type: 'select',
    expandable: true,
    options: [],
  },
  {
    name: 'grainVariety',
    label: '粮食品种',
    type: 'select',
    expandable: true,
    options: [],
  },
]);

const handleSearch = (formData) => {
  console.log('搜索:', formData);
  // 自己处理搜索逻辑
};

const handleReset = () => {
  console.log('重置');
  // 自己处理重置后的逻辑
};
</script>
```

## Props

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `modelValue` | Object | ✓ | 表单数据，支持 v-model 双向绑定 |
| `fields` | Array | ✓ | 字段配置数组 |
| `showExpand` | Boolean | | 是否显示展开/收起功能（默认为true） |
| `initialValue` | Object | | 初始化表单数据 |

### fields 配置项

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | | 字段名（必需） |
| `label` | String | | 字段标签（必需） |
| `type` | String | 'input' | 字段类型：input、select、date、date-range、textarea |
| `placeholder` | String | | 占位符文字 |
| `options` | Array | | 选项列表（type=select 时必需） |
| `expandable` | Boolean | false | 是否为展开时显示的字段 |
| `defaultValue` | Any | | 初始默认值 |

## Events

| 事件 | 参数 | 说明 |
|------|------|------|
| `search` | formData | 点击查询按钮时触发 |
| `reset` | - | 点击重置按钮时触发 |
| `update:modelValue` | newValue | v-model 更新时触发 |

## Slots

### 自定义字段插槽

为了支持自定义字段类型，可以在未识别的字段类型时，通过 `field-{fieldName}` 插槽来自定义渲染：

```vue
<template>
  <SearchForm v-model="searchForm" :fields="fields">
    <!-- 自定义字段插槽示例 -->
    <template #field-customField="{ field, value, update }">
      <!-- 自定义组件 -->
      <t-input-number
        :model-value="value"
        @update:model-value="update(field.name, $event)"
      />
    </template>
  </SearchForm>
</template>
```

### 自定义按钮插槽

在按钮组后添加自定义按钮：

```vue
<template>
  <SearchForm v-model="searchForm" :fields="fields">
    <template #buttons>
      <t-button @click="handleCustomAction">自定义按钮</t-button>
    </template>
  </SearchForm>
</template>
```

## 完整示例（receiptSearch 改造）

```vue
<template>
  <div class="container">
    <SearchForm
      v-model="formObj"
      :fields="searchFields"
      @search="handleSearch"
      @reset="handleReset"
    />

    <!-- 表格区域 -->
    <div class="bottom forNewClassTableAndsearch">
      <div class="btn-box" style="margin: 5px 0 25px 0">
        <t-button
          theme="primary"
          variant="base"
          @click="handleExport"
          class="search-btn my-icon-btn"
          style="width: 110px"
        >
          <file-export-icon slot="icon" />导出表格
        </t-button>
      </div>

      <t-table
        :loading="loading"
        :data="tableData"
        :columns="tableColumns"
        hover
        resizable
        row-key="id"
        :pagination="pagination"
        @page-change="onPageChange"
      >
        <!-- 表格模板 -->
      </t-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { MessagePlugin } from 'tdesign-vue-next';
import { FileExportIcon } from 'tdesign-icons-vue-next';
import SearchForm from '@/components/SearchForm/index.vue';
import { granaryList, getList } from '@/api/receiptSearch.ts';
import { excelExport } from '@/api/settlementSearch.ts';
import { getDict } from '@/api/sampling.ts';
import router from '@/router';

// 搜索表单数据
const formObj = ref({});

// 搜索字段配置
const jobTypeOptions = ref([
  { label: '入库', value: 1 },
  { label: '出库', value: 2 },
]);
const granaryOptions = ref([]);
const jobStatusOptions = ref([
  { value: 'logIn', label: '待扦样' },
  { value: 'sampled', label: '待质检' },
  { value: 'inspected', label: '待称重' },
  { value: 'weighed', label: '待值仓' },
  { value: 'warehoused', label: '待称皮' },
  { value: 'tareWeighed', label: '待结算' },
  { value: 'pendCard', label: '待销卡' },
  { value: 'logout', label: '已结束' },
]);
const grainVarietyOptions = ref([]);

const searchFields = ref([
  {
    name: 'jobNo',
    label: '作业编号',
    type: 'input',
  },
  {
    name: 'jobType',
    label: '作业类型',
    type: 'select',
    options: jobTypeOptions.value,
  },
  {
    name: 'customerName',
    label: '客户名称',
    type: 'input',
  },
  {
    name: 'carrier',
    label: '承运人',
    type: 'input',
  },
  {
    name: 'granary',
    label: '粮仓名称',
    type: 'select',
    options: granaryOptions.value,
    expandable: true,
  },
  {
    name: 'jobStatus',
    label: '流程状态',
    type: 'select',
    options: jobStatusOptions.value,
    expandable: true,
  },
  {
    name: 'grainVariety',
    label: '粮食品种',
    type: 'select',
    options: grainVarietyOptions.value,
    expandable: true,
  },
]);

// 其他数据省略...

const handleSearch = () => {
  pagination.value.current = 1;
  getTableData();
};

const handleReset = () => {
  getTableData();
};

// 其他方法省略...

onMounted(() => {
  getGranaryOptions();
  getGrainVarietyOptions();
  getTableData();
});
</script>
```

## 字段类型说明

### input
文本输入框

```js
{
  name: 'username',
  label: '用户名',
  type: 'input',
  placeholder: '请输入用户名',
}
```

### select
下拉选择框

```js
{
  name: 'status',
  label: '状态',
  type: 'select',
  options: [
    { label: '启用', value: 1 },
    { label: '禁用', value: 2 },
  ],
}
```

### date
日期选择器

```js
{
  name: 'birthday',
  label: '出生日期',
  type: 'date',
}
```

### date-range
日期范围选择器

```js
{
  name: 'dateRange',
  label: '日期范围',
  type: 'date-range',
}
```

### textarea
文本域

```js
{
  name: 'description',
  label: '描述',
  type: 'textarea',
  placeholder: '请输入描述内容',
}
```

## 样式定制

如需定制样式，可以通过 CSS 变量或直接修改组件样式。

```less
// 修改标签宽度
.search-form-wrapper :deep(.t-form__label) {
  width: 80px !important;
}

// 修改间距
.search-form-wrapper .search-form {
  gap: 20px;
}
```

## 常见问题

**Q: 如何动态更新 options？**

使用响应式 ref，直接修改 fields 中的 options：

```js
const searchFields = ref([...]);

// 动态更新 select 的 options
const updateGranaryOptions = async () => {
  const response = await granaryList({});
  const granaryField = searchFields.value.find(f => f.name === 'granary');
  if (granaryField) {
    granaryField.options = response.result.data.map(item => ({
      label: item.warehouseName,
      value: item.id,
    }));
  }
};
```

**Q: 如何支持多选？**

可以通过自定义插槽实现，或修改组件源码添加对应字段类型。

**Q: 能否禁用某些字段？**

可以添加 `disabled` 字段配置，并在组件中处理：

```js
{
  name: 'status',
  label: '状态',
  type: 'select',
  disabled: true,
  options: [...],
}
```
