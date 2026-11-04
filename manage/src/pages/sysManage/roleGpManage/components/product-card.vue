<template>
  <t-card theme="poster2" :bordered="false">
    <template #avatar>
      <t-avatar size="56px" style="    color: rgb(245 159 12); background: rgb(251, 248, 244);">
        <template #icon>
          <calendar-icon v-if="props.product.math < 5" />
          <user-avatar-icon v-if="props.product.math < 9 && props.product.math > 6" />
          <laptop-icon v-if="props.product.math < 7 && props.product.math > 4" />
          <t-icon name="assignment-checked" v-if="props.product.math > 8" />
        </template>
      </t-avatar>
    </template>
    <template #status>
      <!-- <t-tag theme="success" >
         已启用
      </t-tag> -->
    </template>
    <template #content>
      <p class="list-card-item_detail--name">{{ props.product.name }}</p>
      <p class="list-card-item_detail--desc">
        <!-- {{ props.product.description }} -->
        <!-- <div style="display: flex; align-items: center;">
        <div style="margin-right: 15px;"><span>组织机构：</span><span>{{ props.product.organization.name }}</span></div>
        <div><span>行政区：</span><span>{{ props.product.region.name }}</span></div>
        </div> -->
      <div style="display: flex; align-items: center;">
        <div style="margin-right: 15px;"><span>创建时间：</span><span>{{ props.product.addTime }}</span></div>
        <!-- <div><span>行政区：</span><span>{{ props.product.region.name }}</span></div> -->
      </div>
      </p>
    </template>
    <template #footer>
      <t-avatar-group cascading="left-up">
        <t-avatar @click="handleClickManage(props.product)"><t-icon name="edit" /></t-avatar>
        <t-avatar style="background: #fbebf1;color: #d54941;" @click="handleClickDelete(props.product)">
          <template #icon>
            <t-icon name="delete" font-size="14px" />
          </template>
        </t-avatar>
        <!-- <t-popconfirm  theme="danger"  @confirm="handleClickDelete(props.product)">
        <template #content>
          <p class="title">确认删除该角色组？</p>
          <p class="describe">角色组一旦删除，对应用户的菜单将无法<br />显示，请谨慎操作。</p>
        </template>
        <t-avatar  style="background: #fbebf1;color: #d54941;">
          <template #icon>
            <t-icon name="delete" font-size="14px" />
          </template>
        </t-avatar>
      </t-popconfirm> -->

      </t-avatar-group>
    </template>
    <template #actions>
      <!-- <t-dropdown
        :disabled="!props.product.isSetup"
        trigger="click"
        :options="[
          {
            content: t('components.manage'),
            value: 'manage',
            onClick: () => handleClickManage(props.product),
          },
          {
            content: t('components.delete'),
            value: 'delete',
            onClick: () => handleClickDelete(props.product),
          },
        ]"
      >
        <t-button theme="default" :disabled="!props.product.isSetup" shape="square" variant="text"> -->
      <more-icon disabled />
      <!-- </t-button>
      </t-dropdown> -->
    </template>
  </t-card>
</template>
<script setup lang="ts">
import {
  AddIcon,
  CalendarIcon,
  LaptopIcon,
  MoreIcon,
  ServiceIcon,
  ShopIcon,
  UserAvatarIcon,
} from 'tdesign-icons-vue-next';
import type { PropType } from 'vue';

import { t } from '@/locales';

export interface CardProductType {
  type: number;
  isSetup: boolean;
  description: string;
  name: string;
}

const props = defineProps({
  product: {
    type: Object as PropType<CardProductType>,
    default: undefined,
  },
});

const emit = defineEmits(['handleClickDetail', 'delete-item']);

const typeMap = ['A', 'B', 'C', 'D', 'E'];

const handleClickManage = (product: any) => {
  emit('handleClickDetail', product);
};

const handleClickDelete = (product: any) => {
  emit('delete-item', product);
};
</script>
<style lang="less" scoped>
.list-card-item {
  display: flex;
  flex-direction: column;
  cursor: pointer;

  &_detail {
    min-height: 140px;

    &--name {
      margin-bottom: var(--td-comp-margin-s);
      font: var(--td-font-title-medium);
      color: var(--td-text-color-primary);
    }

    &--desc {
      color: var(--td-text-color-secondary);
      font: var(--td-font-body-small);
      overflow: hidden;
      text-overflow: ellipsis;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }
  }
}
</style>
