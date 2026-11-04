<template>
  <t-breadcrumb
    max-item-width="150"
    separator="/"
    class="tdesign-breadcrumb"
    style=" margin-bottom: 0px;margin-left: 11px;font-size: 14px;"
  >
    <!-- :to="item.to" -->
    <t-breadcrumbItem
      v-for="item in crumbs"
      :key="item.to"
    >
      {{ item.meta.title.zh_CN }}
    </t-breadcrumbItem>
  </t-breadcrumb>
</template>
<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useLocale } from "@/locales/useLocale";
import type { RouteMeta } from "@/types/interface";

const router = useRouter();
const { locale } = useLocale();
const route = useRoute();

const crumbs = computed(() => {
  const pathArray = route.path.split("/");
  pathArray.shift();
  // console.log(pathArray, 76)
  let ary = [];
  try {
    let justShowOne = [
      "userCenter",
      "pageHome",
      "weigh",
      "sampling",
      "register",
      "quality",
      "wareHouse",
      "balance",
    ];
    if (justShowOne.indexOf(pathArray[0]) > -1) {
      const resolved = router.resolve({ name: pathArray[0] });
      ary.push(resolved);
    } else {
      for (let i = 0; i < pathArray.length; i++) {
        // console.log(pathArray[i], 889)
        const resolved = router.resolve({ name: pathArray[i] });
        ary.push(resolved);
      }
    }
    // console.log(ary)
  } catch (e) {
    // console.log(e);
  }

  return ary;

  // const breadcrumbs:any =pathArray.reduce((breadcrumbArray, path, idx) => {
  //   // 如果路由下有hiddenBreadcrumb或当前遍历到参数则隐藏
  //   console.log(path,75)
  //   console.log(idx,76)
  //   console.log(route,77)
  //   console.log(route.matched,79)
  //   const meta = route.matched[idx]?.meta as RouteMeta;
  //    console.log(meta,78)
  //   if (meta?.hiddenBreadcrumb || Object.values(route.params).includes(path)) {
  //     return breadcrumbArray;
  //   }
  //   let title = path;
  //   if (meta?.title) {
  //     if (typeof meta.title === 'string') {
  //       title = meta.title;
  //     } else {
  //       title = meta.title[locale.value];
  //     }
  //   }
  //   breadcrumbArray.push({
  //     path,
  //     to: breadcrumbArray[idx - 1] ? `${breadcrumbArray[idx - 1].to}/${path}` : `/${path}`,
  //     title,
  //   });
  //   return breadcrumbArray;
  // }, []);
  // return breadcrumbs;
});
</script>
<style scoped>
.tdesign-breadcrumb {
  margin-bottom: 24px;
}
</style>
