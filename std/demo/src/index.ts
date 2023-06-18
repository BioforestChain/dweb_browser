import {LitElement, css, html} from 'lit';
import {customElement, state, query} from 'lit/decorators.js';
import "../../src/client/components/bluetooth/bluetooth.wc"
import type { HTMLBluetoothElement, $Device } from "../../src/client/components/bluetooth/index"

@customElement("my-app")
export class App extends LitElement {
  static override styles = createAllCSS();
  constructor(){
    super();
  }
  @state() isOpen = false;
  @state() deviceList: $Device[] = [];
  @query("std-bluetooth") stdBluetooth: HTMLBluetoothElement  | undefined;

  toggle(){
    this.isOpen 
    ? this.close()
    : this.open();
  }

  open(){
    this.isOpen = true;
    this.stdBluetooth?.toggle(true)
    .then(res => {
      this.deviceList = [
        {deviceId: "", deviceName: "1"}
      ]
      console.log('oppen', res)
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
    console.log("选中了", item)
    this.stdBluetooth?.selected(item)
  }

  render() {
    return html`
      <std-bluetooth></std-bluetooth>
      <button @click="${this.toggle}">设置 bluetooth </button>
      <ul >
        ${
          this.deviceList.map(item => html`
            <li class="li" @click="${() => this.selected(item)}">${item.deviceName}</li>
          `)
        }
      </ul>
    `;
  }
}

function createAllCSS(){
  return css`
    .li{
      cursor: pointer;
    }
  `
}