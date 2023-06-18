import { bluetoothPlugin } from "../client/components/bluetooth/bluetooth.plugin.js";
import { IpcEvent, IpcResponse, PromiseOut, IpcRequest } from "./deps.ts";
import {
  closeWindow,
  cros,
  nativeOpen,
} from "./tool/tool.native.ts";
import { fetchSignal, onApiRequest } from "./tool/tool.request.ts";

const main = async () => {
  console.log("在如了 worker.js")

  const { jsProcess, http } = navigator.dweb;
  console.log("jsProcess: ", jsProcess)

  

    // 连接 bluetooth
  // 启动主页面的地址
  const mainUrl = new PromiseOut<string>();
  // 关闭信号
  const EXTERNAL_PREFIX = "/external/";
  const externalMap = new Map<number, PromiseOut<IpcResponse>>();

  /**尝试打开view */
  const tryOpenView = async () => {
    console.log("tryOpenView... start");
    const url = await mainUrl.promise;
    nativeOpen(url);
    console.log("tryOpenView... end", url);
  };

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
 

  // 自己api处理 Fetch
  const apiReadableStreamIpc = await apiServer.listen();
  // 文件服务处理
  const wwwReadableStreamIpc = await wwwServer.listen();
   

  apiReadableStreamIpc.onRequest(async (request, ipc) => {
    const url = request.parsed_url;
    // serviceWorker
    if (url.pathname.startsWith("/dns.sys.dweb")) {
      const result = await serviceWorkerFactory(url);
      const ipcResponse = IpcResponse.fromText(
        request.req_id,
        200,
        undefined,
        result,
        ipc
      );
      cros(ipcResponse.headers);
      // 返回数据到前端
      return ipc.postMessage(ipcResponse);
    }
    
    onApiRequest(apiServer.startResult.urlInfo, request, ipc);
  });

  wwwReadableStreamIpc.onRequest(async (request, ipc) => {
    let pathname = request.parsed_url.pathname;
    if (pathname === "/") {
      pathname = "/index.html";
    }
    const remoteIpcResponse = await jsProcess.nativeRequest(
      `file:///usr/www${pathname}?mode=stream`
    );
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

  
  // 转发serviceWorker 请求
  const serviceWorkerFactory = async (url: URL) => {
    const pathname = url.pathname;
    // 关闭的流程需要调整
    // 向dns发送关闭当前 模块的消息
    // woker.js -> dns -> JsMicroModule -> woker.js -> 其他的 NativeMicroModule

    if (pathname.endsWith("restart")) {
      // 关闭全部的服务
      await apiServer.close();
      await wwwServer.close();
      // 关闭所有的DwebView
      await closeWindow();
      // 这里只需要把请求发送过去，因为app已经被关闭，已经无法拿到返回值
      jsProcess.restart();
      return "restart ok";
    }

    // 只关闭 渲染一个渲染进程 不关闭 service
    if (pathname.endsWith("close")) {
      await closeWindow();
      return "window close";
    }
    return "no action for serviceWorker Factory !!!";
  };

  console.error(">>>>>>>>>>>>>>>>>>> jsProcess.onActivity");
  /// 如果有人来激活，那我就唤醒我的界面
  jsProcess.onActivity(async (_ipcEvent, ipc) => {
    await tryOpenView();
    ipc.postMessage(IpcEvent.fromText("ready", "activity"));
  });
  jsProcess.onClose(() => {
    closeWindow();
  });

  const interUrl = wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
    url.pathname = "/index.html";
  });
  interUrl.searchParams.set(
    "X-Plaoc-Internal-Url",
    apiServer.startResult.urlInfo.buildInternalUrl().href
  );
  interUrl.searchParams.set(
    "X-Plaoc-Public-Url",
    apiServer.startResult.urlInfo.buildPublicUrl().href
  );
  mainUrl.resolve(interUrl.href);

  /**
   * 立刻自启动
   */
  tryOpenView();
};

main();
