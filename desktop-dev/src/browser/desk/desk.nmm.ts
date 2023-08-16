import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { MICRO_MODULE_CATEGORY } from "../../core/category.const.ts";
import { buildRequestX } from "../../core/helper/ipcRequestHelper.ts";
import { Ipc, IpcEvent } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { $MMID } from "../../core/types.ts";
import { ChangeableMap, changeState } from "../../helper/ChangeableMap.ts";
import { $Callback, createSignal } from "../../helper/createSignal.ts";
import { simpleEncoder } from "../../helper/encoding.ts";
import { P, fetchMatch } from "../../helper/patternHelper.ts";
import { jsonlinesStreamRead, jsonlinesStreamReadAll } from "../../helper/stream/jsonlinesStreamHelper.ts";
import { ReadableStreamOut, streamReadAll } from "../../helper/stream/readableStreamHelper.ts";
import { z, zq } from "../../helper/zodHelper.ts";
import { createHttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
import { TaskbarApi } from "./api.taskbar.ts";

export class DeskNMM extends NativeMicroModule {
  mmid = "desk.browser.dweb" as const;
  name = "Desk";
  override categories = [MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Desktop];
  readonly runingApps = new ChangeableMap<$MMID, Ipc>();

  protected async _bootstrap(context: $BootstrapContext) {
    // 当前激活的app
    let focusApp: string | null = null;
    this.listenApps();
    this.onAfterShutdown(() => {
      this.runingApps.reset();
    });

    const taskbarServer = await this._createTaskbarWebServer(context);
    const desktopServer = await this._createDesktopWebServer();

    const taskbarApi = await TaskbarApi.create(this, context, taskbarServer, desktopServer);
    this._onShutdown(() => {
      taskbarApi.close();
    });

    const query_app_id = zq.object({
      app_id: zq.mmid(),
    });
    const query_limit = zq.object({
      limit: zq.number().optional(),
    });
    const json_limit = z.object({
      limit: z.number().optional(),
    });
    const query_url = zq.object({
      url: zq.url(),
    });
    const query_resize = zq.object({
      width: zq.number(),
      height: zq.number(),
    });
    const onFetchHanlder = fetchMatch()
      //#region 通用接口
      .get("/readFile", (event) => {
        const { url } = query_url(event.searchParams);
        return this.nativeFetch(url);
      })
      /** 读取浏览器默认的 accpet 头部参数 */
      .get(P.string.startsWith("/readAccept."), (event) => {
        return Response.json({ accept: event.headers.get("Accept") });
      })
      /** 打开应用 */
      .get("/openAppOrActivate", async (event) => {
        const { app_id } = query_app_id(event.searchParams);
        console.always("activity", app_id);
        const ipc = this.runingApps.get(app_id) ?? (await this.connect(app_id));

        if (ipc !== undefined) {
          ipc.postMessage(IpcEvent.fromText("activity", ""));
          focusApp = ipc.remote.mmid;
          /// 成功打开，保存到列表中
          this.runingApps.set(app_id, ipc);
          /// 如果应用关闭，将它从列表中移除
          ipc.onClose(() => {
            this.runingApps.delete(app_id,false);
          });
        }

        return Response.json(ipc !== undefined);
      })
      /** 关闭应用 */
      .get("/closeApp", async (event) => {
        const { app_id } = query_app_id(event.searchParams);
        let closed = false;
        if (this.runingApps.has(app_id)) {
          closed = await context.dns.close(app_id);
          if (closed) {
            this.runingApps.delete(app_id,false);
          }
        }
        return Response.json(closed);
      })
      //#endregion
      .get("/desktop/apps", async () => {
        const desktopApi = await taskbarApi.getDesktopApi();
        return Response.json(await desktopApi.getDesktopAppList());
      })
      .duplex("/desktop/observe/apps", async (event) => {
        const responseBody = new ReadableStreamOut<Uint8Array>();
        const desktopApi = await taskbarApi.getDesktopApi();
        const doWriteJsonline = async () => {
          responseBody.controller.enqueue(
            simpleEncoder(JSON.stringify(await desktopApi.getDesktopAppList()) + "\n", "utf8")
          );
        };
        /// 监听变更，推送数据
        const off = this.runingApps.onChange(doWriteJsonline);
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
        return Response.json(await taskbarApi.getTaskbarAppList(limit));
      })
      .duplex("/taskbar/observe/apps", async (event) => {
        let { limit = Infinity } = query_limit(event.searchParams);
        const responseBody = new ReadableStreamOut<Uint8Array>();
        const doWriteJsonline = async () => {
          responseBody.controller.enqueue(
            simpleEncoder(JSON.stringify(await taskbarApi.getTaskbarAppList(limit)) + "\n", "utf8")
          );
        };
        /// 监听变更，推送数据
        const off = this.runingApps.onChange(doWriteJsonline);
        /// 监听关闭，停止监听
        void jsonlinesStreamReadAll(await event.ipcRequest.body.stream(), {
          map(item: { limit?: number }) {
            limit = json_limit.parse(item).limit ?? Infinity;
          },
        }).finally(() => {
          off();
          /// 如果传来的数据解析异常了，websocket就断掉
          responseBody.controller.close();
        });
        /// 发送一次现有的数据数据
        void doWriteJsonline();
        return { body: responseBody.stream };
      })
      .duplex("/taskbar/observe/status", async (event) => {
        const responseBody = new ReadableStreamOut<Uint8Array>();
        // 桌面端 focus永远是true
        const doWriteJsonline = async () => {
          responseBody.controller.enqueue(simpleEncoder(`{"focus":true,"appId":"${focusApp}"}` + "\n", "utf8"));
        };
        /// 监听变更，推送数据
        const off = this.runingApps.onChange(doWriteJsonline);
        /// 监听关闭，停止监听
        void jsonlinesStreamReadAll(await event.ipcRequest.body.stream()).finally(() => {
          off();
          /// 如果传来的数据解析异常了，websocket就断掉
          responseBody.controller.close();
        });
        /// 发送一次现有的数据数据
        void doWriteJsonline();
        return { body: responseBody.stream };
      })
      .get("/taskbar/resize", async (event) => {
        const { width, height } = query_resize(event.searchParams);
        const changed = await taskbarApi.resize(width, height);
        return Response.json(changed);
      })
      .get("/taskbar/toggle-desktop-view", async (event) => {
        return Response.json(await taskbarApi.toggleDesktopView());
      });
    this.onFetch(onFetchHanlder.run).internalServerError();
  }

  private async listenApps() {
    const connectResult = this.context?.dns.connect("dns.std.dweb");
    if (connectResult === undefined) {
      throw new Error(`dns.std.dweb not found!`);
    }
    /// 发送激活指令
    const [opendAppIpc] = await connectResult;
    const res = await opendAppIpc.request("/observe/app");
    const stream = await res.body.stream();
    for await (const state of jsonlinesStreamRead<changeState<$MMID>>(stream)) {
      this.runingApps.emitChange(state);
    }
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
                body: JSON.stringify({ error: `no support ${mmid}` }),
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

  private _shutdown_signal = createSignal<$Callback>();
  private _onShutdown = this._shutdown_signal.listen;
  protected _shutdown() {
    this._shutdown_signal.emitAndClear();
  }
}
