<script lang="ts" setup>
import { CONST } from "@/const";
import type { $AppMetaData } from "@/types/app.type";
import { Ref, onMounted, ref } from 'vue';
import jmm from "../components/JMM.vue";

const appsInfo:Ref<$AppMetaData[]> = ref([{
  title :"xx",
  short_name: "app",
  id: "id",
  icon: "https://dweb.waterbang.top/logo.svg",
},{
  title :"xx",
  short_name: "app",
  id: "id",
  icon: "https://dweb.waterbang.top/logo.svg",
}])

onMounted(async () => {
  await getAppInfo()
})

async function getAppInfo(){
  const url = `${CONST.BASR_URL}/appsInfo`
  const res = await fetch(url)
  if(res.status !== 200){
    console.error('请求失败：', res.statusText)
    return;
  }
  const data = await res.json()
  console.log('appInfo: ', data)
  appsInfo.value = data
}
const $appContainer = ref<HTMLDivElement>()
</script>
<template>
  <div class="logo">
      <img src="@/assets/logo.svg" alt="Dweb Browser" class="icon" />
      <div class="gradient_text">Dweb Browser</div>
    </div>
  <div class="jmm_container" ref="$appContainer">
    <jmm
    v-for="(app, index) in appsInfo"
    :key="index"
    :app-meta-data="app"
  ></jmm>
  </div>
</template>
<style scoped>
.container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  width: 100%;
}

.logo {
  width: 100%;
  height: 100%;
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0.5;
}

/* .jmm_container {
  width: 100%;
  position: absolute;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-evenly;
  padding: 10px;
} */
.jmm_container{
  margin: 10px auto;
  width: 100%;
  position: absolute;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(auto-fill, 70px); /* 2 */
  grid-gap: 1rem; /* 3 */
  justify-content: space-evenly; /* 4 */
}

.icon {
  width: 210px;
  height: 210px;
  border-radius: 0px 0px 0px 0px;
  opacity: 1;
}
.gradient_text {
  width: 100%;
  height: 20px;
  font-size: 20px;
  font-family: Source Han Sans CN-Medium, Source Han Sans CN;
  font-weight: 500;
  color: #0a1626;
  line-height: 20px;
  display: flex;
  justify-content: center;
}
</style>
