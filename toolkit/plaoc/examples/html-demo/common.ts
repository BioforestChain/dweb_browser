import { ImpactStyle, NotificationType, barcodeScannerPlugin, configPlugin, dwebServiceWorker } from "@plaoc/plugins";
const barcodeScanner = document.querySelector("dweb-barcode-scanning")!;

const close = new CloseWatcher();

close.addEventListener("close", () => {
  alert("拦截到了回退时间");
});

const observerImgUpdate = async ($event: Event) => {
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

const sayHiMessage = document.getElementById("sayHi-message") as HTMLSpanElement;
// 向desktop.dweb.waterbang.top.dweb 发送消息
const sayHi = (message = "今晚吃螃🦀️蟹吗？") => {
  const input = document.getElementById("input1") as HTMLInputElement;
  const data = input.value;
  if (data) {
    message = data;
  }
  dwebServiceWorker
    .fetch(`https://plugins.example.com.dweb/say/hi?message=${message}&activate=true`)
    .then(async (res) => {
      const message = await res.text();
      console.log("收到回应消息=> ", message);
      sayHiMessage.innerText = message;
    })
    .catch((err) => {
      console.log("收到回应报错=> ", err);
      sayHiMessage.innerText = err;
    });
};

const canOpenUrl = async () => {
  const res = await dwebServiceWorker.has(`plugins.example.com.dweb`);
  sayHiMessage.innerText = `存在app吗=>${res}`;
};

dwebServiceWorker.addEventListener("fetch", async (event) => {
  const data = await event.getRemoteManifest();
  console.log("Dweb Service Worker fetch!", data);
  const url = new URL(event.request.url);
  if (url.pathname.endsWith("/say/hi")) {
    const hiMessage = url.searchParams.get("message");
    console.log(`收到:${hiMessage}`);
    // alert(hiMessage);
    console.log("body=>", await event.request.text());
    // 发送消息回去
    return event.respondWith(`plaoc-html-demo/echo:${hiMessage}`);
  }
  return event.respondWith(`Not match any routes:${url.pathname}`);
});

const restart = () => {
  dwebServiceWorker.restart();
};

const setLang = async () => {
  const res = await configPlugin.setLang("en");
  console.log("res=>", res);
};

Object.assign(globalThis, {
  sayHi,
  canOpenUrl,
  getUUID,
  restart,
  shareHandle,
  observerImgUpdate,
  startScanning,
  dwebServiceWorker,
  setLang,
});
