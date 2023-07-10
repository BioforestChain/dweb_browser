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

export declare namespace $AllWatchControllerItem {
  export interface $Send {
    (jsonable: $SendParam): void;
  }

  export type $SendParam = $SendParamGattserverdisconnected;

  export interface $SendParamGattserverdisconnected {
    type: "gattserverdisconnected";
    data: unknown;
  }
}
