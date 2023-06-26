import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import "../../@types/web-bluetooth/index.d.ts"

export class BluetoothPlugin extends BasePlugin{

  private _bluetoothDevice: BluetoothDevice | undefined;
  constructor(){
    super("bluetooth.std.dweb");
  }

  /**
   * 查询 蓝牙设备
   * @returns 
   */
  requestDevice = async() => {
    this._bluetoothDevice = await navigator.bluetooth.requestDevice({
      acceptAllDevices: true,
      optionalServices: [
        0x180F, /**获取电池信息*/
        0x1844, /**声音服务*/
      ]
    })
    return this._bluetoothDevice
  }

  /**
   * 取消查询蓝牙设备
   * @returns 
   */
  @bindThis
  requestDeviceCancel(){
    return this.fetchApi("/close")
  }
 
}

export const bluetoothPlugin = new BluetoothPlugin();