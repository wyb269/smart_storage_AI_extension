<template>
  <div>
    <div class="list-common-table forNewClassTableAndsearch" v-if="justList != 1">
      <t-form :data="formData" :label-width="80" colon @reset="onReset" @submit="onSubmit">
        <t-row :gutter="[24, 20]">
          <t-col :span="3" v-for="(item, i) in searchList" :key="i">

            <template v-if="item.type == 'input'">
              <t-form-item :label="item.label" labelAlign="right" :name="item.value">
                <t-input v-model="searchData[item.value]" :placeholder="item.placeholder" class="form-item-content"
                  type="search" />
              </t-form-item>
            </template>

            <template v-if="item.type == 'rgSelect'">
              <t-form-item :label="item.label" labelAlign="right" :name="item.value">
                <regionSelectForSearch v-if="rgfresh" v-model="searchData[item.value]"></regionSelectForSearch>
              </t-form-item>
            </template>

            <template v-if="item.type == 'orgSelect'">
              <t-form-item :label="item.label" labelAlign="right" :name="item.value">
                <orgSelectForSearch v-if="rgfresh" v-model="searchData[item.value]"></orgSelectForSearch>
              </t-form-item>
            </template>

            <template v-if="item.type == 'select'">
              <t-form-item :label="item.label" labelAlign="right" :name="item.value">
                <t-select v-model="searchData[item.value]" :style="{ minWidth: '134px' }" class="form-item-content"
                  :placeholder="item.placeholder">
                  <t-option v-for="(k, j) in item.data" :value="k.value" :label="k.label"></t-option>
                </t-select>
              </t-form-item>
            </template>

            <template v-if="item.type == 'date'">
              <t-form-item :label="item.label" labelAlign="right" :name="item.value">
                <t-date-picker v-model="searchData[item.value]" :placeholder="item.placeholder"
                  class="form-item-content" clearable allow-input />
              </t-form-item>
            </template>

            <template v-if="item.type == 'dateRange'">
              <t-form-item :label="item.label" labelAlign="right" :name="item.value">

                <t-date-range-picker v-model="searchData[item.value]" :placeholder="item.placeholder"
                  class="form-item-content" allow-input clearable />
              </t-form-item>
            </template>
          </t-col>

          <t-col :span="3" class="operation-container" style="margin-left: auto;">
            <t-button theme="warning" variant="base" type="submit" class="chaxun"
              :style="{ marginLeft: 'var(--td-comp-margin-s)' }">
              <template #icon><t-icon name="search" style="margin-right: 7px;" /></template>
              查询
            </t-button>
            <t-button type="reset" variant="outline" theme="warning" class="chongzhi">
              <template #icon><t-icon name="refresh" style="margin-right: 7px;" /></template>
              重置 </t-button>
          </t-col>

          <!-- <t-col :span="9">
          <t-row :gutter="[24, 24]">

            <template v-for="(item,i) in searchList" :key="i">
              <t-col :span="4" v-if="item.type == 'input'" >
                <t-form-item :label="item.label" labelAlign="right" :name="item.value">
                  <t-input
                    v-model="searchData[item.value]"
                    :placeholder="item.placeholder"
                    class="form-item-content"
                    type="search"
                  />
                </t-form-item>
              </t-col>

              <t-col :span="4" v-if="item.type == 'rgSelect'" >
                <t-form-item :label="item.label" labelAlign="right" :name="item.value">
                  <regionSelectForSearch v-if="rgfresh" v-model="searchData[item.value]" ></regionSelectForSearch>
                </t-form-item>
              </t-col>

              <t-col :span="4" v-if="item.type == 'orgSelect'" >
                <t-form-item :label="item.label" labelAlign="right" :name="item.value">
                  <orgSelectForSearch v-if="rgfresh" v-model="searchData[item.value]" ></orgSelectForSearch>
                </t-form-item>
              </t-col>

              <t-col :span="4" v-if="item.type == 'select'" >
                <t-form-item :label="item.label" labelAlign="right" :name="item.value">
                  <t-select
                    v-model="searchData[item.value]"
                    :style="{ minWidth: '134px' }"
                    class="form-item-content"
                    :placeholder="item.placeholder"
                    >
                    <t-option v-for="(k,j) in item.data" :value="k.value" :label="k.label"></t-option>
                  </t-select>
                </t-form-item>
              </t-col>

              <t-col :span="4" v-if="item.type == 'date'" >
                <t-form-item :label="item.label" labelAlign="right" :name="item.value">
                    <t-date-picker
                    v-model="searchData[item.value]"
                    :placeholder="item.placeholder"
                    class="form-item-content"
            
                    clearable
                    allow-input/>
                </t-form-item>
              </t-col>

              <t-col :span="4" v-if="item.type == 'dateRange'" >
                <t-form-item :label="item.label" labelAlign="right" :name="item.value">

                  <t-date-range-picker
                   v-model="searchData[item.value]"
                    :placeholder="item.placeholder"
                    class="form-item-content"
               
                  allow-input clearable />
                </t-form-item>
              </t-col>


            </template>
          </t-row>
        </t-col>

        <t-col :span="3" class="operation-container">
          <t-button theme="default" type="submit" class="chaxun" :style="{ marginLeft: 'var(--td-comp-margin-s)' }">
            <template #icon><t-icon name="search" style="margin-right: 7px;" /></template>
            查询
          </t-button>
          <t-button type="reset" variant="base" theme="default" class="chongzhi">
            <template #icon><t-icon name="refresh" style="margin-right: 7px;" /></template>
            重置 </t-button>
        </t-col> -->
        </t-row>
      </t-form>
    </div>
    <div class="list-common-table forNewClassTableAndsearch" :style="justList != 1 ? 'margin-top:22px' : 'padding:0px'">

      <t-row v-if="justList != 1" justify="space-between">
        <div class="left-operation-container  tdop">
          <slot name="operation"></slot>
          <!-- <t-button>新增</t-button>
          <t-button variant="base" theme="default" disabled>导出</t-button> -->
          <!-- <p v-if="!!selectedRowKeys.length" class="selected-count">
            {{ t('pages.listBase.select') }} {{ selectedRowKeys.length }} {{ t('pages.listBase.items') }}
          </p> -->
        </div>

      </t-row>


      <div class="table-container comonTable" :style="{ 'marginTop': tbMt }">
        <!-- bordered -->
        <t-table v-if="fresh" :data="tableData" :columns="columns" :row-key="rowKey" :vertical-align="verticalAlign"
          :hover="hover" :pagination="pagination" :loading="dataLoading" :header-affixed-top="headerAffixedTop"
          @page-change="rehandlePageChange" @change="rehandleChange" resizable>
          <template #op="slotProps">
            <t-space>
              <slot name="btnAry" :row="slotProps"></slot>
            </t-space>
          </template>
          <template #empty="slotProps">
            <t-space>
              <div>
                <img style="width: 120px;" src="@/assets/imgs/empty.png">
                <div style="font-family: PingFang SC;
font-weight: 300;
font-style: Regular;
font-size: 13px;
leading-trim: NONE;
line-height: 100%;
letter-spacing: 0px;
color:rgba(149, 149, 149, 1);
text-align: center;
">暂无数据</div>
              </div>

            </t-space>
          </template>

        </t-table>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import type { PageInfo, PrimaryTableCol, TableRowData } from 'tdesign-vue-next';
import { MessagePlugin } from 'tdesign-vue-next';
import { computed, onMounted, ref, getCurrentInstance, watch, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import regionSelectForSearch from '@/components/regionSelectForSearch.vue';
import orgSelectForSearch from '@/components/orgSelectForSearch.vue';

import { getList } from '@/api/list';
import Trend from '@/components/trend/index.vue';
import { prefix } from '@/config/global';
import { CONTRACT_PAYMENT_TYPES, CONTRACT_STATUS, CONTRACT_TYPES } from '@/constants';
import { t } from '@/locales';
import { useSettingStore } from '@/store';
import { template } from 'lodash';

const store = useSettingStore();
const router = useRouter();
const { proxy } = getCurrentInstance();



const { searchList, searchData, pagination } = defineProps({
  searchList: {
    type: Array,
    default: []
  },
  searchData: {
    type: Object,
    default: {}
  },
  columns: {
    type: Array,
    default: []
  },
  tableData: {
    type: Array,
    default: []
  },
  pagination: {
    type: Object,
    default: {}
  },
  tbMt: {
    type: String,
    default: '20px'
  },
  justList: {
    type: String,
    default: '0'
  }



  // visible: {
  //   type: Boolean,
  //   default: false,
  // },
  // data: {
  //   type: Object as PropType<FormData>,
  //   default: undefined,
  // },
});

const fresh = ref(true)
const rgfresh = ref(true)
const orgfresh = ref(true)
watch(
  () => pagination.defaultCurrent,
  () => {
    if (pagination.defaultCurrent == 1) {
      fresh.value = false
      nextTick(() => {
        fresh.value = true
      });
    }
  },
);
watch(
  () => searchData.regionId,
  () => {
    console.log(searchData.regionId)
    if (!searchData.regionId) {
      rgfresh.value = false
      nextTick(() => {
        rgfresh.value = true
      });
    }
  },
);
watch(
  () => searchData.orgId,
  () => {
    console.log(searchData.orgId)
    if (!searchData.orgId) {
      orgfresh.value = false
      nextTick(() => {
        orgfresh.value = true
      });
    }
  },
);

const onReset = (val: unknown) => {
  proxy.$emit("reset");
};
const onSubmit = (val: unknown) => {
  console.log("proxy.searchData", proxy.searchData);
  proxy.$emit("search", proxy.searchData);
};










interface FormData {
  name: string;
  no: string;
  status?: number;
  type: string;
}



const CONTRACT_STATUS_OPTIONS = [
  { value: CONTRACT_STATUS.FAIL, label: t('components.commonTable.contractStatusEnum.fail') },
  { value: CONTRACT_STATUS.AUDIT_PENDING, label: t('components.commonTable.contractStatusEnum.audit') },
  { value: CONTRACT_STATUS.EXEC_PENDING, label: t('components.commonTable.contractStatusEnum.pending') },
  { value: CONTRACT_STATUS.EXECUTING, label: t('components.commonTable.contractStatusEnum.executing') },
  { value: CONTRACT_STATUS.FINISH, label: t('components.commonTable.contractStatusEnum.finish') },
];

const CONTRACT_TYPE_OPTIONS = [
  { value: CONTRACT_TYPES.MAIN, label: t('components.commonTable.contractTypeEnum.main') },
  { value: CONTRACT_TYPES.SUB, label: t('components.commonTable.contractTypeEnum.sub') },
  { value: CONTRACT_TYPES.SUPPLEMENT, label: t('components.commonTable.contractTypeEnum.supplement') },
];

const searchForm = {
  name: '',
  no: '',
  type: '',
};

const formData = ref<FormData>({ ...searchForm });
const rowKey = 'index';
const verticalAlign = 'top' as const;
const hover = true;




const data = ref([]);

const dataLoading = ref(false);
// const fetchData = async () => {
//   dataLoading.value = true;
//   try {
//     const { list } = await getList();
//     data.value = list;
//     pagination.value = {
//       ...pagination.value,
//       total: list.length,
//     };
//   } catch (e) {
//     console.log(e);
//   } finally {
//     dataLoading.value = false;
//   }
// };
onMounted(() => {

});



const rehandlePageChange = (pageInfo: PageInfo, newDataSource: TableRowData[]) => {
  console.log('分页变化', pageInfo, newDataSource);
};
const rehandleChange = (changeParams: unknown, triggerAndData: unknown) => {
  console.log('统一Change', changeParams, triggerAndData);
  proxy.$emit('changePage', changeParams)
};

const headerAffixedTop = computed(
  () =>
    ({
      offsetTop: store.isUseTabsRouter ? 48 : 0,
      container: `.${prefix}-layout`,
    }) as any, // TO BE FIXED
);
</script>
<style lang="less" scoped>
.list-common-table {
  background-color: var(--td-bg-color-container);
  padding: var(--td-comp-paddingTB-xxl) var(--td-comp-paddingLR-xxl);
  border-radius: var(--td-radius-medium);
  border-radius: 3px;



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

:deep(.t-form__label) {
  color: rgba(102, 102, 102, 1);
  margin-right: -15px;
  font-size: 13px;
}

:deep(.t-form__controls) {
  margin-left: 0px !important;
}





// :deep(.comonTable .t-table th) {
//   background: #f9fafc;
//   border-bottom: 1px solid #e8ebf2;
//   border-top: 1px solid #e8ebf2;
//   //  padding: 12px 12px 12px 14px;
//   color: #000;
// }

// :deep(.comonTable .t-table td) {
//   border: none;
//   //  padding: 15px;
//   //  border-bottom: 1px solid rgba(232, 233, 237, 1);
//   //  font-size: 13px;
// }

// :deep(.comonTable .t-table .t-table__body tr:last-child td) {
//   border-bottom: none;
// }

// :deep(.comonTable .t-table .t-table__body tr:nth-child(2n) ){
//     background: #fafafa;
// }

// :deep(.comonTable .t-table th>div) {
//   color: rgba(26, 26, 26, 1);

//   font-family: PingFang SC;
//   font-weight: 400;
//   font-style: Regular;
//   font-size: 13px;
//   leading-trim: NONE;
//   line-height: 100%;
//   letter-spacing: 0px;
// }

// :deep(.comonTable .t-table--bordered th) {
//   border-left: 1px solid #e8eaec;
// }

// :deep(.comonTable .t-table--bordered th:nth-child(1)) {
//   border-left: none;
// }

// :deep(.comonTable .t-table--bordered td) {
//   border-left: 1px solid #e8eaec;
// }

// :deep(.comonTable .t-table--bordered td:nth-child(1)) {
//   border-left: none;
// }

// :deep(.comonTable .t-table--bordered .t-table__content) {
//   border: 1px solid #e8eaec;
// }


// :deep(.comonTable .t-table--bordered .t-table__affixed-header-elm) {
//   border: 1px solid #e8eaec;
//   border-bottom: 0;
//   border-right: 0;
//   overflow: hidden;
// }

// :deep(.comonTable tbody tr:not(.t-table__empty-row):nth-of-type(even)) {
//   background-color: #f9fafc
// }

// :deep(.comonTable .t-table__empty) {
//   background: #fff;
// }

// :deep(.comonTable .t-table__empty-row) {
//   background-color: #fff !important;
// }






.chaxun {
  padding-left: 12px;
  padding-right: 12px;
  font-family: PingFang SC;
  font-weight: 500;
  font-style: Medium;
  font-size: 14px;
  leading-trim: NONE;
  line-height: 100%;
  letter-spacing: 0px;
  background: rgba(245, 158, 11, 1);
  border-color: rgba(245, 158, 11, 1);
  padding-bottom: 2px;
}

.chongzhi {

  padding-left: 12px;
  padding-right: 12px;
  color: rgba(245, 158, 11, 1);
  border: 1px solid rgba(245, 158, 11, 1);
  font-family: PingFang SC;
  font-weight: 500;
  font-style: Medium;
  font-size: 14px;
  leading-trim: NONE;
  line-height: 100%;
  letter-spacing: 0px;
}
</style>
