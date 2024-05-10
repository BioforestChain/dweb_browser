import { ImpactStyle, NotificationType, barcodeScannerPlugin, configPlugin, dwebServiceWorker } from "@plaoc/plugins";
const barcodeScanner = document.querySelector("dweb-barcode-scanning")!;
const handleSubmit = async ($event: Event) => {
  $event.preventDefault();

  const target = document.getElementById("fileToUpload") as HTMLInputElement;
  if (target && target.files?.[0]) {
    const img = target.files[0];
    alert(await barcodeScannerPlugin.process(img));
  }
};

const startScanning = () => {
  barcodeScanner.startScanning();
};

const haptics = document.querySelector("dweb-haptics")!;

const _impactLight = async () => {
  await haptics.impactLight({ style: ImpactStyle.Heavy });
};
const _notification = async () => {
  await haptics.notification({ type: NotificationType.Success });
};

const share = document.querySelector("dweb-share")!;
// 分享
const shareHandle = async ($event: { preventDefault: () => void }) => {
  $event.preventDefault();
  const target = document.getElementById("$shareHandle") as HTMLInputElement;
  if (target && target.files?.[0]) {
    return await share.share({
      title: `分享:${target.files[0].name}`,
      text: `size:${target.files[0].size},type:${target.files[0].type}`,
      files: target.files,
    });
  }
  return await share.share({
    title: "分享标题🍉",
    text: "分享文字分享文字",
    url: "https://gpt.waterbang.top",
    files: undefined,
  });
};

const device = document.querySelector("dweb-device")!;
const getUUID = async () => {
  console.log(await device.getUUID());
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

const canOpenUrl = async () => {
  const res = await dwebServiceWorker.canOpenUrl(`game.dweb.waterbang.top.dweb`);
  console.log("canOpenUrl=>", res);
};

dwebServiceWorker.addEventListener("fetch", async (event) => {
  const data = await event.getRemoteManifest();
  console.log("Dweb Service Worker fetch!", data);
  const url = new URL(event.request.url);
  if (url.pathname.endsWith("/say/hi")) {
    const hiMessage = url.searchParams.get("message");
    console.log(`收到:${hiMessage}`);
    alert(hiMessage);
    console.log("body=>", await event.request.text());
    // 发送消息回去
    return event.respondWith(`plaoc-html-demo/echo:${hiMessage}`);
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
  canOpenUrl,
  getUUID,
  restart,
  shareHandle,
  open,
  handleSubmit,
  startScanning,
  dwebServiceWorker,
});
