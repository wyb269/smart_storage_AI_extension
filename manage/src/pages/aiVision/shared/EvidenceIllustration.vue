<template>
  <figure class="evidence-illustration">
    <button type="button" class="evidence-illustration__preview" @click="previewVisible = true">
      <img :src="imageUrl" :alt="`${subject}场景图片`" loading="lazy" />
    </button>
    <t-image-viewer v-model:visible="previewVisible" :images="[imageUrl]" :z-index="10000" />
  </figure>
</template>
<script setup lang="ts">
import { computed, ref } from 'vue';

type IllustrationKind =
  | 'doorAnomaly'
  | 'videoOcclusion'
  | 'personBehavior'
  | 'groundWater'
  | 'beltMachine'
  | 'perimeterIntrusion'
  | 'pestDetection'
  | 'vehicleRecognition';

const props = defineProps<{ kind: IllustrationKind }>();
const previewVisible = ref(false);

const subjects: Record<IllustrationKind, string> = {
  doorAnomaly: '仓库门',
  videoOcclusion: '镜头遮挡',
  personBehavior: '人员作业',
  groundWater: '地面积水',
  beltMachine: '皮带输送设备',
  perimeterIntrusion: '周界围栏',
  pestDetection: '储粮害虫',
  vehicleRecognition: '运粮车辆',
};

const subject = computed(() => subjects[props.kind]);
const imageUrl = computed(() => `${import.meta.env.BASE_URL}ai-vision-illustrations/${props.kind}.png`);
</script>
<style scoped>
.evidence-illustration {
  width: 100%;
  max-width: 320px;
  margin: 0;
  border: 1px solid #e7e9ee;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
}

.evidence-illustration__preview {
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  background: #f5f6f7;
  cursor: zoom-in;
}

.evidence-illustration__preview img {
  display: block;
  width: 100%;
  height: 150px;
  object-fit: cover;
}
</style>
