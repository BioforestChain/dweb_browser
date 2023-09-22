<script setup lang="ts">
import { ref } from "vue";
import LogPanel from "../components/LogPanel.vue";
import { fileSystemPlugin } from "../plugin";

const title = "fileSystemPlugin";
const $logPanel = ref<typeof LogPanel>();

const fileListChange = ($event: Event) => {
  const target = $event.target as HTMLInputElement;
  if (target && target.files?.[0]) {
    console.log("target.files=>", target.files[0]);
    fileSystemPlugin.savePictures({ files: target.files });
  }
};
const fileChange = ($event: Event) => {
  const target = $event.target as HTMLInputElement;
  if (target && target.files?.[0]) {
    console.log("target.files=>", target.files[0]);
    fileSystemPlugin.savePictures({ file: target.files[0] });
  }
};
</script>
<template>
  <dweb-mwebview ref="$mwebviewPlugin"></dweb-mwebview>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/toast.svg" :alt="title" />
    </figure>

    <article class="card-body">
      <div>
        <h2 class="card-title">保存图片到相册 fileList</h2>
        <FieldLabel label="files:">
          <input type="file" multiple="true" @change="fileListChange($event)" />
        </FieldLabel>
      </div>
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
