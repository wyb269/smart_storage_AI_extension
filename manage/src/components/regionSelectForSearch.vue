<template>
  <t-cascader v-model="value" :options="options" clearable check-strictly :load="load" @change="selectChange" />
</template>

<script setup>
import { ref,onMounted } from 'vue'
import { useSettingStore,useUserStore } from '@/store';
import { listRegionByName,listRegionChildByIdAndLevels,rgFather,listRegionById } from '@/api/login';
const user = useUserStore();
const region = user.userInfo.region

const initlev=ref('')
const options = ref();
const value = ref();

const load = (node) =>
    new Promise((resolve) => {
        let n=initlev.value*1+(node.level+1)*1
        if (node.value) {
            listRegionChildByIdAndLevels(node.value,n).then(res => {
                const list = res ?. result
                console.log(formatRegionData(list, node.value || ''),55)
                resolve(formatRegionData(list, node.value || ''))
            })
        } else {
            resolve()
        }   
    });

const formatRegionData = (data, val) => {
    return data.filter(item => {
        return item.id != val
    }).map(item => {
        return {
            children:true,
            value: item.id,
            label: item.name,
            levels: item.levels,
            fullName: item.fullName,
            ...item
        }
    })
}

const selectChange = (e) => {
    console.log(e)
    emit("update:modelValue",e);
    // const data = regionCascader ?. value ?. getCheckedNodes() .length ?
    // regionCascader.value.getCheckedNodes()[0] ?. data : e
    // emit('selectChange', data)
    
}

const init = () => {
    listRegionById(region ?.id).then(res => {
        const data = res ?. result
        options.value =formatRegionData([data])
        initlev.value=res.result.levels
    })
} 
onMounted(() => {
//   selectValue.value = props.modelValue ?? null;
    init();
})
</script>