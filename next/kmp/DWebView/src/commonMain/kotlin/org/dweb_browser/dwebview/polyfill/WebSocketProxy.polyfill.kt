package org.dweb_browser.dwebview.polyfill

object WebSocketProxy {
  fun getPolyfillScript(port: Int): String {
    return """
      class DwebWebSocket extends WebSocket {
        constructor(url, protocols) {
          let input = 'ws://localhost:${port}/?X-Dweb-Url=';
  
          if (typeof url === 'string') {
            const _url = new URL(url);
            
            if(_url.hostname.endsWith('.dweb')) {
              input += url;
            } else {
              input = url;
            }
          } else if (url instanceof URL) {
            if(url.hostname.endsWith('.dweb')) {
              input += url.href;
            } else {
              input = url.href;
            }
          }
  
          super(input, protocols);
        }
      }
      Object.defineProperty(globalThis, 
        "WebSocket", {
          value: DwebWebSocket
        }
      );
    """.trimIndent()
  }
}