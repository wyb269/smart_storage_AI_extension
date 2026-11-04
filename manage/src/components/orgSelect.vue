<template>
  <t-cascader v-model="value" :options="options" :disabled="disabled" clearable check-strictly :load="load" @change="selectChange" />
</template>

<script setup>
import { ref,onMounted,defineEmits } from 'vue'
import { useSettingStore,useUserStore } from '@/store';
import { orgCas,orgFather } from '@/api/org';
const user = useUserStore();
const region = user.userInfo.region
const emit = defineEmits(['initData','getdata']);


const initData=(data)=>{
    let arry=[]
    console.log(data)
    const getall=(id)=>{
        orgFather(id).then(r=>{
            let v=[]
            r.result.map(x=>{
                v.push(x.name)
            })
            console.log(v)
            v.reverse()
            let a=v.join('/')
            console.log(a)
            value.value=a

          //  v.push(arr)
          //  arry=formatRegionData(arry)
            // // console.log(arry,88)
            // let v=[]
            // const lookup=(id,dat)=>{
            //     let a=arry.filter(x=>x.id==id)
            //     let b=arry.filter(x=>x.parentId==id)
            //     a[0].items=b
            //     options.value=a
            //     console.log(options.value,7777)
            //     // if(b.length>0){
            //     //     dat=[...b]
            //     //     dat[0].items=[]
            //     //     data.map(x=>{
            //     //         x.items
            //     //     })
            //     // }
            // }
            // lookup(0,v)
            console.log(v,777)
        })
    }
    getall(data.id)
}

const initlev=ref('')
const options = ref();
const value = ref();
const load = (node) =>
    new Promise((resolve) => {
        let n=initlev.value*1+(node.level+1)*1
        if (node.value) {
            orgCas(node.value,n).then(res => {
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
    console.log(value.value,999)
   // emit("update:modelValue",e);
    // const data = regionCascader ?. value ?. getCheckedNodes() .length ?
    // regionCascader.value.getCheckedNodes()[0] ?. data : e
    emit('getdata',e)
    
}

const init = () => {
    orgCas('0','0').then(res => {
        const data = res ?. result
        options.value =formatRegionData(data)
        initlev.value=res.result[0].levels
    })
} 
const rst=()=>{
    value.value=''
    init()
}
onMounted(() => {
//   selectValue.value = props.modelValue ?? null;
    init();
})
defineExpose({
initData,
rst
})
</script>