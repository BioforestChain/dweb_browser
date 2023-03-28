
import type { Ipc } from "../../../core/ipc/ipc.cjs";
import type { IpcEvent as $IpcEvent} from "../../../core/ipc/IpcEvent.cjs";
export class AllConnects {
    allConnects: Map<string, Ipc> = new Map()
    onConnect = (ipc: Ipc) => {
      console.log('status-bar.nativeui.sys.dweb 接收到到消息:',)
        ipc.onEvent((ipcEvent: $IpcEvent, nativeIpc: Ipc) => {
          let data: any
          if(typeof ipcEvent.data === "string"){
            data = JSON.parse(ipcEvent.data)
          }else{
            throw new Error(`status-bar.main.cts ipc.onEvent 还没有处理 ipcEvent.data ${ipcEvent.data}`)
          }
    
          if(data.action === "send/url"){
            this.allConnects.set(data.value, nativeIpc)
            return;
          }
          
        })
    }
} 
 