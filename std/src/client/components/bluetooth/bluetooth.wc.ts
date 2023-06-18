import { bluetoothPlugin } from "./bluetooth.plugin"
import { $Device } from "./bluetooth.type";
export class HTMLBluetoothElement extends HTMLElement{
  plugin = bluetoothPlugin;
  
  toggle(isOpen: boolean){
    return this.plugin.toggle(isOpen)
  }

  selected(device: $Device){
    return this.plugin.selected(device)
  }
}

// 注册
customElements.get(bluetoothPlugin.tagName)
? ""
: customElements.define(
  bluetoothPlugin.tagName,
  HTMLBluetoothElement
)

