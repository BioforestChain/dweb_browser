import fs from "node:fs";
import fsPromises from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import { Readable } from "node:stream";
import JSZip from "npm:jszip";
import mime from "npm:mime";
import request from "request";
import progress from "request-progress";
import tar from "tar";
import {
  Ipc,
  IpcHeaders,
  IpcRequest,
  IpcResponse,
} from "../../core/ipc/index.ts";
import { nativeFetchAdaptersManager } from "../dns/nativeFetch.ts";
import { createHttpDwebServer } from "../http-server/$createHttpDwebServer.ts";
import type { $State, JmmNMM } from "./jmm.ts";
import { JmmMetadata } from "./JmmMetadata.ts";
import { JsMicroModule } from "./micro-module.js.ts";

export const JMM_APPS_PATH = path.join(Electron.app.getAppPath(), "apps");
fs.mkdirSync(JMM_APPS_PATH, { recursive: true });

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
    case "/get_data":
      return getData.bind(this)(request, ipc);
    case "/app/download":
      return appDownload.bind(this)(request, ipc);
    case "/close/self":
      return appCloseSelf.bind(this)(request, ipc);
    case "/app/open":
      return appOpen.bind(this)(request, ipc);
    default: {
      throw new Error(`${this.mmid} 有没有处理的pathname === ${path}`);
    }
  }
}

async function getData(this: JmmNMM, request: IpcRequest, ipc: Ipc) {
  const searchParams = request.parsed_url.searchParams;
  const url = searchParams.get("url");
  if (url === null) throw new Error(`${this.mmid} url === null`);
  console.log("getData:", url);
  const res = await fetch(url);
  ipc.postMessage(
    await IpcResponse.fromResponse(request.req_id, res, ipc, true)
  );
}

async function appDownload(this: JmmNMM, ipcRequest: IpcRequest, ipc: Ipc) {
  const search = ipcRequest.parsed_url.searchParams;
  const downloadUrl = search.get("url");
  const id = search.get("id");
  if (downloadUrl === null) throw new Error(`downloadUrl === null`);
  if (id === null) throw new Error(`id === null`);
  this.downloadStream = new ReadableStream({
    start: (_controller) => {
      this.donwloadStramController = _controller;
    },
    cancel: (_resone) => {
      this.donwloadStramController?.close();
    },
    pull: (_controller) => {
      // eventTarget.dispatchEvent(new Event('pull'))
    },
  });

  ipc.postMessage(
    await IpcResponse.fromStream(
      ipcRequest.req_id,
      200,
      new IpcHeaders(),
      this.downloadStream,
      ipc
    )
  );

  const tempFilePath = path.join(JMM_TMP_DIR, path.basename(downloadUrl));
  fs.mkdirSync(path.dirname(tempFilePath), { recursive: true });

  const writeAblestream = fs.createWriteStream(tempFilePath, { flags: "w" });
  writeAblestream.on("close", () =>
    _extract.bind(this)(id, tempFilePath, ipcRequest, ipc)
  );
  // const downloadTask = await fetch(downloadUrl)
  // const totalSize = +(downloadTask.headers.get("content-length")??Infinity);
  // const downloadStream = downloadTask.body!

  progress(request(downloadUrl), {})
    .on("progress", onProgress.bind(this, ipcRequest, ipc))
    .on("error", (err: Error) => {
      throw err;
    })
    .pipe(writeAblestream);
}

function onProgress(
  this: JmmNMM,
  _ipcRequest: IpcRequest,
  _ipc: Ipc,
  state: $State
) {
  // // 测试关闭下载
  // if(this.downloadStream){
  //   this.donwloadStramController?.close()
  // }
  // 测试关闭下载
  const value = (state.percent * 100).toFixed(2);
  const ui8 = new TextEncoder().encode(`${value}\n`);
  this.donwloadStramController?.enqueue(ui8);
}

/**
 * 解压安装包
 */
async function _extract(
  this: JmmNMM,
  _id: string,
  tempFilePath: string,
  _request: IpcRequest,
  _ipc: Ipc
) {
  const outputDir = path.join(JMM_APPS_PATH, _id);
  // 判断 target 目录是否存在 不存在就创建目录
  if (!fs.existsSync(outputDir)) {
    await fsPromises.mkdir(outputDir, {
      recursive: true,
    });
  }
  if (tempFilePath.endsWith(".zip")) {
    const jszip = await JSZip.loadAsync(
      // fs.createReadStream(tempFilePath)
      fs.readFileSync(tempFilePath)
    );
    for (const [filePath, fileZipObj] of Object.entries(jszip.files)) {
      console.log(filePath);
      const targetFilePath = path.join(outputDir, filePath);
      if (fileZipObj.dir) {
        fs.mkdirSync(targetFilePath, { recursive: true });
      } else {
        fileZipObj
          .nodeStream()
          .pipe(fs.createWriteStream(targetFilePath) as NodeJS.WritableStream);
      }
    }
  } else {
    tar.x({
      cwd: outputDir,
      file: tempFilePath,
      sync: true,
    });
  }
  await fsPromises.unlink(tempFilePath);
  this.donwloadStramController?.enqueue(new TextEncoder().encode(`100\n`));
  this.donwloadStramController?.close();

  /// 下载完成，开始安装
  const config = await (
    await this.nativeFetch(`file:///jmm/${_id}/usr/metadata.json`)
  ).json();
  const metadata = new JmmMetadata(config);
  const jmm = new JsMicroModule(metadata);
  this.context!.dns.install(jmm);
}

async function appCloseSelf(this: JmmNMM, ipcRequest: IpcRequest, ipc: Ipc) {
  const referer = ipcRequest.headers.get("referer");
  if (referer === null) throw new Error(`${this.mmid} referer === null`);
  const host = new URL(referer).host;
  const res = await this.nativeFetch(
    `file://mwebview.sys.dweb/destroy_webview_by_host?host=${host}`
  );
  ipc.postMessage(
    await IpcResponse.fromResponse(ipcRequest.req_id, res, ipc, true)
  );
}

async function appOpen(this: JmmNMM, request: IpcRequest, ipc: Ipc) {
  const id = request.parsed_url.searchParams.get("mmid") as $MMID;
  this.context?.dns.connect(id);
}

nativeFetchAdaptersManager.append((remote, parsedUrl) => {
  /// fetch("file:///sys") 匹配
  console.log("[fetch in jmm] ", parsedUrl.href, remote.mmid);

  if (parsedUrl.protocol === "file:" && parsedUrl.hostname === "") {
    if (
      parsedUrl.pathname.startsWith("/jmm/") &&
      remote.mmid === "jmm.sys.dweb" /// 只能自己访问
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
  console.log("read-file:", filepath);
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
