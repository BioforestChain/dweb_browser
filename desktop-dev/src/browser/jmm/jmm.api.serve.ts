import Nedb from "@seald-io/nedb";
import JSZip from "jszip";
import mime from "mime";
import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { Readable } from "node:stream";
import tar from "tar";
import {
  Ipc,
  IpcEvent,
  IpcHeaders,
  IpcRequest,
  IpcResponse,
} from "../../core/ipc/index.ts";
import { simpleEncoder } from "../../helper/encoding.ts";
import { locks } from "../../helper/locksManager.ts";
import { ReadableStreamOut } from "../../helper/readableStreamHelper.ts";
import { $MMID } from "../../helper/types.ts";
import { nativeFetchAdaptersManager } from "../../sys/dns/nativeFetch.ts";
import { createHttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.ts";
import type { $AppMetaData, JmmNMM } from "./jmm.ts";
import { JsMMMetadata, JsMicroModule } from "./micro-module.js.ts";

export const JMM_APPS_PATH = path.join(
  // Electron.app.getPath("userData"),
  // Electron.app.getName(),
  Electron.app.getAppPath(),
  "jmm-apps"
);
fs.mkdirSync(JMM_APPS_PATH, { recursive: true });

const JMM_DB_PATH = path.join(JMM_APPS_PATH, ".db");
/**
 * @type {import("@seald-io/nedb").Nedb<$AppMetaData>}
 */
// @ts-ignore: this is commonjs
export const JMM_DB = new Nedb<$AppMetaData>({
  filename: JMM_DB_PATH,
  autoload: true,
});
JMM_DB.ensureIndexAsync({ fieldName: ["id"], unique: true });
// open<$AppMetaData, $MMID>({
//   path: path.join(JMM_APPS_PATH, "jmm"),
//   encoding: "json",
// });

export const JMM_TMP_DIR = path.join(os.tmpdir(), "jmm");
fs.mkdirSync(JMM_TMP_DIR, { recursive: true });

export async function createApiServer(this: JmmNMM) {
  // 为 下载页面做 准备
  this.apiServer = await createHttpDwebServer(this, {
    subdomain: "api",
    port: 6363,
  });
  const streamIpc = await this.apiServer.listen();
  streamIpc.onRequest(onRequest.bind(this));
}

function onRequest(this: JmmNMM, request: IpcRequest, ipc: Ipc) {
  const path = request.parsed_url.pathname;
  switch (path) {
    case "/app/install":
      return appInstall.call(this, request, ipc);
    case "/close/self":
      return appCloseSelf.call(this, request, ipc);
    case "/app/open":
      return appOpen.call(this, request, ipc);
    default: {
      throw new Error(`${this.mmid} 有没有处理的pathname === ${path}`);
    }
  }
}

export type $InstallProgressInfo = {
  state: "download" | "install";
  progress: number;
  total: number;
  current: number;
  done: boolean;
  error?: string;
};

/**
 * 应用程序安装, 安全锁
 * @param this
 * @param ipcRequest
 * @param ipc
 */
async function appInstall(this: JmmNMM, ipcRequest: IpcRequest, ipc: Ipc) {
  const appInfo: $AppMetaData = JSON.parse(await ipcRequest.body.text());
  await locks.request(`jmm-install:${appInfo.id}`, () =>
    _appInstall.call(this, appInfo, ipcRequest, ipc)
  );
}
/**
 * 应用程序安装的核心逻辑
 * @param this
 * @param ipcRequest
 * @param ipc
 * @returns
 */
async function _appInstall(
  this: JmmNMM,
  appInfo: $AppMetaData,
  ipcRequest: IpcRequest,
  ipc: Ipc
) {
  const tempFilePath = path.join(JMM_TMP_DIR, `${appInfo.id}.jmmbundle`);
  const hashFilePath = path.join(JMM_TMP_DIR, `${appInfo.id}.hash`);
  fs.mkdirSync(JMM_TMP_DIR, { recursive: true });

  let bundleWriter: fs.WriteStream;
  let downloadHeaders: undefined | HeadersInit;
  let bundleWritedSize = 0;

  /// 如果文件存在、且hash不变、并且支持断点续传，那么将会尝试使用追加写入的方式
  if (
    fs.existsSync(tempFilePath) &&
    fs.existsSync(hashFilePath) &&
    fs.readFileSync(hashFilePath, "utf-8") === appInfo.bundle_hash &&
    ipcRequest.headers.get("Accept-Ranges") === "bytes"
  ) {
    bundleWritedSize = fs.statSync(tempFilePath).size;
    downloadHeaders = {
      "Content-Range": `bytes ${bundleWritedSize}-`,
    };
  }

  const downloadTask = await this.nativeFetch(appInfo.bundle_url, {
    headers: downloadHeaders,
  }).ok();
  /// 再次确认这个下载是支持 Range:bytes 的
  if (
    downloadTask.headers.get("Accept-Ranges") === "bytes" &&
    bundleWritedSize !== 0
  ) {
    bundleWriter = fs.createWriteStream(tempFilePath, { flags: "a" });
  } else {
    bundleWritedSize = 0;
    bundleWriter = fs.createWriteStream(tempFilePath, { flags: "w" });
  }
  const totalLen = parseInt(
    /**
     * Content-Length: 2
     * Accept-Ranges: bytes
     * Content-Range: bytes 0-1/4300047
     */
    downloadTask.headers.get("Content-Range")?.split("/").pop() ??
      /**
       * Content-Length: 4300047
       */
      downloadTask.headers.get("Content-Length") ??
      appInfo.bundle_size + ""
  );
  /**
   * 下载进度的响应流
   */
  const downloadProgressStreamOut = new ReadableStreamOut<Uint8Array>();
  const enqueueInstallProgress = (
    state: $InstallProgressInfo["state"],
    chunkByteLength: number,
    done = false,
    error?: string
  ) => {
    bundleWritedSize += chunkByteLength;
    downloadProgressStreamOut.controller.enqueue(
      simpleEncoder(
        JSON.stringify({
          state,
          progress: bundleWritedSize / totalLen,
          total: totalLen,
          current: bundleWritedSize,
          done,
          error,
        } satisfies $InstallProgressInfo) + "\n",
        "utf8"
      )
    );
    if (done) {
      downloadProgressStreamOut.controller.close();
    }
  };
  enqueueInstallProgress("download", 0);

  /// 准备完毕，开始响应 install-app 这个任务，返回JSONLine格式的数据
  ipc.postMessage(
    await IpcResponse.fromStream(
      ipcRequest.req_id,
      200,
      new IpcHeaders(),
      downloadProgressStreamOut.stream,
      ipc
    )
  );

  /// 等待下载完成
  await downloadTask.body!.pipeTo(
    new WritableStream({
      start: () => {
        /// 下载开始，就写入hash
        fs.writeFileSync(hashFilePath, appInfo.bundle_hash);
      },
      write: (chunk, controller) => {
        /// 流量进度监控
        enqueueInstallProgress("download", chunk.byteLength);
        /// 写入文件保存
        bundleWriter.write(chunk);
      },
      close: () => {
        bundleWriter.close();
      },
      abort: (err) => {
        bundleWriter.close();
      },
    })
  );
  /// 将下载完成的文件进行hash校验
  const hashVerifyer = crypto.createHash("sha256");
  for await (const chunk of fs.createReadStream(tempFilePath)) {
    hashVerifyer.update(chunk);
  }
  const bundle_hash = "sha256:" + hashVerifyer.digest("hex");
  /// hash 校验失败，删除下载的文件，并且结束安装任务
  if (bundle_hash !== appInfo.bundle_hash) {
    console.log("hash 校验失败");
    /// 移除文件
    fs.rmSync(tempFilePath);
    fs.rmSync(hashFilePath);
    return enqueueInstallProgress(
      "download",
      0,
      true,
      `hash verifiy failed, actua:${bundle_hash} expect:${appInfo.bundle_hash}`
    );
  }

  console.log("hash 校验通过，开始解压安装");

  /// 开始解压文件
  enqueueInstallProgress("install", 0);
  /**
   * 安装的目录
   */
  const installDir = path.join(JMM_APPS_PATH, appInfo.id);
  /// 判断 target 目录是否存在 不存在就创建目录
  if (!fs.existsSync(installDir)) {
    fs.mkdirSync(installDir, { recursive: true });
  }
  /// 如果存在，就要清空目录
  else {
    fs.rmSync(installDir, { recursive: true });
  }

  const bundleMime = downloadTask.headers.get("Content-Type");
  if (bundleMime === "application/zip") {
    // const jszip = await JSZip.loadAsync(fs.createReadStream(tempFilePath));
    const jszip = await JSZip.loadAsync(fs.readFileSync(tempFilePath));
    for (const [filePath, fileZipObj] of Object.entries(jszip.files)) {
      const targetFilePath = path.join(
        installDir,
        filePath.slice(appInfo.id.length)
      );
      if (fileZipObj.dir) {
        fs.mkdirSync(targetFilePath, { recursive: true });
      } else {
        fileZipObj
          .nodeStream()
          .pipe(fs.createWriteStream(targetFilePath) as NodeJS.WritableStream);
      }
    }
  } else if (bundleMime === "application/x-tar") {
    tar.x({
      cwd: installDir,
      file: tempFilePath,
      sync: true,
    });
  } else {
    return enqueueInstallProgress(
      "install",
      0,
      true,
      `invalid bundle-mime-type: ${bundleMime}`
    );
  }
  await fs.unlinkSync(tempFilePath);
  await fs.unlinkSync(hashFilePath);

  /// 下载完成，开始安装
  const result = await JMM_DB.updateAsync({ id: appInfo.id }, appInfo, {
    upsert: true,
    returnUpdatedDocs: false,
  });
  console.log("update result", result);
  if (result.numAffected === 0) {
    return enqueueInstallProgress(
      "install",
      0,
      true,
      `fail to save app info to database: ${appInfo.id}`
    );
  }

  const metadata = new JsMMMetadata(appInfo);
  const jmm = new JsMicroModule(metadata);
  this.context!.dns.install(jmm);
  // 同步给 broser.dweb
  this.nativeFetch(`file://browser.dweb/apps_info/updated`)
  return enqueueInstallProgress("install", 0, true);
}

/**
 * 获取所有已经安装的应用
 * @returns
 */
export async function getAllApps() {
  const apps = await JMM_DB.getAllData();
  return apps as $AppMetaData[];
}

async function appCloseSelf(this: JmmNMM, ipcRequest: IpcRequest, ipc: Ipc) {
  const referer = ipcRequest.headers.get("referer");
  if (referer === null) throw new Error(`${this.mmid} referer === null`);
  const host = new URL(referer).host;
  const res = await this.nativeFetch(
    `file://mwebview.browser.dweb/destroy_webview_by_host?host=${host}`
  );
  ipc.postMessage(
    await IpcResponse.fromResponse(ipcRequest.req_id, res, ipc, true)
  );
}

async function appOpen(this: JmmNMM, request: IpcRequest, ipc: Ipc) {
  const id = request.parsed_url.searchParams.get("mmid") as $MMID;
  const [opendAppIpc] = await this.context!.dns.connect(id);
  opendAppIpc.postMessage(IpcEvent.fromText("activity", ""));
}

nativeFetchAdaptersManager.append((remote, parsedUrl) => {
  /// fetch("file:///jmm/") 匹配
  if (parsedUrl.protocol === "file:" && parsedUrl.hostname === "") {
    if (
      parsedUrl.pathname.startsWith("/jmm/") &&
      remote.mmid === "jmm.browser.dweb" /// 只能自己访问
    ) {
      const filepath = path.join(
        JMM_APPS_PATH,
        parsedUrl.pathname.replace("/jmm/", "/")
      );
      return resFile(filepath);
    } else if (parsedUrl.pathname.startsWith("/usr/")) {
      const filepath = path.join(
        JMM_APPS_PATH,
        remote.mmid,
        parsedUrl.pathname
      );
      return resFile(filepath);
    }
  }
}, 0);
const resFile = (filepath: string) => {
  try {
    const stats = fs.statSync(filepath);
    if (stats.isDirectory()) {
      throw stats;
    }
    const ext = path.extname(filepath);
    return new Response(Readable.toWeb(fs.createReadStream(filepath)), {
      status: 200,
      headers: {
        "Content-Length": stats.size + "",
        "Content-Type": mime.getType(ext) || "application/octet-stream",
      },
    });
  } catch (err) {
    console.error(err);
    return new Response(String(err), { status: 404 });
  }
};
