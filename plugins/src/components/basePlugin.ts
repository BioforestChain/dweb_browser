/// <reference lib="dom" />
import { createSignal } from '../helper/createSignal.ts';


export class BasePlugin extends HTMLElement {

  // mmid:为对应组件的名称，proxy:为劫持对象的属性
  constructor(readonly mmid: string, readonly proxy: string) {
    super();
  }

  protected nativeFetch(url: RequestInfo, init?: RequestInit): Promise<Response> {
    if (url instanceof Request) {
      return fetch(url, init)
    }
    const api = window.location.host.replace("www", "api")
    console.log("nativeFetch=>", api)
    return fetch(`https://${api}${url}`, init)
  }

  protected createSignal = createSignal
}


