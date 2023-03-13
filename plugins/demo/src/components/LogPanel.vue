<script setup lang="ts">
import { ref } from "vue";

type $Message = {
  message: string;
  type: "info" | "debug" | "success" | "error" | "warn" | "time";
  prefix: string;
  class: string;
};
const messageLists = ref<Map<number, $Message>>(new Map());
let msg_id_acc = 1;
const pushMessage = (msg: $Message, id = msg_id_acc++) => {
  messageLists.value.set(id, msg);
  return id;
};
const format = (...logs: unknown[]) => logs.map((v) => String(v)).join(" ");
const log = (...logs: unknown[]) => {
  const message = format(logs);
  pushMessage({ message, type: "debug", prefix: "~", class: "" });
};
const error = (...logs: unknown[]) => {
  const message = format(logs);
  pushMessage({ message, type: "error", prefix: "❌", class: "text-error" });
};
const warn = (...logs: unknown[]) => {
  const message = format(logs);
  pushMessage({ message, type: "warn", prefix: ">", class: "text-warning" });
};
const success = (...logs: unknown[]) => {
  const message = format(logs);
  pushMessage({ message, type: "success", prefix: "✅", class: "text-success" });
};
const info = (...logs: unknown[]) => {
  const message = format(logs);
  pushMessage({ message, type: "info", prefix: ">", class: "text-info" });
};
const timeMap = new Map<string, { msgId: number; startTime: number }>();
const time = (label: string, ...logs: unknown[]) => {
  const msgId = pushMessage({
    message: format(label, ...logs),
    type: "info",
    prefix: "⏲",
    class: "bg-accent text-accent-content",
  });
  const startTime = Date.now();
  timeMap.set(label, { startTime, msgId });
};
const timeEnd = (label: string, ...logs: unknown[]) => {
  const timeItem = timeMap.get(label);
  if (timeItem) {
    const endTime = Date.now();
    timeMap.delete(label);
    messageLists.value.delete(timeItem.msgId);
    pushMessage(
      {
        message:
          format(label, ...logs) +
          ` <span class="px-1 text-[0.6rem] rounded-lg bg-accent text-accent-content">+${
            (endTime - timeItem.startTime) / 1000
          }ms</span>`,
        type: "info",
        prefix: "●",
        class: "text-accent",
      },
      timeItem.msgId
    );
  }
};
const clear = () => {
  messageLists.value.clear();
};
defineExpose({ log, debug: log, warn, success, error, info, time, timeEnd, clear });
</script>
<template>
  <div class="mockup-code text-xs min-w-max w-full">
    <div class="max-h-[60vh] overflow-y-auto overflow-x-clip flex flex-col-reverse">
      <TransitionGroup name="fade">
        <pre
          v-for="[id, item] in messageLists"
          :key="id"
          :data-prefix="id + ' ' + item.prefix"
          :class="item.class"
        ><code v-html="item.message"></code></pre>
      </TransitionGroup>
    </div>

    <div class="actions pt-5 pr-5 flex justify-end">
      <button class="btn btn-sm btn-outline btn-primary" @click="clear()">Clear Log</button>
    </div>
  </div>
</template>

<script lang="ts">
import type LogPanel from "./LogPanel.vue";
import type { Ref } from "vue";
export const toConsole = (ele: Ref<typeof LogPanel | undefined>) => {
  return ele.value! as unknown as Console;
};
</script>

<style>
.container {
  position: relative;
  padding: 0;
}
/* 1. 声明过渡效果 */
.fade-move,
.fade-enter-active,
.fade-leave-active {
  transition: all 0.5s cubic-bezier(0.55, 0, 0.1, 1);
}

/* 2. 声明进入和离开的状态 */
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: scaleY(0.01) translate(30px, 0);
}

/* 3. 确保离开的项目被移除出了布局流
      以便正确地计算移动时的动画效果。 */
.fade-leave-active {
  position: absolute;
}
</style>
