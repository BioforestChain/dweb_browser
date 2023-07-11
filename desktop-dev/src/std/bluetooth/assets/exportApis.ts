// import "../index.d.ts";
import type { $Device } from "../types.ts";
import {
  BluetoothAdvertisingEvent,
  BluetoothDevice,
  BluetoothRemoteGATTCharacteristic,
  BluetoothRemoteGATTDescriptor,
  BluetoothRemoteGATTServer,
  BluetoothRemoteGATTService,
  RequestDeviceOptions,
} from "../types.ts";

import { importApis } from "../../../helper/openNativeWindow.preload.ts";
import { allDeviceListMap } from "./data.ts";
// const mainApis =
//   importApis<
//     ReturnType<
//       Awaited<
//         ReturnType<
//           import("../bluetooth.main.ts").BluetoothNMM["_createBrowserWindow"]
//         >
//       >["getExport"]
//     >
//   >();
import type { BluetoothNMM } from "../bluetooth.main.ts";
const mainApis = importApis<BluetoothNMM["_exports"]>();

const template: HTMLTemplateElement | null =
  document.querySelector(".template");
let setTimeoutId: number;
let bluetooth: BluetoothDevice;
let bluetoothRemoteGATTServer: BluetoothRemoteGATTServer | undefined;
let bluetoothRemoteGATTService: BluetoothRemoteGATTService | undefined;
let bluetoothRemoteGATTCharacteristic:
  | BluetoothRemoteGATTCharacteristic
  | undefined;
let bluetoothRemoteGATTDescriptor: BluetoothRemoteGATTDescriptor | undefined;
let connectedSuccess: { (server: BluetoothRemoteGATTServer): void } | undefined;
let connectedFail: { (err: Error): void } | undefined;
let isConnecting: boolean = false; // 当前是否在连接 设备状态
let preRequestDeviceOption: RequestDeviceOptions = { acceptAllDevices: true };
let preSelected: { (list: $Device[]): void } | undefined;

/**
 *
 * @returns Promise<null | Error>
 */
async function requestDevice(requestDeviceOptions: RequestDeviceOptions) {
  console.log("requestDeviceOptions", requestDeviceOptions);
  preRequestDeviceOption = requestDeviceOptions;
  (navigator as any).bluetooth
    .requestDevice(requestDeviceOptions)
    .then((_bluetooth: BluetoothDevice) => {
      if (_bluetooth !== undefined) {
        bluetooth = _bluetooth;
        bluetooth.addEventListener("gattserverdisconnected", () => {
          // console.error("error", "gattserverdisconnected");
          mainApis.watchStateChange("gattserverdisconnected", undefined);
        });

        bluetooth.addEventListener(
          "advertisementreceived",
          (ev: BluetoothAdvertisingEvent) => {
            mainApis.watchStateChange("advertisementreceived", ev);
          }
        );
        return bluetooth.gatt?.connect();
      }
      return Promise.reject(new Error(`_bluettoh === undefined`));
    })
    .then((server: BluetoothRemoteGATTServer | undefined) => {
      // console.log("server", server);
      bluetoothRemoteGATTServer = server;
      if (connectedSuccess === undefined)
        throw new Error(`connectedSuccess === undefined`);
      if (server === undefined) throw new Error("server === undefined");
      connectedSuccess(server);
      clearTimeout(setTimeoutId);
    })
    .catch((err: Error) => {
      connectedFail ? connectedFail(err) : "";
      clearTimeout(setTimeoutId);
      // console.error(`requestDevice fail: `, err);
    });
}

async function bluetoothDeviceForget(id: string, resolveId: number) {
  if (bluetooth.id !== id) {
    return operationCallbackError(`bluetooth.id !== ${id}`, resolveId);
  }

  bluetooth.forget().then(
    () => operationCallbackSuccess("ok", resolveId),
    (err) => operationCallbackError(err.message, resolveId)
  );
}

async function bluetoothDeviceWatchAdvertisements(
  id: string,
  resolveId: number
) {
  if (bluetooth.id !== id) {
    return operationCallbackError(`bluetooth.id !== ${id}`, resolveId);
  }
  bluetooth.watchAdvertisements().then(
    () => operationCallbackSuccess("ok", resolveId),
    (err) => operationCallbackError(err.message, resolveId)
  );
}

/**
 * 蓝牙设备断开连接
 * @param id
 * @returns
 */
async function deviceDisconnect(id: string, resolveId: number) {
  if (bluetoothRemoteGATTServer === undefined) {
    mainApis.deviceDisconnectCallback(
      {
        success: false,
        error: `bluetoothRemoteGATTServer === undefined`,
        data: undefined,
      },
      resolveId
    );
    return;
  }
  if (id === bluetoothRemoteGATTServer?.device.id) {
    bluetoothRemoteGATTServer.disconnect();
    mainApis.deviceDisconnectCallback(
      {
        success: true,
        error: undefined,
        data: true,
      },
      resolveId
    );

    Array.from(allDeviceListMap.values()).forEach((item) => {
      item.el.classList.remove("connected");
      item.isConnected = false;
    });

    bluetoothRemoteGATTServer = undefined;
    return;
  }
  mainApis.deviceDisconnectCallback(
    {
      success: false,
      error: `bluetoothRemoteGATTServer.id !== 请求的id`,
      data: undefined,
    },
    resolveId
  );
}

async function bluetoothRemoteGATTServerConnect(id: string, resolveId: number) {
  if (bluetoothRemoteGATTServer === undefined) {
    return operationCallbackError(
      `bluetoothRemoteGATTServer === undefined`,
      resolveId
    );
  }
  if (id === bluetoothRemoteGATTServer.device.id) {
    bluetoothRemoteGATTServer.connect();
    return operationCallbackSuccess("ok", resolveId);
  }
  return operationCallbackError(
    `id !== bluetoothRemoteGATTServer.device.id`,
    resolveId
  );
}

async function bluetoothRemoteGATTServerDisconnect(
  id: string,
  resolveId: number
) {
  if (bluetoothRemoteGATTServer === undefined) {
    return operationCallbackError(
      `bluetoothRemoteGATTServer === undefined`,
      resolveId
    );
  }
  if (id === bluetoothRemoteGATTServer.device.id) {
    bluetoothRemoteGATTServer.disconnect();
    return operationCallbackSuccess("ok", resolveId);
  }
  return operationCallbackError(
    `id !== bluetoothRemoteGATTServer.device.id`,
    resolveId
  );
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
      // console.log("点击了 li");
      // 点击连接在返回回来之前是不能够再次点击的
      if (isConnecting) return;

      deviceSelected(device);

      // // 只有第一次点击 preSelected === undefined
      // if (preSelected === undefined) {
      //   (mainApis ).deviceSelected(device);
      // } else {
      //   requestDevice(preRequestDeviceOption);
      // }

      // preSelected = (list: $Device[]) => {
      //   const item = list.find((item) => item.deviceId == device.deviceId);
      //   if (item === undefined) return;
      //   (mainApis ).deviceSelected(device);
      // };
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
  mainApis.deviceSelected(device);
  isConnecting = true;
  Array.from(allDeviceListMap.values()).forEach((oldDevice) => {
    if (oldDevice.device.deviceId === device.deviceId) {
      oldDevice.el.classList.remove("connected");
      oldDevice.el.classList.add("connecting");
      oldDevice.isConnecting = true;
      oldDevice.isConnected = false;

      connectedFail = (err: Error) => {
        oldDevice.el.classList.remove("connecting");
        oldDevice.isConnecting = false;
        isConnecting = false;
        mainApis.deviceConnectedCallback({
          success: false,
          error: err.message,
          data: undefined,
        });
      };

      // 超时设置
      setTimeoutId = setTimeout(() => {
        oldDevice.el.classList.remove("connecting");
        oldDevice.isConnecting = false;
        isConnecting = false;
        mainApis.deviceConnectedCallback({
          success: false,
          error: `超时`,
          data: undefined,
        });
        console.error("连接超时");
      }, 6000);

      connectedSuccess = (server: BluetoothRemoteGATTServer) => {
        oldDevice.el.classList.remove("connecting");
        oldDevice.el.classList.add("connected");
        oldDevice.isConnecting = false;
        oldDevice.isConnected = true;
        isConnecting = false;
        mainApis.deviceConnectedCallback({
          success: true,
          error: undefined,
          data: {
            device: { id: server.device.id, name: server.device.name },
          },
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

async function bluetoothRemoteGATTServerGetPrimarySevice(
  uuid: string,
  resolveId: number
) {
  if (bluetoothRemoteGATTServer === undefined) {
    return operationCallbackError(
      `bluetoothRemoteGATTServer === undefined`,
      resolveId
    );
  }
  bluetoothRemoteGATTServer.getPrimaryService(uuid).then(
    (_bluetoothRemoteGATTService) => {
      bluetoothRemoteGATTService = _bluetoothRemoteGATTService;
      // console.log("_bluetoothRemoteGATTService: ", _bluetoothRemoteGATTService);
      // console.log("", _bluetoothRemoteGATTService.addEventListener);

      // 注意 没有addEventLisener 这个方法
      // 监听是事件还没有处理
      // bluetoothRemoteGATTService.addEventListener("serviceadded", (event) => {
      //   (mainApis ).bluetoothRemoteGATTServiceListenner(
      //     "serviceadded",
      //     event
      //   );
      // });
      // bluetoothRemoteGATTService.addEventListener("servicechanged", (event) => {
      //   (mainApis ).bluetoothRemoteGATTServiceListenner(
      //     "servicechanged",
      //     event
      //   );
      // });
      // bluetoothRemoteGATTService.addEventListener("serviceremoved", (event) => {
      //   (mainApis ).bluetoothRemoteGATTServiceListenner(
      //     "serviceremoved",
      //     event
      //   );
      // });
      return operationCallbackSuccess(
        {
          device: {
            id: bluetoothRemoteGATTService.device.id,
            name: bluetoothRemoteGATTService.device.name,
          },
          isPrimary: bluetoothRemoteGATTService.isPrimary,
          uuid: uuid,
        },
        resolveId
      );
    },
    (error) => {
      bluetoothRemoteGATTService = undefined;
      return operationCallbackError(error.message, resolveId);
    }
  );
}

async function bluetoothRemoteGATTService_getCharacteristic(
  uuid: string,
  resolveId: number
) {
  if (bluetoothRemoteGATTService === undefined) {
    return operationCallbackError(
      `bluetoothRemoteGATTService === undefined`,
      resolveId
    );
  }

  bluetoothRemoteGATTService.getCharacteristic(uuid).then(
    async (_bluetoothRemoteGATTCharacteristic) => {
      // browserWindow 不能够最小化 如果最小化就会到导致 bluetoothRemoteGATTService 失去联系
      bluetoothRemoteGATTCharacteristic = _bluetoothRemoteGATTCharacteristic;

      //
      bluetoothRemoteGATTCharacteristic.addEventListener(
        "characteristicvaluechanged",
        (event: Event) => {
          // 特征 的值发生了变化
          mainApis.watchStateChange("characteristicvaluechanged", event);
        }
      );

      return operationCallbackSuccess(
        {
          uuid: uuid,
          properties: {
            authenticatedSignedWrites:
              bluetoothRemoteGATTCharacteristic.properties
                .authenticatedSignedWrites,
            broadcast: bluetoothRemoteGATTCharacteristic.properties.broadcast,
            indicate: bluetoothRemoteGATTCharacteristic.properties.indicate,
            notify: bluetoothRemoteGATTCharacteristic.properties.notify,
            read: bluetoothRemoteGATTCharacteristic.properties.read,
            reliableWrite:
              bluetoothRemoteGATTCharacteristic.properties.reliableWrite,
            writableAuxiliaries:
              bluetoothRemoteGATTCharacteristic.properties.writableAuxiliaries,
            write: bluetoothRemoteGATTCharacteristic.properties.write,
            writeWithoutResponse:
              bluetoothRemoteGATTCharacteristic.properties.writeWithoutResponse,
          },
          value: bluetoothRemoteGATTCharacteristic.value,
        },
        resolveId
      );
      //   mainApis.operationCallback(
      //     {
      //       success: true,
      //       error: undefined,
      //       data: {
      //         uuid: uuid,
      //         properties: {
      //           authenticatedSignedWrites:
      //             bluetoothRemoteGATTCharacteristic.properties
      //               .authenticatedSignedWrites,
      //           broadcast: bluetoothRemoteGATTCharacteristic.properties.broadcast,
      //           indicate: bluetoothRemoteGATTCharacteristic.properties.indicate,
      //           notify: bluetoothRemoteGATTCharacteristic.properties.notify,
      //           read: bluetoothRemoteGATTCharacteristic.properties.read,
      //           reliableWrite:
      //             bluetoothRemoteGATTCharacteristic.properties.reliableWrite,
      //           writableAuxiliaries:
      //             bluetoothRemoteGATTCharacteristic.properties
      //               .writableAuxiliaries,
      //           write: bluetoothRemoteGATTCharacteristic.properties.write,
      //           writeWithoutResponse:
      //             bluetoothRemoteGATTCharacteristic.properties
      //               .writeWithoutResponse,
      //         },
      //         value: bluetoothRemoteGATTCharacteristic.value,
      //       },
      //     },
      //     resolveId
      //   );
    },
    (err) => {
      bluetoothRemoteGATTCharacteristic = undefined;
      return operationCallbackError(err.messsage, resolveId);
    }
  );
}

function bluetoothRemoteGATTCharacteristic_readValue(resolveId: number) {
  if (bluetoothRemoteGATTCharacteristic === undefined) {
    return operationCallbackError(
      `bluetoothRemoteGATTCharacteristic === undefined`,
      resolveId
    );
  }
  bluetoothRemoteGATTCharacteristic.readValue().then(
    (res) => {
      return operationCallbackSuccess(res, resolveId);
    },
    (err) => {
      return operationCallbackError(err.message, resolveId);
    }
  );
}

function bluetoothRemoteGATTCharacteristic_writeValue(
  arrayBuffer: ArrayBuffer,
  resolveId: number
) {
  if (bluetoothRemoteGATTCharacteristic === undefined) {
    return operationCallbackError(
      `bluetoothRemoteGATTCharacteristic === undefined`,
      resolveId
    );
  }

  bluetoothRemoteGATTCharacteristic.writeValue(arrayBuffer).then(
    (value) => {
      return operationCallbackSuccess(value, resolveId);
    },
    (err) => {
      return operationCallbackError(err.message, resolveId);
    }
  );
}

function BluetoothRemoteGATTCharacteristic_getDescriptor(
  uuid: string,
  resolveId: number
) {
  if (bluetoothRemoteGATTCharacteristic === undefined) {
    return operationCallbackError(
      "bluetoothRemoteGATTCharacteristic === undefined",
      resolveId
    );
  }

  bluetoothRemoteGATTCharacteristic.getDescriptor(uuid).then(
    (_bluetoothRemoteGATTDescriptor: BluetoothRemoteGATTDescriptor) => {
      bluetoothRemoteGATTDescriptor = _bluetoothRemoteGATTDescriptor;
      return operationCallbackSuccess(
        {
          uuid: uuid,
          value: bluetoothRemoteGATTDescriptor.value,
        },
        resolveId
      );
    },
    (err) => {
      bluetoothRemoteGATTDescriptor = undefined;
      return operationCallbackError(err.message, resolveId);
    }
  );
}

async function bluetoothRemoteGATTDescriptor_readValue(resolveId: number) {
  if (bluetoothRemoteGATTDescriptor === undefined) {
    return operationCallbackError(
      `bluetoothRemoteGATTDescriptor === undefined`,
      resolveId
    );
  }

  bluetoothRemoteGATTDescriptor.readValue().then(
    (value) => {
      return operationCallbackSuccess(value, resolveId);
    },
    (err) => {
      return operationCallbackError(err.message, resolveId);
    }
  );
}

async function bluetoothRemoteGATTDescriptor_writeValue(
  arrayBuffer: ArrayBuffer,
  resolveId: number
) {
  if (bluetoothRemoteGATTDescriptor === undefined) {
    return operationCallbackError(
      "bluetoothRemoteGATTDescriptor === undefined",
      resolveId
    );
  }

  bluetoothRemoteGATTDescriptor.writeValue(arrayBuffer).then(
    (value) => {
      return operationCallbackSuccess(value, resolveId);
    },
    (err) => {
      return operationCallbackError(err.message, resolveId);
    }
  );
}

async function deviceSelectedFailCallback() {
  Array.from(allDeviceListMap.values()).forEach((item) => {
    item.el.classList.remove("connecting");
    item.isConnecting = false;
  });
}

function operationCallbackError(message: string, resolveId: number) {
  mainApis.operationCallback(
    {
      success: false,
      error: message,
      data: undefined,
    },
    resolveId
  );
}

function operationCallbackSuccess(value: unknown, resolveId: number) {
  mainApis.operationCallback(
    {
      success: true,
      error: undefined,
      data: value,
    },
    resolveId
  );
}

export const APIS = {
  devicesUpdate,
  requestDevice,
  bluetoothDeviceForget,
  bluetoothDeviceWatchAdvertisements,
  deviceDisconnect,
  bluetoothRemoteGATTServerConnect,
  bluetoothRemoteGATTServerDisconnect,
  bluetoothRemoteGATTServerGetPrimarySevice,
  bluetoothRemoteGATTService_getCharacteristic,
  bluetoothRemoteGATTCharacteristic_readValue,
  bluetoothRemoteGATTCharacteristic_writeValue,
  BluetoothRemoteGATTCharacteristic_getDescriptor,
  bluetoothRemoteGATTDescriptor_readValue,
  bluetoothRemoteGATTDescriptor_writeValue,
  deviceSelectedFailCallback,
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
