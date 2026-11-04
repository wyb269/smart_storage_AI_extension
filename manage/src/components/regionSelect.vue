<template>
  <t-cascader v-model="value" :options="options" clearable check-strictly :load="load" @change="selectChange" />
</template>

<script setup>
import { ref,onMounted,defineEmits } from 'vue'
import { useSettingStore,useUserStore } from '@/store';
import { listRegionByName,listRegionChildByIdAndLevels,rgFather,listRegionById } from '@/api/login';
const user = useUserStore();
const region = user.userInfo.region

const emit = defineEmits(['initData','getRgdata']);

const initlev=ref('')
const options = ref();
const value = ref();


const initData=(data)=>{
    let arry=[]
    console.log(data,456)
    const getall=(id)=>{
        rgFather(id).then(r=>{
            let v=[]
            let v1=[]
            r.result.map(x=>{
                if(x.id!=0)v.push(x.name)
                if(x.id==0)v1.push(x.name)
            })
            console.log(v)
            //v.reverse()
            v=[...v1,...v]
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
     emit('getRgdata',e)
    // emit("update:modelValue",e);
    // const data = regionCascader ?. value ?. getCheckedNodes() .length ?
    // regionCascader.value.getCheckedNodes()[0] ?. data : e
    // emit('selectChange', data)
    
}

const init = () => {
    // listRegionByName({ name:'中华人民共和国' }).then(res => {
    listRegionById(region ?.id).then(res => {
        const data = res ?. result
        options.value =formatRegionData([data])
        initlev.value=res.result.levels
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