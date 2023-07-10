/// <reference path="./index.d.ts"/>;
import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import {
  $AllWatchControllerItem,
  $BluetoothPluginListener,
  $ResponseData,
} from "./bluetooth.type.ts";

export class BluetoothPlugin extends BasePlugin {
  private _ws: WebSocket | undefined;
  constructor() {
    super("bluetooth.std.dweb");
  }
  // options: RequestDeviceOptions = {
  //   acceptAllDevices: true,
  //   optionalServices: ["00003802-0000-1000-8000-00805f9b34fb"],
  // }

  @bindThis
  async open(): Promise<$ResponseData<undefined>> {
    const res = (await this.fetchApi("/open")).json();
    this._watch();
    return res;
  }

  @bindThis
  async close(): Promise<$ResponseData<undefined>> {
    const res = (await this.fetchApi("/close")).json();
    if (this._ws) {
      this._ws.close();
      this._ws = undefined;
    }
    return res;
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
      res.data = new BluetoothRemoteGATTServer(
        this,
        new BluetoothDevice(
          this,
          res.data.device.id,
          true,
          res.data.device.name
        )
      );
    }
    return res;
  }

  // 创建监听
  // bluetooth 的状态变化全部通过这个 watch 接受
  private _watch = () => {
    const url = new URL(
      BasePlugin.url.replace(/^http:/, "ws:").replace(/^https:/, "wss:")
    );
    url.pathname = `${this.mmid}/watch`;
    const ws = new WebSocket(url);
    this._ws = ws;
    ws.onerror = (err) => {
      console.error("onerror", err);
    };
    // let count = 0;
    ws.onopen = () => {};
    ws.onmessage = async (event: MessageEvent<Blob>) => {
      try {
        const str = await event.data.text();
        if (str.length === 0) return;
        const data = JSON.parse(
          await event.data.text()
        ) as $AllWatchControllerItem.$SendParam;
        const listener = this._allListener.get(data.type);
        if (listener === undefined) return;
        listener(data);
      } catch (err) {
        console.error(
          "_watchGattServerDisconnected",
          "ws.onmessage error",
          err
        );
      }
    };

    ws.onclose = () => {
      console.log("关闭了", "需要把消息发送给监听者");
    };
  };

  private _allListener = new Map<
    $AllWatchControllerItem.$SendParam["type"],
    $BluetoothPluginListener
  >();
  // 添加时间监听器用来相应 watch
  addeEventListener(
    type: $AllWatchControllerItem.$SendParam["type"],
    listener: $BluetoothPluginListener
  ): void {
    this._allListener.set(type, listener);
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

    this.plugin.addeEventListener("gattserverdisconnected", () => {
      this.gettserverdisconnectedListener();
    });
  }

  // 撤销访问的权限
  @bindThis
  async forget() {
    console.error("还没有实现");
  }

  @bindThis
  async watchAdvertisements() {
    console.error("还没有实现");
  }

  @bindThis
  addEventListener(
    type: "gattserverdisconnected" | "advertisementreceived",
    listener: { (ev: Event): void }
  ): void {
    let set = this.eventMap.get(type);
    if (set === undefined) {
      set = new Set();
      this.eventMap.set(type, set);
    }
    set.add(listener);

    // if (type === "gattserverdisconnected") {
    //   this.isWatchGattServerDisconnected
    //     ? ""
    //     : this._watchGattServerDisconnected();
    //   return;
    // }

    // if (type === "advertisementreceived") {
    //   this.isAdvertisementreceived ? "" : this._watchAdvertisementreceived();
    //   return;
    // }
  }

  @bindThis
  removeEventListener(
    type: "gattserverdisconnected" | "advertisementreceived",
    listener: { (ev: Event): void }
  ) {
    const set = this.eventMap.get(type);
    if (set === undefined) return;
    set.delete(listener);
  }

  // private _watchGattServerDisconnected = async () => {
  //   this.isWatchGattServerDisconnected = true;
  //   const url = new URL(
  //     BasePlugin.url.replace(/^http:/, "ws:").replace(/^https:/, "wss:")
  //   );
  //   url.pathname = `${this.plugin.mmid}/bluetooth_device/gattserverdisconnected_watch`;
  //   const ws = new WebSocket(url);
  //   ws.onerror = (err) => {
  //     console.error("onerror", err);
  //   };
  //   // let count = 0;
  //   ws.onopen = () => {};
  //   ws.onmessage = async (event: MessageEvent<Blob>) => {
  //     try {
  //       const str = await event.data.text();
  //       if (str.length === 0) return;
  //       const data = JSON.parse(await event.data.text());
  //       data.type === "gattserverdisconnected"
  //         ? this.gettserverdisconnectedListener()
  //         : "";
  //     } catch (err) {
  //       console.error(
  //         "_watchGattServerDisconnected",
  //         "ws.onmessage error",
  //         err
  //       );
  //     }
  //   };

  //   ws.onclose = () => {
  //     console.log("关闭了", "需要把消息发送给监听者");
  //   };
  // };

  // private _watchAdvertisementreceived = async () => {
  //   this.isAdvertisementreceived = true;
  //   const url = new URL(BasePlugin.url);
  //   url.pathname = `${this.plugin.mmid}/bluetooth_device/advertisementreceived`;
  //   const ws = new WebSocket(url);
  //   console.error("还没有实现");
  // };

  gettserverdisconnectedListener = () => {
    const set = this.eventMap.get("gattserverdisconnected");
    if (set === undefined) {
      return;
    }
    set.forEach((fn) => {
      fn.bind(this)(new GattServerDisconnectedEvent());
    });
  };

  advertisementreceivedListener = () => {
    console.error("还没有实现");
    const set = this.eventMap.get("advertisementreceived");
    if (set === undefined) return;
    set.forEach((fn) => {
      fn.bind(this)(new AdvertisementreceivedEvent(""));
    });
  };
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

export class BluetoothRemoteGATTCharacteristic {
  private eventMap: Map<string, Set<{ (ev: Event): void }>> = new Map();
  private _isWatchCharacteristicvaluechanged = false;
  constructor(
    readonly plugin: BluetoothPlugin,
    readonly service: BluetoothRemoteGATTService,
    readonly uuid: string,
    readonly properties: BluetoothCharacteristicProperties,
    readonly value?: DataView | undefined
  ) {}

  addEventListener = (
    type: "characteristicvaluechanged",
    listener: EventListener
  ) => {
    let set = this.eventMap.get(type);
    if (set === undefined) {
      set = new Set();
      this.eventMap.set(type, set);
    }
    set.add(listener);

    if (type === "characteristicvaluechanged") {
      this._isWatchCharacteristicvaluechanged
        ? ""
        : this.watchCharacteristicvaluechanged();
    }
  };

  removeEvent = (
    type: "characteristicvaluechanged",
    listener: EventListener
  ) => {
    const set = this.eventMap.get(type);
    if (set === undefined) return;
    set.delete(listener);
  };

  watchCharacteristicvaluechanged = async () => {
    console.error("还没有实现");
    this._isWatchCharacteristicvaluechanged = true;
    const url = new URL(BasePlugin.url);
    url.pathname = `${this.plugin.mmid}/bluetooth_device/characteristicvaluechanged_watch`;
    const ws = new WebSocket(url);
    ws.onerror = () => {};
  };

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

class AdvertisementreceivedEvent extends Event {
  constructor(readonly data: unknown) {
    super("advertisementreceived");
  }
}
