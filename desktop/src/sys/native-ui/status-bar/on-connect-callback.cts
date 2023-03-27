
import type { Ipc } from "../../../core/ipc/ipc.cjs";
import type { IpcEvent as $IpcEvent} from "../../../core/ipc/IpcEvent.cjs";
// import { IpcEvent } from "../../../core/ipc/IpcEvent.cjs";
import type{ NativeMicroModule } from "../../../core/micro-module.native.cjs";

export class AllConnects {
    constructor(readonly nmm: NativeMicroModule){
    }
    private _allConnects: Map<string, Ipc> = new Map()
    onConnect = (ipc: Ipc) => {
        ipc.onEvent((ipcEvent: $IpcEvent, nativeIpc: Ipc) => {
          let data: any
          if(typeof ipcEvent.data === "string"){
            data = JSON.parse(ipcEvent.data)
          }else{
            throw new Error(`status-bar.main.cts ipc.onEvent 还没有处理 ipcEvent.data ${ipcEvent.data}`)
          }
    
          if(data.action === "send/url"){
            this._allConnects.set(data.value, nativeIpc)
            return;
          }
          
        })
    }
} 
 