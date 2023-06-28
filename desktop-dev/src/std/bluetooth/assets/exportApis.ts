import "../index.d.ts";
import type { $Device } from "../types.ts";

import { mainApis } from "../../../helper/openNativeWindow.preload.ts";
import { allDeviceListMap } from "./data.ts";

const template: HTMLTemplateElement | null =
  document.querySelector(".template");
let setTimeoutId: number;
let bluetooth: BluetoothDevice;
let bluetoothRemoteGATTServer: BluetoothRemoteGATTServer | undefined;
let connectedSuccess: { (server: BluetoothRemoteGATTServer): void } | undefined;
let connectedFail: { (): void } | undefined;
let isConnecting: boolean = false; // 当前是否在连接 设备状态
let preRequestDeviceOption: RequestDeviceOptions = { acceptAllDevices: true };
let preSelected: { (list: $Device[]): void } | undefined;

/**
 *
 * @returns Promise<null | Error>
 */
async function requestDevice(requestDeviceOptions: RequestDeviceOptions) {
  preRequestDeviceOption = requestDeviceOptions;
  // const requestDeviceOptions = JSON.parse(requestDeviceOptionsStr);
  // bluetooth = await navigator.bluetooth.requestDevice(requestDeviceOptions)
  // bluetoothRemoteGATTServer = await bluetooth.gatt?.connect()
  // console.log('bluetooth: ', bluetooth, bluetoothRemoteGATTServer)
  navigator.bluetooth
    .requestDevice(requestDeviceOptions)
    .then((_bluetooth) => {
      if (_bluetooth !== undefined) {
        bluetooth = _bluetooth;
        return bluetooth.gatt?.connect();
      }
    })
    .then((server: BluetoothRemoteGATTServer | undefined) => {
      console.log("server", server);
      bluetoothRemoteGATTServer = server;
      if (connectedSuccess === undefined)
        throw new Error(`connectedSuccess === undefined`);
      if (server === undefined) throw new Error("server === undefined");
      connectedSuccess(server);
      clearTimeout(setTimeoutId);
    })
    .catch((err) => {
      if (connectedFail === undefined)
        throw new Error(`connectedFail === undefined`);
      connectedFail();
      clearTimeout(setTimeoutId);
      console.error(`requestDevice fail: `, err);
    });
}

/**
 * 蓝牙设备断开连接
 * @param id
 * @returns
 */
async function deviceDisconnect(id: string) {
  if (bluetoothRemoteGATTServer === undefined) {
    (mainApis as any).deviceDisconnectCallback({
      success: false,
      error: new Error(`bluetoothRemoteGATTServer === undefined`),
    });
    return;
  }
  if (id === bluetoothRemoteGATTServer?.device.id) {
    bluetoothRemoteGATTServer.disconnect();
    (mainApis as any).deviceDisconnectCallback({
      success: true,
      error: undefined,
    });

    Array.from(allDeviceListMap.values()).forEach((item) => {
      item.el.classList.remove("connected");
      item.isConnected = false;
    });

    bluetoothRemoteGATTServer = undefined;
    return;
  }
  (mainApis as any).deviceDisconnectCallback({
    success: false,
    error: new Error(`bluetoothRemoteGATTServer.id !== 请求的id`),
  });
}

/**
 * 创建节点
 */
function createListItem(name: string, status: string): HTMLLIElement {
  if (template === null) throw new Error("tempalte === null");
  const fragment = template.content.cloneNode(true) as DocumentFragment;
  (fragment.querySelector(".name") as HTMLElement).innerText = name;
  (fragment.querySelector(".status") as HTMLElement).innerText = status;
  return fragment.children[0] as HTMLLIElement;
}

async function devicesUpdate(list: $Device[]) {
  const ul = document.querySelector(".list_container");
  if (ul === null) throw new Error("ul === null");

  if (preSelected !== undefined) {
    preSelected(list);
  }

  // 添加新的
  list.forEach((device) => {
    if (allDeviceListMap.has(device.deviceId)) {
      return;
    }
    const li = createListItem(device.deviceName, "未连接");
    li.addEventListener("click", async () => {
      console.log("点击了 li");
      // 点击连接在返回回来之前是不能够再次点击的
      if (isConnecting) return;

      deviceSelected(device);

      // 只有第一次点击 preSelected === undefined
      if (preSelected === undefined) {
        (mainApis as any).deviceSelected(device);
      } else {
        requestDevice(preRequestDeviceOption);
      }

      preSelected = (list: $Device[]) => {
        const item = list.find((item) => item.deviceId == device.deviceId);
        if (item === undefined) return;
        (mainApis as any).deviceSelected(device);
      };
    });
    // 添加
    allDeviceListMap.set(device.deviceId, {
      el: li,
      device: device,
      isConnecting: false,
      isConnected: false,
    });
    ul.appendChild(li);
  });
}

async function deviceSelected(device: $Device) {
  // (mainApis as any).deviceSelected(device);
  isConnecting = true;
  Array.from(allDeviceListMap.values()).forEach((oldDevice) => {
    if (oldDevice.device.deviceId === device.deviceId) {
      oldDevice.el.classList.remove("connected");
      oldDevice.el.classList.add("connecting");
      oldDevice.isConnecting = true;
      oldDevice.isConnected = false;

      connectedFail = () => {
        oldDevice.el.classList.remove("connecting");
        oldDevice.isConnecting = false;
        isConnecting = false;
      };

      // 超时设置
      setTimeoutId = setTimeout(() => {
        oldDevice.el.classList.remove("connecting");
        oldDevice.isConnecting = false;
        isConnecting = false;
        console.error("连接超时");
      }, 6000);

      connectedSuccess = (server: BluetoothRemoteGATTServer) => {
        oldDevice.el.classList.remove("connecting");
        oldDevice.el.classList.add("connected");
        oldDevice.isConnecting = false;
        oldDevice.isConnected = true;
        isConnecting = false;
        (mainApis as any).deviceConnectedSuccess({
          device: { id: server.device.id, name: server.device.name },
        });
      };
    } else {
      oldDevice.el.classList.remove("connecting");
      oldDevice.el.classList.remove("connected");
      oldDevice.isConnecting = false;
      oldDevice.isConnected = false;
    }
  });
}

async function deviceSelectedCallback() {
  Array.from(allDeviceListMap.values()).forEach((item) => {
    item.el.classList.remove("connecting");
    item.isConnecting = false;
  });
}

export const APIS = {
  devicesUpdate,
  // deviceSelected,
  requestDevice,
  deviceDisconnect,
  deviceSelectedCallback,
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
