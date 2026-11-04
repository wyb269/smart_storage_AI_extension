<template>
  <div style="background:#fff;margin-top: -25px;margin-left: -25px;margin-right: -25px;padding: 15px;">
    <t-row :gutter="[25, 25]">
      <t-col :span="8">
        <div class="weighLeft bx">
          <div class="hd">
            <div class="tt">检斤称重(出库)</div>
            <span class="lc">
              <t-tag theme="warning" variant="light-outline" class="tg">当前流程 : 待称重</t-tag>
            </span>
          </div>
          <div style="margin-top: 15px;">
            <readCard @result="result" />
          </div>
          <t-form :label-width="70" labelAlign="top">
            <div class="weighCon" style="width: 100%;">

              <div class="weighInp" style="width: 100%;">

                <t-row :gutter="[25, 5]" style="width: 90%;">
                  <t-col :span="6">
                    <t-form-item label="智能卡编号" class="inp">
                      <t-input placeholder="请刷磁卡" disabled />
                    </t-form-item>
                  </t-col>
                  <t-col :span="6">
                    <t-form-item label="承运人" class="inp">
                      <t-input placeholder="请刷磁卡" disabled />
                    </t-form-item>
                  </t-col>
                  <t-col :span="6">
                    <t-form-item label="车牌号" class="inp">
                      <t-input placeholder="请刷磁卡" disabled />
                    </t-form-item>
                  </t-col>
                  <t-col :span="6">

                    <t-form-item label="车牌号" class="inp">
                      <t-input placeholder="请刷磁卡" disabled />
                    </t-form-item>
                  </t-col>

                </t-row>

              </div>

            </div>
          </t-form>
          <div style="margin-top: 20px;">
            <t-form :label-width="70" labelAlign="top">
              <t-row :gutter="[15, 10]">
                <t-col :span="4">
                  <t-form-item label="作业编号">
                    <t-input placeholder="请刷磁卡" />
                  </t-form-item>
                </t-col>
                <t-col :span="4">
                  <t-form-item label="作业编号">
                    <t-input placeholder="请刷磁卡" />
                  </t-form-item>
                </t-col>
                <t-col :span="4">
                  <t-form-item label="作业编号">
                    <t-input placeholder="请刷磁卡" />
                  </t-form-item>
                </t-col>
                <t-col :span="4">
                  <t-form-item label="作业编号">
                    <t-input placeholder="请刷磁卡" />
                  </t-form-item>
                </t-col>
                <t-col :span="4">
                  <t-form-item label="作业编号">
                    <t-input placeholder="请刷磁卡" />
                  </t-form-item>
                </t-col>
                <t-col :span="4">
                  <t-form-item label="作业编号">
                    <t-input placeholder="请刷磁卡" />
                  </t-form-item>
                </t-col>

                <t-col :span="12">
                  <t-form-item label="备注">
                    <t-textarea :height="124" placeholder="请输入" />
                  </t-form-item>
                </t-col>

                <t-col :span="12" style="margin-top: 20px;padding-bottom: 20px;">
                  <div style="text-align: center;">
                    <t-button variant="base" theme="success" class="czbt">
                      <template #icon><t-icon name="undertake-delivery-filled" style="margin-right: 3px;" /></template>
                      确认称重</t-button>
                  </div>
                </t-col>
              </t-row>
            </t-form>
          </div>
        </div>
      </t-col>
      <t-col :span="4">
        <div class="weighPh bx">
          <div class="phhd">
            <t-icon name="camera-1" size="18px" style="margin-right: 3px;" />
            <span>视频监控区域</span>
          </div>
          <div class="bx"
            style="padding: 5px;text-align: center;color:rgb(227 115 24);font-size: 12px;margin-top: 10px;">
            <!-- <t-icon name="error-triangle-filled" size="15px" /> -->
            <!-- <t-icon name="error-circle-filled" size="15px" />  -->
            称重图片使用【文件上传】功能进行上传时，需要分别上传车辆前后两个角度的照片!
          </div>
          <div class="phcon">
            <div class="phCld">
              <div class="upwrap">
                <div style="margin-bottom: 5px;">车辆前</div>
                <div class="uppic" style="background: #f0f7f3;
    border: 1px dashed #98ddb2;">
                </div>
                <div class="upControl">
                  <t-button variant="base" theme="success" class="upbt">
                    <template #icon><t-icon name="camera" style="margin-right: 3px;" /></template>
                    拍照</t-button>

                  <t-button variant="base" theme="success" class="upbt">
                    <template #icon><t-icon name="upload" style="margin-right: 3px;" /></template>
                    上传</t-button>
                  <t-button variant="base" theme="danger" class="upbt">
                    <template #icon><t-icon name="delete" style="margin-right: 3px;" /></template>
                    删除</t-button>
                </div>

              </div>
            </div>
            <div class="phCld">
              <div class="upwrap">
                <div style="margin-bottom: 5px;">车辆后</div>
                <div class="uppic" style="background: #fbeee6;
    border: 1px dashed #f1aa7f">

                </div>
                <div class="upControl">
                  <t-button variant="base" theme="warning" class="upbt">
                    <template #icon><t-icon name="camera" style="margin-right: 3px;" /></template>
                    拍照</t-button>

                  <t-button variant="base" theme="warning" class="upbt">
                    <template #icon><t-icon name="upload" style="margin-right: 3px;" /></template>
                    上传</t-button>
                  <t-button variant="base" theme="danger" class="upbt">
                    <template #icon><t-icon name="delete" style="margin-right: 3px;" /></template>
                    删除</t-button>
                </div>

              </div>
            </div>
          </div>

        </div>
      </t-col>
    </t-row>

    <div class="weighTb">
      <div class="weighTbhd">
        <t-icon name="time" />
        今日检斤记录
      </div>

      <div class="hislist">
        <common-table justList="1" tbMt="0" :columns="columns" :tableData="tableData" :pagination="pagination"
          @changePage="changePage">

          <!-- 列表操作侧按钮插槽 -->
          <!-- <template #btnAry="slotProps">

      <t-link theme="success" @click="handleClickDetail(slotProps.row.row)">编辑</t-link>

    </template> -->
        </common-table>
      </div>

    </div>
  </div>
</template>
<script setup lang="jsx">
import readCard from '@/components/readCard/index.vue';
import uploadCom from './components/uploadCom.vue';
import CommonTable from '@/components/common-table/index.vue';
import { ref, defineComponent, getCurrentInstance } from 'vue';
const result = r => {
  console.log(r)
  //  r.result  0失败   1成功
}

const changePage = (p) => {
  pagination.value.defaultCurrent = p.pagination.current
  pagination.value.defaultPageSize = p.pagination.pageSize
  // init()
}
const pagination = ref({
  defaultPageSize: 10,
  total: 0,
  defaultCurrent: 1,
});
const tableData = ref([])
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
  {
    title: '手机号', colKey: 'phone',
    cell: (h, { row }) => {
      return <p>
        {row.phone ? row.phone : '-'}
      </p>
        ;
    },
  },
  {
    title: '行政区', colKey: 'region',
    cell: (h, { row }) => {
      return <p>
        {row.region.name}
      </p>
        ;
    },
  },
  {
    title: '组织机构', colKey: 'organization',
    cell: (h, { row }) => {
      return <p>
        {row?.organization?.name}
      </p>
        ;
    },
  },
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

</script>
<style lang="less" scoped>
.bx {
  box-shadow: 0 1px 4px rgba(38, 47, 62, 0.2);
  border-radius: 5px;
}

.weighLeft {
  padding: 15px;
  height: 577px;

  .hd {
    display: flex;

    .tt {
      font-size: 19px;
      font-weight: 400;
      margin-right: 12px;
    }

    .lc {
      .tg {
        border-radius: 3px;
        padding: 5px 5px 6px;
      }
    }
  }

  .weighCon {
    padding: 5px 0px;
    display: flex;

    .weighInp {
      // width: calc(100% - 110px);
      // padding-left:15px;
      display: flex;

      .inp {
        // width: 33.33%;
        padding-right: 15px;
      }
    }
  }
}

:deep(.weighCon .t-form__label) {
  padding-right: 15px;
}

.czbt {
  width: 130px;
  height: 42px;
}

.weighPh {
  padding: 15px;
  height: 577px;

  .phhd {
    display: flex;
    align-items: center;
    color: #2ba471;

    span {
      margin-left: 3px;
      font-size: 14px;
    }
  }

  .phcon {

    // display: flex;
    // justify-content: space-between;
    .phCld {
      // width: 48.5%;
    }
  }
}

.weighTb {
  padding: 15px 10px;

  .weighTbhd {
    padding: 15px 0px 5px;
    font-size: 14px;
  }

  .hislist {
    padding-top: 15px;
  }
}

.upwrap {
  padding: 8px 0px 0px;

  .uppic {
    background: #f5f5f5;
    border: 1px sloid #dedede;
    border-radius: 5px;
    height: 170px;
  }

  .upControl {
    padding: 10px 0px 0px;
    display: flex;
    justify-content: space-between;

    .upbt {
      width: 33%;
    }
  }
}














:deep(.t-card__title) {
  font: var(--td-font-title-large);
  font-weight: 400;
}

.user-left-greeting {
  padding: var(--td-comp-paddingTB-xxl) var(--td-comp-paddingLR-xxl);
  font: var(--td-font-title-large);
  background: var(--td-bg-color-container);
  color: var(--td-text-color-primary);
  text-align: left;
  border-radius: var(--td-radius-medium);
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 4px;

  .regular {
    margin-left: var(--td-comp-margin-xl);
    font: var(--td-font-body-medium);
  }

  .logo {
    width: 168px;
  }
}

.user-info-list {
  margin-top: var(--td-comp-margin-l);
  padding: var(--td-comp-paddingTB-xxl) var(--td-comp-paddingLR-xxl);
  height: 47vh;
  border-radius: 4px;

  .content {
    width: 90%;
  }

  :deep(.t-card__header) {
    padding: 0;
  }

  :deep(.t-card__body) {
    padding: 0;
  }

  .contract {
    &-title {
      white-space: nowrap;
      text-overflow: ellipsis;
      overflow: hidden;
      margin: var(--td-comp-margin-xxxl) 0 var(--td-comp-margin-l);
      font: var(--td-font-body-medium);
      color: var(--td-text-color-placeholder);
    }

    &-detail {
      white-space: nowrap;
      text-overflow: ellipsis;
      overflow: hidden;
      font: var(--td-font-body-medium);
      color: var(--td-text-color-primary);
    }
  }

  .contract:last-child {
    margin-bottom: 0;
  }
}

.user-intro {
  padding: var(--td-comp-paddingTB-xxl) var(--td-comp-paddingLR-xxl);
  background: #fff;
  border-radius: 5px;
  color: #000;

  :deep(.t-card__body) {
    padding: 0;
  }

  .name {
    font: var(--td-font-title-large);
    margin-top: var(--td-comp-margin-xxxl);
    color: var(--td-text-color-anti);
  }

  .position {
    font: var(--td-font-body-medium);
    margin-top: var(--td-comp-margin-s);
    color: var(--td-text-color-anti);
  }

  .user-info {
    line-height: 24px;
    font-size: 14px;
    color: var(--td-text-color-primary);

    .hiredate,
    .del,
    .mail {
      display: flex;
    }

    .t-icon {
      height: 24px;
      margin-right: 8px;
    }

    .del {
      margin: 16px 0;
    }
  }
}

.product-container {
  margin-top: var(--td-comp-margin-l);
  border-radius: var(--td-radius-medium);
  padding: var(--td-comp-paddingTB-xxl) var(--td-comp-paddingLR-xxl);
  border-radius: 4px;

  :deep(.t-card__header) {
    padding: 0;
    margin-bottom: var(--td-comp-margin-m);
  }

  :deep(.t-card__body) {
    padding: 0;
  }

  .content {
    width: 100%;
    margin: var(--td-comp-margin-xxxl) 0 0;
  }

  .logo {
    width: var(--td-comp-size-xxl);
  }
}

.content-container {
  margin-top: var(--td-comp-margin-l);
  background: var(--td-bg-color-container);
  border-radius: var(--td-radius-medium);
  padding: var(--td-comp-paddingTB-xxl) var(--td-comp-paddingLR-xxl);

  :deep(.t-card__header) {
    padding: 0;
  }

  :deep(.t-card__body) {
    padding: 0;
  }

  .card-padding-no {
    margin-top: var(--td-comp-margin-xxxl);

    :deep(.t-card__body) {
      margin-top: var(--td-comp-margin-xxl);
    }
  }
}

.user-team {
  margin-top: var(--td-comp-margin-l);
  padding: var(--td-comp-paddingTB-xxl) var(--td-comp-paddingLR-xxl);

  :deep(.t-card__header) {
    padding: 0;
    margin-bottom: var(--td-comp-margin-m);
  }

  :deep(.t-card__body) {
    padding: 0;
  }

  .t-list-item {
    margin-top: var(--td-comp-margin-xxl);
    padding: 0;

    :deep(.t-list-item__meta-avatar) {
      height: var(--td-comp-size-xxl);
      width: var(--td-comp-size-xxl);
      margin-right: var(--td-comp-margin-xxl);
    }

    :deep(.t-list-item__meta-title) {
      margin: 0 0 var(--td-comp-margin-xs);
    }

    :deep(.t-list-item__meta-description) {
      display: inline-block;
      color: var(--td-text-color-placeholder);
      font: var(--td-font-body-medium);
    }
  }
}
</style>
