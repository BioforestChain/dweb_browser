import type { $AllWatchControllerItem } from "../../../../../desktop-dev/src/std/bluetooth/types.ts";
export interface $Device {
  deviceId: string;
  deviceName: string;
}

export interface $ResponseData<T> {
  success: boolean;
  error?: string;
  data?: T;
}

export interface $BluetoothPluginListener {
  (arg: $AllWatchControllerItem.$SendParam): void;
}
