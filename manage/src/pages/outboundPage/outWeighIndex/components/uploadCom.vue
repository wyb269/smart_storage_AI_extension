<template>
  <div>
    <!-- <div class="uppic">
      <slot name="pic"></slot>
    </div>
    <div class="upControl">
      <t-button  variant="base" theme="primary" class="upbt" @click="carmera()">
        <template #icon><t-icon name="camera" style="margin-right: 3px;" /></template>
拍照</t-button>
<t-button variant="base" theme="success" class="upbt" @click="impf()">
  <template #icon><t-icon name="upload" style="margin-right: 3px;" /></template>
  上传</t-button>
</div>
<div style="width: 100%;">
  <t-button variant="outline" theme="danger" style="width: 100%;" @click="deleteP()">
    <template #icon><t-icon name="delete" style="margin-right: 3px;" /></template>
    删除</t-button>
</div> -->

    <div class="phcon">
      <div class="phCld">
        <div class="upwrap">
          <div class="clq">车辆前:</div>
          <div class="uppic" style="background: rgba(239, 245, 255, 1); border: 1px dashed rgba(24, 109, 245, 1); ">
            <t-image-viewer v-if="frontPic" :images="[frontPic]" :z-index="10000"></t-image-viewer>
          </div>
          <div class="upControl">
            <t-button variant="base" theme="primary" class="upbt" @click="carmera()">
              <template #icon><t-icon name="camera" style="margin-right: 3px;" /></template>
              拍照</t-button>

            <t-button variant="outline" theme="primary" class="upbt" @click="impf('0')">
              <template #icon>
                <img style="width: 17px;margin-right: 3px;" src="@/assets/imgs/uploadUp.png" />
              </template>
              上传</t-button>
            <t-button variant="outline" theme="primary" class="upbt" @click="deleteP('0')">
              <template #icon><t-icon name="delete" style="margin-right: 3px;" /></template>
              删除</t-button>
          </div>

        </div>
      </div>
      <div class="phCld">
        <div class="upwrap">
          <div class="clq">车辆后:</div>
          <div class="uppic" style="background:rgba(255, 247, 240, 1); border: 1px dashed rgba(241, 147, 58, 1)">
            <t-image-viewer v-if="backPic" :images="[backPic]" :z-index="10000"></t-image-viewer>
          </div>
          <div class="upControl">
            <t-button variant="base" theme="warning" class="upbt" @click="carmera()">
              <template #icon><t-icon name="camera" style="margin-right: 3px;" /></template>
              拍照</t-button>

            <t-button variant="outline" theme="warning" class="upbt" @click="impf('1')">
              <template #icon>
                <img style="width: 17px;margin-right: 3px;" src="@/assets/imgs/uploadDown.png" />
              </template>
              上传</t-button>
            <t-button variant="outline" theme="warning" class="upbt" @click="deleteP('1')">
              <template #icon><t-icon name="delete" style="margin-right: 3px;" /></template>
              删除</t-button>
          </div>

        </div>
      </div>
    </div>

    <t-dialog v-model:visible="impVisible" :header="derctPic == 0 ? '前车图片上传' : '后车图片上传'" style="border-radius: 5px;"
      class="newDialog newclassNewDialog" :cancelBtn="{ theme: 'default', variant: 'outline' }"
      :confirmBtn="{ theme: 'warning', variant: 'base' }" @confirm="imp">
      <t-space direction="vertical" style="width: 100%;padding-top:10px;">
        <div style="width: 336px; margin: 0 auto;position: relative;">
          <img v-if="file.length == 0" style="position: absolute;
          width: 122px;
          left: 107px;
          top: 23px;" src="@/assets/upicon.png">

          <t-upload :beforeUpload="upchange" v-model="file" :auto-upload="true" :abridge-name="[10, 8]"
            accept="image/jpeg, image/png" draggable>
          </t-upload>
          <div style="color: #394b5a;font-size: 13px;margin-top: 10px;text-align: center;">仅允许上传 <span
              style="color: #247dff;"> jpg,png </span>图片
          </div>
        </div>
      </t-space>
    </t-dialog>


  </div>
</template>
<script setup lang="ts">
import type { SubmitContext, UploadFailContext, UploadFile } from 'tdesign-vue-next';
import { MessagePlugin } from 'tdesign-vue-next';
import { ref, defineComponent, getCurrentInstance } from 'vue';

const { proxy } = getCurrentInstance();
const emit = defineEmits(['upPic', 'deletePic']);

const props = defineProps({
  formData: {},
  pagestu: {},
  derct: {}
});

const carmera = () => {
  MessagePlugin.error('拍照功能暂不支持，请上传照片!')
}
const deleteP = (n: any) => {
  if ((n == 0 && !frontPic.value) || (n == 1 && !backPic.value)) {
    MessagePlugin.error('您未上传照片!')
    return
  }
  derctPic.value = n
  emit('deletePic', n)
}
const confirmDt = () => {
  if (derctPic.value == 0) frontPic.value = ''
  if (derctPic.value == 1) backPic.value = ''
}


const tempPic = ref('')
const file = ref([])
const upchange = (e: any) => {
  // 获取当前时间
  let now = new Date();
  // 格式化日期和时间
  let formattedDate = now.getFullYear() + '-' +
    ('0' + (now.getMonth() + 1)).slice(-2) + '-' +
    ('0' + now.getDate()).slice(-2) + ' ' +
    ('0' + now.getHours()).slice(-2) + ':' +
    ('0' + now.getMinutes()).slice(-2) + ':' +
    ('0' + now.getSeconds()).slice(-2);
  file.value = [{
    name: e.name,
    url: '123.png',
    status: 'success',
    size: e.size,
    uploadTime: formattedDate,
    data: e
  }]
  return false
}

const frontPic = ref('')
const backPic = ref('')
const derctPic = ref('0')
const impf = (n: any) => {
  if (!props.formData.taskId) {
    MessagePlugin.error('请先刷卡!');
    return
  }
  derctPic.value = n
  impVisible.value = true
  file.value = []
}
const impVisible = ref(false)
const imp = () => {
  if (file.value.length == 0) {
    MessagePlugin.error('请上传图片');
    return
  }
  let picfile = file.value[0].data.raw
  proxy.$uploadPic(picfile).then(r => {
    if (r.objectKey) {
      console.log(r)

      emit('upPic', { objectKey: r.objectKey, derct: derctPic.value })
      MessagePlugin.success('上传成功!');
      impVisible.value = false
      showPic(r.objectKey)
    } else {
      MessagePlugin.error('上传失败');
    }
  })
}

const showPic = async (data: any) => {
  let url = await proxy.$getPic(data)
  if (derctPic.value == 0) frontPic.value = url
  if (derctPic.value == 1) backPic.value = url
}

defineExpose({
  confirmDt
})
</script>
<style lang="less" scoped>
.upwrap {
  padding: 15px 0px;

  .uppic {
    background: #f5f5f5;
    border: 1px sloid #dedede;
    border-radius: 5px;
    height: 255px;
    display: flex;
    align-items: center;
    overflow: hidden;
    justify-content: center;
  }

  .upControl {
    padding: 15px 0px 10px;
    display: flex;
    justify-content: space-between;

    .upbt {
      width: 47%;
    }
  }
}


.title {
  font-weight: 500;
  font-size: 14px;
}

.describe {
  margin-top: 8px;
  font-size: 12px;
  color: var(--td-text-color-secondary);
}

:deep(.t-upload__dragger) {
  border-radius: 5px;
  border: 1px dashed #acb3b2;
}



:deep(.t-upload__trigger) {
  color: #353f47;
  margin-top: 80px;
}

:deep(.t-upload__trigger span) {
  color: #353f47;
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

.clq {
  font-family: PingFang SC;
  font-weight: 400;
  font-style: Regular;
  font-size: 14px;
  leading-trim: NONE;
  line-height: 100%;
  letter-spacing: 0px;
  color: rgba(26, 26, 26, 1);
  margin-bottom: 8px;
  margin-top: 3px;
}
</style>
