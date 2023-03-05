const main = async () => {
  const { IpcResponse, IpcHeaders } = ipc;
  const wwwServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "www",
    port: 443,
  });
  const apiServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "api",
    port: 443,
  });
  (await apiServer.listen()).onRequest(async (request, ipc) => {
    ipc.postMessage(
      IpcResponse.fromText(request.req_id, 404, undefined, "forbidden", ipc)
    );
  });

  (await wwwServer.listen()).onRequest(async (request, ipc) => {
    let pathname = request.parsed_url.pathname;
    if (pathname === "/") {
      pathname = "/index.html";
    }
    if (pathname.startsWith("/assets/") === false) {
      pathname = "/locales/zh-Hans" + pathname;
    }
    console.time(`open file ${pathname}`);
    const remoteIpcResponse = await jsProcess.nativeRequest(
      `file:///cot/COT-beta-202302222200${pathname}`
    );
    console.timeEnd(`open file ${pathname}`);

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
    const view_id = await jsProcess
      .nativeFetch(
        `file://mwebview.sys.dweb/open?url=${encodeURIComponent(
          wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
            url.pathname = "/index.html";
          }).href
        )}`
      )
      .text();
  }
};

main();
