<script setup lang="ts">
import { onMounted, ref } from "vue";
import LogPanel, { defineLogAction, toConsole } from "../components/LogPanel.vue";
import type { $Axis, $MotionSensorsController, HTMLDwebMotionSensorsElement } from "../plugin";
const title = "MotionSensors";

const $logPanel = ref<typeof LogPanel>();
const $motionSensorsPlugin = ref<HTMLDwebMotionSensorsElement>();

let console: Console;
let motionSensors: HTMLDwebMotionSensorsElement;
onMounted(() => {
  console = toConsole($logPanel);
  motionSensors = $motionSensorsPlugin.value!;
});

let controllerAccelerometer: $MotionSensorsController | null = null;

const startAccelerometer = defineLogAction(
  async () => {
    controllerAccelerometer = await motionSensors.startAccelerometer(0.5);
    console.info("启动加速计传感器");
    motionSensors.addEventListener("readAccelerometer", (event: Event) => {
      const e = event as CustomEvent<$Axis>;
      console.info("加速计: ", e.detail);
    });
  },
  { name: "startAccelerometer", args: [], logPanel: $logPanel }
);

const stopAccelerometer = defineLogAction(
  async () => {
    console.info("关闭加速计传感器");
    controllerAccelerometer?.stop();
  },
  { name: "stopAccelerometer", args: [], logPanel: $logPanel }
);

let controllerGyroscope: $MotionSensorsController | null = null;

const startGyroscope = defineLogAction(
  async () => {
    controllerGyroscope = await motionSensors.startGyroscope(1);
    console.info("启动陀螺仪传感器");
    motionSensors.addEventListener("readGyroscope", (event: Event) => {
      const e = event as CustomEvent<$Axis>;
      console.info("陀螺仪: ", e.detail);
    });
  },
  { name: "startGyroscope", args: [], logPanel: $logPanel }
);
const stopGyroscope = defineLogAction(
  async () => {
    console.info("关闭螺仪传感器");
    controllerGyroscope?.stop();
  },
  { name: "stopGyroscope", args: [], logPanel: $logPanel }
);
</script>
<template>
  <dweb-motion-sensors ref="$motionSensorsPlugin"></dweb-motion-sensors>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/toast.svg" :alt="title" />
    </figure>

    <article class="card-body">
      <h2 class="card-title">MotionSensors</h2>

      <div class="justify-end card-actions">
        <button class="inline-block rounded-full btn btn-accent" @click="startAccelerometer">启动加速计传感器</button>
        <button class="inline-block rounded-full btn btn-accent" @click="stopAccelerometer">关闭加速计传感器</button>
      </div>
      <div class="justify-end card-actions">
        <button class="inline-block rounded-full btn btn-accent" @click="startGyroscope">启动陀螺仪传感器</button>
        <button class="inline-block rounded-full btn btn-accent" @click="stopGyroscope">关闭陀螺仪传感器</button>
      </div>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>
