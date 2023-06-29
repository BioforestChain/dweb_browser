export interface $Device {
  deviceId: string;
  deviceName: string;
}

export interface $ResponseData<T> {
  success: boolean;
  error?: string;
  data?: T;
}
