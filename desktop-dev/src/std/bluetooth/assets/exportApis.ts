

import { $Device } from "../bluetooth.main.ts";
import { mainApis } from "../../../helper/openNativeWindow.preload.ts"
// import { requestDevice } from "./device.ts"
// import type Electron from "electron";

const template: HTMLTemplateElement | null = document.querySelector(".template");
/**
 * 创建节点
 */ 
function createListItem(name: string, status: string): HTMLLIElement{
  if(template === null) throw new Error('tempalte === null');
  const fragment = template.content.cloneNode(true) as DocumentFragment;
  (fragment.querySelector('.name') as HTMLElement).innerText = name;
  (fragment.querySelector('.status') as HTMLElement).innerText = status;
  return fragment.children[0] as HTMLLIElement
}

let bluetoothDevice;
const _map = new Map<string, {el: HTMLLIElement, device: $Device}>()
async function devicesUpdate(list: $Device[]){
  const ul = document.querySelector('.list_container');
    
  if(ul === null) throw new Error('ul === null');
  
  // 从 已有的中间删除
  // Array.from(_map.values()).forEach(oldDevice => {
  //   if(list.findIndex(device => device.deviceId === oldDevice.device.deviceId) === -1){
  //     _map.delete(oldDevice.device.deviceId)
  //     oldDevice.el.remove();
  //   }
  // })

  // 添加新的
  list.forEach(device => {
    if(_map.has(device.deviceId)){
      return;
    }
    const li = createListItem(device.deviceName, "未连接");
    li.addEventListener('click', async () => {
      ;(mainApis as any).deviceSelected(device);
      Array.from(_map.values()).forEach(oldDevice => {
        oldDevice.el.classList.remove('selected')
      })
      li.classList.add('connecting')
    })
    // 添加
    _map.set(device.deviceId, {el: li, device: device})
    ul.appendChild(li)
  })
  
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