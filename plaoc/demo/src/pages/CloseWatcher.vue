<script setup lang="ts">
import dialogPolyfill from "dialog-polyfill";
import { onMounted, ref } from "vue";
import LogPanel, { toConsole } from "../components/LogPanel.vue";

const title = "Close Watcher";
const $logPanel = ref<typeof LogPanel>();
const $dialogEle = ref<HTMLDialogElement>();
const showModal = ref(false);

let console: Console;
let dialogEle: HTMLDialogElement;
onMounted(() => {
  console = toConsole($logPanel);
  dialogEle = $dialogEle.value!;

  if (!(dialogEle.showModal instanceof Function)) {
    dialogPolyfill.registerDialog(dialogEle);
  }
});

const openDialog = () => {
  if (dialogEle.open) {
    return;
  }
  dialogEle.showModal();
  showModal.value = true;
  const closer = new CloseWatcher();
  closer.addEventListener("close", (event) => {
    console.log("CloseWatcher close", event.isTrusted, event.timeStamp);
    dialogEle.close();
    showModal.value = false;
  });
  dialogEle.onclose = (event) => {
    console.log("DialogEle close", event.isTrusted, event.timeStamp);
    closer.close();
  };
  dialogEle.oncancel = (event) => {
    console.log("DialogEle cancel", event.isTrusted, event.timeStamp);
    closer.close();
  };
};
const closeDialog = () => {
  dialogEle.close();
};
</script>

<template>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/closewatcher.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">Close Watcher</h2>
      <dialog ref="$dialogEle">
        <div class="modal" :class="{'modal-open': showModal}">
          <div class="modal-box">
            <h3 class="text-lg font-bold">Dialog</h3>
            <p class="py-4">Hi</p>
            <div class="modal-action">
              <button class="btn" @click="closeDialog">Yay!</button>
            </div>
          </div>
        </div>
      </dialog>

      <button class="inline-block rounded-full btn btn-accent" @click="openDialog">Open Dialog</button>
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>
