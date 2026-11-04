<template>
  <div v-if="layout === 'side'" class="header-menu-search">
    <t-input
      class="header-search"
      :class="[{ 'hover-active': isSearchFocus }]"
      v-model="searchData"
      placeholder="输入菜单名称"
      @change="chg"
    >
      <template #prefix-icon>
        <t-icon class="icon" name="search" size="16" color="#999" />
      </template>
    </t-input>
  </div>

  <div v-else class="header-menu-search-left">
    <t-button
      :class="{ 'search-icon-hide': isSearchFocus }"
      theme="default"
      shape="square"
      variant="text"
      @click="changeSearchFocus(true)"
    >
      <t-icon name="search" />
    </t-button>
    <t-input
      v-model="searchData"
      class="header-search"
      :class="[{ 'width-zero': !isSearchFocus }]"
      placeholder="输入菜单名称"
      :autofocus="isSearchFocus"
      @blur="changeSearchFocus(false)"
    >
      <template #prefix-icon>
        <t-icon name="search" size="16" />
      </template>
    </t-input>
  </div>
</template>
<script setup>
import { ref,defineEmits } from 'vue';

const emit = defineEmits([]);

import { t } from '@/locales';

defineProps({
  layout: {
    type: String,
    default: '',
  },
});
const chg=(e)=>{
    emit("update:modelValue",searchData.value);
}
const isSearchFocus = ref(false);
const searchData = ref('');
const changeSearchFocus = (value) => {
  if (!value) {
    searchData.value = '';
  }
  isSearchFocus.value = value;
};
</script>
<style lang="less" scoped>
.header-menu-search {
  display: flex;

  .hover-active {
    background: var(--td-bg-color-secondarycontainer);
  }



  .header-search {
    :deep(.t-input) {
      // border: none;
      width: 246px;
      outline: none;
      box-shadow: none;
      transition: background @anim-duration-base linear;

      .t-input__inner {
        transition: background @anim-duration-base linear;
        background: none;
      }

      &:hover {
        background: var(--td-bg-color-secondarycontainer);

        .t-input__inner {
          background: var(--td-bg-color-secondarycontainer);
        }
      }
    }
  }
}

.t-button {
  margin: 0 8px;
  transition: opacity @anim-duration-base @anim-time-fn-easing;

  .t-icon {
    font-size: 20px;

    &.general {
      margin-right: 16px;
    }
  }
}

.search-icon-hide {
  opacity: 0;
}

.header-menu-search-left {
  display: flex;
  align-items: center;

  .header-search {
    width: 200px;
    transition: width @anim-duration-base @anim-time-fn-easing;

    :deep(.t-input) {
      border: 0;

      &:focus {
        box-shadow: none;
      }
    }

    &.width-zero {
      width: 0;
      opacity: 0;
    }
  }
}
</style>
