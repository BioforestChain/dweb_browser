

import { $Device } from "../bluetooth.main.ts";
import { mainApis } from "../../../helper/openNativeWindow.preload.ts"
// import { requestDevice } from "./device.ts"
// import type Electron from "electron";

let bluetoothDevice;
const _map = new Map<string, {el: HTMLLIElement, device: $Device}>()
async function devicesUpdate(list: $Device[]){
  console.log("接受到了额消息222")
  const ul = document.querySelector('ul')
  if(ul === null) throw new Error('ul === null');
  
  // 从 已有的中间删除
  // Array.from(_map.values()).forEach(oldDevice => {
  //   if(list.findIndex(device => device.deviceId === oldDevice.device.deviceId) === -1){
  //     _map.delete(oldDevice.device.deviceId)
  //     oldDevice.el.remove();
  //   }
  // })

  // 添加新的
  const fragment = document.createDocumentFragment()
  list.forEach(device => {
    if(_map.has(device.deviceId)){
      return;
    }
    const li = document.createElement('li')
    li.innerText = `${device.deviceName}`
    li.classList.add('option')
    li.addEventListener('click', async () => {
      ;(mainApis as any).deviceSelected(device);
      Array.from(_map.values()).forEach(oldDevice => {
        oldDevice.el.classList.remove('active')
      })
      li.classList.add('active')
    })
    fragment.appendChild(li)
    // 添加
    _map.set(device.deviceId, {el: li, device: device})
  })
  ul.appendChild(fragment)
}

export const APIS = {
  devicesUpdate,
};
Object.assign(globalThis, APIS);

if ("ipcRenderer" in self) {
  (async () => {
    const { exportApis } = await import(
      "../../../helper/openNativeWindow.preload.ts"
    );
    exportApis(globalThis);
  })();
}