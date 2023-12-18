(() => {
  navigator;
  const wsport = webkit.messageHandlers.websocket;
  const wsevent = new EventTarget();
  Object.assign(wsport, {
    event: wsevent,
  });
  let ws_id_acc = 1;
  const maxBinaryFrameSize = 65500;
  const maxTextFrameSize = maxBinaryFrameSize / 2; // 字符串需要注意UTF8的格式长度？所以这里统一使用双宽字符
  function base64ToArrayBuffer(base64: string) {
    const binaryString = atob(base64);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes;
  }
  function arrayBufferToBase64(bytes: Uint8Array) {
    let binary = "";
    for (const byte of bytes) {
      binary += String.fromCharCode(byte);
    }
    return btoa(binary);
  }
  // const textEncoder = new TextEncoder();
  type $SendData = string | ArrayBufferLike | Blob | ArrayBufferView;
  type $SendDataRaw = string | Uint8Array;
  const DwebWebSocket = class WebSocket extends EventTarget {
    static CONNECTING = 0;
    static OPEN = 1;
    static CLOSING = 2;
    static CLOSED = 3;
    #binaryType = "blob";
    #ws_id = -1;
    #url: string;
    #readyState = WebSocket.CONNECTING;
    #sendQueue: Promise<void>;
    constructor(url: string) {
      super();
      const ws_id = ws_id_acc++;
      this.#ws_id = ws_id;
      void wsport.postMessage([ws_id, "connect", (this.#url = url)]);

      let sendQueueResolver: () => void;
      this.#sendQueue = new Promise((resolve) => (sendQueueResolver = resolve));
      wsevent.addEventListener("message", (e) => {
        const data = (e as MessageEvent).data;
        if (data[0] !== ws_id) {
          return;
        }
        const cmd = data[1];
        switch (cmd) {
          case "open":
            this.#readyState = WebSocket.OPEN;
            sendQueueResolver();
            this.dispatchEvent(new CustomEvent("open"));
            break;
          case "message-text":
            this.dispatchEvent(new MessageEvent("message", { data: data[2] }));
            break;
          case "message-binary": {
            const message_data_raw = base64ToArrayBuffer(data[2]);
            const message_data =
              this.binaryType === "arraybuffer" ? message_data_raw.buffer : new Blob([message_data_raw]);
            this.dispatchEvent(
              new MessageEvent("message", {
                data: message_data,
              })
            );
            break;
          }
          case "error":
            this.dispatchEvent(new ErrorEvent("error", { error: data[2] }));
            break;
          case "closed":
            this.#readyState = WebSocket.CLOSED;
            this.dispatchEvent(new CloseEvent("close", { code: data[2], reason: data[3] }));
            break;
        }
      });
    }
    get binaryType() {
      return this.#binaryType;
    }
    set binaryType(value) {
      this.#binaryType = value;
    }
    get readyState() {
      return this.#readyState;
    }
    get extensions() {
      return "";
    }
    #bufferedAmount = 0;
    get bufferedAmount() {
      return this.#bufferedAmount;
    }

    #calcDataSize(data: $SendData) {
      if (typeof data === "string") {
        return data.length;
      }
      if (data instanceof Blob) {
        return data.size;
      }
      if (
        ArrayBuffer.isView(data) ||
        data instanceof ArrayBuffer ||
        (typeof SharedArrayBuffer === "function" && data instanceof SharedArrayBuffer)
      ) {
        return data.byteLength;
      }
      return ("" + data).length;
    }
    async *#normalizeData(data: $SendData): AsyncGenerator<$SendDataRaw> {
      if (data instanceof Blob) {
        if (data.size > maxBinaryFrameSize) {
          const reader = data.stream().getReader();
          while (true) {
            const frame = await reader.read();
            if (frame.done) {
              return;
            }
            yield* this.#normalizeData(frame.value);
          }
        } else {
          yield new Uint8Array(await data.arrayBuffer());
        }
      } else if (ArrayBuffer.isView(data)) {
        yield* this.#normalizeData(data.buffer.slice(data.byteOffset, data.byteLength));
      } else if (
        data instanceof ArrayBuffer ||
        (typeof SharedArrayBuffer === "function" && data instanceof SharedArrayBuffer)
      ) {
        if (data.byteLength > maxBinaryFrameSize) {
          let pos = 0;
          while (pos < data.byteLength) {
            yield new Uint8Array(data.slice(pos, (pos += maxBinaryFrameSize)));
          }
        } else {
          yield new Uint8Array(data);
        }
      } else {
        if (typeof data !== "string") {
          data = "" + data;
        }
        if (data.length > maxTextFrameSize) {
          let pos = 0;
          while (pos < data.length) {
            yield data.slice(pos, (pos += maxTextFrameSize));
          }
        } else {
          yield data;
        }
      }
    }
    async *#normalizeDataWithFin(data: $SendData) {
      let last_data: $SendDataRaw | undefined;
      for await (const data_raw of this.#normalizeData(data)) {
        if (last_data !== undefined) {
          yield { fin: false, data: last_data };
        }
        last_data = data_raw;
      }
      if (last_data !== undefined) {
        yield { fin: true, data: last_data };
      }
    }
    async #sendRawData(fin: boolean, data_raw: $SendDataRaw) {
      const ws_id = this.#ws_id;
      if (typeof data_raw === "string") {
        await wsport.postMessage([ws_id, "frame-text", fin, data_raw]);
      } else {
        await wsport.postMessage([ws_id, "frame-binary", fin, arrayBufferToBase64(data_raw)]);
      }
    }
    async #send(data: $SendData) {
      for await (const item of this.#normalizeDataWithFin(data)) {
        await this.#sendRawData(item.fin, item.data);
        this.#bufferedAmount -= item.data.length;
      }
    }
    send(data: $SendData) {
      if (this.readyState === WebSocket.CONNECTING) {
        throw new DOMException(`Failed to execute 'send' on 'WebSocket': Still in CONNECTING state.`);
      }
      this.#bufferedAmount += this.#calcDataSize(data);
      if (this.readyState > WebSocket.OPEN) {
        console.error(`WebSocket is already in CLOSING or CLOSED state.`);
        return;
      }
      // WebSocket 必须顺序发送
      this.#sendQueue = this.#sendQueue.then(() => this.#send(data));
    }
    close(code?: number, reason?: string) {
      if (this.readyState === WebSocket.CONNECTING) {
        console.warn(
          `WebSocket connection to '${this.#url}' failed: WebSocket is closed before the connection is established.`
        );
      } else if (this.readyState === WebSocket.OPEN) {
        this.#readyState = WebSocket.CLOSING;
        void wsport.postMessage([
          this.#ws_id,
          "close",
          code == undefined ? undefined : +code,
          reason == undefined ? undefined : "" + reason,
        ]);
      }
    }
    override addEventListener(
      type: string,
      callback: EventListenerOrEventListenerObject | null,
      options?: AddEventListenerOptions | boolean
    ) {
      return super.addEventListener(type, callback, options);
    }
    override dispatchEvent(event: Event) {
      return super.dispatchEvent(event);
    }
    override removeEventListener(
      type: string,
      callback: EventListenerOrEventListenerObject | null,
      options?: EventListenerOptions | boolean
    ) {
      return super.removeEventListener(type, callback, options);
    }
  };
  type $DwebWebSocket = InstanceType<typeof DwebWebSocket>;
  /// on* 属性
  for (const eventName of ["close", "error", "message", "open"]) {
    const wm = new WeakMap<$DwebWebSocket, EventListener>();
    const attributes: PropertyDescriptor & ThisType<$DwebWebSocket> = {
      get() {
        return wm.get(this) ?? null;
      },
      set(v) {
        const oldv = wm.get(this);
        if (oldv == v) {
          return;
        }
        if (typeof oldv === "function") {
          this.removeEventListener(eventName, oldv);
        }

        if (typeof v === "function") {
          wm.set(this, v);
          this.addEventListener(eventName, v);
        } else {
          wm.delete(this);
        }
      },
    };
    Object.defineProperty(DwebWebSocket.prototype, "on" + eventName, attributes);
  }
  /// instanceof WebSocket
  Object.setPrototypeOf(DwebWebSocket.prototype, WebSocket.prototype);

  Object.defineProperty(globalThis, "WebSocket", {
    value: ((NativeWebSocket) => {
      const WebSocketProxy = new Proxy(NativeWebSocket, {
        get(target, propertyKey, receiver) {
          if (propertyKey === "__native__") {
            return NativeWebSocket;
          }
          if (propertyKey === "__dweb__") {
            return DwebWebSocket;
          }
          return Reflect.get(target, propertyKey, receiver);
        },
        construct(target, argArray, newTarget) {
          const url = argArray[0];
          const inputUrl = url instanceof URL ? url : new URL(url);
          if (inputUrl.hostname.endsWith(".dweb")) {
            return new DwebWebSocket(inputUrl.href);
          }
          return Reflect.construct(target, argArray, newTarget);
        },
      });
      return WebSocketProxy;
    })(WebSocket),
  });
})();
