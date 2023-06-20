import { $Device, BluetoothDevice } from "./bluetooth.type.ts"
export class BluetoothPlugin {
  readonly tagName="std-bluetooth"
  mmid = "bluetooth.std.dweb"

  constructor(){
    console.log('bluetooth.plugin.ts')
  }

  async toggle(isOpen: boolean){
    if(isOpen){
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
          return server;
          // Array.from(allDeviceListMap.values()).forEach((item: $AllDeviceListItem) => {
          //   if(item.isConnecting){
          //     item.isConnecting = false;
          //     item.el.classList.remove('connecting');
          //     item.isConnected = true;
          //     item.el.classList.add("connected")
          //   }else{
          //     item.el.classList.remove('connected');
          //   }
          // })
        },
        (err: Error) => {
          console.log('连接失败了', err)
          return undefined
        }
      )
    }else{
      return undefined
    }
  }

  open(){
    const {baseurl, xDwebHost} = this.apiUrlGet();
    const url = `${baseurl}/open?x-dweb-host=${xDwebHost}`
    return  fetch(url)
  }

  close(){
    const {baseurl, xDwebHost} = this.apiUrlGet();
    const url = `${baseurl}/close?x-dweb-host=${xDwebHost}`
    return fetch(url)
  }

  selected(device: $Device){
    const {baseurl, xDwebHost} = this.apiUrlGet();
    const url = `${baseurl}/selected`
    return fetch(`${url}?x-dweb-host=${xDwebHost}&device_id=${device.deviceId}&device_name=${device.deviceName}`)
  }

  apiUrlGet(){
    const host = location.host.replace("www.", "api.");
    const baseurl = `http://${host}/${this.mmid}`
    const xDwebHost = host.split("-")[0]
    return {baseurl, xDwebHost}
  }
}

// http://
// api.app.plaoc.dweb-443.localhost:22605/
// status-bar.nativeui.browser.dweb
// /setState
// ?X-Dweb-Host=api.app.plaoc.dweb 
export const bluetoothPlugin = new BluetoothPlugin();