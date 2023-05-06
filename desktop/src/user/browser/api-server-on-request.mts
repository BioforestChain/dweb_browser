
import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";
import type { ServerUrlInfo } from "../../sys/http-server/const.js"

const symbolETO = Symbol("***eto***");
const { IpcEvent, IpcResponse } = ipc
const MMID = "browser.sys.dweb"

export async function createApiServerOnRequest(www_server_internal_origin: string, apiServerUrlInfo: ServerUrlInfo){
  return async (ipcRequest: IpcRequest, ipc: Ipc): Promise<void> => {
    const pathname = ipcRequest.parsed_url.pathname;
    console.log('pathnaem: ', pathname, pathname === "/open")
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
  console.log('_url: ', _url)
  console.log('globalThis:', globalThis)
  const result = await jsProcess.nativeFetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(_url)}`).text()
  console.log('result: ', result)
} 

async function open_download(
  www_server_internal_origin: string,
  apiServerUrlInfo: ServerUrlInfo,
  ipcRequest: IpcRequest, 
  ipc: Ipc
){

  // 这里还是有问题，需要打开一个新的webview ？？ 
  // download.sys.dweb 同 新的webview de url 的服务
  // const _url = ipcRequest.parsed_url.searchParams.get('url');
  // if(_url === null) throw new Error(`${MMID} createApiServerOnRequest _url === null`)
  // console.log('_url: ', _url)
  // console.log('globalThis:', globalThis)
  const _url = `http://download.sys.dweb-80.localhost:22605`
  const result = await jsProcess.nativeFetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(_url)}`).text()
  // console.log('result: ', result)
} 

 