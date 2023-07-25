import { JsonlinesStream } from "../../../helper/JsonlinesStream.ts";
import { streamRead } from "../../../helper/readableStreamHelper.ts";
import type { $InstallProgressInfo } from "../jmm.api.serve.ts";
import type { $JmmAppInstallManifest } from "../types.ts";

// const elBack = document.querySelector('.top-bar-container')
const elIcon = document.querySelector<HTMLElement>(".icon-container")!;
const elMainTitle = document.querySelector<HTMLElement>(".title")!;
const elMainExplain = document.querySelector<HTMLElement>(".explain")!;
const elBtnDownload = document.querySelector<HTMLButtonElement>(".btn-download")!;
const elBtnDownloadMask = document.querySelector<HTMLElement>(".btn-download-mask")!;
const elBtnDownloadText = document.querySelector<HTMLElement>(".btn-download-label")!;
const elPreviewImgeContainer = document.querySelector<HTMLElement>(".img-container")!;
const elDetailtext = document.querySelector<HTMLElement>(".detail-text")!;
const elBtnMore = document.querySelector<HTMLButtonElement>(".detail-button-more")!;
const elMoreDeveloperInfo = document.querySelector<HTMLElement>(".developer-container")!;
const elDeveloperName = document.querySelector<HTMLElement>(".developer-name")!;
const enum DOWNLOAD_STATUS {
  INIT = -1,
  PAUSE = 0,
  PROGRESS = 1,
  DONE = 2,
}
let downloadState = DOWNLOAD_STATUS.INIT; // -1 没有在下载 0 下载暂停中
let appInfo: $JmmAppInstallManifest;
let fromUrl: string;

// 根据获取到的 appInfo 设置内容
async function setAppInfoByAppInfo(metadata: $JmmAppInstallManifest, metadataUrl: string) {
  appInfo = typeof metadata === "object" ? metadata : JSON.parse(metadata);
  fromUrl = metadataUrl;
  elIcon.style.backgroundImage = `url(${JSON.stringify(metadata.icon)})`;
  elMainTitle.innerHTML = appInfo.name;
  elMainExplain.innerHTML = appInfo.short_name;
  elDetailtext.innerHTML = appInfo.description;
  elDeveloperName.innerText = appInfo.author[0];

  const imgs = appInfo.images.map((item) => {
    const img = document.createElement("img");
    img.classList.add("img-box");
    img.src = item;
    return img;
  });
  elPreviewImgeContainer.append(...imgs);
}

// 下载按钮事件处理器
elBtnDownload.addEventListener("click", async (e) => {
  const url = location.origin.replace("www.", "api.");
  if (downloadState === DOWNLOAD_STATUS.INIT) {
    progress(0);
    downloadState = DOWNLOAD_STATUS.PROGRESS;
    // 通过返回一个 stream 实现 长连接
    const api_origin = location.origin.replace("www.", "api.");
    const install_url = `${api_origin}/app/install`;
    /// 将下载链接进行补全
    if ((appInfo.bundle_url.startsWith("http:") || appInfo.bundle_url.startsWith("https:")) === false) {
      appInfo.bundle_url = new URL(appInfo.bundle_url, fromUrl).href;
    }
    const res = await fetch(install_url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(appInfo),
    });

    const stream = res.body!;
    const installProgressStream = stream
      .pipeThrough(new TextDecoderStream())
      .pipeThrough(new JsonlinesStream<$InstallProgressInfo>());

    for await (const info of streamRead(installProgressStream)) {
      progress(+(info.progress * 100).toFixed(2));
      if (info.error) {
        alert(info.error);
        downloadState = DOWNLOAD_STATUS.INIT;
        elBtnDownloadText.innerText = "重试";
        return;
      }
      if (info.state === "install") {
        elBtnDownloadText.innerText = "安装中";
      } else if (info.state === "download") {
        elBtnDownloadText.innerText = "下载中";
      }
    }

    downloadState = DOWNLOAD_STATUS.DONE;
    elBtnDownloadText.innerText = "打开";
    return;
  }

  if (downloadState === DOWNLOAD_STATUS.PAUSE) {
    console.error("暂停中但是还没有处理");
  }

  if (downloadState === DOWNLOAD_STATUS.PROGRESS) {
    console.error("下载中但是还没处理");
  }

  if (downloadState === DOWNLOAD_STATUS.DONE) {
    // 然后打开指定的应用
    const mmid = appInfo.id;
    const res = await fetch(`${url}/app/open?mmid=${mmid}`);
    if (res.ok) {
      closeSelf();
    }
    return;
  }
});

elBtnMore.addEventListener("click", (e) => {
  const has = elDetailtext.classList.contains("detail-text-open");
  if (has) {
    elDetailtext.classList.remove("detail-text-open");
    elBtnMore.classList.remove("detail-button-more-open");
    elBtnMore.innerText = "更多";
    return;
  }
  elDetailtext.classList.add("detail-text-open");
  elBtnMore.classList.add("detail-button-more-open");
  elBtnMore.innerText = "收起";
});

elMoreDeveloperInfo.addEventListener("click", (e) => {
  console.log("developerContainer");
});

/**
 * 设置 下载按钮的背景色
 */
function setDownloadBackground(percent: number) {
  const backgroundStyle = `linear-gradient(90deg, #1677ff ${percent}%, #999)`;
  elBtnDownloadMask.style.background = backgroundStyle;
}

function setDownloadText(percent: number) {
  elBtnDownloadText.innerText = (percent == 100 ? "更新" : percent).toString();
}

function progress(percent: number) {
  setDownloadBackground(percent);
  setDownloadText(percent);
}

Object.assign(globalThis, {
  __app_upgrade_watcher_kit__: {
    _listeners: {
      progress: [progress],
    },
  },
});

/**
 * 关闭当前APP
 */
function closeSelf() {
  const url = `${getApiOrigin()}/close/self`;
  fetch(url);
}

function getApiOrigin() {
  return location.origin.replace("www.", "api.");
}

(async () => {
  const search = new URLSearchParams(location.search);
  const metadataUrl = search.get("metadataUrl");
  if (!metadataUrl) {
    throw new Error("miss params: metadataUrl");
  }
  const response = await fetch(metadataUrl);
  setAppInfoByAppInfo(await response.json(), metadataUrl);
})();
