<script lang="ts" setup>
import jmm from "../components/JMM.vue";
import { onBeforeMount, reactive } from 'vue'

export interface $AppMetaData {
  title: string;
  subtitle: string;
  id: string;
  bundleUrl: string;
  bundleHash: string;
  bundleSize: number;
  icon: string;
  images: string[];
  introduction: string;
  author: string[];
  version: string;
  keywords: string[];
  home: string;
  mainUrl: string;
  server: {
    root: string;
    entry: string;
  };
  splashScreen: { entry: string };
  staticWebServers: $StaticWebServers[];
  openWebViewList: [];
  permissions: string[];
  plugins: string[];
  releaseDate: string;
}

export interface $StaticWebServers {
  root: string;
  entry: string;
  subdomain: string;
  port: number;
}

// type $DWEB_DEEPLINK = `dweb:${string}`;
interface $MainServer {
    root: string;
    entry: string;
  }

const state: {
  appsInfo: $AppMetaData[]
} = reactive({
  appsInfo: []
})

onBeforeMount(async () => {
  const url = `http://browser.dweb/appsinfo`
  const res = await fetch(url)
  if(res.status !== 200){
    console.error('请求失败：', res.statusText)
    return;
  }
  const appsInfo = await res.json()
  console.log('appInfo: ', appsInfo)
  updateAppsInfo(appsInfo)
})

function updateAppsInfo(value: $AppMetaData[]){
  state.appsInfo = value
}

function jmmOnClick(appMetaData: $AppMetaData){
  console.log('点击了 metadata: ', appMetaData)
}

</script>
<template>
  <div :class="$style.logo">
      <img src="@/assets/logo.svg" alt="Dweb Browser" class="icon" />
      <div :class="$style.gradient_text">Dweb Browser</div>
    </div>
  <div :class="$style.jmm_container">
  <jmm
    v-for="(item, index) in state.appsInfo"
    :key="index"
    :src="item.icon"
    @click="() => jmmOnClick(item)"
  >{{ item }}</jmm>
  </div>
</template>
<style module>
.container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  width: 100%;
  padding: 10px;
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

.jmm_container {
  width: 100%;
  height: 100%;
  position: absolute;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: flex-start;
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
