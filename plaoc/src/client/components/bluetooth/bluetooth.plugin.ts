import "../../@types/web-bluetooth/index.d.ts";
import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";

export class BluetoothPlugin extends BasePlugin {
  constructor() {
    super("bluetooth.std.dweb");
  }

  @bindThis
  async open(options: RequestDeviceOptions = { acceptAllDevices: true }) {
    const res = await this.fetchApi("/open", {
      method: "POST",
      body: JSON.stringify(options),
    });
    const deviceConnted = await res.json();
    const server = new BluetoothRemoteGATTServer(this, {
      ...deviceConnted.device,
    });
    // console.log('server: ', server, server.connected)
    // console.log("res: ", deviceConnted)
    return server;
  }

  // @bindThis
  // async connect(id: string) {
  //   return this.fetchApi(`/connect?id=${id}`);
  // }

  @bindThis
  async close() {
    return this.fetchApi("/close");
  }
}

// export class BluetoothDevice extends EventTarget {
//   private _gatt: BluetoothRemoteGATTServer | undefined;
//   constructor(
//     readonly id: string,
//     readonly watchingAdvertisements: boolean,
//     readonly name?: string | undefined
//   ) {
//     super();
//     if (watchingAdvertisements) {
//       this.watchAdvertisements();
//     }
//     this._gatt = new BluetoothRemoteGATTServer(this);
//   }

//   get gatt() {
//     return this._gatt;
//   }

//   // 撤销访问的权限
//   @bindThis
//   async forget() {}

//   @bindThis
//   async watchAdvertisements() {}
// }

export class BluetoothRemoteGATTServer {
  connected = false;
  constructor(
    readonly plugin: BluetoothPlugin,
    readonly device: BluetoothDevice,
    connected: boolean = true
  ) {
    this.connected = connected;
  }

  connect() {
    console.error("默认是连接的 是否可以再次连接");
  }

  // 断开蓝牙连接
  @bindThis
  disconnect() {
    const result = this.plugin.fetchApi(
      `/bluetooth_remote_gatt_server/disconnect`,
      {
        search: {
          id: this.device.id,
        },
      }
    );
    console.log("result: ", result);
  }

  getPrimaryService(service: BluetoothServiceUUID) {}

  getPrimaryServices() {}
}

class PrimaryService extends EventTarget {
  constructor(
    readonly device: BluetoothDevice,
    readonly uuid: string,
    readonly isPrimary: boolean
  ) {
    super();
  }

  // async getCharacteristic(characteristic: BluetoothCharacteristicUUID): Promise<BluetoothRemoteGATTCharacteristic>{

  // }
}

export const bluetoothPlugin = new BluetoothPlugin();
