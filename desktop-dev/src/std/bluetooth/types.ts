/// <reference lib="dom"/>
export interface $Device {
  deviceId: string;
  deviceName: string;
}

export interface $AllDeviceListItem {
  el: HTMLLIElement;
  device: $Device;
  isConnecting: boolean;
  isConnected: boolean;
}

export interface $ResponseJsonable<T> {
  success: boolean;
  error?: string;
  data?: T;
}

export interface $AllWatchControllerItem {
  controller: ReadableStreamDefaultController;
  send: $AllWatchControllerItem.$Send;
}

export declare namespace $AllWatchControllerItem {
  export interface $Send {
    (jsonable: $SendParam): void;
  }

  export type $SendParam =
    | $SendParamGattserverdisconnected
    | $SendParamAdvertisementreceived
    | $SendParamCharacteristicValueChange;

  export interface $SendParamGattserverdisconnected {
    type: "gattserverdisconnected";
    data: unknown;
  }

  export interface $SendParamAdvertisementreceived {
    type: "advertisementreceived";
    data: unknown;
  }

  export interface $SendParamCharacteristicValueChange {
    type: "characteristicvaluechanged";
    data: unknown;
  }
}

type BluetoothServiceUUID = number | string;
type BluetoothCharacteristicUUID = number | string;
type BluetoothDescriptorUUID = number | string;

type BluetoothManufacturerData = Map<number, DataView>;
type BluetoothServiceData = Map<BluetoothServiceUUID, DataView>;

interface BluetoothDataFilter {
  readonly dataPrefix?: BufferSource | undefined;
  readonly mask?: BufferSource | undefined;
}

interface BluetoothManufacturerDataFilter extends BluetoothDataFilter {
  companyIdentifier: number;
}

interface BluetoothServiceDataFilter extends BluetoothDataFilter {
  service: BluetoothServiceUUID;
}

interface BluetoothLEScanFilter {
  readonly name?: string | undefined;
  readonly namePrefix?: string | undefined;
  readonly services?: BluetoothServiceUUID[] | undefined;
  readonly manufacturerData?: BluetoothManufacturerDataFilter[] | undefined;
  readonly serviceData?: BluetoothServiceDataFilter[] | undefined;
}

interface BluetoothLEScanOptions {
  readonly filters?: BluetoothLEScanFilter[] | undefined;
  readonly keepRepeatedDevices?: boolean | undefined;
  readonly acceptAllAdvertisements?: boolean | undefined;
}

interface BluetoothLEScan extends BluetoothLEScanOptions {
  active: boolean;
  stop: () => void;
}

export type RequestDeviceOptions =
  | {
      filters: BluetoothLEScanFilter[];
      optionalServices?: BluetoothServiceUUID[] | undefined;
      optionalManufacturerData?: number[] | undefined;
    }
  | {
      acceptAllDevices: boolean;
      optionalServices?: BluetoothServiceUUID[] | undefined;
      optionalManufacturerData?: number[] | undefined;
    };

export interface BluetoothAdvertisingEvent extends Event {
  readonly device: BluetoothDevice;
  readonly uuids: BluetoothServiceUUID[];
  readonly manufacturerData: BluetoothManufacturerData;
  readonly serviceData: BluetoothServiceData;
  readonly name?: string | undefined;
  readonly appearance?: number | undefined;
  readonly rssi?: number | undefined;
  readonly txPower?: number | undefined;
}

export interface BluetoothRemoteGATTDescriptor {
  readonly characteristic: BluetoothRemoteGATTCharacteristic;
  readonly uuid: string;
  readonly value?: DataView | undefined;
  readValue(): Promise<DataView>;
  writeValue(value: BufferSource): Promise<void>;
}

interface BluetoothCharacteristicProperties {
  readonly broadcast: boolean;
  readonly read: boolean;
  readonly writeWithoutResponse: boolean;
  readonly write: boolean;
  readonly notify: boolean;
  readonly indicate: boolean;
  readonly authenticatedSignedWrites: boolean;
  readonly reliableWrite: boolean;
  readonly writableAuxiliaries: boolean;
}

interface CharacteristicEventHandlers {
  oncharacteristicvaluechanged: (this: this, ev: Event) => any;
}

export interface BluetoothRemoteGATTCharacteristic extends EventTarget, CharacteristicEventHandlers {
  readonly service: BluetoothRemoteGATTService;
  readonly uuid: string;
  readonly properties: BluetoothCharacteristicProperties;
  readonly value?: DataView | undefined;
  getDescriptor(descriptor: BluetoothDescriptorUUID): Promise<BluetoothRemoteGATTDescriptor>;
  getDescriptors(descriptor?: BluetoothDescriptorUUID): Promise<BluetoothRemoteGATTDescriptor[]>;
  readValue(): Promise<DataView>;
  writeValue(value: BufferSource): Promise<void>;
  writeValueWithResponse(value: BufferSource): Promise<void>;
  writeValueWithoutResponse(value: BufferSource): Promise<void>;
  startNotifications(): Promise<BluetoothRemoteGATTCharacteristic>;
  stopNotifications(): Promise<BluetoothRemoteGATTCharacteristic>;
  addEventListener(
    type: "characteristicvaluechanged",
    listener: (this: this, ev: Event) => any,
    useCapture?: boolean
  ): void;
  addEventListener(type: string, listener: EventListenerOrEventListenerObject, useCapture?: boolean): void;
}

interface ServiceEventHandlers {
  onserviceadded: (this: this, ev: Event) => any;
  onservicechanged: (this: this, ev: Event) => any;
  onserviceremoved: (this: this, ev: Event) => any;
}

export interface BluetoothRemoteGATTService extends EventTarget, CharacteristicEventHandlers, ServiceEventHandlers {
  readonly device: BluetoothDevice;
  readonly uuid: string;
  readonly isPrimary: boolean;
  getCharacteristic(characteristic: BluetoothCharacteristicUUID): Promise<BluetoothRemoteGATTCharacteristic>;
  getCharacteristics(characteristic?: BluetoothCharacteristicUUID): Promise<BluetoothRemoteGATTCharacteristic[]>;
  getIncludedService(service: BluetoothServiceUUID): Promise<BluetoothRemoteGATTService>;
  getIncludedServices(service?: BluetoothServiceUUID): Promise<BluetoothRemoteGATTService[]>;
  addEventListener(type: "serviceadded", listener: (this: this, ev: Event) => any, useCapture?: boolean): void;
  addEventListener(type: "servicechanged", listener: (this: this, ev: Event) => any, useCapture?: boolean): void;
  addEventListener(type: "serviceremoved", listener: (this: this, ev: Event) => any, useCapture?: boolean): void;
  addEventListener(type: string, listener: EventListenerOrEventListenerObject, useCapture?: boolean): void;
}

export interface BluetoothRemoteGATTServer {
  readonly device: BluetoothDevice;
  readonly connected: boolean;
  connect(): Promise<BluetoothRemoteGATTServer>;
  disconnect(): void;
  getPrimaryService(service: BluetoothServiceUUID): Promise<BluetoothRemoteGATTService>;
  getPrimaryServices(service?: BluetoothServiceUUID): Promise<BluetoothRemoteGATTService[]>;
}

interface BluetoothDeviceEventHandlers {
  onadvertisementreceived: (this: this, ev: BluetoothAdvertisingEvent) => any;
  ongattserverdisconnected: (this: this, ev: Event) => any;
}

interface WatchAdvertisementsOptions {
  signal?: AbortSignal;
}

export interface BluetoothDevice
  extends EventTarget,
    BluetoothDeviceEventHandlers,
    CharacteristicEventHandlers,
    ServiceEventHandlers {
  readonly id: string;
  readonly name?: string | undefined;
  readonly gatt?: BluetoothRemoteGATTServer | undefined;
  forget(): Promise<void>;
  watchAdvertisements(options?: WatchAdvertisementsOptions): Promise<void>;
  readonly watchingAdvertisements: boolean;
  addEventListener(
    type: "gattserverdisconnected",
    listener: (this: this, ev: Event) => any,
    useCapture?: boolean
  ): void;
  addEventListener(
    type: "advertisementreceived",
    listener: (this: this, ev: BluetoothAdvertisingEvent) => any,
    useCapture?: boolean
  ): void;
  addEventListener(type: string, listener: EventListenerOrEventListenerObject, useCapture?: boolean): void;
}

interface Bluetooth
  extends EventTarget,
    BluetoothDeviceEventHandlers,
    CharacteristicEventHandlers,
    ServiceEventHandlers {
  getDevices(): Promise<BluetoothDevice[]>;
  getAvailability(): Promise<boolean>;
  onavailabilitychanged: (this: this, ev: Event) => any;
  readonly referringDevice?: BluetoothDevice | undefined;
  requestDevice(options?: RequestDeviceOptions): Promise<BluetoothDevice>;
  requestLEScan(options?: BluetoothLEScanOptions): Promise<BluetoothLEScan>;
  addEventListener(type: "availabilitychanged", listener: (this: this, ev: Event) => any, useCapture?: boolean): void;
  addEventListener(
    type: "advertisementreceived",
    listener: (this: this, ev: BluetoothAdvertisingEvent) => any,
    useCapture?: boolean
  ): void;
  addEventListener(type: string, listener: EventListenerOrEventListenerObject, useCapture?: boolean): void;
}

declare namespace BluetoothUUID {
  function getService(name: string | number): string;
  function getCharacteristic(name: string | number): string;
  function getDescriptor(name: string | number): string;
  function canonicalUUID(alias: string | number): string;
}

export interface Navigator {
  bluetooth: Bluetooth;
}
