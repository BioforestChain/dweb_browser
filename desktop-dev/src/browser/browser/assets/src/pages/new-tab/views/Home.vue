<script lang="ts" setup>
import { deleteApp, getAppInfo } from "@/api/new-tab";
import type { $AppMetaData } from "@/types/app.type";
import { Ref, onMounted, reactive, ref } from "vue";
import jmm from "../components/JMM.vue";
const $appContainer = ref<HTMLDivElement>();

const appsInfo: Ref<$AppMetaData[]> = ref([
  // {
  //   title: "app",
  //   short_name: "app name",
  //   icon: "https://dweb.waterbang.top/logo.svg",
  //   id: "waterbang.dweb",
  // },
]);
const showDialog = ref(false);
const dialogData = reactive({
  title: "app",
  icon: "https://dweb.waterbang.top/logo.svg",
  id: "id",
  index:0
});
onMounted(async () => {
  appsInfo.value = await getAppInfo();
});

//删除app
function showUninstall(index: number) {
  showDialog.value = true;
  const app = appsInfo.value[index];
  dialogData.icon = app.icon;
  dialogData.id = app.id;
  dialogData.title = app.short_name;
  dialogData.index = index
}
// 卸载app
async function uninstall() {
  showDialog.value = false;
  const response = await deleteApp(dialogData.id);
  if (response.ok) {
    appsInfo.value.splice(dialogData.index, 1);
  }
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
        @on-uninstall="showUninstall"
      ></jmm>
    </div>
  </div>
  <v-dialog v-model="showDialog" persistent width="90%">
    <div class="dialog">
      <div class="app-icon">
        <img class="img" :src="dialogData.icon" alt="app icon" />
      </div>
      <div class="text">是否卸载"{{ dialogData.title }}"?</div>
      <div class="btn-content">
        <v-btn
          class="btn"
          color="green-darken-1"
          variant="text"
          @click="showDialog = false"
        >
          取消
        </v-btn>
        <div class="vertical-line"></div>
        <v-btn class="btn" color="red" variant="text" @click="uninstall"> 卸载 </v-btn>
      </div>
    </div>
  </v-dialog>
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
.dialog {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border-radius: 15px;
  box-shadow: 0px 3px 5px rgba(0, 0, 0, 0.3);
  background-color: rgba(255, 255, 255, 0.805);
  padding: 1em;
  .app-icon {
    width: 60px;
    height: 60px;
    border-radius: 15px;
    background-color: #fff;
    display: flex;
    justify-content: center;
    align-items: center;
    box-shadow: 0px 3px 5px rgba(0, 0, 0, 0.3);
    .img {
      width: 90%;
      height: auto;
    }
  }
  .text {
    font-size: 14px;
    font-weight: 0.1em;
    color: #333;
    margin: 1em auto;
    text-align: center;
    white-space: nowrap;
    overflow: hidden;
  }
  .btn-content {
    display: flex;
    width: 80%;
    justify-content: space-between;
    align-items: center;
    font-weight: bold;
  }
  .vertical-line {
    position: relative;
    height: 1.2em;
    width: 2px;
    background-color: rgba(190, 190, 190, 0.5); /* 设置线的颜色 */
  }

  .vertical-line::before {
    content: "";
    position: absolute;
    top: 0;
    left: 50%;
    transform: translateX(-50%);
  }
  .btn {
    font-size: 16px;
    font-weight: 1em;
  }
}
</style>
