<script lang="ts" setup>
import { getAppInfo } from "@/api/new-tab";
import type { $AppMetaData } from "@/types/app.type";
import { onMounted, Ref, ref } from "vue";
import jmm from "../components/JMM.vue";
const $appContainer = ref<HTMLDivElement>();
//控制背景虚化
const filter = ref(false)

const appsInfo: Ref<$AppMetaData[]> = ref([
  {
    title: "xx",
    short_name: "app",
    id: "id",
    icon: "https://dweb.waterbang.top/logo.svg",
  },
  {
    title: "xx",
    short_name: "app",
    id: "id",
    icon: "https://dweb.waterbang.top/logo.svg",
  },
]);

onMounted(async () => {
  appsInfo.value = await getAppInfo()
});
// 长按事件
function onLongPress(value:boolean,index:number) {
  filter.value = value
  const appsDom = $appContainer.value
  if(!appsDom) return
  const apps: NodeListOf<HTMLDivElement> = appsDom.querySelectorAll(".app")
  console.log(apps.length,index)
  apps.forEach((item,i) => {
    if (index !== i) {
      item.style.filter = "blur(5px)";
    }
  })
const appElement = apps[index]
// 监听页面点击事件用来取消模糊
document.addEventListener("click", function(event) {
  // 检查点击的目标元素是否是 app 元素或其子元素
  var isClickedInsideApp = appElement.contains(event.target as Node);

  // 如果点击的目标元素不是 app 元素或其子元素，则移除模糊效果
  if (!isClickedInsideApp) {
    filter.value = false
    apps.forEach((item) => {
      item.style.filter = "none";
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
        ref="$appHtmlRefHook"
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
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
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
