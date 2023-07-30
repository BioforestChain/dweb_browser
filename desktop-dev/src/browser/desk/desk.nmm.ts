import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { MICRO_MODULE_CATEGORY } from "../../core/category.const.ts";
import { buildRequestX } from "../../core/helper/ipcRequestHelper.ts";
import { Ipc, IpcEvent } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { $MMID } from "../../core/types.ts";
import { ChangeableMap } from "../../helper/ChangeableMap.ts";
import { JsonlinesStream } from "../../helper/JsonlinesStream.ts";
import { $Callback, createSignal } from "../../helper/createSignal.ts";
import { tryDevUrl } from "../../helper/electronIsDev.ts";
import { simpleEncoder } from "../../helper/encoding.ts";
import { createComlinkNativeWindow } from "../../helper/openNativeWindow.ts";
import { P, fetchMatch } from "../../helper/patternHelper.ts";
import { ReadableStreamOut, streamReadAll } from "../../helper/readableStreamHelper.ts";
import { z, zq } from "../../helper/zodHelper.ts";
import { HttpDwebServer, createHttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
import { TaskbarApi } from "./api.taskbar.ts";
import { window_options } from "./const.ts";
import { deskStore } from "./desk.store.ts";
import { $DeskAppMetaData } from "./types.ts";

export class DeskNMM extends NativeMicroModule {
  mmid = "desk.browser.dweb" as const;
  name = "Desk";
  override categories = [MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Desktop];

  protected async _bootstrap(context: $BootstrapContext) {
    const query_app_id = zq.object({
      app_id: zq.mmid(),
    });
    const query_limit = zq.object({
      limit: zq.number().optional(),
    });
    const json_limit = z.object({
      limit: z.number().optional(),
    });

    const taskbarAppList = [...deskStore.get("taskbar/apps", () => new Set())];
    const runingApps = new ChangeableMap<$MMID, Ipc>();
    runingApps.onChange((map) => {
      for (const app_id of map.keys()) {
        taskbarAppList.unshift(app_id);
      }
      deskStore.set("taskbar/apps", new Set(taskbarAppList));
    });

    const getDesktopAppList = async () => {
      const apps = await context.dns.search(MICRO_MODULE_CATEGORY.Application);
      return apps.map((metaData) => {
        return { ...metaData, running: runingApps.has(metaData.mmid) };
      });
    };

    const getTaskbarAppList = async (limit: number) => {
      const apps: $DeskAppMetaData[] = [];

      for (const app_id of taskbarAppList) {
        const metaData = await context.dns.query(app_id);
        if (metaData) {
          apps.push({ ...metaData, running: runingApps.has(app_id) });
        }
        if (apps.length >= limit) {
          return apps;
        }
      }
      return apps;
    };

    const query_url = zq.object({
      url: zq.url(),
    });

    const onFetchHanlder = fetchMatch()
      //#region 通用接口
      .get("/readFile", (event) => {
        const { url } = query_url(event.searchParams);
        return this.nativeFetch(url);
      })
      /** 读取浏览器默认的 accpet 头部参数ß */
      .get(P.string.startsWith("/readAccept."), (event) => {
        return { body: event.headers.get("Accept") };
      })
      /** 打开应用 */
      .get("/openAppOrActivate", async (event) => {
        const { app_id } = query_app_id(event.searchParams);
        console.always("activity", app_id);
        const ipc = runingApps.get(app_id) ?? (await this.connect(app_id));

        if (ipc !== undefined) {
          ipc.postMessage(IpcEvent.fromText("activity", ""));
          /// 如果成功打开，将它“追加”到列表中
          runingApps.delete(app_id);
          runingApps.set(app_id, ipc);
          /// 如果应用关闭，将它从列表中移除
          ipc.onClose(() => {
            runingApps.delete(app_id);
          });
        }

        return Response.json(ipc !== undefined);
      })
      /** 关闭应用 */
      .get("/closeApp", async (event) => {
        const { app_id } = query_app_id(event.searchParams);
        let closed = false;
        if (runingApps.has(app_id)) {
          closed = await context.dns.close(app_id);
          if (closed) {
            runingApps.delete(app_id);
          }
        }
        return Response.json(closed);
      })
      //#endregion
      .get("/desktop/apps", async () => {
        return Response.json(await getDesktopAppList());
      })
      .duplex("/desktop/observe/apps", async (event) => {
        const responseBody = new ReadableStreamOut<Uint8Array>();
        const doWriteJsonline = async () => {
          responseBody.controller.enqueue(simpleEncoder(JSON.stringify(await getDesktopAppList()) + "\n", "utf8"));
        };
        /// 监听变更，推送数据
        const off = runingApps.onChange(doWriteJsonline);
        /// 监听关闭，停止监听
        void streamReadAll(await event.ipcRequest.body.stream()).finally(() => {
          off();
          /// 确保双向中断
          responseBody.controller.close();
        });
        /// 发送一次现有的数据数据
        void doWriteJsonline();
        return { body: responseBody.stream };
      })
      .get("/taskbar/apps", async (event) => {
        const { limit = Infinity } = query_limit(event.searchParams);
        return Response.json(await getTaskbarAppList(limit));
      })
      .duplex("/taskbar/observe/apps", async (event) => {
        let { limit = Infinity } = query_limit(event.searchParams);
        const responseBody = new ReadableStreamOut<Uint8Array>();
        const doWriteJsonline = async () => {
          responseBody.controller.enqueue(simpleEncoder(JSON.stringify(await getTaskbarAppList(limit)) + "\n", "utf8"));
        };
        /// 监听变更，推送数据
        const off = runingApps.onChange(doWriteJsonline);
        /// 监听关闭，停止监听
        void streamReadAll(
          (await event.ipcRequest.body.stream())
            .pipeThrough(new TextDecoderStream())
            .pipeThrough(new JsonlinesStream<{ limit?: number }>()),
          {
            map(item) {
              limit = json_limit.parse(item).limit ?? Infinity;
            },
          }
        ).finally(() => {
          off();
          /// 如果传来的数据解析异常了，websocket就断掉
          responseBody.controller.close();
        });
        /// 发送一次现有的数据数据
        void doWriteJsonline();
        return { body: responseBody.stream };
      });
    this.onFetch(onFetchHanlder.run).internalServerError();

    const taskbarServer = await this._createTaskbarWebServer(context);
    const desktopServer = await this._createDesktopWebServer();

    const taskbarWin = await this._createTaskbarView(taskbarServer, desktopServer);
    this._onShutdown(() => {
      taskbarWin.close();
    });
  }

  private async _createTaskbarWebServer(context: $BootstrapContext) {
    const taskbarServer = await createHttpDwebServer(this, {
      subdomain: "taskbar",
      port: 433,
    });
    {
      const API_PREFIX = "/api/";
      (await taskbarServer.listen()).onFetch(async (event) => {
        const { pathname, search } = event.url;
        let url: string;
        if (pathname.startsWith(API_PREFIX)) {
          url = `file://${pathname.slice(API_PREFIX.length)}${search}`;
          const mmid = new URL(url).hostname as $MMID;
          if (mmid !== this.mmid) {
            /// 不支持
            if ((await context.dns.query(mmid)) === undefined) {
              return {
                statusCode: 404,
                body: "",
              };
            }
          }
        } else {
          url = `file:///sys/browser/desk${pathname}?mode=stream`;
        }
        const request = buildRequestX(url, {
          method: event.method,
          headers: event.headers,
          body: event.ipcRequest.body.raw,
        });

        const res = await this.nativeFetch(request);
        return res;
      });
    }
    return taskbarServer;
  }
  private async _createDesktopWebServer() {
    const desktopServer = await createHttpDwebServer(this, {
      subdomain: "desktop",
      port: 433,
    });
    {
      const API_PREFIX = "/api/";
      (await desktopServer.listen()).onFetch(async (event) => {
        const { pathname, search } = event.url;
        let url: string;
        if (pathname.startsWith(API_PREFIX)) {
          url = `file://${pathname.slice(API_PREFIX.length)}${search}`;
        } else {
          url = `file:///sys/browser/desk${pathname}?mode=stream`;
        }
        const request = buildRequestX(url, {
          method: event.method,
          headers: event.headers,
          body: event.ipcRequest.body.raw,
        });

        const res = await this.nativeFetch(request);
        return res;
      });
    }
    return desktopServer;
  }

  private async _createTaskbarView(taskbarServer: HttpDwebServer, desktopServer: HttpDwebServer) {
    const taskbarWin = await createComlinkNativeWindow(
      await tryDevUrl(
        taskbarServer.startResult.urlInfo.buildInternalUrl((url) => {
          url.pathname = "/taskbar.html";
        }).href,
        `http://localhost:3600/taskbar.html`
      ),
      window_options,
      async (win) => {
        return new TaskbarApi(this, win, taskbarServer, desktopServer);
      }
    );

    taskbarWin.webContents.openDevTools({ mode: "undocked" });

    // taskbarWin.setPosition()

    return taskbarWin;
  }

  private _shutdown_signal = createSignal<$Callback>();
  private _onShutdown = this._shutdown_signal.listen;
  protected _shutdown() {
    this._shutdown_signal.emitAndClear();
  }
}

