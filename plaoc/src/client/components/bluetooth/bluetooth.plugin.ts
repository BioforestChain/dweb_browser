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
  async requestAndConnectDevice(
    options: RequestDeviceOptions = {
      acceptAllDevices: true,
      optionalServices: ["00003802-0000-1000-8000-00805f9b34fb"],
    }
  ): Promise<$ResponseData<BluetoothRemoteGATTServer>> {
    const res = await (
      await this.fetchApi(`/request_connect_device`, {
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

export class BluetoothDevice {
  private _gatt: BluetoothRemoteGATTServer | undefined;
  private name: string | undefined;
  private gatt: BluetoothRemoteGATTServer | undefined;
  private eventMap: Map<string, Set<{ (ev: Event): void }>> = new Map();
  private isWatchGattServerDisconnected = false;
  private isAdvertisementreceived = false;
  constructor(
    readonly plugin: BluetoothPlugin,
    readonly id: string,
    readonly watchingAdvertisements: boolean,
    _name?: string | undefined,
    _gatt?: BluetoothRemoteGATTServer | undefined
  ) {
    this.name = _name;
    this.gatt = _gatt;
    // if (watchingAdvertisements) {
    //   this.watchAdvertisements();
    // }
  }

  // 撤销访问的权限
  @bindThis
  async forget() {}

  @bindThis
  async watchAdvertisements() {}

  @bindThis
  addEventListener(
    type: "gattserverdisconnected" | "advertisementreceived",
    listener: { (ev: Event): void }
  ): void {
    let set = this.eventMap.get(type);
    if (set === undefined) {
      set = new Set();
      this.eventMap.set(type, set);
      set.add(listener);
      // 发起 webSocke 连接
      // this.plugin.fetchApi();
      // 一但就出发 gettserverdisconnectedListener()
    }

    if (type === "gattserverdisconnected") {
      this.isWatchGattServerDisconnected
        ? ""
        : this._watchGattServerDisconnected();
      return;
    }

    if (type === "advertisementreceived") {
      return;
    }
  }

  private _watchGattServerDisconnected = () => {
    this.isWatchGattServerDisconnected = true;
    const url = new URL(BasePlugin.url);
    url.pathname = `${this.plugin.mmid}/bluetooth_device/gattserverdisconnected_watch`;
    const ws = new WebSocket(url);
    ws.onerror = () => {};
    // ws.on
  };

  gettserverdisconnectedListener() {
    const set = this.eventMap.get("gattserverdisconnected");
    if (set === undefined) {
      return;
    }
    set.forEach((fn) => {
      fn.bind(this)(new GattServerDisconnectedEvent());
    });
  }
}

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
  async connect(): Promise<BluetoothRemoteGATTServer> {
    const result = await (
      await this.plugin.fetchApi(`/bluetooth_remote_gatt_server/connect`, {
        search: {
          id: this.device.id,
        },
      })
    ).json();

    if (result.success === true) {
      this.connected = true;
    }
    console.log("再次连接");
    return this;
  }

  // 断开蓝牙连接
  @bindThis
  async disconnect(): Promise<BluetoothRemoteGATTServer> {
    const res = await (
      await this.plugin.fetchApi(`/bluetooth_remote_gatt_server/disconnect`, {
        search: {
          id: this.device.id,
        },
      })
    ).json();
    if (res.success === true) {
      // 需要修改 状态
      this.connected = false;
    }
    return this;
  }

  @bindThis
  async getPrimaryService(
    uuid: BluetoothServiceUUID
  ): Promise<$ResponseData<BluetoothRemoteGATTService>> {
    const res = await (
      await this.plugin.fetchApi(
        `/bluetooth_remote_gatt_server/get_primary_service`,
        {
          search: {
            uuid: uuid,
          },
        }
      )
    ).json();

    if (res.success === true) {
      console.log("success", res.data);
      const bluetoothRemoteGATTSefvice = new BluetoothRemoteGATTService(
        this.plugin,
        res.data.device,
        res.data.uuid,
        res.data.isPrimary
      );
      res.data = bluetoothRemoteGATTSefvice;
    }

    return res;
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
    const res = await (
      await this.plugin.fetchApi(
        `/bluetooth_remote_gatt_service/get_characteristic`,
        {
          search: {
            uuid: bluetoothCharacteristicUUID,
          },
        }
      )
    ).json();

    if (res.success) {
      res.data = new BluetoothRemoteGATTCharacteristic(
        this.plugin,
        this,
        res.data.uuid,
        res.data.properties,
        res.data.value ? res.data.value : undefined
      );
    }
    return res;
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
  async readValue(): Promise<$ResponseData<DataView | undefined>> {
    const res = await (
      await this.plugin.fetchApi(
        `/bluetooth_remote_gatt_characteristic/read_value`
      )
    ).json();
    if (res.success) {
      res.data = new DataView(
        // deno-lint-ignore ban-types
        Uint8Array.from([...Object.values(res.data as Object)]).buffer
      );
    }
    return res;
  }

  @bindThis
  async writeValue(arrayBuffer: ArrayBuffer): Promise<$ResponseData<unknown>> {
    const res = await (
      await this.plugin.fetchApi(
        `/bluetooth_remote_gatt_characteristic/write_value`,
        {
          method: "POST",
          body: new Blob([arrayBuffer]),
          headers: {
            "Content-Type": "application/octet-stream",
          },
        }
      )
    ).json();
    return res;
  }

  @bindThis
  async getDescriptor(
    uuid: string
  ): Promise<$ResponseData<BluetoothRemoteGATTDescriptor>> {
    const res = await (
      await this.plugin.fetchApi(
        `/bluetooth_remote_gatt_characteristic/get_descriptor`,
        {
          search: { uuid: uuid },
        }
      )
    ).json();
    if (res.success) {
      res.data = new BluetoothRemoteGATTDescriptor(this, uuid, res.data.value);
    }
    return res;
  }
}

export class BluetoothRemoteGATTDescriptor {
  constructor(
    readonly characteristic: BluetoothRemoteGATTCharacteristic,
    readonly uuid: string,
    readonly value?: DataView
  ) {}
  @bindThis
  async readValue(): Promise<$ResponseData<DataView | undefined>> {
    const res = await (
      await this.characteristic.plugin.fetchApi(
        `/bluetooth_remote_gatt_descriptor/reaed_value`
      )
    ).json();
    if (res.success) {
      res.data = new DataView(
        // deno-lint-ignore ban-types
        Uint8Array.from([...Object.values(res.data as Object)]).buffer
      );
    }
    return res;
  }

  @bindThis
  async writeValue(arrayBuffer: ArrayBuffer) {
    const res = await (
      await this.characteristic.plugin.fetchApi(
        `/bluetooth_remote_gatt_descriptor/write_value`,
        {
          method: "POST",
          body: new Blob([arrayBuffer]),
          headers: {
            "Content-Type": "application/octet-stream",
          },
        }
      )
    ).json();

    return res;
  }
}

export const bluetoothPlugin = new BluetoothPlugin();

class GattServerDisconnectedEvent extends Event {
  readonly state = "disconnected";
  constructor() {
    super("gattserverdisconnected");
  }
}
