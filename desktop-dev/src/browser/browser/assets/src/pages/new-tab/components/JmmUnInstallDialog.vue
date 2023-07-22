<script setup lang="ts">
import { deleteApp } from "@/api/new-tab";
import { watchEffect } from "vue";
import { CloseWatcher } from "../../../../../../../../../plaoc/src/client/components/close-watcher/close-watcher.shim";

const props = defineProps({
  appId: { type: String, required: true },
  appIcon: { type: String, required: true },
  appName: { type: String, required: true },
  show: { type: Boolean },
});

const emit = defineEmits<{
  (event: "close", confirm: boolean): void;
}>();

async function doUninstall() {
  const response = await deleteApp(props.appId);
  if (response.ok) {
    emit("close", true);
  }
}
let dialogCloser: CloseWatcher | null = null;
watchEffect(() => {
  if (props.show) {
    dialogCloser = new CloseWatcher();
    dialogCloser.addEventListener("close", () => {
      dialogCloser = null;
      emit("close", false);
    });
  }
});
</script>

<template>
  <v-dialog
    v-model="$props.show"
    class="uninstall-dialog ios-ani items-end"
    transition="dialog-bottom-transition"
    persistent
    width="min(90%, 26em)"
  >
    <div class="glass flex rounded-2xl flex-column p-4 justify-center items-center">
      <div class="w-16 h-16 p-2 box-content">
        <img class="img" :src="props.appIcon" alt="app icon" />
      </div>
      <h2 class="text-lg p-2">卸载“{{ props.appName }}”</h2>
      <p class="text-base">卸载后其所有数据也将被删除</p>
      <hr class="h-4" />
      <div class="flex justify-evenly items-center w-full">
        <button class="p-2 flex-1 text-base" color="green-darken-1" variant="text" @click="() => emit('close', false)">
          取消
        </button>
        <span class="w-px bg-neutral-400 h-5"></span>
        <button class="p-2 flex-1 text-base text-error" color="red" variant="text" @click="doUninstall">卸载</button>
      </div>
    </div>
  </v-dialog>
</template>
<style scoped lang="scss">
.glass {
  backdrop-filter: blur(var(--glass-blur, 40px)) saturate(1.2) contrast(1.2);
  background-color: rgba(255, 255, 255, 0.2);
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
