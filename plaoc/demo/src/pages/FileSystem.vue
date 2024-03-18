<script setup lang="ts">
import { ref } from "vue";
import LogPanel from "../components/LogPanel.vue";
import { mediaPlugin } from "../plugin";

const title = "fileSystemPlugin";
const $logPanel = ref<typeof LogPanel>();

const fileChange = ($event: Event) => {
  const target = $event.target as HTMLInputElement;
  if (target && target.files?.[0]) {
    console.log("target.files=>", target.files[0]);
    mediaPlugin.savePictures({ file: target.files[0] });
  }
};
</script>
<template>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/toast.svg" :alt="title" />
    </figure>

    <article class="card-body">
      <div>
        <h2 class="card-title">保存图片到相册 File</h2>
        <FieldLabel label="files:">
          <input type="file" accept="image/*" @change="fileChange($event)" />
        </FieldLabel>
      </div>
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>
