<script lang="ts" setup>
import { getAppInfo, vibrateHeavyClick } from "@/api/new-tab";
import type { $AppMetaData } from "@/types/app.type";
import { Ref, onMounted, ref } from "vue";
import jmm from "../components/JMM.vue";
const $appContainer = ref<HTMLDivElement>();
//控制背景虚化
const filter = ref(false)

const appsInfo: Ref<$AppMetaData[]> = ref([]);

onMounted(async () => {
  appsInfo.value = await getAppInfo()
});
// 长按事件
function onLongPress(value:boolean,index:number) {
  vibrateHeavyClick() //震动
  filter.value = value
  const appsDom = $appContainer.value
  if(!appsDom) return
  const apps: NodeListOf<HTMLDivElement> = appsDom.querySelectorAll(".app")
  apps.forEach((item,i) => {
    if (index !== i) {
      item.style.filter = "blur(5px)";
      item.style.pointerEvents = "none"
    }
  })
const appElement = apps[index]
// 监听页面点击事件用来取消模糊
document.addEventListener("click", function(event) {
  // 检查点击的目标元素是否是 app 元素或其子元素
  const isClickedInsideApp = appElement.contains(event.target as Node);
  // 如果点击的目标元素不是 app 元素或其子元素，则移除模糊效果
  if (!isClickedInsideApp) {
    filter.value = false
    apps.forEach((item) => {
      item.style.filter = "none";
      item.style.pointerEvents = "auto"
  })
  }
});
}

</script>
<template>
  <div class="container">
    <div class="logo" :class="{filter:filter}">
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
      ></jmm>
    </div>
  </div>
</template>
<style scoped>
.filter {
  filter: blur(4px);
}

.container {
  position: relative;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
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
