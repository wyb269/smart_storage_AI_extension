<template>
  <l-side-nav
    v-if="settingStore.showSidebar"
    :show-logo="settingStore.showSidebarLogo"
    :layout="settingStore.layout"
    :is-fixed="settingStore.isSidebarFixed"
    :menu="sideMenu"
    :theme="settingStore.displaySideMode"
    :is-compact="settingStore.isSidebarCompact"
  />
</template>
<script setup lang="ts">
import { storeToRefs } from "pinia";
import { computed } from "vue";
import { useRoute } from "vue-router";

import { usePermissionStore, useSettingStore } from "@/store";
import type { MenuRoute } from "@/types/interface";

import LSideNav from "./SideNav.vue";

const route = useRoute();
const permissionStore = usePermissionStore();
const settingStore = useSettingStore();
const { routers: menuRouters } = storeToRefs(permissionStore);

const { searchData } = defineProps({
  searchData: {
    type: String,
    default: "",
  },
});

const sideMenu = computed(() => {
  const { layout, splitMenu } = settingStore;
  let mu = menuRouters.value as Array<MenuRoute>;
  let newMenuRouters = JSON.parse(JSON.stringify(mu));
  const lookUp = (a: any) => {
    let newary: any = [];
    a.children.map((n: any) => {
      if (!n.hidden) {
        newary.push(n);
        if (n.children) lookUp(n);
      }
    });
    a.children = newary;
  };
  let ary: any = [];
  newMenuRouters.map((x: any) => {
    if (!x.hidden) {
      if (x.children) lookUp(x);
      ary.push(x);
    }
  });
  const fl = (son: any, part: any) => {
    // console.log(part,55)
    son = son.filter((y: any) => {
      if (y.meta?.title.zh_CN.indexOf(searchData) > -1) {
        return y;
      } else {
        y.a = false;
        if (y.children) y.a = fl(y.children, y);
        return y.a == true;
      }
    });
    // console.log(son, 77);
    part.children = son;
    if (son.length > 0) return true;
  };
  ary = ary.filter((x: any) => {
    if (x.meta?.title.zh_CN.indexOf(searchData) > -1) {
      return x;
    } else {
      // console.log(x, 66);
      x.a = false;
      if (x.children) x.a = fl(x.children, x);
      return x.a == true;
    }
  });
  // console.log(ary,998)
  newMenuRouters = ary;
  if (layout === "mix" && splitMenu) {
    newMenuRouters.forEach((menu) => {
      if (route.path.indexOf(menu.path) === 0) {
        newMenuRouters = menu.children.map((subMenu) => ({
          ...subMenu,
          path: `${menu.path}/${subMenu.path}`,
        }));
      }
    });
  }
  return newMenuRouters;
});
</script>
