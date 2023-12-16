package org.dweb_browser.dwebview.polyfill

const val WebSocketPolyfillScript = """
const wsport = webkit.messageHandlers.websocket;
const wsevent = new EventTarget();
wsport.event = wsevent;
let ws_id_acc = 1;
function base64ToArrayBuffer(base64) {
  var binaryString = atob(base64);
  var bytes = new Uint8Array(binaryString.length);
  for (var i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes;
}
function arrayBufferToBase64(bytes) {
  let binary = "";
  const len = bytes.byteLength;
  for (var i = 0; i < len; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}
Object.defineProperty(globalThis, "WebSocket", {
  value: ((NWS) =>
    class WebSocket extends EventTarget {
      static CONNECTING = 0;
      static OPEN = 1;
      static CLOSING = 2;
      static CLOSED = 3;
      #binaryType = "blob";
      #ws = null;
      #ws_id = -1;
      #readyState = WebSocket.CONNECTING;
      constructor(url, protocols) {
        super();
        const inputUrl = new URL(url);
        if (inputUrl.hostname.endsWith(".dweb")) {
          const ws_id = ws_id_acc++;
          this.#ws_id = ws_id;
          wsport.postMessage([ws_id, "connect", inputUrl.href]);
          wsevent.addEventListener("message", (e) => {
            const data = e.data;
            if (data[0] !== ws_id) {
              return;
            }
            const cmd = data[1];
            switch (cmd) {
              case "open":
                this.#readyState = WebSocket.OPEN;
                this.dispatchEvent(new CustomEvent("open"));
                break;
              case "message-text":
                this.dispatchEvent(
                  new MessageEvent("message", { data: data[2] })
                );
                break;
              case "message-binary":
                const message_data_raw = base64ToArrayBuffer(data[2]);
                const message_data =
                  this.binaryType === "arraybuffer"
                    ? message_data_raw.buffer
                    : new Blob([message_data_raw]);
                this.dispatchEvent(
                  new MessageEvent("message", {
                    data: message_data,
                  })
                );
                break;
              case "error":
                this.dispatchEvent(new ErrorEvent("error", { error: data[2] }));
                break;
              case "closed":
                this.#readyState = WebSocket.CLOSED;
                this.dispatchEvent(
                  new CloseEvent("close", { code: data[2], reason: data[3] })
                );
                break;
            }
          });
        } else {
          this.#ws = new NWS(url, protocols);
        }
      }
      get binaryType() {
        return this.#binaryType;
      }
      set binaryType(value) {
        this.#binaryType = value;
        if (this.#ws) {
          this.#ws.binaryType = value;
        }
      }
      get readyState() {
        return this.#ws?.readyState ?? this.#readyState;
      }
      get extensions() {
        return "";
      }
      get bufferedAmount() {
        return this.#ws?.bufferedAmount ?? 0;
      }
      #onclose = null;
      get onclose() {
        return this.#ws ? this.#ws.onclose : this.#onclose ?? null;
      }
      set onclose(v) {
        if (this.#ws) {
          this.#ws.onclose = v;
        } else {
          this.#onclose = v;
        }
      }
      #onerror = null;
      get onerror() {
        return this.#ws ? this.#ws.onerror : this.#onerror ?? null;
      }
      set onerror(v) {
        if (this.#ws) {
          this.#ws.onerror = v;
        } else {
          this.#onerror = v;
        }
      }
      #onmessage = null;
      get onmessage() {
        return this.#ws ? this.#ws.onmessage : this.#onmessage ?? null;
      }
      set onmessage(v) {
        if (this.#ws) {
          this.#ws.onmessage = v;
        } else {
          this.#onmessage = v;
        }
      }
      #onopen = null;
      get onopen() {
        return this.#ws ? this.#ws.onopen : this.#onopen ?? null;
      }
      set onopen(v) {
        if (this.#ws) {
          this.#ws.onopen = v;
        } else {
          this.#onopen = v;
        }
      }
      dispatchEvent(event) {
        if (this.#ws) {
          this.#ws.dispatchEvent(event);
        } else {
          super.dispatchEvent(event);
          switch (event.type) {
            case "close":
            case "error":
            case "message":
            case "open":
              this["on" + event.type]?.(event);
          }
        }
      }
      send(data) {
        /// string | ArrayBufferLike | Blob | ArrayBufferView
        if (this.#ws) {
          this.#ws.send(data);
        } else {
          const ws_id = this.#ws_id;
          void (async () => {
            let isBinary = false;
            if (data instanceof Blob) {
              isBinary = true;
              data = new Uint8Array(await data.arrayBuffer());
            } else if (ArrayBuffer.isView(data)) {
              isBinary = true;
              if (!(data instanceof Uint8Array)) {
                data = new Uint8Array(
                  data.buffer,
                  data.byteOffset,
                  data.byteLength
                );
              }
            } else if (
              data instanceof ArrayBuffer ||
              (typeof SharedArrayBuffer === "function" &&
                data instanceof SharedArrayBuffer)
            ) {
              isBinary = true;
              data = new Uint8Array(data);
            } else {
              data = "" + data;
            }
            if (isBinary) {
              data = arrayBufferToBase64(data);
              wsport.postMessage([ws_id, "message-binary", data]);
            } else {
              wsport.postMessage([ws_id, "message-text", data]);
            }
          })();
        }
      }
      close(code, reason) {
        if (this.#ws) {
          this.#ws.close(code, reason);
        } else {
          wsport.postMessage([
            this.#ws_id,
            "close",
            code == undefined ? undefined : +code,
            reason == undefined ? undefined : "" + reason,
          ]);
          this.#readyState = WebSocket.CLOSING;
        }
      }
    })(WebSocket),
});

""";

object WebSocketProxy {
  fun getPolyfillScript(): String {
    return WebSocketPolyfillScript
  }
}