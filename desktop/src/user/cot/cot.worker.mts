const main = async () => {
  console.log("start");
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
      `file:///cot${pathname}?mode=stream`
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

    // console.time(`open file ${pathname}`);
    // const remoteIpcResponse = await jsProcess.nativeFetch(
    //   `file:///cot/COT-beta-202302222200${pathname}?mode=buffer`
    // );
    // console.timeEnd(`open file ${pathname}`);

    // ipc.postMessage(
    //   await IpcResponse.fromResponse(request.req_id, remoteIpcResponse, ipc)
    // );
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
