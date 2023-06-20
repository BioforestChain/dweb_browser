import {LitElement, css, html} from 'lit';
import {customElement, state, query} from 'lit/decorators.js';
import { classMap } from "lit/directives/class-map.js";
import "../../../../src/client/components/bluetooth/bluetooth.wc"
import "./components/device_list.ts";
import type { HTMLBluetoothElement, $Device } from "../../../../src/client/components/bluetooth/index"

@customElement("dweb-bluetooth")
export class App extends LitElement {
  static override styles = createAllCSS();
  constructor(){
    super();
  }
  @state() isOpen = false;
  @state() deviceList: $Device[] = [{deviceId: "1", deviceName: "name_1"}];
  @state() connectingDeviceId: string = "1xx";
  @state() connectedDeviceId: string = "1";
  @query("std-bluetooth") stdBluetooth: HTMLBluetoothElement  | undefined;

  bluetoothToggle(){
    this.isOpen 
    ? this.close()
    : this.open();
  }

  open(){
    this.isOpen = true;
    this.stdBluetooth?.toggle(true)
    .then(res => {
      this.deviceList = [
        {deviceId: "1", deviceName: "name_1"}
      ]
       
    })
  }

  close(){
    this.isOpen = false;
    this.stdBluetooth?.toggle(false)
    .then(res => {
      // 关闭了
      this.deviceList = []
    })
  }

  selected(item: $Device){
    this.stdBluetooth?.selected(item)
  }

  render() {


    return html`
      <std-bluetooth></std-bluetooth>
      <div class="title">蓝牙</div>
      <div 
        class=${classMap({
          toggle: true,
          toggle_active: this.isOpen
        })}
        @click="${this.bluetoothToggle}"
      >
        <label class="toggle_label">蓝牙</label>
        <div class="toggle_content_container">
          <div class="toggle_btn"></div>
        </div>
      </div>
      <p class="device_title">我的设备</p>
      <dweb-bluetooth-device-list
        .list=${this.deviceList}
        .connectingDeviceId=${this.connectingDeviceId}
        .connectedDeviceId =${this.connectedDeviceId}
      >
      </dweb-bluetooth-device-list>
    `;
  }
}

function createAllCSS(){
  return css`
    :host{
      display: block;
      width: 100%;
      height: 100%;
    }

    .title{
      display: flex;
      justify-content: center;
      align-items: center;
      width: 100%;
      height: 60px;;
    }

    .toggle{
      display: flex;
      justify-content: space-between;
      align-items: center;
      box-sizing: border-box;
      padding: 0px 20px;
      width: 100%;
      height: 50px;
      border-radius: 10px;
      background: #fff;
    }

    .toggle_content_container{
      position: relative;
      display: flex;
      align-items: center;
      width: 38px;
      height: 26px;
      border-radius: 19px;
      background: #ddd;
      cursor: pointer;
    }

    .toggle_active .toggle_content_container{
      background: #1677ff;
    }

    .toggle_btn{
      position: absolute;
      left: 2px;
      width: 22px;
      height: 22px;
      border-radius: 100%;
      background: #fff;
    }

    .toggle_active .toggle_btn{
      left: auto;
      right: 2px;
    }

    .device_title{
      padding-top: 20px;
      width: 100%;
      height: 20px;
      font-size: 13px;
      color: #666;
    }
  `
}