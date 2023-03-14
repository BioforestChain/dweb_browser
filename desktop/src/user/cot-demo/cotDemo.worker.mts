import { onApiRequest } from "./cotDemo.request.mjs";

const main = async () => {
  console.log("start cot-demo");
  const { IpcResponse, IpcHeaders } = ipc;
  const wwwServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "www",
    port: 443,
  });
  const apiServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "api",
    port: 443,
  });
  console.log(
    "will do listen!!",
    wwwServer.startResult.urlInfo.host,
    apiServer.startResult.urlInfo.host
  );
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
        remoteIpcResponse.headers,
        remoteIpcResponse.body,
        ipc
      )
    );
  });

  {
    const interUrl = wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
    }).href
    console.log("cot#open interUrl=>", interUrl)
    const view_id = await jsProcess
      .nativeFetch(
        `file://mwebview.sys.dweb/open?url=${encodeURIComponent(interUrl)}`
      )
      .text();
  }

  {
    // const http = await jsProcess.nativeFetch(`file://http.sys.dweb/listen`)

    // const pluginUrl = wwwServer.startResult.urlInfo.buildPublicUrl((url) => {
    // }).href
    // console.log("公共 url", pluginUrl)
  }
  {
    // const mwebviewIpc = await jsProcess.connect("mwebview.sys.dweb");
    // Object.assign(globalThis, { mwebviewIpc });
    // mwebviewIpc.onEvent((event) => {
    //   console.log("got event:", event.name, event.text);
    // });
  }
};

main();
