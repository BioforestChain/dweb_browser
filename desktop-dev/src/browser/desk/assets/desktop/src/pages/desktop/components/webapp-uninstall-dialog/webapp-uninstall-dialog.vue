<script setup lang="ts">
import AppIcon from "src/components/app-icon/app-icon.vue";
import { $AppIconInfo } from "src/components/app-icon/types";
import { deleteApp } from "src/provider/api.ts";
import { $CloseWatcher, CloseWatcher } from "src/provider/shim.ts";
import { watchEffect } from "vue";

const props = defineProps({
  appId: { type: String, required: true },
  appIcon: { type: Object as () => $AppIconInfo, required: true },
  appName: { type: String, required: true },
  show: { type: Boolean },
});

const emit = defineEmits<{
  (event: "close", confirm: boolean): void;
}>();

async function doUninstall() {
  const response = await deleteApp(props.appId);
  if (response) {
    emit("close", true);
  }
  emit("close", true);
}
let dialogCloser: $CloseWatcher | null = null;
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
    :model-value="show"
    class="uninstall-dialog ios-ani align-end"
    scrim="danger-overlay-scrim"
    scrim-class="bg-danger-overlay-scrim"
    content-class="mb-16"
    transition="dialog-bottom-transition"
    persistent
    width="min(90%, 26em)"
    offset="10em"
  >
    <div class="glass flex rounded-2xl flex-column p-4 justify-center items-center dialog-layout">
      <div class="w-16 h-16 p-2 box-content">
        <AppIcon :icon="appIcon" size="100%"></AppIcon>
      </div>
      <h2 class="text-lg p-2">卸载“{{ appName }}”</h2>
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
.dialog-layout {
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
