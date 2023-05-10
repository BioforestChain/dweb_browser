// src/user/browser/www-server-on-request.mts
var { IpcResponse, IpcHeaders } = ipc;
async function wwwServerOnRequest(request, ipc2) {
  let pathname = request.parsed_url.pathname;
  pathname = pathname === "/" ? "/index.html" : pathname;
  const url = `file:///assets/html/browser.html?mode=stream`;
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

// src/user/browser/api-server-on-request.mts
var symbolETO = Symbol("***eto***");
var { IpcEvent, IpcResponse: IpcResponse2 } = ipc;
var MMID = "browser.sys.dweb";
async function createApiServerOnRequest(www_server_internal_origin, apiServerUrlInfo) {
  return async (ipcRequest, ipc2) => {
    const pathname = ipcRequest.parsed_url.pathname;
    switch (ipcRequest.parsed_url.pathname) {
      case "/open":
        open(
          www_server_internal_origin,
          apiServerUrlInfo,
          ipcRequest,
          ipc2
        );
        break;
      case "/open_download":
        open_download(
          www_server_internal_origin,
          apiServerUrlInfo,
          ipcRequest,
          ipc2
        );
        break;
    }
  };
}
async function open(www_server_internal_origin, apiServerUrlInfo, ipcRequest, ipc2) {
  const _url = ipcRequest.parsed_url.searchParams.get("url");
  if (_url === null)
    throw new Error(`${MMID} createApiServerOnRequest _url === null`);
  const result = await jsProcess.nativeFetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(_url)}`).text();
  ipc2.postMessage(
    await IpcResponse2.fromText(
      ipcRequest.req_id,
      200,
      void 0,
      result,
      ipc2
    )
  );
}
async function open_download(www_server_internal_origin, apiServerUrlInfo, ipcRequest, ipc2) {
  const metadataUrl = ipcRequest.parsed_url.searchParams.get("url");
  if (metadataUrl === null)
    throw new Error("metadataUrl === null");
  const appInfo = JSON.stringify(await (await fetch(metadataUrl)).json());
  const webview_url = `http://download.sys.dweb-80.localhost:22605`;
  const webview_id = await jsProcess.nativeFetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(webview_url)}`).text();
  const url = `file://mwebview.sys.dweb/webview_execute_javascript_by_webview_url?`;
  const init = {
    body: `
      (() => {
        setAppInfoByAppInfo('${appInfo}');
        globalThis.metadataUrl = "${metadataUrl}"
      })()
    `,
    method: "POST",
    headers: {
      "webview_url": webview_url
    }
  };
  const request = new Request(url, init);
  await jsProcess.nativeFetch(request);
  ipc2.postMessage(
    await IpcResponse2.fromText(
      ipcRequest.req_id,
      200,
      void 0,
      "ok",
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
