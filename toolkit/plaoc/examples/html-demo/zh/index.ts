import { configPlugin, dwebServiceWorker } from "@plaoc/plugins";
import { process } from "./barcode-scannering.ts";
import { message } from "./debug.ts";

// 测试所有的api
const runPlugins = () => {
  message("测试 process识别图片");
  process();
};

// 向desktop.dweb.waterbang.top.dweb 发送消息
const sayHi = async (message = "今晚吃螃🦀️蟹吗？") => {
  const input = document.getElementById("input1") as HTMLInputElement;
  const data = input.value;
  if (data) {
    message = data;
  }
  const base = new URL(document.baseURI);
  const url = new URL("/say/hi", base.origin);
  url.searchParams.set("message", message);
  console.log("sayHi=>", data, url.href);
  const res = await dwebServiceWorker.externalFetch(`game.dweb.waterbang.top.dweb`, url);
  console.log("收到回应消息 => ", await res.text());
};

dwebServiceWorker.addEventListener("fetch", async (event) => {
  const data = await event.getRemoteManifest();
  console.log("Dweb Service Worker fetch!", data);
  const url = new URL(event.request.url);
  if (url.pathname.endsWith("/say/hi")) {
    const hiMessage = url.searchParams.get("message");
    console.log(`收到:${hiMessage}`);
    console.log("body=>", await event.request.text());
    // 发送消息回去
    return event.respondWith(`我是plaoc-html-demo 我接收到了消息`);
  }
  return event.respondWith("Not match any routes");
});

const restart = () => {
  dwebServiceWorker.restart();
};

const setLang = async () => {
  const res = await configPlugin.setLang("en", false);
  if (res) {
    dwebServiceWorker.restart();
  }
  console.log("res=>", res);
};

Object.assign(globalThis, {
  setLang,
  sayHi,
  runPlugins,
  restart,
  dwebServiceWorker,
});
