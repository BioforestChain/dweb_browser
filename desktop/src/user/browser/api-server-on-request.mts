import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";
import type { ServerUrlInfo } from "../../sys/http-server/const.js"

const symbolETO = Symbol("***eto***");
const { IpcEvent, IpcResponse } = ipc
const MMID = "browser.sys.dweb"

export async function createApiServerOnRequest(www_server_internal_origin: string, apiServerUrlInfo: ServerUrlInfo){
  return async (ipcRequest: IpcRequest, ipc: Ipc): Promise<void> => {
    const pathname = ipcRequest.parsed_url.pathname;
    switch(ipcRequest.parsed_url.pathname){
      case "/open":
        open(
          www_server_internal_origin, 
          apiServerUrlInfo,
          ipcRequest,
          ipc
        )
        break;
      case "/open_download":
        open_download(
          www_server_internal_origin, 
          apiServerUrlInfo,
          ipcRequest,
          ipc)
        break;
    }
  }
}

async function open(
  www_server_internal_origin: string,
  apiServerUrlInfo: ServerUrlInfo,
  ipcRequest: IpcRequest, 
  ipc: Ipc
){
  const _url = ipcRequest.parsed_url.searchParams.get('url');
  if(_url === null) throw new Error(`${MMID} createApiServerOnRequest _url === null`)
  const result = await jsProcess.nativeFetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(_url)}`).text()
  ipc.postMessage(
    await IpcResponse.fromText(
      ipcRequest.req_id,
      200,
      undefined,
      result,
      ipc, 
    )
  )
} 

async function open_download(
  www_server_internal_origin: string,
  apiServerUrlInfo: ServerUrlInfo,
  ipcRequest: IpcRequest, 
  ipc: Ipc
){

   
  const metadataUrl = ipcRequest.parsed_url.searchParams.get('url');
  if(metadataUrl === null) throw new Error('metadataUrl === null')

  const appInfo = JSON.stringify(await (await fetch(metadataUrl)).json())
  const webview_url = `http://download.sys.dweb-80.localhost:22605`
  const webview_id = await jsProcess.nativeFetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(webview_url)}`).text()
  // 向 webview 执行 javascript
  const url = `file://mwebview.sys.dweb/webview_execute_javascript_by_webview_url?`
   // setContentByMateDataJsonUrl('${metaDataJsonUrl}');
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
  }
  const request = new Request(url,init);
  await jsProcess.nativeFetch(request)
  ipc.postMessage(
    await IpcResponse.fromText(
      ipcRequest.req_id,
      200,
      undefined,
      "ok",
      ipc, 
    )
  )
} 

 