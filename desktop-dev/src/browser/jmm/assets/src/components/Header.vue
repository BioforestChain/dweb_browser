<script setup lang="ts">
import type { $InstallProgressInfo } from "&/jmm.api.serve.ts";
import type { $JmmAppInstallManifest } from "&/types.ts";
import { toJsonlinesStream } from "helper/stream/jsonlinesStreamHelper.ts";
import { streamRead } from "helper/stream/readableStreamHelper";
import { ref, watchEffect } from "vue";
const enum DOWNLOAD_STATUS {
  INIT = -1, // 初始状态
  PAUSE = 0, // 暂停
  PROGRESS = 1, // 正在下载
  DONE = 2, // 下载完成
}
const downloadState = ref(DOWNLOAD_STATUS.INIT); // -1 没有在下载 0 下载暂停中
const downloadText = ref("下载");
const props = defineProps({
  appInfo: {
    type: Object as () => $JmmAppInstallManifest,
    required: true,
  },
  metadataUrl: {
    type: String,
    required: true,
  },
});

watchEffect(async () => {
  if (props.appInfo.id && downloadState.value !== DOWNLOAD_STATUS.DONE) {
    await querySelf();
  }
});

async function querySelf() {
  const mmid = props.appInfo.id;
  const url = `${getApiOrigin()}/app/query?mmid=${mmid}`;
  const res = await fetch(url);
  const result: Boolean = await res.json();

  if (res.ok && false) {
    downloadState.value = DOWNLOAD_STATUS.DONE;
  }
}

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

const dowloadProcess = ref(0);

const update = async () => {
  downloadState.value = DOWNLOAD_STATUS.PROGRESS;
  // 通过返回一个 stream 实现 长连接
  const api_origin = location.origin.replace("www.", "api.");
  const install_url = `${api_origin}/app/install`;
  /// 将下载链接进行补全
  if ((props.appInfo.bundle_url.startsWith("http:") || props.appInfo.bundle_url.startsWith("https:")) === false) {
    props.appInfo.bundle_url = new URL(props.appInfo.bundle_url, props.metadataUrl).href;
  }
  const res = await fetch(install_url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(props.appInfo),
  });

  const stream = res.body!;
  const installProgressStream = toJsonlinesStream<$InstallProgressInfo>(stream);

  for await (const info of streamRead(installProgressStream)) {
    if (info.error) {
      downloadState.value = DOWNLOAD_STATUS.INIT;
      downloadText.value = "重试";
      return;
    }
    if (info.state === "download") {
      dowloadProcess.value = +(info.progress * 100).toFixed(2);
    }
  }
  downloadState.value = DOWNLOAD_STATUS.DONE;
  downloadText.value = "打开";
};
const openApp = async () => {
  const url = location.origin.replace("www.", "api.");
  // 然后打开指定的应用
  const mmid = props.appInfo.id;
  const res = await fetch(`${url}/app/open?mmid=${mmid}`);

  if (res.ok) {
    closeSelf();
  }
};
</script>

<template>
  <div class="header">
    <img class="icon" :src="props.appInfo.logo" :alt="props.appInfo.short_name" />
    <div class="text-container">
      <h1 class="title">{{ props.appInfo.short_name }}</h1>
      <p class="explain">{{ props.appInfo.name }}</p>
      <div class="button-group-container">
        <button class="btn-download">
          <div v-if="downloadState == DOWNLOAD_STATUS.INIT" class="btn-download-label" @click="update">
            {{ downloadText }}
          </div>
          <div v-else-if="downloadState == DOWNLOAD_STATUS.PROGRESS" class="btn-download-label" @click="update">
            {{ dowloadProcess }}
          </div>
          <div v-else-if="downloadState == DOWNLOAD_STATUS.DONE" class="btn-download-label" @click="openApp">打开</div>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.header {
  display: flex;
  flex-direction: row;
  padding: 20px 12px;
  width: auto;
  cursor: pointer;
}
.icon {
  flex-shrink: 0;
  width: 100px;
  height: 100px;
  background-size: 60%;
  background-position: center;
  background-repeat: no-repeat;
  box-shadow: 0px 0px 10px 0px rgba(0, 0, 0, 0.3);
  border-radius: 20px;
}
.text-container {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  margin-left: 15px;
  width: 100%;
  height: 90px;
}

.title {
  margin: 0px;
  width: 100%;
  font-size: 24px;
  line-height: 26px;
}

.explain {
  font-size: 14px;
  color: #888;
}
.button-group-container {
  display: flex;
  justify-content: space-between;
  width: 100%;
}

.btn-download {
  padding: 3px 20px;
  height: 30px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 900;
  letter-spacing: 2px;
  color: #fff;
  border: none;
  background: #ddd;
  cursor: pointer;
  background: linear-gradient(90deg, #1677ff 100%, #999);
}

.btn-download-label {
  z-index: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 14px;
  width: 100%;
  height: 100%;
}
</style>
