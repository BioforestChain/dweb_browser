import fs from "node:fs";
import fsPromises from "node:fs/promises";
import path from "node:path";
import request from "request";
import progress from "request-progress";
import extract from "extract-zip";
import tar from "tar";
import {
  Ipc,
  IpcHeaders,
  IpcRequest,
  IpcResponse,
} from "../../core/ipc/index.ts";
import { createHttpDwebServer } from "../http-server/$createHttpDwebServer.ts";
import type { $AppMetaData, $State, JmmNMM } from "./jmm.ts";

export async function createApiServer(this: JmmNMM) {
  // 为 下载页面做 准备
  this.apiServer = await createHttpDwebServer(this, {
    subdomain: "api",
    port: 6363,
  });
  const streamIpc = await this.apiServer.listen();
  streamIpc.onRequest(onRequest.bind(this));
}

async function onRequest(this: JmmNMM, request: IpcRequest, ipc: Ipc) {
  const path = request.parsed_url.pathname;
  switch (request.parsed_url.pathname) {
    case "/get_data":
      return getData.bind(this)(request, ipc);
    case "/app/download":
      return appDownload.bind(this)(request, ipc);
    case "/close/self":
      return appCloseSelf.bind(this)(request, ipc);
    default: {
      throw new Error(
        `${this.mmid} 有没有处理的pathname === ${request.parsed_url.pathname}`
      );
      debugger;
    }
  }
}

async function getData(this: JmmNMM, request: IpcRequest, ipc: Ipc) {
  const searchParams = request.parsed_url.searchParams;
  const url = searchParams.get("url");
  if (url === null) throw new Error(`${this.mmid} url === null`);
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
    cancel: (resone) => {
      this.donwloadStramController?.close();
    },
    pull: (controller) => {
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
  const tempPath = path.resolve(process.cwd(), `./temp/${id}.tar.gz`);
  const writeAblestream = fs.createWriteStream(tempPath, { flags: "w" });
  writeAblestream.on("close", () =>
    _extract.bind(this)(id, tempPath, ipcRequest, ipc)
  );
  progress(request(downloadUrl), {})
    .on("progress", onProgress.bind(this, ipcRequest, ipc))
    .on("error", (err: Error) => {
      throw err;
    })
    .pipe(writeAblestream);
}

async function onProgress(
  this: JmmNMM,
  ipcRequest: IpcRequest,
  ipc: Ipc,
  state: $State
) {
  // // 测试关闭下载
  // if(this.downloadStream){
  //   this.donwloadStramController?.close()
  // }
  // 测试关闭下载
  const value = (state.percent * 100).toFixed(0);
  const ui8 = new TextEncoder().encode(`${value}\n`);
  this.donwloadStramController?.enqueue(ui8);
}

async function _extract(
  this: JmmNMM,
  id: string,
  tempPath: string,
  request: IpcRequest,
  ipc: Ipc
) {
  const target = path.resolve(process.cwd(), `./apps`);
  // 判断 target 目录是否存在 不存在就创建目录
  if (!fs.existsSync(target)) {
    await fsPromises.mkdir(target, {
      recersive: true,
    });
  }

  tar.x({
    cwd: target,
    file: tempPath,
    sync: true,
  });
  await fsPromises.unlink(tempPath);
  this.donwloadStramController?.enqueue(new TextEncoder().encode(`100\n`));
  this.donwloadStramController?.close();
}

async function appCloseSelf(this: JmmNMM, ipcRequest: IpcRequest, ipc: Ipc) {
  const headers = ipcRequest.headers;
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
