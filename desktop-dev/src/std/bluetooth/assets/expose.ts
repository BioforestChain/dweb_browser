// import { $Device } from "../bluetooth.main.ts";
// import type Electron from "electron";
// import {
//   mainApis,
// } from "../../../helper/openNativeWindow.preload.ts";

// let bluetoothDevice;
// async function devicesUpdate(list: $Device[]){
//   const ul = document.querySelector('ul')
//   if(ul === null) throw new Error('ul === null');
//   const fragment = document.createDocumentFragment()
//     list.forEach(device => {
//       const li = document.createElement('li')
//       li.innerText = `${device.deviceName}`
//       li.classList.add('option')
//       li.addEventListener('click', async () => {
//         // console.log('点击了： ', mainApis)
//         // const bluetoothDevice = await bluetooth.selected(device)
//         // console.log("bluetoothDevice: ", bluetoothDevice)
//         // const server = await bluetoothDevice.gatt.connect();
//         // console.log('连接完成')
//         // const service = await server.getPrimaryService(0x180F);
//         // console.log('获取到服务')
//         // const characteristic = await service.getCharacteristic(0x2A19);
//         // console.log('获取到特征')
//         // const batteryLevel = await characteristic.readValue();
//         // console.log('batteryLevel: ', batteryLevel.getUint8(0))

//         // console.log('连接完成')
//         // // 查询 Services by Name 的列表可以知道标准服务
//         // const service = await server.getPrimaryService(0x1844);
//         // console.log('获取到服务')
//         // // 查询 Characteristics by Name 的列表可以知道特征
//         // const characteristic = await service.getCharacteristic(0x2B7D);
//         // console.log('获取到特征')
//         // const batteryLevel = await characteristic.readValue();
//         // console.log('batteryLevel: ', batteryLevel.getUint8(0))
//       })
//       fragment.appendChild(li)
//     })

//     ul.innerHTML = "";
//     ul.appendChild(fragment)
 
// }

// export const APIS = {
//   devicesUpdate,
// };
// Object.assign(globalThis, APIS);

 