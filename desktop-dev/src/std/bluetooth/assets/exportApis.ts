// import "../index.d.ts";
import type { $Device } from "../types.ts";
import {
  BluetoothDevice,
  BluetoothRemoteGATTCharacteristic,
  BluetoothRemoteGATTDescriptor,
  BluetoothRemoteGATTServer,
  BluetoothRemoteGATTService,
  RequestDeviceOptions,
} from "../types.ts";

import { mainApis } from "../../../helper/openNativeWindow.preload.ts";
import { allDeviceListMap } from "./data.ts";

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
async function requestDevice(
  requestDeviceOptions: RequestDeviceOptions,
  resolveId: number
) {
  console.log("requestDeviceOptions", requestDeviceOptions);
  preRequestDeviceOption = requestDeviceOptions;
  (navigator as any).bluetooth
    .requestDevice(requestDeviceOptions)
    .then((_bluetooth: BluetoothDevice) => {
      if (_bluetooth !== undefined) {
        bluetooth = _bluetooth;
        return bluetooth.gatt?.connect();
      }
      return Promise.reject(new Error(`_bluettoh === undefined`));
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
    .catch((err: Error) => {
      connectedFail ? connectedFail(err) : "";
      clearTimeout(setTimeoutId);
      console.error(`requestDevice fail: `, err);
    });
}

/**
 * 蓝牙设备断开连接
 * @param id
 * @returns
 */
async function deviceDisconnect(id: string, resolveId: number) {
  if (bluetoothRemoteGATTServer === undefined) {
    (mainApis as any).deviceDisconnectCallback(
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
    (mainApis as any).deviceDisconnectCallback(
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
  (mainApis as any).deviceDisconnectCallback(
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
    (mainApis as any).operationCallback(
      {
        success: false,
        error: `bluetoothRemoteGATTServer === undefined`,
        data: undefined,
      },
      resolveId
    );
    return;
  }
  if (id === bluetoothRemoteGATTServer.device.id) {
    bluetoothRemoteGATTServer.connect();
    (mainApis as any).operationCallback(
      {
        success: true,
        error: undefined,
        data: "ok",
      },
      resolveId
    );
    return;
  }
  (mainApis as any).operationCallback(
    {
      success: false,
      error: `bluetoothRemoteGATTServer === undefined`,
      data: undefined,
    },
    resolveId
  );
}

async function bluetoothRemoteGATTServerDisconnect(
  id: string,
  resolveId: number
) {
  if (bluetoothRemoteGATTServer === undefined) {
    (mainApis as any).operationCallback(
      {
        success: false,
        error: `bluetoothRemoteGATTServer === undefined`,
        data: undefined,
      },
      resolveId
    );
    return;
  }
  if (id === bluetoothRemoteGATTServer.device.id) {
    bluetoothRemoteGATTServer.disconnect();
    (mainApis as any).operationCallback(
      {
        success: true,
        error: undefined,
        data: "ok",
      },
      resolveId
    );
    return;
  }
  (mainApis as any).operationCallback(
    {
      success: false,
      error: `参数 id 没有匹配的 BluetoothRemoteGATTServer`,
      data: undefined,
    },
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
      console.log("点击了 li");
      // 点击连接在返回回来之前是不能够再次点击的
      if (isConnecting) return;

      deviceSelected(device);

      // // 只有第一次点击 preSelected === undefined
      // if (preSelected === undefined) {
      //   (mainApis as any).deviceSelected(device);
      // } else {
      //   requestDevice(preRequestDeviceOption);
      // }

      // preSelected = (list: $Device[]) => {
      //   const item = list.find((item) => item.deviceId == device.deviceId);
      //   if (item === undefined) return;
      //   (mainApis as any).deviceSelected(device);
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
  (mainApis as any).deviceSelected(device);
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
        (mainApis as any).deviceConnectedCallback({
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
        (mainApis as any).deviceConnectedCallback({
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
        (mainApis as any).deviceConnectedCallback({
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
    (mainApis as any).operationCallback(
      {
        success: false,
        error: `bluetoothRemoteGATTServer === undefined`,
        data: undefined,
      },
      resolveId
    );
    return;
  }
  bluetoothRemoteGATTServer.getPrimaryService(uuid).then(
    (_bluetoothRemoteGATTService) => {
      bluetoothRemoteGATTService = _bluetoothRemoteGATTService;
      // console.log("_bluetoothRemoteGATTService: ", _bluetoothRemoteGATTService);
      // console.log("", _bluetoothRemoteGATTService.addEventListener);

      // 注意 没有addEventLisener 这个方法
      // 监听是事件还没有处理
      // bluetoothRemoteGATTService.addEventListener("serviceadded", (event) => {
      //   (mainApis as any).bluetoothRemoteGATTServiceListenner(
      //     "serviceadded",
      //     event
      //   );
      // });
      // bluetoothRemoteGATTService.addEventListener("servicechanged", (event) => {
      //   (mainApis as any).bluetoothRemoteGATTServiceListenner(
      //     "servicechanged",
      //     event
      //   );
      // });
      // bluetoothRemoteGATTService.addEventListener("serviceremoved", (event) => {
      //   (mainApis as any).bluetoothRemoteGATTServiceListenner(
      //     "serviceremoved",
      //     event
      //   );
      // });

      (mainApis as any).operationCallback(
        {
          success: true,
          error: undefined,
          data: {
            device: {
              id: bluetoothRemoteGATTService.device.id,
              name: bluetoothRemoteGATTService.device.name,
            },
            isPrimary: bluetoothRemoteGATTService.isPrimary,
            uuid: uuid,
          },
        },
        resolveId
      );
    },
    (error) => {
      bluetoothRemoteGATTService = undefined;
      (mainApis as any).operationCallback(
        {
          success: false,
          error: error.message,
          data: undefined,
        },
        resolveId
      );
    }
  );
}

async function bluetoothRemoteGATTService_getCharacteristic(
  uuid: string,
  resolveId: number
) {
  if (bluetoothRemoteGATTService === undefined) {
    (mainApis as any).deviceGetCharacteristicCallback(
      {
        success: false,
        error: `bluetoothRemoteGATTService === undefined`,
        data: undefined,
      },
      resolveId
    );
    return;
  }

  bluetoothRemoteGATTService.getCharacteristic(uuid).then(
    (_bluetoothRemoteGATTCharacteristic) => {
      // browserWindow 不能够最小化 如果最小化就会到导致 bluetoothRemoteGATTService 失去联系
      bluetoothRemoteGATTCharacteristic = _bluetoothRemoteGATTCharacteristic;
      (mainApis as any).deviceGetCharacteristicCallback(
        {
          success: true,
          error: undefined,
          data: {
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
                bluetoothRemoteGATTCharacteristic.properties
                  .writableAuxiliaries,
              write: bluetoothRemoteGATTCharacteristic.properties.write,
              writeWithoutResponse:
                bluetoothRemoteGATTCharacteristic.properties
                  .writeWithoutResponse,
            },
            value: bluetoothRemoteGATTCharacteristic.value,
          },
        },
        resolveId
      );
      console.log(
        "bluetoothRemoteGATTCharacteristic.readValue",
        bluetoothRemoteGATTCharacteristic.readValue()
      );
    },
    (err) => {
      bluetoothRemoteGATTCharacteristic = undefined;
      (mainApis as any).deviceGetCharacteristicCallback(
        {
          success: false,
          error: err.message,
          data: undefined,
        },
        resolveId
      );
    }
  );
}

function bluetoothRemoteGATTCharacteristic_readValue(resolveId: number) {
  if (bluetoothRemoteGATTCharacteristic === undefined) {
    (mainApis as any).characteristicReadValueCallback(
      {
        success: false,
        error: `bluetoothRemoteGATTService === undefined`,
        data: undefined,
      },
      resolveId
    );
    return;
  }
  bluetoothRemoteGATTCharacteristic.readValue().then(
    (res: DataView) => {
      console.log("readValue: ", res);
      (mainApis as any).characteristicReadValueCallback(
        {
          success: true,
          error: undefined,
          data: res,
        },
        resolveId
      );
    },
    (err) => {
      console.error("characteristicRaadValue error", err);
      (mainApis as any).characteristicReadValueCallback(
        {
          success: false,
          error: err.message,
          data: undefined,
        },
        resolveId
      );
      return;
    }
  );
}

function BluetoothRemoteGATTCharacteristic_getDescriptor(
  uuid: string,
  resolveId: number
) {
  if (bluetoothRemoteGATTCharacteristic === undefined) {
    (mainApis as any).characteristicReadValueCallback(
      {
        success: false,
        error: `bluetoothRemoteGATTService === undefined`,
        data: undefined,
      },
      resolveId
    );
    return;
  }

  bluetoothRemoteGATTCharacteristic.getDescriptor(uuid).then(
    (_bluetoothRemoteGATTDescriptor: BluetoothRemoteGATTDescriptor) => {
      bluetoothRemoteGATTDescriptor = _bluetoothRemoteGATTDescriptor;
      (mainApis as any).characteristicGetDescriptorCallback(
        {
          success: true,
          error: undefined,
          data: {
            value: bluetoothRemoteGATTDescriptor.value,
          },
        },
        resolveId
      );
      return;
    },
    (err) => {
      (mainApis as any).characteristicGetDescriptorCallback(
        {
          success: false,
          error: err.message,
          data: undefined,
        },
        resolveId
      );
      return;
    }
  );
}

async function descriptorReadValue(resolveId: number) {
  if (bluetoothRemoteGATTDescriptor === undefined) {
    (mainApis as any).descriptorReadValueCallback(
      {
        success: false,
        error: `bluetoothRemoteGATTDescriptor === undefined`,
        data: undefined,
      },
      resolveId
    );
    return;
  }

  bluetoothRemoteGATTDescriptor.readValue().then(
    (value: DataView) => {
      (mainApis as any).descriptorReadValueCallback(
        {
          success: false,
          error: undefined,
          data: value,
        },
        resolveId
      );
    },
    (err) => {
      (mainApis as any).descriptorReadValueCallback(
        {
          success: false,
          error: err.message,
          data: undefined,
        },
        resolveId
      );
    }
  );
}

async function deviceSelectedFailCallback() {
  Array.from(allDeviceListMap.values()).forEach((item) => {
    item.el.classList.remove("connecting");
    item.isConnecting = false;
  });
}

export const APIS = {
  devicesUpdate,
  requestDevice,
  deviceDisconnect,
  bluetoothRemoteGATTServerConnect,
  bluetoothRemoteGATTServerDisconnect,
  bluetoothRemoteGATTServerGetPrimarySevice,
  bluetoothRemoteGATTService_getCharacteristic,
  bluetoothRemoteGATTCharacteristic_readValue,
  BluetoothRemoteGATTCharacteristic_getDescriptor,
  deviceSelectedFailCallback,
  descriptorReadValue,
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
