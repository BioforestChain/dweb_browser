import { $Device } from "./bluetooth.type"
export class BluetoothPlugin {
  readonly tagName="std-bluetooth"
  mmid = "bluetooth.std.dweb"

  constructor(){
    console.log('bluetooth.plugin.ts')
  }

  toggle(isOpen: boolean){
    return isOpen ? this.open() : this.close()
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