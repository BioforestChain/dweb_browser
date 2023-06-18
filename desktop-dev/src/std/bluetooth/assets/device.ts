let bluetoothDevice;
export async function requestDevice(){
  bluetoothDevice = await (navigator as any).bluetooth.requestDevice({
    acceptAllDevices: true,
    optionalServices: [
      0x180F, /**获取电池信息*/
      0x1844, /**声音服务*/
    ]
  })
  bluetoothDevice.gatt.connect();
  requestDevice()
}