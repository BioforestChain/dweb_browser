/// <reference path="../../@types/web-bluetooth/index.d.ts"/>;
import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { $ResponseData } from "./bluetooth.type.ts";

export class BluetoothPlugin extends BasePlugin {
  constructor() {
    super("bluetooth.std.dweb");
  }
  // options: RequestDeviceOptions = {
  //   acceptAllDevices: true,
  //   optionalServices: ["00003802-0000-1000-8000-00805f9b34fb"],
  // }

  @bindThis
  async open(): Promise<$ResponseData<undefined>> {
    return (await this.fetchApi("/open")).json();
  }

  @bindThis
  async close(): Promise<$ResponseData<undefined>> {
    return (await this.fetchApi("/close")).json();
  }

  @bindThis
  async requestDevice(
    options: RequestDeviceOptions = {
      acceptAllDevices: true,
      optionalServices: ["00003802-0000-1000-8000-00805f9b34fb"],
    }
  ): Promise<$ResponseData<BluetoothRemoteGATTServer>> {
    const res = await (
      await this.fetchApi(`/request_device`, {
        method: "POST",
        body: JSON.stringify(options),
      })
    ).json();
    if (res.success) {
      res.data = new BluetoothRemoteGATTServer(this, {
        ...res.data.device,
      });
    }
    return res;
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
  @bindThis
  connect() {
    const result = this.plugin.fetchApi(
      `/bluetooth_remote_gatt_server/connect`,
      {
        search: {
          id: this.device.id,
        },
      }
    );
    console.log("再次连接");
    return result;
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

    return result;
  }

  @bindThis
  async getPrimaryService(
    uuid: BluetoothServiceUUID
  ): Promise<$ResponseData<BluetoothRemoteGATTService>> {
    const result = await this.plugin.fetchApi(
      `/bluetooth_remote_gatt_server/get_primary_service`,
      {
        search: {
          uuid: uuid,
        },
      }
    );
    const o = await result.json();
    if (o.success === true) {
      console.log("success", o.data);
      const bluetoothRemoteGATTSefvice = new BluetoothRemoteGATTService(
        this.plugin,
        o.data.device,
        o.data.uuid,
        o.data.isPrimary
      );
      o.data = bluetoothRemoteGATTSefvice;
    }
    return o;
  }
}

export class BluetoothRemoteGATTService extends EventTarget {
  constructor(
    readonly plugin: BluetoothPlugin,
    readonly device: BluetoothDevice,
    readonly uuid: string,
    readonly isPrimary: boolean
  ) {
    super();
  }

  @bindThis
  async getCharacteristic(
    bluetoothCharacteristicUUID: string
  ): Promise<$ResponseData<BluetoothRemoteGATTCharacteristic>> {
    const res = await this.plugin.fetchApi(
      `/bluetooth_remote_gatt_service/get_characteristic`,
      {
        search: {
          uuid: bluetoothCharacteristicUUID,
        },
      }
    );
    const o = await res.json();
    if (o.success) {
      o.data = new BluetoothRemoteGATTCharacteristic(
        this.plugin,
        this,
        o.data.uuid,
        o.data.properties,
        o.data.value ? o.data.value : undefined
      );
    }
    return o;
  }

  // async getCharacteristic(characteristic: BluetoothCharacteristicUUID): Promise<BluetoothRemoteGATTCharacteristic>{

  // }
}

export class BluetoothRemoteGATTCharacteristic extends EventTarget {
  constructor(
    readonly plugin: BluetoothPlugin,
    readonly service: BluetoothRemoteGATTService,
    readonly uuid: string,
    readonly properties: BluetoothCharacteristicProperties,
    readonly value?: DataView | undefined
  ) {
    super();
  }
  @bindThis
  async readValue() {
    const res = await this.plugin.fetchApi(
      `/bluetooth_remote_gatt_characteristic/read_value`
    );
    return res;
  }

  @bindThis
  async getDescriptor(
    uuid: string
  ): Promise<$ResponseData<BluetoothRemoteGATTDescriptor>> {
    const res = await this.plugin.fetchApi(
      `/bluetooth_remote_gatt_characteristic/get_descriptor`,
      {
        search: { uuid: uuid },
      }
    );
    const o = await res.json();
    if (o.success) {
      o.data = new BluetoothRemoteGATTDescriptor(this, uuid, o.data.value);
    }
    return o;
  }
}

export class BluetoothRemoteGATTDescriptor {
  constructor(
    readonly characteristic: BluetoothRemoteGATTCharacteristic,
    readonly uuid: string,
    readonly value?: DataView
  ) {}
  @bindThis
  async readValue() {
    const res = await this.characteristic.plugin.fetchApi(
      `/bluetooth_remote_gatt_descriptor/reaed_value`
    );
    return res;
  }
}

export const bluetoothPlugin = new BluetoothPlugin();
