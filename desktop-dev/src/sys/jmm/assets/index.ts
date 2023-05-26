// const elBack = document.querySelector('.top-bar-container')
const decoder = new TextDecoder();
const elIcon = document.querySelector<HTMLElement>(".icon-container")!;
const elMainTitle = document.querySelector<HTMLElement>(".title")!;
const elMainExplain = document.querySelector<HTMLElement>(".explain")!;
const elBtnDownload =
  document.querySelector<HTMLButtonElement>(".btn-download")!;
const elBtnDownloadMask =
  document.querySelector<HTMLElement>(".btn-download-mask")!;
const elBtnDownloadText = document.querySelector<HTMLElement>(
  ".btn-download-label"
)!;
const elPreviewImgeContainer =
  document.querySelector<HTMLElement>(".img-container")!;
const elDetailtext = document.querySelector<HTMLElement>(".detail-text")!;
const elBtnMore = document.querySelector<HTMLButtonElement>(
  ".detail-button-more"
)!;
const elMoreDeveloperInfo = document.querySelector<HTMLElement>(
  ".developer-container"
)!;
const elDeveloperName = document.querySelector<HTMLElement>(".developer-name")!;
const enum DOWNLOAD_STATUS {
  INIT = -1,
  PAUSE = 0,
  PROGRESS = 1,
  DONE = 2,
}
let downloadState = DOWNLOAD_STATUS.INIT; // -1 没有在下载 0 下载暂停中

type $AppMetaData = import("../jmm.ts").$AppMetaData;
declare global {
  var appInfo: $AppMetaData;
}

// 根据获取到的 appInfo 设置内容
async function setAppInfoByAppInfo(info: any) {
  const appInfo: $AppMetaData =
    typeof info === "object" ? info : JSON.parse(info);
  window.appInfo = appInfo;
  elIcon.style.backgroundImage =
    "url('https://www.bfmeta.info/imgs/logo3.webp')";
  elMainTitle.innerHTML = appInfo.title;
  elMainExplain.innerHTML = appInfo.subtitle;
  elDetailtext.innerHTML = appInfo.introduction;
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
    const url = location.origin.replace("www.", "api.");
    const res = await fetch(
      `${url}/app/download?url=${window.appInfo.downloadUrl}&id=${window.appInfo.id}`
    );

    const stream = res.body!;
    const reader = stream.getReader();

    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      console.log("done: ", done);
      let percent = decoder.decode(value);
      progress(parseInt(percent));
    }
    reader.releaseLock();
    stream.cancel("done");
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
    // closeSelf();
    // 然后打开指定的应用
    const mmid = window.appInfo.id;
    const root =
      "file:///apps/" + window.appInfo.id + window.appInfo.server.root;
    const entry = window.appInfo.server.entry;
    const res = await fetch(
      `${url}/app/open?mmid=${mmid}&root=${root}&entry=${entry}`
    );
    return;
  }

  //

  // 因为 注入改写了 fetch
  // setDownloadBackground(0)
  // setDownloadText(0)
  // nativeFetch(fetchUrl, { headers: {origin: location.origin}})
  // .then(
  //   res => console.log('res', res),
  //   err => console.error(err)
  // )
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

window.__app_upgrade_watcher_kit__ = {
  _listeners: {
    progress: [progress],
  },
};

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

////

(async () => {
  const search = new URLSearchParams(location.search);
  const metadataUrl = search.get("metadataUrl");
  console.log("metadataUrl: ", metadataUrl);
  const url = getApiOrigin();
  const response = await fetch(`${url}/get_data?url=${metadataUrl}`);
  setAppInfoByAppInfo(await response.json());
})();

// 测试开启
// setTimeout(() => {
//   const url = `http://api.browser.sys.dweb-443.localhost:22605/status-bar.nativeui.sys.dweb/setState?X-Dweb-Host=api.browser.sys.dweb%3A443&color=%7B%22red%22%3A204%2C%22green%22%3A30%2C%22blue%22%3A30%2C%22alpha%22%3A255%7D`
//   fetch(url)
//   .then(res => {
//     setTimeout(() => {
//       closeSelf()
//     },1000)
//   })
// },1000)
