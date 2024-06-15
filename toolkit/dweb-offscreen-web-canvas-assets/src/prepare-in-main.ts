/// <reference lib="dom"/>
const nativeWorkerCtor = Worker;
export class MockMessagePort extends EventTarget {
  remote!: MockMessagePort;
  private closed = false;
  private beforeStart?: Array<[data: any, transfer?: Transferable[]]>[] = [];
  start() {
    const beforeStartArgs = this.beforeStart;
    if (beforeStartArgs) {
      this.beforeStart = undefined;
      for (const [data, transfer] of beforeStartArgs) {
        this.dispatchMessageEvent(data, transfer);
      }
      this.addEventListener = super.addEventListener.bind(this);
    }
  }
  postMessage(data, transfer?: Transferable[]) {
    if (this.closed) {
      return;
    }
    if (this.remote.beforeStart) {
      this.remote.beforeStart.push([data, transfer]);
      return;
    }
    this.remote.dispatchMessageEvent(data, transfer);
  }
  private dispatchMessageEvent(data, transfer?: Transferable[]) {
    const ports = (transfer?.filter((v) => v instanceof MessagePort) ?? []) as MessagePort[];
    this.dispatchEvent(
      new MessageEvent("message", {
        data: data,
        ports: ports,
      })
    );
  }
  addEventListener(type: any, callback: any, options?: any): void {
    super.addEventListener(type, callback, options);
    this.start();
  }
  close() {
    if (this.closed) {
      return;
    }
    this.closed = true;
    this.remote.close();
  }
}
export class MokeWorker {
  readonly script: HTMLScriptElement;
  private mockChannel = (() => {
    const port1 = new MockMessagePort();
    const port2 = new MockMessagePort();
    port1.remote = port2;
    port2.remote = port1;
    return [port1 as unknown as MessagePort, port2 as unknown as MessagePort] as const;
  })();
  readonly toWorkerPort = this.mockChannel[0];
  readonly toMainPort = this.mockChannel[1];
  constructor(workerUrl: string) {
    const randomId = (Date.now() + Math.random()).toString(36);
    const script = (this.script = document.createElement("script"));
    debugger;
    script.dataset.workerId = randomId;
    script.type = "module";
    script.async = true;
    document.head.appendChild(script);
    script.src = workerUrl;
    Object.assign(window, { Worker: nativeWorkerCtor, [randomId]: this });
  }
  postMessage = this.toWorkerPort.postMessage.bind(this.toWorkerPort);
  addEventListener = this.toWorkerPort.addEventListener.bind(this.toWorkerPort);
}

export function prepareInMain(canvas: HTMLCanvasElement) {
  if (typeof canvas.transferControlToOffscreen !== "function") {
    const offscreencanvas = canvas as unknown as OffscreenCanvas;
    offscreencanvas.convertToBlob = (options?: ImageEncodeOptions) => {
      return new Promise<Blob>((resolve, reject) => {
        canvas.toBlob(
          (blob) => {
            if (blob) {
              resolve(blob);
            } else {
              reject();
            }
          },
          options?.type,
          options?.quality
        );
      });
    };
    let isoffscreen = false;
    canvas.transferControlToOffscreen = () => {
      if (isoffscreen) {
        return offscreencanvas;
      }
      isoffscreen = true;
      const offscreenContainer = document.createElement("div");
      offscreenContainer.appendChild(canvas);
      document.body.appendChild(offscreenContainer);
      offscreenContainer.style.pointerEvents = "none";
      offscreenContainer.style.position = "absolute";
      offscreenContainer.style.left = "0";
      offscreenContainer.style.top = "0";
      offscreenContainer.style.width = "0";
      offscreenContainer.style.height = "0";
      offscreenContainer.style.opacity = "0";
      //@ts-ignore
      delete canvas.transferControlToOffscreen;
      return offscreencanvas;
    };
    if (window.Worker.valueOf() !== MokeWorker) {
      Object.assign(window, {
        Worker: MokeWorker,
      });
    }
  }

  // const offscreen = canvas.transferControlToOffscreen();

  //   var randomId = "Offscreen" + Math.round(Math.random() * 1000);
  //   var script = document.createElement("script");
  //   script.src = workerUrl;
  //   script.async = true;
  //   script.dataset.id = randomId;

  //   var connection = { msgs: [], host: listener };
  //   var api = {
  //     post: function (data) {
  //       if (connection.worker) {
  //         connection.worker({ data: data });
  //       } else {
  //         connection.msgs.push(data);
  //       }
  //     },
  //   };

  //   script.onload = function () {
  //     api.post({
  //       canvas: canvas,
  //       width: canvas.clientWidth,
  //       height: canvas.clientHeight,
  //     });
  //   };

  //   document.head.appendChild(script);
  //   window[randomId] = connection;
  //   return api;
  // }
}
