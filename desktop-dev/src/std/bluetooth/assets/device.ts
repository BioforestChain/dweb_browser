import { allDeviceListMap } from "./data.ts"
import type { $Device, $AllDeviceListItem } from "../types.ts";
import type { BluetoothDevice } from "electron";
 
export async function requestDevice(){
  console.log('触发了')
  const bluetoothDevice: BluetoothDevice = await (navigator as any).bluetooth.requestDevice({
    acceptAllDevices: true,
    optionalServices: [
      0x180F, /**获取电池信息*/
      0x1844, /**声音服务*/
    ]
  })
  
  bluetoothDevice.gatt.connect()
  .then(
    (server: any) => {
      console.log("链接成功了")
      Array.from(allDeviceListMap.values()).forEach((item: $AllDeviceListItem) => {
        if(item.isConnecting){
          item.isConnecting = false;
          item.el.classList.remove('connecting');
          item.isConnected = true;
          item.el.classList.add("connected")
        }else{
          item.el.classList.remove('connected');
        }
      })
    },
    (err: Error) => {
      console.log('连接失败了', err)
    }
  )
}