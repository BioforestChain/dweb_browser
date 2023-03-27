
import type { Ipc } from "../../../core/ipc/ipc.cjs";
import { IpcEvent } from "../../../core/ipc/IpcEvent.cjs";
 
// 拦截 htpp 请求 保持住res不返回
// 用来向 UI 发送消息

export function intercept(httpIpc: Ipc, mmid: $MMID){
    httpIpc
    .postMessage(
      IpcEvent
        .fromText(
          "http.sys.dweb", 
          JSON.stringify({
            action: "filter/request",
            host: mmid,
            urlPre: "/wait_for_operation"
          })
        )
    )
}