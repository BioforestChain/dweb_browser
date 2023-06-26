

import { $Device } from "../types.ts";
import { mainApis } from "../../../helper/openNativeWindow.preload.ts"
import { allDeviceListMap } from "./data.ts"

const template: HTMLTemplateElement | null = document.querySelector(".template");
let setTimeoutId: number;
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

async function devicesUpdate(list: $Device[]){
  const ul = document.querySelector('.list_container');
  if(ul === null) throw new Error('ul === null');
  
  // 添加新的
  list.forEach(device => {
    if(allDeviceListMap.has(device.deviceId)){
      return;
    }
    const li = createListItem(device.deviceName, "未连接");
    li.addEventListener('click', async () => {
      ;(mainApis as any).deviceSelected(device);
      Array.from(allDeviceListMap.values()).forEach(oldDevice => {
        if(oldDevice.device.deviceId === device.deviceId){
          li.classList.add('connecting')
          oldDevice.isConnecting = true;
        }else{
          oldDevice.el.classList.remove('connecting')
          oldDevice.isConnecting = false;
        }

        // 超时设置
        setTimeoutId = setTimeout(() => {
          oldDevice.el.classList.remove('connecting')
          oldDevice.isConnecting = false;
          // 超时提示框
          console.error('选择失败')
           
        },6000)
      })
    })
    // 添加
    allDeviceListMap.set(device.deviceId, {el: li, device: device, isConnecting: false, isConnected: false})
    ul.appendChild(li)
  })
}

const deviceSelected = async (device?:$Device) => {
  if(device === undefined) return;
  const deviceListItem = allDeviceListMap.get(device.deviceId)
  if(deviceListItem === undefined) throw new Error(`deviceListItem === undefined`);
  deviceListItem.el.classList.remove("connecting")
  deviceListItem.el.classList.add("connected")
  deviceListItem.isConnecting = false;
  deviceListItem.isConnected = true;
  clearTimeout(setTimeoutId);
}

export const APIS = {
  devicesUpdate,
  deviceSelected,
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