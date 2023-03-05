// src/user/cot/cot.worker.mts
var main = async () => {
  const { IpcResponse, IpcHeaders } = ipc;
  const wwwServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "www",
    port: 443
  });
  const apiServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "api",
    port: 443
  });
  (await apiServer.listen()).onRequest(async (request, ipc2) => {
    ipc2.postMessage(
      IpcResponse.fromText(request.req_id, 404, void 0, "forbidden", ipc2)
    );
  });
  (await wwwServer.listen()).onRequest(async (request, ipc2) => {
    let pathname = request.parsed_url.pathname;
    if (pathname === "/") {
      pathname = "/index.html";
    }
    if (pathname.startsWith("/assets/") === false) {
      pathname = "/locales/zh-Hans" + pathname;
    }
    console.time(`open file ${pathname}`);
    const remoteIpcResponse = await jsProcess.nativeFetch(
      `file:///cot/COT-beta-202302222200${pathname}?mode=buffer`
    );
    console.timeEnd(`open file ${pathname}`);
    ipc2.postMessage(
      await IpcResponse.fromResponse(request.req_id, remoteIpcResponse, ipc2)
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
};
main();
