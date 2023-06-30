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

export interface $ResponseJsonable {
  success: boolean;
  error: string | undefined;
  data: any;
}
