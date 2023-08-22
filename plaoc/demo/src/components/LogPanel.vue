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
const format = (...logs: unknown[]) =>
  logs
    .map((v) => {
      if (typeof v === "object" && v !== null) {
        if (Object.getPrototypeOf(v) === Object.prototype) {
          try {
            return `<span class="text-blue">${JSON.stringify(v)}</span>`;
          } catch {}
        } else if (v instanceof Error) {
          return `<span class="text-red">${v.stack || v.message}</span>`;
        }
      }
      return String(v);
    })
    .join(" ");
const log = (...logs: unknown[]) => {
  const message = format(...logs);
  pushMessage({ message, type: "debug", prefix: "~", class: "" });
};
const error = (...logs: unknown[]) => {
  const message = format(...logs);
  pushMessage({ message, type: "error", prefix: ">", class: "text-error" });
};
const warn = (...logs: unknown[]) => {
  const message = format(...logs);
  pushMessage({ message, type: "warn", prefix: ">", class: "text-warning" });
};
const success = (...logs: unknown[]) => {
  const message = format(...logs);
  pushMessage({ message, type: "success", prefix: ">", class: "text-success" });
};
const info = (...logs: unknown[]) => {
  const message = format(...logs);
  pushMessage({ message, type: "info", prefix: ">", class: "text-info" });
};
const timeMap = new Map<string, { msgId: number; startTime: number }>();
const time = (label: string, ...logs: unknown[]) => {
  if (timeMap.has(label)) {
    return;
  }
  const msgId = pushMessage({
    message: format(label, ...logs),
    type: "info",
    prefix: ".",
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
          ` <span class="px-1 text-[0.6rem] rounded-lg bg-accent text-accent-content shrink-0">+${
            (endTime - timeItem.startTime) / 1000
          }ms</span>`,
        type: "info",
        prefix: ".",
        class: "text-accent",
      },
      timeItem.msgId
    );
  }
};
const clear = () => {
  timeMap.clear();
  messageLists.value.clear();
};
defineExpose({ log, debug: log, warn, success, error, info, time, timeEnd, clear });
</script>
<template>
  <div class="mockup-code text-xs min-w-full w-full">
    <div class="max-h-[60vh] overflow-y-auto overflow-x-clip flex flex-col-reverse">
      <div class="anchor"></div>
      <TransitionGroup name="fade">
        <pre
          v-for="[id, item] in messageLists"
          :key="id"
          :data-prefix="id + item.prefix"
          class="whitespace-normal flex flex-row justify-start py-1"
          :class="item.class"
        ><code class="flex-1 break-all flex flex-wrap justify-between flex-row" v-html="item.message"></code></pre>
      </TransitionGroup>
    </div>

    <div class="actions pt-5 pr-5 flex justify-end">
      <button class="btn btn-sm btn-outline btn-primary" @click="clear()">Clear Log</button>
    </div>
  </div>
</template>

<script lang="ts">
import { Ref, isRef } from "vue";
import type LogPanel from "./LogPanel.vue";
export const toConsole = (ele: Ref<typeof LogPanel | undefined>) => {
  return ele.value! as unknown as Console;
};

const normalizeArgs = (args: any[]) => {
  const res: any[] = [];
  for (const arg of args) {
    if (isRef(arg)) {
      res.push(arg.value);
    } else {
      res.push(arg);
    }
  }
  return res;
};

export const defineLogAction = <T extends (...args: any[]) => Promise<unknown>>(
  fun: T,
  config: {
    logPanel: Ref<typeof LogPanel | undefined>;
    name: string;
    args: Array<unknown | Ref<unknown>>;
  }
) => {
  const nargs = () => normalizeArgs(config.args);
  return (async (...args: any[]) => {
    const logger = config.logPanel.value || console;
    logger.time(config.name, ...nargs());
    try {
      const result = await fun(...args);
      logger.timeEnd(
        config.name,
        ...nargs(),
        `<span class="px-1 text-[0.6rem] rounded-lg bg-accent text-accent-content shrink-0">-></span>`,
        result
      );
      return result;
    } catch (err) {
      logger.timeEnd(config.name, ...nargs());
      logger.error(config.name, err);
    }
  }) as unknown as T;
};
</script>
<style>
.mockup-code pre.flex:before {
  flex-shrink: 0;
}
</style>

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
.anchor {
  overflow-anchor: auto;
}
</style>
