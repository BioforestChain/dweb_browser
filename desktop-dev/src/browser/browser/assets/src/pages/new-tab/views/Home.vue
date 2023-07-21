<script lang="ts" setup>
import { deleteApp, getAppInfo, getWidgetInfo } from "@/api/new-tab";
import wallpaper_url from "@/assets/wallpaper.webp";
import type { $AppMetaData, $TileSize, $WidgetMetaData } from "@/types/app.type";
import { onMounted, reactive, Ref, ref, StyleValue } from "vue";
import JMMVue from "../components/JMM.vue";
import TileItem from "../components/TileItem.vue";
import TilePanel from "../components/TilePanel.vue";
import WidgetVue from "../components/Widget.vue";

type $LayoutInfo = (
  | {
      type: "app";
      data: $AppMetaData;
    }
  | {
      type: "widget";
      data: $WidgetMetaData;
    }
  | {
      type: "blank";
    }
) & {
  xywh?: $XYWH;
  size: $TileSize;
};

interface $XYWH {
  x: number;
  y: number;
  w: number;
  h: number;
}

const layoutInfoListRef: Ref<$LayoutInfo[]> = ref([
  // {
  //   title: "app",
  //   short_name: "app name",
  //   icon: "https://dweb.waterbang.top/logo.svg",
  //   id: "waterbang.dweb",
  // },
]);

// 监听app消息的更新
const updateApps = async () => {
  const layoutInfoList: $LayoutInfo[] = [];

  for (const data of await getWidgetInfo()) {
    layoutInfoList.push({
      type: "widget",
      data,
      size: data.size,
    });
  }
  for (const data of await getAppInfo()) {
    layoutInfoList.push({
      type: "app",
      data,
      size: { column: 1, row: 1 },
    });
  }
  layoutInfoListRef.value = layoutInfoList;
};
Object.assign(globalThis, { updateApps });

const showDialog = ref(false);
const dialogData = reactive({
  title: "app",
  icon: "https://dweb.waterbang.top/logo.svg",
  id: "id",
  index: 0,
});
onMounted(async () => {
  await updateApps();
});

//删除app
function showUninstall(app: $AppMetaData, index: number) {
  showDialog.value = true;

  dialogData.icon = app.icon;
  dialogData.id = app.id;
  dialogData.title = app.short_name;

  dialogData.index = index;
}
// 卸载app
async function uninstall() {
  showDialog.value = false;
  const response = await deleteApp(dialogData.id);
  if (response.ok) {
    layoutInfoListRef.value.splice(dialogData.index, 1);
  }
}

const isFullscreen = window.screen.width === window.innerWidth;
const bgStyle = {
  // opacity: isFullscreen ? 1 : 0.62,
  backgroundImage: `url(${wallpaper_url})`,
} satisfies StyleValue;
</script>
<template>
  <div class="desktop">
    <div class="logo" :style="bgStyle">
      <img src="@/assets/logo.svg" alt="Dweb Browser" class="icon" />
      <div class="gradient_text">Dweb Browser</div>
    </div>
    <TilePanel>
      <TileItem v-for="(info, index) in layoutInfoListRef" :key="index" :column="info.size.column" :row="info.size.row">
        <JMMVue
          v-if="info.type === 'app'"
          :key="index"
          :index="index"
          :app-meta-data="info.data"
          @on-uninstall="() => showUninstall(info.data, index)"
        ></JMMVue>
        <WidgetVue v-if="info.type === 'widget'" :key="index" :index="index" :widget-meta-data="info.data"></WidgetVue>
      </TileItem>
    </TilePanel>
  </div>
  <v-dialog v-model="showDialog" persistent width="90%">
    <div class="dialog">
      <div class="app-icon">
        <img class="img" :src="dialogData.icon" alt="app icon" />
      </div>
      <div class="text">是否卸载"{{ dialogData.title }}"?</div>
      <div class="btn-content">
        <v-btn class="btn" color="green-darken-1" variant="text" @click="showDialog = false"> 取消 </v-btn>
        <div class="vertical-line"></div>
        <v-btn class="btn" color="red" variant="text" @click="uninstall"> 卸载 </v-btn>
      </div>
    </div>
  </v-dialog>
</template>
<style scoped lang="scss">
.desktop {
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: 1fr;
  grid-template-areas: "view";
  height: 100%;
  user-select: none;
  .logo {
    grid-area: view;
    z-index: 0;
    display: grid;
    place-items: center;
    background-size: cover;
    background-position: center;
    .icon {
      width: 13.5em;
      height: 13.5em;
      mix-blend-mode: color-burn;
    }
    .gradient_text {
      width: 100%;
      height: 2em;
      font-size: 20px;
      font-weight: 500;
      line-height: 1em;
      display: flex;
      justify-content: center;
      color: #fff;
      mix-blend-mode: overlay;
    }
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
