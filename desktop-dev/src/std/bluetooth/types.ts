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
