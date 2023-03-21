import { PromiseOut } from "../../helper/PromiseOut.mjs";
import { cros, onApiRequest } from "./cotDemo.request.mjs";

const main = async () => {
  let view_id: Promise<string> | undefined;
  const mainUrl = new PromiseOut<string>();
  const tryOpenView = async () => {
    const url = await mainUrl.promise;
    // view_id ??=
    jsProcess
      .nativeFetch(
        `file://mwebview.sys.dweb/open?url=${encodeURIComponent(url)}`
      )
      .text();
  };
  /// 根据桌面协议，收到activity后就会被唤醒
  jsProcess.onConnect((ipc) => {
    console.log("on connect", ipc);

    ipc.onEvent(async (event) => {
      console.log("ookkkk", event);
      if (event.name === "activity") {
        tryOpenView();
      }
    });
  });

  console.log("[cotDemo.worker.mts] main");
  const { IpcResponse, IpcHeaders } = ipc;

  // createHttpDwebServer 的本质是通过 jsProcess 这个模块向
  // 发起一个 file://http.sys.dweb/start 请求
  // 现在的问题是 这个请求 http.sys.dweb/start 并没有收到
  // 需要解决这个问题
  // 但是 启动这个模块的时候却触发这个请求

  //http.createHttpDwebServer(jsProcess, {}) === jsProcess.nativeFetch()
  // jsProcess.nativeFetch() === jsProcess._nativeFetch()
  // jsProcess._nativeFetch() === this.fetchIpc.request()
  // this.fetchIpc.request() === this.nativeFetchPort.postMessage()
  // this.nativeFetchPort === js-process.web.mts 发送过来的port
  // js-process.web.mts 发送过来的post === js-process.cts createProcess() 发送过来的port
  // js-process.cts createProcess() 发送过来的是port2, port1 还是保存在 ipc_to_worker 对象里面
  // ipc_to_worker.onMessage 会把消息 通过 ipc 转发出去
  // ipc js-process.cts 调用createProcessAndRun（） 传递进来的
  // createProcessAndRun() 传递来的ipc 是通过 registerCommonIpcOnMessageHandler. /create-process 传递进来的ipc
  // registerCommonIpcOnMessageHandler. /create-process  是通过 micro-module.js.cts. streamIpc.bindIncomeStream 中调用 调用传递过来的ipc
  // 到这里为止 cotdemo.worker.mts 发送的消息会转发到 micro-module.js.cts 中 卡在这里了没有接受到消息
  // micro-module.js.cts 需要注册 this.onConnect() 把消息转发给指定的 nmm 模块
  // 消息的扭转
  // http.createHttpDwebServer(jsProcess, {}) === jsProcess.nativeFetch() 本质上就是通过 jsProcess.worker 向 http.sys.dweb 发送一个创建服务的消息
  // js 对 nmm 模块的访问方法1
  // appworker -> jsProcess.worker ->jsProcess.cts -> micro-module.js.cts 也就是 JsMicroModule 模块 -> 在转发给相应的 nmm 模块
  // 最终实现对 nmm 模块的访问

  const wwwServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "www",
    port: 443,
  });

  const apiServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "api",
    port: 443,
  });

  (await apiServer.listen()).onRequest(async (request, ipc) => {
    onApiRequest(apiServer.startResult.urlInfo, request, ipc);
  });

  // await sleep(5000)
  // await wwwServer.listen();
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
    // console.log(`${remoteIpcResponse.statusCode} ${JSON.stringify(remoteIpcResponse.headers.toJSON())}`)
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

  {
    const interUrl = wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
    }).href;
    mainUrl.resolve(interUrl);
    // console.log("cot#open interUrl=>", interUrl);
    // const view_id = await jsProcess
    //   .nativeFetch(
    //     `file://mwebview.sys.dweb/open?url=${encodeURIComponent(interUrl)}`
    //   )
    //   .text();

    // const windowHanlder = mwebview.openWindow(interUrl);
    // windowHanlder.onClose(() => {});

    // jsProcess.fetchIpc.onEvent((event) => {});
  }
  {
    // const mwebviewIpc = await jsProcess.connect("mwebview.sys.dweb");
    // Object.assign(globalThis, { mwebviewIpc });
    // mwebviewIpc.onEvent((event) => {
    //   console.log("got event:", event.name, event.text);
    //   if (event.name === "close") {
    //     wwwServer.close()
    //     apiServer.close()
    //   }
    // });
  }
};

main();
