// src/user/desktop/assets/index.html.cts
var html = String.raw;

// src/user/desktop/desktop.worker.mts
console.log("ookkkkk, i'm in worker");
var main = async () => {
<<<<<<< HEAD
  const { IpcHeaders, IpcResponse } = ipc;
  const { createHttpDwebServer } = http;
  debugger;
  const httpDwebServer = await createHttpDwebServer(jsProcess, {});
  if (jsProcess.meta.optionalBoolean("debug")) {
    await new Promise((resolve) => {
      Object.assign(self, { start_main: resolve });
    });
  }
  console.log("will do listen!!", httpDwebServer.startResult.urlInfo.host);
  (await httpDwebServer.listen()).onRequest(async (request, httpServerIpc) => {
    console.log("worker on request", request.parsed_url);
    if (request.parsed_url.pathname === "/" || request.parsed_url.pathname === "/index.html") {
      console.log("request body text:", await request.body.text());
      httpServerIpc.postMessage(
        IpcResponse.fromText(
          request.req_id,
          200,
          new IpcHeaders({
            "Content-Type": "text/html",
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Headers": "*",
            // 要支持 X-Dweb-Host
            "Access-Control-Allow-Methods": "*"
          }),
          await CODE2(request),
          httpServerIpc
        )
      );
    } else if (request.parsed_url.pathname === "/desktop.web.mjs") {
      httpServerIpc.postMessage(
        IpcResponse.fromText(
          request.req_id,
          200,
          new IpcHeaders({
            "Content-Type": "application/javascript"
          }),
          await CODE(request),
          httpServerIpc
        )
      );
    } else {
      httpServerIpc.postMessage(
        IpcResponse.fromText(
          request.req_id,
          404,
          void 0,
          "No Found",
          httpServerIpc
        )
      );
    }
  });
  console.log("http \u670D\u52A1\u521B\u5EFA\u6210\u529F");
  const main_url = httpDwebServer.startResult.urlInfo.buildInternalUrl("/index.html").href;
  console.log("\u8BF7\u6C42\u6D4F\u89C8\u5668\u9875\u9762", main_url);
  const response = await jsProcess.nativeFetch(main_url);
  console.log("html content:", response.status, await response.text());
  console.log("\u6253\u5F00\u6D4F\u89C8\u5668\u9875\u9762", main_url);
  {
    const view_id = await jsProcess.nativeFetch(
      `file://mwebview.sys.dweb/open?url=${encodeURIComponent(main_url)}`
    ).text();
  }
=======
>>>>>>> 43b5766 (临时保存   IpcResponse.fromResponse 问题)
};
main().catch(console.error);
export {
  main
};
