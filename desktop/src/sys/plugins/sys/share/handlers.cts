import querystring from "querystring"
import type { Ipc, IpcRequest } from "../../../../core/ipc/index.cjs";
import type { $Schema1, $Schema1ToType } from "../../../../helper/types.cjs";
import type { ShareNMM } from "./share.main.cjs"

/**
 * 设置状态
 * @param this 
 * @param args 
 * @param client_ipc 
 * @param ipcRequest 
 * @returns 
 */
export async function share(
  this: ShareNMM,
  args: $Schema1ToType<{}>,
  client_ipc: Ipc, 
  ipcRequest: IpcRequest
){
  const host = ipcRequest.parsed_url.host;
  const pathname = ipcRequest.parsed_url.pathname;
  const search = ipcRequest.parsed_url.search;
  const url = `file://mwebview.sys.dweb/plugin/${host}${pathname}${search}`
  
  const stream = await ipcRequest.body.stream()
  
  if(stream instanceof ReadableStream){
    const reader =  stream.getReader()
    while(true) {
      const {value,done} = await reader.read()
      console.log('value: ', value)
      if(done){
        return
      }
    }
  }else{
    console.log('stream: ', stream)
  }

  debugger;
  const result = await this.nativeFetch(url,{
    body: await ipcRequest.body.stream(),
    headers: ipcRequest.headers,
    method: ipcRequest.method,
  })

  return result;
}

// 判断类型

function isReadableStream(o: unknown): o is ReadableStream{
  if((o as ReadableStream).getReader){
    return true
  }else{
    return false
  }
}
