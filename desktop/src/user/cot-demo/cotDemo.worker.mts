import { PromiseOut } from "../../helper/PromiseOut.mjs";
import { EVENT, WebViewState } from "../tool/tool.event.mjs";
import { nativeOpen, nativeActivate } from "../tool/tool.native.mjs";
import { cros, onApiRequest } from "../tool/tool.request.mjs";
import { DetailedDiff, detailedDiff } from "deep-object-diff"

const main = async () => {
  const { IpcEvent } = ipc;
  const mainUrl = new PromiseOut<string>();
  const webViewMap = new Map<string, WebViewState>()
  let oldWebviewState: WebViewState[] = [];

  /**尝试打开view */
  const tryOpenView = async () => {
    console.log("cotDemo.worker tryOpenView=>", webViewMap.size);
    if (webViewMap.size === 0) {
      // open
      const url = await mainUrl.promise;
      const view_id = await nativeOpen(url)
      return view_id
    }
    // 当前的策略是有多少个webview激活多少个
    webViewMap.forEach((item, key) => {
      // activate
      nativeActivate(key)
    })
  };


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

    // console.time(`open file ${pathname}`);

    const remoteIpcResponse = await jsProcess.nativeRequest(
      `file:///cot-demo${pathname}?mode=stream`
    );
    // console.timeEnd(`open file ${pathname}`);
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

  // 连接到browser
  const connectBrowser = async () => {
    const browserIpc = await jsProcess.connect("browser.sys.dweb");
    Object.assign(globalThis, { browserIpc });
    browserIpc.onEvent(async (event) => {
      // console.log("cotDemo.worker event browser.sys.dweb", event.name, event.text);
      // browser点击图标，需要开启的逻辑
      if (event.name === "activity") {
        hasActivity = true;
        const view_id = await tryOpenView();
        // console.log("cotDemo.worker activity =>", view_id);
        browserIpc.postMessage(IpcEvent.fromText("ready", view_id ?? "activity"));
        return
      }
    });
  }
  connectBrowser()

  // 这里是全局的连接 负责接收别人发送的信息
  const connectGlobal = () => {
    jsProcess.onConnect((ipc) => {
      ipc.onEvent(async (event) => {
        // console.log("cotDemo.worker event global => ", event.name, event.data);
        if (event.name === EVENT.State && typeof event.data === "string") {
          const newState = JSON.parse(event.data)
          const diff = detailedDiff(oldWebviewState, newState)
          oldWebviewState = newState
          // console.log("cotDemo.worker mwebview diff=>", diff, newState);
          diffFactory(diff)
        }
      });
    });
  }
  connectGlobal()

  const diffFactory = (diff: DetailedDiff) => {
    //  是否有新增
    for (const id in diff.added) {
      // console.log("cotDemo.worker added=>", id)
      webViewMap.set(id, JSON.parse(diff.added[id as keyof typeof diff.added]));
    }
    // 是否有删除
    for (const id in diff.deleted) {
      // console.log("cotDemo.worker deleted=>", id)
      webViewMap.delete(id)
    }
    // 是否有更新
    for (const id in diff.updated) {
      // console.log("cotDemo.worker updated=>", id)
      webViewMap.set(id, JSON.parse(diff.updated[id as keyof typeof diff.updated]));
    }
  }

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
