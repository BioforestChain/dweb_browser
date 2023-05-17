import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { locks } from "../../helper/locksManager.cjs";
import {
  $NativeWindow,
  openNativeWindow,
} from "../../helper/openNativeWindow.cjs";
import { log } from "../../helper/devtools.cjs";
import { createHttpDwebServer } from "../http-server/$createHttpDwebServer.cjs";
import chalk from "chalk"
import { converRGBAToHexa, hexaToRGBA } from "../plugins/helper.cjs";
import querystring from "node:querystring"
import path from "node:path"
import { 
  open,
  barGetState,
  barSetState,
  safeAreaGetState,
  safeAreaSetState,
  virtualKeyboardGetState,
  virtualKeyboardSetState,
  toastShow,
  shareShare,
  toggleTorch,
  torchState,
  haptics
 } from "./handler.cjs"
import type { $BootstrapContext } from "../../core/bootstrapContext.cjs";
import type { Remote } from "comlink";
import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IncomingMessage, OutgoingMessage } from "node:http";
import type { $BarState, $BAR_STYLE, $SafeAreaState, $VirtualKeyboardState } from "./assets/types";

// @ts-ignore
type $APIS = typeof import("./assets/multi-webview.html.mjs")["APIS"];
/**
 * 构建一个视图树
 * 如果是桌面版，所以不用去管树的概念，直接生成生成就行了
 * 但这里是模拟手机版，所以还是构建一个层级视图
 */
export class MultiWebviewNMM extends NativeMicroModule {
  mmid = "mwebview.sys.dweb" as const;
  observeMap: $ObserveMapNww= new Map()
  encoder = new TextEncoder()
  private _uid_wapis_map = new Map<
    number,
    { nww: $NativeWindow; apis: Remote<$APIS> }
  >();

  async _bootstrap(context: $BootstrapContext) {
    log.green(`${this.mmid} _bootstrap`)
    
    const httpDwebServer = await createHttpDwebServer(this, {});
    this._after_shutdown_signal.listen(() => httpDwebServer.close());
    /// 从本地文件夹中读取数据返回，
    /// 如果是Android，则使用 AssetManager API 读取文件数据，并且需要手动绑定 mime 与 statusCode
    (await httpDwebServer.listen()).onRequest(async (request, ipc) => {
      ipc.postMessage(
        await IpcResponse.fromResponse(
          request.req_id,
          await this.nativeFetch(
            "file:///bundle/multi-webview" + request.parsed_url.pathname
          ),
          ipc
        )
      );
    });

    const root_url = httpDwebServer.startResult.urlInfo.buildInternalUrl(
      (url) => {
        url.pathname = "/index.html";
      }
    ).href;

    /**
     * 打开 应用
     * 如果 是由 jsProcdss 调用 会在当前的 browserWindow 打开一个新的 webview
     * 如果 是由 NMM 调用的 会打开一个新的 borserWindow 同时打开一个新的 webview
     */
    this.registerCommonIpcOnMessageHandler({
      pathname: "/open",
      matchMode: "full",
      input: { url: "string" },
      output: "number",
      handler: open.bind(this, root_url),
    });

    // 关闭 ？？ 这个是关闭整个window  还是关闭一个 webview 标签
    // 用来关闭webview标签
    this.registerCommonIpcOnMessageHandler({
      pathname: "/close",
      matchMode: "full",
      input: { webview_id: "number" },
      output: "boolean",
      handler: async (args, client_ipc) => {
        const wapis = await this.forceGetWapis(client_ipc, root_url);
        return wapis.apis.closeWebview(args.webview_id);
      },
    });

    // 销毁指定的 webview
    this.registerCommonIpcOnMessageHandler({
      pathname: "/destroy_webview_by_host",
      matchMode: "full",
      input: { host: "string" },
      output: "boolean",
      handler: async (args, client_ipc) => {
        Array.from(this._uid_wapis_map.values()).forEach(wapis => {
          wapis.apis.destroyWebviewByHost(args.host)
        })
        // console.log('------ multi-webview.mobile.cts 执行了销毁', wapisArr);
        // console.log('this._uid_wapis_map: ', this._uid_wapis_map)
        // console.log('ipc.uid: ', (client_ipc as any).uid)
        // const wapis = await this.forceGetWapis(client_ipc, root_url);
        // return wapis.apis.destroyWebviewByHost(args.host);\
        return true;
      },
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/restart_webview_by_host",
      matchMode: "full",
      input: { host: "string" },
      output: "boolean",
      handler: async (args, client_ipc) => {
        Array.from(this._uid_wapis_map.values()).forEach(wapis => {
          wapis.apis.restartWebviewByHost(args.host)
        })
        return true;
      },
    })

    // 通过 host 执行 javascript
    this.registerCommonIpcOnMessageHandler({
      pathname: "/webview_execute_javascript_by_webview_url",
      method: "POST",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: async(args, client_ipc, request) => {
        const host = request.headers.get("webview_url")
        if(host === null) {
          throw new Error(chalk.red(`
            ${this.mmid} registerCommonIpcOnMessageHandler /webview_execute_javascript_by_webview_url host === null
            args: ${JSON.stringify(args)}
            request: ${JSON.stringify(request)}
          `));
        }
        const code = await request.body.text();
        // 问题新的 webveiw 没有被添加进来？？？
        // console.log('-------------', this._uid_wapis_map.values())
        Array.from(this._uid_wapis_map.values()).forEach(({apis}) => {
          apis.executeJavascriptByHost(host, code);
        })
        return true;
      }
    })

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/status-bar.nativeui.sys.dweb/getState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: barGetState.bind(this, "statusBarGetState",root_url)
    })

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/status-bar.nativeui.sys.dweb/setState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: barSetState.bind(this, "statusBarSetState", root_url)
    })

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/navigation-bar.nativeui.sys.dweb/getState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: barGetState.bind(this, "navigationBarGetState", root_url)
    })

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/navigation-bar.nativeui.sys.dweb/setState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: barSetState.bind(this, "navigationBarSetState", root_url)
    })

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/safe-area.nativeui.sys.dweb/getState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: safeAreaGetState.bind(this, root_url)
    })

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/safe-area.nativeui.sys.dweb/setState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: safeAreaSetState.bind(this, root_url)
    })    
    
    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/virtual-keyboard.nativeui.sys.dweb/getState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: virtualKeyboardGetState.bind(this, root_url)
    })

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/virtual-keyboard.nativeui.sys.dweb/setState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: virtualKeyboardSetState.bind(this, root_url)
    })

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/toast.sys.dweb/show",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: toastShow.bind(this, root_url)
    })

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/share.sys.dweb/share",
      method: "POST",
      matchMode: "full",
      input: {},
      output: "object",
      handler: shareShare.bind(this, root_url)
    })

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/torch.nativeui.sys.dweb/toggleTorch",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: toggleTorch.bind(this, root_url)
    })

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/torch.nativeui.sys.dweb/torchState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: torchState.bind(this, root_url)
    })

    // url:  file://mwebview.sys.dweb/plugin/haptics.sys.dweb/impactLight?X-Dweb-Host=api.browser.sys.dweb%3A443&style=HEAVY&action=impactLight
    // url:  file://mwebview.sys.dweb/plugin/haptics.sys.dweb/notification?X-Dweb-Host=api.browser.sys.dweb%3A443&style=SUCCESS&action=notification
    // url:  file://mwebview.sys.dweb/plugin/haptics.sys.dweb/vibrateClick?X-Dweb-Host=api.browser.sys.dweb%3A443&action=vibrateClick
    // url:  file://mwebview.sys.dweb/plugin/haptics.sys.dweb/vibrateDisabled?X-Dweb-Host=api.browser.sys.dweb%3A443&action=vibrateDisabled
    // url:  file://mwebview.sys.dweb/plugin/haptics.sys.dweb/vibrateDoubleClick?X-Dweb-Host=api.browser.sys.dweb%3A443&action=vibrateDoubleClick
    // url:  file://mwebview.sys.dweb/plugin/haptics.sys.dweb/vibrateHeavyClick?X-Dweb-Host=api.browser.sys.dweb%3A443&action=vibrateHeavyClick
    // url:  file://mwebview.sys.dweb/plugin/haptics.sys.dweb/vibrateTick?X-Dweb-Host=api.browser.sys.dweb%3A443&action=vibrateTick
    // url:  file://mwebview.sys.dweb/plugin/haptics.sys.dweb/customize?X-Dweb-Host=api.browser.sys.dweb%3A443&duration=300&action=customize
    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/haptics.sys.dweb",
      method: "GET",
      matchMode: "prefix",
      input: {action: "string"},
      output: "boolean",
      handler: haptics.bind(this, root_url)
    })
    
  }

  // ipcMinOnStateChange = async<
  //   $ApisPerperName extends keyof Pick<
  //     $APIS, "safeAreaGetState" | "navigationBarGetState" | "statusBarGetState"
  //   >
  // >(
  //   apisPropertyName: $ApisPerperName,
  //   mmid: $MMID
  // )=> {
  //   const apis  = this.apisGetFromFocused()
  //   if(apis === undefined) throw new Error('apis === undefined')
  //   const state = await apis[apisPropertyName]();
  //   this.observeMapWrite(
  //     Buffer.from(JSON.stringify(state)),
  //     mmid
  //   )
  // }

  // internalObserveToogle = async (isObserve: boolean,req: IncomingMessage, res: OutgoingMessage) => {
  //   // 获取 mmid
  //   const mmid = req.url?.split("?")[0].split("/")[1];
  //   if(mmid === undefined) throw new Error(`mmid === undefined`);
  //   const nww = this.browserWindowGetFocused();
  //   if(nww === undefined) throw new Error(`nww === undefined`);
  //   const observe_map_nww_item = this.observeMap.get(nww);
  //   if(observe_map_nww_item === undefined){
  //     const observe_map_nww_item = new Map();
  //     observe_map_nww_item.set(mmid, { isObserve: isObserve, res: undefined})
  //     this.observeMap.set(nww, observe_map_nww_item)
  //     res.end()
  //     return;
  //   }
  //   const observerItem = observe_map_nww_item.get(mmid);
  //   if(observerItem === undefined){
  //     const observerItem = {isObserve: isObserve, res: undefined}
  //     observe_map_nww_item.set(mmid, observerItem)
  //     res.end()
  //     return;
  //   }

  //   observerItem.isObserve = isObserve;
  //   res.end()
  // }

  // internalObserve = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const queryStr = req.url?.split("?")[1]
  //   if(queryStr === undefined) throw new Error(`queryStr === undefined`)
  //   const mmid = querystring.parse(queryStr).mmid
  //   if(typeof mmid !== 'string') throw new Error(`typeof mmid !== 'string'`)
  //   // 保存在当前激活的 nww.mmid = observe
  //   const nww = this.browserWindowGetFocused()
  //   if(nww === undefined) throw new Error(`nww === undefined`)
  //   const observe_nww_item = this.observeMap.get(nww)
  //   if(observe_nww_item === undefined){
  //     const observeItem = {res: res, isObserve: false};
  //     const observe_nww_item = new Map()
  //     observe_nww_item.set(mmid, observeItem)
  //     this.observeMap.set(nww, observe_nww_item)
  //     return;
  //   }
  //   const observeItem = observe_nww_item.get(mmid)
  //   if(observeItem === undefined){
  //     const _observeItem = {res: res, isObserve: false};
  //     observe_nww_item.set(mmid, _observeItem)
  //     return;
  //   }

  //   observeItem.res = res;
  // } 

  // barGetState = async <
  //   $ApisFnName extends keyof Pick<$APIS, 'statusBarGetState' | "navigationBarGetState">
  // >(
  //   apisFnName: $ApisFnName,
  //   req: IncomingMessage,
  //   res: OutgoingMessage
  // ) =>{
  //   const apis  = this.apisGetFromFocused()
  //   if(apis === undefined) throw new Error('apis === undefined')
  //   const state = await apis[apisFnName]()
  //   const stateRGB = {
  //     ...state,
  //     color: hexaToRGBA(state.color)
  //   }
  //   res.end(Buffer.from(JSON.stringify(stateRGB)))
  // }

  // onHistoryBack = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const origin = getOriginByReq(req)
  //   const apis = this.apisGetFromFocused()
  //   if(apis === undefined) throw new Error(`apis === undefined`)
  //   apis.acceptMessageFromWebview({
  //     origin: origin,
  //     action: "history_back",
  //     value: ""
  //   });
  //   res.end()
  // }

  // barSetState = async <
  //   $ApiFnName extends keyof Pick<
  //     $APIS, "statusBarSetState" | "navigationBarSetState"
  //   >,
  // >(
  //   apisFnName: $ApiFnName,
  //   req: IncomingMessage, 
  //   res: OutgoingMessage
  // ) => {
  //   const mmid = req.url?.split("?")[0].split("/")[1];
  //   if(mmid === undefined) throw new Error(`mmid === undefined`);
  //   const searchParams = querystring.parse(req.url as string);
  //   const apis  = this.apisGetFromFocused()
  //   if(apis === undefined) throw new Error('apis === undefined')
  //   let state: $BarState | undefined;
  //   if(searchParams.color !== undefined && typeof searchParams.color === "string"){
  //     const color = JSON.parse(searchParams.color)
  //     state  = await apis[apisFnName](
  //       'color', 
  //       converRGBAToHexa(
  //         color.red, color.green, color.blue, color.alpha
  //       )
  //     )
  //   }

  //   if(searchParams.style !== undefined && typeof searchParams.style === "string"){
  //     state = await apis[apisFnName](
  //       'style', searchParams.style as $BAR_STYLE
  //     )
  //   }

  //   if(searchParams.overlay !== undefined && typeof searchParams.overlay === "string"){
  //     state = await apis[apisFnName](
  //       'overlay', searchParams.overlay === "true" ? true : false
  //     )
  //   }

  //   if(searchParams.visible !== undefined && typeof searchParams.visible === "string"){
  //     state = await apis[apisFnName](
  //       'visible', searchParams.visible === "true" ? true : false
  //     )
  //   }

  //   if(state === undefined) throw new Error(`state === undefined`);
  //   const stateRGB = {
  //     ...state,
  //     color: hexaToRGBA(state.color)
  //   }

  //   const buffer = Buffer.from(JSON.stringify(stateRGB))
  //   res.end(Buffer.from(buffer))
  //   this.observeMapWrite(buffer, mmid)
  // }

  // safeAreaSetState = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const mmid = req.url?.split("?")[0].split("/")[1];
  //   if(mmid === undefined) throw new Error(`mmid === undefined`);
  //   const searchParams = querystring.parse(req.url as string);
  //   if(searchParams.overlay == undefined) throw new Error(`searchParams.overlay == undefined`);
  //   if(typeof searchParams.overlay !== 'string') throw new Error(`typeof searchParams.overlay !== 'string'`)
  //   const apis  = this.apisGetFromFocused()
  //   if(apis === undefined) throw new Error('apis === undefined')
  //   let state: $SafeAreaState
  //   state = await apis.safeAreaSetOverlay(
  //     searchParams.overlay === "true" ? true : false
  //   )
  //   const buffer = Buffer.from(JSON.stringify(state))
  //   res.end(Buffer.from(buffer))
  //   this.observeMapWrite(buffer, mmid)
  // }

  // observeMapWrite(buffer: Buffer, mmid: string){
  //   const nww = this.browserWindowGetFocused()
  //   if(nww === undefined) throw new Error(`nww === undefined`);
  //   const observe_map_nww_item =  this.observeMap.get(nww)
  //   if(observe_map_nww_item === undefined) {
  //     console.log("observe_map_nww_item === undefined")
  //     return;
  //   };
  //   const observeItem = observe_map_nww_item.get(mmid);
  //   if(observeItem === undefined) {
  //     // 如果没有表示没有监听
  //     return;
  //   };
  //   if(observeItem.res === undefined) throw new Error(`observeItem.res === undefined`);
  //   observeItem.isObserve 
  //   ? observeItem.res.write(Buffer.concat([buffer, this.encoder.encode("\n")])) 
  //   : ""
  // }

  // safeAreaGetState = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const apis  = this.apisGetFromFocused()
  //   if(apis === undefined) throw new Error('apis === undefined')
  //   const state = await apis.safeAreaGetState()
  //   res.end(Buffer.from(JSON.stringify(state)))
  // }

  // virtualKeyboardGetState = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const apis  = this.apisGetFromFocused()
  //   if(apis === undefined) throw new Error('apis === undefined')
  //   const state = await apis.virtualKeyboardGetState()
  //   res.end(Buffer.from(JSON.stringify(state)))
  // }

  // virtualKeyboardSetState = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const mmid = req.url?.split("?")[0].split("/")[1];
  //   if(mmid === undefined) throw new Error(`mmid === undefined`);
  //   const searchParams = querystring.parse(req.url as string);
  //   if(searchParams.overlay == undefined) throw new Error(`searchParams.overlay == undefined`);
  //   if(typeof searchParams.overlay !== 'string') throw new Error(`typeof searchParams.overlay !== 'string'`)
  //   const apis  = this.apisGetFromFocused()
  //   if(apis === undefined) throw new Error('apis === undefined')
  //   let state: $VirtualKeyboardState
  //   state = await apis.virtualKeyboardSetOverlay(
  //     searchParams.overlay === "true" ? true : false
  //   )
  //   const buffer = Buffer.from(JSON.stringify(state))
  //   res.end(Buffer.from(buffer))
  //   this.observeMapWrite(buffer, mmid)
  // }

  /**
   * 获取当前激活的 browserWindow 的 apis
   */
  apisGetFromFocused(){
    return Array.from(this._uid_wapis_map.values()).find(item => item.nww.isFocused())?.apis
  }

  browserWindowGetFocused(){
    return Array.from(this._uid_wapis_map.values()).find(item => item.nww.isFocused())?.nww
  }
  
  _shutdown() {
    this._uid_wapis_map.forEach((wapi) => {
      wapi.nww.close();
    });
    this._uid_wapis_map.clear();
  }

  // 是不是可以获取 multi-webviw.html 中的全部api
  // this._uid_wapis_map 是更具 ipc.uid 作为键明保存的，
  // 但是在一个 BrowserWindow 的内部会有多个 ipc 
  // 这样会导致问题出现 会多次触发 openNativeWindow
  forceGetWapis(ipc: Ipc, root_url: string) {
    return locks.request("multi-webview-get-window-" + ipc.uid, async () => {
      let wapi = this._uid_wapis_map.get(ipc.uid);
      if (wapi === undefined) {
        const nww = await openNativeWindow(root_url, {
          webPreferences: {
            webviewTag: true,
          },
          autoHideMenuBar: true,
        });
        nww.maximize();

        // 打开 开发工具
        nww.webContents.openDevTools();

        const apis = nww.getApis<$APIS>();
        const absolutePath = "file://" + path.resolve(__dirname, "./assets/preload.cjs")
        apis.preloadAbsolutePathSet(absolutePath)
        this._uid_wapis_map.set(ipc.uid, (wapi = { nww, apis }));
      }
      return wapi;
    });
  }

  getWapisByUid(uid: number){
    console.log("this._uid_wapis_map: ", this._uid_wapis_map,'---', uid)
    return this._uid_wapis_map.get(uid);
  }
}

function getOriginByReq(req: IncomingMessage){
  return req.headers.origin ?? new URL(req.headers.referer as string).origin
}

export interface $ObserveItem{
  res: OutgoingMessage | undefined,
  isObserve: boolean,
}

type $ObserveMapNwwItem = Map<
  string /** mmid */, 
  $ObserveItem
>
 
type $ObserveMapNww = Map<
  // nww
  Electron.CrossProcessExports.BrowserWindow,
  $ObserveMapNwwItem
>
