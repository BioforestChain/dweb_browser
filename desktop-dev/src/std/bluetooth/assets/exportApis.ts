
import "../index.d.ts";
import type { $Device} from "../types.ts";

import { mainApis } from "../../../helper/openNativeWindow.preload.ts"
import { allDeviceListMap } from "./data.ts"

const template: HTMLTemplateElement | null = document.querySelector(".template");
let setTimeoutId: number;
let bluetooth : BluetoothDevice
let bluetoothRemoteGATTServer: BluetoothRemoteGATTServer | undefined
let connectedSuccess: {(server: BluetoothRemoteGATTServer): void} | undefined;
let connectedFail: {(): void} | undefined;

/**
 * 
 * @returns Promise<null | Error>
 */
async function requestDevice(requestDeviceOptions: RequestDeviceOptions){
  // const requestDeviceOptions = JSON.parse(requestDeviceOptionsStr);
  // bluetooth = await navigator.bluetooth.requestDevice(requestDeviceOptions)
  // bluetoothRemoteGATTServer = await bluetooth.gatt?.connect()
  // console.log('bluetooth: ', bluetooth, bluetoothRemoteGATTServer)
  navigator.bluetooth.requestDevice(requestDeviceOptions)
  .then((_bluetooth) => {
    if(_bluetooth !== undefined){
      bluetooth = _bluetooth
      return bluetooth.gatt?.connect();
    }
  })
  .then((server: BluetoothRemoteGATTServer | undefined) => {
    console.log("server", server)
    bluetoothRemoteGATTServer = server;
    if(connectedSuccess === undefined) throw new Error(`connectedSuccess === undefined`);
    connectedSuccess(server)
    clearTimeout(setTimeoutId)
  })
  .catch(err => {
    if(connectedFail === undefined) throw new Error(`connectedFail === undefined`)
    connectedFail();
    clearTimeout(setTimeoutId)
    console.error(`requestDevice fail: `, err)
  })
}

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
      console.log('click device: ', device)
      ;(mainApis as any).deviceSelected(device);
      Array.from(allDeviceListMap.values()).forEach(oldDevice => {
        if(oldDevice.device.deviceId === device.deviceId){
          li.classList.remove('connected');
          li.classList.add('connecting')
          oldDevice.isConnecting = true;
          oldDevice.isConnected = false;

          connectedFail = () => {
            oldDevice.el.classList.remove('connecting')
            oldDevice.isConnecting = false;
          }
          
          // 超时设置
          setTimeoutId = setTimeout(() => {
            oldDevice.el.classList.remove('connecting')
            oldDevice.isConnecting = false;
            console.error("连接超时")
          },6000)

          connectedSuccess = (server: BluetoothRemoteGATTServer) => {
            oldDevice.el.classList.remove("connecting")
            oldDevice.el.classList.add("connected")
            oldDevice.isConnecting = false;
            oldDevice.isConnected = true;
            ;(mainApis as any).deviceConnectedSuccess({ device: {id: server.device.id, name: server.device.name}});
            
          }
        }else{
          oldDevice.el.classList.remove('connecting')
          oldDevice.el.classList.remove('connected');
          oldDevice.isConnecting = false;
          oldDevice.isConnected = false;
        }
      })
    })
    // 添加
    allDeviceListMap.set(device.deviceId, {el: li, device: device, isConnecting: false, isConnected: false})
    ul.appendChild(li)
  })
}

export const APIS = {
  devicesUpdate,
  // deviceSelected,
  requestDevice,
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