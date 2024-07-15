<script setup lang="ts">
import { onMounted, ref } from "vue";
import LogPanel, { defineLogAction, toConsole } from "../components/LogPanel.vue";
import FieldLabel from "../components/FieldLabel.vue";
import { HTMLDwebKeychainElement, keychainPlugin } from "@plaoc/plugins";
import { watch } from "vue";
const $logPanel = ref<typeof LogPanel>();
let console: Console;

const $keychainPlugin = ref<HTMLDwebKeychainElement>();

let keychain: HTMLDwebKeychainElement;

onMounted(async () => {
  console = toConsole($logPanel);
  keychain = $keychainPlugin.value!;
});

const keychain_key = ref(localStorage.getItem("keychain-key") || "account-id");
const keychain_value = ref(localStorage.getItem("keychain-value") || "some-password");
const debounce_symbol = Symbol.for("debounce");
type $DebounceFn = {
  [debounce_symbol]?: any;
} & (() => void);
const $debounce = (fn: $DebounceFn, ms: number) => {
  clearTimeout(fn[debounce_symbol]);
  fn[debounce_symbol] = setTimeout(fn, ms);
};

watch(keychain_key, (keychain_key) => {
  $debounce(() => {
    localStorage.setItem("keychain-key", keychain_key);
  }, 200);
});

// webComponent 的调用方法
const getWb = defineLogAction(
  async () => {
    const data = await keychain.get(keychain_key.value);
    const decoder = new TextDecoder();
    return data && `${decoder.decode(data)} (${data.join(",")})`;
  },
  { name: "get", args: [], logPanel: $logPanel }
);

const hasWb = defineLogAction(
  async () => {
    return await keychain.has(keychain_key.value);
  },
  { name: "has", args: [], logPanel: $logPanel }
);

const deleteWb = defineLogAction(
  async () => {
    return await keychain.delete(keychain_key.value);
  },
  { name: "delete", args: [], logPanel: $logPanel }
);

const setWb = defineLogAction(
  async () => {
    return await keychain.set(keychain_key.value, keychain_value.value);
  },
  { name: "set", args: [], logPanel: $logPanel }
);

const title = "KeychainManager";
</script>

<template>
  <dweb-keychain ref="$keychainPlugin"></dweb-keychain>
  <div class="card glass">
    <figure class="icon">
      <div class="swap-on">🔑</div>
    </figure>
    <article class="card-body">
      <h2 class="card-title">钥匙串访问</h2>
      <FieldLabel label="Keychain Key:">
        <input type="text" id="keychain-key" v-model="keychain_key" />
      </FieldLabel>
      <FieldLabel label="Keychain Value:">
        <input type="text" id="keychain-value" v-model="keychain_value" />
      </FieldLabel>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="getWb">get</button>
        <button class="inline-block rounded-full btn btn-accent" @click="hasWb">has</button>
        <button class="inline-block rounded-full btn btn-accent" @click="deleteWb">delete</button>
        <button class="inline-block rounded-full btn btn-accent" @click="setWb">set</button>
      </div>
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>
