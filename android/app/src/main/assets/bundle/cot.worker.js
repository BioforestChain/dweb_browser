// src/user/cot/cot.worker.mts
var main = async () => {
  console.log("start");
  const { IpcResponse, IpcHeaders } = ipc;
  const wwwServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "www",
    port: 443
  });
  const apiServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "api",
    port: 443
  });
  console.log("will do listen!!", wwwServer.startResult.urlInfo.host, apiServer.startResult.urlInfo.host);
  (await apiServer.listen()).onRequest(async (request, ipc2) => {
    console.log("\u63A5\u53D7\u5230\u4E86\u8BF7\u6C42 apiServer\uFF1A request.parsed_url.pathname\uFF1A ", request.parsed_url.pathname);
    ipc2.postMessage(
      IpcResponse.fromText(request.req_id, 404, void 0, "forbidden", ipc2)
    );
  });
  (await wwwServer.listen()).onRequest(async (request, ipc2) => {
    console.log("\u63A5\u53D7\u5230\u4E86\u8BF7\u6C42 wwwServer request.parsed_url.pathname\uFF1A ", request.parsed_url.pathname);
    let pathname = request.parsed_url.pathname;
    if (pathname === "/") {
      pathname = "/index.html";
    }
    if (pathname.startsWith("/assets/") === false) {
      pathname = "/locales/zh-Hans" + pathname;
    }
    console.time(`open file ${pathname}`);
    const remoteIpcResponse = await jsProcess.nativeRequest(
      `file:///cot${pathname}?mode=stream`
    );
    console.timeEnd(`open file ${pathname}`);
    ipc2.postMessage(
      new IpcResponse(
        request.req_id,
        remoteIpcResponse.statusCode,
        remoteIpcResponse.headers,
        remoteIpcResponse.body,
        ipc2
      )
    );
  });
  {
    const view_id = await jsProcess.nativeFetch(
      `file://mwebview.sys.dweb/open?url=${encodeURIComponent(
        wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
          url.pathname = "/index.html";
        }).href
      )}`
    ).text();
  }
  {
    const mwebviewIpc = await jsProcess.connect("mwebview.sys.dweb");
    Object.assign(globalThis, { mwebviewIpc });
    mwebviewIpc.onEvent((event) => {
      console.log("got event:", event.name, event.text);
    });
  }
};
main();
