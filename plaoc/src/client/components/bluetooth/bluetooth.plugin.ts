import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import "../../@types/web-bluetooth/index.d.ts"

export class BluetoothPlugin extends BasePlugin{
  constructor(){
    super("bluetooth.std.dweb");
  }

  requestDevice = async() => {
    const bluetoothDevice = await navigator.bluetooth.requestDevice({
      acceptAllDevices: true,
      optionalServices: [
        0x180F, /**获取电池信息*/
        0x1844, /**声音服务*/
      ]
    })
    console.log('bluetoothDevice: ', bluetoothDevice)
    return bluetoothDevice
  }

  @bindThis
  requestDeviceCancel(){
    return this.fetchApi("/close")
  }
  
  // toggle = async (isOpen: boolean) => {
  //   if(isOpen){
  //     const bluetoothDevice = await navigator.bluetooth.requestDevice({
  //       acceptAllDevices: true,
  //       optionalServices: [
  //         0x180F, /**获取电池信息*/
  //         0x1844, /**声音服务*/
  //       ]
  //     })
  //     console.log('bluetoothDevice: ', bluetoothDevice)
  //     return bluetoothDevice
  //   }else{ // 关闭 bluetooth
  //     this.close()
  //     return undefined
  //   }
  // }

  @bindThis
  open(){
    // this.fetchApi("/observe")
    // .then(async (res) => {
    //   const readableStream = res.body;
    //   const reader = readableStream?.getReader()
    //   if(reader === undefined) throw new Error('reader === undefined');
    //   let loop = true
    //   while(loop){
    //     const { value, done} = await reader.read();
    //     if(value){
    //       console.log('value: ', value)
    //     }
    //     loop = !done
    //   }
    // })

    // const {baseurl, xDwebHost} = this.apiUrlGet();
    // const url = `${baseurl}/open?x-dweb-host=${xDwebHost}`
    // return  fetch(url)
  }
 
  @bindThis
  close(){
    return this.fetchApi("/close")
  }

  // selected(device: $Device){
  //   const {baseurl, xDwebHost} = this.apiUrlGet();
  //   const url = `${baseurl}/selected`
  //   return fetch(`${url}?x-dweb-host=${xDwebHost}&device_id=${device.deviceId}&device_name=${device.deviceName}`)
  // }

  // apiUrlGet(){
  //   const host = location.host.replace("www.", "api.");
  //   const baseurl = `http://${host}/${this.mmid}`
  //   const xDwebHost = host.split("-")[0]
  //   return {baseurl, xDwebHost}
  // }
}

// http://
// api.app.plaoc.dweb-443.localhost:22605/
// status-bar.nativeui.browser.dweb
// /setState
// ?X-Dweb-Host=api.app.plaoc.dweb 
export const bluetoothPlugin = new BluetoothPlugin();