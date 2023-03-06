/// <reference path="../desktop/src/sys/js-process/js-process.worker.d.ts"/>

export class BasePlugin extends HTMLElement {
  constructor(readonly mmid: string) {
    super();
  }

  protected nativeFetch(url: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    return this.nativeFetch(`${this.mmid}/${url}`, init)
  }

} 
