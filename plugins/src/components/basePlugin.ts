/// <reference lib="dom" />
import { createSignal } from '../helper/createSignal.ts';


export class BasePlugin extends HTMLElement {

  // mmid:为对应组件的名称，proxy:为劫持对象的属性
  constructor(readonly mmid: string, readonly proxy: string) {
    super();
  }

  protected nativeFetch(url: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    return fetch(`${url}&X-Dweb-Host=${this.mmid}`, init)
  }

  protected createSignal = createSignal
}


