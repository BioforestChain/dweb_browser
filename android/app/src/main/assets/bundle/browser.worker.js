// src/user/browser/www-server-on-request.mts
var { IpcResponse, IpcHeaders } = ipc;
async function wwwServerOnRequest(request, ipc2) {
  let pathname = request.parsed_url.pathname;
  pathname = pathname === "/" ? "/index.html" : pathname;
  const url = `file:///app/cot-demo${pathname}?mode=stream`;
  const response = await jsProcess.nativeRequest(url);
  ipc2.postMessage(
    new IpcResponse(
      request.req_id,
      response.statusCode,
      response.headers,
      response.body,
      ipc2
    )
  );
}

// src/helper/devtools.cts
var Log = class {
  log(str) {
    console.log(str);
  }
  red(str) {
    console.log(`\x1B[31m%s\x1B[0m`, str);
  }
  green(str) {
    console.log(`\x1B[32m%s\x1B[0m`, str);
  }
  yellow(str) {
    console.log(`\x1B[33m%s\x1B[0m`, str);
  }
  blue(str) {
    console.log(`\x1B[34m%s\x1B[0m`, str);
  }
  // 品红色
  magenta(str) {
    console.log(`\x1B[35m%s\x1B[0m`, str);
  }
  cyan(str) {
    console.log(`\x1B[36m%s\x1B[0m`, str);
  }
  grey(str) {
    console.log(`\x1B[36m%s\x1B[0m`, str);
  }
};
var log = new Log();

// src/user/browser/api-server-on-request.mts
var symbolETO = Symbol("***eto***");
var { IpcEvent, IpcResponse: IpcResponse2 } = ipc;
async function createApiServerOnRequest(www_server_internal_origin, apiServerUrlInfo) {
  return async (ipcRequest, ipc2) => {
    apiServerOnRequest(ipcRequest, ipc2, www_server_internal_origin, apiServerUrlInfo);
  };
}
async function apiServerOnRequest(ipcRequest, ipc2, www_server_internal_origin, apiServerUrlInfo) {
  const pathname = ipcRequest.parsed_url.pathname;
  console.log("api-server-on-request.mts", ipcRequest.parsed_url);
  switch (pathname) {
    case (pathname.startsWith("/internal") ? pathname : symbolETO):
      apiServerOnRequestInternal(ipcRequest, ipc2, www_server_internal_origin, apiServerUrlInfo);
      break;
    default:
      throw new Error(`[\u7F3A\u5C11\u5904\u7406\u5668] ${ipcRequest.parsed_url}`);
  }
}
async function apiServerOnRequestInternal(ipcRequest, ipc2, www_server_internal_origin, apiServerUrlInfo) {
  const pathname = ipcRequest.parsed_url.pathname;
  switch (pathname) {
    case "/internal/public-url":
      apiServerOnRequestInternalPublicUrl(ipcRequest, ipc2, www_server_internal_origin, apiServerUrlInfo);
      break;
    default:
      throw new Error(`[\u7F3A\u5C11\u5904\u7406\u5668] ${ipcRequest.parsed_url}`);
  }
}
async function apiServerOnRequestInternalPublicUrl(ipcRequest, ipc2, www_server_internal_origin, apiServerUrlInfo) {
  const ipcResponse = IpcResponse2.fromText(
    ipcRequest.req_id,
    200,
    void 0,
    apiServerUrlInfo.buildPublicUrl(() => {
    }).href,
    ipc2
  );
  ipcResponse.headers.init("Access-Control-Allow-Origin", "*");
  ipcResponse.headers.init("Access-Control-Allow-Headers", "*");
  ipcResponse.headers.init("Access-Control-Allow-Methods", "*");
  ipc2.postMessage(ipcResponse);
}
var { IpcHeaders: IpcHeaders2 } = ipc;

// src/user/tool/tool.native.mts
var nativeOpen = async (url) => {
  return await jsProcess.nativeFetch(
    `file://mwebview.sys.dweb/open?url=${encodeURIComponent(url)}`
  ).text();
};

// src/user/browser/browser.worker.mts
var main = async () => {
  log.green("[browser.worker.mts bootstrap]");
  const { IpcEvent: IpcEvent2 } = ipc;
  const wwwServer = await http.createHttpDwebServer(jsProcess, { subdomain: "www", port: 443 });
  const apiServer = await http.createHttpDwebServer(jsProcess, { subdomain: "api", port: 443 });
  ;
  (await wwwServer.listen()).onRequest(wwwServerOnRequest);
  (await apiServer.listen()).onRequest(await createApiServerOnRequest(wwwServer.startResult.urlInfo.internal_origin, apiServer.startResult.urlInfo));
  {
    const interUrl = wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
    }).href;
    console.log("cot#open interUrl=>", interUrl);
    const view_id = await nativeOpen(interUrl);
  }
  {
    jsProcess.onConnect((ipc2) => {
      console.log("browser.worker.mts onConnect");
      ipc2.onEvent((event, ipc3) => {
        console.log("got event:", ipc3.remote.mmid, event.name, event.text);
        setTimeout(() => {
          ipc3.postMessage(IpcEvent2.fromText(event.name, "echo:" + event.text));
        }, 500);
      });
      ipc2.onMessage(() => {
        console.error("ipc onmessage");
      });
    });
  }
};
main();
