import type { IpcRenderer } from "electron";

export interface $AppMetaData {
  title: string;
  short_name: string;
  id: string;
  icon: string;
}
declare global {
  interface Window  {
    electron: {
      messageSend(...args: unknown[]): void,
      messageOn(callback: $Callback): void
      on: IpcRenderer["on"]
    }
  }
}
 
interface $Callback{
  (event: Event, type: string, ...args: unknown[]): void
}
 


