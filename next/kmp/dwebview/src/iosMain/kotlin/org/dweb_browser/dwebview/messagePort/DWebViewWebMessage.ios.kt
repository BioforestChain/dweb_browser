package org.dweb_browser.dwebview.messagePort

import org.dweb_browser.core.ipc.helper.DWebMessage
import org.dweb_browser.dwebview.DWebView
import platform.Foundation.NSNumber
import platform.Foundation.valueForKey
import platform.WebKit.WKContentWorld
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.darwin.NSObject

internal class DWebViewWebMessage(val webview: DWebView) {
  companion object {

    const val WebMessagePortPrepareCode = """
      const ALL_PORT = new Map();
      let portIdAcc = 1;
      const PORTS_ID = new WeakMap();
      const getPortId = (port) => {
          let port_id = PORTS_ID.get(port);
          if (port_id === undefined) {
              const current_port_id = portIdAcc++;
              port_id = current_port_id;
              ALL_PORT.set(port_id, port);
              port.addEventListener('message', (event) => {
                  let data = event.data;
                  if (typeof data !== 'string') {
                      data = Array.from(data);
                  }
                  webkit.messageHandlers.webMessagePort.postMessage({
                      type: 'message',
                      id: current_port_id,
                      data: data,
                      ports: event.ports.map(getPortId),
                  });
              });
          }
          return port_id;
      };
      function nativeCreateMessageChannel() {
          const channel = new MessageChannel();
          const port1_id = getPortId(channel.port1);
          const port2_id = getPortId(channel.port2);
          return [port1_id, port2_id];
      }
      function forceGetPort(port_id) {
          const port = ALL_PORT.get(port_id);
          if (port === undefined) {
              throw new Error(`no found messagePort by ref: $\{port_id}`);
          }
          return port;
      }
      function nativePortPostMessage(port_id, data, ports_id) {
          const origin_port = forceGetPort(port_id);
          const transfer_ports = ports_id.map(forceGetPort);
          if (typeof data !== "string") {
              const u8a = new Uint8Array(data);
              transfer_ports.push(u8a.buffer);
              origin_port.postMessage(u8a, transfer_ports);
          } else if(typeof data === "object") {
              origin_port.postMessage(JSON.stringify(data), transfer_ports);    
          }
          else {
              origin_port.postMessage(data, transfer_ports);
          }
      }
      function nativeStart(port_id) {
          const origin_port = forceGetPort(port_id);
          origin_port.start();
      }
      function nativeWindowPostMessage(data, ports_id) {
          const ports = ports_id.map(forceGetPort);
          dispatchEvent(new MessageEvent('message', { data, ports }));
      }
      function nativeClose(port_id) {
          const origin_port = forceGetPort(port_id);
          origin_port.close();
      }
      """;
    val webMessagePortContentWorld = WKContentWorld.worldWithName("web-message-port");
    val allPorts = mutableMapOf<Int, DWebMessagePort>()

  }

  internal class WebMessagePortMessageHandler : NSObject(), WKScriptMessageHandlerProtocol {
    override fun userContentController(
      userContentController: WKUserContentController,
      didReceiveScriptMessage: WKScriptMessage
    ) {
      try {
        val message = didReceiveScriptMessage.body as NSObject
        val type = message.valueForKey("type") as String

        if (type == "message") {
          val id = (message.valueForKey("id") as NSNumber).intValue
          val data = message.valueForKey("data") as String
          val ports = mutableListOf<DWebMessagePort>()
          val originPort = allPorts[id] ?: throw Exception("no found port by id:$id")

          originPort.dispatchMessage(DWebMessage.DWebMessageString(data, ports))
        }
      } catch (_: Throwable) {
      }
    }
  }
}