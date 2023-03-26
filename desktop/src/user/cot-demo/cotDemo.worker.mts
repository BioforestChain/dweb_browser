import { PromiseOut } from "../../helper/PromiseOut.mjs";
import type { ViewTree } from "../../sys/multi-webview/assets/multi-webview.html.mjs";
import { EVENT } from "./cotDemo.event.mjs";
import { nativeOpen, nativeActivate } from "./cotDemo.native.mjs";
import { cros, onApiRequest } from "./cotDemo.request.mjs";

const main = async () => {
  const { IpcEvent } = ipc;
  const mainUrl = new PromiseOut<string>();
  const webviewSet = new Map<string, ViewTree>();

  /**尝试打开view */
  const tryOpenView = async (webview_id?: string) => {
    console.log("tryOpenView", webview_id);
    if (webview_id && webviewSet.has(webview_id)) {
      // activate
      return nativeActivate(webview_id)
    }
    // open
    const url = await mainUrl.promise;
    const view_id = await nativeOpen(url)
    if (webviewSet.size == 0) {
      connectMwebview()
    }
    return view_id
  };

  /**建立连接 */
  const connectMwebview = async () => {
    const mwebviewIpc = await jsProcess.connect("mwebview.sys.dweb");
    Object.assign(globalThis, { mwebviewIpc });
    mwebviewIpc.onEvent((event) => {
      console.log("cotDemo#got event:", event.name, event.text);
      if (event.name === EVENT.State) {
        // todo 使用signal 让状态更改和事件分开
      }
    });
  }

  console.log("[cotDemo.worker.mts] main");
  const { IpcResponse, IpcHeaders } = ipc;

  /**给前端的文件服务 */
  const wwwServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "www",
    port: 443,
  });

  /**给前端的api服务 */
  const apiServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "api",
    port: 443,
  });

  (await apiServer.listen()).onRequest(async (request, ipc) => {
    onApiRequest(apiServer.startResult.urlInfo, request, ipc);
  });


  (await wwwServer.listen()).onRequest(async (request, ipc) => {
    let pathname = request.parsed_url.pathname;
    if (pathname === "/") {
      pathname = "/index.html";
    }

    console.time(`open file ${pathname}`);

    const remoteIpcResponse = await jsProcess.nativeRequest(
      `file:///cot-demo${pathname}?mode=stream`
    );
    console.timeEnd(`open file ${pathname}`);
    /**
     * 流转发，是一种高性能的转发方式，等于没有真正意义上去读取response.body，
     * 而是将response.body的句柄直接转发回去，那么根据协议，一旦流开始被读取，自己就失去了读取权。
     *
     * 如此数据就不会发给我，节省大量传输成本
     */
    ipc.postMessage(
      new IpcResponse(
        request.req_id,
        remoteIpcResponse.statusCode,
        cros(remoteIpcResponse.headers),
        remoteIpcResponse.body,
        ipc
      )
    );
  });

  let hasActivity = false;
  // 这里是全局的连接 根据桌面协议，收到activity后就会被唤醒
  jsProcess.onConnect((ipc) => {
    ipc.onEvent(async (event) => {
      console.log("cotDemo.worker => ", event.name, event.text);
      if (event.name === "activity" && typeof event.data === "string") {
        hasActivity = true;
        const view_id = await tryOpenView(event.data);
        console.log("cotDemo.worker => activity", view_id);
        ipc.postMessage(IpcEvent.fromText("ready", view_id));
        return
      }
    });
  });

  {
    const interUrl = wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
    }).href;
    mainUrl.resolve(interUrl);
    // 如果没有被 browser 激活，那么也尝试自启动
    if (hasActivity === false) {
      await tryOpenView();
    }
  }
};

main();
