<template>
  <div :class="sideNavCls">
    <t-menu
      :class="menuCls"
      :theme="theme"
      :value="active"
      :collapsed="collapsed"
      :expanded="expanded"
      :expand-mutex="menuAutoCollapsed"
      @expand="onExpanded"
    >
      <template #logo>
        <span v-if="showLogo" :class="`${prefix}-side-nav-logo-wrapper`" @click="goHome">
          <!-- <component v-if="collapsed"  :is="getLogo()" :class="logoCls" /> -->
           <img v-if="collapsed" style="width:32px;    margin-left: -18px;" src="@/assets/zhlogo.png">
          <div v-if="!collapsed" class="zhlogo" style="display: flex;align-items: center;">
            <img style=" width:32px;  margin-right:4px;" src="@/assets/zhlogo.png">种芯粮安智控管理平台</div>
        </span>
      </template>
      <menu-content :nav-data="menu" />
      <!-- <template #operations>
        <span :class="versionCls"> {{ !collapsed ? 'TDesign Starter' : '' }} {{ pgk.version }} </span>
      </template> -->
    </t-menu>
    <div :class="`${prefix}-side-nav-placeholder${collapsed ? '-hidden' : ''}`"></div>
  </div>
</template>
<script setup lang="ts">
import { difference, remove, union } from 'lodash';
import type { MenuValue } from 'tdesign-vue-next';
import type { PropType } from 'vue';
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';

import AssetLogoFull from '@/assets/assets-logo-full.svg?component';
import AssetLogo from '@/assets/assets-t-logo.svg?component';
import { prefix } from '@/config/global';
import { getActive } from '@/router';
import { useSettingStore } from '@/store';
import type { MenuRoute, ModeType } from '@/types/interface';

import pgk from '../../../package.json';
import MenuContent from './MenuContent.vue';

const { menu, showLogo, isFixed, layout, theme, isCompact } = defineProps({
  menu: {
    type: Array as PropType<MenuRoute[]>,
    default:[],
  },
  showLogo: {
    type: Boolean as PropType<boolean>,
    default: true,
  },
  isFixed: {
    type: Boolean as PropType<boolean>,
    default: true,
  },
  layout: {
    type: String as PropType<string>,
    default: '',
  },
  headerHeight: {
    type: String as PropType<string>,
    default: '64px',
  },
  theme: {
    type: String as PropType<ModeType>,
    default: 'dark',
  },
  isCompact: {
    type: Boolean as PropType<boolean>,
    default: false,
  },
});

const MIN_POINT = 992 - 1;

const collapsed = computed(() => useSettingStore().isSidebarCompact);
const menuAutoCollapsed = computed(() => useSettingStore().menuAutoCollapsed);

const active = computed(() => getActive());

const expanded = ref<MenuValue[]>([]);

const getExpanded = () => {
  const path = getActive();
  const parts = path.split('/').slice(1);
  const result = parts.map((_, index) => `/${parts.slice(0, index + 1).join('/')}`);

  expanded.value = menuAutoCollapsed.value ? result : union(result, expanded.value);
};

watch(
  () => active.value,
  () => {
    getExpanded();
  },
);

const onExpanded = (value: MenuValue[]) => {
  const currentOperationMenu = difference(expanded.value, value);
  const allExpanded = union(value, expanded.value);
  remove(allExpanded, (item) => currentOperationMenu.includes(item));
  expanded.value = allExpanded;
};

const sideMode = computed(() => {
  return theme === 'dark';
});
const sideNavCls = computed(() => {
  return [
    `${prefix}-sidebar-layout`,
    {
      [`${prefix}-sidebar-compact`]: isCompact,
    },
  ];
});
const logoCls = computed(() => {
  return [
    `${prefix}-side-nav-logo-${collapsed.value ? 't' : 'tdesign'}-logo`,
    {
      [`${prefix}-side-nav-dark`]: sideMode.value,
    },
  ];
});
const versionCls = computed(() => {
  return [
    `version-container`,
    {
      [`${prefix}-side-nav-dark`]: sideMode.value,
    },
  ];
});
const menuCls = computed(() => {
  return [
    `${prefix}-side-nav`,
    {
      [`${prefix}-side-nav-no-logo`]: !showLogo,
      [`${prefix}-side-nav-no-fixed`]: !isFixed,
      [`${prefix}-side-nav-mix-fixed`]: layout === 'mix' && isFixed,
    },
  ];
});

const router = useRouter();
const settingStore = useSettingStore();

const autoCollapsed = () => {
  const isCompact = window.innerWidth <= MIN_POINT;
  settingStore.updateConfig({
    isSidebarCompact: isCompact,
  });
};

onMounted(() => {
  getExpanded();
  autoCollapsed();

  window.addEventListener('resize', autoCollapsed);
});

onUnmounted(() => {
  window.removeEventListener('resize', autoCollapsed);
});

const goHome = () => {
  router.push('/dashboard/base');
};

const getLogo = () => {
  if (collapsed.value) return AssetLogo;
  return AssetLogoFull;
};
</script>
<style lang="less" scoped>
.zhlogo {
    // font: var(--td-font-headline-large);
    color: #fff;
    margin-left: -45px;
    margin-top: -5px;
    font-family: PingFang SC;
    // font-weight: 500;
    font-style: Medium;
    font-size: 16px;
    leading-trim: NONE;
    line-height: 100%;
    letter-spacing: 0px;
}


:deep(.t-default-menu .t-menu__item .t-icon-forDanJu) {
  background: url(@/assets/imgs/danju.png) center 0px no-repeat;
} 
</style>
