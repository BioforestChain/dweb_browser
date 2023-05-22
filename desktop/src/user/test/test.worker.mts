import { nativeOpen } from "../tool/tool.native.mjs";
;(async () => {
  const server = await http.createHttpDwebServer(jsProcess,{subdomain: "www", port: 443});
  const streamIpc = await server.listen();
  const { IpcHeaders, IpcResponse, IpcStreamData, IpcStreamEnd } = ipc;
  streamIpc.onRequest(async (request, ipc) => {
    if (
      request.parsed_url.pathname === "/" ||
      request.parsed_url.pathname === "/index.html"
    ) {
      const response = await IpcResponse.fromText(
        request.req_id,
        200,
        new IpcHeaders({
          "Content-Type": "text/html",
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Headers": "*", // 要支持 X-Dweb-Host
          "Access-Control-Allow-Methods": "*",
        }),
        `<html><div>test--  aaaaaaa-------->>>>>>>>>>>>>>>>>1000</div><html>`,
        ipc
      )
      ipc.postMessage(
        response
      )
      return ;
    } 
    ipc.postMessage(
      IpcResponse.fromText(
        request.req_id,
        404,
        undefined,
        "No Found",
        ipc
      )
    );
  })

  const main_url = server.startResult.urlInfo.buildInternalUrl("/index.html").href;
  await nativeOpen(main_url)
})();

setTimeout(() => {
 
}, 1000)
