let bluetoothDevice;
export async function requestDevice(){
  console.log('开启了蓝牙设备查询')
  bluetoothDevice = await (navigator as any).bluetooth.requestDevice({
    acceptAllDevices: true,
    optionalServices: [
      0x180F, /**获取电池信息*/
      0x1844, /**声音服务*/
    ]
  })
  bluetoothDevice.gatt.connect()
  .then(
    (server: any) => {
      console.log('连接成功了', server)
    },
    (err: Error) => {
      console.log('连接失败了', err)
    }
  )
  // console.log('连接了')
  // requestDevice()
}