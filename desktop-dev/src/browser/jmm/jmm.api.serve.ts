import { blue, green, red } from "colors";
import JSZip from "jszip";
import crypto from "node:crypto";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { isDeepStrictEqual } from "node:util";
import tar from "tar";
import { FetchEvent, IpcEvent } from "../../core/ipc/index.ts";
import { $MMID } from "../../core/types.ts";
import { resolveToDataRoot } from "../../helper/createResolveTo.ts";
import { Store } from "../../helper/electronStore.ts";
import { simpleEncoder } from "../../helper/encoding.ts";
import { headersGetTotalLength } from "../../helper/httpHelper.ts";
import { locks } from "../../helper/locksManager.ts";
import { fetchMatch } from "../../helper/patternHelper.ts";
import { ReadableStreamOut } from "../../helper/stream/readableStreamHelper.ts";
import { createHttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
import type { JmmNMM } from "./jmm.ts";
import { JsMMMetadata, JsMicroModule } from "./micro-module.js.ts";
import { $JmmAppInstallManifest } from "./types.ts";

export const JMM_APPS_PATH = resolveToDataRoot("jmm-apps");
fs.mkdirSync(JMM_APPS_PATH, { recursive: true });

export class JmmDatabase extends Store<{
  apps: Map<$MMID, $JmmAppInstallManifest>;
}> {
  constructor() {
    super("jmm-apps");
  }
  private _apps = this.get("apps", () => new Map());
  private _save() {
    this.set("apps", this._apps);
  }
  async upsert(app: $JmmAppInstallManifest) {
    const oldApp = this._apps.get(app.id);
    if (isDeepStrictEqual(oldApp, app)) {
      return true;
    }

    this._apps.set(app.id, app);
    this._save();
    return true;
  }
  async find(mmid: $MMID) {
    return this._apps.get(mmid);
  }
  async remove(mmid: $MMID) {
    this._apps.delete(mmid);
    this._save();
    return true;
  }
  async all() {
    return [...this._apps.values()];
  }
}
export const JMM_DB = new JmmDatabase();
export const JMM_TMP_DIR = path.join(os.tmpdir(), "jmm");
fs.mkdirSync(JMM_TMP_DIR, { recursive: true });

export async function createApiServer(this: JmmNMM) {
  // 为 下载页面做 准备
  this.apiServer = await createHttpDwebServer(this, {
    subdomain: "api",
  });
  const serverIpc = await this.apiServer.listen();

  const onFetchMatcher = fetchMatch()
    .post("/app/install", async (event) => {
      const appInfo = await event.json<$JmmAppInstallManifest>();
      const res = await locks.request(`jmm-install:${appInfo.id}`, () => _appInstall.call(this, event, appInfo));
      /// 上锁
      return res;
    })
    .get("/close/self", async (event) => {
      return this.jmmServer?.close();
    })
    .get("/app/open", async (event) => {
      const id = event.searchParams.get("mmid") as $MMID;
      // 打开之前需要先关闭 否者更新后无法实现 更新打开 ？？
      await this.context?.dns.close(id);
      const connectResult = await this.context?.dns.connect(id);
      if (connectResult === undefined) {
        throw new Error(`${id} not found!`);
      }
      connectResult[0].postMessage(IpcEvent.fromText("renderer", ""));
      return Response.json(true);
    })
    .get("/app/query", async (event) => {
      const id = event.searchParams.get("mmid") as $MMID;
      if(await this.context?.dns.query(id)) {
        return Response.json(true);
      } else {
        return Response.json(false);
      }
    });
  serverIpc.onFetch(onFetchMatcher.run).internalServerError().cors();
}
/**
 * 应用程序安装的核心逻辑
 */
async function _appInstall(this: JmmNMM, event: FetchEvent, appInfo: $JmmAppInstallManifest) {
  const { ipcRequest, ipc } = event;

  //#region 准备工作

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
  if (downloadTask.headers.get("Accept-Ranges") === "bytes" && bundleWritedSize !== 0) {
    bundleWriter = fs.createWriteStream(tempFilePath, { flags: "a" });
  } else {
    bundleWritedSize = 0;
    bundleWriter = fs.createWriteStream(tempFilePath, { flags: "w" });
  }
  const totalLen = headersGetTotalLength(downloadTask.headers) ?? appInfo.bundle_size;
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
  //#endregion

  /// 准备完毕，开始正式执行下载与安装，同时响应 /install/app（返回JSONLine格式的数据）
  doDownloadAndInstall.call(this);
  return {
    body: downloadProgressStreamOut.stream,
  };

  async function doDownloadAndInstall(this: JmmNMM) {
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
      console.always("jmm serve", red("hash 校验失败"));
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

    console.always("jmm serve", blue("hash 校验通过，开始解压安装"));

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
      const jszip = await JSZip.loadAsync(fs.readFileSync(tempFilePath));

      for (const [filePath, fileZipObj] of Object.entries(jszip.files)) {
        const targetFilePath = path.join(installDir, filePath);
        if (fileZipObj.dir) {
          fs.mkdirSync(targetFilePath, { recursive: true });
        } else {
          fileZipObj.nodeStream().pipe(fs.createWriteStream(targetFilePath) as NodeJS.WritableStream);
        }
      }
    } else if (bundleMime === "application/x-tar") {
      tar.x({
        cwd: installDir,
        file: tempFilePath,
        sync: true,
      });
    } else {
      return enqueueInstallProgress("install", 0, true, `invalid bundle-mime-type: ${bundleMime}`);
    }
    // createSession
    const sessionFilePath = path.join(installDir, "/usr/sys/session.json");
    const nowDate = Date.now();
    if (fs.existsSync(sessionFilePath)) {
      const sessionInfo = fs.readFileSync(sessionFilePath).toJSON() as unknown as $SessionInfo;
      sessionInfo.upgradeTime = nowDate;
      fs.writeFileSync(sessionFilePath, JSON.stringify(sessionInfo));
    }
    {
      fs.mkdirSync(path.join(installDir, "/usr/sys"), { recursive: true });
      fs.writeFileSync(
        sessionFilePath,
        `{"installTime":${nowDate},"upgradeTime":${nowDate},"installUrl":"${appInfo.bundle_url}"}`,
        {}
      );
    }
    fs.unlinkSync(tempFilePath);
    fs.unlinkSync(hashFilePath);

    /// 下载完成，开始安装
    const result = await JMM_DB.upsert(appInfo);
    if (result === false) {
      return enqueueInstallProgress("install", 0, true, `fail to save app info to database: ${appInfo.id}`);
    }

    const metadata = new JsMMMetadata(appInfo);
    const jmm = new JsMicroModule(metadata);
    this.context!.dns.install(jmm);
    console.always("jmm serve", green("安装成功！"));
    enqueueInstallProgress("install", 0, true);
  }
}

export type $SessionInfo = {
  installTime: number;
  upgradeTime: number;
  installUrl: string;
};

export type $InstallProgressInfo = {
  state: "download" | "install";
  progress: number;
  total: number;
  current: number;
  done: boolean;
  error?: string;
};
