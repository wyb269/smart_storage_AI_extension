<template>
  <div class="index-container">
    <div class="form-and-tab-wrapper">
      <div class="form-content">
        <outboundreggisterPage :current-tab="activeTab" @tab-change="handleTabChange" v-if="activeTab === 'register'" />
        <outboundlogoutPage :current-tab="activeTab" @tab-change="handleTabChange" v-else-if="activeTab === 'logout'" />
      </div>
    </div>
  </div>
</template>

<script setup>
import outboundlogoutPage from './outboundlogoutPage.vue';
import outboundreggisterPage from './outboundreggisterPage.vue';
import { ref } from 'vue';

const activeTab = ref('register');

const handleTabChange = (targetTab) => {
  activeTab.value = targetTab;
};
</script>

<style lang="less" scoped>
.index-container {
  width: 100%;
  min-height: 100vh;
  box-sizing: border-box;
}

.form-and-tab-wrapper {
  width: 100%;
  border-radius: 4px;
  overflow: hidden;
}

.form-content {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  padding: 0;
}
</style>