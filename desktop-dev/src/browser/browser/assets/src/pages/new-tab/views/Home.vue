<script lang="ts" setup>
import { getAppInfo, vibrateHeavyClick } from "@/api/new-tab";
import type { $AppMetaData } from "@/types/app.type";
import { Ref, onMounted, ref } from "vue";
import jmm from "../components/JMM.vue";
const $appContainer = ref<HTMLDivElement>();
//控制背景虚化
const show = ref(false);

const appsInfo: Ref<$AppMetaData[]> = ref([
  // {
  //   title: "app",
  //   short_name: "app name",
  //   icon: "",
  //   id: "waterbang.dweb",
  // }
]);

onMounted(async () => {
  appsInfo.value = await getAppInfo();
});
// 长按事件
function onLongPress(value: boolean) {
  vibrateHeavyClick(); //震动
  show.value = value;
}
//删除app
function onUninstall(index: number) {
  appsInfo.value.splice(index, 1);
}
</script>
<template>
  <div class="container">
    <div class="logo">
      <img src="@/assets/logo.svg" alt="Dweb Browser" class="icon" />
      <div class="gradient_text">Dweb Browser</div>
    </div>
    <div class="jmm_container" ref="$appContainer">
      <jmm
        v-for="(app, index) in appsInfo"
        :key="index"
        :index="index"
        :app-meta-data="app"
        @on-long-press="onLongPress"
        @on-uninstall="onUninstall"
      ></jmm>
    </div>
  </div>
</template>
<style scoped lang="scss">
.container {
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: 1fr;
  grid-template-areas: "view";
  height: 100%;
  .logo {
    grid-area: view;
    z-index: 0;
    display: grid;
    place-items: center;
    opacity: 0.5;
    .icon {
      width: 13.5em;
      height: 13.5em;
    }
    .gradient_text {
      width: 100%;
      height: 2em;
      font-size: 20px;
      font-family: Source Han Sans CN-Medium, Source Han Sans CN;
      font-weight: 500;
      color: #0a1626;
      line-height: 1em;
      display: flex;
      justify-content: center;
    }
  }
  .jmm_container {
    grid-area: view;
    z-index: 1;
    align-self: start;
    display: grid;
    grid-template-columns: repeat(auto-fill, 90px); /* 2 */
    justify-content: space-evenly; /* 4 */
  }
}
</style>
