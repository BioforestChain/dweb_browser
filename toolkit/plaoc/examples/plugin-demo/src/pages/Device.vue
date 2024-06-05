<script setup lang="ts">
import { isDweb } from "@plaoc/is-dweb";
import type { HTMLDeviceElement } from "@plaoc/plugins";
import { onMounted, ref } from "vue";
import LogPanel, { toConsole } from "../components/LogPanel.vue";

const title = "device";
const $deviceElement = ref<HTMLDeviceElement>();
const $logPanel = ref<typeof LogPanel>();

let console: Console;
let device: HTMLDeviceElement;

onMounted(async () => {
  device = $deviceElement.value!;
  console = toConsole($logPanel);
});

async function getUUID() {
  const res = await device.getUUID();
  console.log("uuid", res.uuid);
}

function isDwebBrowser() {
  console.log("isDwebBrowser=>", isDweb());
}

const props = ["name", "email", "tel", "address", "icon"];
const opts = { multiple: true };

declare global {
  interface Navigator {
    contacts?: Contacts; // 假设contacts是一个可选属性，并且它的类型是Contacts
  }
  interface Contacts {
    // 定义你期望的Contacts对象属性和方法
    select: (props: string[], opts: { multiple: boolean }) => Promise<Contact[]>;
  }
  interface Contact {
    name: string;
    email: string;
    tel: string;
    address: string;
    icon: string;
  }
}
async function getContacts() {
  try {
    const contacts = await navigator?.contacts?.select(props, opts);
    console.log(contacts);
  } catch (ex) {
    // Handle any errors here.
    console.log(ex);
  }
}
</script>
<template>
  <dweb-device ref="$deviceElement"></dweb-device>
  <div class="card glass">
    <figure class="icon">
      <div class="swap-on">🧬</div>
    </figure>
    <article class="card-body">
      <h2 class="card-title">UUID</h2>
      <v-btn color="indigo-darken-3" @click="getUUID">查询 UUID</v-btn>
    </article>
    <article class="card-body">
      <h2 class="card-title">是否是dweb</h2>
      <v-btn color="indigo-darken-3" @click="isDwebBrowser">是否在dweb环境下</v-btn>
    </article>
    <article class="card-body">
      <h2 class="card-title">获取联系人(android/ios suport)</h2>
      <v-btn color="indigo-darken-3" @click="getContacts">获取联系人</v-btn>
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>
